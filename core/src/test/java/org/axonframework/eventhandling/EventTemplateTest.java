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

package org.axonframework.eventhandling;

import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.axonframework.correlation.CorrelationDataHolder;
import org.axonframework.domain.EventMessage;
import org.axonframework.domain.GenericEventMessage;
import org.axonframework.unitofwork.CurrentUnitOfWork;
import org.axonframework.unitofwork.UnitOfWork;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Allard Buijze
 */
public class EventTemplateTest {

    private UnitOfWork mockUnitOfWork;
    private EventBus mockEventBus;
    private EventTemplate testSubject;
    private Object payload;

    @Before
    public void setUp() throws Exception {
        CorrelationDataHolder.clear();
        mockUnitOfWork = mock(UnitOfWork.class);
        mockEventBus = mock(EventBus.class);
        testSubject = new EventTemplate(mockEventBus, Collections.singletonMap("key1", "value1"));
        payload = new Object();
    }

    @After
    public void tearDown() throws Exception {
        CorrelationDataHolder.clear();
        while (CurrentUnitOfWork.isStarted()) {
            CurrentUnitOfWork.clear(CurrentUnitOfWork.get());
        }
    }

    @Test
    public void testUnitOfWorkUsedToSendEvent() throws Exception {
        CurrentUnitOfWork.set(mockUnitOfWork);

        testSubject.publishEvent(payload);

        verifyNoInteractions(mockEventBus);
        verify(mockUnitOfWork).publishEvent(argThat(new ArgumentMatcher<>() {
            @Override
            public boolean matches(EventMessage<?> eventMessage) {
                return "value1".equals(eventMessage.getMetaData().get("key1"))
                        && eventMessage.getPayload().equals(payload);
            }

            @Override
            public String toString() {
                return "an event message with a 'key1' meta data property";
            }
        }), eq(mockEventBus));
    }

    @Test
    public void testMessagesHaveCorrelationDataAttached() {
        CorrelationDataHolder.setCorrelationData(Collections.singletonMap("correlationId", "testing"));
        testSubject.publishEvent(payload, Collections.singletonMap("scope", "test"));

        verify(mockEventBus).publish(argThat(new ArgumentMatcher<EventMessage<?>>() {
            @Override
            public boolean matches(EventMessage<?> eventMessage) {
                return "testing".equals(eventMessage.getMetaData().get("correlationId"))
                        && eventMessage.getMetaData().containsKey("scope")
                        && eventMessage.getPayload().equals(payload);
            }

            @Override
            public String toString() {
                return "an event message with a 'correlationId' and 'scope' meta data property";
            }
        }));
        verifyNoMoreInteractions(mockUnitOfWork, mockEventBus);
    }

    @Test
    public void testMessagesUseExplicitlyProvidedHeadersWhenConflictingWithCorrelationHeaders() {
        CorrelationDataHolder.setCorrelationData(Collections.singletonMap("correlationId", "testing"));
        testSubject.publishEvent(payload, Collections.singletonMap("correlationId", "overridden"));

        verify(mockEventBus).publish(argThat(new ArgumentMatcher<EventMessage<?>>() {
            @Override
            public boolean matches(EventMessage<?> eventMessage) {
                return "overridden".equals(eventMessage.getMetaData().get("correlationId"))
                        && eventMessage.getPayload().equals(payload);
            }

            @Override
            public String toString() {
                return "an event message with a meta data property 'correlationId' of value 'overridden'";
            }
        }));
        verifyNoMoreInteractions(mockUnitOfWork, mockEventBus);
    }

    @Test
    public void testMessagesUseExplicitlyProvidedHeadersInMessageWhenConflictingWithCorrelationHeaders() {
        CorrelationDataHolder.setCorrelationData(Collections.singletonMap("correlationId", "testing"));
        testSubject.publishEvent(GenericEventMessage.asEventMessage(payload)
                                                    .withMetaData(Collections.singletonMap("correlationId",
                                                                                           "overridden")));

        verify(mockEventBus).publish(argThat(new ArgumentMatcher<EventMessage<?>>() {
            @Override
            public boolean matches(EventMessage<?> eventMessage) {
                return "overridden".equals(eventMessage.getMetaData().get("correlationId"))
                        && eventMessage.getPayload().equals(payload);
            }

            @Override
            public String toString() {
                return "an event message with a meta data property 'correlationId' of value 'overridden'";
            }
        }));
        verifyNoMoreInteractions(mockUnitOfWork, mockEventBus);
    }

