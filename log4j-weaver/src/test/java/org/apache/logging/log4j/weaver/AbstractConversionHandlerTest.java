/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.weaver;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class AbstractConversionHandlerTest {

    protected static Class<?> convertedClass;
    protected static Object testObject;

    protected static void transformClass(String internalName) throws Exception {
        final TestClassLoader testCl = new TestClassLoader();

        final ByteArrayOutputStream dest = new ByteArrayOutputStream();
        final LocationClassConverter converter = new LocationClassConverter();
        final LocationCacheGenerator locationCache = new LocationCacheGenerator();

        getNestedClasses(internalName).forEach(classFile -> assertDoesNotThrow(() -> {
            dest.reset();
            converter.convert(Files.newInputStream(classFile), dest, locationCache);
            testCl.defineClass(dest.toByteArray());
        }));
        locationCache.generateClasses().values().forEach(testCl::defineClass);
        convertedClass = testCl.loadClass(internalName.replaceAll("/", "."));
        testObject = assertDoesNotThrow(() -> convertedClass.getConstructor().newInstance());
    }

    private static Stream<Path> getNestedClasses(String internalName) throws Exception {
        final Path topClass = Paths
                .get(AbstractConversionHandlerTest.class.getClassLoader().getResource(internalName + ".class").toURI());
        final String simpleClassName = Paths.get(internalName).getFileName().toString();
        return Files.walk(topClass.getParent(), 1).filter(p -> {
            final String nested = p.getFileName().toString();
            return nested.startsWith(simpleClassName) && nested.endsWith(".class");
        }).sorted();
    }

    private static class TestClassLoader extends ClassLoader {

        public TestClassLoader() {
            super(AbstractConversionHandlerTest.class.getClassLoader());
        }

        public Class<?> defineClass(byte[] bytes) {
            return defineClass(null, bytes, 0, bytes.length);
        }
    }
}
