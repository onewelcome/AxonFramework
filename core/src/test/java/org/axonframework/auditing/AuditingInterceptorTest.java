/*
 * Copyright (c) 2010-2012. Axon Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.axonframework.auditing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.commandhandling.GenericCommandMessage;
import org.axonframework.commandhandling.InterceptorChain;
import org.axonframework.domain.DomainEventMessage;
import org.axonframework.domain.EventMessage;
import org.axonframework.domain.StubAggregate;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.testutils.MockException;
import org.axonframework.unitofwork.CurrentUnitOfWork;
import org.axonframework.unitofwork.DefaultUnitOfWork;
import org.axonframework.unitofwork.SaveAggregateCallback;
import org.axonframework.unitofwork.UnitOfWork;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import java.util.Collections;
import java.util.List;

/**
 * @author Allard Buijze
 */
@SuppressWarnings({"unchecked", "ThrowableInstanceNeverThrown"})
public class AuditingInterceptorTest {

    private AuditingInterceptor testSubject;
    private AuditDataProvider mockAuditDataProvider;
    private AuditLogger mockAuditLogger;
    private InterceptorChain mockInterceptorChain;

    @Before
    public void setUp() {
        testSubject = new AuditingInterceptor();
        mockAuditDataProvider = mock(AuditDataProvider.class);
        mockAuditLogger = mock(AuditLogger.class);
        mockInterceptorChain = mock(InterceptorChain.class);

        testSubject.setAuditDataProvider(mockAuditDataProvider);
        testSubject.setAuditLogger(mockAuditLogger);

        when(mockAuditDataProvider.provideAuditDataFor(any(CommandMessage.class)))
                .thenReturn(Collections.singletonMap("key", (Object) "value"));
    }

    @After
    public void tearDown() {
        while (CurrentUnitOfWork.isStarted()) {
            CurrentUnitOfWork.get().rollback(null);
        }
    }

    @Test
    public void testInterceptCommand_SuccessfulExecution() throws Throwable {
        when(mockInterceptorChain.proceed()).thenReturn("Return value");
        UnitOfWork uow = DefaultUnitOfWork.startAndGet();
        StubAggregate aggregate = new StubAggregate();
        uow.registerAggregate(aggregate, mock(EventBus.class), mock(SaveAggregateCallback.class));
        GenericCommandMessage<String> command = new GenericCommandMessage<String>("Command!");
        Object result = testSubject.handle(command, uow, mockInterceptorChain);
        verify(mockAuditDataProvider, never()).provideAuditDataFor(any(CommandMessage.class));

        aggregate.doSomething();
        aggregate.doSomething();

        assertEquals("Return value", result);
        uow.commit();

        verify(mockAuditDataProvider, atLeast(1)).provideAuditDataFor(command);
        verify(mockAuditLogger, times(1)).logSuccessful(eq(command), any(Object.class), listWithTwoEventMessages());
        DomainEventMessage eventFromAggregate = aggregate.getUncommittedEvents().next();
        assertEquals("value", eventFromAggregate.getMetaData().get("key"));
    }

    @SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
    @Test
    public void testInterceptCommand_FailedExecution() throws Throwable {
        RuntimeException mockException = new MockException();
        when(mockInterceptorChain.proceed()).thenThrow(mockException);
        UnitOfWork uow = DefaultUnitOfWork.startAndGet();

        GenericCommandMessage command = new GenericCommandMessage("Command!");
        try {
            testSubject.handle(command, uow, mockInterceptorChain);
        } catch (RuntimeException e) {
            assertSame(mockException, e);
        }

        StubAggregate aggregate = new StubAggregate();
        uow.registerAggregate(aggregate, mock(EventBus.class), mock(SaveAggregateCallback.class));
        aggregate.doSomething();
        aggregate.doSomething();

        RuntimeException mockFailure = new RuntimeException("mock");
        uow.rollback(mockFailure);

        verify(mockAuditDataProvider, times(2)).provideAuditDataFor(any(CommandMessage.class));
        verify(mockAuditLogger, never()).logSuccessful(eq(command), any(Object.class), any(List.class));
        verify(mockAuditLogger).logFailed(eq(command), eq(mockFailure), listWithTwoEventMessages());
    }

    private List<EventMessage> listWithTwoEventMessages() {
        return argThat(new ArgumentMatcher<>() {
            @Override
            public boolean matches(List<EventMessage> item) {
                return item != null && item.size() == 2;
            }

            @Override
            public String toString() {
                return "A List with two EventMessages";
            }
        });
    }
}
