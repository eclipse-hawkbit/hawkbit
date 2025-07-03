/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.rsql;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.Type;

import org.eclipse.hawkbit.repository.DistributionSetFields;
import org.eclipse.hawkbit.repository.RsqlQueryField;
import org.eclipse.hawkbit.repository.SoftwareModuleFields;
import org.eclipse.hawkbit.repository.TargetFields;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.TenantConfigurationValue;
import org.eclipse.hawkbit.repository.model.helper.SystemSecurityContextHolder;
import org.eclipse.hawkbit.repository.model.helper.TenantConfigurationManagementHolder;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyResolver;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
/**
 * Feature: Component Tests - Repository<br/>
 * Story: RSQL search utility
 */
@Disabled
// TODO: fully document tests -> description for long text and reasonable
// method name as short text
class RsqlUtilityTest {

    private static final TenantConfigurationValue<String> TEST_POLLING_TIME_INTERVAL =
            TenantConfigurationValue.<String> builder().value("00:05:00").build();
    private static final TenantConfigurationValue<String> TEST_POLLING_OVERDUE_TIME_INTERVAL =
            TenantConfigurationValue.<String> builder().value("00:07:37").build();

    @Spy
    private final VirtualPropertyResolver macroResolver = new VirtualPropertyResolver();
    private final Database testDb = Database.H2;
    @MockitoBean
    private TenantConfigurationManagement confMgmt;
    @MockitoBean
    private SystemSecurityContext securityContext;
    @Mock
    private Root<Object> baseSoftwareModuleRootMock;
    @Mock
    private CriteriaQuery<SoftwareModule> criteriaQueryMock;
    @Mock
    private CriteriaBuilder criteriaBuilderMock;
    @Mock
    private Subquery<SoftwareModule> subqueryMock;
    @Mock
    private Root<SoftwareModule> subqueryRootMock;
    @Mock
    private Attribute attribute;

    @BeforeEach
    void beforeEach() {
        setupRoot(baseSoftwareModuleRootMock);
        setupRoot(subqueryRootMock);
    }

    @Test
    void wrongFieldThrowUnsupportedFieldException() {
        when(baseSoftwareModuleRootMock.getJavaType()).thenReturn((Class) SoftwareModule.class);
        final Specification<Object> rsqlSpecification = RsqlUtility.buildRsqlSpecification("unknownField==abc", SoftwareModuleFields.class, null, testDb);
        assertThatExceptionOfType(RSQLParameterUnsupportedFieldException.class)
                .as("RSQLParameterUnsupportedFieldException because of unknown RSQL field")
                .isThrownBy(() -> rsqlSpecification.toPredicate(baseSoftwareModuleRootMock, criteriaQueryMock, criteriaBuilderMock));
    }

    @Test
    void wrongRsqlMapSyntaxThrowSyntaxException() {
        final Specification<Object> rsqlSpecification =
                RsqlUtility.buildRsqlSpecification(TargetFields.ATTRIBUTE + "==abc", TargetFields.class, null, testDb);
        assertThatExceptionOfType(RSQLParameterUnsupportedFieldException.class)
                .as("RSQLParameterSyntaxException for target attributes map, caused by wrong RSQL syntax (key was not present)")
                .isThrownBy(() -> rsqlSpecification.toPredicate(baseSoftwareModuleRootMock, criteriaQueryMock, criteriaBuilderMock));

        final Specification<Object> rsqlSpecification2 =
                RsqlUtility.buildRsqlSpecification(TargetFields.ATTRIBUTE + ".unknown.wrong==abc", TargetFields.class, null, testDb);
        assertThatExceptionOfType(RSQLParameterUnsupportedFieldException.class)
                .as("RSQLParameterSyntaxException for target attributes map, caused by wrong RSQL syntax (key includes dots)")
                .isThrownBy(() -> rsqlSpecification2.toPredicate(baseSoftwareModuleRootMock, criteriaQueryMock, criteriaBuilderMock));

        final Specification<Object> rsqlSpecification3 =
                RsqlUtility.buildRsqlSpecification(TargetFields.METADATA + ".unknown.wrong==abc", TargetFields.class, null, testDb);
        assertThatExceptionOfType(RSQLParameterUnsupportedFieldException.class)
                .as("RSQLParameterSyntaxException for target metadata map, caused by wrong RSQL syntax (key includes dots)")
                .isThrownBy(() -> rsqlSpecification3.toPredicate(baseSoftwareModuleRootMock, criteriaQueryMock, criteriaBuilderMock));

        final Specification<Object> rsqlSpecification4 =
                RsqlUtility.buildRsqlSpecification(DistributionSetFields.METADATA + "==abc", TargetFields.class, null, testDb);
        assertThatExceptionOfType(RSQLParameterUnsupportedFieldException.class)
                .as("RSQLParameterSyntaxException for distribution set metadata map, caused by wrong RSQL syntax (key was not present)\"")
                .isThrownBy(() -> rsqlSpecification4.toPredicate(baseSoftwareModuleRootMock, criteriaQueryMock, criteriaBuilderMock));
    }

