/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.sdk;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.hateoas.config.EnableHypermediaSupport;

@Slf4j
@Configuration
@EnableConfigurationProperties({ HawkbitServer.class, Tenant.class })
@EnableHypermediaSupport(type = EnableHypermediaSupport.HypermediaType.HAL)
@Import(FeignClientsConfiguration.class)
@PropertySource("classpath:/hawkbit-sdk-defaults.properties")
public class HawkbitSDKConfiguration {}