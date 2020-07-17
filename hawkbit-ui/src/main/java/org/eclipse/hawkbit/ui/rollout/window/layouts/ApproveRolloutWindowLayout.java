/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout.window.layouts;

import org.eclipse.hawkbit.ui.common.data.proxies.ProxyRolloutWindow;
import org.eclipse.hawkbit.ui.rollout.window.RolloutWindowDependencies;
import org.eclipse.hawkbit.ui.rollout.window.components.ApprovalLayout;

import com.vaadin.data.ValidationException;
import com.vaadin.ui.GridLayout;

/**
 * Layout builder for Approve Rollout window.
 */
@SuppressWarnings({ "squid:MaximumInheritanceDepth", "squid:S2160" })
public class ApproveRolloutWindowLayout extends UpdateRolloutWindowLayout {

    private final ApprovalLayout approvalLayout;

    /**
     * Constructor for ApproveRolloutWindowLayout
     *
     * @param dependencies
     *          RolloutWindowDependencies
     */
    public ApproveRolloutWindowLayout(final RolloutWindowDependencies dependencies) {
        super(dependencies);

        this.approvalLayout = rolloutComponentBuilder.createApprovalLayout();

        addValidatableLayout(approvalLayout);
    }

    @Override
    protected void addComponents(final GridLayout rootLayout) {
        super.addComponents(rootLayout);

        rootLayout.insertRow(rootLayout.getRows());

        final int lastRowIdx = rootLayout.getRows() - 1;
        final int lastColumnIdx = rootLayout.getColumns() - 1;

        approvalLayout.addApprovalToLayout(rootLayout, lastColumnIdx, lastRowIdx);
    }

    @Override
    public void setEntity(final ProxyRolloutWindow proxyEntity) {
        super.setEntity(proxyEntity);

        approvalLayout.setBean(proxyEntity.getRolloutApproval());
    }

    @Override
    public ProxyRolloutWindow getValidatableEntity() throws ValidationException {
        final ProxyRolloutWindow proxyEntity = super.getValidatableEntity();
        proxyEntity.setRolloutApproval(approvalLayout.getBean());

        return proxyEntity;
    }

    @Override
    public void resetValidation() {
        approvalLayout.resetValidationStatus();
    }

    /**
     * Disable the rollout form layout
     */
    public void disableRolloutFormLayout() {
        rolloutFormLayout.disableAllFields();
    }
}
