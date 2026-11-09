/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ql.rsql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.eclipse.hawkbit.repository.ActionStatusCodeManagement;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.ActionStatusCode;
import org.eclipse.hawkbit.repository.qfields.ActionStatusCodeFields;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * Test for RSQL queries on {@link ActionStatusCodeFields}.
 */
class RsqlActionStatusCodeFieldsTest extends AbstractJpaIntegrationTest {

    @Autowired
    private ActionStatusCodeManagement<ActionStatusCode> actionStatusCodeManagement;

    @BeforeEach
    void setup() {
        actionStatusCodeManagement.create(
                ActionStatusCodeManagement.Create.builder()
                        .code(100)
                        .name("TestCode")
                        .severity(ActionStatusCode.Severity.ERROR)
                        .build()
        );
    }

    @Test
    void testFilterByCode() {
        assertRSQLQuery(ActionStatusCodeFields.CODE.name() + "==100", 1);
        assertRSQLQuery(ActionStatusCodeFields.CODE.name() + "!=100", 0);
    }

    @Test
    void testFilterByName() {
        assertRSQLQuery(ActionStatusCodeFields.NAME.name() + "==TestCode", 1);
        assertRSQLQuery(ActionStatusCodeFields.NAME.name() + "==Test*", 1);
        assertRSQLQuery(ActionStatusCodeFields.NAME.name() + "!=TestCode", 0);
    }

    @Test
    void testFilterBySeverity() {
        assertRSQLQuery(ActionStatusCodeFields.SEVERITY.name() + "==ERROR", 1);
        assertRSQLQuery(ActionStatusCodeFields.SEVERITY.name() + "==INFO", 0);
    }

    @Test
    void testUnsupportedFieldRaisesException() {
        assertThatThrownBy(() -> assertRSQLQuery("description==test", 0))
                .isInstanceOf(RSQLParameterUnsupportedFieldException.class);
    }

    private void assertRSQLQuery(final String rsql, final long expectedCount) {
        final Page<ActionStatusCode> result = actionStatusCodeManagement.findByRsql(
                rsql,
                PageRequest.of(0, 100)
        );
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(expectedCount);
    }
}
