/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.eclipse.hawkbit.security.HawkbitSecurityProperties.Dos.ControllerAttributes;
import org.junit.jupiter.api.Test;

class HawkbitSecurityPropertiesTest {

    @Test
    void intervalForDefaultsToZeroWhenNothingConfigured() {
        final ControllerAttributes props = new ControllerAttributes();
        assertThat(props.intervalFor("TENANT")).isEqualTo(Duration.ZERO);
        assertThat(props.intervalFor(null)).isEqualTo(Duration.ZERO);
    }

    @Test
    void intervalForFallsBackToDefaultWhenTenantNotListed() {
        final ControllerAttributes props = new ControllerAttributes();
        props.setMinUpdateInterval(Duration.ofMinutes(2));
        assertThat(props.intervalFor("UNLISTED")).isEqualTo(Duration.ofMinutes(2));
    }

    @Test
    void intervalForResolvesPerTenantOverrideCaseInsensitively() {
        final ControllerAttributes props = new ControllerAttributes();
        props.setMinUpdateInterval(Duration.ofMinutes(2));
        props.getPerTenant().put("Abusive", Duration.ofMinutes(5));
        assertThat(props.intervalFor("ABUSIVE")).isEqualTo(Duration.ofMinutes(5));
        assertThat(props.intervalFor("abusive")).isEqualTo(Duration.ofMinutes(5));
        assertThat(props.intervalFor("OTHER")).isEqualTo(Duration.ofMinutes(2));
    }
}
