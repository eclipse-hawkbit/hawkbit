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

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.ObjectProperty;
import com.vaadin.data.validator.NullValidator;
import com.vaadin.data.validator.RangeValidator;
import com.vaadin.data.validator.RegexpValidator;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
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

        final TextField tf1 = new TextField("name prefix", "dmfSimulated");
        tf1.setIcon(FontAwesome.INFO);
        tf1.setRequired(true);
        tf1.addValidator(new NullValidator("Must be given", false));

        final TextField tf2 = new TextField("amount", new ObjectProperty<Integer>(10));
        tf2.setIcon(FontAwesome.GEAR);
        tf2.setRequired(true);
        tf2.addValidator(new RangeValidator<Integer>("Must be between 1 and 30000", Integer.class, 1, 30000));

        final TextField tf3 = new TextField("tenant", "default");
        tf3.setIcon(FontAwesome.USER);
        tf3.setRequired(true);
        tf3.addValidator(new NullValidator("Must be given", false));

        final TextField tf4 = new TextField("poll delay (sec)", new ObjectProperty<Integer>(10));
        tf4.setIcon(FontAwesome.CLOCK_O);
        tf4.setRequired(true);
        tf4.setVisible(false);
        tf4.addValidator(new RangeValidator<Integer>("Must be between 1 and 60", Integer.class, 1, 60));

        final TextField tf5 = new TextField("base poll URL endpoint", "http://localhost:8080");
        tf5.setColumns(50);
        tf5.setIcon(FontAwesome.FLAG_O);
        tf5.setRequired(true);
        tf5.setVisible(false);
        tf5.addValidator(new RegexpValidator(
                "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]", "is not an URL"));

        final TextField tf6 = new TextField("gateway token", "");
        tf6.setColumns(50);
        tf6.setIcon(FontAwesome.FLAG_O);
        tf6.setRequired(true);
        tf6.setVisible(false);

        final OptionGroup protocolGroup = new OptionGroup("Simulated Device Protocol");
        protocolGroup.addItem(Protocol.DMF_AMQP);
        protocolGroup.addItem(Protocol.DDI_HTTP);
        protocolGroup.setItemCaption(Protocol.DMF_AMQP, "Device Management Federation API (AMQP push)");
        protocolGroup.setItemCaption(Protocol.DDI_HTTP, "Direct Device Interface (HTTP poll)");
        protocolGroup.setNullSelectionAllowed(false);
        protocolGroup.select(Protocol.DMF_AMQP);
        protocolGroup.addValueChangeListener(new ValueChangeListener() {
            private static final long serialVersionUID = 1L;

            @Override
            public void valueChange(final ValueChangeEvent event) {
                if (event.getProperty().getValue().equals(Protocol.DDI_HTTP)) {
                    tf4.setVisible(true);
                    tf5.setVisible(true);
                    tf6.setVisible(true);
                } else {
                    tf4.setVisible(false);
                    tf5.setVisible(false);
                    tf6.setVisible(false);
                }
            }
        });

        final Button buttonOk = new Button("generate");
        buttonOk.setImmediate(true);
        buttonOk.setIcon(FontAwesome.GEARS);
        buttonOk.addClickListener(new ClickListener() {
            private static final long serialVersionUID = 1L;

            @Override
            public void buttonClick(final ClickEvent event) {
                try {
                    callback.okButton(tf1.getValue(), tf3.getValue(), Integer.valueOf(tf2.getValue().replace(".", "")),
                            Integer.valueOf(tf4.getValue().replace(".", "")), new URL(tf5.getValue()), tf6.getValue(),
                            (Protocol) protocolGroup.getValue());
                } catch (final NumberFormatException e) {
                    LOGGER.info(e.getMessage(), e);
                } catch (final MalformedURLException e) {
                    LOGGER.info(e.getMessage(), e);
                }
                GenerateDialog.this.close();
            }
        });

        tf1.addValueChangeListener(event -> checkValid(tf1, tf2, tf3, tf4, buttonOk));
        tf2.addValueChangeListener(event -> checkValid(tf1, tf2, tf3, tf4, buttonOk));
        tf3.addValueChangeListener(event -> checkValid(tf1, tf2, tf3, tf4, buttonOk));

        formLayout.addComponent(tf1);
        formLayout.addComponent(tf2);
        formLayout.addComponent(tf3);
        formLayout.addComponent(protocolGroup);
        formLayout.addComponent(tf4);
        formLayout.addComponent(tf5);
        formLayout.addComponent(tf6);
        formLayout.addComponent(buttonOk);

        setCaption("Simulate Devices");
        setContent(formLayout);
        setResizable(false);
        center();
    }

    private void checkValid(final TextField tf1, final TextField tf2, final TextField tf3, final TextField tf4,
            final Button buttonOk) {
        if (tf1.isValid() && tf2.isValid() && tf3.isValid() && tf4.isValid()) {
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
}
