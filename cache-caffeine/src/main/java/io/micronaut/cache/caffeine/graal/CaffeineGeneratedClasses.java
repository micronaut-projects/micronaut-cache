/*
 * Copyright 2017-2026 original authors
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
package io.micronaut.cache.caffeine.graal;

import io.micronaut.core.annotation.Internal;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * Lists the classes that Caffeine generates for each kind of cache and cache entry, such as {@code SSLA} or
 * {@code PSW}. Caffeine picks one of them by name from the configuration of the cache, then reads its static
 * {@code FACTORY} field with a {@code VarHandle} or looks up its constructor.
 *
 * @author Graeme Rocher
 * @since 6.1.2
 */
@Internal
final class CaffeineGeneratedClasses {

    static final String CACHE_PACKAGE = "com.github.benmanes.caffeine.cache";
    static final String LOCAL_CACHE_FACTORY = CACHE_PACKAGE + ".LocalCacheFactory";

    private static final String CLASS_SUFFIX = ".class";
    private static final String PACKAGE_PATH = CACHE_PACKAGE.replace('.', '/') + '/';

    private CaffeineGeneratedClasses() {
    }

    /**
     * Lists the generated classes next to a class of Caffeine.
     *
     * @param anchor A class of Caffeine, such as {@code LocalCacheFactory}
     * @return The fully qualified names of the generated classes, or an empty list when the location of the
     * classes is unknown
     */
    static List<String> classNames(Class<?> anchor) {
        CodeSource codeSource = anchor.getProtectionDomain().getCodeSource();
        if (codeSource == null || codeSource.getLocation() == null) {
            return List.of();
        }
        Path location;
        try {
            location = Path.of(codeSource.getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Invalid location of the Caffeine classes: " + codeSource.getLocation(), e);
        }
        return classNames(location);
    }

    /**
     * Lists the generated classes of a jar or a directory of classes.
     *
     * @param location The jar of Caffeine, or the directory of its classes
     * @return The fully qualified names of the generated classes
     */
    static List<String> classNames(Path location) {
        try {
            if (Files.isDirectory(location)) {
                Path packageDirectory = location.resolve(PACKAGE_PATH);
                if (!Files.isDirectory(packageDirectory)) {
                    return List.of();
                }
                try (Stream<Path> files = Files.list(packageDirectory)) {
                    return generatedClassNames(files.map(file -> PACKAGE_PATH + file.getFileName()));
                }
            }
            if (!Files.isRegularFile(location)) {
                return List.of();
            }
            try (JarFile jar = new JarFile(location.toFile())) {
                return generatedClassNames(jar.stream().map(JarEntry::getName));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot list the classes of Caffeine in " + location, e);
        }
    }

    /**
     * The generated classes are the top level classes of the cache package whose names are made of capital
     * letters only.
     *
     * @param className The fully qualified name of a class
     * @return Whether Caffeine generated the class
     */
    static boolean isGeneratedClass(String className) {
        if (!className.startsWith(CACHE_PACKAGE + '.')) {
            return false;
        }
        String name = className.substring(CACHE_PACKAGE.length() + 1);
        return !name.isEmpty() && name.chars().allMatch(c -> c >= 'A' && c <= 'Z');
    }

    private static List<String> generatedClassNames(Stream<String> entryNames) {
        return entryNames
            .filter(name -> name.startsWith(PACKAGE_PATH) && name.endsWith(CLASS_SUFFIX))
            .map(name -> name.substring(0, name.length() - CLASS_SUFFIX.length()).replace('/', '.'))
            .filter(CaffeineGeneratedClasses::isGeneratedClass)
            .sorted()
            .toList();
    }
}
