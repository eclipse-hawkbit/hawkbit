/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.tenantconfiguration.repository;

import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.ACTION_CLEANUP_ACTION_EXPIRY;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.ACTION_CLEANUP_ACTION_STATUS;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.TenantConfiguration;
import org.eclipse.hawkbit.repository.model.TenantConfigurationValue;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;
import org.eclipse.hawkbit.ui.common.builder.LabelBuilder;
import org.eclipse.hawkbit.ui.common.builder.TextFieldBuilder;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.tenantconfiguration.generic.AbstractBooleanTenantConfigurationItem;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.util.StringUtils;

import com.vaadin.data.Validator;
import com.vaadin.data.validator.IntegerRangeValidator;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

/**
 * This class represents the UI item for the target security token section in
 * the authentication configuration view.
 */
public class ActionAutocleanupConfigurationItem extends AbstractBooleanTenantConfigurationItem {

    private static final long serialVersionUID = 1L;

    private static final int MAX_EXPIRY_IN_DAYS = 1000000;

    private static final String MSG_KEY_PREFIX = "label.configuration.repository.autocleanup.action.prefix";
    private static final String MSG_KEY_BODY = "label.configuration.repository.autocleanup.action.body";
    private static final String MSG_KEY_SUFFIX = "label.configuration.repository.autocleanup.action.suffix";

    private static final Collection<ActionStatusOption> ACTION_STATUS_OPTIONS = Arrays.asList(
            new ActionStatusOption(Status.CANCELED), new ActionStatusOption(Status.ERROR),
            new ActionStatusOption(Status.CANCELED, Status.ERROR));

    private final VerticalLayout container;
    private final ComboBox actionStatusCombobox;
    private final TextField actionExpiryInput;

    private final VaadinMessageSource i18n;

    private boolean cleanupEnabled;
    private boolean cleanupEnabledChanged;
    private boolean actionStatusChanged;
    private boolean actionExpiryChanged;

    public ActionAutocleanupConfigurationItem(final TenantConfigurationManagement tenantConfigurationManagement,
            final VaadinMessageSource i18n) {
        super(TenantConfigurationKey.ACTION_CLEANUP_ENABLED, tenantConfigurationManagement, i18n);
        super.init("label.configuration.repository.autocleanup.action");

        this.i18n = i18n;
        cleanupEnabled = isConfigEnabled();

        container = new VerticalLayout();
        container.setImmediate(true);

        final HorizontalLayout horizontalContainer = new HorizontalLayout();
        horizontalContainer.setSpacing(true);
        horizontalContainer.setImmediate(true);

        actionStatusCombobox = SPUIComponentProvider.getComboBox(null, "200", null, null, false, "",
                "label.combobox.action.status.options");
        actionStatusCombobox.setId(UIComponentIdProvider.SYSTEM_CONFIGURATION_ACTION_CLEANUP_ACTION_TYPES);
        actionStatusCombobox.setNullSelectionAllowed(false);

        for (final ActionStatusOption statusOption : ACTION_STATUS_OPTIONS) {
            actionStatusCombobox.addItem(statusOption);
            actionStatusCombobox.setItemCaption(statusOption, statusOption.getName());
        }
        actionStatusCombobox.setImmediate(true);
        actionStatusCombobox.addValueChangeListener(e -> onActionStatusChanged());
        actionStatusCombobox.select(getActionStatusOption());

        actionExpiryInput = new TextFieldBuilder(TenantConfiguration.VALUE_MAX_SIZE).buildTextComponent();
        actionExpiryInput.setId(UIComponentIdProvider.SYSTEM_CONFIGURATION_ACTION_CLEANUP_ACTION_EXPIRY);
        actionExpiryInput.setWidth(75, Unit.PIXELS);
        actionExpiryInput.setNullSettingAllowed(false);
        actionExpiryInput.addTextChangeListener(e -> onActionExpiryChanged());
        actionExpiryInput.addValidator(new ActionExpiryValidator("Not valid"));
        actionExpiryInput.setValue(String.valueOf(getActionExpiry()));

        horizontalContainer.addComponent(newLabel(MSG_KEY_PREFIX));
        horizontalContainer.addComponent(actionStatusCombobox);
        horizontalContainer.addComponent(newLabel(MSG_KEY_BODY));
        horizontalContainer.addComponent(actionExpiryInput);
        horizontalContainer.addComponent(newLabel(MSG_KEY_SUFFIX));

        container.addComponent(horizontalContainer);

        if (isConfigEnabled()) {
            setSettingsVisible(true);
        }

    }

    @Override
    public void configEnable() {
        if (!cleanupEnabled) {
            cleanupEnabledChanged = true;
        }
        cleanupEnabled = true;
        setSettingsVisible(true);
    }

