/*
 * Copyright 2017-2020 original authors
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
package io.micronaut.cache.coherence
import com.tangosol.net.Coherence
import io.micronaut.cache.tck.AbstractAsyncCacheSpec
import io.micronaut.context.ApplicationContext
import spock.lang.Retry

/**
 * @author Vaso Putica
 */
@Retry
class CoherenceClientAsyncCacheSpec extends AbstractAsyncCacheSpec {

    ApplicationContext applicationContext

    @Override
    ApplicationContext createApplicationContext() {
        return applicationContext
    }

    void setup() {
        applicationContext = ApplicationContext.run()
        Coherence coherence = applicationContext.getBean(Coherence)
        coherence.start().join()
    }

    void cleanup() {
        Coherence.closeAll()
        applicationContext = null
    }
}