    @Test
    public void testUnitOfWorkUsedToSendEvent_OverlappingMetaData() throws Exception {
        CurrentUnitOfWork.set(mockUnitOfWork);

        Map<String, Object> moreMetaData = new HashMap<String, Object>();
        moreMetaData.put("key1", "value2");
        moreMetaData.put("key2", "value1");
        testSubject.publishEvent(payload, moreMetaData);

        verifyNoInteractions(mockEventBus);
        verify(mockUnitOfWork).publishEvent(argThat(new ArgumentMatcher<>() {
            @Override
            public boolean matches(EventMessage<?> eventMessage) {
                return "value2".equals(eventMessage.getMetaData().get("key1"))
                        && eventMessage.getMetaData().containsKey("key2")
                        && eventMessage.getPayload().equals(payload);
            }

            @Override
            public String toString() {
                return "an event message with a 'key1' and 'key2' meta data property";
            }
        }), eq(mockEventBus));
    }

    @Test
    public void testEventSentImmediatelyWhenNoActiveUnitOfWorkExists_OverlappingMetaData() throws Exception {

        Map<String, Object> moreMetaData = new HashMap<String, Object>();
        moreMetaData.put("key1", "value2");
        moreMetaData.put("key2", "value1");
        testSubject.publishEvent(payload, moreMetaData);

        verifyNoInteractions(mockUnitOfWork);
        verify(mockEventBus).publish(argThat(new ArgumentMatcher<EventMessage<?>>() {
            @Override
            public boolean matches(EventMessage<?> eventMessage) {
                return "value2".equals(eventMessage.getMetaData().get("key1"))
                        && eventMessage.getMetaData().containsKey("key2")
                        && eventMessage.getPayload().equals(payload);
            }

            @Override
            public String toString() {
                return "an event message with a 'key1' and 'key2' meta data property";
            }
        }));
    }

    @Test
    public void testEventSentImmediatelyWhenNoActiveUnitOfWorkExists() throws Exception {
        testSubject.publishEvent(payload);

        verifyNoInteractions(mockUnitOfWork);
        verify(mockEventBus).publish(argThat(new ArgumentMatcher<EventMessage<?>>() {
            @Override
            public boolean matches(EventMessage<?> eventMessage) {
                return "value1".equals(eventMessage.getMetaData().get("key1"))
                        && eventMessage.getPayload().equals(payload);
            }

            @Override
            public String toString() {
                return "an event message with a 'key1' meta data property";
            }
        }));
    }

    @Test
    public void testEventSentImmediatelyWhenNoActiveUnitOfWorkExists_NoAdditionalMetaData() throws Exception {
        testSubject = new EventTemplate(mockEventBus);
        testSubject.publishEvent(payload);

        verifyNoInteractions(mockUnitOfWork);
        verify(mockEventBus).publish(argThat(new ArgumentMatcher<EventMessage<?>>() {
            @Override
            public boolean matches(EventMessage<?> eventMessage) {
                return eventMessage.getMetaData().isEmpty()
                        && eventMessage.getPayload().equals(payload);
            }

            @Override
            public String toString() {
                return "an event message with a 'key1' meta data property";
            }
        }));
    }

    @Test
    public void testActiveUnitOfWorkUsedToDispatchEvent_NoAdditionalMetaData() throws Exception {
        CurrentUnitOfWork.set(mockUnitOfWork);

        testSubject = new EventTemplate(mockEventBus);
        testSubject.publishEvent(payload);

        verifyNoInteractions(mockEventBus);
        verify(mockUnitOfWork).publishEvent(argThat(new ArgumentMatcher<>() {
            @Override
            public boolean matches(EventMessage<?> eventMessage) {
                return eventMessage.getMetaData().isEmpty()
                        && eventMessage.getPayload().equals(payload);
            }

            @Override
            public String toString() {
                return "an event message with a 'key1' meta data property";
            }
        }), eq(mockEventBus));
    }
}
