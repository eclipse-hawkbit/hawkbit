/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.specifications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature("Unit Tests - Repository")
@Story("Specifications builder")
public class SpecificationsBuilderTest {

    @Test
    @Description("Test the combination of specs on an empty list which returns null")
    public void combineWithAndEmptyList() {
        final List<Specification<Object>> specList = Collections.emptyList();
        assertThat(SpecificationsBuilder.combineWithAnd(specList)).isNull();
    }

    @Test
    @Description("Test the combination of specs on an immutable list with one entry")
    public void combineWithAndSingleImmutableList() {
        final Specification<Object> spec = (root, query, cb) -> cb.equal(root.get("field1"), "testValue");
        final List<Specification<Object>> specList = Collections.singletonList(spec);
        final Specification<Object> specifications = SpecificationsBuilder.combineWithAnd(specList);
        assertThat(specifications).as("Specifications").isNotNull();

        // mocks to call toPredicate on specifications
        final CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        final Path field1 = mock(Path.class);
        final Predicate equalPredicate = mock(Predicate.class);
        final CriteriaQuery<Object[]> query = mock(CriteriaQuery.class);
        final Root<Object> root = mock(Root.class);

        when(criteriaBuilder.equal(any(Expression.class), anyString())).thenReturn(equalPredicate);
        when(root.get("field1")).thenReturn(field1);

        final Predicate predicate = specifications.toPredicate(root, query, criteriaBuilder);

        assertThat(predicate).isEqualTo(equalPredicate);

    }

    @Test
    @Description("Test the combination of specs on a list with multiple entries")
    public void combineWithAndList() {
        final Specification<Object> spec1 = (root, query, cb) -> cb.equal(root.get("field1"), "testValue1");
        final Specification<Object> spec2 = (root, query, cb) -> cb.equal(root.get("field2"), "testValue2");

        final List<Specification<Object>> specList = new ArrayList<>(2);
        specList.add(spec1);
        specList.add(spec2);

        final Specification<Object> specifications = SpecificationsBuilder.combineWithAnd(specList);
        assertThat(specifications).as("Specifications").isNotNull();

        // mocks to call toPredicate on specifications
        final CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        final Path field1 = mock(Path.class);
        final Path field2 = mock(Path.class);
        final Predicate equalPredicate1 = mock(Predicate.class);
        final Predicate equalPredicate2 = mock(Predicate.class);
        final Predicate combinedPredicate = mock(Predicate.class);
        final CriteriaQuery<Object[]> query = mock(CriteriaQuery.class);
        final Root<Object> root = mock(Root.class);

        when(criteriaBuilder.equal(any(Path.class), eq("testValue1"))).thenReturn(equalPredicate1);
        when(criteriaBuilder.equal(any(Path.class), eq("testValue2"))).thenReturn(equalPredicate2);
        when(criteriaBuilder.and(eq(equalPredicate2), eq(equalPredicate1))).thenReturn(combinedPredicate);
        when(root.get("field1")).thenReturn(field1);
        when(root.get("field2")).thenReturn(field2);

        final Predicate predicate = specifications.toPredicate(root, query, criteriaBuilder);

        assertThat(predicate).as("Combined predicate").isEqualTo(combinedPredicate);

    }

}