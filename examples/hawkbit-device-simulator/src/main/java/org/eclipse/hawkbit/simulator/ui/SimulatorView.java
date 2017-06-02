/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.simulator.ui;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

import org.eclipse.hawkbit.simulator.AbstractSimulatedDevice;
import org.eclipse.hawkbit.simulator.AbstractSimulatedDevice.Protocol;
import org.eclipse.hawkbit.simulator.AbstractSimulatedDevice.Status;
import org.eclipse.hawkbit.simulator.DeviceSimulatorRepository;
import org.eclipse.hawkbit.simulator.SimulatedDeviceFactory;
import org.eclipse.hawkbit.simulator.UpdateStatus.ResponseStatus;
import org.eclipse.hawkbit.simulator.amqp.AmqpProperties;
import org.eclipse.hawkbit.simulator.event.InitUpdate;
import org.eclipse.hawkbit.simulator.event.NextPollCounterUpdate;
import org.eclipse.hawkbit.simulator.event.ProgressUpdate;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.data.util.BeanContainer;
import com.vaadin.data.util.BeanItem;
import com.vaadin.data.util.converter.Converter;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.FontAwesome;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.CellReference;
import com.vaadin.ui.Grid.CellStyleGenerator;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.renderers.HtmlRenderer;
import com.vaadin.ui.renderers.ProgressBarRenderer;

/**
 * Vaadin view which allows to generate devices through the DMF API and show the
 * current simulated devices in a grid with their current status and update
 * progress.
 *
 */
@SpringView(name = "")
// The inheritance comes from Vaadin
@SuppressWarnings("squid:MaximumInheritanceDepth")
public class SimulatorView extends VerticalLayout implements View {

    private static final String HTML_SPAN = ";</span>";

    private static final String NEXT_POLL_COUNTER_SEC_COL = "nextPollCounterSec";

    private static final String RESPONSE_STATUS_COL = "updateStatus";

    private static final String PROTOCOL_COL = "protocol";

    private static final String TENANT_COL = "tenant";

    private static final String PROGRESS_COL = "progress";

    private static final String SWVERSION_COL = "swversion";

    private static final String STATUS_COL = "status";

    private static final String ID_COL = "id";

    private static final long serialVersionUID = 1L;

    @Autowired
    private transient DeviceSimulatorRepository repository;

    @Autowired
    private transient SimulatedDeviceFactory deviceFactory;

    @Autowired
    private transient EventBus eventbus;

    @Autowired
    private transient AmqpProperties amqpProperties;

    private final Label caption = new Label("DMF/DDI Simulated Devices");
    private final HorizontalLayout toolbar = new HorizontalLayout();
    private final Grid grid = new Grid();
    private final ComboBox responseComboBox = new ComboBox("",
            Arrays.asList(ResponseStatus.SUCCESSFUL, ResponseStatus.ERROR));

    private BeanContainer<String, AbstractSimulatedDevice> beanContainer;

