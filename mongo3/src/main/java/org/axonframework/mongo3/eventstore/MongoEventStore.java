/*
 * Copyright (c) 2010-2016. Axon Framework
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

package org.axonframework.mongo3.eventstore;

import com.mongodb.MongoBulkWriteException;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCursor;
import org.axonframework.domain.DomainEventMessage;
import org.axonframework.domain.DomainEventStream;
import org.axonframework.eventstore.EventStreamNotFoundException;
import org.axonframework.eventstore.EventVisitor;
import org.axonframework.eventstore.PartialStreamSupport;
import org.axonframework.eventstore.SnapshotEventStore;
import org.axonframework.eventstore.management.Criteria;
import org.axonframework.eventstore.management.EventStoreManagement;
import org.axonframework.mongo3.eventstore.criteria.MongoCriteria;
import org.axonframework.mongo3.eventstore.criteria.MongoCriteriaBuilder;
import org.axonframework.repository.ConcurrencyException;
import org.axonframework.serializer.Serializer;
import org.axonframework.serializer.xml.XStreamSerializer;
import org.axonframework.upcasting.SimpleUpcasterChain;
import org.axonframework.upcasting.UpcasterAware;
import org.axonframework.upcasting.UpcasterChain;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * <p>Implementation of the <code>EventStore</code> based on a MongoDB instance or replica set. Sharding and pairing
 * are not explicitly supported.</p> <p>This event store implementation needs a serializer as well as a {@link
 * MongoTemplate} to interact with the mongo database.</p> <p><strong>Warning:</strong> This implementation is
 * still in progress and may be subject to alterations. The implementation works, but has not been optimized to fully
 * leverage MongoDB's features, yet.</p>
 *
 * @author Jettro Coenradie
 * @since 2.0 (in incubator since 0.7)
 */
public class MongoEventStore implements SnapshotEventStore, EventStoreManagement, UpcasterAware, PartialStreamSupport {

    private static final Logger logger = LoggerFactory.getLogger(MongoEventStore.class);

    private final MongoTemplate mongoTemplate;

    private final Serializer eventSerializer;
    private final StorageStrategy storageStrategy;
    private UpcasterChain upcasterChain = SimpleUpcasterChain.EMPTY;

    /**
     * Constructor that accepts a Serializer and the MongoTemplate. A Document-Per-Event storage strategy is used,
     * causing each event to be stored in a separate Mongo Document.
     * <p/>
     * <em>Note: the SerializedType of Message Meta Data is not stored. Upon retrieval, it is set to the default value
     * (name = "org.axonframework.domain.MetaData", revision = null). See {@link org.axonframework.serializer.SerializedMetaData#isSerializedMetaData(org.axonframework.serializer.SerializedObject)}</em>
     *
     * @param eventSerializer Your own Serializer
     * @param mongo           Mongo instance to obtain the database and the collections.
     */
    public MongoEventStore(Serializer eventSerializer, MongoTemplate mongo) {
        this(mongo, eventSerializer, new DocumentPerEventStorageStrategy());
    }

    /**
     * Constructor that uses the default Serializer. A Document-Per-Event storage strategy is used, causing each event
     * to be stored in a separate Mongo Document.
     *
     * @param mongo MongoTemplate instance to obtain the database and the collections.
     */
    public MongoEventStore(MongoTemplate mongo) {
        this(new XStreamSerializer(), mongo);
    }

    /**
     * Constructor that accepts a MongoTemplate and a custom StorageStrategy.
     *
     * @param mongoTemplate   The template giving access to the required collections
     * @param storageStrategy The strategy for storing and retrieving events from the collections
     */
    public MongoEventStore(MongoTemplate mongoTemplate, StorageStrategy storageStrategy) {
        this(mongoTemplate, new XStreamSerializer(), storageStrategy);
    }

    /**
     * Initialize the mongo event store with given <code>mongoTemplate</code>, <code>eventSerializer</code> and
     * <code>storageStrategy</code>.
     *
     * @param mongoTemplate   The template giving access to the required collections
     * @param eventSerializer The serializer to serialize events with
     * @param storageStrategy The strategy for storing and retrieving events from the collections
     */
    public MongoEventStore(MongoTemplate mongoTemplate, Serializer eventSerializer, StorageStrategy storageStrategy) {
        this.eventSerializer = eventSerializer;
        this.mongoTemplate = mongoTemplate;
        this.storageStrategy = storageStrategy;
    }

