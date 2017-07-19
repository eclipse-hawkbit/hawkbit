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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

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
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.TimestampCalculator;
import org.eclipse.hawkbit.repository.exception.RSQLParameterSyntaxException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.TenantConfigurationValue;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyResolver;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@RunWith(PowerMockRunner.class)
@Features("Component Tests - Repository")
@Stories("RSQL search utility")
@PrepareForTest(TimestampCalculator.class)
// TODO: fully document tests -> @Description for long text and reasonable
// method name as short text
public class RSQLUtilityTest {

    @Spy
    private final VirtualPropertyResolver macroResolver = new VirtualPropertyResolver();

    @Mock
    private TenantConfigurationManagement confMgmt;

    @Mock
    private Root<Object> baseSoftwareModuleRootMock;

    @Mock
    private CriteriaQuery<SoftwareModule> criteriaQueryMock;
    @Mock
    private CriteriaBuilder criteriaBuilderMock;

    @Mock
    private Attribute attribute;

    private static final TenantConfigurationValue<String> TEST_POLLING_TIME_INTERVAL = TenantConfigurationValue
            .<String> builder().value("00:05:00").build();
    private static final TenantConfigurationValue<String> TEST_POLLING_OVERDUE_TIME_INTERVAL = TenantConfigurationValue
            .<String> builder().value("00:07:37").build();

