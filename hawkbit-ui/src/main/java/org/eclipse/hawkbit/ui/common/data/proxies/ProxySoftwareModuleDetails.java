/**
 * Copyright (c) 2020 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.common.data.proxies;

import java.security.SecureRandom;

/**
 * Proxy to display details for Software Modules.
 */
public class ProxySoftwareModuleDetails extends ProxyIdentifiableEntity {
    private static final long serialVersionUID = 1L;

    private final boolean isMandatory;
    private final Long typeId;
    private final String typeName;
    private final Long smId;
    private final String smNameAndVersion;

    /**
     * Constructor for ProxySoftwareModuleDetails
     *
     * @param isMandatory
     *          flag for software module detail
     * @param typeId
     *          Software module detail type id
     * @param typeName
     *          Software module detail type name
     * @param smId
     *          Software module detail id
     * @param smNameAndVersion
     *          Software module detail name and version
     */
    public ProxySoftwareModuleDetails(final boolean isMandatory, final Long typeId, final String typeName,
            final Long smId, final String smNameAndVersion) {
        super(new SecureRandom().nextLong());

        this.isMandatory = isMandatory;
        this.typeId = typeId;
        this.typeName = typeName;
        this.smId = smId;
        this.smNameAndVersion = smNameAndVersion;
    }

    /**
     * Flag that indicates if the software module detail is mandatory.
     *
     * @return <code>true</code> if the software module detail is mandatory, otherwise
     *         <code>false</code>
     */
    public boolean isMandatory() {
        return isMandatory;
    }

    /**
     * Gets the id of software module detail type
     *
     * @return typeId
     */
    public Long getTypeId() {
        return typeId;
    }

    /**
     * Gets the software module detail type name
     *
     * @return typeName
     */
    public String getTypeName() {
        return typeName;
    }

    /**
     * Gets the id of software module detail
     *
     * @return smId
     */
    public Long getSmId() {
        return smId;
    }

    /**
     * Gets the software module detail name and version
     *
     * @return smNameAndVersion
     */
    public String getSmNameAndVersion() {
        return smNameAndVersion;
    }
}
