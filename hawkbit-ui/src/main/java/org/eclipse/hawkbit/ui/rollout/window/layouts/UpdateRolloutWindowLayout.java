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
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyRolloutWindow.GroupDefinitionMode;
import org.eclipse.hawkbit.ui.rollout.window.RolloutWindowDependencies;
import org.eclipse.hawkbit.ui.rollout.window.components.RolloutFormLayout;
import org.eclipse.hawkbit.ui.rollout.window.components.VisualGroupDefinitionLayout;

import com.vaadin.data.ValidationException;
import com.vaadin.ui.GridLayout;

/**
 * Layout builder for Update Rollout window.
 */
@SuppressWarnings({ "squid:MaximumInheritanceDepth", "squid:S2160" })
public class UpdateRolloutWindowLayout extends AbstractRolloutWindowLayout {
    protected final RolloutFormLayout rolloutFormLayout;
    private final VisualGroupDefinitionLayout visualGroupDefinitionLayout;

    /**
     * Constructor for UpdateRolloutWindowLayout
     *
     * @param dependencies
     *          RolloutWindowDependencies
     */
    public UpdateRolloutWindowLayout(final RolloutWindowDependencies dependencies) {
        super(dependencies);

        this.rolloutFormLayout = rolloutComponentBuilder.createRolloutFormLayout();
        this.visualGroupDefinitionLayout = rolloutComponentBuilder.createVisualGroupDefinitionLayout();

        addValidatableLayout(rolloutFormLayout);
    }

    @Override
    protected void addComponents(final GridLayout rootLayout) {
        rootLayout.setRows(6);

        final int lastColumnIdx = rootLayout.getColumns() - 1;

        rolloutFormLayout.addFormToEditLayout(rootLayout);
        visualGroupDefinitionLayout.addChartWithLegendToLayout(rootLayout, lastColumnIdx, 3);
    }

    @Override
    public void setEntity(final ProxyRolloutWindow proxyEntity) {
        rolloutFormLayout.setBean(proxyEntity.getRolloutForm());
        visualGroupDefinitionLayout.setGroupDefinitionMode(GroupDefinitionMode.ADVANCED);
        visualGroupDefinitionLayout
                .setAdvancedRolloutGroupDefinitions(proxyEntity.getAdvancedRolloutGroupDefinitions());
    }

    @Override
    public ProxyRolloutWindow getValidatableEntity() throws ValidationException {
        final ProxyRolloutWindow proxyEntity = new ProxyRolloutWindow();
        proxyEntity.setRolloutForm(rolloutFormLayout.getBean());

        return proxyEntity;
    }

    /**
     * Sets the count of total targets
     *
     * @param totalTargets
     *          Total targets
     */
    public void setTotalTargets(final Long totalTargets) {
        visualGroupDefinitionLayout.setTotalTargets(totalTargets);
    }

    /**
     * Reset rollout form layout validation
     */
    public void resetValidation() {
        rolloutFormLayout.resetValidationStatus();
    }

    /**
     *  Adapt rollout form layout when status is pending
     */
    public void adaptForPendingStatus() {
        rolloutFormLayout.disableFieldsOnEditForInActive();
    }

    /**
     *  Adapt rollout form layout when status is started
     */
    public void adaptForStartedStatus() {
        rolloutFormLayout.disableFieldsOnEditForActive();
    }
}
