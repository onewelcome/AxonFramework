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

package org.axonframework.eventhandling.amqp.spring;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.axonframework.domain.EventMessage;
import org.axonframework.domain.GenericEventMessage;
import org.axonframework.eventhandling.Cluster;
import org.axonframework.eventhandling.amqp.DefaultAMQPMessageConverter;
import org.axonframework.eventhandling.io.EventMessageWriter;
import org.axonframework.serializer.Serializer;
import org.axonframework.serializer.xml.XStreamSerializer;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.charset.Charset;

/**
 * @author Allard Buijze
 */
public class ClusterMessageListenerTest {

    @Test
    public void testMessageListenerInvokesAllClusters() throws Exception {
        Serializer serializer = new XStreamSerializer();
        Cluster cluster = mock(Cluster.class);
        ClusterMessageListener testSubject = new ClusterMessageListener(cluster,
                                                                        new DefaultAMQPMessageConverter(serializer));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        EventMessageWriter outputStream = new EventMessageWriter(new DataOutputStream(baos), serializer);
        outputStream.writeEventMessage(new GenericEventMessage<String>("Event"));
        testSubject.onMessage(new Message(baos.toByteArray(), new MessageProperties()));

        verify(cluster).publish(argThat(new ArgumentMatcher<EventMessage>() {
            @Override
            public boolean matches(EventMessage item) {
                return "Event".equals(item.getPayload());
            }

            @Override
            public String toString() {
                return "EventMessage with String payload";
            }
        }));
    }

    @Test
    public void testMessageListenerIgnoredOnDeserializationFailure() throws Exception {
        Serializer serializer = new XStreamSerializer();
        Cluster cluster = mock(Cluster.class);
        ClusterMessageListener testSubject = new ClusterMessageListener(cluster,
                                                                        new DefaultAMQPMessageConverter(serializer));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        EventMessageWriter outputStream = new EventMessageWriter(new DataOutputStream(baos), serializer);
        outputStream.writeEventMessage(new GenericEventMessage<String>("Event"));
        byte[] body = baos.toByteArray();
        body = new String(body, Charset.forName("UTF-8")).replace("string", "strong")
                                                         .getBytes(Charset.forName("UTF-8"));
        testSubject.onMessage(new Message(body, new MessageProperties()));

        verify(cluster, never()).publish(any(EventMessage.class));
    }
}
