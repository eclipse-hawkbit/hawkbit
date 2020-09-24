/** 
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.tenantconfiguration.window;

import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.data.providers.DistributionSetTypeDataProvider;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTypeInfo;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

/**
 * Dependencies for system config window
 */
public class SystemConfigWindowDependencies {

    private final SystemManagement systemManagement;
    private final VaadinMessageSource i18n;
    private final SpPermissionChecker permissionChecker;
    private final DistributionSetTypeManagement distributionSetTypeManagement;
    private final DistributionSetTypeDataProvider<ProxyTypeInfo> distributionSetTypeDataProvider;

    /**
     * Constructor for VaadinMessageSource
     *
     * @param systemManagement
     *            SystemManagement
     * @param i18n
     *            VaadinMessageSource
     * @param permissionChecker
     *            SpPermissionChecker
     * @param distributionSetTypeManagement
     *            DistributionSetTypeManagement
     * @param distributionSetTypeDataProvider
     *            DistributionSetTypeDataProvider
     */
    public SystemConfigWindowDependencies(final SystemManagement systemManagement, final VaadinMessageSource i18n,
            final SpPermissionChecker permissionChecker,
            final DistributionSetTypeManagement distributionSetTypeManagement,
            final DistributionSetTypeDataProvider<ProxyTypeInfo> distributionSetTypeDataProvider) {
        this.systemManagement = systemManagement;
        this.i18n = i18n;
        this.permissionChecker = permissionChecker;
        this.distributionSetTypeManagement = distributionSetTypeManagement;
        this.distributionSetTypeDataProvider = distributionSetTypeDataProvider;
    }

    /**
     * @return System management
     */
    public SystemManagement getSystemManagement() {
        return systemManagement;
    }

    /**
     * @return Vaadin message source
     */
    public VaadinMessageSource getI18n() {
        return i18n;
    }

    /**
     * @return Permission checker
     */
    public SpPermissionChecker getPermissionChecker() {
        return permissionChecker;
    }

    /**
     * @return Distribution set type management
     */
    public DistributionSetTypeManagement getDistributionSetTypeManagement() {
        return distributionSetTypeManagement;
    }

    /**
     * @return Distribution set type data provider
     */
    public DistributionSetTypeDataProvider<ProxyTypeInfo> getDistributionSetTypeDataProvider() {
        return distributionSetTypeDataProvider;
    }
}
