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

package org.axonframework.common;

import static org.mockito.ArgumentMatchers.argThat;

import org.axonframework.domain.EventMessage;
import org.mockito.ArgumentMatcher;

/**
 *
 */
public class MatcherUtils {

    public static EventMessage isEventWith(final Class<?> payloadType) {
        return argThat(new ArgumentMatcher<>() {
            @Override
            public boolean matches(EventMessage eventMessage) {
                return payloadType.isInstance(eventMessage.getPayload());
            }

            @Override
            public String toString() {
                return String.format("Event with payload of type [%s]", payloadType.getName());
            }
        });
    }
}
