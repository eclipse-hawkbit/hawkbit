/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.common.data.proxies;

import com.google.common.base.MoreObjects;
import java.util.Set;
import org.eclipse.hawkbit.repository.model.TargetType;

import java.util.Objects;

/**
 * Proxy for {@link TargetType}.
 */
public class ProxyTargetType extends ProxyFilterButton {

    private static final long serialVersionUID = 1L;

    private boolean isNoTargetType;

    private Set<ProxyType> selectedDsTypes;

    /**
     * Constructor
     */
    public ProxyTargetType() {
    }

    /**
     * Constructor for ProxyTargetType
     *
     * @param id
     *          Type id
     * @param name
     *          Type name
     * @param colour
     *          Type colour
     */
    public ProxyTargetType(final Long id, final String name, final String colour) {
        setId(id);
        setName(name);
        setColour(colour);
    }

    public boolean isNoTargetType() {
        return isNoTargetType;
    }

    public void setNoTargetType(boolean noTargetType) {
        isNoTargetType = noTargetType;
    }

    /**
     * Gets the selected distribution sets types
     *
     * @return selectedDsTypes
     */
    public Set<ProxyType> getSelectedDsTypes() {
        return selectedDsTypes;
    }

    /**
     * Sets the selectedDsTypes
     *
     * @param selectedDsTypes
     *            Selected distribution sets types
     */
    public void setSelectedDsTypes(final Set<ProxyType> selectedDsTypes) {
        this.selectedDsTypes = selectedDsTypes;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ProxyTargetType other = (ProxyTargetType) obj;
        return Objects.equals(this.getId(), other.getId()) && Objects.equals(this.getName(), other.getName())
                && Objects.equals(this.getColour(), other.getColour());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getName(), getColour());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("id", getId()).add("name", getName()).add("color", getColour())
                .toString();
    }
}
