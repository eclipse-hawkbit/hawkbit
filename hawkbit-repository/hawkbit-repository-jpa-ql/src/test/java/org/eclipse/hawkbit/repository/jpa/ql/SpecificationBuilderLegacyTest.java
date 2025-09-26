/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.ql;

import static org.eclipse.hawkbit.repository.jpa.ql.QLSupport.SpecBuilder.LEGACY_G1;
import static org.eclipse.hawkbit.repository.jpa.ql.QLSupport.SpecBuilder.LEGACY_G2;

import java.util.Arrays;
import java.util.List;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.repository.QueryField;
import org.eclipse.hawkbit.repository.jpa.ql.QLSupport.SpecBuilder;
import org.eclipse.hawkbit.repository.jpa.rsql.legacy.SpecificationBuilderLegacy;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.jpa.vendor.Database;

@SuppressWarnings("java:S2699") // java:S2699 - assertions are un the super methods that are called
@Slf4j
class SpecificationBuilderLegacyTest extends SpecificationBuilderTest {

    private final SpecificationBuilderLegacy<RootField, Root> builder = new SpecificationBuilderLegacy<>(RootField.class, null, Database.H2);

    private static void runWithRsqlToSpecBuilder(final Runnable runnable, final SpecBuilder rsqlToSpecBuilder) {
        final SpecBuilder defaultBuilder = QLSupport.getInstance().getSpecBuilder();
        QLSupport.getInstance().setSpecBuilder(rsqlToSpecBuilder);
        try {
            runnable.run();
        } finally {
            QLSupport.getInstance().setSpecBuilder(defaultBuilder);
        }
    }

    @Test
    void singularStringAttributeG1() {
        runWithRsqlToSpecBuilder(super::singularStringAttribute, LEGACY_G1);
    }
    @Override
    @Test
    void singularStringAttribute() {
        runWithRsqlToSpecBuilder(super::singularStringAttribute, LEGACY_G2);
    }

    @Test
    void singularIntAttributeG1() {
        runWithRsqlToSpecBuilder(super::singularIntAttribute, LEGACY_G1);
    }
    @Override
    @Test
    void singularIntAttribute() {
        runWithRsqlToSpecBuilder(super::singularIntAttribute, LEGACY_G2);
    }

    @Test
    void singularEntityAttributeG1() {
        runWithRsqlToSpecBuilder(super::singularEntityAttribute, LEGACY_G1);
    }
    @Override
    @Test
    void singularEntityAttribute() {
        runWithRsqlToSpecBuilder(super::singularEntityAttribute, LEGACY_G2);
    }

    @Test
    void pluralSubSetAttributeG1() {
        runWithRsqlToSpecBuilder(super::pluralSubSetAttribute, LEGACY_G1);
    }
    @Override
    @Test
    void pluralSubSetAttribute() {
        runWithRsqlToSpecBuilder(super::pluralSubSetAttribute, LEGACY_G2);
    }

    // Legacy G1 doesn't support hibernate maps
    @Override
    @Test
    void pluralSubMapAttribute() {
        runWithRsqlToSpecBuilder(super::pluralSubMapAttribute, LEGACY_G2);
    }

    @Test
    void singularEntitySubSubAttributeG1() {
        runWithRsqlToSpecBuilder(super::singularEntitySubSubAttribute, LEGACY_G1);
    }
    @Override
    @Test
    void singularEntitySubSubAttribute() {
        runWithRsqlToSpecBuilder(super::singularEntitySubSubAttribute, LEGACY_G2);
    }

    @Override
    @Test
    void deapSearchSubSubSubSubAttribute() {
        // legacy builders doesn't support deep search
    }

    @Override
    protected Specification<Root> getSpecification(final String rsql) {
        return builder.specification(rsql);
    }

    @Getter
    private enum RootField implements QueryField {

        INTVALUE("intValue"),
        STRVALUE("strValue"),
        SUBENTITY("subEntity", "strValue", "intValue", "subSub"),
        SUBSET("subSet", "strValue", "intValue"),
        SUBMAP("subMap");

        private final String jpaEntityFieldName;
        private final List<String> subEntityAttributes;

        RootField(final String jpaEntityFieldName, final String... subFields) {
            this.jpaEntityFieldName = jpaEntityFieldName;
            this.subEntityAttributes = Arrays.asList(subFields);
        }

        @Override
        public boolean isMap() {
            return this == SUBMAP;
        }
    }
}