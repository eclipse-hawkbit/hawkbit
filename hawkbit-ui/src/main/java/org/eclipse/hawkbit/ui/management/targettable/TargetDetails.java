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

import org.apache.commons.lang3.StringUtils;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.ui.common.detailslayout.AbstractTableDetailsLayout;
import org.eclipse.hawkbit.ui.common.tagdetails.TargetTagToken;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.management.event.TargetTableEvent;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPDateTimeUtil;
import org.eclipse.hawkbit.ui.utils.SPUIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Target details layout.
 */
@SpringComponent
@ViewScope
public class TargetDetails extends AbstractTableDetailsLayout<Target> {

    private static final long serialVersionUID = 4571732743399605843L;

    @Autowired
    private ManagementUIState managementUIState;

    @Autowired
    private TargetAddUpdateWindowLayout targetAddUpdateWindowLayout;

    @Autowired
    private TargetTagToken targetTagToken;

    private VerticalLayout assignedDistLayout;
    private VerticalLayout installedDistLayout;

    @Override
    public void init() {
        super.init();
        targetAddUpdateWindowLayout.init();
    }

    @Override
    protected String getDefaultCaption() {
        return getI18n().get("target.details.header");
    }

    @Override
    protected void addTabs(final TabSheet detailsTab) {
        detailsTab.addTab(createDetailsLayout(), getI18n().get("caption.tab.details"), null);
        detailsTab.addTab(createDescriptionLayout(), getI18n().get("caption.tab.description"), null);
        detailsTab.addTab(createAttributesLayout(), getI18n().get("caption.attributes.tab"), null);
        detailsTab.addTab(createAssignedDistLayout(), getI18n().get("header.target.assigned"), null);
        detailsTab.addTab(createInstalledDistLayout(), getI18n().get("header.target.installed"), null);
        detailsTab.addTab(createTagsLayout(), getI18n().get("caption.tags.tab"), null);
        detailsTab.addTab(createLogLayout(), getI18n().get("caption.logs.tab"), null);

    }

    private Component createInstalledDistLayout() {
        installedDistLayout = getTabLayout();
        return installedDistLayout;
    }

    private Component createAssignedDistLayout() {
        assignedDistLayout = getTabLayout();
        return assignedDistLayout;
    }

    private VerticalLayout createTagsLayout() {
        final VerticalLayout tagsLayout = getTabLayout();
        tagsLayout.addComponent(targetTagToken.getTokenField());
        return tagsLayout;
    }

    @Override
    protected void onEdit(final ClickEvent event) {
        if (getSelectedBaseEntity() == null) {
            return;
        }
        targetAddUpdateWindowLayout.populateValuesOfTarget(getSelectedBaseEntity().getControllerId());
        openWindow();
    }

    private void openWindow() {
        final Window newDistWindow = targetAddUpdateWindowLayout.getWindow();
        newDistWindow.setCaption(getI18n().get("caption.update.dist"));
        UI.getCurrent().addWindow(newDistWindow);
        newDistWindow.setVisible(Boolean.TRUE);
    }

    @Override
    protected String getEditButtonId() {
        return SPUIComponentIdProvider.TARGET_EDIT_ICON;
    }

    @Override
    protected Boolean onLoadIsTableRowSelected() {
        return managementUIState.getLastSelectedTargetIdName() != null;
    }

    @Override
    protected Boolean onLoadIsTableMaximized() {
        return managementUIState.isTargetTableMaximized();
    }

    @Override
    protected void populateDetailsWidget() {
        if (getSelectedBaseEntity() != null) {
            updateDetailsLayout(getSelectedBaseEntity().getControllerId(),
                    getSelectedBaseEntity().getTargetInfo().getAddress(), getSelectedBaseEntity().getSecurityToken(),
                    SPDateTimeUtil.getFormattedDate(getSelectedBaseEntity().getTargetInfo().getLastTargetQuery()));
            populateDistributionDtls(installedDistLayout,
                    getSelectedBaseEntity().getTargetInfo().getInstalledDistributionSet());
            populateDistributionDtls(assignedDistLayout, getSelectedBaseEntity().getAssignedDistributionSet());
        } else {
            updateDetailsLayout(null, null, null, null);
            populateDistributionDtls(installedDistLayout, null);
            populateDistributionDtls(assignedDistLayout, null);
        }
        updateAttributesLayout(getSelectedBaseEntity());
    }

