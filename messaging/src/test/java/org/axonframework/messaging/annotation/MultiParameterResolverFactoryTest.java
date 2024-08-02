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

package org.axonframework.messaging.annotation;

import org.axonframework.common.Priority;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventhandling.GenericEventMessage;
import org.axonframework.messaging.Message;
import org.axonframework.messaging.unitofwork.ProcessingContext;
import org.junit.jupiter.api.*;
import org.mockito.*;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Allard Buijze
 * @author Nakul Mishra
 */
class MultiParameterResolverFactoryTest {

    private ParameterResolverFactory mockFactory1;
    private ParameterResolverFactory mockFactory2;

    private ParameterResolver mockResolver1;
    private ParameterResolver mockResolver2;

    private MultiParameterResolverFactory testSubject;

    @BeforeEach
    void setUp() {
        mockFactory1 = mock(ParameterResolverFactory.class);
        mockFactory2 = mock(ParameterResolverFactory.class);

        mockResolver1 = mock(ParameterResolver.class);
        mockResolver2 = mock(ParameterResolver.class);

        when(mockFactory1.createInstance(ArgumentMatchers.any(Executable.class),
                                         ArgumentMatchers.any(),
                                         ArgumentMatchers.anyInt())).thenReturn(mockResolver1);
        when(mockFactory2.createInstance(ArgumentMatchers.any(Executable.class),
                                         ArgumentMatchers.any(),
                                         ArgumentMatchers.anyInt())).thenReturn(mockResolver2);

        testSubject = new MultiParameterResolverFactory(mockFactory1, mockFactory2);
    }

    @Test
    void resolversQueriedInOrderProvided() throws Exception {
        Method equals = getClass().getMethod("equals", Object.class);
        ParameterResolver factory = testSubject.createInstance(equals, equals.getParameters(), 0);
        assertFalse(factory.matches(null, null));

        InOrder inOrder = inOrder(mockFactory1, mockFactory2, mockResolver1, mockResolver2);
        inOrder.verify(mockFactory1).createInstance(ArgumentMatchers.any(Executable.class),
                                                    ArgumentMatchers.any(),
                                                    ArgumentMatchers.anyInt());
        inOrder.verify(mockResolver1).matches(any(), isNull(ProcessingContext.class));

        verify(mockFactory2, never()).createInstance(ArgumentMatchers.any(Executable.class),
                                                     ArgumentMatchers.any(),
                                                     ArgumentMatchers.anyInt());

        verify(mockResolver2, never()).matches(any(Message.class), isNull(ProcessingContext.class));
    }

    @Test
    void firstMatchingResolverMayReturnValue() throws Exception {
        Method equals = getClass().getMethod("equals", Object.class);
        final EventMessage<Object> message = GenericEventMessage.asEventMessage("test");
        when(mockFactory1.createInstance(ArgumentMatchers.any(Executable.class),
                                         ArgumentMatchers.any(),
                                         ArgumentMatchers.anyInt()))
                .thenReturn(null);
        when(mockResolver2.matches(message, null)).thenReturn(true);
        when(mockResolver2.resolveParameterValue(message, ProcessingContext.NONE)).thenReturn("Resolved");

        ParameterResolver factory = testSubject.createInstance(equals, equals.getParameters(), 0);
        assertTrue(factory.matches(message, null));
        assertEquals("Resolved", factory.resolveParameterValue(message, ProcessingContext.NONE));

        verify(mockResolver1, never()).resolveParameterValue(any(Message.class), eq(ProcessingContext.NONE));
    }

    @Test
    void nestedParameterResolversAreOrdered() {
        final LowPrioParameterResolverFactory lowPrio = new LowPrioParameterResolverFactory();
        final HighPrioParameterResolverFactory highPrio = new HighPrioParameterResolverFactory();
        testSubject = MultiParameterResolverFactory.ordered(mockFactory1,
                                                            new MultiParameterResolverFactory(lowPrio, mockFactory2),
                                                            new MultiParameterResolverFactory(highPrio));

        assertEquals(Arrays.asList(highPrio, mockFactory1, mockFactory2, lowPrio), testSubject.getDelegates());
    }

    @Priority(Priority.LOW)
    private static class LowPrioParameterResolverFactory extends AbstractNoopParameterResolverFactory {

    }

    @Priority(Priority.HIGH)
    private static class HighPrioParameterResolverFactory extends AbstractNoopParameterResolverFactory {

    }

    private static class AbstractNoopParameterResolverFactory implements ParameterResolverFactory {

        @Override
        public ParameterResolver createInstance(Executable executable, Parameter[] parameters, int parameterIndex) {
            return null;
        }
    }


}
