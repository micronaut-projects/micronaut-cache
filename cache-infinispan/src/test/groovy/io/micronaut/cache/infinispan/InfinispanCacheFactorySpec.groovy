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
package io.micronaut.cache.infinispan

import io.micronaut.context.ApplicationContext
import io.micronaut.context.exceptions.NoSuchBeanException
import org.infinispan.client.hotrod.RemoteCacheManager
import org.infinispan.client.hotrod.impl.async.DefaultAsyncExecutorFactory
import spock.lang.Specification

class InfinispanCacheFactorySpec extends Specification {

    void "it creates a remote cache manager"(){
        given:
        ApplicationContext ctx = ApplicationContext.run("infinispan.enabled": true)

        when:
        RemoteCacheManager remoteCacheManager = ctx.getBean(RemoteCacheManager)

        then:
        remoteCacheManager.started

        cleanup:
        ctx.close()
    }

    void "infinispan can be disabled"() {
        given:
        ApplicationContext ctx = ApplicationContext.run("infinispan.enabled": false)

        when:
        ctx.getBean(RemoteCacheManager)

        then:
        thrown(NoSuchBeanException)

        cleanup:
        ctx.close()
    }

    void "it can customise connection settings"() {
        given:
        ApplicationContext ctx = ApplicationContext.run([
                "infinispan.client.hotrod.server.host": "localhost",
                "infinispan.client.hotrod.server.port": 11223
        ])

        when:
        RemoteCacheManager remoteCacheManager = ctx.getBean(RemoteCacheManager)

        then:
        remoteCacheManager.servers.size() == 1
        remoteCacheManager.servers.last().endsWith(':11223')

        cleanup:
        ctx.close()
    }

    void "it can read configuration from hotrod-client.properties"() {
        given:
        ApplicationContext ctx = ApplicationContext.run([
                "infinispan.client.hotrod.config-file": "classpath:hotrod.properties"
        ])

        when:
        RemoteCacheManager remoteCacheManager = ctx.getBean(RemoteCacheManager)

        then:
        remoteCacheManager.servers.size() == 2
        remoteCacheManager.servers.first().endsWith(':11224')
        remoteCacheManager.servers.last().endsWith(':11225')
        remoteCacheManager.configuration.statistics().enabled()

        cleanup:
        ctx.close()
    }

    void "it uses MicronautExecutorFactory if async-executor-factory.factory-class is unset"() {
        given:
        ApplicationContext ctx = ApplicationContext.run("infinispan.enabled": true)

        when:
        RemoteCacheManager remoteCacheManager = ctx.getBean(RemoteCacheManager)

        then:
        remoteCacheManager.configuration.asyncExecutorFactory().factory().class == MicronautExecutorFactory
        !remoteCacheManager.configuration.asyncExecutorFactory().factoryClass()

        cleanup:
        ctx.close()
    }

    void "it can use Infinispan's executor factory if explicitly set" () {
        given:
        ApplicationContext ctx = ApplicationContext.run("infinispan.client.hotrod.async-executor-factory.factory-class": 'org.infinispan.client.hotrod.impl.async.DefaultAsyncExecutorFactory')

        when:
        RemoteCacheManager remoteCacheManager = ctx.getBean(RemoteCacheManager)

        then:
        remoteCacheManager.configuration.asyncExecutorFactory().factoryClass() == DefaultAsyncExecutorFactory
        remoteCacheManager.configuration.asyncExecutorFactory().factory()

        cleanup:
        ctx.close()
    }

}
