package io.micronaut.cache.infinispan

import org.infinispan.configuration.global.GlobalConfiguration
import org.infinispan.configuration.global.GlobalConfigurationBuilder
import org.infinispan.globalstate.ConfigurationStorage
import org.infinispan.manager.DefaultCacheManager
import org.infinispan.server.core.admin.embeddedserver.EmbeddedServerAdminOperationHandler
import org.infinispan.server.hotrod.HotRodServer
import org.infinispan.server.hotrod.configuration.HotRodServerConfiguration
import org.infinispan.server.hotrod.configuration.HotRodServerConfigurationBuilder
import spock.lang.Shared

trait EmbeddedHotRodServerSupport {

    @Shared
    HotRodServer server = new HotRodServer()

    void setupSpec() {
        GlobalConfiguration globalConfiguration = new GlobalConfigurationBuilder()
                .globalJmxStatistics()
                    .enable()
                .globalState()
                    .enable()
                    .persistentLocation(System.getProperty('java.io.tmpdir'))
                    .sharedPersistentLocation(System.getProperty('java.io.tmpdir'))
                    .configurationStorage(ConfigurationStorage.OVERLAY)
                .build()
        DefaultCacheManager defaultCacheManager = new DefaultCacheManager(globalConfiguration)
        HotRodServerConfiguration configuration = new HotRodServerConfigurationBuilder()
                .adminOperationsHandler(new EmbeddedServerAdminOperationHandler())
                .build()
        HotRodServer server = new HotRodServer()
        server.start(configuration, defaultCacheManager)
    }

    void cleanupSpec() {
        server.stop()
    }

}
