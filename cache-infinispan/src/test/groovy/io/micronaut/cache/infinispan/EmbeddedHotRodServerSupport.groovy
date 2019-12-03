package io.micronaut.cache.infinispan

import org.infinispan.configuration.cache.Configuration
import org.infinispan.configuration.cache.ConfigurationBuilder
import org.infinispan.configuration.global.GlobalConfiguration
import org.infinispan.configuration.global.GlobalConfigurationBuilder
import org.infinispan.eviction.EvictionStrategy
import org.infinispan.globalstate.ConfigurationStorage
import org.infinispan.manager.DefaultCacheManager
import org.infinispan.server.core.admin.embeddedserver.EmbeddedServerAdminOperationHandler
import org.infinispan.server.hotrod.HotRodServer
import org.infinispan.server.hotrod.configuration.HotRodServerConfiguration
import org.infinispan.server.hotrod.configuration.HotRodServerConfigurationBuilder
import spock.lang.Shared

trait EmbeddedHotRodServerSupport {

    @Shared
    HotRodServer server

    void setupSpec() {
        String uniqueName = UUID.randomUUID().toString()
        new File(System.getProperty('java.io.tmpdir') + "/${uniqueName}").mkdirs()
        GlobalConfiguration globalConfiguration = new GlobalConfigurationBuilder()
                .cacheManagerName(uniqueName)
                .cacheContainer()
                    .statistics(true)
                .globalJmxStatistics()
                    .disable()
                .globalState()
                    .enable()
                    .persistentLocation(System.getProperty('java.io.tmpdir') + "/${uniqueName}/")
                    .sharedPersistentLocation(System.getProperty('java.io.tmpdir') + "/${uniqueName}/")
                    .configurationStorage(ConfigurationStorage.OVERLAY)
                .build()
        DefaultCacheManager defaultCacheManager = new DefaultCacheManager(globalConfiguration)

        Configuration cacheConfiguration = new ConfigurationBuilder().memory().evictionStrategy(EvictionStrategy.LRU).size(3).build()
        defaultCacheManager.defineConfiguration("test", cacheConfiguration)

        HotRodServerConfiguration configuration = new HotRodServerConfigurationBuilder()
                .adminOperationsHandler(new EmbeddedServerAdminOperationHandler())
                .build()
        server = new HotRodServer()
        server.start(configuration, defaultCacheManager)
    }

    void cleanupSpec() {
        server.stop()
    }

}
