/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.filtermanagement;

import java.io.Serializable;

import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.ui.common.CommonDialogWindow;
import org.eclipse.hawkbit.ui.common.DistributionSetIdName;
import org.eclipse.hawkbit.ui.common.builder.WindowBuilder;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleNoBorderWithIcon;
import org.eclipse.hawkbit.ui.distributions.state.ManageDistUIState;
import org.eclipse.hawkbit.ui.filtermanagement.event.CustomFilterUIEvent;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.data.Property;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Sizeable;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

/**
 * Creates a dialog window to select the distribution set for a target filter
 * query.
 */
public class DistributionSetSelectWindow
        implements CommonDialogWindow.SaveDialogCloseListener, Property.ValueChangeListener {

    private static final long serialVersionUID = 4752345414134989396L;

    private final I18N i18n;

    private final DistributionSetSelectTable dsTable;

    private final EventBus.UIEventBus eventBus;

    private final TargetManagement targetManagement;

    private final TargetFilterQueryManagement targetFilterQueryManagement;

    private CommonDialogWindow window;
    private CheckBox checkBox;
    private Long tfqId;

    public DistributionSetSelectWindow(final I18N i18n, final UIEventBus eventBus,
            final TargetManagement targetManagement, final TargetFilterQueryManagement targetFilterQueryManagement,
            final ManageDistUIState manageDistUIState) {
        this.i18n = i18n;
        this.dsTable = new DistributionSetSelectTable(i18n, eventBus, manageDistUIState);
        this.eventBus = eventBus;
        this.targetManagement = targetManagement;
        this.targetFilterQueryManagement = targetFilterQueryManagement;
    }

    private void initLocal() {
        final Label label = new Label(i18n.get("label.auto.assign.description"));

        checkBox = new CheckBox(i18n.get("label.auto.assign.enable"));
        checkBox.setId(UIComponentIdProvider.DIST_SET_SELECT_ENABLE_ID);
        checkBox.setImmediate(true);
        checkBox.addValueChangeListener(this);

        final VerticalLayout verticalLayout = new VerticalLayout();
        verticalLayout.addComponent(label);
        verticalLayout.addComponent(checkBox);
        verticalLayout.addComponent(dsTable);

        window = new WindowBuilder(SPUIDefinitions.CREATE_UPDATE_WINDOW)
                .caption(i18n.get("caption.select.auto.assign.dist")).content(verticalLayout).layout(verticalLayout)
                .i18n(i18n).saveDialogCloseListener(this).buildCommonDialogWindow();
        window.setId(UIComponentIdProvider.DIST_SET_SELECT_WINDOW_ID);
    }

    public void setValue(final DistributionSetIdName distSet) {
        dsTable.setVisible(distSet != null);
        checkBox.setValue(distSet != null);
        dsTable.setValue(distSet);
        dsTable.setCurrentPageFirstItemId(distSet);
    }

    public DistributionSetIdName getValue() {
        if (checkBox.getValue()) {
            return (DistributionSetIdName) dsTable.getValue();
        }
        return null;
    }

    /**
     * Shows a distribution set select window for the given target filter query
     * 
     * @param tfqId
     *            target filter query id
     */
    public void showForTargetFilter(final Long tfqId) {
        this.tfqId = tfqId;
        final TargetFilterQuery tfq = targetFilterQueryManagement.findTargetFilterQueryById(tfqId);
        if (tfq == null) {
            throw new IllegalStateException("TargetFilterQuery does not exist for the given id");
        }

        initLocal();

        final DistributionSet distributionSet = tfq.getAutoAssignDistributionSet();
        if (distributionSet != null) {
            setValue(DistributionSetIdName.generate(distributionSet));
        } else {
            setValue(null);
        }

        window.setWidth(40.0F, Sizeable.Unit.PERCENTAGE);
        UI.getCurrent().addWindow(window);
        window.setVisible(true);
    }

    /**
     * Is triggered when the checkbox value changes
     * 
     * @param event
     *            change event
     */
    @Override
    public void valueChange(final Property.ValueChangeEvent event) {
        dsTable.setVisible(checkBox.getValue());
        if (window != null) {
            window.center();

        }
    }

    /**
     * Is triggered when the save button is clicked
     * 
     * @return whether the click should be allowed
     */
    @Override
    public boolean canWindowSaveOrUpdate() {
        return !checkBox.getValue() || dsTable.getValue() != null;
    }

    /**
     * Is called when the new value should be saved after the save button has
     * been clicked
     */
    @Override
    public void saveOrUpdate() {
        if (checkBox.getValue() && dsTable.getValue() != null) {
            final DistributionSetIdName ds = (DistributionSetIdName) dsTable.getValue();
            updateTargetFilterQueryDS(tfqId, ds.getId());

        } else if (!checkBox.getValue()) {
            updateTargetFilterQueryDS(tfqId, null);

        }

    }

    private void updateTargetFilterQueryDS(final Long targetFilterQueryId, final Long dsId) {
        final TargetFilterQuery tfq = targetFilterQueryManagement.findTargetFilterQueryById(targetFilterQueryId);

        if (dsId != null) {
            confirmWithConsequencesDialog(tfq, dsId);
        } else {
            targetFilterQueryManagement.updateTargetFilterQueryAutoAssignDS(targetFilterQueryId, null);
            eventBus.publish(this, CustomFilterUIEvent.UPDATED_TARGET_FILTER_QUERY);
        }

    }

    private void confirmWithConsequencesDialog(final TargetFilterQuery tfq, final Long dsId) {

        final ConfirmConsequencesDialog dialog = new ConfirmConsequencesDialog(tfq, dsId, new ConfirmCallback() {
            @Override
            public void onConfirmResult(final boolean accepted) {
                if (accepted) {
                    targetFilterQueryManagement.updateTargetFilterQueryAutoAssignDS(tfq.getId(), dsId);
                    eventBus.publish(this, CustomFilterUIEvent.UPDATED_TARGET_FILTER_QUERY);
                }
            }
        });

        dialog.setWidth(40.0F, Sizeable.Unit.PERCENTAGE);

        UI.getCurrent().addWindow(dialog);
        dialog.setVisible(true);

    }

    /**
     * A dialog that displays how many targets will be assigned immediately with
     * the
     */
    private class ConfirmConsequencesDialog extends Window implements Button.ClickListener {

        private static final long serialVersionUID = 7738545414137389326L;

        private final TargetFilterQuery targetFilterQuery;
        private final Long distributionSetId;

        private Button okButton;

        private final ConfirmCallback callback;

        public ConfirmConsequencesDialog(final TargetFilterQuery targetFilterQuery, final Long dsId,
                final ConfirmCallback callback) {
            super(i18n.get("caption.confirm.assign.consequences"));

            this.callback = callback;
            this.targetFilterQuery = targetFilterQuery;
            this.distributionSetId = dsId;

            init();

        }

        private void init() {
            setId(UIComponentIdProvider.DIST_SET_SELECT_CONS_WINDOW_ID);
            setModal(true);
            setResizable(false);

            final VerticalLayout layout = new VerticalLayout();
            layout.setSpacing(true);
            layout.setMargin(true);
            setContent(layout);

            final Long targetsCount = targetManagement.countTargetsByTargetFilterQueryAndNonDS(distributionSetId,
                    targetFilterQuery);
            Label mainTextLabel;
            if (targetsCount == 0) {
                mainTextLabel = new Label(i18n.get("message.confirm.assign.consequences.none"));
            } else {
                mainTextLabel = new Label(
                        i18n.get("message.confirm.assign.consequences.text", new Object[] { targetsCount }));
            }

            layout.addComponent(mainTextLabel);

            final HorizontalLayout buttonsLayout = new HorizontalLayout();
            buttonsLayout.setSizeFull();
            buttonsLayout.setSpacing(true);
            buttonsLayout.addStyleName("actionButtonsMargin");
            layout.addComponent(buttonsLayout);

            okButton = SPUIComponentProvider.getButton(UIComponentIdProvider.SAVE_BUTTON, i18n.get("button.ok"), "", "",
                    true, FontAwesome.SAVE, SPUIButtonStyleNoBorderWithIcon.class);
            okButton.setSizeUndefined();
            okButton.addStyleName("default-color");
            okButton.addClickListener(this);
            buttonsLayout.addComponent(okButton);
            buttonsLayout.setComponentAlignment(okButton, Alignment.MIDDLE_RIGHT);
            buttonsLayout.setExpandRatio(okButton, 1.0F);

            final Button cancelButton = SPUIComponentProvider.getButton(UIComponentIdProvider.CANCEL_BUTTON,
                    i18n.get("button.cancel"), "", "", true, FontAwesome.TIMES, SPUIButtonStyleNoBorderWithIcon.class);
            cancelButton.setSizeUndefined();
            cancelButton.addStyleName("default-color");
            cancelButton.addClickListener(this);
            buttonsLayout.addComponent(cancelButton);
            buttonsLayout.setComponentAlignment(cancelButton, Alignment.MIDDLE_LEFT);
            buttonsLayout.setExpandRatio(cancelButton, 1.0F);

        }

        @Override
        public void buttonClick(final Button.ClickEvent event) {
            if (event.getButton().getId().equals(okButton.getId())) {
                callback.onConfirmResult(true);
            } else {
                callback.onConfirmResult(false);
            }

            close();

        }
    }

    @FunctionalInterface
    private interface ConfirmCallback extends Serializable {
        void onConfirmResult(boolean accepted);
    }
}
