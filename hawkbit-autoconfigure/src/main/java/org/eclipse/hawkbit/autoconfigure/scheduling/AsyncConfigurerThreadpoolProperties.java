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
 *
 */
@ConfigurationProperties("hawkbit.threadpool")
public class AsyncConfigurerThreadpoolProperties {

    private Integer queuesize = 250;

    private Integer corethreads = 5;

    private Integer maxthreads = 50;

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

}
