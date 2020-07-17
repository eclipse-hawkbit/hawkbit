/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.data.proxies;

import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.ui.common.data.aware.TypeInfoAware;
import org.eclipse.hawkbit.ui.common.data.aware.VersionAware;

/**
 * Proxy for {@link SoftwareModule} to display details in Software modules
 * table.
 */
public class ProxySoftwareModule extends ProxyNamedEntity implements VersionAware, TypeInfoAware {
    private static final long serialVersionUID = 1L;

    private String version;

    private String nameAndVersion;

    private String vendor;

    private ProxyTypeInfo typeInfo;

    private boolean assigned;

    /**
     * Gets the software module vendor
     *
     * @return vendor
     */
    public String getVendor() {
        return vendor;
    }

    /**
     * Sets the vendor
     *
     * @param vendor
     *            software module vendor
     */
    public void setVendor(final String vendor) {
        this.vendor = vendor;
    }

    /**
     * Gets the software module name and version
     *
     * @return nameAndVersion
     */
    public String getNameAndVersion() {
        return nameAndVersion;
    }

    /**
     * Sets the nameAndVersion
     *
     * @param nameAndVersion
     *            software module name and version
     */
    public void setNameAndVersion(final String nameAndVersion) {
        this.nameAndVersion = nameAndVersion;
    }

    /**
     * Flag that indicates if the software module is assigned.
     *
     * @return <code>true</code> if the software module is assigned, otherwise
     *         <code>false</code>
     */
    public boolean isAssigned() {
        return assigned;
    }

    /**
     * Sets the flag that indicates if the software module is assigned.
     *
     * @param assigned
     *            <code>true</code> if the software module is assigned,
     *            otherwise <code>false</code>
     */
    public void setAssigned(final boolean assigned) {
        this.assigned = assigned;
    }

    /**
     * Gets the typeInfo
     *
     * @return typeInfo
     */
    @Override
    public ProxyTypeInfo getTypeInfo() {
        return typeInfo;
    }

    /**
     * Sets the typeInfo
     *
     * @param typeInfo
     *            typeInfo
     */
    @Override
    public void setTypeInfo(final ProxyTypeInfo typeInfo) {
        this.typeInfo = typeInfo;
    }

    /**
     * Gets the software module version
     *
     * @return version
     */
    @Override
    public String getVersion() {
        return version;
    }

    /**
     * Sets the version
     *
     * @param version
     *            software module version
     */
    @Override
    public void setVersion(final String version) {
        this.version = version;
    }
}