    /**
     * Make sure an index is created on the collection that stores domain events.
     */
    @PostConstruct
    public void ensureIndexes() {
        storageStrategy.ensureIndexes(mongoTemplate.domainEventCollection(), mongoTemplate.snapshotEventCollection());
    }

    @Override
    public void appendEvents(String type, DomainEventStream events) {
        if (!events.hasNext()) {
            return;
        }

        List<DomainEventMessage> messages = new ArrayList<DomainEventMessage>();
        while (events.hasNext()) {
            messages.add(events.next());
        }

        try {
            mongoTemplate.domainEventCollection().insertMany(storageStrategy.createDocuments(type,
                                                                                         eventSerializer,
                                                                                         messages));
        } catch (MongoBulkWriteException e) {
            throw new ConcurrencyException("Trying to insert an Event for an aggregate with a sequence "
                                                   + "number that is already present in the Event Store", e);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("{} events appended", new Object[]{messages.size()});
        }
    }

    @Override
    public DomainEventStream readEvents(String type, Object identifier) {
        long snapshotSequenceNumber = -1;
        List<DomainEventMessage> lastSnapshotCommit = loadLastSnapshotEvent(type, identifier);
        if (lastSnapshotCommit != null && !lastSnapshotCommit.isEmpty()) {
            snapshotSequenceNumber = lastSnapshotCommit.get(0).getSequenceNumber();
        }
        final MongoCursor<Document> dbCursor = storageStrategy.findEvents(mongoTemplate.domainEventCollection(),
                                                                 type,
                                                                 identifier.toString(),
                                                                 snapshotSequenceNumber + 1);

        DomainEventStream stream = new CursorBackedDomainEventStream(dbCursor, lastSnapshotCommit, identifier, false);
        if (!stream.hasNext()) {
            throw new EventStreamNotFoundException(type, identifier);
        }
        return stream;
    }

    @Override
    public DomainEventStream readEvents(String type, Object identifier, long firstSequenceNumber) {
        return readEvents(type, identifier, firstSequenceNumber, Long.MAX_VALUE);
    }

    @Override
    public DomainEventStream readEvents(String type, Object identifier, long firstSequenceNumber,
                                        long lastSequenceNumber) {
        final MongoCursor<Document> dbCursor = storageStrategy.findEvents(mongoTemplate.domainEventCollection(),
                                                             type,
                                                             identifier.toString(),
                                                             firstSequenceNumber);

        DomainEventStream stream = new CursorBackedDomainEventStream(dbCursor, null, identifier, lastSequenceNumber,
                                                                     false);
        if (!stream.hasNext()) {
            throw new EventStreamNotFoundException(type, identifier);
        }
        return stream;
    }

    @Override
    public void appendSnapshotEvent(String type, DomainEventMessage snapshotEvent) {
        final Document document = storageStrategy.createDocuments(type, eventSerializer,
                                                              Collections.singletonList(snapshotEvent)).get(0);
        try {
            mongoTemplate.snapshotEventCollection().insertOne(document);
        } catch (MongoWriteException e) {
            throw new ConcurrencyException("Trying to insert a SnapshotEvent with aggregate identifier and sequence "
                                                   + "number that is already present in the Event Store", e);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("snapshot event of type {} appended.");
        }
    }

    @Override
    public void visitEvents(EventVisitor visitor) {
        visitEvents(null, visitor);
    }

    @Override
    public void visitEvents(Criteria criteria, EventVisitor visitor) {
        MongoCursor<Document> cursor = storageStrategy.findEvents(mongoTemplate.domainEventCollection(),
                                                     (MongoCriteria) criteria);

//        cursor.noCursorTimeout(true);
        CursorBackedDomainEventStream events = new CursorBackedDomainEventStream(cursor, null, null, true);
        try {
            while (events.hasNext()) {
                visitor.doWithEvent(events.next());
            }
        } finally {
            events.close();
        }
    }

    @Override
    public MongoCriteriaBuilder newCriteriaBuilder() {
        return new MongoCriteriaBuilder();
    }

