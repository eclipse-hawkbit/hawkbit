/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.data.proxies;

import java.util.Set;

import org.eclipse.hawkbit.repository.model.Type;

/**
 * Proxy for {@link Type}.
 */
public class ProxyType extends ProxyFilterButton {

    private static final long serialVersionUID = 1L;

    private String key;

    private boolean deleted;

    private boolean mandatory;

    private SmTypeAssign smTypeAssign;

    private Set<ProxyType> selectedSmTypes;

    private int maxAssignments;

    /**
     * Constructor for ProxyType
     */
    public ProxyType() {
        this.smTypeAssign = SmTypeAssign.SINGLE;
    }

    /**
     * Gets the key
     *
     * @return key
     */
    public String getKey() {
        return key;
    }

    /**
     * Sets the key
     *
     * @param key
     *            Entity key
     */
    public void setKey(final String key) {
        this.key = key;
    }

    /**
     * Flag that indicates if the entity is deleted.
     *
     * @return <code>true</code> if the entity is deleted, otherwise
     *         <code>false</code>
     */
    public boolean isDeleted() {
        return deleted;
    }

    /**
     * Sets the flag that indicates if the entity is deleted.
     *
     * @param deleted
     *            <code>true</code> if entity is deleted, otherwise
     *            <code>false</code>
     */
    public void setDeleted(final boolean deleted) {
        this.deleted = deleted;
    }

    /**
     * Flag that indicates if the autoAssignment is enabled.
     *
     * @return <code>true</code> if the autoAssignment is enabled, otherwise
     *         <code>false</code>
     */
    public boolean isMandatory() {
        return mandatory;
    }

    /**
     * Sets the flag that indicates if the entity is mandatory.
     *
     * @param mandatory
     *            <code>true</code> if the entity is mandatory, otherwise
     *            <code>false</code>
     */
    public void setMandatory(final boolean mandatory) {
        this.mandatory = mandatory;
    }

    /**
     * Gets the Software module type assign
     *
     * @return smTypeAssign
     */
    public SmTypeAssign getSmTypeAssign() {
        return smTypeAssign;
    }

    /**
     * Sets the smTypeAssign
     *
     * @param smTypeAssign
     *            Software module type assign
     */
    public void setSmTypeAssign(final SmTypeAssign smTypeAssign) {
        this.smTypeAssign = smTypeAssign;
    }

    /**
     * Gets the selected software module types
     *
     * @return selectedSmTypes
     */
    public Set<ProxyType> getSelectedSmTypes() {
        return selectedSmTypes;
    }

    /**
     * Sets the selectedSmTypes
     *
     * @param selectedSmTypes
     *            Selected software module types
     */
    public void setSelectedSmTypes(final Set<ProxyType> selectedSmTypes) {
        this.selectedSmTypes = selectedSmTypes;
    }

    /**
     * Gets the maxAssignments
     *
     * @return maxAssignments
     */
    public int getMaxAssignments() {
        return maxAssignments;
    }

    /**
     * Sets the maxAssignments
     *
     * @param maxAssignments
     *            Entity maxAssignments
     */
    public void setMaxAssignments(final int maxAssignments) {
        this.maxAssignments = maxAssignments;
    }

    /**
     * Software module type assign
     */
    public enum SmTypeAssign {
        SINGLE, MULTI;
    }

}
