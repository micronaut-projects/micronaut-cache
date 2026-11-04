package io.micronaut.cache.caffeine.graal

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Expiry
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.TempDir

import java.lang.reflect.Executable
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.function.Consumer
import java.util.function.Function

class CaffeineReflectionSpec extends Specification {

    private static final String CACHE_PACKAGE = "com.github.benmanes.caffeine.cache"

    @Shared
    Class<?> localCacheFactory = Class.forName("${CACHE_PACKAGE}.LocalCacheFactory")

    @Shared
    Class<?> nodeFactory = Class.forName("${CACHE_PACKAGE}.NodeFactory")

    @Shared
    Set<String> generatedClasses = CaffeineReflection.classNames(localCacheFactory) as Set

    @TempDir
    Path tempDir

    void "the generated classes are the top level classes of the cache package named with capital letters"() {
        expect:
        CaffeineReflection.isGeneratedClass(className) == generated

        where:
        className                                         | generated
        "${CACHE_PACKAGE}.SSLA"                           | true
        "${CACHE_PACKAGE}.PSW"                            | true
        "${CACHE_PACKAGE}.FD"                             | true
        "${CACHE_PACKAGE}.LocalCacheFactory"              | false
        "${CACHE_PACKAGE}.Caffeine"                       | false
        "${CACHE_PACKAGE}.BLCHeader\$DrainStatusRef"      | false
        "${CACHE_PACKAGE}.SSLA\$1"                        | false
        "${CACHE_PACKAGE}.stats.CacheStats"               | false
        "${CACHE_PACKAGE}.stats.ABC"                      | false
        "${CACHE_PACKAGE}."                               | false
        "com.github.benmanes.caffeine.cacheX.SSLA"        | false
        "org.example.SSLA"                                | false
    }

    void "the classes of the Caffeine jar are listed"() {
        expect:
        generatedClasses.containsAll(["SSLA", "SSAW", "PSW", "PS"].collect { "${CACHE_PACKAGE}.${it}".toString() })
        generatedClasses.every { CaffeineReflection.isGeneratedClass(it) }
    }

    void "the generated classes are found"() {
        when:
        List<Class<?>> classes = CaffeineReflection.generatedClasses(this::findClass)

        then:
        classes*.name as Set == generatedClasses
    }

    void "no class is found without Caffeine"() {
        expect:
        CaffeineReflection.generatedClasses({ String name -> null } as Function).isEmpty()
        CaffeineReflection.fields({ String name -> null } as Function).isEmpty()
    }

    void "the classes that are missing from the image are skipped"() {
        given:
        Function<String, Class<?>> findClass = { String name ->
            name.endsWith(".SSLA") ? null : findClass(name)
        }

        when:
        List<Class<?>> classes = CaffeineReflection.generatedClasses(findClass)

        then:
        classes.size() == generatedClasses.size() - 1
        !classes*.name.contains("${CACHE_PACKAGE}.SSLA".toString())
    }

    void "the fields looked up outside the generated classes are found"() {
        when:
        Set<String> fields = CaffeineReflection.fields(this::findClass).collect { "${it.declaringClass.simpleName}.${it.name}".toString() } as Set

        then:
        fields == [
                "DrainStatusRef.drainStatus",
                "ReadCounterRef.readCounter",
                "ReadAndWriteCounterRef.writeCounter",
                "StripedBuffer.tableBusy",
                "Thread.threadLocalRandomProbe",
        ] as Set
    }

    void "the generated classes are registered with their constructors and fields"() {
        given:
        List<Class<?>> classes = []
        List<Executable> constructors = []
        List<Field> fields = []

        when:
        CaffeineReflection.register(this::findClass,
                { Class<?>[] types -> classes.addAll(types) } as Consumer,
                { Executable[] executables -> constructors.addAll(executables) } as Consumer,
                { Field[] declared -> fields.addAll(declared) } as Consumer)
        Class<?> ssla = Class.forName("${CACHE_PACKAGE}.SSLA")
        Class<?> psw = Class.forName("${CACHE_PACKAGE}.PSW")

        then:
        classes*.name as Set == generatedClasses
        fields.contains(ssla.getDeclaredField("FACTORY"))
        fields.contains(psw.getDeclaredField("writeTime"))
        fields.containsAll(CaffeineReflection.fields(this::findClass))
        constructors.contains(psw.getDeclaredConstructor())
    }

    void "the generated classes of a directory of classes are listed"() {
        given:
        Path packageDirectory = Files.createDirectories(tempDir.resolve(CACHE_PACKAGE.replace('.', '/')))
        ["SSLA.class", "PSW.class", "Caffeine.class", "SSLA\$1.class", "README.txt"].each {
            Files.createFile(packageDirectory.resolve(it))
        }

        expect:
        CaffeineReflection.classNames(tempDir) == ["${CACHE_PACKAGE}.PSW", "${CACHE_PACKAGE}.SSLA"]
    }

    void "nothing is listed when there are no classes"() {
        expect:
        CaffeineReflection.classNames(tempDir).isEmpty()
        CaffeineReflection.classNames(tempDir.resolve("missing.jar")).isEmpty()
        CaffeineReflection.classNames(String).isEmpty()
    }

    void "an unreadable jar fails"() {
        given:
        Path jar = Files.writeString(tempDir.resolve("broken.jar"), "not a jar")

        when:
        CaffeineReflection.classNames(jar)

        then:
        thrown(UncheckedIOException)
    }

    void "every cache and node class that Caffeine picks is listed and has a factory"() {
        given:
        List<Caffeine<Object, Object>> builders = builders()
        Set<String> cacheClasses = builders.collect { className(localCacheFactory, it) }.findAll() as Set
        Set<String> nodeClasses = builders.collectMany { [className(nodeFactory, it, false), className(nodeFactory, it, true)] } as Set

        expect: "the configurations cover many classes"
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

    private static Class<?> findClass(String name) {
        try {
            return Class.forName(name, false, CaffeineReflectionSpec.classLoader)
        } catch (ClassNotFoundException ignored) {
            return null
        }
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
        return name.contains('.') ? name : "${CACHE_PACKAGE}.${name}".toString()
    }

    /**
     * Every combination of the settings that the names of the generated classes depend on.
     */
    private static List<Caffeine<Object, Object>> builders() {
        List<List<Closure>> settings = [
                [{ }, { Caffeine b -> b.weakKeys() }],
                [{ }, { Caffeine b -> b.weakValues() }, { Caffeine b -> b.softValues() }],
                [{ }, { Caffeine b -> b.removalListener { k, v, cause -> } }],
                [{ }, { Caffeine b -> b.recordStats() }],
                [{ }, { Caffeine b -> b.maximumSize(10) }, { Caffeine b -> b.maximumWeight(10).weigher { k, v -> 1 } }],
                [
                        { },
                        { Caffeine b -> b.expireAfterAccess(Duration.ofMinutes(1)) },
                        { Caffeine b -> b.expireAfterWrite(Duration.ofMinutes(1)) },
                        { Caffeine b -> b.expireAfterAccess(Duration.ofMinutes(1)).expireAfterWrite(Duration.ofMinutes(1)) },
                        { Caffeine b -> b.expireAfter(Expiry.creating { k, v -> Duration.ofMinutes(1) }) },
                ],
                [{ }, { Caffeine b -> b.refreshAfterWrite(Duration.ofMinutes(1)) }],
        ]
        return settings.combinations().collect { List<Closure> combination ->
            Caffeine<Object, Object> builder = Caffeine.newBuilder()
            combination.each { it.call(builder) }
            builder
        }
    }
}
