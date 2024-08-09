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

package org.axonframework.eventsourcing;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.axonframework.common.DirectExecutor;
import org.axonframework.domain.DomainEventMessage;
import org.axonframework.domain.DomainEventStream;
import org.axonframework.domain.GenericDomainEventMessage;
import org.axonframework.domain.MetaData;
import org.axonframework.domain.SimpleDomainEventStream;
import org.axonframework.eventstore.SnapshotEventStore;
import org.axonframework.repository.ConcurrencyException;
import org.axonframework.unitofwork.TransactionManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;

/**
 * @author Allard Buijze
 */
public class AbstractSnapshotterTest {

    private AbstractSnapshotter testSubject;
    private SnapshotEventStore mockEventStore;
    private Logger logger;
    private Logger originalLogger;

    @Before
    public void setUp() throws Exception {
        mockEventStore = mock(SnapshotEventStore.class);
        testSubject = new AbstractSnapshotter() {
            @Override
            protected DomainEventMessage createSnapshot(String typeIdentifier, Object aggregateIdentifier,
                                                        DomainEventStream eventStream) {
                long lastIdentifier = getLastIdentifierFrom(eventStream);
                if (lastIdentifier <= 0) {
                    return null;
                }
                return new GenericDomainEventMessage<String>(aggregateIdentifier, lastIdentifier,
                                                             "Mock contents", MetaData.emptyInstance());
            }
        };
        testSubject.setEventStore(mockEventStore);
        testSubject.setExecutor(DirectExecutor.INSTANCE);
        logger = mock(Logger.class);
    }

    @Test
    public void testScheduleSnapshot() {
        Object aggregateIdentifier = "aggregateIdentifier";
        when(mockEventStore.readEvents("test", aggregateIdentifier))
                .thenReturn(new SimpleDomainEventStream(
                        new GenericDomainEventMessage<String>(aggregateIdentifier, (long) 0,
                                                              "Mock contents", MetaData.emptyInstance()),
                        new GenericDomainEventMessage<String>(aggregateIdentifier, (long) 1,
                                                              "Mock contents", MetaData.emptyInstance())));
        testSubject.scheduleSnapshot("test", aggregateIdentifier);
        verify(mockEventStore).appendSnapshotEvent(eq("test"), argThat(event(aggregateIdentifier, 1)));
    }

    @Test
    public void testScheduleSnapshot_ConcurrencyExceptionIsSilenced()
            throws NoSuchFieldException, IllegalAccessException {
        final Object aggregateIdentifier = "aggregateIdentifier";
        doNothing()
                .doThrow(new ConcurrencyException("Mock"))
                .when(mockEventStore).appendSnapshotEvent(eq("test"), isA(DomainEventMessage.class));
        when(mockEventStore.readEvents("test", aggregateIdentifier))
                .thenAnswer(new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                        return new SimpleDomainEventStream(
                                new GenericDomainEventMessage<String>(aggregateIdentifier, (long) 0,
                                                                      "Mock contents", MetaData.emptyInstance()),
                                new GenericDomainEventMessage<String>(aggregateIdentifier, (long) 1,
                                                                      "Mock contents", MetaData.emptyInstance()));
                    }
                });
        testSubject.scheduleSnapshot("test", aggregateIdentifier);

        testSubject.scheduleSnapshot("test", aggregateIdentifier);
        verify(mockEventStore, times(2)).appendSnapshotEvent(eq("test"), argThat(event(aggregateIdentifier, 1)));
        verify(logger, never()).warn(anyString());
        verify(logger, never()).error(anyString());
    }

    @Test
    public void testScheduleSnapshot_SnapshotIsNull() {
        Object aggregateIdentifier = "aggregateIdentifier";
        when(mockEventStore.readEvents("test", aggregateIdentifier))
                .thenReturn(new SimpleDomainEventStream(
                        new GenericDomainEventMessage<String>(aggregateIdentifier, (long) 0,
                                                              "Mock contents", MetaData.emptyInstance())));
        testSubject.scheduleSnapshot("test", aggregateIdentifier);
        verify(mockEventStore, never()).appendSnapshotEvent(any(String.class), any(DomainEventMessage.class));
    }

    @Test
    public void testScheduleSnapshot_SnapshotReplacesOneEvent() {
        Object aggregateIdentifier = "aggregateIdentifier";
        when(mockEventStore.readEvents("test", aggregateIdentifier))
                .thenReturn(new SimpleDomainEventStream(
                        new GenericDomainEventMessage<String>(aggregateIdentifier, (long) 2,
                                                              "Mock contents", MetaData.emptyInstance())));
        testSubject.scheduleSnapshot("test", aggregateIdentifier);
        verify(mockEventStore, never()).appendSnapshotEvent(any(String.class), any(DomainEventMessage.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testScheduleSnapshot_WithTransaction() {
        final TransactionManager txManager = mock(TransactionManager.class);
        testSubject.setTxManager(txManager);

        testScheduleSnapshot();

        InOrder inOrder = inOrder(mockEventStore, txManager);
        inOrder.verify(txManager).startTransaction();
        inOrder.verify(mockEventStore).readEvents(isA(String.class), any());
        inOrder.verify(mockEventStore).appendSnapshotEvent(anyString(), isA(DomainEventMessage.class));
        inOrder.verify(txManager).commitTransaction(any());
    }

    private ArgumentMatcher<DomainEventMessage> event(final Object aggregateIdentifier, final long i) {
        return event -> {
            if (event == null) {
                return false;
            }
            return aggregateIdentifier.equals(event.getAggregateIdentifier())
                    && event.getSequenceNumber() == i;
        };
    }

    private long getLastIdentifierFrom(DomainEventStream eventStream) {
        long lastSequenceNumber = -1;
        while (eventStream.hasNext()) {
            lastSequenceNumber = eventStream.next().getSequenceNumber();
        }
        return lastSequenceNumber;
    }
}
