/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.resource;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Metamodel;

import org.eclipse.hawkbit.repository.FieldNameProvider;
import org.eclipse.hawkbit.repository.SoftwareModuleFields;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.rsql.RSQLParameterSyntaxException;
import org.eclipse.hawkbit.repository.rsql.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.rsql.RSQLUtility;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@RunWith(MockitoJUnitRunner.class)
@Features("Component Tests - Management RESTful API")
@Stories("RSQL search utility")
// TODO: fully document tests -> @Description for long text and reasonable
// method name as short text
public class RSQLUtilityTest {

    @Mock
    private Root<Object> baseSoftwareModuleRootMock;

    @Mock
    private CriteriaQuery<SoftwareModule> criteriaQueryMock;
    @Mock
    private CriteriaBuilder criteriaBuilderMock;
    @Mock
    private EntityManager entityManager;

    @Mock
    private Metamodel metamodel;

    @Mock
    private ManagedType managedType;

    @Mock
    private Attribute attribute;

    @Test(expected = RSQLParameterSyntaxException.class)
    public void wrongRsqlSyntaxThrowSyntaxException() {
        final String wrongRSQL = "name==abc;d";
        when(entityManager.getMetamodel()).thenReturn(metamodel);
        RSQLUtility.parse(wrongRSQL, SoftwareModuleFields.class, entityManager).toPredicate(baseSoftwareModuleRootMock,
                criteriaQueryMock, criteriaBuilderMock);
    }

    @Test(expected = RSQLParameterUnsupportedFieldException.class)
    public void wrongFieldThrowUnsupportedFieldException() {
        final String wrongRSQL = "unknownField==abc";
        when(baseSoftwareModuleRootMock.getJavaType()).thenReturn((Class) SoftwareModule.class);
        doEntitySetup(SoftwareModule.class);
        RSQLUtility.parse(wrongRSQL, SoftwareModuleFields.class, entityManager).toPredicate(baseSoftwareModuleRootMock,
                criteriaQueryMock, criteriaBuilderMock);
    }

    @Test
    public <T> void correctRsqlBuildsPredicate() {
        reset(baseSoftwareModuleRootMock, criteriaQueryMock, criteriaBuilderMock);
        final String correctRsql = "name==abc;version==1.2";
        when(baseSoftwareModuleRootMock.get("name")).thenReturn(baseSoftwareModuleRootMock);
        when(baseSoftwareModuleRootMock.get("version")).thenReturn(baseSoftwareModuleRootMock);
        when(baseSoftwareModuleRootMock.getJavaType()).thenReturn((Class) SoftwareModule.class);
        when(criteriaBuilderMock.equal(any(Root.class), anyString())).thenReturn(mock(Predicate.class));
        when(criteriaBuilderMock.<String> greaterThanOrEqualTo(any(Expression.class), any(String.class))).thenReturn(
                mock(Predicate.class));

        doEntitySetup(SoftwareModule.class);

        // test
        RSQLUtility.parse(correctRsql, SoftwareModuleFields.class, entityManager).toPredicate(
                baseSoftwareModuleRootMock, criteriaQueryMock, criteriaBuilderMock);

        // verfication
        verify(criteriaBuilderMock, times(1)).and(any(Predicate.class));
    }

    @Test
    public void correctRsqlBuildsNotEqualPredicate() {
        reset(baseSoftwareModuleRootMock, criteriaQueryMock, criteriaBuilderMock);
        final String correctRsql = "name!=abc";
        when(baseSoftwareModuleRootMock.get("name")).thenReturn(baseSoftwareModuleRootMock);
        when(baseSoftwareModuleRootMock.getJavaType()).thenReturn((Class) SoftwareModule.class);
        when(criteriaBuilderMock.equal(any(Root.class), anyString())).thenReturn(mock(Predicate.class));
        when(criteriaBuilderMock.<String> greaterThanOrEqualTo(any(Expression.class), any(String.class))).thenReturn(
                mock(Predicate.class));
        doEntitySetup(SoftwareModule.class);
        // test
        RSQLUtility.parse(correctRsql, SoftwareModuleFields.class, entityManager).toPredicate(
                baseSoftwareModuleRootMock, criteriaQueryMock, criteriaBuilderMock);

        // verfication
        verify(criteriaBuilderMock, times(1)).and(any(Predicate.class));
        verify(criteriaBuilderMock, times(1)).notEqual(eq(baseSoftwareModuleRootMock), eq("abc"));
    }

    @Test
    public void correctRsqlBuildsLessThanPredicate() {
        reset(baseSoftwareModuleRootMock, criteriaQueryMock, criteriaBuilderMock);
        final String correctRsql = "name=lt=abc";
        when(baseSoftwareModuleRootMock.get("name")).thenReturn(baseSoftwareModuleRootMock);
        when(baseSoftwareModuleRootMock.getJavaType()).thenReturn((Class) SoftwareModule.class);
        when(criteriaBuilderMock.equal(any(Root.class), anyString())).thenReturn(mock(Predicate.class));
        when(criteriaBuilderMock.<String> greaterThanOrEqualTo(any(Expression.class), any(String.class))).thenReturn(
                mock(Predicate.class));
        doEntitySetup(SoftwareModule.class);
        // test
        RSQLUtility.parse(correctRsql, SoftwareModuleFields.class, entityManager).toPredicate(
                baseSoftwareModuleRootMock, criteriaQueryMock, criteriaBuilderMock);

        // verfication
        verify(criteriaBuilderMock, times(1)).and(any(Predicate.class));
        verify(criteriaBuilderMock, times(1)).lessThan(eq(pathOfString(baseSoftwareModuleRootMock)), eq("abc"));
    }

