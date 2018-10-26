/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Unit Tests - Artifact URL Handler")
@Stories("Base62 Utility tests")
public class Base62UtilTest {

    @Test
    @Description("Convert Base10 numbres to Base62 ASCII strings.")
    public void fromBase10() {
        assertThat(Base62Util.fromBase10(0L)).isEqualTo("0");
        assertThat(Base62Util.fromBase10(11L)).isEqualTo("B");
        assertThat(Base62Util.fromBase10(36L)).isEqualTo("a");
        assertThat(Base62Util.fromBase10(999L)).isEqualTo("G7");
    }

    @Test
    @Description("Convert Base62 ASCII strings to Base10 numbers.")
    public void toBase10() {
        assertThat(Base62Util.toBase10("0")).isEqualTo(0L);
        assertThat(Base62Util.toBase10("B")).isEqualTo(11);
        assertThat(Base62Util.toBase10("a")).isEqualTo(36L);
        assertThat(Base62Util.toBase10("G7")).isEqualTo(999L);
    }
}