    @Override
    protected String getName() {
        return getSelectedBaseEntity().getName();
    }

    private void updateDetailsLayout(final String controllerId, final URI address, final String securityToken,
            final String lastQueryDate) {
        final VerticalLayout detailsTabLayout = getDetailsLayout();
        detailsTabLayout.removeAllComponents();

        final Label controllerLabel = SPUIComponentProvider.createNameValueLabel(getI18n().get("label.target.id"),
                HawkbitCommonUtil.trimAndNullIfEmpty(controllerId) == null ? "" : controllerId);
        controllerLabel.setId(SPUIComponentIdProvider.TARGET_CONTROLLER_ID);
        detailsTabLayout.addComponent(controllerLabel);

        final Label lastPollDtLabel = SPUIComponentProvider.createNameValueLabel(
                getI18n().get("label.target.lastpolldate"),
                HawkbitCommonUtil.trimAndNullIfEmpty(lastQueryDate) == null ? "" : lastQueryDate);
        lastPollDtLabel.setId(SPUIComponentIdProvider.TARGET_LAST_QUERY_DT);
        detailsTabLayout.addComponent(lastPollDtLabel);

        final Label typeLabel = SPUIComponentProvider.createNameValueLabel(getI18n().get("label.ip"),
                address == null ? StringUtils.EMPTY : address.toString());
        typeLabel.setId(SPUIComponentIdProvider.TARGET_IP_ADDRESS);
        detailsTabLayout.addComponent(typeLabel);

        if (securityToken != null) {
            final HorizontalLayout securityTokenLayout = getSecurityTokenLayout(securityToken);
            controllerLabel.setId(SPUIComponentIdProvider.TARGET_SECURITY_TOKEN);
            detailsTabLayout.addComponent(securityTokenLayout);
        }
    }

    private HorizontalLayout getSecurityTokenLayout(final String securityToken) {
        final HorizontalLayout securityTokenLayout = new HorizontalLayout();

        final Label securityTableLbl = new Label(
                SPUIComponentProvider.getBoldHTMLText(getI18n().get("label.target.security.token")), ContentMode.HTML);
        securityTableLbl.addStyleName(SPUIDefinitions.TEXT_STYLE);
        securityTableLbl.addStyleName("label-style");

        final TextField securityTokentxt = new TextField();
        securityTokentxt.addStyleName(ValoTheme.TEXTFIELD_BORDERLESS);
        securityTokentxt.addStyleName(ValoTheme.TEXTFIELD_TINY);
        securityTokentxt.addStyleName("targetDtls-securityToken");
        securityTokentxt.addStyleName(SPUIDefinitions.TEXT_STYLE);
        securityTokentxt.setCaption(null);
        securityTokentxt.setValue(securityToken);
        securityTokentxt.setReadOnly(true);

        securityTokenLayout.addComponent(securityTableLbl);
        securityTokenLayout.addComponent(securityTokentxt);
        return securityTokenLayout;
    }

    private void populateDistributionDtls(final VerticalLayout layout, final DistributionSet distributionSet) {
        layout.removeAllComponents();
        if (distributionSet == null) {
            return;
        }
        layout.addComponent(SPUIComponentProvider.createNameValueLabel(getI18n().get("label.dist.details.name"),
                distributionSet.getName()));

        layout.addComponent(SPUIComponentProvider.createNameValueLabel(getI18n().get("label.dist.details.version"),
                distributionSet.getVersion()));

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
    private Label getSWModlabel(final String labelName, final SoftwareModule swModule) {
        return SPUIComponentProvider.createNameValueLabel(labelName + " : ", swModule.getName(), swModule.getVersion());
    }

    @Override
    protected Boolean hasEditPermission() {
        return getPermissionChecker().hasUpdateTargetPermission();
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final TargetTableEvent targetTableEvent) {
        onBaseEntityEvent(targetTableEvent);
    }

    @Override
    protected String getTabSheetId() {
        return SPUIComponentIdProvider.TARGET_DETAILS_TABSHEET;
    }

    @Override
    protected String getDetailsHeaderCaptionId() {
        return SPUIComponentIdProvider.TARGET_DETAILS_HEADER_LABEL_ID;
    }

}
