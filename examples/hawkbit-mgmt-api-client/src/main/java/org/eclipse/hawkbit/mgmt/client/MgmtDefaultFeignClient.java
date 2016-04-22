/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.client;

import org.eclipse.hawkbit.mgmt.client.resource.MgmtDistributionSetClientResource;
import org.eclipse.hawkbit.mgmt.client.resource.MgmtDistributionSetTagClientResource;
import org.eclipse.hawkbit.mgmt.client.resource.MgmtDistributionSetTypeClientResource;
import org.eclipse.hawkbit.mgmt.client.resource.MgmtDownloadArtifactClientResource;
import org.eclipse.hawkbit.mgmt.client.resource.MgmtDownloadClientResource;
import org.eclipse.hawkbit.mgmt.client.resource.MgmtRolloutClientResource;
import org.eclipse.hawkbit.mgmt.client.resource.MgmtSoftwareModuleClientResource;
import org.eclipse.hawkbit.mgmt.client.resource.MgmtSoftwareModuleTypeClientResource;
import org.eclipse.hawkbit.mgmt.client.resource.MgmtTargetClientResource;
import org.eclipse.hawkbit.mgmt.client.resource.MgmtTargetTagClientResource;
import org.springframework.cloud.netflix.feign.support.ResponseEntityDecoder;

import feign.Feign;
import feign.Feign.Builder;
import feign.Logger;
import feign.Logger.Level;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;

/**
 *
 */
public class MgmtDefaultFeignClient {

    private MgmtDistributionSetClientResource mgmtDistributionSetClientResource;
    private MgmtDistributionSetTagClientResource mgmtDistributionSetTagClientResource;
    private MgmtDistributionSetTypeClientResource mgmtDistributionSetTypeClientResource;
    private MgmtRolloutClientResource mgmtRolloutClientResource;
    private MgmtSoftwareModuleClientResource mgmtSoftwareModuleClientResource;
    private MgmtSoftwareModuleTypeClientResource mgmtSoftwareModuleTypeClientResource;
    private MgmtTargetClientResource mgmtTargetClientResource;
    private MgmtTargetTagClientResource mgmtTargetTagClientResource;
    private MgmtDownloadClientResource mgmtDownloadClientResource;
    private MgmtDownloadArtifactClientResource mgmtDownloadArtifactClientResource;

    private final Builder feignBuilder;
    private final String baseUrl;

    public MgmtDefaultFeignClient(final String baseUrl) {
        feignBuilder = Feign.builder().contract(new IgnoreMultipleConsumersProducersSpringMvcContract())
                .requestInterceptor(new ApplicationJsonRequestHeaderInterceptor()).logLevel(Level.FULL)
                .logger(new Logger.ErrorLogger()).encoder(new JacksonEncoder())
                .decoder(new ResponseEntityDecoder(new JacksonDecoder()));
        this.baseUrl = baseUrl;
    }

    public Builder getFeignBuilder() {
        return feignBuilder;
    }

    public MgmtDistributionSetClientResource getMgmtDistributionSetClientResource() {
        if (mgmtDistributionSetClientResource == null) {
            mgmtDistributionSetClientResource = feignBuilder.target(MgmtDistributionSetClientResource.class,
                    this.baseUrl + MgmtDistributionSetClientResource.PATH);
        }
        return mgmtDistributionSetClientResource;
    }

    public MgmtDistributionSetTagClientResource getMgmtDistributionSetTagClientResource() {
        if (mgmtDistributionSetTagClientResource == null) {
            mgmtDistributionSetTagClientResource = feignBuilder.target(MgmtDistributionSetTagClientResource.class,
                    this.baseUrl + MgmtDistributionSetTagClientResource.PATH);
        }
        return mgmtDistributionSetTagClientResource;
    }

    public MgmtDistributionSetTypeClientResource getMgmtDistributionSetTypeClientResource() {
        if (mgmtDistributionSetTypeClientResource == null) {
            mgmtDistributionSetTypeClientResource = feignBuilder.target(MgmtDistributionSetTypeClientResource.class,
                    this.baseUrl + MgmtDistributionSetTypeClientResource.PATH);
        }
        return mgmtDistributionSetTypeClientResource;
    }

    public MgmtRolloutClientResource getMgmtRolloutClientResource() {
        if (mgmtRolloutClientResource == null) {
            mgmtRolloutClientResource = feignBuilder.target(MgmtRolloutClientResource.class,
                    this.baseUrl + MgmtRolloutClientResource.PATH);
        }
        return mgmtRolloutClientResource;
    }

    public MgmtSoftwareModuleClientResource getMgmtSoftwareModuleClientResource() {
        if (mgmtSoftwareModuleClientResource == null) {
            mgmtSoftwareModuleClientResource = feignBuilder.target(MgmtSoftwareModuleClientResource.class,
                    this.baseUrl + MgmtSoftwareModuleClientResource.PATH);
        }
        return mgmtSoftwareModuleClientResource;
    }

    public MgmtSoftwareModuleTypeClientResource getMgmtSoftwareModuleTypeClientResource() {
        if (mgmtSoftwareModuleTypeClientResource == null) {
            mgmtSoftwareModuleTypeClientResource = feignBuilder.target(MgmtSoftwareModuleTypeClientResource.class,
                    this.baseUrl + MgmtSoftwareModuleTypeClientResource.PATH);
        }
        return mgmtSoftwareModuleTypeClientResource;
    }

    public MgmtTargetClientResource getMgmtTargetClientResource() {
        if (mgmtTargetClientResource == null) {
            mgmtTargetClientResource = feignBuilder.target(MgmtTargetClientResource.class,
                    this.baseUrl + MgmtTargetClientResource.PATH);
        }
        return mgmtTargetClientResource;
    }

    public MgmtTargetTagClientResource getMgmtTargetTagClientResource() {
        if (mgmtTargetTagClientResource == null) {
            mgmtTargetTagClientResource = feignBuilder.target(MgmtTargetTagClientResource.class,
                    this.baseUrl + MgmtTargetTagClientResource.PATH);
        }
        return mgmtTargetTagClientResource;
    }

    public MgmtDownloadClientResource getMgmtDownloadClientResource() {
        if (mgmtDownloadClientResource == null) {
            mgmtDownloadClientResource = feignBuilder.target(MgmtDownloadClientResource.class,
                    this.baseUrl + MgmtDownloadClientResource.PATH);
        }
        return mgmtDownloadClientResource;
    }

    public MgmtDownloadArtifactClientResource getMgmtDownloadArtifactClientResource() {
        if (mgmtDownloadArtifactClientResource == null) {
            mgmtDownloadArtifactClientResource = feignBuilder.target(MgmtDownloadArtifactClientResource.class,
                    this.baseUrl + MgmtDownloadArtifactClientResource.PATH);
        }
        return mgmtDownloadArtifactClientResource;
    }

}
