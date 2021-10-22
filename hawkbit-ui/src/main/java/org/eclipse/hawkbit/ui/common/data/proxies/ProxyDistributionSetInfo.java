/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.data.proxies;

import java.util.Objects;

import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;

/**
 * Holds information about a distribution set
 */
public class ProxyDistributionSetInfo extends ProxyIdentifiableEntity {
    private static final long serialVersionUID = 1L;

    private String name;
    private String version;
    private String nameVersion;
    private Long dsTypeId;
    private boolean isValid;

    /**
     * Constructor
     */
    public ProxyDistributionSetInfo() {
        super();
    }

    /**
     * Constructor
     *
     * @param id
     *            distribution set ID
     * @param name
     *            distribution set name
     * @param version
     *            distribution set version
     * @param dsTypeId
     *            ID of the assigned dsType
     * @param isValid
     *            invalidation state
     */
    public ProxyDistributionSetInfo(final Long id, final String name, final String version, final Long dsTypeId,
            final boolean isValid) {
        super(id);

        this.name = name;
        this.version = version;
        this.dsTypeId = dsTypeId;
        this.isValid = isValid;
        this.nameVersion = HawkbitCommonUtil.getFormattedNameVersion(name, version);
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

    public String getNameVersion() {
        return nameVersion;
    }

    public void setNameVersion(final String nameVersion) {
        this.nameVersion = nameVersion;
    }

    public boolean isValid() {
        return isValid;
    }

    public void setValid(final boolean isValid) {
        this.isValid = isValid;
    }

    public Long getDsTypeId() {
        return dsTypeId;
    }

    public void setDsTypeId(final Long dsTypeId) {
        this.dsTypeId = dsTypeId;
    }

    @Override
    public int hashCode() {
        // nameVersion is ignored because it is a composition of name and
        // version
        return Objects.hash(getId(), getName(), getVersion(), getDsTypeId(), isValid());
    }

    // equals method requires all of the used conditions
    @SuppressWarnings("squid:S1067")
    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ProxyDistributionSetInfo other = (ProxyDistributionSetInfo) obj;

        // nameVersion is ignored because it is a composition of name and
        // version
        return Objects.equals(this.getId(), other.getId()) && Objects.equals(this.getName(), other.getName())
                && Objects.equals(this.getVersion(), other.getVersion())
                && Objects.equals(this.getDsTypeId(), other.getDsTypeId())
                && Objects.equals(this.isValid(), other.isValid());
    }
}
