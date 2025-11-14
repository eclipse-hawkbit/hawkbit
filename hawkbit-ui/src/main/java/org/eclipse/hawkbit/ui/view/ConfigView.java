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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import jakarta.annotation.security.RolesAllowed;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
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
public class ConfigView extends VerticalLayout {

    private static final String WIDTH = "width";
    private static final String PX_300 = "300px";

    private final Map<String, MgmtSystemTenantConfigurationValueRequest> configValue = new HashMap<>();

    public ConfigView(final HawkbitMgmtClient hawkbitClient) {
        setSpacing(false);
        final Button saveButton = new Button("Save");
        Optional.ofNullable(
                hawkbitClient.getTenantManagementRestApi().getTenantConfiguration().getBody()).ifPresent(config ->
                config.forEach((k, v) -> {
                    if (v.getValue() instanceof String strValue) {
                        final TextField tf = new TextField(k, strValue, event -> {
                            final MgmtSystemTenantConfigurationValueRequest vre = new MgmtSystemTenantConfigurationValueRequest();
                            vre.setValue(event.getValue());
                            configValue.put(k, vre);
                        });
                        tf.getElement().getStyle().set(WIDTH, PX_300);
                        add(tf);
                    } else if (v.getValue() instanceof Boolean boolValue) {
                        add(new Checkbox(k, boolValue, event -> {
                            final MgmtSystemTenantConfigurationValueRequest vre = new MgmtSystemTenantConfigurationValueRequest();
                            vre.setValue(event.getValue());
                            configValue.put(k, vre);
                        }));
                    } else if (v.getValue() instanceof Long longValue) {
                        final NumberField nf = new NumberField(k, (double) longValue, event -> {
                            final MgmtSystemTenantConfigurationValueRequest vre = new MgmtSystemTenantConfigurationValueRequest();
                            vre.setValue(event.getValue());
                            configValue.put(k, vre);
                        });
                        nf.getElement().getStyle().set(WIDTH, PX_300);
                        add(nf);
                    } else if (v.getValue() instanceof Integer intValue) {
                        final NumberField nf = new NumberField(k, (double) intValue, event -> {
                            MgmtSystemTenantConfigurationValueRequest vre = new MgmtSystemTenantConfigurationValueRequest();
                            vre.setValue(event.getValue());
                            configValue.put(k, vre);
                        });
                        nf.getElement().getStyle().set(WIDTH, PX_300);
                        add(nf);
                    } else {
                        log.debug("Unexpected value type: {} -> {} (class: {})",
                                k, v.getValue(), v.getValue() == null ? "null" : v.getValue().getClass());
                    }
                }));

        saveButton.addClickListener(click -> configValue.forEach(
                (key, value) -> hawkbitClient.getTenantManagementRestApi().updateTenantConfigurationValue(key, value)));
        saveButton.addClickShortcut(Key.ENTER);
        add(saveButton);
    }
}