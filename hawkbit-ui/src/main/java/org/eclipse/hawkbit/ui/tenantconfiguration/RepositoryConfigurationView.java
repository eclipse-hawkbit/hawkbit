/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.tenantconfiguration;

import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.TenantConfigurationValue;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;
import org.eclipse.hawkbit.ui.UiProperties;
import org.eclipse.hawkbit.ui.common.builder.FormComponentBuilder;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxySystemConfigRepository;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.tenantconfiguration.repository.ActionAutoCleanupConfigurationItem;
import org.eclipse.hawkbit.ui.tenantconfiguration.repository.ActionAutoCloseConfigurationItem;
import org.eclipse.hawkbit.ui.tenantconfiguration.repository.MultiAssignmentsConfigurationItem;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.ACTION_CLEANUP_ACTION_EXPIRY;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.ACTION_CLEANUP_ACTION_STATUS;

/**
 * View to configure the authentication mode.
 */
public class RepositoryConfigurationView extends BaseConfigurationView<ProxySystemConfigRepository> {

    private static final String DIST_CHECKBOX_STYLE = "dist-checkbox-style";
    private static final Set<Action.Status> EMPTY_STATUS_SET = EnumSet.noneOf(Action.Status.class);

    private static final long serialVersionUID = 1L;

    private final VaadinMessageSource i18n;
    private final UiProperties uiProperties;

    private ActionAutoCloseConfigurationItem actionAutocloseConfigurationItem;
    private ActionAutoCleanupConfigurationItem actionAutocleanupConfigurationItem;
    private MultiAssignmentsConfigurationItem multiAssignmentsConfigurationItem;

    private CheckBox multiAssignmentsCheckBox;

    RepositoryConfigurationView(final VaadinMessageSource i18n, final UiProperties uiProperties,
            final TenantConfigurationManagement tenantConfigurationManagement) {
        super(tenantConfigurationManagement);
        this.i18n = i18n;
        this.uiProperties = uiProperties;
    }

    @Override
    public void afterPropertiesSet() {
        super.afterPropertiesSet();
        this.actionAutocloseConfigurationItem = new ActionAutoCloseConfigurationItem(i18n);
        this.actionAutocleanupConfigurationItem = new ActionAutoCleanupConfigurationItem(getBinder(), i18n);
        this.multiAssignmentsConfigurationItem = new MultiAssignmentsConfigurationItem(i18n, getBinder());
        init();
    }

    private void init() {

        final Panel rootPanel = new Panel();
        rootPanel.setSizeFull();

        rootPanel.addStyleName("config-panel");

        final VerticalLayout vLayout = new VerticalLayout();
        vLayout.setSpacing(false);
        vLayout.setMargin(true);
        vLayout.setSizeFull();

        final Label header = new Label(i18n.getMessage("configuration.repository.title"));
        header.addStyleName("config-panel-header");
        vLayout.addComponent(header);

        final GridLayout gridLayout = new GridLayout(3, 3);
        gridLayout.setSpacing(true);

        gridLayout.setColumnExpandRatio(1, 1.0F);
        gridLayout.setSizeFull();

        final CheckBox actionAutoCloseCheckBox = FormComponentBuilder.getCheckBox(
                UIComponentIdProvider.REPOSITORY_ACTIONS_AUTOCLOSE_CHECKBOX, getBinder(),
                ProxySystemConfigRepository::isActionAutoclose, ProxySystemConfigRepository::setActionAutoclose);
        actionAutoCloseCheckBox.setStyleName(DIST_CHECKBOX_STYLE);
        actionAutoCloseCheckBox.setEnabled(!getBinderBean().isMultiAssignments());
        actionAutocloseConfigurationItem.setEnabled(!getBinderBean().isMultiAssignments());
        gridLayout.addComponent(actionAutoCloseCheckBox, 0, 0);
        gridLayout.addComponent(actionAutocloseConfigurationItem, 1, 0);

        multiAssignmentsCheckBox = FormComponentBuilder.getCheckBox(
                UIComponentIdProvider.REPOSITORY_MULTI_ASSIGNMENTS_CHECKBOX, getBinder(),
                ProxySystemConfigRepository::isMultiAssignments, ProxySystemConfigRepository::setMultiAssignments);
        multiAssignmentsCheckBox.setStyleName(DIST_CHECKBOX_STYLE);
        multiAssignmentsCheckBox.setEnabled(!getBinderBean().isMultiAssignments());
        multiAssignmentsConfigurationItem.setEnabled(!getBinderBean().isMultiAssignments());
        multiAssignmentsCheckBox.addValueChangeListener(event -> {
            actionAutoCloseCheckBox.setEnabled(!event.getValue());
            actionAutocloseConfigurationItem.setEnabled(!event.getValue());
            if (event.getValue()) {
                multiAssignmentsConfigurationItem.showSettings();
            } else {
                multiAssignmentsConfigurationItem.hideSettings();
            }
        });
        gridLayout.addComponent(multiAssignmentsCheckBox, 0, 1);
        gridLayout.addComponent(multiAssignmentsConfigurationItem, 1, 1);

        final CheckBox actionAutoCleanupCheckBox = FormComponentBuilder.getCheckBox(
                UIComponentIdProvider.REPOSITORY_ACTIONS_AUTOCLEANUP_CHECKBOX, getBinder(),
                ProxySystemConfigRepository::isActionAutocleanup, ProxySystemConfigRepository::setActionAutocleanup);
        actionAutoCleanupCheckBox.setStyleName(DIST_CHECKBOX_STYLE);
        actionAutoCleanupCheckBox.addValueChangeListener(event -> {
            if (event.getValue()) {
                actionAutocleanupConfigurationItem.showSettings();
            } else {
                actionAutocleanupConfigurationItem.hideSettings();
            }
        });
        gridLayout.addComponent(actionAutoCleanupCheckBox, 0, 2);
        gridLayout.addComponent(actionAutocleanupConfigurationItem, 1, 2);

        final Link linkToProvisioningHelp = SPUIComponentProvider.getHelpLink(i18n,
                uiProperties.getLinks().getDocumentation().getProvisioningStateMachine());
        gridLayout.addComponent(linkToProvisioningHelp, 2, 2);
        gridLayout.setComponentAlignment(linkToProvisioningHelp, Alignment.BOTTOM_RIGHT);

        vLayout.addComponent(gridLayout);
        rootPanel.setContent(vLayout);
        setCompositionRoot(rootPanel);
    }