    @SuppressWarnings("unchecked")
    @Override
    public void enter(final ViewChangeEvent event) {
        eventbus.register(this);
        setSizeFull();

        // caption
        caption.addStyleName("h2");

        // toolbar
        createToolbar();

        beanContainer = new BeanContainer<>(AbstractSimulatedDevice.class);
        beanContainer.setBeanIdProperty(ID_COL);

        grid.setSizeFull();
        grid.setCellStyleGenerator(new CellStyleGenerator() {

            private static final long serialVersionUID = 1L;

            @Override
            public String getStyle(final CellReference cellReference) {
                return cellReference.getPropertyId().equals(STATUS_COL) ? "centeralign" : null;
            }
        });

        grid.setSelectionMode(SelectionMode.NONE);
        grid.setContainerDataSource(beanContainer);
        grid.appendHeaderRow().getCell(RESPONSE_STATUS_COL).setComponent(responseComboBox);
        grid.setColumnOrder(ID_COL, STATUS_COL, SWVERSION_COL, PROGRESS_COL, TENANT_COL, PROTOCOL_COL,
                RESPONSE_STATUS_COL, NEXT_POLL_COUNTER_SEC_COL);
        // header widths
        grid.getColumn(STATUS_COL).setMaximumWidth(80);
        grid.getColumn(PROTOCOL_COL).setMaximumWidth(180);
        grid.getColumn(RESPONSE_STATUS_COL).setMaximumWidth(240);
        grid.getColumn(NEXT_POLL_COUNTER_SEC_COL).setMaximumWidth(210);

        grid.getColumn(NEXT_POLL_COUNTER_SEC_COL).setHeaderCaption("Next Poll in (sec)");
        grid.getColumn(SWVERSION_COL).setHeaderCaption("SW Version");
        grid.getColumn(RESPONSE_STATUS_COL).setHeaderCaption("Response Update Status");
        grid.getColumn(PROGRESS_COL).setRenderer(new ProgressBarRenderer());
        grid.getColumn(PROTOCOL_COL).setConverter(createProtocolConverter());
        grid.getColumn(STATUS_COL).setRenderer(new HtmlRenderer(), createStatusConverter());
        grid.removeColumn(TENANT_COL);

        // grid combobox
        responseComboBox.setItemIcon(ResponseStatus.SUCCESSFUL, FontAwesome.CHECK_CIRCLE);
        responseComboBox.setItemIcon(ResponseStatus.ERROR, FontAwesome.EXCLAMATION_CIRCLE);
        responseComboBox.setNullSelectionAllowed(false);
        responseComboBox.setValue(ResponseStatus.SUCCESSFUL);
        responseComboBox.addValueChangeListener(
                valueChangeEvent -> beanContainer.getItemIds().forEach(itemId -> beanContainer.getItem(itemId)
                        .getItemProperty(RESPONSE_STATUS_COL).setValue(valueChangeEvent.getProperty().getValue())));

        // add all components
        addComponent(caption);
        addComponent(toolbar);
        addComponent(grid);

        setExpandRatio(grid, 1.0F);

        // load beans
        repository.getAll().forEach(beanContainer::addBean);
    }

    @Override
    public void detach() {
        super.detach();
        eventbus.unregister(this);
    }

    @SuppressWarnings("unchecked")
    @Subscribe
    public void pollCounterUpdate(final NextPollCounterUpdate update) {
        final Collection<AbstractSimulatedDevice> devices = update.getDevices();
        this.getUI().access(() -> devices.forEach(device -> {
            final BeanItem<AbstractSimulatedDevice> item = beanContainer.getItem(device.getId());
            if (item != null) {
                item.getItemProperty(NEXT_POLL_COUNTER_SEC_COL).setValue(device.getNextPollCounterSec());
            }
        }));
    }

    /**
     * Method to retrieve {@link InitUpdate} events from the event bus.
     * 
     * @param update
     *            the update event posted on the event bus
     */
    @SuppressWarnings("unchecked")
    @Subscribe
    public void initUpdate(final InitUpdate update) {
        final AbstractSimulatedDevice device = update.getDevice();
        this.getUI().access(() -> {
            final BeanItem<AbstractSimulatedDevice> item = beanContainer.getItem(device.getId());
            if (item == null) {
                return;
            }

            item.getItemProperty(PROGRESS_COL).setValue(device.getProgress());
            item.getItemProperty(STATUS_COL).setValue(Status.PEDNING);
            item.getItemProperty(SWVERSION_COL).setValue(device.getSwversion());
        });
    }

