/**
 * Copyright (c) 2022 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.vaadin.server.FontAwesome;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;
import org.eclipse.hawkbit.repository.ConfirmationManagement;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.UiProperties;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.ConfirmationDialog;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyKeyValueDetails;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTargetConfirmationOptions;
import org.eclipse.hawkbit.ui.common.detailslayout.KeyValueDetailsComponent;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleNoBorder;
import org.eclipse.hawkbit.ui.utils.SPDateTimeUtil;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.themes.ValoTheme;
import org.springframework.util.StringUtils;

import static org.eclipse.hawkbit.ui.utils.UIComponentIdProvider.AUTO_CONFIRMATION_TOGGLE_DIALOG;
import static org.eclipse.hawkbit.ui.utils.UIComponentIdProvider.AUTO_CONFIRMATION_DETAILS_ACTIVATEDAT;
import static org.eclipse.hawkbit.ui.utils.UIComponentIdProvider.AUTO_CONFIRMATION_DETAILS_INITIATOR;
import static org.eclipse.hawkbit.ui.utils.UIComponentIdProvider.AUTO_CONFIRMATION_DETAILS_REMARK;
import static org.eclipse.hawkbit.ui.utils.UIComponentIdProvider.AUTO_CONFIRMATION_DETAILS_ROLLOUTS_USER;
import static org.eclipse.hawkbit.ui.utils.UIComponentIdProvider.AUTO_CONFIRMATION_DETAILS_STATE;

/**
 * target auto confirmation detail component
 */
public class TargetConfirmationOptionsComponent extends CustomField<ProxyTargetConfirmationOptions> {

    private static final long serialVersionUID = 1L;

    private final transient TargetAutoConfActivationWindowBuilder windowBuilder;
    private final transient ConfirmationManagement confirmationManagement;
    private final transient SpPermissionChecker permissionChecker;
    private final VaadinMessageSource i18n;
    private final HorizontalLayout targetConfirmationOptionsLayout;

    /**
     * Constructor for TargetConfirmationOptionsComponent
     *
     * @param commonUiDependencies
     *            the {@link CommonUiDependencies}
     * @param uiProperties
     *            the {@link UiProperties}
     * @param confirmationManagement
     *            the {@link ConfirmationManagement}
     * @param tenantAware
     *            the {@link TenantAware}
     */
    public TargetConfirmationOptionsComponent(final CommonUiDependencies commonUiDependencies,
            final UiProperties uiProperties, final ConfirmationManagement confirmationManagement,
            final TenantAware tenantAware) {
        this.i18n = commonUiDependencies.getI18n();
        this.confirmationManagement = confirmationManagement;
        this.permissionChecker = commonUiDependencies.getPermChecker();
        this.windowBuilder = new TargetAutoConfActivationWindowBuilder(commonUiDependencies, uiProperties, tenantAware,
                confirmationManagement);

        this.targetConfirmationOptionsLayout = new HorizontalLayout();
        this.targetConfirmationOptionsLayout.setSpacing(true);
        this.targetConfirmationOptionsLayout.setMargin(false);
        this.targetConfirmationOptionsLayout.setSizeFull();
    }

    @Override
    public ProxyTargetConfirmationOptions getValue() {
        // keep returning same value since reloading (triggering 'doSetValue' method)
        // will be done on button click.
        return new ProxyTargetConfirmationOptions();
    }

    @Override
    protected Component initContent() {
        return targetConfirmationOptionsLayout;
    }

    @Override
    protected void doSetValue(final ProxyTargetConfirmationOptions targetConfirmationOptions) {
        targetConfirmationOptionsLayout.removeAllComponents();

        if (targetConfirmationOptions == null) {
            return;
        }

        final boolean isAutoConfirmationEnabled = targetConfirmationOptions.isAutoConfirmationEnabled();

        if (isAutoConfirmationEnabled) {
            final KeyValueDetailsComponent detailsLayout = buildAutoConfirmationDetailsLayout(
                    targetConfirmationOptions);
            targetConfirmationOptionsLayout.addComponent(detailsLayout);
            targetConfirmationOptionsLayout.setExpandRatio(detailsLayout, 1.0F);
        } else {
            final KeyValueDetailsComponent component = toKeyValueDetailsComponent(
                    Collections.singletonList(new ProxyKeyValueDetails(AUTO_CONFIRMATION_DETAILS_STATE,
                            i18n.getMessage("label.target.auto.confirmation.state"),
                            i18n.getMessage("label.target.auto.confirmation.deactivated"))));

            targetConfirmationOptionsLayout.addComponent(component);
            targetConfirmationOptionsLayout.setExpandRatio(component, 1.0F);
        }

        // do only provide toggle button when having permission for that
        if (permissionChecker.hasUpdateTargetPermission()) {
            final Button button = buildAutoConfirmationToggleButton(targetConfirmationOptions);
            targetConfirmationOptionsLayout.addComponent(button);
        }
    }

