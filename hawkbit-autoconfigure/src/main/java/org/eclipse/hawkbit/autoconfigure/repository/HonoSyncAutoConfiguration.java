/**
 * Copyright (c) 2019 Kiwigrid GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.autoconfigure.repository;

import org.eclipse.hawkbit.repository.hono.HonoTargetSync;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Configuration
@ConditionalOnProperty(prefix = "hawkbit.server.repository.hono-sync", name = "devices-uri")
public class HonoSyncAutoConfiguration {

    @Value("${hawkbit.server.repository.hono-sync.poll-rate:60}")
    private Integer pollRate;

    @Bean
    @ConditionalOnMissingBean
    public HonoTargetSync honoTargetSync() {
        HonoTargetSync honoTargetSync = new HonoTargetSync();

        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(honoTargetSync, 0, pollRate, TimeUnit.SECONDS);

        return honoTargetSync;
    }
}
