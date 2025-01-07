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

package org.axonframework.integration.eventbus;

import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.axonframework.domain.EventMessage;
import org.axonframework.eventhandling.EventListener;
import org.axonframework.integration.StubDomainEvent;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.springframework.integration.message.GenericMessage;

/**
 * @author Allard Buijze
 */
public class MessageHandlerAdapterTest {

    @SuppressWarnings({"unchecked"})
    @Test
    public void testMessageForwarded() {
        EventListener mockEventListener = mock(EventListener.class);
        MessageHandlerAdapter adapter = new MessageHandlerAdapter(mockEventListener);

        final StubDomainEvent payload = new StubDomainEvent();
        adapter.handleMessage(new GenericMessage<StubDomainEvent>(payload));
        adapter.handleMessage(new GenericMessage<StubDomainEvent>(new StubDomainEvent()));

        verify(mockEventListener, times(1)).handle(argThat(new ArgumentMatcher<EventMessage<?>>() {
            @Override
            public boolean matches(EventMessage<?> o) {
                return (o.getPayload().equals(payload));
            }

            @Override
            public String toString() {
                return "Event with correct payload";
            }
        }));
    }
}
