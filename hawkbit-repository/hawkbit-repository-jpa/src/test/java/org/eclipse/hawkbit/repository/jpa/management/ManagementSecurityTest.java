/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
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
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.stream.Stream;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.auth.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.PermissionSupport;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.TenantStatsManagement;
import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.test.util.AbstractIntegrationTest;
import org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.spel.ast.MethodReference;
import org.springframework.expression.spel.ast.OpAnd;
import org.springframework.expression.spel.ast.OpOr;
import org.springframework.expression.spel.ast.StringLiteral;
import org.springframework.expression.spel.ast.VariableReference;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

@Slf4j
@TestPropertySource(properties = { "logging.level.org.eclipse.hawkbit.repository.test.util=off" })
class ManagementSecurityTest extends AbstractJpaIntegrationTest {

    private static final SpelExpressionParser SPEL_EXPRESSION_PARSER = new SpelExpressionParser();

    @Autowired
    protected TenantStatsManagement tenantStatsManagement;
    @Autowired
    protected TenantConfigurationManagement tenantConfigurationManagement;

    @Override
    @BeforeEach
    public void beforeAll() {
        // override - shall not do anything
    }

    @ParameterizedTest
    @MethodSource("testMethods")
    void testMethod(final Class<?> managementInterface, final Method managementInterfaceMethod) {
        final Object managementObject = TenantStatsManagement.class == managementInterface
                ? tenantStatsManagement // it's not a field of AbstractIntegrationTest, so we need to use the autowired instance
                : TenantConfigurationManagement.class == managementInterface
                        ? tenantConfigurationManagement // it's not a field of AbstractIntegrationTest, so we need to use the autowired instance
                        : Stream
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
        if (implementationMethod == null) {
            throw new AssertionError("No management implementation found for " + managementInterfaceMethod + " in " + managedClass.getName());
        }
        final String permissionGroup = managementObject instanceof PermissionSupport permissionSupport
                ? permissionSupport.permissionGroup()
                : null;
        Set<String> preAuthorizedPermissions = collectPreAuthorizedPermissions(implementationMethod, permissionGroup);
        if (ObjectUtils.isEmpty(preAuthorizedPermissions)) {
            preAuthorizedPermissions = collectPreAuthorizedPermissions(managementInterfaceMethod, permissionGroup);
        }
        if (ObjectUtils.isEmpty(preAuthorizedPermissions)) {
            fail("No PreAuthorize annotation found for " + managementInterface.getSimpleName() + " -> " + implementationMethod);
        } else {
            assertPermissionsCheck(managementInterfaceMethod, managementObject, preAuthorizedPermissions.toArray(new String[0]));
        }
    }

    private static Stream<Arguments> testMethods() {
        final String packageName = "org.eclipse.hawkbit.repository";
        try (final ScanResult scanResult = new ClassGraph().acceptPackages(packageName).scan()) {
            return scanResult.getAllClasses()
                    .stream()
                    // scan scans subpackages as well, so we need to filter out the classes that are not in the package (e.g. JpaActionManagement)
                    .filter(classInPackage -> classInPackage.getPackageName().equals(packageName))
                    .filter(classInPackage -> classInPackage.getSimpleName().endsWith("Management"))
                    // RepositoryManagement is not a management interface but a super of such interfaces
                    .filter(classInPackage -> !classInPackage.getSimpleName().equals("RepositoryManagement"))
                    // QuotaManagement and its implementation PropertiesQuotaManagement is not protected using @PreAuthorize
                    // it is not an exposed db service but internally used
                    .filter(classInPackage -> !classInPackage.getSimpleName().equals("QuotaManagement") &&
                            !classInPackage.getSimpleName().equals("PropertiesQuotaManagement"))
                    .map(ClassInfo::loadClass)
                    .flatMap(clazz -> collectMethods(clazz, new ArrayList<>()).stream()
                            // permissionGroup is an internal method and should not be protected by @PreAuthorize
                            .filter(method -> !"permissionGroup".equals(method.getName()) ||
                                    method.getParameterCount() != 0 ||
                                    method.getReturnType() != String.class)
                            // jacoco adds some methods with bytecode instrumentation
                            .filter(method -> !"$jacocoInit".equals(method.getName()))
                            // skip maxAssignmentsExceededHandler in DeploymentManagement since it throws quota exception
                            // because of action.cleanup.onQuotaHit.percent not configured
                            // other option would be to configure it for all tests
                            .filter(method -> !"handleMaxAssignmentsExceeded".equals(method.getName()))
                            .map(method -> Arguments.of(clazz, method)))
                    // consumes the stream because scan result couldn't be used after being closed
                    .toList()
                    .stream();
        }
    }

