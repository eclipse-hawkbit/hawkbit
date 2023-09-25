/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.common.data.proxies;

import org.eclipse.hawkbit.repository.model.Action;
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

    private boolean isConfirmationRequired;

    private ProxyDistributionSetInfo autoAssignDsInfo;

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
     *            Target filter query id
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
     *            Target filter query
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
     *            ActionType
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
     * Gets the autoAssign Distribution set Info
     *
     * @return ProxyDistributionSetInfo
     */
    @Override
    public ProxyDistributionSetInfo getDistributionSetInfo() {
        return autoAssignDsInfo;
    }

    /**
     * Sets the autoAssign Distribution set Info
     *
     * @param dsInfo
     *            Target filter autoAssign Distribution set Info
     */
    @Override
    public void setDistributionSetInfo(final ProxyDistributionSetInfo dsInfo) {
        this.autoAssignDsInfo = dsInfo;
    }

    /**
     * @return if confirmation is required for configured auto assignment
     *         (considered with confirmation flow active)
     */
    public boolean isConfirmationRequired() {
        return isConfirmationRequired;
    }

    /**
     * Specify initial confirmation state of resulting {@link Action} in auto
     * assignment
     *
     * @param confirmationRequired
     *            if confirmation is required for configured auto assignment
     *            (considered with confirmation flow active)
     */
    public void setConfirmationRequired(final boolean confirmationRequired) {
        isConfirmationRequired = confirmationRequired;
    }

    /**
     * Sets the Id, name and query of target filter query
     *
     * @param tfqInfo
     *            ProxyTargetFilterQuery
     *
     * @return proxy of target filter query
     */
    public static ProxyTargetFilterQuery of(final ProxyTargetFilterQueryInfo tfqInfo) {
        final ProxyTargetFilterQuery tfq = new ProxyTargetFilterQuery();

        tfq.setId(tfqInfo.getId());
        tfq.setName(tfqInfo.getName());
        tfq.setQuery(tfqInfo.getQuery());

        return tfq;
    }

    /**
     * Gets the Id, name and query of target filter query
     *
     * @return proxy of Id, Name and query
     */
    public ProxyTargetFilterQueryInfo getInfo() {
        return new ProxyTargetFilterQueryInfo(getId(), getName(), getQuery());
    }

}
