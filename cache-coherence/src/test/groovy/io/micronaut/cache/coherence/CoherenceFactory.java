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
package io.micronaut.cache.coherence;

import com.tangosol.net.Coherence;
import com.tangosol.net.CoherenceConfiguration;
import com.tangosol.net.Session;
import com.tangosol.net.SessionConfiguration;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Prototype;

import javax.inject.Singleton;

/**
 * Factory class that creates a {@link com.tangosol.net.Coherence}.
 *
 * @author Vaso Putica
 */
@Factory
public class CoherenceFactory {

    @Singleton
    @Bean(preDestroy = "close")
    public Coherence coherenceInstance() {
        CoherenceConfiguration cfg = CoherenceConfiguration.builder()
                .named("TestInstance")
                .withSession(SessionConfiguration.defaultSession())
                .build();
        Coherence coherence = Coherence.create(cfg);
        coherence.start();
        return coherence;
    }

    @Prototype
    public Session sessionInstance(Coherence coherence) {
        return coherence.getSession();
    }
}
