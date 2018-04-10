/*
 * Copyright 2017-2018 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.retry.event;

import io.micronaut.context.event.ApplicationEvent;
import io.micronaut.inject.ExecutableMethod;

/**
 * An event fired when a Circuit is {@link io.micronaut.retry.CircuitState#CLOSED} and has resumed
 * accepting requests
 *
 * @author graemerocher
 * @since 1.0
 */
public class CircuitClosedEvent extends ApplicationEvent {

    public CircuitClosedEvent(
        ExecutableMethod<?, ?> source) {
        super(source);
    }

    /**
     * @return The method that represents the circuit
     */
    @Override
    public ExecutableMethod<?, ?> getSource() {
        return (ExecutableMethod<?, ?>) super.getSource();
    }
}
