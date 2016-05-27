/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.rsql;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.Attribute;

import org.eclipse.hawkbit.repository.DistributionSetFields;
import org.eclipse.hawkbit.repository.FieldNameProvider;
import org.eclipse.hawkbit.repository.SoftwareModuleFields;
import org.eclipse.hawkbit.repository.TargetFields;
import org.eclipse.hawkbit.repository.exception.RSQLParameterSyntaxException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@RunWith(MockitoJUnitRunner.class)
@Features("Component Tests - Repository")
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
    private Attribute attribute;

    @Test
    public void wrongRsqlSyntaxThrowSyntaxException() {
        final String wrongRSQL = "name==abc;d";
        try {
            RSQLUtility.parse(wrongRSQL, SoftwareModuleFields.class).toPredicate(baseSoftwareModuleRootMock,
                    criteriaQueryMock, criteriaBuilderMock);
            fail("Missing expected RSQLParameterSyntaxException because of wrong RSQL syntax");
        } catch (final RSQLParameterSyntaxException e) {
        }
    }

    @Test
    public void wrongFieldThrowUnsupportedFieldException() {
        final String wrongRSQL = "unknownField==abc";
        when(baseSoftwareModuleRootMock.getJavaType()).thenReturn((Class) SoftwareModule.class);
        try {
            RSQLUtility.parse(wrongRSQL, SoftwareModuleFields.class).toPredicate(baseSoftwareModuleRootMock,
                    criteriaQueryMock, criteriaBuilderMock);
            fail("Missing an expected RSQLParameterUnsupportedFieldException because of unknown RSQL field");
        } catch (final RSQLParameterUnsupportedFieldException e) {
        }

    }

    @Test
    public void wrongRsqlMapSyntaxThrowSyntaxException() {
        String wrongRSQL = TargetFields.ATTRIBUTE + "==abc";
        try {
            RSQLUtility.parse(wrongRSQL, TargetFields.class).toPredicate(baseSoftwareModuleRootMock, criteriaQueryMock,
                    criteriaBuilderMock);
            fail("Missing expected RSQLParameterSyntaxException because of wrong RSQL syntax");
        } catch (final RSQLParameterUnsupportedFieldException e) {
        }

        wrongRSQL = TargetFields.ATTRIBUTE + ".unkwon.wrong==abc";
        try {
            RSQLUtility.parse(wrongRSQL, TargetFields.class).toPredicate(baseSoftwareModuleRootMock, criteriaQueryMock,
                    criteriaBuilderMock);
            fail("Missing expected RSQLParameterSyntaxException because of wrong RSQL syntax");
        } catch (final RSQLParameterUnsupportedFieldException e) {
        }

        wrongRSQL = DistributionSetFields.METADATA + "==abc";
        try {
            RSQLUtility.parse(wrongRSQL, DistributionSetFields.class).toPredicate(baseSoftwareModuleRootMock,
                    criteriaQueryMock, criteriaBuilderMock);
            fail("Missing expected RSQLParameterSyntaxException because of wrong RSQL syntax");
        } catch (final RSQLParameterUnsupportedFieldException e) {
        }

    }

    @Test
    public void wrongRsqlSubEntitySyntaxThrowSyntaxException() {
        String wrongRSQL = TargetFields.ASSIGNEDDS + "==abc";
        try {
            RSQLUtility.parse(wrongRSQL, TargetFields.class).toPredicate(baseSoftwareModuleRootMock, criteriaQueryMock,
                    criteriaBuilderMock);
            fail("Missing expected RSQLParameterSyntaxException because of wrong RSQL syntax");
        } catch (final RSQLParameterUnsupportedFieldException e) {
        }

        wrongRSQL = TargetFields.ASSIGNEDDS + ".unknownField==abc";
        try {
            RSQLUtility.parse(wrongRSQL, TargetFields.class).toPredicate(baseSoftwareModuleRootMock, criteriaQueryMock,
                    criteriaBuilderMock);
            fail("Missing expected RSQLParameterSyntaxException because of wrong RSQL syntax");
        } catch (final RSQLParameterUnsupportedFieldException e) {
        }

        wrongRSQL = TargetFields.ASSIGNEDDS + ".unknownField.ToMuch==abc";
        try {
            RSQLUtility.parse(wrongRSQL, TargetFields.class).toPredicate(baseSoftwareModuleRootMock, criteriaQueryMock,
                    criteriaBuilderMock);
            fail("Missing expected RSQLParameterSyntaxException because of wrong RSQL syntax");
        } catch (final RSQLParameterUnsupportedFieldException e) {
        }
    }

    @Test
    public <T> void correctRsqlBuildsPredicate() {
        reset(baseSoftwareModuleRootMock, criteriaQueryMock, criteriaBuilderMock);
        final String correctRsql = "name==abc;version==1.2";
        when(baseSoftwareModuleRootMock.get("name")).thenReturn(baseSoftwareModuleRootMock);
        when(baseSoftwareModuleRootMock.get("version")).thenReturn(baseSoftwareModuleRootMock);
        when(baseSoftwareModuleRootMock.getJavaType()).thenReturn((Class) SoftwareModule.class);
        when(criteriaBuilderMock.equal(any(Root.class), anyString())).thenReturn(mock(Predicate.class));
        when(criteriaBuilderMock.<String> greaterThanOrEqualTo(any(Expression.class), any(String.class)))
                .thenReturn(mock(Predicate.class));

        // test
        RSQLUtility.parse(correctRsql, SoftwareModuleFields.class).toPredicate(baseSoftwareModuleRootMock,
                criteriaQueryMock, criteriaBuilderMock);

        // verfication
        verify(criteriaBuilderMock, times(1)).and(any(Predicate.class));
    }

    @Test
    public void correctRsqlBuildsNotLikePredicate() {
        reset(baseSoftwareModuleRootMock, criteriaQueryMock, criteriaBuilderMock);
        final String correctRsql = "name!=abc";
        when(baseSoftwareModuleRootMock.get("name")).thenReturn(baseSoftwareModuleRootMock);
        when(baseSoftwareModuleRootMock.getJavaType()).thenReturn((Class) SoftwareModule.class);
        when(criteriaBuilderMock.equal(any(Root.class), anyString())).thenReturn(mock(Predicate.class));
        when(criteriaBuilderMock.<String> greaterThanOrEqualTo(any(Expression.class), any(String.class)))
                .thenReturn(mock(Predicate.class));
        when(criteriaBuilderMock.upper(eq(pathOfString(baseSoftwareModuleRootMock))))
                .thenReturn(pathOfString(baseSoftwareModuleRootMock));
        // test
        RSQLUtility.parse(correctRsql, SoftwareModuleFields.class).toPredicate(baseSoftwareModuleRootMock,
                criteriaQueryMock, criteriaBuilderMock);

        // verfication
        verify(criteriaBuilderMock, times(1)).and(any(Predicate.class));
        verify(criteriaBuilderMock, times(1)).notLike(eq(pathOfString(baseSoftwareModuleRootMock)),
                eq("abc".toUpperCase()));
    }

    @Test
    public void correctRsqlBuildsLikePredicateWithPercentage() {
        reset(baseSoftwareModuleRootMock, criteriaQueryMock, criteriaBuilderMock);
        final String correctRsql = "name==a%";
        when(baseSoftwareModuleRootMock.get("name")).thenReturn(baseSoftwareModuleRootMock);
        when(baseSoftwareModuleRootMock.getJavaType()).thenReturn((Class) SoftwareModule.class);
        when(criteriaBuilderMock.equal(any(Root.class), anyString())).thenReturn(mock(Predicate.class));
        when(criteriaBuilderMock.<String> greaterThanOrEqualTo(any(Expression.class), any(String.class)))
                .thenReturn(mock(Predicate.class));
        when(criteriaBuilderMock.upper(eq(pathOfString(baseSoftwareModuleRootMock))))
                .thenReturn(pathOfString(baseSoftwareModuleRootMock));
        // test
        RSQLUtility.parse(correctRsql, SoftwareModuleFields.class).toPredicate(baseSoftwareModuleRootMock,
                criteriaQueryMock, criteriaBuilderMock);

        // verfication
        verify(criteriaBuilderMock, times(1)).and(any(Predicate.class));
        verify(criteriaBuilderMock, times(1)).like(eq(pathOfString(baseSoftwareModuleRootMock)),
                eq("a\\%".toUpperCase()));
    }

    @Test
    public void correctRsqlBuildsLessThanPredicate() {
        reset(baseSoftwareModuleRootMock, criteriaQueryMock, criteriaBuilderMock);
        final String correctRsql = "name=lt=abc";
        when(baseSoftwareModuleRootMock.get("name")).thenReturn(baseSoftwareModuleRootMock);
        when(baseSoftwareModuleRootMock.getJavaType()).thenReturn((Class) SoftwareModule.class);
        when(criteriaBuilderMock.equal(any(Root.class), anyString())).thenReturn(mock(Predicate.class));
        when(criteriaBuilderMock.<String> greaterThanOrEqualTo(any(Expression.class), any(String.class)))
                .thenReturn(mock(Predicate.class));
        // test
        RSQLUtility.parse(correctRsql, SoftwareModuleFields.class).toPredicate(baseSoftwareModuleRootMock,
                criteriaQueryMock, criteriaBuilderMock);

        // verfication
        verify(criteriaBuilderMock, times(1)).and(any(Predicate.class));
        verify(criteriaBuilderMock, times(1)).lessThan(eq(pathOfString(baseSoftwareModuleRootMock)), eq("abc"));
    }

    @Test
    public void correctRsqlWithEnumValue() {
        reset(baseSoftwareModuleRootMock, criteriaQueryMock, criteriaBuilderMock);
        final String correctRsql = "testfield==bumlux";
        when(baseSoftwareModuleRootMock.get("testfield")).thenReturn(baseSoftwareModuleRootMock);
        when(baseSoftwareModuleRootMock.getJavaType()).thenReturn((Class) TestValueEnum.class);
        when(criteriaBuilderMock.equal(any(Root.class), anyString())).thenReturn(mock(Predicate.class));

        // test
        RSQLUtility.parse(correctRsql, TestFieldEnum.class).toPredicate(baseSoftwareModuleRootMock, criteriaQueryMock,
                criteriaBuilderMock);

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

        try {
            // test
            RSQLUtility.parse(correctRsql, TestFieldEnum.class).toPredicate(baseSoftwareModuleRootMock,
                    criteriaQueryMock, criteriaBuilderMock);
            fail("missing RSQLParameterUnsupportedFieldException for wrong enum value");
        } catch (final RSQLParameterUnsupportedFieldException e) {
            // nope expected
        }
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
