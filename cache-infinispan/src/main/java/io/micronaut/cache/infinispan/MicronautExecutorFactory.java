/*
 * Copyright 2017-2020 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.cache.infinispan;

import io.micronaut.scheduling.TaskExecutors;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.infinispan.commons.executors.ExecutorFactory;

import java.util.Properties;
import java.util.concurrent.ExecutorService;

/**
 * An {@link ExecutorFactory} implementation based on the existing IO {@link ExecutorService} bean.
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
@Singleton
public class MicronautExecutorFactory implements ExecutorFactory {

    private ExecutorService executorService;

    /**
     * @param executorService the executor service
     */
    public MicronautExecutorFactory(@Named(TaskExecutors.IO) ExecutorService executorService) {
        this.executorService = executorService;
    }

    @Override
    public ExecutorService getExecutor(Properties p) {
        return executorService;
    }
}
