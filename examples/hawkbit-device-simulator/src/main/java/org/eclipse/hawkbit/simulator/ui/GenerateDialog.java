/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.simulator.ui;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.hawkbit.simulator.AbstractSimulatedDevice.Protocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.Property;
import com.vaadin.data.Validator;
import com.vaadin.data.util.ObjectProperty;
import com.vaadin.data.validator.NullValidator;
import com.vaadin.data.validator.RangeValidator;
import com.vaadin.data.validator.RegexpValidator;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Resource;
import com.vaadin.ui.Button;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Window;

/**
 * Popup dialog window for setting the values of generating the simulated
 * devices, e.g. the amount.
 * 
 *
 */
// Vaadin Inheritance
@SuppressWarnings("squid:MaximumInheritanceDepth")
public class GenerateDialog extends Window {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateDialog.class);

    private final FormLayout formLayout = new FormLayout();

    private final TextField namePrefixTextField;
    private final TextField amountTextField;
    private final TextField tenantTextField;
    private final TextField pollDelayTextField;
    private final TextField pollUrlTextField;
    private final TextField gatewayTokenTextField;
    private OptionGroup protocolGroup;
    private Button buttonOk;
    private final boolean dmfEnabled;

    /**
     * Creates a new pop window for setting the configuration of simulating
     * devices.
     * 
     * @param callback
     *            the callback which is called when the dialog has been
     *            successfully confirmed.
     * @param dmfEnabled
     *            indicates if the AMQP/DMF interface is enabled by
     *            configuration and if the option DMF should be enabled or not
     */
    public GenerateDialog(final GenerateDialogCallback callback, final boolean dmfEnabled) {
        this.dmfEnabled = dmfEnabled;
        formLayout.setSpacing(true);
        formLayout.setMargin(true);

        namePrefixTextField = createRequiredTextfield("name prefix", "dmfSimulated", FontAwesome.INFO,
                new NullValidator("Must be given", false));

        amountTextField = createRequiredTextfield("amount", new ObjectProperty<Integer>(10), FontAwesome.GEAR,
                new RangeValidator<Integer>("Must be between 1 and 30000", Integer.class, 1, 30000));

        tenantTextField = createRequiredTextfield("tenant", "default", FontAwesome.USER,
                new NullValidator("Must be given", false));

        pollDelayTextField = createRequiredTextfield("poll delay (sec)", new ObjectProperty<Integer>(10),
                FontAwesome.CLOCK_O, new RangeValidator<Integer>("Must be between 1 and 60", Integer.class, 1, 60));

        pollUrlTextField = createRequiredTextfield("base poll URL endpoint", "http://localhost:8080",
                FontAwesome.FLAG_O, new RegexpValidator(
                        "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]", "is not an URL"));
        pollUrlTextField.setColumns(50);
        pollUrlTextField.setVisible(false);

        gatewayTokenTextField = createRequiredTextfield("gateway token", "", FontAwesome.FLAG_O, null);
        gatewayTokenTextField.setColumns(50);
        gatewayTokenTextField.setVisible(false);

        createProtocolGroup();
        createOkButton(callback);

        namePrefixTextField.addValueChangeListener(event -> checkValid());
        amountTextField.addValueChangeListener(event -> checkValid());
        tenantTextField.addValueChangeListener(event -> checkValid());

        formLayout.addComponent(namePrefixTextField);
        formLayout.addComponent(amountTextField);
        formLayout.addComponent(tenantTextField);
        formLayout.addComponent(protocolGroup);
        formLayout.addComponent(pollDelayTextField);
        formLayout.addComponent(pollUrlTextField);
        formLayout.addComponent(gatewayTokenTextField);
        formLayout.addComponent(buttonOk);

        setCaption("Simulate Devices");
        setContent(formLayout);
        setResizable(false);
        center();
    }

    private void checkValid() {
        buttonOk.setEnabled(namePrefixTextField.isValid() && amountTextField.isValid() && tenantTextField.isValid()
                && pollDelayTextField.isValid());

    }

    /**
     * Callback interface to retrieve the result from the dialog window.
     * 
     * @author Michael Hirsch
     *
     */
    @FunctionalInterface
    interface GenerateDialogCallback {
        /**
         * Callback method which is called when dialog closes with the OK
         * button.
         * 
         * @param namePrefix
         *            the parameter for name prefix for the simulated devices
         * @param tenant
         *            the tenant for the simulated devices
         * @param amount
         *            the number of simulated devices to be created
         * @param pollDelay
         *            the delay poll time in seconds for DDI devices
         * @param basePollURL
         *            the base http URL endpoint for DDI devices
         * @param gatewayToken
         *            the gateway token header for authentication for DDI
         *            devices
         * @param protocol
         *            the protocol to be used for the simulated devices to be
         *            generated
         */
        void okButton(final String namePrefix, final String tenant, final int amount, final int pollDelay,
                final URL basePollURL, final String gatewayToken, final Protocol protocol);
    }

    private void createProtocolGroup() {

        this.protocolGroup = new OptionGroup("Simulated Device Protocol");
        protocolGroup.addItem(Protocol.DMF_AMQP);
        protocolGroup.addItem(Protocol.DDI_HTTP);
        protocolGroup.select(Protocol.DMF_AMQP);
        protocolGroup.setItemCaption(Protocol.DMF_AMQP, "Device Management Federation API (AMQP push)");
        protocolGroup.setItemCaption(Protocol.DDI_HTTP, "Direct Device Interface (HTTP poll)");
        protocolGroup.setNullSelectionAllowed(false);
        protocolGroup.addValueChangeListener(event -> {
            final boolean directDeviceOptionSelected = event.getProperty().getValue().equals(Protocol.DDI_HTTP);
            pollUrlTextField.setVisible(directDeviceOptionSelected);
            gatewayTokenTextField.setVisible(directDeviceOptionSelected);
        });
        protocolGroup.setItemEnabled(Protocol.DMF_AMQP, dmfEnabled);
        if (!dmfEnabled) {
            protocolGroup.select(Protocol.DDI_HTTP);
        }
    }

    private void createOkButton(final GenerateDialogCallback callback) {

        this.buttonOk = new Button("generate");
        buttonOk.setImmediate(true);
        buttonOk.setIcon(FontAwesome.GEARS);
        buttonOk.addClickListener(event -> {
            try {
                callback.okButton(namePrefixTextField.getValue(), tenantTextField.getValue(),
                        Integer.valueOf(amountTextField.getValue().replace(".", "").replace(",", "")),
                        Integer.valueOf(pollDelayTextField.getValue().replace(".", "")),
                        new URL(pollUrlTextField.getValue()), gatewayTokenTextField.getValue(),
                        (Protocol) protocolGroup.getValue());
            } catch (final NumberFormatException | MalformedURLException e) {
                LOGGER.info(e.getMessage(), e);
            }
            GenerateDialog.this.close();
        });
    }

    private static TextField createRequiredTextfield(final String caption, final String value, final Resource icon,
            final Validator validator) {
        final TextField textField = new TextField(caption, value);
        return addTextFieldValues(textField, icon, validator);
    }

    private static TextField createRequiredTextfield(final String caption, final Property<?> dataSource,
            final Resource icon, final Validator validator) {
        final TextField textField = new TextField(caption, dataSource);
        return addTextFieldValues(textField, icon, validator);
    }

    private static TextField addTextFieldValues(final TextField textField, final Resource icon,
            final Validator validator) {
        textField.setIcon(icon);
        textField.setRequired(true);
        if (validator != null) {
            textField.addValidator(validator);
        }
        return textField;
    }

}
