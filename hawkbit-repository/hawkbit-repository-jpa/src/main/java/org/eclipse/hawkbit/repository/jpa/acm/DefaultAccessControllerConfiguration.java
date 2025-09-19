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

import java.util.List;

import lombok.Getter;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.DistributionSetFields;
import org.eclipse.hawkbit.repository.DistributionSetTypeFields;
import org.eclipse.hawkbit.repository.RsqlQueryField;
import org.eclipse.hawkbit.repository.SoftwareModuleFields;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeFields;
import org.eclipse.hawkbit.repository.TagFields;
import org.eclipse.hawkbit.repository.TargetFields;
import org.eclipse.hawkbit.repository.TargetTypeFields;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetType;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleType;
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
        return new DefaultAccessController<>(TargetFields.class, SpPermission.TARGET);
    }

    // after adding support for internal action field search support it add matchIfMissing = true
    @Bean
    @ConditionalOnProperty(name = "hawkbit.acm.access-controller.action.enabled", havingValue = "true", matchIfMissing = false)
    AccessController<JpaAction> actionAccessController() {
        return new DefaultAccessController<>(ActionFieldsInternal.class, SpPermission.TARGET);
    }

    @Bean
    @ConditionalOnProperty(name = "hawkbit.acm.access-controller.target-type.enabled", havingValue = "true")
    AccessController<JpaTargetType> targetTypeAccessController() {
        return new DefaultAccessController<>(TargetTypeFields.class, SpPermission.TARGET_TYPE);
    }

    @Bean
    @ConditionalOnProperty(name = "hawkbit.acm.access-controller.software-module.enabled", havingValue = "true", matchIfMissing = true)
    AccessController<JpaSoftwareModule> softwareModuleAccessController() {
        return new DefaultAccessController<>(SoftwareModuleFields.class, SpPermission.SOFTWARE_MODULE);
    }

    @Bean
    @ConditionalOnProperty(name = "hawkbit.acm.access-controller.software-module-type.enabled", havingValue = "true")
    AccessController<JpaSoftwareModuleType> softwareModuleTypeAccessController() {
        return new DefaultAccessController<>(SoftwareModuleTypeFields.class, SpPermission.SOFTWARE_MODULE_TYPE);
    }

    @Bean
    @ConditionalOnProperty(name = "hawkbit.acm.access-controller.distribution-set.enabled", havingValue = "true", matchIfMissing = true)
    AccessController<JpaDistributionSet> distributionSetAccessController() {
        return new DefaultAccessController<>(DistributionSetFields.class, SpPermission.DISTRIBUTION_SET);
    }

    @Bean
    @ConditionalOnProperty(name = "hawkbit.acm.access-controller.distribution-set-type.enabled", havingValue = "true")
    AccessController<JpaDistributionSetType> distributionSetTypeAccessController() {
        return new DefaultAccessController<>(DistributionSetTypeFields.class, SpPermission.DISTRIBUTION_SET_TYPE);
    }

    // contains the same fields as TargetFields, but with "target." prefix for JPA queries in order to be applied to JpaAction repository
    @Getter
    public enum ActionFieldsInternal implements RsqlQueryField {

        ID("controllerId"),
        NAME("name"),
        DESCRIPTION("description"),
        CREATEDAT("createdAt"),
        CREATEDBY("createdBy"),
        LASTMODIFIEDAT("lastModifiedAt"),
        LASTMODIFIEDBY("lastModifiedBy"),
        CONTROLLERID("controllerId"),
        UPDATESTATUS("updateStatus"),
        IPADDRESS("address"),
        ATTRIBUTE("controllerAttributes"),
        GROUP("group"),
        ASSIGNEDDS("assignedDistributionSet",
                DistributionSetFields.NAME.getJpaEntityFieldName(), DistributionSetFields.VERSION.getJpaEntityFieldName()),
        INSTALLEDDS("installedDistributionSet",
                DistributionSetFields.NAME.getJpaEntityFieldName(), DistributionSetFields.VERSION.getJpaEntityFieldName()),
        TAG("tags", TagFields.NAME.getJpaEntityFieldName()),
        LASTCONTROLLERREQUESTAT("lastTargetQuery"),
        METADATA("metadata"),
        TARGETTYPE("targetType",
                TargetTypeFields.ID.getJpaEntityFieldName(),
                TargetTypeFields.KEY.getJpaEntityFieldName(),
                TargetTypeFields.NAME.getJpaEntityFieldName());

        private final String jpaEntityFieldName;
        private final List<String> subEntityAttributes;

        ActionFieldsInternal(final String jpaEntityFieldName, final String... subEntityAttributes) {
            this.jpaEntityFieldName = "target." + jpaEntityFieldName;
            this.subEntityAttributes = List.of(subEntityAttributes);
        }

        @Override
        public boolean isMap() {
            return this == ATTRIBUTE || this == METADATA;
        }
    }
}