    @Test
    void wrongRsqlSubEntitySyntaxThrowSyntaxException() {
        final Specification<Object> rsqlSpecification =
                RsqlUtility.buildRsqlSpecification(TargetFields.ASSIGNEDDS + "==abc", TargetFields.class, null, testDb);
        assertThatExceptionOfType(RSQLParameterUnsupportedFieldException.class)
                .as("RSQLParameterSyntaxException because of wrong RSQL syntax")
                .isThrownBy(() -> rsqlSpecification.toPredicate(baseSoftwareModuleRootMock, criteriaQueryMock, criteriaBuilderMock));

        final Specification<Object> rsqlSpecification2 =
                RsqlUtility.buildRsqlSpecification(TargetFields.ASSIGNEDDS + ".unknownField==abc", TargetFields.class, null, testDb);
        assertThatExceptionOfType(RSQLParameterUnsupportedFieldException.class)
                .as("RSQLParameterSyntaxException because of wrong RSQL syntax")
                .isThrownBy(() -> rsqlSpecification2.toPredicate(baseSoftwareModuleRootMock, criteriaQueryMock, criteriaBuilderMock));

        final Specification<Object> rsqlSpecification3 =
                RsqlUtility.buildRsqlSpecification(TargetFields.ASSIGNEDDS + ".unknownField.ToMuch==abc", TargetFields.class, null, testDb);
        assertThatExceptionOfType(RSQLParameterUnsupportedFieldException.class)
                .as("RSQLParameterSyntaxException because of wrong RSQL syntax")
                .isThrownBy(() -> rsqlSpecification3.toPredicate(baseSoftwareModuleRootMock, criteriaQueryMock, criteriaBuilderMock));
    }

    @Test
    <T> void correctRsqlBuildsPredicate() {
        reset0(baseSoftwareModuleRootMock, criteriaQueryMock, criteriaBuilderMock);
        final String correctRsql = "name==abc;version==1.2";
        when(baseSoftwareModuleRootMock.get("name")).thenReturn(baseSoftwareModuleRootMock);
        when(baseSoftwareModuleRootMock.get("version")).thenReturn(baseSoftwareModuleRootMock);
        when(baseSoftwareModuleRootMock.getJavaType()).thenReturn((Class) SoftwareModule.class);
        when(criteriaBuilderMock.upper(pathOfString(baseSoftwareModuleRootMock))).thenReturn(pathOfString(baseSoftwareModuleRootMock));
        when(criteriaBuilderMock.like(any(Expression.class), anyString(), eq('\\'))).thenReturn(mock(Predicate.class));
        when(criteriaBuilderMock.equal(any(Expression.class), any(String.class))).thenReturn(mock(Predicate.class));
        when(criteriaBuilderMock.and(any(Predicate[].class))).thenReturn(mock(Predicate.class));

        // test
        RsqlUtility.buildRsqlSpecification(correctRsql, SoftwareModuleFields.class, null, testDb)
                .toPredicate(baseSoftwareModuleRootMock, criteriaQueryMock, criteriaBuilderMock);

        // verification
        verify(criteriaBuilderMock, times(1)).and(any(Predicate.class));
    }

