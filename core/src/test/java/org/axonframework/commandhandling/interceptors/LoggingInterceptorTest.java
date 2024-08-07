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

package org.axonframework.commandhandling.interceptors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.AdditionalMatchers.and;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.axonframework.commandhandling.GenericCommandMessage;
import org.axonframework.commandhandling.InterceptorChain;
import org.axonframework.unitofwork.UnitOfWork;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.Log4jLoggerAdapter;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;

/**
 * @author Allard Buijze
 */
@SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
public class LoggingInterceptorTest {

    private LoggingInterceptor testSubject;
    private org.apache.log4j.Logger mockLogger;
    private InterceptorChain interceptorChain;
    private UnitOfWork unitOfWork;

    @Before
    public void setUp() throws Exception {
        testSubject = new LoggingInterceptor();
        Log4jLoggerAdapter logger = (Log4jLoggerAdapter) LoggerFactory.getLogger(LoggingInterceptor.class);
        Field loggerField = logger.getClass().getDeclaredField("logger");
        ReflectionUtils.makeAccessible(loggerField);
        mockLogger = mock(Logger.class);
        loggerField.set(logger, mockLogger);
        interceptorChain = mock(InterceptorChain.class);
        unitOfWork = mock(UnitOfWork.class);
    }

    @Test
    public void testIncomingLogging_NullReturnValue() throws Throwable {
        when(mockLogger.isInfoEnabled()).thenReturn(true);
        when(interceptorChain.proceed()).thenReturn(null);

        testSubject.handle(new GenericCommandMessage<Object>(new StubCommand()), unitOfWork, interceptorChain);

        verify(mockLogger, atLeast(1)).isInfoEnabled();
        verify(mockLogger, times(2)).log(any(String.class), any(Priority.class), contains("[StubCommand]"),
                                         isNull(Throwable.class));
        verify(mockLogger).log(any(String.class), any(Priority.class), and(contains("[StubCommand]"),
                                                                           contains("[null]")), isNull(Throwable.class));
        verifyNoMoreInteractions(mockLogger);
    }

    @Test
    public void testSuccessfulExecution_VoidReturnValue() throws Throwable {
        when(mockLogger.isInfoEnabled()).thenReturn(true);
        when(interceptorChain.proceed()).thenReturn(null);

        testSubject.handle(new GenericCommandMessage<Object>(new StubCommand()), unitOfWork, interceptorChain);

        verify(mockLogger, atLeast(1)).isInfoEnabled();
        verify(mockLogger, times(2)).log(any(String.class), any(Priority.class), contains("[StubCommand]"),
                                         isNull(Throwable.class));
        verify(mockLogger).log(any(String.class), any(Priority.class), and(contains("[StubCommand]"),
                                                                           contains("[null]")), isNull(Throwable.class));
        verifyNoMoreInteractions(mockLogger);
    }

    @Test
    public void testSuccessfulExecution_CustomReturnValue() throws Throwable {
        when(interceptorChain.proceed()).thenReturn(new StubResponse());
        when(mockLogger.isInfoEnabled()).thenReturn(true);

        testSubject.handle(new GenericCommandMessage<Object>(new StubCommand()), unitOfWork, interceptorChain);

        verify(mockLogger, atLeast(1)).isInfoEnabled();
        verify(mockLogger).log(any(String.class), eq(Level.INFO),
                               and(contains("[StubCommand]"), contains("[StubResponse]")),
                               isNull(Throwable.class));
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    @Test
    public void testFailedExecution() throws Throwable {
        RuntimeException exception = new RuntimeException();
        when(interceptorChain.proceed()).thenThrow(exception);
        when(mockLogger.isInfoEnabled()).thenReturn(true);

        try {
            testSubject.handle(new GenericCommandMessage<Object>(new StubCommand()), unitOfWork, interceptorChain);
            fail("Expected exception to be propagated");
        } catch (RuntimeException e) {
            // expected
        }

        verify(mockLogger).log(any(String.class), eq(Level.WARN), and(contains("[StubCommand]"),
                                                                      contains("failed")), eq(exception));
    }

    @Test
    public void testConstructorWithCustomLogger() throws Exception {
        testSubject = new LoggingInterceptor("my.custom.logger");
        Field field = testSubject.getClass().getDeclaredField("logger");
        field.setAccessible(true);
        Log4jLoggerAdapter logger = (Log4jLoggerAdapter) field.get(testSubject);
        assertEquals("my.custom.logger", logger.getName());
    }

    private static class StubCommand {

    }

    private static class StubResponse {

    }
}
