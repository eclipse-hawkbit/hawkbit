/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettable;

import java.net.URI;

import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TargetTagManagement;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.detailslayout.AbstractTableDetailsLayout;
import org.eclipse.hawkbit.ui.common.tagdetails.TargetTagToken;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.management.event.TargetTableEvent;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.utils.SPDateTimeUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Target details layout which is shown on the Deployment View.
 */
public class TargetDetails extends AbstractTableDetailsLayout<Target> {

    private static final long serialVersionUID = 1L;

    private final TargetTagToken targetTagToken;

    private final TargetAddUpdateWindowLayout targetAddUpdateWindowLayout;

    private final transient TargetManagement targetManagement;

    private final transient DeploymentManagement deploymentManagement;

    private VerticalLayout assignedDistLayout;

    private VerticalLayout installedDistLayout;

    TargetDetails(final VaadinMessageSource i18n, final UIEventBus eventBus,
            final SpPermissionChecker permissionChecker, final ManagementUIState managementUIState,
            final UINotification uiNotification, final TargetTagManagement tagManagement,
            final TargetManagement targetManagement, final DeploymentManagement deploymentManagement,
            final EntityFactory entityFactory, final TargetTable targetTable) {
        super(i18n, eventBus, permissionChecker, managementUIState);
        this.targetTagToken = new TargetTagToken(permissionChecker, i18n, uiNotification, eventBus, managementUIState,
                tagManagement, targetManagement);
        targetAddUpdateWindowLayout = new TargetAddUpdateWindowLayout(i18n, targetManagement, eventBus, uiNotification,
                entityFactory, targetTable);
        this.targetManagement = targetManagement;
        this.deploymentManagement = deploymentManagement;
        addDetailsTab();
        restoreState();
    }

    @Override
    protected String getDefaultCaption() {
        return getI18n().getMessage("target.details.header");
    }

    private final void addDetailsTab() {
        getDetailsTab().addTab(getDetailsLayout(), getI18n().getMessage("caption.tab.details"), null);
        getDetailsTab().addTab(getDescriptionLayout(), getI18n().getMessage("caption.tab.description"), null);
        getDetailsTab().addTab(getAttributesLayout(), getI18n().getMessage("caption.attributes.tab"), null);
        getDetailsTab().addTab(createAssignedDistLayout(), getI18n().getMessage("header.target.assigned"), null);
        getDetailsTab().addTab(createInstalledDistLayout(), getI18n().getMessage("header.target.installed"), null);
        getDetailsTab().addTab(getTagsLayout(), getI18n().getMessage("caption.tags.tab"), null);
        getDetailsTab().addTab(getLogLayout(), getI18n().getMessage("caption.logs.tab"), null);
    }

    private Component createInstalledDistLayout() {
        installedDistLayout = createTabLayout();
        return installedDistLayout;
    }

    private Component createAssignedDistLayout() {
        assignedDistLayout = createTabLayout();
        return assignedDistLayout;
    }

    @Override
    protected void onEdit(final ClickEvent event) {
        if (getSelectedBaseEntity() == null) {
            return;
        }
        openWindow();
    }

    private void openWindow() {
        final Window targetWindow = targetAddUpdateWindowLayout.getWindow(getSelectedBaseEntity().getControllerId());
        if (targetWindow == null) {
            return;
        }
        targetWindow.setCaption(getI18n().getMessage(UIComponentIdProvider.TARGET_UPDATE_CAPTION));
        UI.getCurrent().addWindow(targetWindow);
        targetWindow.setVisible(Boolean.TRUE);
    }

    @Override
    protected String getEditButtonId() {
        return UIComponentIdProvider.TARGET_EDIT_ICON;
    }

    @Override
    protected boolean onLoadIsTableMaximized() {
        return getManagementUIState().isTargetTableMaximized();
    }

    @Override
    protected void populateDetailsWidget() {
        if (getSelectedBaseEntity() != null) {
            updateAttributesLayout(targetManagement.getControllerAttributes(getSelectedBaseEntity().getControllerId()));

            updateDetailsLayout(getSelectedBaseEntity().getControllerId(), getSelectedBaseEntity().getAddress(),
                    getSelectedBaseEntity().getSecurityToken(),
                    SPDateTimeUtil.getFormattedDate(getSelectedBaseEntity().getLastTargetQuery()));

            populateDistributionDtls(assignedDistLayout, deploymentManagement
                    .getAssignedDistributionSet(getSelectedBaseEntity().getControllerId()).orElse(null));
            populateDistributionDtls(installedDistLayout, deploymentManagement
                    .getInstalledDistributionSet(getSelectedBaseEntity().getControllerId()).orElse(null));
        } else {
            updateDetailsLayout(null, null, null, null);
            populateDistributionDtls(installedDistLayout, null);
            populateDistributionDtls(assignedDistLayout, null);
        }
        populateTags(targetTagToken);
    }

