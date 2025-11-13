/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.rsql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.hawkbit.repository.jpa.rsql.RsqlParser.parse;

import org.eclipse.hawkbit.repository.qfields.TargetFields;
import org.junit.jupiter.api.Test;

class RsqlParserTest {

    @Test
    void testSimpleComparisonNode() {
        assertThat(parse("a == 6")).hasToString("a == \"6\"");
        assertThat(parse("a != 6")).hasToString("a != \"6\"");
        assertThat(parse("a > 6")).hasToString("a > \"6\"");
        assertThat(parse("a >= 6")).hasToString("a >= \"6\"");
        assertThat(parse("a < 6")).hasToString("a < \"6\"");
        assertThat(parse("a <= 6")).hasToString("a <= \"6\"");
        assertThat(parse("a =in= 6")).hasToString("a in (\"6\")");
        assertThat(parse("a =in= (6)")).hasToString("a in (\"6\")");
        assertThat(parse("a =in= (6, 7)")).hasToString("a in (\"6\", \"7\")");
        assertThat(parse("a =out= 6")).hasToString("a not in (\"6\")");
        assertThat(parse("a =out= (6)")).hasToString("a not in (\"6\")");
        assertThat(parse("a =out= (6, 7)")).hasToString("a not in (\"6\", \"7\")");
    }

    @Test
    void testNullComparison() {
        assertThat(parse("a == null")).hasToString("a == \"null\"");
        assertThat(parse("a =is= 6")).hasToString("a == \"6\"");
        assertThat(parse("a =is= null")).hasToString("a == null");

        assertThat(parse("a != null")).hasToString("a != \"null\"");
        assertThat(parse("a =not= 6")).hasToString("a != \"6\"");
        assertThat(parse("a =not= null")).hasToString("a != null");
    }

    @Test
    void testLikeComparison() {
        assertThat(parse("a == test*")).hasToString("a like \"test*\"");
        assertThat(parse("a == test\\*")).hasToString("a == \"test\\*\"");

        assertThat(parse("a != test*")).hasToString("a not like \"test*\"");
        assertThat(parse("a != test\\*")).hasToString("a != \"test\\*\"");
    }

    @Test
    void testOrLogical() {
        assertThat(parse("a == 6 or a == 7")).hasToString("a == \"6\" || a == \"7\"");
        assertThat(parse("a == 6 or a == 7 or a == '8'")).hasToString("a == \"6\" || a == \"7\" || a == \"8\"");
        assertThat(parse("(a == 6 or a == 7) or a == '8'")).hasToString("a == \"6\" || a == \"7\" || a == \"8\"");
        assertThat(parse("a == 6 or (a == 7 or a == '8')")).hasToString("a == \"6\" || a == \"7\" || a == \"8\"");
        assertThat(parse("a == 6 or a == 7 or (a == '8')")).hasToString("a == \"6\" || a == \"7\" || a == \"8\"");
    }

    @Test
    void testAndLogical() {
        assertThat(parse("a == 6 and a == 7")).hasToString("a == \"6\" && a == \"7\"");
        assertThat(parse("a == 6 and a == 7 and a == '8'")).hasToString("a == \"6\" && a == \"7\" && a == \"8\"");
        assertThat(parse("(a == 6 and a == 7) and a == '8'")).hasToString("a == \"6\" && a == \"7\" && a == \"8\"");
        assertThat(parse("a == 6 and (a == 7 and a == '8')")).hasToString("a == \"6\" && a == \"7\" && a == \"8\"");
        assertThat(parse("a == 6 and a == 7 and (a == '8')")).hasToString("a == \"6\" && a == \"7\" && a == \"8\"");
    }

    @Test
    void testComplexLogical() {
        assertThat(parse("a == 6 and a == 7 or a == '8'")).hasToString("a == \"6\" && a == \"7\" || a == \"8\"");
        assertThat(parse("(a == 6 or a == 7) and a == '8'")).hasToString("(a == \"6\" || a == \"7\") && a == \"8\"");
        assertThat(parse("a == 6 or (a == 7 and a == '8')")).hasToString("a == \"6\" || a == \"7\" && a == \"8\"");
        assertThat(parse("a == 6 and (a == 7 or a == '8')")).hasToString("a == \"6\" && (a == \"7\" || a == \"8\")");
    }

    @Test
    void testQueryFieldsComparisonNode() {
        // simple attribute
        assertThat(parse("controllerid == 6", TargetFields.class)).hasToString("controllerId == \"6\"");
        // reference attribute
        assertThat(parse("assignedds.name == 6", TargetFields.class)).hasToString("assignedDistributionSet.name == \"6\"");
        // reference attribute with default sub-attribute
        assertThat(parse("tag == 6", TargetFields.class)).hasToString("tags.name == \"6\"");
        assertThat(parse("tag.name == 6", TargetFields.class)).hasToString("tags.name == \"6\"");
        // map attribute
        assertThat(parse("metadata.x == 6", TargetFields.class)).hasToString("metadata.x == \"6\"");
        assertThat(parse("metadata.x.y.z == 6", TargetFields.class)).hasToString("metadata.x.y.z == \"6\""); // with dots
    }
}