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

package org.axonframework.integration.adapter;

import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.axonframework.domain.EventMessage;
import org.axonframework.domain.GenericEventMessage;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.integration.StubDomainEvent;
import org.junit.Before;
import org.junit.Test;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;

/**
 * @author Allard Buijze
 */
public class EventListeningMessageChannelAdapterTest {

    private EventListeningMessageChannelAdapter testSubject;
    private EventBus mockEventBus;
    private MessageChannel mockChannel;
    private EventFilter mockFilter;

    @Before
    public void setUp() {
        mockEventBus = mock(EventBus.class);
        mockChannel = mock(MessageChannel.class);
        mockFilter = mock(EventFilter.class);
        testSubject = new EventListeningMessageChannelAdapter(mockEventBus, mockChannel);
    }

    @Test
    public void testMessageForwardedToChannel() {
        StubDomainEvent event = new StubDomainEvent();
        testSubject.handle(new GenericEventMessage<StubDomainEvent>(event));

        verify(mockChannel).send(messageWithPayload(event));
    }

    @Test
    public void testEventListenerRegisteredOnInit() throws Exception {
        verify(mockEventBus, never()).subscribe(testSubject);
        testSubject.afterPropertiesSet();
        verify(mockEventBus).subscribe(testSubject);
    }

    @SuppressWarnings({"unchecked"})
    @Test
    public void testFilterBlocksEvents() throws Exception {
        when(mockFilter.accept(isA(Class.class))).thenReturn(false);
        testSubject = new EventListeningMessageChannelAdapter(mockEventBus, mockChannel, mockFilter);
        testSubject.handle(newDomainEvent());
        verify(mockEventBus, never()).publish(isA(EventMessage.class));
    }

    private EventMessage<String> newDomainEvent() {
        return new GenericEventMessage<String>("Mock");
    }

    private Message<?> messageWithPayload(final StubDomainEvent event) {
        return argThat(message -> event.equals(message.getPayload()));
    }
}
