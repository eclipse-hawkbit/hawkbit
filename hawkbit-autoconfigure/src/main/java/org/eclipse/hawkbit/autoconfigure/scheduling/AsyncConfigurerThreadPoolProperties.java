/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.autoconfigure.scheduling;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties for the async configurer.
 */
@Data
@ConfigurationProperties("hawkbit.threadpool")
public class AsyncConfigurerThreadPoolProperties {

    /**
     * Max queue size for central event executor.
     */
    private Integer queueSize = 5_000;

    /**
     * Core processing threads for central event executor.
     */
    private Integer coreThreads = 5;

    /**
     * Maximum thread pool size for central event executor.
     */
    private Integer maxThreads = 20;

    /**
     * Core processing threads for scheduled event executor.
     */
    private Integer schedulerThreads = 3;

    /**
     * When the number of threads is greater than the core, this is the maximum
     * time that excess idle threads will wait for new tasks before terminating.
     */
    private Long idleTimeout = 10000L;
}
