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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.axonframework.domain.DomainEventMessage;
import org.axonframework.domain.DomainEventStream;
import org.axonframework.domain.EventMessage;
import org.axonframework.domain.GenericDomainEventMessage;
import org.axonframework.domain.MetaData;
import org.axonframework.domain.SimpleDomainEventStream;
import org.axonframework.domain.StubDomainEvent;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.eventstore.EventStore;
import org.axonframework.eventstore.SnapshotEventStore;
import org.axonframework.repository.ConflictingAggregateVersionException;
import org.axonframework.unitofwork.CurrentUnitOfWork;
import org.axonframework.unitofwork.DefaultUnitOfWork;
import org.axonframework.unitofwork.UnitOfWork;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * @author Allard Buijze
 */
public class EventSourcingRepositoryTest {

    private SnapshotEventStore mockEventStore;
    private EventBus mockEventBus;
    private EventSourcingRepository<TestAggregate> testSubject;
    private UnitOfWork unitOfWork;
    private StubAggregateFactory stubAggregateFactory;

    @Before
    public void setUp() {
        mockEventStore = mock(SnapshotEventStore.class);
        mockEventBus = mock(EventBus.class);
        stubAggregateFactory = new StubAggregateFactory();
        testSubject = new EventSourcingRepository<TestAggregate>(stubAggregateFactory, mockEventStore);
        testSubject.setEventBus(mockEventBus);
        unitOfWork = DefaultUnitOfWork.startAndGet();
    }

    @After
    public void tearDown() {
        if (unitOfWork.isStarted()) {
            unitOfWork.rollback();
        }
    }

    @Test
    public void testLoadAndSaveAggregate() {
        UUID identifier = UUID.randomUUID();
        DomainEventMessage event1 = new GenericDomainEventMessage<String>(identifier, (long) 1,
                                                                          "Mock contents", MetaData.emptyInstance());
        DomainEventMessage event2 = new GenericDomainEventMessage<String>(identifier, (long) 2,
                                                                          "Mock contents", MetaData.emptyInstance());
        when(mockEventStore.readEvents("test", identifier)).thenReturn(new SimpleDomainEventStream(event1, event2));

        TestAggregate aggregate = testSubject.load(identifier, null);

        assertEquals(0, aggregate.getUncommittedEventCount());
        assertEquals(2, aggregate.getHandledEvents().size());
        assertSame(event1, aggregate.getHandledEvents().get(0));
        assertSame(event2, aggregate.getHandledEvents().get(1));

        // now the aggregate is loaded (and hopefully correctly locked)
        StubDomainEvent event3 = new StubDomainEvent();

        aggregate.apply(event3);

        CurrentUnitOfWork.commit();

        verify(mockEventBus).publish(isA(DomainEventMessage.class));
        verify(mockEventBus, never()).publish(event1);
        verify(mockEventBus, never()).publish(event2);
        verify(mockEventStore, times(1)).appendEvents(eq("test"), isA(DomainEventStream.class));
        assertEquals(0, aggregate.getUncommittedEventCount());
    }

    @Test
    public void testLoad_FirstEventIsSnapshot() {
        UUID identifier = UUID.randomUUID();
        TestAggregate aggregate = new TestAggregate(identifier);
        when(mockEventStore.readEvents("test", identifier)).thenReturn(new SimpleDomainEventStream(
                new GenericDomainEventMessage<TestAggregate>(identifier, 10, aggregate)
        ));
        assertSame(aggregate, testSubject.load(identifier));
    }

    @Test
    public void testLoadAndSaveWithConflictingChanges() {
        ConflictResolver conflictResolver = mock(ConflictResolver.class);
        UUID identifier = UUID.randomUUID();
        DomainEventMessage event2 = new GenericDomainEventMessage<String>(identifier, (long) 2,
                                                                          "Mock contents", MetaData.emptyInstance());
        DomainEventMessage event3 = new GenericDomainEventMessage<String>(identifier, (long) 3,
                                                                          "Mock contents", MetaData.emptyInstance());
        when(mockEventStore.readEvents("test", identifier)).thenReturn(
                new SimpleDomainEventStream(new GenericDomainEventMessage<String>(identifier, (long) 1,
                                                                                  "Mock contents",
                                                                                  MetaData.emptyInstance()
                ), event2, event3));
        testSubject.setConflictResolver(conflictResolver);
        TestAggregate actual = testSubject.load(identifier, 1L);
        verify(conflictResolver, never()).resolveConflicts(anyList(), anyList());
        final StubDomainEvent appliedEvent = new StubDomainEvent();
        actual.apply(appliedEvent);

        CurrentUnitOfWork.commit();

        verify(conflictResolver).resolveConflicts(payloadsEqual(appliedEvent), eq(Arrays.asList(event2, event3)));
    }

