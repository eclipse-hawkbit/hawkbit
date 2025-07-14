/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.acm;

import org.eclipse.hawkbit.repository.DistributionSetFields;
import org.eclipse.hawkbit.repository.TargetFields;
import org.eclipse.hawkbit.repository.TargetTypeFields;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "hawkbit.acm.access-controller.enabled", havingValue = "true", matchIfMissing = true)
public class DefaultAccessControllerConfiguration {

    @Bean
    @ConditionalOnProperty(name = "hawkbit.acm.access-controller.target.enabled", havingValue = "true", matchIfMissing = true)
    AccessController<JpaTarget> targetAccessController() {
        return new DefaultAccessController<>(TargetFields.class, "TARGET");
    }

    @Bean
    @ConditionalOnProperty(name = "hawkbit.acm.access-controller.target-type.enabled", havingValue = "true", matchIfMissing = true)
    AccessController<JpaTargetType> targetTypeAccessController() {
        return new DefaultAccessController<>(TargetTypeFields.class, "TARGET_TYPE");
    }

    @Bean
    @ConditionalOnProperty(name = "hawkbit.acm.access-controller.distribution-set.enabled", havingValue = "true", matchIfMissing = true)
    AccessController<JpaDistributionSet> distributionSetAccessController() {
        return new DefaultAccessController<>(DistributionSetFields.class, "DISTRIBUTION_SET");
    }
}
