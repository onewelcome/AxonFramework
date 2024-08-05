/*
 * Copyright (c) 2010-2024. Axon Framework
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

package org.axonframework.messaging;

import jakarta.validation.constraints.NotNull;
import reactor.core.publisher.Flux;

import java.util.concurrent.CompletableFuture;

class ConcatenatingMessageStream<T extends Message<?>> implements MessageStream<T> {

    private final MessageStream<T> first;
    private final MessageStream<T> second;

    public ConcatenatingMessageStream(@NotNull MessageStream<T> first, @NotNull MessageStream<T> second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public CompletableFuture<T> asCompletableFuture() {
        return first.asCompletableFuture().thenCompose(i -> {
            if (i == null) {
                return second.asCompletableFuture();
            }
            return CompletableFuture.completedFuture(i);
        });
    }

    @Override
    public Flux<T> asFlux() {
        return first.asFlux().concatWith(second.asFlux());
    }
}