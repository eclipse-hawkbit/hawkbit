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
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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
            if (!METHOD_SECURITY_EXCLUSION.contains(method) && !method.isSynthetic()
                    && Modifier.isPublic(method.getModifiers())) {
                final PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
                assertThat(annotation).as("The public method " + method.getName() + " in class " + clazz.getName()
                        + " is not annoated with @PreAuthorize, security leak?").isNotNull();
            }
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
        final List<Class<?>> interfacesToReturn = new ArrayList<>();
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        final Enumeration<URL> resources = classLoader.getResources(p.getName().replace(".", "/"));
        while (resources.hasMoreElements()) {
            final File packageDirectory = new File(resources.nextElement().getFile());
            final File[] filesInPackage = packageDirectory.listFiles();
            for (final File classFile : filesInPackage) {
                final String classNameWithExtension = classFile.getName();
                final int indexOfExtension = classNameWithExtension.indexOf(".class");
                if (indexOfExtension > 0) {
                    final String classNameWithoutExtension = classNameWithExtension.substring(0, indexOfExtension);
                    if (includeFilter.matcher(classNameWithoutExtension).matches()) {
                        final Class<?> classInPackage = Class.forName(p.getName() + "." + classNameWithoutExtension);
                        if (classInPackage.isInterface()) {
                            interfacesToReturn.add(classInPackage);
                        }
                    }
                }
            }
        }
        return interfacesToReturn;
    }

    private static Method getMethod(final Class<?> clazz, final String methodName, final Class<?>... parameterTypes) {
        try {
            return clazz.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException | SecurityException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
