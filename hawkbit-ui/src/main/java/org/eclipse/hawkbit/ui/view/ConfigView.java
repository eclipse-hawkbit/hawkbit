/**
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.view;

import java.io.Serial;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

import jakarta.annotation.security.RolesAllowed;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.mgmt.json.model.system.MgmtSystemTenantConfigurationValueRequest;
import org.eclipse.hawkbit.ui.HawkbitMgmtClient;
import org.eclipse.hawkbit.ui.MainLayout;

@PageTitle("Config")
@Route(value = "config", layout = MainLayout.class)
@RolesAllowed({ "CONFIG_READ" })
@Slf4j
public final class ConfigView extends VerticalLayout implements BeforeLeaveObserver {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String OTHER_GROUP = "Other";

    /** Ordered mapping of group title -&gt; ordered config keys belonging to that group. */
    private static final Map<String, List<String>> GROUPS = new LinkedHashMap<>();
    /** Mapping of config key -&gt; human-friendly label. */
    private static final Map<String, String> LABELS = new LinkedHashMap<>();

    static {
        GROUPS.put("Authentication", List.of(
                "authentication.header.enabled",
                "authentication.header.authority",
                "authentication.gatewaytoken.enabled",
                "authentication.gatewaytoken.key",
                "authentication.targettoken.enabled"));
        GROUPS.put("Polling", List.of(
                "pollingTime",
                "pollingOverdueTime",
                "maintenanceWindowPollCount"));
        GROUPS.put("Rollout & Auto-assignment", List.of(
                "rollout.approval.enabled",
                "auto.assignment.approval.enabled"));
        GROUPS.put("Repository & Actions", List.of(
                "repository.actions.autoclose.enabled",
                "action.cleanup.auto.expiry",
                "action.cleanup.auto.status",
                "action.cleanup.onQuotaHit.percent",
                "action.delete.allowed.statuses",
                "batch.assignments.enabled",
                "user.confirmation.flow.enabled",
                "implicit.lock.enabled"));

        LABELS.put("authentication.header.enabled", "Header authentication");
        LABELS.put("authentication.header.authority", "Header authority");
        LABELS.put("authentication.targettoken.enabled", "Target token authentication");
        LABELS.put("authentication.gatewaytoken.enabled", "Gateway token authentication");
        LABELS.put("authentication.gatewaytoken.key", "Gateway token key");
        LABELS.put("pollingTime", "Polling time");
        LABELS.put("pollingOverdueTime", "Polling overdue time");
        LABELS.put("maintenanceWindowPollCount", "Maintenance window poll count");
        LABELS.put("rollout.approval.enabled", "Rollout approval required");
        LABELS.put("auto.assignment.approval.enabled", "Auto-assignment approval required");
        LABELS.put("repository.actions.autoclose.enabled", "Auto-close running actions");
        LABELS.put("action.cleanup.auto.expiry", "Action cleanup expiry (ms)");
        LABELS.put("action.cleanup.auto.status", "Action cleanup status");
        LABELS.put("action.cleanup.onQuotaHit.percent", "Action cleanup on quota-hit (%)");
        LABELS.put("action.delete.allowed.statuses", "User-deletable action statuses");
        LABELS.put("batch.assignments.enabled", "Batch assignments");
        LABELS.put("user.confirmation.flow.enabled", "User confirmation flow");
        LABELS.put("implicit.lock.enabled", "Implicit locking");
    }

    private final transient Map<String, MgmtSystemTenantConfigurationValueRequest> configValue = new LinkedHashMap<>();

    public ConfigView(final HawkbitMgmtClient hawkbitClient) {
        setSpacing(false);
        setSizeFull();

        final Map<String, Component> fields = new LinkedHashMap<>();
        Optional.ofNullable(hawkbitClient.getTenantManagementRestApi().getTenantConfiguration().getBody())
                .ifPresent(config -> config.forEach((key, value) -> {
                    final Component field = createField(key, value.getValue());
                    if (field != null) {
                        fields.put(key, field);
                    }
                }));

        add(buildContent(fields), buildFooter(hawkbitClient));
    }

    @Override
    public void beforeLeave(final BeforeLeaveEvent event) {
        if (configValue.isEmpty()) {
            return;
        }
        final BeforeLeaveEvent.ContinueNavigationAction action = event.postpone();
        final ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Unsaved changes");
        dialog.setText("You have unsaved changes. Leave without saving?");
        dialog.setCancelable(true);
        dialog.setCancelText("Stay");
        dialog.setConfirmText("Leave");
        dialog.setConfirmButtonTheme(ButtonVariant.LUMO_ERROR.getVariantName());
        dialog.addConfirmListener(e -> {
            dialog.close();
            action.proceed();
        });
        dialog.addCancelListener(e -> {
            dialog.close();
            action.cancel();
        });
        dialog.open();
    }

    private Component buildContent(final Map<String, Component> fields) {
        final Div content = new Div();
        content.setWidthFull();
        content.getStyle()
                .set("columns", "22rem 2")
                .set("column-gap", "var(--lumo-space-l)")
                .set("padding-bottom", "var(--lumo-space-m)");

        final List<String> rendered = new ArrayList<>();
        GROUPS.forEach((title, keys) -> {
            final List<Component> groupFields = new ArrayList<>();
            keys.forEach(key -> {
                final Component field = fields.get(key);
                if (field != null) {
                    groupFields.add(field);
                    rendered.add(key);
                }
            });
            if (!groupFields.isEmpty()) {
                content.add(groupCard(title, groupFields));
            }
        });

        final Map<String, Component> other = new TreeMap<>();
        fields.forEach((key, field) -> {
            if (!rendered.contains(key)) {
                other.put(key, field);
            }
        });
        if (!other.isEmpty()) {
            content.add(groupCard(OTHER_GROUP, new ArrayList<>(other.values())));
        }

        return content;
    }

    private static Component groupCard(final String title, final List<Component> fields) {
        final VerticalLayout card = new VerticalLayout();
        card.setPadding(true);
        card.setSpacing(true);
        card.add(new H4(title));
        fields.forEach(card::add);
        card.getStyle()
                .set("display", "block")
                .set("break-inside", "avoid")
                .set("-webkit-column-break-inside", "avoid")
                .set("width", "100%")
                .set("margin-bottom", "var(--lumo-space-m)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "var(--lumo-border-radius-m)");
        return card;
    }

    private HorizontalLayout buildFooter(final HawkbitMgmtClient hawkbitClient) {
        final Button saveButton = new Button("Save");
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(click -> save(hawkbitClient));
        saveButton.addClickShortcut(Key.ENTER);

        final HorizontalLayout footer = new HorizontalLayout(saveButton);
        footer.setWidthFull();
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        footer.getStyle()
                .set("position", "sticky")
                .set("bottom", "0")
                .set("background", "var(--lumo-base-color)")
                .set("border-top", "1px solid var(--lumo-contrast-10pct)")
                .set("padding", "var(--lumo-space-s) 0");
        return footer;
    }

    private void save(final HawkbitMgmtClient hawkbitClient) {
        if (configValue.isEmpty()) {
            successNotification("No changes to save");
            return;
        }
        final List<String> failed = new ArrayList<>();
        new LinkedHashMap<>(configValue).forEach((key, value) -> {
            try {
                hawkbitClient.getTenantManagementRestApi().updateTenantConfigurationValue(key, value);
                configValue.remove(key);
            } catch (final RuntimeException ex) {
                log.warn("Failed to update tenant configuration '{}'", key, ex);
                failed.add(key);
            }
        });
        if (failed.isEmpty()) {
            successNotification("Configuration saved");
        } else {
            errorNotification("Failed to save: " + String.join(", ", failed));
        }
    }

    private Component createField(final String key, final Object value) {
        final String label = LABELS.getOrDefault(key, key);
        switch (value) {
            case String strValue -> {
                final TextField tf = new TextField(label);
                tf.setValue(strValue);
                tf.setWidthFull();
                tf.addValueChangeListener(event -> onChange(key, strValue, event.getValue()));
                return tf;
            }
            case Boolean boolValue -> {
                final Checkbox cb = new Checkbox(label);
                cb.setValue(boolValue);
                cb.addValueChangeListener(event -> onChange(key, boolValue, event.getValue()));
                return cb;
            }
            case Long longValue -> {
                return numberField(label, key, (double) longValue);
            }
            case Integer intValue -> {
                return numberField(label, key, (double) intValue);
            }
            case null -> {
                log.debug("Null configuration value for key: {}", key);
                return null;
            }
            default -> {
                log.debug("Unexpected value type: {} -> {} (class: {})", key, value, value.getClass());
                return null;
            }
        }
    }

    private NumberField numberField(final String label, final String key, final double initialValue) {
        final NumberField nf = new NumberField(label);
        nf.setValue(initialValue);
        nf.setWidthFull();
        nf.addValueChangeListener(event -> onChange(key, initialValue, event.getValue()));
        return nf;
    }

    // Marks the key dirty only when the new value differs from the originally loaded one; reverting a
    // field back to its original value (e.g. checking then unchecking a box) clears it, so the
    // unsaved-changes guard does not trigger for a no-op edit.
    private void onChange(final String key, final Object original, final Object newValue) {
        if (Objects.equals(original, newValue)) {
            configValue.remove(key);
        } else {
            final MgmtSystemTenantConfigurationValueRequest request = new MgmtSystemTenantConfigurationValueRequest();
            request.setValue(newValue);
            configValue.put(key, request);
        }
    }

    private static void successNotification(final String message) {
        notification(message, NotificationVariant.LUMO_SUCCESS);
    }

    private static void errorNotification(final String message) {
        notification(message, NotificationVariant.LUMO_ERROR);
    }

    // Shown top-right so toasts never cover the sticky Save button in the bottom-right footer.
    private static void notification(final String message, final NotificationVariant variant) {
        final Notification notification = Notification.show(message);
        notification.addThemeVariants(variant);
        notification.setPosition(Notification.Position.TOP_END);
    }
}