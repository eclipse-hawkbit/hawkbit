/**
 * Copyright (c) 2025 Bosch Digital GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.repository.artifact.urlhandler;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.hawkbit.repository.artifact.urlhandler.Base62Util;
import org.junit.jupiter.api.Test;

/**
 * Feature: Unit Tests - Artifact URL Handler<br/>
 * Story: Base62 Utility tests
 */
class Base62UtilTest {

    /**
     * Convert Base10 numbers to Base62 ASCII strings.
     */
    @Test
    void fromBase10() {
        assertThat(Base62Util.fromBase10(0L)).isEqualTo("0");
        assertThat(Base62Util.fromBase10(11L)).isEqualTo("B");
        assertThat(Base62Util.fromBase10(36L)).isEqualTo("a");
        assertThat(Base62Util.fromBase10(999L)).isEqualTo("G7");
    }

    /**
     * Convert Base62 ASCII strings to Base10 numbers.
     */
    @Test
    void toBase10() {
        assertThat(Base62Util.toBase10("0")).isZero();
        assertThat(Base62Util.toBase10("B")).isEqualTo(11);
        assertThat(Base62Util.toBase10("a")).isEqualTo(36L);
        assertThat(Base62Util.toBase10("G7")).isEqualTo(999L);
    }
}