    @Override
    public void configDisable() {
        if (cleanupEnabled) {
            cleanupEnabledChanged = true;
        }
        cleanupEnabled = false;
        setSettingsVisible(false);
    }

    @Override
    public void save() {
        if (cleanupEnabledChanged) {
            setActionCleanupEnabled(cleanupEnabled);
            cleanupEnabledChanged = false;
        }
        if (cleanupEnabled && actionStatusChanged) {
            setActionStatus((ActionStatusOption) actionStatusCombobox.getValue());
            actionStatusChanged = false;
        }
        if (cleanupEnabled && actionExpiryChanged) {
            setActionExpiry(Long.parseLong(actionExpiryInput.getValue()));
            actionExpiryChanged = false;
        }
    }

    @Override
    public void undo() {
        cleanupEnabledChanged = false;
        cleanupEnabled = readConfigValue(getConfigurationKey(), Boolean.class).getValue();
        actionStatusChanged = false;
        actionStatusCombobox.select(getActionStatusOption());
        actionExpiryChanged = false;
        actionExpiryInput.setValue(String.valueOf(getActionExpiry()));
    }

    private void onActionExpiryChanged() {
        actionExpiryChanged = true;
        notifyConfigurationChanged();
    }

    private void onActionStatusChanged() {
        actionStatusChanged = true;
        notifyConfigurationChanged();
    }

    private Label newLabel(final String msgKey) {
        final Label label = new LabelBuilder().name(i18n.getMessage(msgKey)).buildLabel();
        label.setWidthUndefined();
        return label;
    }

    private void setSettingsVisible(final boolean visible) {
        if (visible) {
            addComponent(container);
        } else {
            removeComponent(container);
        }
    }

    private void setActionCleanupEnabled(final boolean enabled) {
        writeConfigValue(getConfigurationKey(), enabled);
    }

    private void setActionExpiry(final long days) {
        writeConfigValue(ACTION_CLEANUP_ACTION_EXPIRY, TimeUnit.DAYS.toMillis(days));
    }

    private long getActionExpiry() {
        return TimeUnit.MILLISECONDS.toDays(readConfigValue(ACTION_CLEANUP_ACTION_EXPIRY, Long.class).getValue());
    }

    private void setActionStatus(final ActionStatusOption statusOption) {
        setActionStatus(statusOption.getStatus());
    }

    private void setActionStatus(final Set<Status> status) {
        writeConfigValue(ACTION_CLEANUP_ACTION_STATUS,
                status.stream().map(Status::name).collect(Collectors.joining(",")));
    }

    private ActionStatusOption getActionStatusOption() {
        final Set<Status> actionStatus = getActionStatus();
        return ACTION_STATUS_OPTIONS.stream().filter(option -> actionStatus.equals(option.getStatus())).findFirst()
                .orElse(ACTION_STATUS_OPTIONS.iterator().next());
    }

    private Set<Status> getActionStatus() {
        final TenantConfigurationValue<String> statusStr = readConfigValue(ACTION_CLEANUP_ACTION_STATUS, String.class);
        if (statusStr != null) {
            return Arrays.stream(statusStr.getValue().split(";|,")).map(Status::valueOf).collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    private <T extends Serializable> TenantConfigurationValue<T> readConfigValue(final String key,
            final Class<T> valueType) {
        return getTenantConfigurationManagement().getConfigurationValue(key, valueType);
    }

    private <T extends Serializable> void writeConfigValue(final String key, final T value) {
        getTenantConfigurationManagement().addOrUpdateConfiguration(key, value);
    }

    private static class ActionStatusOption {

        private static final CharSequence SEPARATOR = " + ";
        private final Set<Status> statusSet;
        private String name;

        public ActionStatusOption(final Status... status) {
            statusSet = Arrays.stream(status).collect(Collectors.toSet());
        }

        public String getName() {
            if (name == null) {
                name = statusSet.stream().map(Status::name).collect(Collectors.joining(SEPARATOR));
            }
            return name;
        }

        public Set<Status> getStatus() {
            return statusSet;
        }

    }

    static class ActionExpiryValidator implements Validator {
        private static final long serialVersionUID = 1L;

        private final String message;

        ActionExpiryValidator(final String message) {
            this.message = message;
        }

        @Override
        public void validate(final Object value) {
            System.out.println(value);
            if (!StringUtils.isEmpty(value)) {
                try {
                    new IntegerRangeValidator(message, 1, MAX_EXPIRY_IN_DAYS)
                            .validate(Integer.parseInt(value.toString()));
                } catch (final RuntimeException e) {
                    throw new InvalidValueException(message);
                }
            } else {
                System.out.println("empty");
                throw new InvalidValueException(message);
            }
        }
    }

}