    @Test
    public void correctRsqlBuildsLikePredicate() {
        reset(baseSoftwareModuleRootMock, criteriaQueryMock, criteriaBuilderMock);
        final String correctRsql = "name=li=abc";
        when(baseSoftwareModuleRootMock.get("name")).thenReturn(baseSoftwareModuleRootMock);
        when(baseSoftwareModuleRootMock.getJavaType()).thenReturn((Class) SoftwareModule.class);
        when(criteriaBuilderMock.equal(any(Root.class), anyString())).thenReturn(mock(Predicate.class));
        when(criteriaBuilderMock.<String> greaterThanOrEqualTo(any(Expression.class), any(String.class))).thenReturn(
                mock(Predicate.class));
        when(criteriaBuilderMock.upper(eq(pathOfString(baseSoftwareModuleRootMock)))).thenReturn(
                pathOfString(baseSoftwareModuleRootMock));
        doEntitySetup(SoftwareModule.class);
        // test
        RSQLUtility.parse(correctRsql, SoftwareModuleFields.class, entityManager).toPredicate(
                baseSoftwareModuleRootMock, criteriaQueryMock, criteriaBuilderMock);

        // verfication
        verify(criteriaBuilderMock, times(1)).and(any(Predicate.class));
        verify(criteriaBuilderMock, times(1)).like(eq(pathOfString(baseSoftwareModuleRootMock)),
                eq("abc".toUpperCase()));
    }

    @Test
    public void correctRsqlWithEnumValue() {
        reset(baseSoftwareModuleRootMock, criteriaQueryMock, criteriaBuilderMock);
        final String correctRsql = "testfield==bumlux";
        when(baseSoftwareModuleRootMock.get("testfield")).thenReturn(baseSoftwareModuleRootMock);
        when(baseSoftwareModuleRootMock.getJavaType()).thenReturn((Class) TestValueEnum.class);
        when(criteriaBuilderMock.equal(any(Root.class), anyString())).thenReturn(mock(Predicate.class));

        doEntitySetup(TestValueEnum.class);
        // test
        RSQLUtility.parse(correctRsql, TestFieldEnum.class, entityManager).toPredicate(baseSoftwareModuleRootMock,
                criteriaQueryMock, criteriaBuilderMock);

        // verfication
        verify(criteriaBuilderMock, times(1)).and(any(Predicate.class));
        verify(criteriaBuilderMock, times(1)).equal(eq(baseSoftwareModuleRootMock), eq(TestValueEnum.BUMLUX));
    }

    @Test
    public void wrongRsqlWithWrongEnumValue() {
        reset(baseSoftwareModuleRootMock, criteriaQueryMock, criteriaBuilderMock);
        final String correctRsql = "testfield==unknownValue";
        when(baseSoftwareModuleRootMock.get("testfield")).thenReturn(baseSoftwareModuleRootMock);
        when(baseSoftwareModuleRootMock.getJavaType()).thenReturn((Class) TestValueEnum.class);
        when(criteriaBuilderMock.equal(any(Root.class), anyString())).thenReturn(mock(Predicate.class));

        doEntitySetup(TestValueEnum.class);

        try {
            // test
            RSQLUtility.parse(correctRsql, TestFieldEnum.class, entityManager).toPredicate(baseSoftwareModuleRootMock,
                    criteriaQueryMock, criteriaBuilderMock);
            fail("missing RSQLParameterUnsupportedFieldException for wrong enum value");
        } catch (final RSQLParameterUnsupportedFieldException e) {
            // nope expected
        }
    }

    private void doEntitySetup(final Class clasName) {
        when(entityManager.getMetamodel()).thenReturn(metamodel);
        when(metamodel.managedType(clasName)).thenReturn(managedType);
        when(managedType.getJavaType()).thenReturn(clasName);

        doAnswer(new Answer<Attribute>() {
            @Override
            public Attribute answer(final InvocationOnMock invocation) throws Throwable {
                return attribute;
            }
        }).when(managedType).getAttribute(anyString());

        when(attribute.isAssociation()).thenReturn(false);

    }

    @SuppressWarnings("unchecked")
    private <Y> Path<Y> pathOfString(final Path<?> path) {
        return (Path<Y>) path;
    }

    private enum TestFieldEnum implements FieldNameProvider {
        TESTFIELD;

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.eclipse.hawkbit.server.rest.resource.model.FieldNameProvider#
         * getFieldName()
         */
        @Override
        public String getFieldName() {
            return "testfield";
        }
    }

    private enum TestValueEnum {
        BUMLUX;
    }

}