    @Test
    public void wrongRsqlSyntaxThrowSyntaxException() {
        final String wrongRSQL = "name==abc;d";
        try {
            RSQLUtility.parse(wrongRSQL, SoftwareModuleFields.class, null).toPredicate(baseSoftwareModuleRootMock,
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
            RSQLUtility.parse(wrongRSQL, SoftwareModuleFields.class, null).toPredicate(baseSoftwareModuleRootMock,
                    criteriaQueryMock, criteriaBuilderMock);
            fail("Missing an expected RSQLParameterUnsupportedFieldException because of unknown RSQL field");
        } catch (final RSQLParameterUnsupportedFieldException e) {
        }

    }

    @Test
    public void wrongRsqlMapSyntaxThrowSyntaxException() {
        String wrongRSQL = TargetFields.ATTRIBUTE + "==abc";
        try {
            RSQLUtility.parse(wrongRSQL, TargetFields.class, null).toPredicate(baseSoftwareModuleRootMock,
                    criteriaQueryMock, criteriaBuilderMock);
            fail("Missing expected RSQLParameterSyntaxException because of wrong RSQL syntax");
        } catch (final RSQLParameterUnsupportedFieldException e) {
        }

        wrongRSQL = TargetFields.ATTRIBUTE + ".unkwon.wrong==abc";
        try {
            RSQLUtility.parse(wrongRSQL, TargetFields.class, null).toPredicate(baseSoftwareModuleRootMock,
                    criteriaQueryMock, criteriaBuilderMock);
            fail("Missing expected RSQLParameterSyntaxException because of wrong RSQL syntax");
        } catch (final RSQLParameterUnsupportedFieldException e) {
        }

        wrongRSQL = DistributionSetFields.METADATA + "==abc";
        try {
            RSQLUtility.parse(wrongRSQL, DistributionSetFields.class, null).toPredicate(baseSoftwareModuleRootMock,
                    criteriaQueryMock, criteriaBuilderMock);
            fail("Missing expected RSQLParameterSyntaxException because of wrong RSQL syntax");
        } catch (final RSQLParameterUnsupportedFieldException e) {
        }

    }

    @Test
    public void wrongRsqlSubEntitySyntaxThrowSyntaxException() {
        String wrongRSQL = TargetFields.ASSIGNEDDS + "==abc";
        try {
            RSQLUtility.parse(wrongRSQL, TargetFields.class, null).toPredicate(baseSoftwareModuleRootMock,
                    criteriaQueryMock, criteriaBuilderMock);
            fail("Missing expected RSQLParameterSyntaxException because of wrong RSQL syntax");
        } catch (final RSQLParameterUnsupportedFieldException e) {
        }

        wrongRSQL = TargetFields.ASSIGNEDDS + ".unknownField==abc";
        try {
            RSQLUtility.parse(wrongRSQL, TargetFields.class, null).toPredicate(baseSoftwareModuleRootMock,
                    criteriaQueryMock, criteriaBuilderMock);
            fail("Missing expected RSQLParameterSyntaxException because of wrong RSQL syntax");
        } catch (final RSQLParameterUnsupportedFieldException e) {
        }

        wrongRSQL = TargetFields.ASSIGNEDDS + ".unknownField.ToMuch==abc";
        try {
            RSQLUtility.parse(wrongRSQL, TargetFields.class, null).toPredicate(baseSoftwareModuleRootMock,
                    criteriaQueryMock, criteriaBuilderMock);
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
        RSQLUtility.parse(correctRsql, SoftwareModuleFields.class, null).toPredicate(baseSoftwareModuleRootMock,
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
        RSQLUtility.parse(correctRsql, SoftwareModuleFields.class, null).toPredicate(baseSoftwareModuleRootMock,
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
        RSQLUtility.parse(correctRsql, SoftwareModuleFields.class, null).toPredicate(baseSoftwareModuleRootMock,
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
        RSQLUtility.parse(correctRsql, SoftwareModuleFields.class, null).toPredicate(baseSoftwareModuleRootMock,
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
        RSQLUtility.parse(correctRsql, TestFieldEnum.class, null).toPredicate(baseSoftwareModuleRootMock,
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

        try {
            // test
            RSQLUtility.parse(correctRsql, TestFieldEnum.class, null).toPredicate(baseSoftwareModuleRootMock,
                    criteriaQueryMock, criteriaBuilderMock);
            fail("missing RSQLParameterUnsupportedFieldException for wrong enum value");
        } catch (final RSQLParameterUnsupportedFieldException e) {
            // nope expected
        }
    }

    @Test
    @Description("Tests the resolution of overdue_ts placeholder in context of a RSQL expression.")
    public void correctRsqlWithOverdueMacro() {
        reset(baseSoftwareModuleRootMock, criteriaQueryMock, criteriaBuilderMock);
        final String overdueProp = "overdue_ts";
        final String overduePropPlaceholder = "${" + overdueProp + "}";
        final String correctRsql = "testfield=le=" + overduePropPlaceholder;
        when(baseSoftwareModuleRootMock.get("testfield")).thenReturn(baseSoftwareModuleRootMock);
        when(baseSoftwareModuleRootMock.getJavaType()).thenReturn((Class) String.class);
        when(criteriaBuilderMock.equal(any(Root.class), anyString())).thenReturn(mock(Predicate.class));
        when(criteriaBuilderMock.<String> lessThanOrEqualTo(any(Expression.class), eq(overduePropPlaceholder)))
                .thenReturn(mock(Predicate.class));

        // test
        RSQLUtility.parse(correctRsql, TestFieldEnum.class, setupMacroLookup()).toPredicate(baseSoftwareModuleRootMock,
                criteriaQueryMock, criteriaBuilderMock);

        // verification
        verify(macroResolver).lookup(overdueProp);
        // the macro is already replaced when passed to #lessThanOrEqualTo ->
        // the method is never invoked with the
        // placeholder:
        verify(criteriaBuilderMock, never()).lessThanOrEqualTo(eq(pathOfString(baseSoftwareModuleRootMock)),
                eq(overduePropPlaceholder));
    }

    @Test
    @Description("Tests RSQL expression with an unknown placeholder.")
    public void correctRsqlWithUnknownMacro() {
        reset(baseSoftwareModuleRootMock, criteriaQueryMock, criteriaBuilderMock);
        final String overdueProp = "unknown";
        final String overduePropPlaceholder = "${" + overdueProp + "}";
        final String correctRsql = "testfield=le=" + overduePropPlaceholder;
        when(baseSoftwareModuleRootMock.get("testfield")).thenReturn(baseSoftwareModuleRootMock);
        when(baseSoftwareModuleRootMock.getJavaType()).thenReturn((Class) String.class);
        when(criteriaBuilderMock.equal(any(Root.class), anyString())).thenReturn(mock(Predicate.class));
        when(criteriaBuilderMock.<String> lessThanOrEqualTo(any(Expression.class), eq(overduePropPlaceholder)))
                .thenReturn(mock(Predicate.class));

        // test
        RSQLUtility.parse(correctRsql, TestFieldEnum.class, setupMacroLookup()).toPredicate(baseSoftwareModuleRootMock,
                criteriaQueryMock, criteriaBuilderMock);

        // verification
        verify(macroResolver).lookup(overdueProp);
        // the macro is unknown and hence never replaced -> #lessThanOrEqualTo
        // is invoked with the placeholder:
        verify(criteriaBuilderMock).lessThanOrEqualTo(eq(pathOfString(baseSoftwareModuleRootMock)),
                eq(overduePropPlaceholder));
    }

    public VirtualPropertyReplacer setupMacroLookup() {
        when(confMgmt.getConfigurationValue(TenantConfigurationKey.POLLING_TIME_INTERVAL, String.class))
                .thenReturn(TEST_POLLING_TIME_INTERVAL);
        when(confMgmt.getConfigurationValue(TenantConfigurationKey.POLLING_OVERDUE_TIME_INTERVAL, String.class))
                .thenReturn(TEST_POLLING_OVERDUE_TIME_INTERVAL);

        mockStatic(TimestampCalculator.class);
        when(TimestampCalculator.getTenantConfigurationManagement()).thenReturn(confMgmt);

        return macroResolver;
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
