/** 
 * Copyright (c) 2022 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.layout.listener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;

import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties;
import org.eclipse.hawkbit.ui.common.event.EventTopics;
import org.eclipse.hawkbit.ui.common.event.TenantConfigChangedEventPayload;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

/**
 * Event listener for tenant configuration changes
 */
public class TenantConfigChangedListener extends TopicEventListener {

    private final String tenantFilter;
    private final Collection<String> tenantConfigFilter;
    private final Consumer<TenantConfigChangedEventPayload> onApplicationScope;
    private final Consumer<TenantConfigChangedEventPayload> onUiScope;

    /**
     * Constructor for TenantConfigChangedListener
     *
     * @param eventBus
     *            UIEventBus
     * @param tenantFilter
     *            filter events related to a specific tenant
     * @param tenantConfigFilter
     *            filter the tenant configurations of interest listed in
     *            {@link TenantConfigurationProperties.TenantConfigurationKey}
     * @param onApplicationScope
     *            defines what should happen on an application wide event
     * @param onUiScope
     *            defines what should happen on current UI event
     */
    TenantConfigChangedListener(final UIEventBus eventBus, final String tenantFilter,
            final Collection<String> tenantConfigFilter,
            final Consumer<TenantConfigChangedEventPayload> onApplicationScope,
            final Consumer<TenantConfigChangedEventPayload> onUiScope) {
        super(eventBus, EventTopics.TENANT_CONFIG_CHANGED);

        this.tenantFilter = tenantFilter;
        this.tenantConfigFilter = tenantConfigFilter;
        this.onApplicationScope = onApplicationScope;
        this.onUiScope = onUiScope;
    }

    /**
     * Get a builder instance to define the {@link TenantConfigChangedListener}
     * 
     * @param eventBus
     *            is required
     * @return an the empty {@link Builder}
     */
    public static Builder newBuilder(final UIEventBus eventBus) {
        return new Builder(eventBus);
    }

    /**
     * With scope {@link EventScope#APPLICATION} to get notified about configuration
     * changes triggered by other UI sessions/users
     *
     * @param configuration
     *            the new configuration
     */
    @EventBusListenerMethod(scope = EventScope.APPLICATION)
    private void onApplicationEvent(final TenantConfigChangedEventPayload configuration) {
        if (onApplicationScope != null && shouldProcess(configuration)) {
            onApplicationScope.accept(configuration);
        }
    }

    /**
     * With scope {@link EventScope#UI} to get notified about configuration changes
     * triggered by the current UI
     *
     * @param configuration
     *            the new configuration
     */
    @EventBusListenerMethod(scope = EventScope.UI)
    private void onUiEvent(final TenantConfigChangedEventPayload configuration) {
        if (onUiScope != null && shouldProcess(configuration)) {
            onUiScope.accept(configuration);
        }
    }

    private boolean shouldProcess(final TenantConfigChangedEventPayload configuration) {
        return (tenantFilter == null || tenantFilter.equalsIgnoreCase(configuration.getTenant()))
                && tenantConfigFilter.contains(configuration.getKey());
    }

    /**
     * Builder to define the structure of the listener
     */
    public static class Builder {
        private final UIEventBus eventBus;
        private String tenantFilter;
        private Collection<String> tenantConfigFilter;
        private Consumer<TenantConfigChangedEventPayload> onApplicationScope;
        private Consumer<TenantConfigChangedEventPayload> onUiScope;

        Builder(final UIEventBus eventBus) {
            this.eventBus = eventBus;
        }

        /**
         * To filter for event for a specific tenant.
         */
        public Builder tenantFilter(final String tenantFilter) {
            this.tenantFilter = tenantFilter;
            return this;
        }

        /**
         * Set filter for events for specific tenant configuration.
         */
        public Builder setConfigFilter(final Collection<String> configFilter) {
            this.tenantConfigFilter = configFilter;
            return this;
        }

        /**
         * Add filter for events for specific tenant configuration.
         */
        public Builder addConfigFilter(final String configFilter) {
            if (this.tenantConfigFilter == null) {
                this.tenantConfigFilter = new ArrayList<>();
            }
            this.tenantConfigFilter.add(configFilter);
            return this;
        }

        /**
         * Set consumer in case of an event with scope {@link EventScope#APPLICATION}.
         */
        public Builder applicationEventConsumer(
                final Consumer<TenantConfigChangedEventPayload> applicationEventConsumer) {
            this.onApplicationScope = applicationEventConsumer;
            return this;
        }

        /**
         * Set consumer in case of an event with scope {@link EventScope#UI}.
         */
        public Builder uiEventConsumer(final Consumer<TenantConfigChangedEventPayload> uiEventConsumer) {
            this.onUiScope = uiEventConsumer;
            return this;
        }

        /**
         * Build the instance based on provided values.
         * 
         * @return an instance of {@link TenantConfigChangedListener}
         */
        public TenantConfigChangedListener build() {
            return new TenantConfigChangedListener(eventBus, tenantFilter,
                    tenantConfigFilter == null ? Collections.emptyList() : tenantConfigFilter, onApplicationScope,
                    onUiScope);
        }

    }
}