    private List<DomainEventMessage> loadLastSnapshotEvent(String type, Object identifier) {
        MongoCursor<Document> dbCursor = storageStrategy.findLastSnapshot(mongoTemplate.snapshotEventCollection(),
                                                                          type,
                                                                          identifier.toString());
        if (!dbCursor.hasNext()) {
            return null;
        }

        return storageStrategy.extractEventMessages(dbCursor.next(), identifier, eventSerializer, upcasterChain, false);
    }

    @Override
    public void setUpcasterChain(UpcasterChain upcasterChain) {
        this.upcasterChain = upcasterChain;
    }

    private class CursorBackedDomainEventStream implements DomainEventStream, Closeable {

        private Iterator<DomainEventMessage> messagesToReturn = Collections.<DomainEventMessage>emptyList().iterator();
        private DomainEventMessage next;
        private final MongoCursor<Document> dbCursor;
        private final Object actualAggregateIdentifier;
        private final long lastSequenceNumber;
        private boolean skipUnknownTypes;

        /**
         * Initializes the DomainEventStream, streaming events obtained from the given <code>dbCursor</code> and
         * optionally the given <code>lastSnapshotEvent</code>.
         *
         * @param dbCursor                  The cursor providing access to the query results in the Mongo instance
         * @param lastSnapshotCommit        The last snapshot event read, or <code>null</code> if no snapshot is
         *                                  available
         * @param actualAggregateIdentifier The actual aggregateIdentifier instance used to perform the lookup, or
         *                                  <code>null</code> if unknown
         * @param skipUnknownTypes          Whether or not the stream should ignore events that cannot be deserialized
         */
        public CursorBackedDomainEventStream(MongoCursor<Document> dbCursor, List<DomainEventMessage> lastSnapshotCommit,
                                             Object actualAggregateIdentifier, boolean skipUnknownTypes) {
            this(dbCursor, lastSnapshotCommit, actualAggregateIdentifier, Long.MAX_VALUE, skipUnknownTypes);
        }

        /**
         * Initializes the DomainEventStream, streaming events obtained from the given <code>dbCursor</code> and
         * optionally the given <code>lastSnapshotEvent</code>, which stops streaming once an event with a sequence
         * number higher given than <code>lastSequenceNumber</code>.
         *
         * @param dbCursor                  The cursor providing access to the query results in the Mongo instance
         * @param lastSnapshotCommit        The last snapshot event read, or <code>null</code> if no snapshot is
         *                                  available
         * @param actualAggregateIdentifier The actual aggregateIdentifier instance used to perform the lookup, or
         *                                  <code>null</code> if unknown
         * @param lastSequenceNumber        The highest sequence number this stream may return before indicating
         *                                  end-of-stream
         * @param skipUnknownTypes          Whether or not the stream should ignore events that cannot be deserialized
         */
        public CursorBackedDomainEventStream(MongoCursor<Document> dbCursor, List<DomainEventMessage> lastSnapshotCommit,
                                             Object actualAggregateIdentifier, long lastSequenceNumber,
                                             boolean skipUnknownTypes) {
            this.dbCursor = dbCursor;
            this.actualAggregateIdentifier = actualAggregateIdentifier;
            this.lastSequenceNumber = lastSequenceNumber;
            this.skipUnknownTypes = skipUnknownTypes;
            if (lastSnapshotCommit != null) {
                messagesToReturn = lastSnapshotCommit.iterator();
            }
            initializeNextItem();
        }

        @Override
        public boolean hasNext() {
            return next != null && next.getSequenceNumber() <= lastSequenceNumber;
        }

        @Override
        public DomainEventMessage next() {
            DomainEventMessage itemToReturn = next;
            initializeNextItem();
            return itemToReturn;
        }

        @Override
        public DomainEventMessage peek() {
            return next;
        }

        /**
         * Ensures that the <code>next</code> points to the correct item, possibly reading from the dbCursor.
         */
        private void initializeNextItem() {
            while (!messagesToReturn.hasNext() && dbCursor.hasNext()) {
                messagesToReturn = storageStrategy.extractEventMessages(dbCursor.next(), actualAggregateIdentifier,
                                                                        eventSerializer, upcasterChain,
                                                                        skipUnknownTypes).iterator();
            }
            next = messagesToReturn.hasNext() ? messagesToReturn.next() : null;
        }

        @Override
        public void close() {
            dbCursor.close();
        }
    }
}
