/**
 * Copyright (c) 2022 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.repository.RepositoryManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.test.util.AbstractIntegrationTest;
import org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.domain.Pageable;
import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.spel.ast.MethodReference;
import org.springframework.expression.spel.ast.OpAnd;
import org.springframework.expression.spel.ast.OpOr;
import org.springframework.expression.spel.ast.StringLiteral;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

@Slf4j
@TestPropertySource(properties = {
        "logging.level.org.eclipse.hawkbit.repository.test.util=off" })
class ManagementSecurityTest extends AbstractJpaIntegrationTest {

    private static final SpelExpressionParser SPEL_EXPRESSION_PARSER = new SpelExpressionParser();

    @ParameterizedTest
    @MethodSource("testMethods")
    void testMethod(final Class<?> managementInterface, final Method managementInterfaceMethod) {
        final Object managementObject = Stream
                .of(AbstractIntegrationTest.class.getDeclaredFields())
                .filter(field -> managementInterface.isAssignableFrom(field.getType()))
                .findFirst()
                .map(field -> {
                    field.setAccessible(true);
                    try {
                        return field.get(this);
                    } catch (final IllegalAccessException e) {
                        throw new AssertionError("Could not access field " + field.getName(), e);
                    }
                })
                .orElseThrow(() -> new AssertionError("No management implementation found for " + managementInterface));
        final Class<?> managedClass = ClassUtils.getUserClass(managementObject); // managed class is a proxy
        final Method implementationMethod = findImplementationMethod(managedClass, managementInterfaceMethod);
        Set<String> preAuthorizedPermissions = collectPreAuthorizedPermissions(implementationMethod);
        if (ObjectUtils.isEmpty(preAuthorizedPermissions)) {
            preAuthorizedPermissions = collectPreAuthorizedPermissions(managementInterfaceMethod);
        }
        if (ObjectUtils.isEmpty(preAuthorizedPermissions)) {
            fail("No PreAuthorize annotation found for " + getClass().getSimpleName() + " -> " + implementationMethod);
        } else {
            assertPermissionsCheck(
                    () -> {
                        try {
                            implementationMethod.invoke(
                                    managementObject,
                                    Stream.of(managementInterfaceMethod.getGenericParameterTypes())
                                            .map(type -> {
                                                if (RepositoryManagement.class.isAssignableFrom(managedClass) && type instanceof TypeVariable<?> typeVariable) {
                                                    // hard to make full discovery so - custom
                                                    ParameterizedType parameterizedType = null;
                                                    for (final Type superInterface : managedClass.getGenericInterfaces()) {
                                                        if (superInterface instanceof ParameterizedType) {
                                                            parameterizedType = (ParameterizedType) superInterface;
                                                        } else {
                                                            for (final Type superInterface2 : ((Class) superInterface).getGenericInterfaces()) {
                                                                if (superInterface2 instanceof ParameterizedType) {
                                                                    parameterizedType = (ParameterizedType) superInterface2;
                                                                }
                                                            }
                                                        }
                                                    }
                                                    if (parameterizedType == null) {
                                                        throw new IllegalStateException("No parameterized type found for " + managedClass);
                                                    } else { // RepositoryManagement
                                                        if ("T".equals(typeVariable.getName())) {
                                                            return parameterizedType.getActualTypeArguments()[0];
                                                        } else if ("C".equals(typeVariable.getName())) {
                                                            return parameterizedType.getActualTypeArguments()[1];
                                                        } else if ("U".equals(typeVariable.getName())) {
                                                            return parameterizedType.getActualTypeArguments()[2];
                                                        } else {
                                                            throw new IllegalStateException("Unknown type variable: " + type);
                                                        }
                                                    }
                                                } else {
                                                    return type;
                                                }
                                            })
                                            .map(this::instance)
                                            .toArray());
                        } catch (final InvocationTargetException e) {
                            if (e.getCause() instanceof RuntimeException re) {
                                throw re;
                            } else {
                                throw new AssertionError(e.getCause());
                            }
                        }
                        return null;
                    },
                    preAuthorizedPermissions.toArray(new String[0]));
        }
    }

    private static Stream<Arguments> testMethods() {
        final String packageName = "org.eclipse.hawkbit.repository";
        try (final ScanResult scanResult = new ClassGraph().acceptPackages(packageName).scan()) {
            return scanResult.getAllClasses()
                    .stream()
                    // scan scans subpackages as well, so we need to filter out the classes that are not in the package
                    .filter(classInPackage -> classInPackage.getPackageName().equals(packageName))
                    .filter(classInPackage -> classInPackage.getSimpleName().endsWith("Management"))
                    // TenantStatsManagement and PropertiesQuotaManagement do not present in super class
                    .filter(classInPackage ->
                            !classInPackage.getSimpleName().equals("TenantStatsManagement") &&
                                    !classInPackage.getSimpleName().equals("PropertiesQuotaManagement"))
                    // RepositoryManagement is not a management interface but a super of such
                    .filter(classInPackage -> !classInPackage.getSimpleName().equals("RepositoryManagement"))
                    // QuotaManagement is not protected using @PreAuthorize
                    .filter(classInPackage -> !classInPackage.getSimpleName().equals("QuotaManagement"))
                    .map(ClassInfo::loadClass)
                    .flatMap(clazz -> {
                        final List<Method> methods = new ArrayList<>();
                        collectMethods(clazz, methods);
                        return methods.stream()
                                // sometimes there is $jacocoInit
                                .filter(method -> (method.getModifiers() ^ Modifier.PUBLIC) != 0)
                                // TODO - remove start, selects single method
//                                .filter(method -> clazz.getSimpleName().equals("TargetManagement") && method.getName().equals("countByAssignedDistributionSet"))
//                                .filter(method -> clazz.getSimpleName().equals("DistributionSetManagement") && method.getName().equals("create"))
                                // TODO - remove end
                                .map(method -> Arguments.of(clazz, method));
                    })
                    // consumes the stream because scan result couldn't be used after being closed
                    .toList()
                    .stream();
        }
    }

    private static void collectMethods(final Class<?> clazz, final List<Method> methods) {
        if (clazz.getSuperclass() != null) {
            collectMethods(clazz.getSuperclass(), methods);
        }
        for (final Class<?> interfaceClass : clazz.getInterfaces()) {
            collectMethods(interfaceClass, methods);
        }
        methods.addAll(Arrays.asList(clazz.getDeclaredMethods()));
    }

    private static Method findImplementationMethod(final Class<?> managementClass, final Method managementInterfaceMethod) {
        final Method classMethod = findClassImplementationMethod(managementClass, managementInterfaceMethod);
        if (classMethod == null) {
            return findInterfaceDefaultMethod(managementClass, managementInterfaceMethod);
        } else {
            return classMethod;
        }
    }

    private static Method findClassImplementationMethod(final Class<?> managementClass, final Method managementInterfaceMethod) {
        return Stream.of(managementClass.getDeclaredMethods())
                .filter(m -> match(m, managementInterfaceMethod))
                .findFirst()
                .orElseGet(() -> {
                    final Class<?> superClass = managementClass.getSuperclass();
                    if (superClass == null) {
                        return null;
                    } else {
                        return findImplementationMethod(superClass, managementInterfaceMethod);
                    }
                });
    }

    private static Method findInterfaceDefaultMethod(final Class<?> managementClassOrInterface, final Method managementInterfaceMethod) {
        if (!managementInterfaceMethod.getDeclaringClass().isAssignableFrom(managementClassOrInterface)) {
            return null;
        }
        Method interfaceMethod = null;
        for (final Class<?> superInterface : managementClassOrInterface.getInterfaces()) {
            final Method method = Stream.of(superInterface.getDeclaredMethods())
                    .filter(Method::isDefault)
                    .filter(m -> match(m, managementInterfaceMethod))
                    .findFirst()
                    .orElseGet(() -> findInterfaceDefaultMethod(superInterface, managementInterfaceMethod));
            if (method != null) { // found
                if (interfaceMethod != null) {
                    // should not happen, but check anyway
                    throw new IllegalStateException(
                            "Multiple default methods found for " + managementInterfaceMethod + " in interfaces: " + interfaceMethod + " and " + method);
                }
                interfaceMethod = method;
            }
        }
        return interfaceMethod;
    }

    private static boolean match(final Method method, final Method managementInterfaceMethod) {
        return method.getName().equals(managementInterfaceMethod.getName()) &&
                // TODO - check for generics
                Arrays.equals(method.getParameterTypes(), managementInterfaceMethod.getParameterTypes());
    }

    private Set<String> collectPreAuthorizedPermissions(final Method method) {
        if (method.isAnnotationPresent(PreAuthorize.class)) {
            final PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
            final SpelExpression expr = (SpelExpression) SPEL_EXPRESSION_PARSER.parseExpression(preAuthorize.value());
            final Set<String> expressionPermissions = new HashSet<>();
            addSufficientPermissions(expr.getAST(), expressionPermissions);
            if (expressionPermissions.isEmpty()) {
                throw new IllegalStateException("No permissions found in expression: " + preAuthorize.value());
            }
            return expressionPermissions;
        } else {
            return null;
        }
    }

    private void addSufficientPermissions(final SpelNode spelNode, final Set<String> preAuthorizedPermissions) {
        if (spelNode instanceof OpOr) {
            addSufficientPermissions(spelNode.getChild(0), preAuthorizedPermissions);
        } else if (spelNode instanceof OpAnd) {
            for (int i = 0; i < spelNode.getChildCount(); i++) {
                addSufficientPermissions(spelNode.getChild(i), preAuthorizedPermissions);
            }
        } else if (spelNode instanceof MethodReference methodReference) {
            final String method = methodReference.getName();
            if ("hasAuthority".equals(method)) {
                for (int i = 0; i < spelNode.getChildCount(); i++) {
                    addSufficientPermissions(spelNode.getChild(i), preAuthorizedPermissions);
                }
            } else if ("hasAnyRole".equals(method)) {
                final SpelNode child = spelNode.getChild(0);
                if (child instanceof StringLiteral literal) {
                    String permission = (String) literal.getLiteralValue().getValue();
                    if (permission.toUpperCase().startsWith("ROLE_")) {
                        permission = permission.substring(5);
                    }
                    preAuthorizedPermissions.add(permission);
                } else {
                    addSufficientPermissions(child, preAuthorizedPermissions);
                }
            } else {
                throw new IllegalStateException("Unexpected MethodReference: " + method);
            }
        } else if (spelNode instanceof StringLiteral literal) {
            preAuthorizedPermissions.add((String) literal.getLiteralValue().getValue());
        } else {
            throw new IllegalStateException("Unexpected SpelNode: " + spelNode + " of type " + spelNode.getClass());
        }
    }

    @SneakyThrows
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Object instance(final Type type) {
        final Class clazz = type instanceof ParameterizedType parameterizedType ? (Class) parameterizedType.getRawType() :
                type instanceof TypeVariable ? Object.class : (Class) type;
        if (clazz.isArray()) {
            final Object array = Array.newInstance(clazz.getComponentType(), 1);
            Array.set(array, 0, instance(clazz.getComponentType()));
        }

        if (Collection.class.isAssignableFrom(clazz)) {
            final Collection collection;
            if (clazz == List.class || clazz == Collection.class) {
                collection = new ArrayList<>();
            } else if (clazz == Set.class) {
                collection = new HashSet<>();
            } else {
                throw new IllegalStateException("No instance for collection interface " + clazz);
            }
            if (type instanceof ParameterizedType parameterizedType) {
                collection.add(instance(parameterizedType.getActualTypeArguments()[0]));
            }
            return collection;
        }

        if (clazz == Map.class) {
            final Map map = new HashMap<>();
            if (type instanceof ParameterizedType parameterizedType) {
                map.put(instance(parameterizedType.getActualTypeArguments()[0]), instance(parameterizedType.getActualTypeArguments()[1]));
            }
            return map;
        }

        if (clazz.isInterface()) {
            if (clazz == Pageable.class) {
                return Pageable.ofSize(10);
            } else if (clazz.getPackageName().startsWith("org.eclipse.hawkbit.repository")) {
                if (clazz.getSimpleName().endsWith("Management")) {
                    return Stream
                            .of(AbstractIntegrationTest.class.getDeclaredFields())
                            .filter(field -> clazz.isAssignableFrom(field.getType()))
                            .findFirst()
                            .map(field -> {
                                field.setAccessible(true);
                                try {
                                    return field.get(this);
                                } catch (final IllegalAccessException e) {
                                    throw new AssertionError("Could not access field " + field.getName(), e);
                                }
                            })
                            .orElseThrow(() -> new IllegalStateException("No management implementation found for " + clazz));
                }
                try (final ScanResult scanResult = new ClassGraph().acceptPackages("org.eclipse.hawkbit.repository").scan()) {
                    return scanResult.getClassesImplementing(clazz)
                            .stream()
                            .filter(impl -> !impl.isAbstract())
                            .findFirst()
                            .map(impl -> instance(impl.loadClass()))
                            .orElseThrow(() -> new IllegalStateException("No instance for interface " + clazz));
                }
            } else {
                throw new IllegalStateException("No instance for interface " + clazz);
            }
        }

        if (clazz.isEnum()) {
            return clazz.getEnumConstants()[0];
        }

        if (clazz == boolean.class || clazz == Boolean.class) {
            return false;
        } else if (clazz == int.class || clazz == Integer.class) {
            return 1;
        } else if (clazz == long.class || clazz == Long.class) {
            return 1L;
        } else if (clazz == float.class || clazz == Float.class) {
            return 1.0f;
        } else if (clazz == double.class || clazz == Double.class) {
            return 1.0;
        } else if (clazz == short.class || clazz == Short.class) {
            return (short) 1;
        } else if (clazz == byte.class || clazz == Byte.class) {
            return (byte) 1;
        } else if (clazz == char.class || clazz == Character.class) {
            return 'a';
        } else if (clazz == String.class) {
            return "id==0";
        } else if (clazz == InputStream.class) {
            return new ByteArrayInputStream(new byte[1]);
        } else {
            final Constructor[] constructors = clazz.getDeclaredConstructors();
            if (ObjectUtils.isEmpty(constructors)) {
                throw new IllegalStateException("No public constructor found for " + clazz);
            }
            // prefer empty constructor
            for (final Constructor constructor : constructors) {
                if (constructor.getParameterCount() == 0) {
                    constructor.setAccessible(true);
                    return constructor.newInstance();
                }
            }
            constructors[0].setAccessible(true);
            return constructors[0].newInstance(Stream.of(constructors[0].getParameterTypes())
                    .map(this::instance)
                    .toArray());
        }
    }

    @SneakyThrows
    protected void assertPermissionsCheck(final Callable<?> callable, final String... permissions) {
        // check if the user has the correct permissions
        SecurityContextSwitch.runAs(SecurityContextSwitch.withUser("user_with_permissions", permissions), () -> {
            try {
                callable.call();
            } catch (final Throwable th) {
                if (th instanceof EntityNotFoundException) {
                    log.info("Expected (at most) EntityNotFoundException catch: {}", th.getMessage());
                } else {
                    throw new AssertionError(
                            "Expected no Exception (other then EntityNotFound) to be thrown, but got: " + th +
                                    " (permissions: " + Arrays.toString(permissions) + ")", th);
                }
            }
        });
        if (permissions.length > 0) {
            // check if the user has not the correct permissions
            final String[] permissionsWithoutOne = new String[permissions.length - 1];
            System.arraycopy(permissions, 0, permissionsWithoutOne, 0, permissionsWithoutOne.length);
            SecurityContextSwitch.runAs(SecurityContextSwitch.withUser("user_without_permissions", permissionsWithoutOne), () -> {
                try {
                    callable.call();
                    throw new AssertionError(
                            "Expected Exception InsufficientPermissionException to be thrown, but request passed with no exception" +
                                    " (permissions: " + Arrays.toString(permissionsWithoutOne) + ", needed: " + Arrays.asList(
                                    permissions) + ")");
                } catch (final Exception ex) {
                    assertThat(ex).isInstanceOf(InsufficientPermissionException.class);
                }
            });
        }
    }
}