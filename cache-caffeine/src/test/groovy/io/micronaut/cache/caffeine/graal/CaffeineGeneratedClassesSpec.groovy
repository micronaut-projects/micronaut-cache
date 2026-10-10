package io.micronaut.cache.caffeine.graal

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Expiry
import spock.lang.Shared
import spock.lang.Specification

import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.time.Duration

class CaffeineGeneratedClassesSpec extends Specification {

    @Shared
    Class<?> localCacheFactory = Class.forName("com.github.benmanes.caffeine.cache.LocalCacheFactory")

    @Shared
    Class<?> nodeFactory = Class.forName("com.github.benmanes.caffeine.cache.NodeFactory")

    @Shared
    Set<String> generatedClasses = CaffeineGeneratedClasses.classNames(localCacheFactory) as Set

    void "the generated classes are the top level classes of the cache package named with capital letters"() {
        expect:
        CaffeineGeneratedClasses.isGeneratedClass(className) == generated

        where:
        className                                                       | generated
        "com.github.benmanes.caffeine.cache.SSLA"                       | true
        "com.github.benmanes.caffeine.cache.PSW"                        | true
        "com.github.benmanes.caffeine.cache.FD"                         | true
        "com.github.benmanes.caffeine.cache.LocalCacheFactory"          | false
        "com.github.benmanes.caffeine.cache.Caffeine"                   | false
        "com.github.benmanes.caffeine.cache.BLCHeader\$DrainStatusRef"  | false
        "com.github.benmanes.caffeine.cache.SSLA\$1"                    | false
        "com.github.benmanes.caffeine.cache.stats.CacheStats"           | false
        "com.github.benmanes.caffeine.cache.stats.ABC"                  | false
        "com.github.benmanes.caffeine.cache."                           | false
        "com.github.benmanes.caffeine.cacheX.SSLA"                      | false
        "org.example.SSLA"                                              | false
    }

    void "the classes of the Caffeine jar are listed"() {
        expect:
        generatedClasses.containsAll([
                "com.github.benmanes.caffeine.cache.SSLA",
                "com.github.benmanes.caffeine.cache.SSAW",
                "com.github.benmanes.caffeine.cache.PSW",
                "com.github.benmanes.caffeine.cache.PS",
        ])
        generatedClasses.every { CaffeineGeneratedClasses.isGeneratedClass(it) }
    }

    void "every cache and node class that Caffeine picks is listed and has a factory"() {
        given:
        Set<String> cacheClasses = [] as Set
        Set<String> nodeClasses = [] as Set

        when:
        for (Caffeine<Object, Object> builder : builders()) {
            String cacheClass = className(localCacheFactory, builder)
            if (cacheClass != null) {
                cacheClasses << cacheClass
            }
            nodeClasses << className(nodeFactory, builder, false)
            nodeClasses << className(nodeFactory, builder, true)
        }

        then: "the configurations cover many classes"
        cacheClasses.size() > 50
        nodeClasses.size() > 50

        and: "they are all listed"
        generatedClasses.containsAll(cacheClasses)
        generatedClasses.containsAll(nodeClasses)

        and: "Caffeine reads the FACTORY field of a cache class"
        cacheClasses.every { Modifier.isStatic(Class.forName(it).getDeclaredField("FACTORY").modifiers) }

        and: "Caffeine looks up the FACTORY field or the constructor of a node class"
        nodeClasses.every { hasFactory(Class.forName(it)) }
    }

    private static boolean hasFactory(Class<?> type) {
        type.declaredFields.any { it.name == "FACTORY" } || type.declaredConstructors.any { it.parameterCount == 0 }
    }

    private static String className(Class<?> factory, Object... args) {
        Method method = factory.declaredMethods.find { it.name == "getClassName" && it.parameterCount == args.length }
        method.accessible = true
        String name = method.invoke(null, args)
        if (name == null) {
            return null
        }
        return name.contains('.') ? name : "com.github.benmanes.caffeine.cache." + name
    }

    /**
     * Every combination of the settings that the names of the generated classes depend on.
     */
    private static List<Caffeine<Object, Object>> builders() {
        List<Caffeine<Object, Object>> builders = []
        for (boolean weakKeys : [false, true]) {
            for (String values : ["strong", "weak", "soft"]) {
                for (boolean listener : [false, true]) {
                    for (boolean stats : [false, true]) {
                        for (String bound : ["none", "size", "weight"]) {
                            for (String expiry : ["none", "access", "write", "both", "variable"]) {
                                for (boolean refresh : [false, true]) {
                                    Caffeine<Object, Object> builder = Caffeine.newBuilder()
                                    if (weakKeys) {
                                        builder.weakKeys()
                                    }
                                    if (values == "weak") {
                                        builder.weakValues()
                                    } else if (values == "soft") {
                                        builder.softValues()
                                    }
                                    if (listener) {
                                        builder.removalListener { k, v, cause -> }
                                    }
                                    if (stats) {
                                        builder.recordStats()
                                    }
                                    if (bound == "size") {
                                        builder.maximumSize(10)
                                    } else if (bound == "weight") {
                                        builder.maximumWeight(10).weigher { k, v -> 1 }
                                    }
                                    if (expiry in ["access", "both"]) {
                                        builder.expireAfterAccess(Duration.ofMinutes(1))
                                    }
                                    if (expiry in ["write", "both"]) {
                                        builder.expireAfterWrite(Duration.ofMinutes(1))
                                    }
                                    if (expiry == "variable") {
                                        builder.expireAfter(Expiry.creating { k, v -> Duration.ofMinutes(1) })
                                    }
                                    if (refresh) {
                                        builder.refreshAfterWrite(Duration.ofMinutes(1))
                                    }
                                    builders << builder
                                }
                            }
                        }
                    }
                }
            }
        }
        return builders
    }
}