    private List<DomainEventMessage> payloadsEqual(final StubDomainEvent expectedEvent) {
        return argThat(new ArgumentMatcher<>() {
            @Override
            public boolean matches(List<DomainEventMessage> domainEventMessages) {
                return domainEventMessages != null && domainEventMessages.size() >= 0
                        && domainEventMessages.get(0).getPayload().equals(expectedEvent);
            }

            @Override
            public String toString() {
                return String.format("List with an event with a %s payload", expectedEvent);
            }
        });
    }

    @Test
    public void testLoadWithConflictingChanges_NoConflictResolverSet() {
        UUID identifier = UUID.randomUUID();
        DomainEventMessage event2 = new GenericDomainEventMessage<String>(identifier, (long) 2,
                                                                          "Mock contents", MetaData.emptyInstance());
        DomainEventMessage event3 = new GenericDomainEventMessage<String>(identifier, (long) 3,
                                                                          "Mock contents", MetaData.emptyInstance());
        when(mockEventStore.readEvents("test", identifier)).thenReturn(
                new SimpleDomainEventStream(new GenericDomainEventMessage<String>(identifier, (long) 1,
                                                                                  "Mock contents",
                                                                                  MetaData.emptyInstance()
                ), event2, event3)
        );

        try {
            testSubject.load(identifier, 1L);
            fail("Expected ConflictingAggregateVersionException");
        } catch (ConflictingAggregateVersionException e) {
            assertEquals(identifier, e.getAggregateIdentifier());
            assertEquals(1L, e.getExpectedVersion());
            assertEquals(3L, e.getActualVersion());
        }
    }

    @Test
    public void testLoadWithConflictingChanges_NoConflictResolverSet_UsingTooHighExpectedVersion() {
        UUID identifier = UUID.randomUUID();
        DomainEventMessage event2 = new GenericDomainEventMessage<String>(identifier, (long) 2,
                                                                          "Mock contents", MetaData.emptyInstance());
        DomainEventMessage event3 = new GenericDomainEventMessage<String>(identifier, (long) 3,
                                                                          "Mock contents", MetaData.emptyInstance());
        when(mockEventStore.readEvents("test", identifier)).thenReturn(
                new SimpleDomainEventStream(new GenericDomainEventMessage<String>(identifier, (long) 1,
                                                                                  "Mock contents",
                                                                                  MetaData.emptyInstance()
                ), event2, event3));

        try {
            testSubject.load(identifier, 100L);
            fail("Expected ConflictingAggregateVersionException");
        } catch (ConflictingAggregateVersionException e) {
            assertEquals(identifier, e.getAggregateIdentifier());
            assertEquals(100L, e.getExpectedVersion());
            assertEquals(3L, e.getActualVersion());
        }
    }

    @Test
    public void testLoadAndSaveWithoutConflictingChanges() {
        ConflictResolver conflictResolver = mock(ConflictResolver.class);
        UUID identifier = UUID.randomUUID();
        when(mockEventStore.readEvents("test", identifier)).thenReturn(
                new SimpleDomainEventStream(new GenericDomainEventMessage<String>(identifier, (long) 1,
                                                                                  "Mock contents",
                                                                                  MetaData.emptyInstance()
                ),
                                            new GenericDomainEventMessage<String>(identifier, (long) 2,
                                                                                  "Mock contents",
                                                                                  MetaData.emptyInstance()
                                            ),
                                            new GenericDomainEventMessage<String>(identifier, (long) 3,
                                                                                  "Mock contents",
                                                                                  MetaData.emptyInstance()
                                            )));
        testSubject.setConflictResolver(conflictResolver);
        TestAggregate actual = testSubject.load(identifier, 3L);
        verify(conflictResolver, never()).resolveConflicts(anyList(), anyList());
        actual.apply(new StubDomainEvent());

        CurrentUnitOfWork.commit();

        verify(conflictResolver, never()).resolveConflicts(anyList(), anyList());
    }

    @Test
    public void testLoadEventsWithDecorators() {
        UUID identifier = UUID.randomUUID();
        SpyEventPreprocessor decorator1 = new SpyEventPreprocessor();
        SpyEventPreprocessor decorator2 = new SpyEventPreprocessor();
        testSubject.setEventStreamDecorators(Arrays.asList(decorator1, decorator2));
        when(mockEventStore.readEvents("test", identifier)).thenReturn(
                new SimpleDomainEventStream(new GenericDomainEventMessage<String>(identifier, (long) 1,
                                                                                  "Mock contents",
                                                                                  MetaData.emptyInstance()
                ),
                                            new GenericDomainEventMessage<String>(identifier, (long) 2,
                                                                                  "Mock contents",
                                                                                  MetaData.emptyInstance()
                                            ),
                                            new GenericDomainEventMessage<String>(identifier, (long) 3,
                                                                                  "Mock contents",
                                                                                  MetaData.emptyInstance()
                                            )));
        TestAggregate aggregate = testSubject.load(identifier);
        // loading them in...
        InOrder inOrder = Mockito.inOrder(decorator1.lastSpy, decorator2.lastSpy);
        inOrder.verify(decorator2.lastSpy).next();
        inOrder.verify(decorator1.lastSpy).next();

        inOrder.verify(decorator2.lastSpy).next();
        inOrder.verify(decorator1.lastSpy).next();

        inOrder.verify(decorator2.lastSpy).next();
        inOrder.verify(decorator1.lastSpy).next();
        aggregate.apply(new StubDomainEvent());
        aggregate.apply(new StubDomainEvent());
    }

