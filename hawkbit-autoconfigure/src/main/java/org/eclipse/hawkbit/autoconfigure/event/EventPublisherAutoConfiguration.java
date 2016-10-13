/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.autoconfigure.event;

import java.util.concurrent.Executor;

import org.eclipse.hawkbit.repository.event.remote.RemoteTenantAwareEvent;
import org.eclipse.hawkbit.repository.jpa.model.helper.EventPublisherHolder;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.bus.ServiceMatcher;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.ResolvableType;

/**
 * Auto configuration for the event bus.
 *
 */
@Configuration
public class EventPublisherAutoConfiguration {

    @Autowired
    @Qualifier("asyncExecutor")
    private Executor executor;

    @Autowired
    private TenantAware tenantAware;

    /**
     * Server internal event publisher that allows parallel event processing if
     * the event listener is marked as so.
     *
     * @return publisher bean
     */
    @Bean(name = AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME)
    public ApplicationEventMulticaster applicationEventMulticaster() {
        final SimpleApplicationEventMulticaster simpleApplicationEventMulticaster = new TenantAwareApplicationEventPublisher(
                tenantAware);
        simpleApplicationEventMulticaster.setTaskExecutor(executor);
        return simpleApplicationEventMulticaster;
    }

    /**
     * @return the singleton instance of the {@link EventPublisherHolder}
     */
    @Bean
    public EventPublisherHolder eventBusHolder() {
        return EventPublisherHolder.getInstance();
    }

    private static class TenantAwareApplicationEventPublisher extends SimpleApplicationEventMulticaster {

        private final TenantAware tenantAware;

        @Autowired(required = false)
        private ServiceMatcher serviceMatcher;

        /**
         * Constructor.
         * 
         * @param tenantAware
         *            the tenant ware
         */
        protected TenantAwareApplicationEventPublisher(final TenantAware tenantAware) {
            this.tenantAware = tenantAware;
        }

        @Override
        public void multicastEvent(final ApplicationEvent event, final ResolvableType eventType) {
            if (serviceMatcher == null || !(event instanceof RemoteTenantAwareEvent)) {
                super.multicastEvent(event, eventType);
                return;
            }
            final RemoteTenantAwareEvent remoteEvent = (RemoteTenantAwareEvent) event;

            if (serviceMatcher.isFromSelf(remoteEvent)) {
                super.multicastEvent(event, eventType);
                return;
            }

            tenantAware.runAsTenant(remoteEvent.getTenant(), () -> {
                super.multicastEvent(event, eventType);
                return null;
            });
        }

    }

}
