/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.api;

import static org.assertj.core.api.Assertions.assertThat;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Test;

@Feature("Unit Tests - Artifact URL Handler")
@Story("Base62 Utility tests")
class Base62UtilTest {

    @Test
    @Description("Convert Base10 numbres to Base62 ASCII strings.")
    void fromBase10() {
        assertThat(Base62Util.fromBase10(0L)).isEqualTo("0");
        assertThat(Base62Util.fromBase10(11L)).isEqualTo("B");
        assertThat(Base62Util.fromBase10(36L)).isEqualTo("a");
        assertThat(Base62Util.fromBase10(999L)).isEqualTo("G7");
    }

    @Test
    @Description("Convert Base62 ASCII strings to Base10 numbers.")
    void toBase10() {
        assertThat(Base62Util.toBase10("0")).isZero();
        assertThat(Base62Util.toBase10("B")).isEqualTo(11);
        assertThat(Base62Util.toBase10("a")).isEqualTo(36L);
        assertThat(Base62Util.toBase10("G7")).isEqualTo(999L);
    }
}
