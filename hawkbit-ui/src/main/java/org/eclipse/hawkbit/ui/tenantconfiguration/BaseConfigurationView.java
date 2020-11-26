/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.tenantconfiguration;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.vaadin.data.Binder;
import com.vaadin.ui.CustomComponent;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.model.TenantConfigurationValue;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxySystemConfigWindow;
import org.springframework.beans.factory.InitializingBean;

/**
 * Base class for all configuration views. This class implements the logic for
 * the handling of the configurations in a consistent way.
 * 
 */
public abstract class BaseConfigurationView<B extends ProxySystemConfigWindow> extends CustomComponent
        implements ConfigurationGroup, InitializingBean {

    private static final long serialVersionUID = 1L;

    private final List<ConfigurationItemChangeListener> configurationChangeListeners = new ArrayList<>();
    private final transient TenantConfigurationManagement tenantConfigurationManagement;
    private final Binder<B> binder;

    public BaseConfigurationView(final TenantConfigurationManagement tenantConfigurationManagement) {
        this.tenantConfigurationManagement = tenantConfigurationManagement;
        binder = new Binder<>();
    }

    @Override
    public void afterPropertiesSet() {
        binder.setBean(populateSystemConfig());
    }

    protected abstract B populateSystemConfig();

    @Override
    public void addChangeListener(final ConfigurationItemChangeListener listener) {
        configurationChangeListeners.add(listener);
    }

    @Override
    public boolean isUserInputValid() {
        // default return value is true, because often user can only choose from
        // different valid options.
        return true;
    }

    @Override
    public void undo() {
        binder.setBean(populateSystemConfig());
    }

    protected boolean readConfigOption(final String configurationKey) {
        final TenantConfigurationValue<Boolean> enabled = tenantConfigurationManagement
                .getConfigurationValue(configurationKey, Boolean.class);

        return enabled.getValue() && !enabled.isGlobal();
    }

    protected <T extends Serializable> void writeConfigOption(final String key, final T value) {
        tenantConfigurationManagement.addOrUpdateConfiguration(key, value);
    }

    protected TenantConfigurationManagement getTenantConfigurationManagement() {
        return tenantConfigurationManagement;
    }

    protected Binder<B> getBinder() {
        return binder;
    }

    protected B getBinderBean() {
        return getBinder().getBean();
    }

}
