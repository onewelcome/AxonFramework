/*
 * Copyright (c) 2010-2023. Axon Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.axonframework.eventhandling;

import org.axonframework.common.transaction.NoTransactionManager;
import org.axonframework.eventhandling.async.FullConcurrencyPolicy;
import org.axonframework.eventhandling.gateway.DefaultEventGateway;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.axonframework.eventhandling.tokenstore.jdbc.JdbcTokenStore;
import org.axonframework.eventhandling.tokenstore.jdbc.PostgresTokenTableFactory;
import org.axonframework.eventsourcing.eventstore.EmbeddedEventStore;
import org.axonframework.eventsourcing.eventstore.EventStoreException;
import org.axonframework.eventsourcing.eventstore.jdbc.JdbcEventStorageEngine;
import org.axonframework.eventsourcing.eventstore.jdbc.PostgresEventTableFactory;
import org.axonframework.serialization.TestSerializer;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

class TestConcurrentTrackingEventProcessor {

    private static final Logger logger = LoggerFactory.getLogger(Test.class);
    private static final Duration MAX_WAIT_TIME = Duration.ofSeconds(30);
    private static final int NUMBER_OF_THREADS = 10;

    private final ExecutorService connectionProvider = Executors.newSingleThreadExecutor();

    @RepeatedTest(100)
    void test() {
        try (PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:12")) {
            postgreSQLContainer.start();
            final JdbcEventStorageEngine eventStorageEngine = JdbcEventStorageEngine
                    .builder()
                    .connectionProvider(() -> failFastConnection(postgreSQLContainer))
                    .transactionManager(NoTransactionManager.instance())
                    .snapshotSerializer(TestSerializer.XSTREAM.getSerializer())
                    .eventSerializer(TestSerializer.XSTREAM.getSerializer())
                    .build();
            eventStorageEngine.createSchema(PostgresEventTableFactory.INSTANCE);
            final EmbeddedEventStore eventStore = EmbeddedEventStore
                    .builder()
                    .storageEngine(eventStorageEngine)
                    .build();
            final JdbcTokenStore tokenStore = JdbcTokenStore
                    .builder()
                    .connectionProvider(() -> failFastConnection(postgreSQLContainer))
                    .serializer(TestSerializer.XSTREAM.getSerializer())
                    .claimTimeout(Duration.ofSeconds(20))
                    .build();
            tokenStore.createSchema(PostgresTokenTableFactory.INSTANCE);
            final TrackingEventProcessor trackingEventProcessor = TrackingEventProcessor
                    .builder()
                    .name("tracking")
                    .eventHandlerInvoker(SimpleEventHandlerInvoker
                                                 .builder()
                                                 .eventHandlers(new Object())
                                                 .sequencingPolicy(new FullConcurrencyPolicy())
                                                 .build())
                    .messageSource(eventStore)
                    .tokenStore(tokenStore)
                    .transactionManager(NoTransactionManager.instance())
                    .trackingEventProcessorConfiguration(TrackingEventProcessorConfiguration
                                                                 .forParallelProcessing(NUMBER_OF_THREADS)
                                                                 .andEventAvailabilityTimeout(1, TimeUnit.SECONDS)
                                                                 .andWorkerTerminationTimeout(10 + 1, TimeUnit.SECONDS))
                    .build();

            trackingEventProcessor.start();
            assertThatAllThreadsExist();

            final EventGateway eventGateway = DefaultEventGateway
                    .builder()
                    .eventBus(eventStore)
                    .build();
            eventGateway.publish(new Object());

            trackingEventProcessor.shutDown();
            assertThatNoThreadsExist();
            trackingEventProcessor.resetTokens();
            trackingEventProcessor.start();

            assertThatAllThreadsExist();
            trackingEventProcessor.shutDown();
            assertThatNoThreadsExist();
        }
    }

    private void assertThatNoThreadsExist() {
        await()
                .pollInterval(Duration.ofSeconds(1L))
                .atMost(MAX_WAIT_TIME)
                .untilAsserted(() -> {
                    final List<String> relevantThreadNames = getRelevantThreadNames();
                    logger.warn("List of current threads: {}", getAllThreadNames());
                    Assertions.assertTrue(relevantThreadNames.isEmpty());
                });
    }

    private void assertThatAllThreadsExist() {
        await()
                .pollInterval(Duration.ofSeconds(1L))
                .atMost(MAX_WAIT_TIME)
                .untilAsserted(() -> {
                    final List<String> relevantThreadNames = getRelevantThreadNames();
                    logger.info("List of current threads: {}", getAllThreadNames());
                    Assertions.assertEquals(NUMBER_OF_THREADS, relevantThreadNames.size());
                });
    }

    private List<String> getRelevantThreadNames() {
        return Thread.getAllStackTraces()
                     .keySet()
                     .stream()
                     .map(Thread::getName)
                     .filter(name -> name.startsWith("EventProcessor[tracking]"))
                     .toList();
    }

    private List<String> getAllThreadNames() {
        return Thread.getAllStackTraces()
                     .keySet()
                     .stream()
                     .map(Thread::getName)
                     .toList();
    }

    private Connection failFastConnection(PostgreSQLContainer<?> postgreSQLContainer) {
        Future<Connection> future = connectionProvider.submit(() -> postgreSQLContainer.createConnection(""));
        try {
            return future.get(5L, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new EventStoreException("Could not get connection", e);
        }
    }
}
