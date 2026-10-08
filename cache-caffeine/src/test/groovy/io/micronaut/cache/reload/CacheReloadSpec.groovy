package io.micronaut.cache.reload

import io.micronaut.cache.AbstractMapBasedSyncCache
import io.micronaut.cache.AsyncCache
import io.micronaut.cache.CacheManager
import io.micronaut.cache.CounterService
import io.micronaut.cache.SyncCache
import io.micronaut.cache.caffeine.DefaultSyncCache
import io.micronaut.cache.interceptor.CacheInterceptor
import io.micronaut.context.ApplicationContext
import io.micronaut.context.reload.ClassChange
import io.micronaut.context.reload.ClassChangeEvent
import io.micronaut.context.reload.ReloadStrategy
import io.micronaut.core.convert.ConversionService
import io.micronaut.inject.qualifiers.Qualifiers
import spock.lang.Specification

class CacheReloadSpec extends Specification {

    private static final String RELOADER = 'io.micronaut.cache.DevelopmentCacheReloader'

    void "in development mode an in-place change of a cached class invalidates every cache, and a restart or an unrelated change does not"() {
        given:
        ApplicationContext context = devContext(true)
        CounterService service = context.getBean(CounterService)
        CacheInterceptor interceptor = context.getBean(CacheInterceptor)
        SyncCache<?> dynamic = context.getBean(CacheManager).getCache('dynamic')
        dynamic.put('key', 'value')
        MapCache asyncBacking = new MapCache()
        context.registerSingleton(AsyncCache, asyncBacking.async(), Qualifiers.byName('async'))
        asyncBacking.put('key', 'value')

        expect: 'the reloader exists only in development mode'
        context.containsBean(reloader())

        when: 'a value is cached, then changes behind the cache'
        service.increment('one')
        service.incrementNoCache('one')

        then:
        service.getValue('one') == 1
        size(context, 'counter') == 1

        when: 'the application restarts: the new context has new caches'
        context.publishEvent(classChange([CounterService.classLoader] as Set, [], ReloadStrategy.RESTART))

        then:
        service.getValue('one') == 1
        dynamic.get('key', String).present
        asyncBacking.nativeCache.size() == 1
        context.getBean(CacheInterceptor).is(interceptor)

        when: 'a class without cache operations is redefined in place'
        context.publishEvent(classChange([] as Set, [new ClassChange(CacheReloadSpec.name, ClassChange.Kind.MODIFIED)], ReloadStrategy.RELOAD))

        then:
        service.getValue('one') == 1
        dynamic.get('key', String).present

        when: 'a class with cache operations is redefined in place'
        context.publishEvent(classChange([] as Set, [new ClassChange(CounterService.name, ClassChange.Kind.MODIFIED)], ReloadStrategy.RELOAD))

        then: 'every cache is emptied, the dynamic ones included, and the next invocation computes the value again'
        size(context, 'counter') == 0
        !dynamic.get('key', String).present
        asyncBacking.nativeCache.isEmpty()
        service.getValue('one') == 2

        and: 'the interceptor, whose maps are keyed by methods that did not change, is kept'
        context.getBean(CacheInterceptor).is(interceptor)
        context.getBean(CounterService).is(service)

        cleanup:
        context.close()
    }

    void "in development mode a reload that retires a classloader invalidates the caches and recreates the interceptor and the beans it advises"() {
        given:
        ApplicationContext context = devContext(true)
        CounterService service = context.getBean(CounterService)
        CacheInterceptor interceptor = context.getBean(CacheInterceptor)
        service.increment('one')

        expect:
        size(context, 'counter') == 1

        when:
        context.publishEvent(classChange([CounterService.classLoader] as Set, [], ReloadStrategy.RELOAD))
        CounterService recreated = context.getBean(CounterService)

        then: 'the cache is empty'
        size(context, 'counter') == 0

        and: 'the interceptor keyed by the retired methods is replaced, and the advised bean with it'
        !context.getBean(CacheInterceptor).is(interceptor)
        !recreated.is(service)

        and: 'the new bean caches through the new interceptor'
        recreated.increment('two') == 1
        recreated.incrementNoCache('two') == 2
        recreated.getValue('two') == 1
        size(context, 'counter') == 1

        cleanup:
        context.close()
    }

    void "in development mode a context that does not track bean dependencies invalidates the caches but keeps the interceptor"() {
        given:
        ApplicationContext context = devContext(false)
        CounterService service = context.getBean(CounterService)
        CacheInterceptor interceptor = context.getBean(CacheInterceptor)
        service.increment('one')

        expect:
        context.containsBean(reloader())

        when:
        context.publishEvent(classChange([CounterService.classLoader] as Set, [], ReloadStrategy.RELOAD))

        then:
        size(context, 'counter') == 0
        context.getBean(CacheInterceptor).is(interceptor)
        context.getBean(CounterService).is(service)

        cleanup:
        context.close()
    }

    void "outside development mode there is no reloader, and class changes leave the caches and the interceptor as they are"() {
        given:
        ApplicationContext context = ApplicationContext.run(cacheProperties())
        CounterService service = context.getBean(CounterService)
        CacheInterceptor interceptor = context.getBean(CacheInterceptor)
        CacheManager manager = context.getBean(CacheManager)
        service.increment('one')
        service.incrementNoCache('one')

        expect:
        !context.containsBean(reloader())

        when:
        context.publishEvent(classChange([CounterService.classLoader] as Set, [new ClassChange(CounterService.name, ClassChange.Kind.MODIFIED)], ReloadStrategy.RELOAD))

        then:
        size(context, 'counter') == 1
        service.getValue('one') == 1
        context.getBean(CacheInterceptor).is(interceptor)
        context.getBean(CacheManager).is(manager)
        context.getBean(CounterService).is(service)

        cleanup:
        context.close()
    }

    private static ApplicationContext devContext(boolean track) {
        return ApplicationContext.builder()
            .properties(cacheProperties() + ['micronaut.dev.enabled': true])
            .beanDependencyTrackingEnabled(track)
            .start()
    }

    private static Map<String, Object> cacheProperties() {
        return ['micronaut.caches.counter.maximum-size': 20, 'micronaut.caches.counter.test-mode': true]
    }

    private static long size(ApplicationContext context, String name) {
        DefaultSyncCache cache = (DefaultSyncCache) context.getBean(CacheManager).getCache(name)
        return cache.nativeCache.asMap().size()
    }

    private static Class<?> reloader() {
        return Class.forName(RELOADER)
    }

    private static ClassChangeEvent classChange(Set<ClassLoader> retired, List<ClassChange> changes, ReloadStrategy strategy) {
        return new ClassChangeEvent(CacheReloadSpec, retired, CacheReloadSpec.classLoader, changes, strategy)
    }
}


class MapCache extends AbstractMapBasedSyncCache<Map<Object, Object>> {
    MapCache() {
        super(ConversionService.SHARED, new java.util.concurrent.ConcurrentHashMap<Object, Object>())
    }

    @Override
    String getName() {
        return 'async'
    }
}
