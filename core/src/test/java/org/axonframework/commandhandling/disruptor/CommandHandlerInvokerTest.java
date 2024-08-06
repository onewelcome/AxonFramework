package org.axonframework.commandhandling.disruptor;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.axonframework.cache.Cache;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.commandhandling.CommandHandlerInterceptor;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.domain.DomainEventStream;
import org.axonframework.domain.GenericDomainEventMessage;
import org.axonframework.domain.SimpleDomainEventStream;
import org.axonframework.eventsourcing.EventSourcedAggregateRoot;
import org.axonframework.eventsourcing.EventStreamDecorator;
import org.axonframework.eventsourcing.GenericAggregateFactory;
import org.axonframework.eventsourcing.annotation.AbstractAnnotatedAggregateRoot;
import org.axonframework.eventsourcing.annotation.AggregateIdentifier;
import org.axonframework.eventsourcing.annotation.EventSourcingHandler;
import org.axonframework.eventstore.EventStore;
import org.axonframework.repository.Repository;
import org.axonframework.unitofwork.UnitOfWork;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.stubbing.answers.ReturnsArgumentAt;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Collections;

/**
 *
 */
public class CommandHandlerInvokerTest {

    private CommandHandlerInvoker testSubject;
    private EventStore mockEventStore;
    private Cache mockCache;
    private CommandHandlingEntry commandHandlingEntry;
    private String aggregateIdentifier;
    private CommandMessage<?> mockCommandMessage;
    private CommandHandler mockCommandHandler;
    private EventStreamDecorator eventStreamDecorator;

    @Before
    public void setUp() throws Exception {
        mockEventStore = mock(EventStore.class);
        mockCache = mock(Cache.class);
        testSubject = new CommandHandlerInvoker(mockEventStore, mockCache, 0);
        aggregateIdentifier = "mockAggregate";
        mockCommandMessage = mock(CommandMessage.class);
        mockCommandHandler = mock(CommandHandler.class);
        commandHandlingEntry = new CommandHandlingEntry(false);
        commandHandlingEntry.reset(mockCommandMessage, mockCommandHandler, 0, 0, 0, null,
                                   Collections.<CommandHandlerInterceptor>emptyList(),
                                   Collections.<CommandHandlerInterceptor>emptyList());
        eventStreamDecorator = mock(EventStreamDecorator.class);
        when(eventStreamDecorator.decorateForAppend(anyString(), any(EventSourcedAggregateRoot.class), any(DomainEventStream.class))).thenAnswer(new ReturnsArgumentAt(2));
        when(eventStreamDecorator.decorateForRead(anyString(), any(), any(DomainEventStream.class))).thenAnswer(
                new ReturnsArgumentAt(2));
    }

    @Test
    public void testLoadFromRepositoryStoresLoadedAggregateInCache() throws Throwable {
        final Repository<StubAggregate> repository = testSubject.createRepository(
                new GenericAggregateFactory<StubAggregate>(StubAggregate.class), eventStreamDecorator);
        when(mockCommandHandler.handle(eq(mockCommandMessage), isA(UnitOfWork.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return repository.load(aggregateIdentifier);
            }
        });
        when(mockEventStore.readEvents(anyString(), any()))
                .thenReturn(new SimpleDomainEventStream(
                        new GenericDomainEventMessage(aggregateIdentifier, 0, aggregateIdentifier)));
        testSubject.onEvent(commandHandlingEntry, 0, true);

        verify(mockCache).get(aggregateIdentifier);
        verify(mockCache).put(eq(aggregateIdentifier), isA(StubAggregate.class));
        verify(mockEventStore).readEvents(anyString(), eq(aggregateIdentifier));
    }

    @Test
    public void testLoadFromRepositoryLoadsFromCache() throws Throwable {
        final Repository<StubAggregate> repository = testSubject.createRepository(
                new GenericAggregateFactory<StubAggregate>(StubAggregate.class), eventStreamDecorator);
        when(mockCommandHandler.handle(eq(mockCommandMessage), isA(UnitOfWork.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return repository.load(aggregateIdentifier);
            }
        });
        when(mockCache.get(aggregateIdentifier)).thenReturn(new StubAggregate(aggregateIdentifier));
        testSubject.onEvent(commandHandlingEntry, 0, true);

        verify(mockCache).get(aggregateIdentifier);
        verify(mockEventStore, never()).readEvents(anyString(), eq(aggregateIdentifier));
    }

    @Test
    public void testAddToRepositoryAddsInCache() throws Throwable {
        final Repository<StubAggregate> repository = testSubject.createRepository(
                new GenericAggregateFactory<StubAggregate>(StubAggregate.class), eventStreamDecorator);
        when(mockCommandHandler.handle(eq(mockCommandMessage), isA(UnitOfWork.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                StubAggregate aggregate = new StubAggregate(aggregateIdentifier);
                aggregate.doSomething();
                repository.add(aggregate);
                return aggregate;
            }
        });

        testSubject.onEvent(commandHandlingEntry, 0, true);

        verify(mockCache).put(eq(aggregateIdentifier), isA(StubAggregate.class));
        verify(mockEventStore, never()).readEvents(anyString(), eq(aggregateIdentifier));
        verify(mockEventStore, never()).appendEvents(anyString(), any(DomainEventStream.class));
        assertTrue(commandHandlingEntry.getUnitOfWork().getAggregate() instanceof StubAggregate);
    }

    @Test
    public void testCacheEntryInvalidatedOnRecoveryEntry() throws Exception {
        commandHandlingEntry.resetAsRecoverEntry(aggregateIdentifier);
        testSubject.onEvent(commandHandlingEntry, 0, true);

        verify(mockCache).remove(aggregateIdentifier);
        verify(mockEventStore, never()).readEvents(anyString(), eq(aggregateIdentifier));
    }

    @Test
    public void testCreateRepositoryReturnsSameInstanceOnSecondInvocation() {
        final Repository<StubAggregate> repository1 = testSubject.createRepository(
                new GenericAggregateFactory<StubAggregate>(StubAggregate.class), eventStreamDecorator);
        final Repository<StubAggregate> repository2= testSubject.createRepository(
                new GenericAggregateFactory<StubAggregate>(StubAggregate.class), eventStreamDecorator);

        assertSame(repository1, repository2);
    }

    public static class StubAggregate extends AbstractAnnotatedAggregateRoot<String> {

        @AggregateIdentifier
        private String id;

        public StubAggregate() {
        }

        public StubAggregate(String id) {
            this.id = id;
        }

        public void doSomething() {
            apply(id);
        }

        @EventSourcingHandler
        public void handle(String id) {
            this.id = id;
        }

    }

}