    private static List<Method> collectMethods(final Class<?> clazz, final List<Method> methods) {
        methods.addAll(Arrays.asList(clazz.getDeclaredMethods()));
        for (final Class<?> interfaceClass : clazz.getInterfaces()) {
            collectMethods(interfaceClass, methods);
        }
        if (clazz.getSuperclass() != null) {
            collectMethods(clazz.getSuperclass(), methods);
        }
        return methods;
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

    private Set<String> collectPreAuthorizedPermissions(final Method method, final String permissionGroup) {
        if (method.isAnnotationPresent(PreAuthorize.class)) {
            final PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
            final SpelExpression expr = (SpelExpression) SPEL_EXPRESSION_PARSER.parseExpression(preAuthorize.value());
            final Set<String> expressionPermissions = new HashSet<>();
            addSufficientPermissions(expr.getAST(), expressionPermissions, permissionGroup);
            if (expressionPermissions.isEmpty()) {
                throw new IllegalStateException("No permissions found in expression: " + preAuthorize.value());
            }
            return expressionPermissions;
        } else {
            return null;
        }
    }

    private void addSufficientPermissions(final SpelNode spelNode, final Set<String> preAuthorizedPermissions, final String permissionGroup) {
        if (spelNode instanceof OpOr) {
            addSufficientPermissions(spelNode.getChild(0), preAuthorizedPermissions, permissionGroup);
        } else if (spelNode instanceof OpAnd) {
            for (int i = 0; i < spelNode.getChildCount(); i++) {
                addSufficientPermissions(spelNode.getChild(i), preAuthorizedPermissions, permissionGroup);
            }
        } else if (spelNode instanceof MethodReference methodReference) {
            final String method = methodReference.getName();
            switch (method) {
                case "hasAuthority" -> {
                    for (int i = 0; i < spelNode.getChildCount(); i++) {
                        addSufficientPermissions(spelNode.getChild(i), preAuthorizedPermissions, permissionGroup);
                    }
                }
                case "hasAnyRole" -> {
                    final SpelNode child = spelNode.getChild(0);
                    if (child instanceof StringLiteral literal) {
                        final String permission = (String) literal.getLiteralValue().getValue();
                        preAuthorizedPermissions.add(permission.toUpperCase().startsWith("ROLE_") ? permission : "ROLE_" + permission);
                    } else {
                        addSufficientPermissions(child, preAuthorizedPermissions, permissionGroup);
                    }
                }
                case "hasPermission" -> {
                    assertThat(spelNode.getChildCount()).isEqualTo(2);
                    assertThat(spelNode.getChild(0) instanceof VariableReference varRef && varRef.toStringAST().equals("#root")).isTrue();
                    assertThat(spelNode.getChild(1)).isInstanceOf(StringLiteral.class);
                    final StringLiteral literal = (StringLiteral) spelNode.getChild(1);
                    preAuthorizedPermissions.add(String.valueOf(literal.getLiteralValue().getValue())
                            .replace(SpringEvalExpressions.PERMISSION_GROUP_PLACEHOLDER, permissionGroup));
                }
                default -> throw new IllegalStateException("Unexpected MethodReference: " + method);
            }
        } else if (spelNode instanceof StringLiteral literal) {
            preAuthorizedPermissions.add((String) literal.getLiteralValue().getValue());
        } else {
            throw new IllegalStateException("Unexpected SpelNode: " + spelNode + " of type " + spelNode.getClass());
        }
    }

    @SneakyThrows
    @SuppressWarnings("rawtypes")
    private Object instance(final Class<?> clazz) {
        if (clazz.isArray()) {
            return Array.newInstance(clazz.getComponentType(), 0);
        }

        if (Collection.class.isAssignableFrom(clazz)) {
            if (clazz == List.class || clazz == Collection.class) {
                return new ArrayList<>();
            } else if (clazz == Set.class) {
                return new HashSet<>();
            } else {
                throw new IllegalStateException("No instance for collection interface " + clazz);
            }
        }

        if (clazz == Map.class) {
            return new HashMap<>();
        }

        if (clazz.isInterface()) {
            if (clazz == Pageable.class) {
                return Pageable.ofSize(10);
            } else if (clazz == Serializable.class) {
                return "";
            } else if (clazz == Consumer.class) {
                return (Consumer<String>) s -> {};
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
        } else if (clazz == URI.class) {
            return new URI("http://localhost");
        } else if (clazz == Class.class) {
            return String.class;
        } else {
            try {
                final Constructor[] constructors = clazz.getDeclaredConstructors();
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
            } catch (final InstantiationException e) {
                // try builder pattern
                try {
                    final Object builder = clazz.getDeclaredMethod("builder").invoke(null);
                    final Method build = builder.getClass().getDeclaredMethod("build");
                    build.setAccessible(true);
                    return build.invoke(builder);
                } catch (final NoSuchMethodException | IllegalAccessException | InvocationTargetException e1) {
                    log.debug("{} is not a builder. Throws could not instantiate", clazz.getName());
                }
                log.error("Could not instantiate {}", clazz.getName(), e);
                throw e;
            }
        }
    }

    private static final Set<String> EXPECTED_EXCEPTIONS_TYPES = new HashSet<>();

    @AfterAll
    static void afterAll() {
        final List<String> exceptions = new ArrayList<>(EXPECTED_EXCEPTIONS_TYPES);
        Collections.sort(exceptions);
        log.info("Expected exceptions occurred during tests:\n\t{}", String.join("\n\t", exceptions));
    }

    @SneakyThrows
    protected void assertPermissionsCheck(final Method managementInterfaceMethod, final Object managementObject, final String... permissions) {
        final Callable<?> callable = () -> {
            try {
                final Object[] params = new Object[managementInterfaceMethod.getParameterCount()];
                for (int i = 0; i < params.length; i++) {
                    params[i] = instance(managementInterfaceMethod.getParameterTypes()[i]);
                }
                return managementInterfaceMethod.invoke(managementObject, params);
            } catch (final InvocationTargetException e) {
                if (e.getCause() instanceof RuntimeException re) {
                    throw re;
                } else {
                    throw new AssertionError(e.getCause());
                }
            }
        };

        // check if the user has the correct permissions
        SecurityContextSwitch.runAs(SecurityContextSwitch.withUser("user_with_permissions", permissions), () -> {
            try {
                callable.call();
            } catch (final Throwable th) {
                if (th instanceof InsufficientPermissionException || th instanceof AuthorizationDeniedException) {
                    throw new AssertionError(
                            "Expected no InsufficientPermissionException or AuthorizationDeniedException to be thrown, but got: " + th +
                                    " (permissions: " + Arrays.toString(permissions) + ")", th);
                } else {
                    Stream.of(th.getStackTrace())
                            .filter(stackTraceElement -> {
                                // if the method seem to exist in the stack trace
                                try {
                                    final Class<?> clazz = Class.forName(stackTraceElement.getClassName());
                                    return clazz.isAssignableFrom(managementObject.getClass()) && // in class or implementation in hierarchy
                                            stackTraceElement.getMethodName().equals(managementInterfaceMethod.getName()); //
                                } catch (final ClassNotFoundException e) {
                                    return false;
                                }
                            })
                            .findAny()
                            .orElseThrow(() -> new AssertionError(
                                    "Unexpected Exception is thrown (permissions: " + Arrays.toString(permissions) + ")", th));
                    EXPECTED_EXCEPTIONS_TYPES.add(th.getClass().getName());
                    log.debug("Expected catch: {}", th.getMessage());
                }
            }
        });

        // check if the user has not the correct permissions
        final String[] permissionsWithoutOne = new String[permissions.length - 1];
        System.arraycopy(permissions, 0, permissionsWithoutOne, 0, permissionsWithoutOne.length);
        SecurityContextSwitch.runAs(SecurityContextSwitch.withUser("user_without_permissions", permissionsWithoutOne), () -> {
            try {
                callable.call();
                throw new AssertionError(
                        "Expected Exception InsufficientPermissionException to be thrown, but request passed with no exception" +
                                " (permissions: " + Arrays.toString(permissionsWithoutOne) + ", needed: " + Arrays.asList(permissions) + ")");
            } catch (final Exception ex) {
                // default interface methods as TargetManagement.getWithAutoConfigurationStatus are not handled to
                // throw InsufficientPermissionException
                assertThat(ex).isInstanceOfAny(InsufficientPermissionException.class, AuthorizationDeniedException.class);
            }
        });
    }
}