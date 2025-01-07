/*
 * Copyright (c) 2010-2014. Axon Framework
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

package org.axonframework.saga.repository.jpa;

import org.axonframework.saga.Saga;
import org.axonframework.serializer.SerializedObject;
import org.axonframework.serializer.Serializer;
import org.axonframework.serializer.SimpleSerializedObject;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Transient;

/**
 * Java Persistence Entity allowing sagas to be stored in a relational database.
 *
 * @author Allard Buijze
 * @since 0.7
 */
@Entity
public class SagaEntry {

    @SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
    @Id
    private String sagaId; // NOSONAR

    @Basic
    private String sagaType;
    @Basic
    private String revision;
    @Lob
    private byte[] serializedSaga;

    @Transient
    private transient Saga saga;

    /**
     * Constructs a new SagaEntry for the given <code>saga</code>. The given saga must be serializable. The provided
     * saga is not modified by this operation.
     *
     * @param saga       The saga to store
     * @param serializer The serialization mechanism to convert the Saga to a byte stream
     */
    public SagaEntry(Saga saga, Serializer serializer) {
        this.sagaId = saga.getSagaIdentifier();
        SerializedObject<byte[]> serialized = serializer.serialize(saga, byte[].class);
        this.serializedSaga = serialized.getData();
        this.sagaType = serialized.getType().getName();
        this.revision = serialized.getType().getRevision();
        this.saga = saga;
    }

    /**
     * Returns the Saga instance stored in this entry.
     *
     * @param serializer The serializer to decode the Saga
     * @return the Saga instance stored in this entry
     */
    public Saga getSaga(Serializer serializer) {
        if (saga != null) {
            return saga;
        }
        return (Saga) serializer.deserialize(new SimpleSerializedObject<byte[]>(serializedSaga, byte[].class,
                                                                                sagaType, revision));
    }

    /**
     * Constructor required by JPA. Do not use.
     *
     * @see #SagaEntry(org.axonframework.saga.Saga, org.axonframework.serializer.Serializer)
     */
    protected SagaEntry() {
        // required by JPA
    }

    /**
     * Returns the serialized form of the Saga.
     *
     * @return the serialized form of the Saga
     */
    public byte[] getSerializedSaga() {
        return serializedSaga; //NOSONAR
    }

    /**
     * Returns the identifier of the saga contained in this entry
     *
     * @return the identifier of the saga contained in this entry
     */
    public String getSagaId() {
        return sagaId;
    }

    /**
     * Returns the revision of the serialized saga
     *
     * @return the revision of the serialized saga
     */
    public String getRevision() {
        return revision;
    }

    /**
     * Returns the type identifier of the serialized saga
     * @return the type identifier of the serialized saga
     */
    public String getSagaType() {
        return sagaType;
    }
}