    private KeyValueDetailsComponent buildAutoConfirmationDetailsLayout(final ProxyTargetConfirmationOptions options) {
        final List<ProxyKeyValueDetails> values = new ArrayList<>();
        values.add(new ProxyKeyValueDetails(AUTO_CONFIRMATION_DETAILS_STATE,
                i18n.getMessage("label.target.auto.confirmation.state"),
                i18n.getMessage("label.target.auto.confirmation.active")));

        if (StringUtils.hasText(options.getInitiator())) {
            values.add(new ProxyKeyValueDetails(AUTO_CONFIRMATION_DETAILS_INITIATOR,
                    i18n.getMessage("label.target.auto.confirmation.initiator"), options.getInitiator()));
        }
        values.add(new ProxyKeyValueDetails(AUTO_CONFIRMATION_DETAILS_ROLLOUTS_USER,
                i18n.getMessage("label.target.auto.confirmation.systemuser"), options.getInitiatedSystemUser()));
        if (StringUtils.hasText(options.getRemark())) {
            values.add(new ProxyKeyValueDetails(AUTO_CONFIRMATION_DETAILS_REMARK,
                    i18n.getMessage("label.target.auto.confirmation.remark"), options.getRemark()));
        }
        values.add(new ProxyKeyValueDetails(AUTO_CONFIRMATION_DETAILS_ACTIVATEDAT,
                i18n.getMessage("label.target.auto.confirmation.activatedat"),
                SPDateTimeUtil.getFormattedDate(options.getActivatedAt())));

        return toKeyValueDetailsComponent(values);
    }

    private static KeyValueDetailsComponent toKeyValueDetailsComponent(final List<ProxyKeyValueDetails> values) {
        final KeyValueDetailsComponent details = new KeyValueDetailsComponent();
        details.disableSpacing();
        details.setValue(values);
        return details;
    }

    private Button buildAutoConfirmationToggleButton(final ProxyTargetConfirmationOptions options) {
        final Button toggleAutoConfirmationButton;

        if (options.isAutoConfirmationEnabled()) {
            toggleAutoConfirmationButton = SPUIComponentProvider.getButton(
                    UIComponentIdProvider.AUTO_CONFIRMATION_DETAILS_TOGGLE, "",
                    i18n.getMessage("button.target.auto.confirmation.disable"), ValoTheme.BUTTON_HUGE, true,
                    FontAwesome.TOGGLE_ON, SPUIButtonStyleNoBorder.class);
        } else {
            toggleAutoConfirmationButton = SPUIComponentProvider.getButton(
                    UIComponentIdProvider.AUTO_CONFIRMATION_DETAILS_TOGGLE, "",
                    i18n.getMessage("button.target.auto.confirmation.activate"), ValoTheme.BUTTON_HUGE, true,
                    FontAwesome.TOGGLE_OFF, SPUIButtonStyleNoBorder.class);
        }

        toggleAutoConfirmationButton.addClickListener(e -> {
            if (options.isAutoConfirmationEnabled()) {
                final ConfirmationDialog dialog = ConfirmationDialog
                        .newBuilder(i18n, AUTO_CONFIRMATION_TOGGLE_DIALOG).icon(VaadinIcons.WARNING)
                        .caption(i18n.getMessage("caption.target.auto.confirmation.disable"))
                        .question(i18n.getMessage("message.target.auto.confirmation.disable")).onSaveOrUpdate(() -> {
                            confirmationManagement.deactivateAutoConfirmation(options.getControllerId());
                            doSetValue(ProxyTargetConfirmationOptions.disabled(options.getControllerId()));
                        }).build();

                UI.getCurrent().addWindow(dialog.getWindow());

                dialog.getWindow().bringToFront();
            } else {
                final Window window = windowBuilder.getWindowForUpdate(options);
                UI.getCurrent().addWindow(window);
                window.setVisible(Boolean.TRUE);
            }
        });

        return toggleAutoConfirmationButton;
    }

}
