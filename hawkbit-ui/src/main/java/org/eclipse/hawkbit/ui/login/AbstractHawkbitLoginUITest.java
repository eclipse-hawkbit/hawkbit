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

/**
 * Tests for {@link AbstractHawkbitLoginUI}
 *
 */
public class AbstractHawkbitLoginUITest {

    @Test
    @Description("Verfies that forbidden content is disallowed.")
    public void isAllowedCookieContent() {
        assertThat(AbstractHawkbitLoginUI.isAllowedCookieContent("<script>test</script>")).isFalse();
        assertThat(AbstractHawkbitLoginUI.isAllowedCookieContent("\n<script>test</script>foobar")).isFalse();
        assertThat(AbstractHawkbitLoginUI.isAllowedCookieContent("foobar<script>test</script>")).isFalse();
        assertThat(AbstractHawkbitLoginUI.isAllowedCookieContent("\nfoobar<script>test</script>")).isFalse();
    }

}
