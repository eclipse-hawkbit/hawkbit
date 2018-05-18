/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.login;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 * Tests for {@link AbstractHawkbitLoginUI}
 *
 */
@Features("Unit Tests - Management UI")
@Stories("Login UI")
public class AbstractHawkbitLoginUITest {

    @Test
    @Description("Verfies that forbidden content is disallowed.")
    public void isAllowedCookieValue() {
        assertThat(AbstractHawkbitLoginUI.isAllowedCookieValue("<script>test</script>")).isFalse();
        assertThat(AbstractHawkbitLoginUI.isAllowedCookieValue("\n<script>test</script>foobar")).isFalse();
        assertThat(AbstractHawkbitLoginUI.isAllowedCookieValue("foobar<script>test</script>")).isFalse();
        assertThat(AbstractHawkbitLoginUI.isAllowedCookieValue("\nfoobar<script>test</script>")).isFalse();
    }

}