    /**
     * Disable multiple assignment option
     */
    public void disableMultipleAssignmentOption() {
        multiAssignmentsCheckBox.setEnabled(false);
        multiAssignmentsConfigurationItem.setEnabled(false);
    }

    @Override
    public void save() {
        writeConfigOption(TenantConfigurationKey.ACTION_CLEANUP_ENABLED, getBinderBean().isActionAutocleanup());
        if (getBinderBean().isActionAutocleanup()) {
            writeConfigOption(ACTION_CLEANUP_ACTION_STATUS, getBinderBean().getActionCleanupStatus().getStatus()
                    .stream().map(Action.Status::name).collect(Collectors.joining(",")));

            writeConfigOption(TenantConfigurationKey.ACTION_CLEANUP_ACTION_EXPIRY,
                    TimeUnit.DAYS.toMillis(Long.parseLong(getBinderBean().getActionExpiryDays())));
        }
        if (!readConfigOption(TenantConfigurationKey.MULTI_ASSIGNMENTS_ENABLED)) {
            writeConfigOption(TenantConfigurationKey.REPOSITORY_ACTIONS_AUTOCLOSE_ENABLED,
                    getBinderBean().isActionAutoclose());
        }
        if (getBinderBean().isMultiAssignments()
                && !readConfigOption(TenantConfigurationKey.MULTI_ASSIGNMENTS_ENABLED)) {
            writeConfigOption(TenantConfigurationKey.MULTI_ASSIGNMENTS_ENABLED, getBinderBean().isMultiAssignments());
            this.disableMultipleAssignmentOption();
        }
    }

    @Override
    protected ProxySystemConfigRepository populateSystemConfig() {
        final ProxySystemConfigRepository configBean = new ProxySystemConfigRepository();
        configBean.setActionAutoclose(readConfigOption(TenantConfigurationKey.REPOSITORY_ACTIONS_AUTOCLOSE_ENABLED));
        configBean.setMultiAssignments(readConfigOption(TenantConfigurationKey.MULTI_ASSIGNMENTS_ENABLED));
        configBean.setActionAutocleanup(readConfigOption(TenantConfigurationKey.ACTION_CLEANUP_ENABLED));
        configBean.setActionCleanupStatus(getActionStatusOption());
        configBean.setActionExpiryDays(String.valueOf(getActionExpiry()));
        return configBean;
    }

    private long getActionExpiry() {
        return TimeUnit.MILLISECONDS.toDays(readConfigValue(ACTION_CLEANUP_ACTION_EXPIRY, Long.class).getValue());
    }

    private ActionAutoCleanupConfigurationItem.ActionStatusOption getActionStatusOption() {
        final Set<Action.Status> actionStatus = getActionStatus();
        final Collection<ActionAutoCleanupConfigurationItem.ActionStatusOption> actionStatusOptions = ActionAutoCleanupConfigurationItem
                .getActionStatusOptions();

        return actionStatusOptions.stream().filter(option -> actionStatus.equals(option.getStatus())).findFirst()
                .orElse(actionStatusOptions.iterator().next());
    }

    private <T extends Serializable> TenantConfigurationValue<T> readConfigValue(final String key,
            final Class<T> valueType) {
        return getTenantConfigurationManagement().getConfigurationValue(key, valueType);
    }

    private Set<Action.Status> getActionStatus() {
        final TenantConfigurationValue<String> statusStr = readConfigValue(ACTION_CLEANUP_ACTION_STATUS, String.class);
        if (statusStr != null) {
            return Arrays.stream(statusStr.getValue().split("[;,]")).map(Action.Status::valueOf)
                    .collect(Collectors.toSet());
        }

        return EMPTY_STATUS_SET;
    }
}
