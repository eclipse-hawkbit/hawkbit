/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import static org.fest.assertions.Assertions.assertThat;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Unit Tests - Repository")
@Stories("Security Test")
public class RepositoryManagementMethodPreAuthorizeAnnotatedTest {

    private static final Set<Method> METHOD_SECURITY_EXCLUSION = new HashSet<>();

    static {
        METHOD_SECURITY_EXCLUSION.add(getMethod(SystemManagement.class, "currentTenant"));
    }

    @Test
    @Description("Verfies that repository methods are @PreAuthorize annotated")
    public void repositoryManagementMethodsArePreAuthorizedAnnotated()
            throws ClassNotFoundException, URISyntaxException, IOException {
        final List<Class<?>> findInterfacesInPackage = findInterfacesInPackage(getClass().getPackage(),
                Pattern.compile(".*Management"));

        assertThat(findInterfacesInPackage).isNotEmpty();
        for (final Class<?> interfaceToCheck : findInterfacesInPackage) {
            assertDeclaredMethodsContainsPreAuthorizeAnnotaions(interfaceToCheck);
        }

        // all exclusion should be used, otherwise the method exlusion should be
        // cleaned up again
        assertThat(METHOD_SECURITY_EXCLUSION).isEmpty();
    }

    /**
     * asserts that the given methods are annotated with the
     * {@link PreAuthorize} annotation for security. Inherited methods are not
     * checked. The following methods are excluded due inherited from
     * {@link Object}, like equals() or toString().
     * 
     * @param clazz
     *            the class to retrieve the public declared methods
     */
    private static void assertDeclaredMethodsContainsPreAuthorizeAnnotaions(final Class<?> clazz) {
        final Method[] declaredMethods = clazz.getDeclaredMethods();
        for (final Method method : declaredMethods) {
            final boolean methodExcluded = METHOD_SECURITY_EXCLUSION.contains(method);
            if (methodExcluded || method.isSynthetic() || Modifier.isPublic(method.getModifiers())) {
                // skip method because it should be excluded
                METHOD_SECURITY_EXCLUSION.remove(method);
                continue;
            }
            final PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
            assertThat(annotation).as("The public method " + method.getName() + " in class " + clazz.getName()
                    + " is not annoated with @PreAuthorize, security leak?").isNotNull();
        }
    }

    /**
     * Finds all interfaces in a given packages which matches the given filter.
     * 
     * @param p
     *            the package to search for interfaces in
     * @param includeFilter
     *            the pattern which interfaces class names should be included
     * @return a list of loaded interfaces in a specific package and matches the
     *         given filter
     * @throws URISyntaxException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private List<Class<?>> findInterfacesInPackage(final Package p, final Pattern includeFilter)
            throws URISyntaxException, IOException, ClassNotFoundException {
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        final Enumeration<URL> resources = classLoader.getResources(p.getName().replace(".", "/"));
        final RegexIncludeInterfaceFileCollector regexIncludeInterfaceFileCollector = new RegexIncludeInterfaceFileCollector(
                p, includeFilter);
        while (resources.hasMoreElements()) {
            listFilesInPackage(resources.nextElement(), regexIncludeInterfaceFileCollector);
        }
        return regexIncludeInterfaceFileCollector.getInterfaceClasses();
    }

    private File[] listFilesInPackage(final URL resource, final RegexIncludeInterfaceFileCollector clazzCollector)
            throws URISyntaxException {
        final String packagePath = new URI(resource.toString()).getPath();
        if (packagePath != null) {
            final File packageDirectory = new File(packagePath);
            final File[] filesInPackage = packageDirectory.listFiles(clazzCollector);
            return filesInPackage;
        }
        return new File[0];
    }

    private static Method getMethod(final Class<?> clazz, final String methodName, final Class<?>... parameterTypes) {
        try {
            return clazz.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException | SecurityException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private static final class RegexIncludeInterfaceFileCollector implements FileFilter {

        private final List<Class<?>> interfaceClasses = new ArrayList<>();
        private final Pattern includeFilter;
        private final Package basePackage;

        public RegexIncludeInterfaceFileCollector(final Package basePackage, final Pattern pattern) {
            this.basePackage = basePackage;
            this.includeFilter = pattern;
        }

        @Override
        public boolean accept(final File pathname) {
            final String classNameWithExtension = pathname.getName();
            final int indexOfExtension = classNameWithExtension.indexOf(".class");
            if (indexOfExtension == -1) {
                return false;
            }
            final String classNameWithoutExtension = classNameWithExtension.substring(0, indexOfExtension);
            if (!includeFilter.matcher(classNameWithoutExtension).matches()) {
                return false;
            }

            try {
                final Class<?> classInPackage = Class.forName(basePackage.getName() + "." + classNameWithoutExtension);
                if (classInPackage.isInterface()) {
                    interfaceClasses.add(classInPackage);
                }
            } catch (final ClassNotFoundException e) {
                // don't need to handle here
            }
            return false;
        }

        public List<Class<?>> getInterfaceClasses() {
            return interfaceClasses;
        }
    }
}