    @Test
    void correctRsqlBuildsSimpleNotEqualPredicate() {
        reset0(baseSoftwareModuleRootMock, criteriaQueryMock, criteriaBuilderMock);
        final String correctRsql = "name!=abc";
        when(baseSoftwareModuleRootMock.get("name")).thenReturn(baseSoftwareModuleRootMock);
        when(baseSoftwareModuleRootMock.getJavaType()).thenReturn((Class) String.class);

        when(criteriaBuilderMock.isNull(any(Expression.class))).thenReturn(mock(Predicate.class));
        when(criteriaBuilderMock.notEqual(any(Expression.class), anyString())).thenReturn(mock(Predicate.class));
        when(criteriaBuilderMock.upper(pathOfString(baseSoftwareModuleRootMock))).thenReturn(pathOfString(baseSoftwareModuleRootMock));

        // test
        RsqlUtility.buildRsqlSpecification(correctRsql, SoftwareModuleFields.class, null, testDb)
                .toPredicate(baseSoftwareModuleRootMock, criteriaQueryMock, criteriaBuilderMock);

        // verification
        verify(criteriaBuilderMock, times(1)).or(any(Predicate.class), any(Predicate.class));
        verify(criteriaBuilderMock, times(1)).isNull(pathOfString(baseSoftwareModuleRootMock));
        verify(criteriaBuilderMock, times(1)).notEqual(pathOfString(baseSoftwareModuleRootMock), "abc".toUpperCase());
    }

    @Test
    void correctRsqlBuildsSimpleNotLikePredicate() {
        reset0(baseSoftwareModuleRootMock, criteriaQueryMock, criteriaBuilderMock);
        final String correctRsql = "name!=abc*";
        when(baseSoftwareModuleRootMock.get("name")).thenReturn(baseSoftwareModuleRootMock);
        when(baseSoftwareModuleRootMock.getJavaType()).thenReturn((Class) String.class);

        when(criteriaBuilderMock.isNull(any(Expression.class))).thenReturn(mock(Predicate.class));
        when(criteriaBuilderMock.notLike(any(Expression.class), anyString(), eq('\\')))
                .thenReturn(mock(Predicate.class));
        when(criteriaBuilderMock.upper(pathOfString(baseSoftwareModuleRootMock))).thenReturn(pathOfString(baseSoftwareModuleRootMock));

        // test
        RsqlUtility.buildRsqlSpecification(correctRsql, SoftwareModuleFields.class, null, testDb)
                .toPredicate(baseSoftwareModuleRootMock, criteriaQueryMock, criteriaBuilderMock);

        // verification
        verify(criteriaBuilderMock, times(1)).or(any(Predicate.class), any(Predicate.class));
        verify(criteriaBuilderMock, times(1)).isNull(pathOfString(baseSoftwareModuleRootMock));
        verify(criteriaBuilderMock, times(1)).notLike(pathOfString(baseSoftwareModuleRootMock), "abc%".toUpperCase(), '\\');
    }

    @Test
    void correctRsqlBuildsNotSimpleNotLikePredicate() {
        reset0(baseSoftwareModuleRootMock, criteriaQueryMock, criteriaBuilderMock);
        // with this query a subquery has to be made, so it is no simple query
        final String correctRsql = "type!=abc";
        when(baseSoftwareModuleRootMock.get(anyString())).thenReturn(baseSoftwareModuleRootMock);
        when(baseSoftwareModuleRootMock.getJavaType()).thenReturn((Class) SoftwareModule.class);

        final Path pathMock = mock(Path.class);
        when(pathMock.get(anyString())).thenReturn(pathMock);
        when(subqueryRootMock.get(anyString())).thenReturn(pathMock);

        when(criteriaBuilderMock.and(any(), any())).thenReturn(mock(Predicate.class));

        when(criteriaQueryMock.subquery(SoftwareModule.class)).thenReturn(subqueryMock);

        when(subqueryMock.from(SoftwareModule.class)).thenReturn(subqueryRootMock);
        when(subqueryMock.select(subqueryRootMock)).thenReturn(subqueryMock);
        when(subqueryMock.where(any(Expression.class))).thenReturn(subqueryMock);

        // test
        RsqlUtility.buildRsqlSpecification(correctRsql, SoftwareModuleFields.class, null, testDb)
                .toPredicate(baseSoftwareModuleRootMock, criteriaQueryMock, criteriaBuilderMock);

        // verification
        verify(criteriaBuilderMock, times(1)).not(criteriaBuilderMock.exists(eq(subqueryMock)));
    }

