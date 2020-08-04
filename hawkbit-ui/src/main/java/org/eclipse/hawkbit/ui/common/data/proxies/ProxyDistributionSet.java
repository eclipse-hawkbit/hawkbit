/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.data.proxies;

import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.ui.common.data.aware.TypeInfoAware;
import org.eclipse.hawkbit.ui.common.data.aware.VersionAware;

/**
 * Proxy for {@link DistributionSet}.
 */
public class ProxyDistributionSet extends ProxyNamedEntity implements VersionAware, TypeInfoAware {
    private static final long serialVersionUID = 1L;

    private Boolean isComplete;

    private String version;

    private String nameVersion;

    private ProxyTypeInfo typeInfo;

    private boolean requiredMigrationStep;

    /**
     * Default constructor
     */
    public ProxyDistributionSet() {
    }

    /**
     * Constructor for ProxyDistributionSet
     *
     * @param id
     *            Id of distribution set
     */
    public ProxyDistributionSet(final Long id) {
        super(id);
    }

    /**
     * Gets the nameVersion
     *
     * @return name of the version
     */
    public String getNameVersion() {
        return nameVersion;
    }

    /**
     * Sets the name of the version
     *
     * @param nameVersion
     *            name of the version
     */
    public void setNameVersion(final String nameVersion) {
        this.nameVersion = nameVersion;
    }

    /**
     * Flag that indicates if the action is complete.
     *
     * @return <code>true</code> if the action is complete, otherwise
     *         <code>false</code>
     */
    public Boolean getIsComplete() {
        return isComplete;
    }

    /**
     * Sets the flag that indicates if the action is complete
     *
     * @param isComplete
     *            <code>true</code> if the action is complete, otherwise
     *            <code>false</code>
     */
    public void setIsComplete(final Boolean isComplete) {
        this.isComplete = isComplete;
    }

    /**
     * Flag that indicates if the migration step is required.
     *
     * @return <code>true</code> if the action is complete, otherwise
     *         <code>false</code>
     */
    public boolean isRequiredMigrationStep() {
        return requiredMigrationStep;
    }

    /**
     * Sets the flag that indicates if the migration step is required
     *
     * @param requiredMigrationStep
     *            <code>true</code> if th migration step is required, otherwise
     *            <code>false</code>
     */
    public void setRequiredMigrationStep(final boolean requiredMigrationStep) {
        this.requiredMigrationStep = requiredMigrationStep;
    }

    @Override
    public ProxyTypeInfo getTypeInfo() {
        return typeInfo;
    }

    @Override
    public void setTypeInfo(final ProxyTypeInfo typeInfo) {
        this.typeInfo = typeInfo;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public void setVersion(final String version) {
        this.version = version;
    }

    /**
     * Sets the Id, name and version of distribution set
     *
     * @param dsIdNameVersion
     *            ProxyIdNameVersion
     *
     * @return proxy of distribution set
     */
    public ProxyDistributionSet of(final ProxyIdNameVersion dsIdNameVersion) {
        final ProxyDistributionSet ds = new ProxyDistributionSet();

        ds.setId(dsIdNameVersion.getId());
        ds.setName(dsIdNameVersion.getName());
        ds.setVersion(dsIdNameVersion.getVersion());

        return ds;
    }

    /**
     * Gets the Id, Name and version of distribution set
     *
     * @return proxy of Id, Name and version
     */
    public ProxyIdNameVersion getIdNameVersion() {
        return new ProxyIdNameVersion(getId(), getName(), getVersion());
    }
}
