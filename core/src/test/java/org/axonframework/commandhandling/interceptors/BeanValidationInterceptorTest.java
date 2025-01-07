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

package org.axonframework.commandhandling.interceptors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.commandhandling.GenericCommandMessage;
import org.axonframework.commandhandling.InterceptorChain;
import org.axonframework.unitofwork.UnitOfWork;
import org.junit.Before;
import org.junit.Test;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * @author Allard Buijze
 */
public class BeanValidationInterceptorTest {

    private BeanValidationInterceptor testSubject;
    private InterceptorChain mockInterceptorChain;
    private UnitOfWork uow;

    @Before
    public void setUp() throws Exception {
        testSubject = new BeanValidationInterceptor();
        mockInterceptorChain = mock(InterceptorChain.class);
        uow = mock(UnitOfWork.class);
    }

    @Test
    public void testValidateSimpleObject() throws Throwable {
        final GenericCommandMessage<Object> command = new GenericCommandMessage<Object>("Simple instance");
        testSubject.handle(command, uow, mockInterceptorChain);
        verify(mockInterceptorChain).proceed(same(command));
    }

    @Test
    public void testValidateAnnotatedObject_IllegalNullValue() throws Throwable {
        try {
            testSubject.handle(new GenericCommandMessage<Object>(new JSR303AnnotatedInstance(null)),
                               uow, mockInterceptorChain);
            fail("Expected exception");
        } catch (JSR303ViolationException e) {
            assertFalse(e.getViolations().isEmpty());
        }
        verify(mockInterceptorChain, never()).proceed();
        verify(mockInterceptorChain, never()).proceed(isA(CommandMessage.class));
    }

    @Test
    public void testValidateAnnotatedObject_LegalValue() throws Throwable {
        final GenericCommandMessage<Object> commandMessage =
                new GenericCommandMessage<Object>(new JSR303AnnotatedInstance("abc"));
        testSubject.handle(commandMessage, uow, mockInterceptorChain);

        verify(mockInterceptorChain).proceed(same(commandMessage));
    }

    @Test
    public void testValidateAnnotatedObject_IllegalValue() throws Throwable {
        try {
            testSubject.handle(new GenericCommandMessage<Object>(new JSR303AnnotatedInstance("bea")),
                               uow, mockInterceptorChain);
            fail("Expected exception");
        } catch (JSR303ViolationException e) {
            assertFalse(e.getViolations().isEmpty());
        }
        verify(mockInterceptorChain, never()).proceed();
        verify(mockInterceptorChain, never()).proceed(isA(CommandMessage.class));
    }

    @Test
    public void testCustomValidatorFactory() throws Throwable {
        ValidatorFactory mockValidatorFactory = spy(Validation.buildDefaultValidatorFactory());
        testSubject = new BeanValidationInterceptor(mockValidatorFactory);

        testSubject.handle(new GenericCommandMessage<Object>(new JSR303AnnotatedInstance("abc")),
                           uow, mockInterceptorChain);

        verify(mockValidatorFactory).getValidator();
    }

    public static class JSR303AnnotatedInstance {

        @Pattern(regexp = "ab.*")
        @NotNull
        private String notNull;

        public JSR303AnnotatedInstance(String notNull) {
            this.notNull = notNull;
        }
    }
}
