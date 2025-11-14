/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ql.rsql;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.hawkbit.ql.Node;
import org.junit.jupiter.api.Test;

class NodeTest {

    @Test
    void testSimplify() {
        final Node aEqB = new Node.Comparison("a", Node.Comparison.Operator.EQ, "b");
        final Node aEqC = new Node.Comparison("a", Node.Comparison.Operator.EQ, "c");
        final Node aEqD = new Node.Comparison("a", Node.Comparison.Operator.EQ, "d");
        final Node aEqE = new Node.Comparison("a", Node.Comparison.Operator.EQ, "e");

        assertThat(aEqB.and(aEqC).and(aEqD).and(aEqE).getChildren())
                .hasSize(4)
                .containsExactly(aEqB, aEqC, aEqD, aEqE);
        assertThat(aEqB.and(aEqC).and(aEqD.and(aEqE)).getChildren())
                .hasSize(4)
                .containsExactly(aEqB, aEqC, aEqD, aEqE);
        assertThat(aEqB.or(aEqC).or(aEqD).or(aEqE).getChildren())
                .hasSize(4)
                .containsExactly(aEqB, aEqC, aEqD, aEqE);
        assertThat(aEqB.or(aEqC).or(aEqD.or(aEqE)).getChildren())
                .hasSize(4)
                .containsExactly(aEqB, aEqC, aEqD, aEqE);
    }
}
