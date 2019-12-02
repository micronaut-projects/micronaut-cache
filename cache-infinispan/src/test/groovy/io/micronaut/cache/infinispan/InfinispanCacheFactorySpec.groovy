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
        remoteCacheManager.servers.last() == 'localhost:11223'

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
        remoteCacheManager.servers.first() == 'localhost:11224'
        remoteCacheManager.servers.last() == 'localhost:11225'
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
    }

    void "it can use Infinispan's executor factory if explicitly set" () {
        given:
        ApplicationContext ctx = ApplicationContext.run("infinispan.client.hotrod.async-executor-factory.factory-class": 'org.infinispan.client.hotrod.impl.async.DefaultAsyncExecutorFactory')

        when:
        RemoteCacheManager remoteCacheManager = ctx.getBean(RemoteCacheManager)

        then:
        remoteCacheManager.configuration.asyncExecutorFactory().factoryClass() == DefaultAsyncExecutorFactory
        !remoteCacheManager.configuration.asyncExecutorFactory().factory()
    }

}
