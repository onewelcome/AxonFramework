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

package org.axonframework.integrationtests.domain;

import static junit.framework.Assert.assertEquals;

import org.axonframework.serializer.SerializedObject;
import org.axonframework.serializer.xml.XStreamSerializer;
import org.axonframework.testutils.XStreamSerializerFactory;
import org.junit.Test;

import java.io.UnsupportedEncodingException;

/**
 * Test that reproduces a problem where a structured aggregate (containing multiple entities) is not serialized
 * properly.
 *
 * @author Allard Buijze
 */
public class StructuredAggregateSerializationTest {

    @Test
    public void testSerializeAndDeserializeAggregate() throws UnsupportedEncodingException {
        StructuredAggregateRoot aggregateRoot = new StructuredAggregateRoot();
        aggregateRoot.invoke();
        assertEquals(2, aggregateRoot.getInvocations());
        assertEquals(2, aggregateRoot.getEntity().getInvocations());
        aggregateRoot.commitEvents();
        XStreamSerializer serializer = XStreamSerializerFactory.create(StructuredAggregateRoot.class);
        SerializedObject<byte[]> serialized = serializer.serialize(aggregateRoot, byte[].class);
        StructuredAggregateRoot deserializedAggregate = (StructuredAggregateRoot) serializer.deserialize(serialized);

        deserializedAggregate.invoke();
        assertEquals(3, deserializedAggregate.getInvocations());
        assertEquals(3, deserializedAggregate.getEntity().getInvocations());
    }
}