    /**
     * Method to retrieve {@link ProgressUpdate} events from the event bus.
     * 
     * @param update
     *            the update event posted on the event bus
     */
    @SuppressWarnings("unchecked")
    @Subscribe
    public void progessUpdate(final ProgressUpdate update) {
        final AbstractSimulatedDevice device = update.getDevice();
        this.getUI().access(() -> {
            final BeanItem<AbstractSimulatedDevice> item = beanContainer.getItem(device.getId());
            if (item != null) {
                item.getItemProperty(PROGRESS_COL).setValue(device.getProgress());
                setStatusColumn(device, item);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void setStatusColumn(final AbstractSimulatedDevice device, final BeanItem<AbstractSimulatedDevice> item) {
        if (device.getProgress() >= 1) {
            switch (device.getUpdateStatus().getResponseStatus()) {
            case SUCCESSFUL:
                item.getItemProperty(STATUS_COL).setValue(Status.FINISH);
                break;
            case ERROR:
                item.getItemProperty(STATUS_COL).setValue(Status.ERROR);
                break;
            default:
                item.getItemProperty(STATUS_COL).setValue(Status.UNKNWON);
            }
        } else {
            item.getItemProperty(STATUS_COL).setValue(Status.PEDNING);
        }
    }

    private void createToolbar() {
        final Button createDevicesButton = new Button("generate...");
        createDevicesButton.setIcon(FontAwesome.GEARS);
        createDevicesButton.addClickListener(event -> openGenerateDialog());

        final Button clearDevicesButton = new Button("clear");
        clearDevicesButton.setIcon(FontAwesome.ERASER);
        clearDevicesButton.addClickListener(event -> clearSimulatedDevices());

        toolbar.addComponent(createDevicesButton);
        toolbar.addComponent(clearDevicesButton);
        toolbar.setSpacing(true);
    }

    private void clearSimulatedDevices() {
        repository.clear();
        beanContainer.removeAllItems();
    }

    private void openGenerateDialog() {
        UI.getCurrent().addWindow(
                new GenerateDialog((namePrefix, tenant, amount, pollDelay, basePollUrl, gatewayToken, protocol) -> {
                    for (int index = 0; index < amount; index++) {
                        final String deviceId = namePrefix + index;
                        beanContainer
                                .addBean(repository.add(deviceFactory.createSimulatedDeviceWithImmediatePoll(deviceId,
                                        tenant.toLowerCase(), protocol, pollDelay, basePollUrl, gatewayToken)));
                    }
                }, amqpProperties.isEnabled()));
    }

    private ProtocolConverter createProtocolConverter() {
        return new ProtocolConverter();
    }

    private StatusConverter createStatusConverter() {
        return new StatusConverter();
    }

    public static final class ProtocolConverter implements Converter<String, Protocol> {
        private static final long serialVersionUID = 1L;

        @Override
        public Protocol convertToModel(final String value, final Class<? extends Protocol> targetType,
                final Locale locale) {
            return null;
        }

        @Override
        public String convertToPresentation(final Protocol value, final Class<? extends String> targetType,
                final Locale locale) {
            switch (value) {
            case DDI_HTTP:
                return "DDI API (http)";
            case DMF_AMQP:
                return "DMF API (amqp)";
            default:
                return "unknown";
            }
        }

        @Override
        public Class<Protocol> getModelType() {
            return Protocol.class;
        }

        @Override
        public Class<String> getPresentationType() {
            return String.class;
        }
    }

    private static final class StatusConverter implements Converter<String, Status> {
        private static final long serialVersionUID = 1L;

        @Override
        public Status convertToModel(final String value, final Class<? extends Status> targetType,
                final Locale locale) {
            return null;
        }

        @Override
        public String convertToPresentation(final Status value, final Class<? extends String> targetType,
                final Locale locale) {
            switch (value) {
            case UNKNWON:
                return "<span class=\"v-icon grayicon\" style=\"font-family: " + FontAwesome.FONT_FAMILY
                        + ";\"color\":\"gray\";\">&#x" + Integer.toHexString(FontAwesome.QUESTION_CIRCLE.getCodepoint())
                        + HTML_SPAN;
            case PEDNING:
                return "<span class=\"v-icon yellowicon\" style=\"font-family: " + FontAwesome.FONT_FAMILY
                        + ";\"color\":\"yellow\";\">&#x" + Integer.toHexString(FontAwesome.REFRESH.getCodepoint())
                        + HTML_SPAN;
            case FINISH:
                return "<span class=\"v-icon greenicon\" style=\"font-family: " + FontAwesome.FONT_FAMILY
                        + ";\"color\":\"green\";\">&#x" + Integer.toHexString(FontAwesome.CHECK_CIRCLE.getCodepoint())
                        + HTML_SPAN;
            case ERROR:
                return "<span class=\"v-icon redicon\" style=\"font-family: " + FontAwesome.FONT_FAMILY
                        + ";\"color\":\"red\";\">&#x"
                        + Integer.toHexString(FontAwesome.EXCLAMATION_CIRCLE.getCodepoint()) + HTML_SPAN;
            default:
                throw new IllegalStateException("unknown value");
            }
        }

        @Override
        public Class<Status> getModelType() {
            return Status.class;
        }

        @Override
        public Class<String> getPresentationType() {
            return String.class;
        }
    }

}
