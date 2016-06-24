/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.autoconfigure.scheduling;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties for the async configurer.
 * 
 */
@ConfigurationProperties("hawkbit.threadpool")
public class AsyncConfigurerThreadpoolProperties {

    /**
     * Max queue size for central event executor.
     */
    private Integer queuesize = 5_000;

    /**
     * Core processing threads for central event executor.
     */
    private Integer corethreads = 5;

    /**
     * Maximum thread pool size for central event executor.
     */
    private Integer maxthreads = 20;

    /**
     * Core processing threads for scheduled event executor.
     */
    private Integer schedulerThreads = 3;

    /**
     * When the number of threads is greater than the core, this is the maximum
     * time that excess idle threads will wait for new tasks before terminating.
     */
    private Long idletimeout = 10000L;

    public Integer getQueuesize() {
        return queuesize;
    }

    public void setQueuesize(final Integer queuesize) {
        this.queuesize = queuesize;
    }

    public Integer getCorethreads() {
        return corethreads;
    }

    public void setCorethreads(final Integer corethreads) {
        this.corethreads = corethreads;
    }

    public Integer getMaxthreads() {
        return maxthreads;
    }

    public void setMaxthreads(final Integer maxthreads) {
        this.maxthreads = maxthreads;
    }

    public Long getIdletimeout() {
        return idletimeout;
    }

    public void setIdletimeout(final Long idletimeout) {
        this.idletimeout = idletimeout;
    }

    public Integer getSchedulerThreads() {
        return schedulerThreads;
    }

    public void setSchedulerThreads(final Integer schedulerThreads) {
        this.schedulerThreads = schedulerThreads;
    }

}