    @Test
    public void testSaveEventsWithDecorators() {
        testSubject = new EventSourcingRepository<TestAggregate>(stubAggregateFactory, new EventStore() {
            @Override
            public void appendEvents(String type, DomainEventStream events) {
                while (events.hasNext()) {
                    events.next();
                }
            }

            @Override
            public DomainEventStream readEvents(String type, Object identifier) {
                return mockEventStore.readEvents(type, identifier);
            }
        });
        testSubject.setEventBus(mockEventBus);
        SpyEventPreprocessor decorator1 = new SpyEventPreprocessor();
        SpyEventPreprocessor decorator2 = new SpyEventPreprocessor();
        testSubject.setEventStreamDecorators(Arrays.asList(decorator1, decorator2));
        UUID identifier = UUID.randomUUID();
        when(mockEventStore.readEvents("test", identifier)).thenReturn(
                new SimpleDomainEventStream(new GenericDomainEventMessage<String>(identifier, (long) 3,
                                                                                  "Mock contents",
                                                                                  MetaData.emptyInstance()
                )));
        TestAggregate aggregate = testSubject.load(identifier);
        aggregate.apply(new StubDomainEvent());
        aggregate.apply(new StubDomainEvent());

        CurrentUnitOfWork.commit();

        InOrder inOrder = Mockito.inOrder(decorator1.lastSpy, decorator2.lastSpy);
        inOrder.verify(decorator1.lastSpy).next();
        inOrder.verify(decorator2.lastSpy).next();

        inOrder.verify(decorator1.lastSpy).next();
        inOrder.verify(decorator2.lastSpy).next();
    }

    private static class StubAggregateFactory extends AbstractAggregateFactory<TestAggregate> {

        @Override
        public TestAggregate doCreateAggregate(Object aggregateIdentifier,
                                             DomainEventMessage firstEvent) {
            return new TestAggregate((UUID) aggregateIdentifier);
        }

        @Override
        public String getTypeIdentifier() {
            return "test";
        }

        @Override
        public Class<TestAggregate> getAggregateType() {
            return TestAggregate.class;
        }
    }

    private static class TestAggregate extends AbstractEventSourcedAggregateRoot {

        private List<EventMessage> handledEvents = new ArrayList<EventMessage>();
        private UUID identifier;

        private TestAggregate(UUID identifier) {
            this.identifier = identifier;
        }

        @Override
        protected void apply(Object eventPayload) {
            super.apply(eventPayload);
        }

        @Override
        protected Collection<EventSourcedEntity> getChildEntities() {
            return null;
        }

        @Override
        protected void handle(DomainEventMessage event) {
            identifier = (UUID) event.getAggregateIdentifier();
            handledEvents.add(event);
        }

        public List<EventMessage> getHandledEvents() {
            return handledEvents;
        }

        @Override
        public UUID getIdentifier() {
            return identifier;
        }
    }

    public static class SpyEventPreprocessor implements EventStreamDecorator {

        private DomainEventStream lastSpy;

        @Override
        public DomainEventStream decorateForRead(final String aggregateType, Object aggregateIdentifier,
                                                 final DomainEventStream eventStream) {
            createSpy(eventStream);
            return lastSpy;
        }

        @Override
        public DomainEventStream decorateForAppend(final String aggregateType, EventSourcedAggregateRoot aggregate,
                                                   DomainEventStream eventStream) {
            createSpy(eventStream);
            return lastSpy;
        }

        private void createSpy(final DomainEventStream eventStream) {
            lastSpy = mock(DomainEventStream.class);
            when(lastSpy.next()).thenAnswer(new Answer<Object>() {
                @Override
                public Object answer(InvocationOnMock invocation) throws Throwable {
                    return eventStream.next();
                }
            });
            when(lastSpy.hasNext()).thenAnswer(new Answer<Object>() {
                @Override
                public Object answer(InvocationOnMock invocation) throws Throwable {
                    return eventStream.hasNext();
                }
            });
            when(lastSpy.peek()).thenAnswer(new Answer<Object>() {
                @Override
                public Object answer(InvocationOnMock invocation) throws Throwable {
                    return eventStream.peek();
                }
            });
        }
    }
}