    @Test
    void correctRsqlBuildsEqualPredicateWithPercentage() {
        reset0(baseSoftwareModuleRootMock, criteriaQueryMock, criteriaBuilderMock);
        final String correctRsql = "name==a%";
        when(baseSoftwareModuleRootMock.get("name")).thenReturn(baseSoftwareModuleRootMock);
        when(baseSoftwareModuleRootMock.getJavaType()).thenReturn((Class) String.class);
        when(criteriaBuilderMock.equal(any(Expression.class), anyString())).thenReturn(mock(Predicate.class));
        when(criteriaBuilderMock.greaterThanOrEqualTo(any(Expression.class), any(String.class))).thenReturn(mock(Predicate.class));
        when(criteriaBuilderMock.upper(pathOfString(baseSoftwareModuleRootMock))).thenReturn(pathOfString(baseSoftwareModuleRootMock));
        // test
        RsqlUtility.buildRsqlSpecification(correctRsql, SoftwareModuleFields.class, null, Database.H2)
                .toPredicate(baseSoftwareModuleRootMock, criteriaQueryMock, criteriaBuilderMock);

        // verification
        verify(criteriaBuilderMock, times(1)).and(any(Predicate.class));
        verify(criteriaBuilderMock, times(1)).equal(pathOfString(baseSoftwareModuleRootMock), "a%".toUpperCase());
    }

    @Test
    void correctRsqlBuildsLikePredicateWithPercentage() {
        reset0(baseSoftwareModuleRootMock, criteriaQueryMock, criteriaBuilderMock);
        final String correctRsql = "name==a%*";
        when(baseSoftwareModuleRootMock.get("name")).thenReturn(baseSoftwareModuleRootMock);
        when(baseSoftwareModuleRootMock.getJavaType()).thenReturn((Class) String.class);
        when(criteriaBuilderMock.like(any(Expression.class), anyString(), eq('\\'))).thenReturn(mock(Predicate.class));
        when(criteriaBuilderMock.greaterThanOrEqualTo(any(Expression.class), any(String.class))).thenReturn(mock(Predicate.class));
        when(criteriaBuilderMock.upper(pathOfString(baseSoftwareModuleRootMock))).thenReturn(pathOfString(baseSoftwareModuleRootMock));
        // test
        RsqlUtility.buildRsqlSpecification(correctRsql, SoftwareModuleFields.class, null, Database.H2)
                .toPredicate(baseSoftwareModuleRootMock, criteriaQueryMock, criteriaBuilderMock);

        // verification
        verify(criteriaBuilderMock, times(1)).and(any(Predicate.class));
        verify(criteriaBuilderMock, times(1)).like(pathOfString(baseSoftwareModuleRootMock), "a\\%%".toUpperCase(), '\\');
    }

    // MsSQL is not officially supported
    // thought it may be available through configuration and adding necessarily dependencies
    // so we keep RSQL compatibility testing
    @Test
    void correctRsqlBuildsLikePredicateWithPercentageSQLServer() {
        reset0(baseSoftwareModuleRootMock, criteriaQueryMock, criteriaBuilderMock);
        final String correctRsql = "name==a%*";
        when(baseSoftwareModuleRootMock.get("name")).thenReturn(baseSoftwareModuleRootMock);
        when(baseSoftwareModuleRootMock.getJavaType()).thenReturn((Class) String.class);
        when(criteriaBuilderMock.upper(pathOfString(baseSoftwareModuleRootMock))).thenReturn(pathOfString(baseSoftwareModuleRootMock));
        when(criteriaBuilderMock.like(any(Expression.class), anyString(), eq('\\'))).thenReturn(mock(Predicate.class));
        when(criteriaBuilderMock.<String> greaterThanOrEqualTo(any(Expression.class), any(String.class))).thenReturn(mock(Predicate.class));

        // test
        RsqlUtility.buildRsqlSpecification(correctRsql, SoftwareModuleFields.class, null, Database.SQL_SERVER)
                .toPredicate(baseSoftwareModuleRootMock, criteriaQueryMock, criteriaBuilderMock);

        // verification
        verify(criteriaBuilderMock, times(1)).and(any(Predicate.class));
        verify(criteriaBuilderMock, times(1)).like(pathOfString(baseSoftwareModuleRootMock), "a[%]%".toUpperCase(), '\\');
    }

    @Test
    void correctRsqlBuildsLessThanPredicate() {
        reset0(baseSoftwareModuleRootMock, criteriaQueryMock, criteriaBuilderMock);
        final String correctRsql = "name=lt=abc";
        when(baseSoftwareModuleRootMock.get("name")).thenReturn(baseSoftwareModuleRootMock);
        when(baseSoftwareModuleRootMock.getJavaType()).thenReturn((Class) SoftwareModule.class);
        when(criteriaBuilderMock.lessThan(any(Expression.class), anyString())).thenReturn(mock(Predicate.class));
        when(criteriaBuilderMock.<String> greaterThanOrEqualTo(any(Expression.class), any(String.class)))
                .thenReturn(mock(Predicate.class));
        // test
        RsqlUtility.buildRsqlSpecification(correctRsql, SoftwareModuleFields.class, null, testDb)
                .toPredicate(baseSoftwareModuleRootMock, criteriaQueryMock, criteriaBuilderMock);

        // verification
        verify(criteriaBuilderMock, times(1)).and(any(Predicate.class));
        verify(criteriaBuilderMock, times(1)).lessThan(pathOfString(baseSoftwareModuleRootMock), "abc");
    }

