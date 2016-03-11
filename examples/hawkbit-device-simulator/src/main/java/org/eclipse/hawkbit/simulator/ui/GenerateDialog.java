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
 * @author Michael Hirsch
 *
 */
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
    private final OptionGroup protocolGroup;
    private final Button buttonOk;

    /**
     * Creates a new pop window for setting the configuration of simulating
     * devices.
     * 
     * @param callback
     *            the callback which is called when the dialog has been
     *            successfully confirmed.
     */
    public GenerateDialog(final GenerateDialogCallback callback) {

        formLayout.setSpacing(true);
        formLayout.setMargin(true);

        namePrefixTextField = createRequiredTextfield("name prefix", "dmfSimulated", FontAwesome.INFO, true,
                new NullValidator("Must be given", false));

        amountTextField = createRequiredTextfield("amount", new ObjectProperty<Integer>(10), FontAwesome.GEAR, true,
                new RangeValidator<Integer>("Must be between 1 and 30000", Integer.class, 1, 30000));

        tenantTextField = createRequiredTextfield("tenant", "default", FontAwesome.USER, true,
                new NullValidator("Must be given", false));

        pollDelayTextField = createRequiredTextfield("poll delay (sec)", new ObjectProperty<Integer>(10),
                FontAwesome.CLOCK_O, true,
                new RangeValidator<Integer>("Must be between 1 and 60", Integer.class, 1, 60));
        pollDelayTextField.setVisible(false);

        pollUrlTextField = createRequiredTextfield("base poll URL endpoint", "http://localhost:8080",
                FontAwesome.FLAG_O, true, new RegexpValidator(
                        "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]", "is not an URL"));
        pollUrlTextField.setColumns(50);
        pollUrlTextField.setVisible(false);

        gatewayTokenTextField = createRequiredTextfield("gateway token", "", FontAwesome.FLAG_O, true, null);
        gatewayTokenTextField.setColumns(50);
        gatewayTokenTextField.setVisible(false);

        protocolGroup = createProtocolGroup();
        buttonOk = createOkButton(callback);

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
        if (namePrefixTextField.isValid() && amountTextField.isValid() && tenantTextField.isValid()
                && pollDelayTextField.isValid()) {
            buttonOk.setEnabled(true);
        } else {
            buttonOk.setEnabled(false);
        }
    }

    @Override
    public int hashCode() {// NOSONAR - as this is generated
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((formLayout == null) ? 0 : formLayout.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {// NOSONAR - as this is generated
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final GenerateDialog other = (GenerateDialog) obj;
        if (formLayout == null) {
            if (other.formLayout != null) {
                return false;
            }
        } else if (!formLayout.equals(other.formLayout)) {
            return false;
        }
        return true;
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

    private OptionGroup createProtocolGroup() {

        final OptionGroup protocolGroup = new OptionGroup("Simulated Device Protocol");
        protocolGroup.addItem(Protocol.DMF_AMQP);
        protocolGroup.addItem(Protocol.DDI_HTTP);
        protocolGroup.setItemCaption(Protocol.DMF_AMQP, "Device Management Federation API (AMQP push)");
        protocolGroup.setItemCaption(Protocol.DDI_HTTP, "Direct Device Interface (HTTP poll)");
        protocolGroup.setNullSelectionAllowed(false);
        protocolGroup.select(Protocol.DMF_AMQP);
        protocolGroup.addValueChangeListener(event -> {
            pollDelayTextField.setVisible(!pollDelayTextField.isVisible());
            pollUrlTextField.setVisible(!pollUrlTextField.isVisible());
            gatewayTokenTextField.setVisible(!gatewayTokenTextField.isVisible());
        });
        return protocolGroup;
    }

    private Button createOkButton(final GenerateDialogCallback callback) {

        final Button buttonOk = new Button("generate");
        buttonOk.setImmediate(true);
        buttonOk.setIcon(FontAwesome.GEARS);
        buttonOk.addClickListener(event -> {
            try {
                callback.okButton(namePrefixTextField.getValue(), tenantTextField.getValue(),
                        Integer.valueOf(amountTextField.getValue().replace(".", "").replace(",", "")),
                        Integer.valueOf(pollDelayTextField.getValue().replace(".", "")),
                        new URL(pollUrlTextField.getValue()), gatewayTokenTextField.getValue(),
                        (Protocol) protocolGroup.getValue());
            } catch (final NumberFormatException e) {
                LOGGER.info(e.getMessage(), e);
            } catch (final MalformedURLException e) {
                LOGGER.info(e.getMessage(), e);
            }
            GenerateDialog.this.close();
        });
        return buttonOk;
    }

    private TextField createRequiredTextfield(final String caption, final String value, final Resource icon,
            final boolean required, final Validator validator) {
        final TextField textField = new TextField(caption, value);
        textField.setIcon(icon);
        textField.setRequired(required);
        textField.addValidator(validator);
        return textField;

    }

    private TextField createRequiredTextfield(final String caption, final Property dataSource, final Resource icon,
            final boolean required, final Validator validator) {
        final TextField textField = new TextField(caption, dataSource);
        textField.setIcon(icon);
        textField.setRequired(required);
        textField.addValidator(validator);
        return textField;

    }

}