    @Override
    protected String getName() {
        return getSelectedBaseEntity().getName();
    }

    private void updateDetailsLayout(final String controllerId, final URI address, final String securityToken,
            final String lastQueryDate) {
        final VerticalLayout detailsTabLayout = getDetailsLayout();
        detailsTabLayout.removeAllComponents();

        final Label controllerLabel = SPUIComponentProvider.createNameValueLabel(
                getI18n().getMessage("label.target.id"), controllerId == null ? "" : controllerId);
        controllerLabel.setId(UIComponentIdProvider.TARGET_CONTROLLER_ID);
        detailsTabLayout.addComponent(controllerLabel);

        final Label lastPollDtLabel = SPUIComponentProvider.createNameValueLabel(
                getI18n().getMessage("label.target.lastpolldate"), lastQueryDate == null ? "" : lastQueryDate);
        lastPollDtLabel.setId(UIComponentIdProvider.TARGET_LAST_QUERY_DT);
        detailsTabLayout.addComponent(lastPollDtLabel);

        final Label typeLabel = SPUIComponentProvider.createNameValueLabel(getI18n().getMessage("label.ip"),
                address == null ? "" : address.toString());
        typeLabel.setId(UIComponentIdProvider.TARGET_IP_ADDRESS);
        detailsTabLayout.addComponent(typeLabel);

        final HorizontalLayout securityTokenLayout = getSecurityTokenLayout(securityToken);
        controllerLabel.setId(UIComponentIdProvider.TARGET_SECURITY_TOKEN);
        detailsTabLayout.addComponent(securityTokenLayout);
    }

    private HorizontalLayout getSecurityTokenLayout(final String securityToken) {
        final HorizontalLayout securityTokenLayout = new HorizontalLayout();

        final Label securityTableLbl = new Label(
                SPUIComponentProvider.getBoldHTMLText(getI18n().getMessage("label.target.security.token")),
                ContentMode.HTML);
        securityTableLbl.addStyleName(SPUIDefinitions.TEXT_STYLE);
        securityTableLbl.addStyleName("label-style");

        final TextField securityTokentxt = new TextField();
        securityTokentxt.addStyleName(ValoTheme.TEXTFIELD_BORDERLESS);
        securityTokentxt.addStyleName(ValoTheme.TEXTFIELD_TINY);
        securityTokentxt.addStyleName("targetDtls-securityToken");
        securityTokentxt.addStyleName(SPUIDefinitions.TEXT_STYLE);
        securityTokentxt.setCaption(null);
        securityTokentxt.setNullRepresentation("");
        securityTokentxt.setValue(securityToken);
        securityTokentxt.setReadOnly(true);

        securityTokenLayout.addComponent(securityTableLbl);
        securityTokenLayout.addComponent(securityTokentxt);
        return securityTokenLayout;
    }

    private void populateDistributionDtls(final VerticalLayout layout, final DistributionSet distributionSet) {
        layout.removeAllComponents();
        layout.addComponent(SPUIComponentProvider.createNameValueLabel(getI18n().getMessage("label.dist.details.name"),
                distributionSet == null ? "" : distributionSet.getName()));

        layout.addComponent(
                SPUIComponentProvider.createNameValueLabel(getI18n().getMessage("label.dist.details.version"),
                        distributionSet == null ? "" : distributionSet.getVersion()));

        if (distributionSet == null) {
            return;
        }
        distributionSet.getModules()
                .forEach(module -> layout.addComponent(getSWModlabel(module.getType().getName(), module)));
    }

    /**
     * Create Label for SW Module.
     * 
     * @param labelName
     *            as Name
     * @param swModule
     *            as Module (JVM|OS|AH)
     * @return Label as UI
     */
    private static Label getSWModlabel(final String labelName, final SoftwareModule swModule) {
        return SPUIComponentProvider.createNameValueLabel(labelName + " : ", swModule.getName(), swModule.getVersion());
    }

    @Override
    protected boolean hasEditPermission() {
        return getPermissionChecker().hasUpdateTargetPermission();
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final TargetTableEvent targetTableEvent) {
        onBaseEntityEvent(targetTableEvent);
    }

    @Override
    protected String getTabSheetId() {
        return UIComponentIdProvider.TARGET_DETAILS_TABSHEET;
    }

    @Override
    protected String getDetailsHeaderCaptionId() {
        return UIComponentIdProvider.TARGET_DETAILS_HEADER_LABEL_ID;
    }

    @Override
    protected boolean isMetadataIconToBeDisplayed() {
        return false;
    }

    @Override
    protected void showMetadata(final ClickEvent event) {
        // No implementation required
    }

    @Override
    protected void populateMetadataDetails() {
        // No implementation required
    }

}