    @Test
    void correctRsqlWithEnumValue() {
        reset0(baseSoftwareModuleRootMock, criteriaQueryMock, criteriaBuilderMock);
        final String correctRsql = "testfield==bumlux";
        when(baseSoftwareModuleRootMock.get("testfield")).thenReturn(baseSoftwareModuleRootMock);
        when(baseSoftwareModuleRootMock.getJavaType()).thenReturn((Class) TestValueEnum.class);
        when(criteriaBuilderMock.equal(any(Root.class), any(TestValueEnum.class))).thenReturn(mock(Predicate.class));

        // test
        RsqlUtility.buildRsqlSpecification(correctRsql, TestFieldEnum.class, null, testDb)
                .toPredicate(baseSoftwareModuleRootMock, criteriaQueryMock, criteriaBuilderMock);

        // verification
        verify(criteriaBuilderMock, times(1)).and(any(Predicate.class));
        verify(criteriaBuilderMock, times(1)).equal(baseSoftwareModuleRootMock, TestValueEnum.BUMLUX);
    }

    @Test
    void wrongRsqlWithWrongEnumValue() {
        reset0(baseSoftwareModuleRootMock, criteriaQueryMock, criteriaBuilderMock);
        final String correctRsql = "testfield==unknownValue";
        when(baseSoftwareModuleRootMock.get("testfield")).thenReturn(baseSoftwareModuleRootMock);
        when(baseSoftwareModuleRootMock.getJavaType()).thenReturn((Class) TestValueEnum.class);
        when(criteriaBuilderMock.equal(any(Root.class), anyString())).thenReturn(mock(Predicate.class));

        final Specification<Object> rsqlSpecification = RsqlUtility.buildRsqlSpecification(correctRsql, TestFieldEnum.class, null, testDb);
        assertThatExceptionOfType(RSQLParameterUnsupportedFieldException.class)
                .as("RSQLParameterUnsupportedFieldException for wrong enum value")
                .isThrownBy(() -> rsqlSpecification.toPredicate(baseSoftwareModuleRootMock, criteriaQueryMock, criteriaBuilderMock));
    }

    /**
     * Tests the resolution of overdue_ts placeholder in context of a RSQL expression.
     */
    @Test
    void correctRsqlWithOverdueMacro() {
        reset0(baseSoftwareModuleRootMock, criteriaQueryMock, criteriaBuilderMock);
        final String overdueProp = "overdue_ts";
        final String overduePropPlaceholder = "${" + overdueProp + "}";
        final String correctRsql = "testfield=le=" + overduePropPlaceholder;
        when(baseSoftwareModuleRootMock.get("testfield")).thenReturn(baseSoftwareModuleRootMock);
        when(baseSoftwareModuleRootMock.getJavaType()).thenReturn((Class) String.class);
        when(criteriaBuilderMock.upper(pathOfString(baseSoftwareModuleRootMock))).thenReturn(pathOfString(baseSoftwareModuleRootMock));
        when(criteriaBuilderMock.like(any(Expression.class), anyString(), eq('\\'))).thenReturn(mock(Predicate.class));
        when(criteriaBuilderMock.lessThanOrEqualTo(any(Expression.class), eq(overduePropPlaceholder))).thenReturn(mock(Predicate.class));

        // test
        RsqlUtility.buildRsqlSpecification(correctRsql, TestFieldEnum.class, setupMacroLookup(), testDb)
                .toPredicate(baseSoftwareModuleRootMock, criteriaQueryMock, criteriaBuilderMock);

        // verification
        // the macro is already replaced when passed to #lessThanOrEqualTo -> the method is never invoked with the placeholder:
        verify(criteriaBuilderMock, never()).lessThanOrEqualTo(pathOfString(baseSoftwareModuleRootMock), overduePropPlaceholder);
    }

