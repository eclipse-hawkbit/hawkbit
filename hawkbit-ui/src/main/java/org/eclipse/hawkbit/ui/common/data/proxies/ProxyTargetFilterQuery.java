/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.data.proxies;

import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.ui.common.data.aware.DsIdAware;

/**
 * Proxy for {@link TargetFilterQuery}.
 */
public class ProxyTargetFilterQuery extends ProxyNamedEntity implements DsIdAware {
    private static final long serialVersionUID = 1L;

    private String query;

    private boolean isAutoAssignmentEnabled;

    private ProxyIdNameVersion autoAssignDsIdNameVersion;

    private ActionType autoAssignActionType;

    /**
     * Constructor
     */
    public ProxyTargetFilterQuery() {
    }

    /**
     * Constructor for ProxyTargetFilterQuery
     *
     * @param id
     *          Target filter query id
     */
    public ProxyTargetFilterQuery(final Long id) {
        super(id);
    }

    /**
     * Gets the query
     *
     * @return query
     */
    public String getQuery() {
        return query;
    }

    /**
     * Sets the query
     *
     * @param query
     *         Target filter query
     */
    public void setQuery(final String query) {
        this.query = query;
    }

    /**
     * Gets the autoAssignActionType
     *
     * @return autoAssignActionType
     */
    public ActionType getAutoAssignActionType() {
        return autoAssignActionType;
    }

    /**
     * Sets the autoAssignActionType
     *
     * @param autoAssignActionType
     *         ActionType
     */
    public void setAutoAssignActionType(final ActionType autoAssignActionType) {
        this.autoAssignActionType = autoAssignActionType;
    }

    /**
     * Flag that indicates if the autoAssignment is enabled.
     *
     * @return <code>true</code> if the autoAssignment is enabled, otherwise
     *         <code>false</code>
     */
    public boolean isAutoAssignmentEnabled() {
        return isAutoAssignmentEnabled;
    }

    /**
     * Sets the flag that indicates if the autoAssignment is enabled.
     *
     * @param isAutoAssignmentEnabled
     *            <code>true</code> if the autoAssignment is enabled, otherwise
     *            <code>false</code>
     */
    public void setAutoAssignmentEnabled(final boolean isAutoAssignmentEnabled) {
        this.isAutoAssignmentEnabled = isAutoAssignmentEnabled;
    }

    /**
     * Gets the autoAssign Distribution set IdNameVersion
     *
     * @return autoAssignDsIdNameVersion
     */
    public ProxyIdNameVersion getAutoAssignDsIdNameVersion() {
        return autoAssignDsIdNameVersion;
    }

    /**
     * Sets the securityToken
     *
     * @param autoAssignDsIdNameVersion
     *         Target filter autoAssign Distribution set IdNameVersion
     */
    public void setAutoAssignDsIdNameVersion(final ProxyIdNameVersion autoAssignDsIdNameVersion) {
        this.autoAssignDsIdNameVersion = autoAssignDsIdNameVersion;
    }

    @Override
    public void setDistributionSetId(final Long id) {
        if (autoAssignDsIdNameVersion != null) {
            autoAssignDsIdNameVersion.setId(id);
        } else {
            autoAssignDsIdNameVersion = new ProxyIdNameVersion(id, null, null);
        }
    }

    @Override
    public Long getDistributionSetId() {
        return autoAssignDsIdNameVersion != null ? autoAssignDsIdNameVersion.getId() : null;
    }
}
