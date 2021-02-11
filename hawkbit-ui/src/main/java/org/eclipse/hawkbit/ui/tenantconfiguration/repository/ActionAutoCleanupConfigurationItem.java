/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.tenantconfiguration.repository;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.TenantConfiguration;
import org.eclipse.hawkbit.ui.common.builder.TextFieldBuilder;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxySystemConfigRepository;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.data.Binder;
import com.vaadin.data.ValidationResult;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.validator.IntegerRangeValidator;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

/**
 * This class represents the UI item for configuring automatic action cleanup in
 * the Repository Configuration section of the System Configuration view.
 */
public class ActionAutoCleanupConfigurationItem extends VerticalLayout {

    private static final long serialVersionUID = 1L;

    private static final int MAX_EXPIRY_IN_DAYS = 1000;

    private static final String MSG_KEY_PREFIX = "label.configuration.repository.autocleanup.action.prefix";
    private static final String MSG_KEY_BODY = "label.configuration.repository.autocleanup.action.body";
    private static final String MSG_KEY_SUFFIX = "label.configuration.repository.autocleanup.action.suffix";
    private static final String MSG_KEY_INVALID_EXPIRY = "label.configuration.repository.autocleanup.action.expiry.invalid";
    private static final String MSG_KEY_NOTICE = "label.configuration.repository.autocleanup.action.notice";

    private static final Collection<ActionStatusOption> ACTION_STATUS_OPTIONS = Arrays.asList(
            new ActionStatusOption(Status.CANCELED), new ActionStatusOption(Status.ERROR),
            new ActionStatusOption(Status.CANCELED, Status.ERROR));

    private final VerticalLayout container;
    private final ComboBox<ActionStatusOption> actionStatusCombobox;
    private final TextField actionExpiryInput;

    private final VaadinMessageSource i18n;

    /**
     * Constructs the Action Cleanup configuration UI.
     *
     * @param binder
     *            ProxySystemConfigWindow binder
     * @param i18n
     *            VaadinMessageSource
     */
    public ActionAutoCleanupConfigurationItem(final Binder<ProxySystemConfigRepository> binder,
            final VaadinMessageSource i18n) {
        this.i18n = i18n;
        this.setSpacing(false);
        this.setMargin(false);
        addComponent(SPUIComponentProvider.generateLabel(i18n, "label.configuration.repository.autocleanup.action"));
        container = new VerticalLayout();
        container.setSpacing(false);
        container.setMargin(false);
        final HorizontalLayout row1 = newHorizontalLayout();
        actionStatusCombobox = SPUIComponentProvider.getComboBox(
                UIComponentIdProvider.SYSTEM_CONFIGURATION_ACTION_CLEANUP_ACTION_TYPES, null, null, null, false,
                ActionStatusOption::getName, DataProvider.ofCollection(ACTION_STATUS_OPTIONS));
        actionStatusCombobox.removeStyleName(ValoTheme.COMBOBOX_SMALL);
        actionStatusCombobox.addStyleName(ValoTheme.COMBOBOX_TINY);
        actionStatusCombobox.setWidth(200.0F, Unit.PIXELS);
        binder.bind(actionStatusCombobox, ProxySystemConfigRepository::getActionCleanupStatus,
                ProxySystemConfigRepository::setActionCleanupStatus);
        actionExpiryInput = new TextFieldBuilder(TenantConfiguration.VALUE_MAX_SIZE).buildTextComponent();
        actionExpiryInput.setId(UIComponentIdProvider.SYSTEM_CONFIGURATION_ACTION_CLEANUP_ACTION_EXPIRY);
        actionExpiryInput.setWidth(55, Unit.PIXELS);
        binder.forField(actionExpiryInput).asRequired(i18n.getMessage(MSG_KEY_INVALID_EXPIRY))
                .withValidator((value, context) -> {
                    try {
                        return new IntegerRangeValidator(i18n.getMessage(MSG_KEY_INVALID_EXPIRY), 1, MAX_EXPIRY_IN_DAYS)
                                .apply(Integer.parseInt(value), context);
                    } catch (final NumberFormatException ex) {
                        return ValidationResult.error(i18n.getMessage(MSG_KEY_INVALID_EXPIRY));
                    }
                }).bind(ProxySystemConfigRepository::getActionExpiryDays, ProxySystemConfigRepository::setActionExpiryDays);

        row1.addComponent(newLabel(MSG_KEY_PREFIX));
        row1.addComponent(actionStatusCombobox);
        row1.addComponent(newLabel(MSG_KEY_BODY));
        row1.addComponent(actionExpiryInput);
        row1.addComponent(newLabel(MSG_KEY_SUFFIX));
        container.addComponent(row1);

        final HorizontalLayout row2 = newHorizontalLayout();
        row2.addComponent(newLabel(MSG_KEY_NOTICE));
        container.addComponent(row2);
        if (binder.getBean().isActionAutocleanup()) {
            showSettings();
        }
    }

    private Label newLabel(final String msgKey) {
        final Label label = SPUIComponentProvider.generateLabel(i18n, msgKey);
        label.setWidthUndefined();
        return label;
    }

    private static HorizontalLayout newHorizontalLayout() {
        final HorizontalLayout layout = new HorizontalLayout();
        layout.setSpacing(true);
        return layout;
    }

    /**
     * Show action auto cleanup settings
     */
    public void showSettings() {
        addComponent(container);
    }

    /**
     * Hide action auto cleanup settings
     */
    public void hideSettings() {
        removeComponent(container);
    }

    /**
     * @return Action status option of auto cleanup
     */
    public static Collection<ActionStatusOption> getActionStatusOptions() {
        return ACTION_STATUS_OPTIONS;
    }

    /**
     * Auto cleanup action status options
     */
    public static class ActionStatusOption implements Serializable {
        private static final long serialVersionUID = 1L;

        private static final CharSequence SEPARATOR = " + ";
        private final Set<Status> statusSet;
        private String name;

        /**
         * Constructor for ActionStatusOption
         *
         * @param status
         *            Action status
         */
        public ActionStatusOption(final Status... status) {
            statusSet = Arrays.stream(status).collect(Collectors.toCollection(() -> EnumSet.noneOf(Status.class)));
        }

        /**
         * @return Status name
         */
        public String getName() {
            if (name == null) {
                name = assembleName();
            }
            return name;
        }

        /**
         * @return List of status
         */
        public Set<Status> getStatus() {
            return statusSet;
        }

        /**
         * @return Join action status
         */
        private String assembleName() {
            return statusSet.stream().map(Status::name).collect(Collectors.joining(SEPARATOR));
        }
    }
}