    /**
     * Tests RSQL expression with an unknown placeholder.
     */
    @Test
    void correctRsqlWithUnknownMacro() {
        reset0(baseSoftwareModuleRootMock, criteriaQueryMock, criteriaBuilderMock);
        final String overdueProp = "unknown";
        final String overduePropPlaceholder = "${" + overdueProp + "}";
        final String correctRsql = "testfield=le=" + overduePropPlaceholder;
        when(baseSoftwareModuleRootMock.get("testfield")).thenReturn(baseSoftwareModuleRootMock);
        when(baseSoftwareModuleRootMock.getJavaType()).thenReturn((Class) String.class);
        when(criteriaBuilderMock.equal(any(Root.class), anyString())).thenReturn(mock(Predicate.class));
        when(criteriaBuilderMock.<String> lessThanOrEqualTo(any(Expression.class), eq(overduePropPlaceholder)))
                .thenReturn(mock(Predicate.class));

        // test
        RsqlUtility.buildRsqlSpecification(correctRsql, TestFieldEnum.class, setupMacroLookup(), testDb)
                .toPredicate(baseSoftwareModuleRootMock, criteriaQueryMock, criteriaBuilderMock);

        // verification
        // the macro is unknown and hence never replaced -> #lessThanOrEqualTo is invoked with the placeholder:
        verify(criteriaBuilderMock).lessThanOrEqualTo(pathOfString(baseSoftwareModuleRootMock), overduePropPlaceholder);
    }

    VirtualPropertyReplacer setupMacroLookup() {
        when(securityContext.runAsSystem(Mockito.any())).thenAnswer(a -> ((Callable<?>) a.getArgument(0)).call());

        when(confMgmt.getConfigurationValue(TenantConfigurationKey.POLLING_TIME_INTERVAL, String.class))
                .thenReturn(TEST_POLLING_TIME_INTERVAL);
        when(confMgmt.getConfigurationValue(TenantConfigurationKey.POLLING_OVERDUE_TIME_INTERVAL, String.class))
                .thenReturn(TEST_POLLING_OVERDUE_TIME_INTERVAL);

        return macroResolver;
    }

    @SuppressWarnings("unchecked")
    private <Y> Path<Y> pathOfString(final Path<?> path) {
        return (Path<Y>) path;
    }

    private void reset0(final Object... mocks) {
        reset(mocks);
        if (Arrays.asList(mocks).contains(baseSoftwareModuleRootMock)) {
            setupRoot(baseSoftwareModuleRootMock);
        }
        if (Arrays.asList(mocks).contains(subqueryRootMock)) {
            setupRoot(subqueryRootMock);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void setupRoot(final Root<?> root) {
        final Type type = Mockito.mock(Type.class);
        when(type.getPersistenceType()).thenReturn(Type.PersistenceType.BASIC);
        final SingularAttribute singularAttribute = Mockito.mock(SingularAttribute.class);
        when(singularAttribute.getType()).thenReturn(type);
        final EntityType entityType = Mockito.mock(EntityType.class);
        when(entityType.getAttribute(any())).thenReturn(singularAttribute);
        when(entityType.getPersistenceType()).thenReturn(Type.PersistenceType.BASIC);
        when(root.getModel()).thenReturn(entityType);
    }

    private enum TestFieldEnum implements RsqlQueryField {
        TESTFIELD("testfield"), TESTFIELD_WITH_SUB_ENTITIES("testfieldWithSubEntities", "subentity11", "subentity22");

        private final String fieldName;
        private final List<String> subEntityAttributes;

        TestFieldEnum(final String fieldName) {
            this(fieldName, new String[0]);
        }

        TestFieldEnum(final String fieldName, final String... subEntityAttributes) {
            this.fieldName = fieldName;
            this.subEntityAttributes = (Arrays.asList(subEntityAttributes));
        }

        @Override
        public String getJpaEntityFieldName() {
            return this.fieldName;
        }

        @Override
        public List<String> getSubEntityAttributes() {
            return subEntityAttributes;
        }
    }

    private enum TestValueEnum {
        BUMLUX;
    }

    @Configuration
    static class Config {

        @Bean
        TenantConfigurationManagementHolder tenantConfigurationManagementHolder() {
            return TenantConfigurationManagementHolder.getInstance();
        }

        @Bean
        SystemSecurityContextHolder systemSecurityContextHolder() {
            return SystemSecurityContextHolder.getInstance();
        }

        @Bean
        RsqlConfigHolder rsqlConfigHolder() {
            return RsqlConfigHolder.getInstance();
        }
    }
}