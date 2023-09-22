package org.eclipse.hawkbit.repository.jpa.acm;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

public class DefaultAccessControllingConfiguration {

    @Bean
    @ConditionalOnMissingBean
    TargetAccessController targetAccessControlManager() {
        return DefaultAccessController.targetAccessController();
    }

    @Bean
    @ConditionalOnMissingBean
    TargetTypeAccessController targetTypeAccessControlManager() {
        return DefaultAccessController.targetTypeAccessController();
    }

    @Bean
    @ConditionalOnMissingBean
    DistributionSetAccessController distributionSetAccessController() {
        return DefaultAccessController.distributionSetAccessController();
    }

    @Bean
    @ConditionalOnMissingBean
    DistributionSetTypeAccessController distributionSetTypeAccessController() {
        return DefaultAccessController.distributionSetTypeAccessController();
    }

}
