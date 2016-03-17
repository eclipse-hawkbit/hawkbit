/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.simulator.ui;

import java.net.URL;
import java.util.List;
import java.util.Locale;

import org.eclipse.hawkbit.simulator.AbstractSimulatedDevice;
import org.eclipse.hawkbit.simulator.AbstractSimulatedDevice.Protocol;
import org.eclipse.hawkbit.simulator.AbstractSimulatedDevice.ResponseStatus;
import org.eclipse.hawkbit.simulator.AbstractSimulatedDevice.Status;
import org.eclipse.hawkbit.simulator.DeviceSimulatorRepository;
import org.eclipse.hawkbit.simulator.SimulatedDeviceFactory;
import org.eclipse.hawkbit.simulator.amqp.SpSenderService;
import org.eclipse.hawkbit.simulator.event.InitUpdate;
import org.eclipse.hawkbit.simulator.event.NextPollCounterUpdate;
import org.eclipse.hawkbit.simulator.event.ProgressUpdate;
import org.eclipse.hawkbit.simulator.ui.GenerateDialog.GenerateDialogCallback;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
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
 * @author Michael Hirsch
 *
 */
@SpringView(name = "")
public class SimulatorView extends VerticalLayout implements View {

    private static final long serialVersionUID = 1L;

    @Autowired
    private transient SpSenderService spSenderService;
    @Autowired
    private transient DeviceSimulatorRepository repository;
    @Autowired
    private transient SimulatedDeviceFactory deviceFactory;

    @Autowired
    private transient EventBus eventbus;

    private final Label caption = new Label("DMF/DDI Simulated Devices");
    private final HorizontalLayout toolbar = new HorizontalLayout();
    private final Grid grid = new Grid();
    private final ComboBox responseComboBox = new ComboBox("",
            Lists.newArrayList(ResponseStatus.SUCCESSFUL, ResponseStatus.ERROR));

    private BeanContainer<String, AbstractSimulatedDevice> beanContainer;

    @Override
    public void enter(final ViewChangeEvent event) {
        eventbus.register(this);
        setSizeFull();

        // caption
        caption.addStyleName("h2");

        // toolbar
        createToolbar();

        beanContainer = new BeanContainer<>(AbstractSimulatedDevice.class);
        beanContainer.setBeanIdProperty("id");

        grid.setSizeFull();
        grid.setCellStyleGenerator(new CellStyleGenerator() {

            private static final long serialVersionUID = 1L;

            @Override
            public String getStyle(final CellReference cellReference) {
                return cellReference.getPropertyId().equals("status") ? "centeralign" : null;
            }
        });

        grid.setSelectionMode(SelectionMode.NONE);
        grid.setContainerDataSource(beanContainer);
        grid.appendHeaderRow().getCell("responseStatus").setComponent(responseComboBox);
        grid.setColumnOrder("id", "status", "swversion", "progress", "tenant", "protocol", "responseStatus",
                "nextPollCounterSec");
        // header widths
        grid.getColumn("status").setMaximumWidth(80);
        grid.getColumn("protocol").setMaximumWidth(180);
        grid.getColumn("responseStatus").setMaximumWidth(240);
        grid.getColumn("nextPollCounterSec").setMaximumWidth(210);

        grid.getColumn("nextPollCounterSec").setHeaderCaption("Next Poll in (sec)");
        grid.getColumn("swversion").setHeaderCaption("SW Version");
        grid.getColumn("responseStatus").setHeaderCaption("Response Update Status");
        grid.getColumn("progress").setRenderer(new ProgressBarRenderer());
        grid.getColumn("protocol").setConverter(createProtocolConverter());
        grid.getColumn("status").setRenderer(new HtmlRenderer(), createStatusConverter());
        grid.removeColumn("tenant");

        // grid combobox
        responseComboBox.setItemIcon(ResponseStatus.SUCCESSFUL, FontAwesome.CHECK_CIRCLE);
        responseComboBox.setItemIcon(ResponseStatus.ERROR, FontAwesome.EXCLAMATION_CIRCLE);
        responseComboBox.setNullSelectionAllowed(false);
        responseComboBox.setValue(ResponseStatus.SUCCESSFUL);
        responseComboBox.addValueChangeListener(valueChangeEvent -> {
            beanContainer.getItemIds().forEach(itemId -> beanContainer.getItem(itemId).getItemProperty("responseStatus")
                    .setValue(valueChangeEvent.getProperty().getValue()));
        });

        // add all components
        addComponent(caption);
        addComponent(toolbar);
        addComponent(grid);

        setExpandRatio(grid, 1.0F);

        // load beans
        repository.getAll().forEach(device -> beanContainer.addBean(device));
    }

    @Override
    public void detach() {
        super.detach();
        eventbus.unregister(this);
    }

    @Subscribe
    public void pollCounterUpdate(final NextPollCounterUpdate update) {
        final List<AbstractSimulatedDevice> devices = update.getDevices();
        this.getUI().access(new Runnable() {
            @Override
            public void run() {
                devices.forEach(device -> {
                    final BeanItem<AbstractSimulatedDevice> item = beanContainer.getItem(device.getId());
                    if (item != null) {
                        item.getItemProperty("nextPollCounterSec").setValue(device.getNextPollCounterSec());
                    }
                });
            }
        });
    }

    /**
     * Method to retrieve {@link InitUpdate} events from the event bus.
     * 
     * @param update
     *            the update event posted on the event bus
     */
    @Subscribe
    public void initUpdate(final InitUpdate update) {
        final AbstractSimulatedDevice device = update.getDevice();
        this.getUI().access(new Runnable() {
            @Override
            public void run() {
                final BeanItem<AbstractSimulatedDevice> item = beanContainer.getItem(device.getId());
                if (item != null) {
                    item.getItemProperty("progress").setValue(device.getProgress());
                    item.getItemProperty("status").setValue(Status.PEDNING);
                    item.getItemProperty("swversion").setValue(device.getSwversion());
                }

            }
        });
    }

    /**
     * Method to retrieve {@link ProgressUpdate} events from the event bus.
     * 
     * @param update
     *            the update event posted on the event bus
     */
    @Subscribe
    public void progessUpdate(final ProgressUpdate update) {
        final AbstractSimulatedDevice device = update.getDevice();
        this.getUI().access(new Runnable() {
            @Override
            public void run() {
                final BeanItem<AbstractSimulatedDevice> item = beanContainer.getItem(device.getId());
                if (item != null) {
                    item.getItemProperty("progress").setValue(device.getProgress());
                    if (device.getProgress() >= 1) {
                        switch (device.getResponseStatus()) {
                        case SUCCESSFUL:
                            item.getItemProperty("status").setValue(Status.FINISH);
                            break;
                        case ERROR:
                            item.getItemProperty("status").setValue(Status.ERROR);
                            break;
                        default:
                            item.getItemProperty("status").setValue(Status.UNKNWON);
                        }
                    } else {
                        item.getItemProperty("status").setValue(Status.PEDNING);
                    }
                }

            }
        });
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
        UI.getCurrent().addWindow(new GenerateDialog(new GenerateDialogCallback() {
            @Override
            public void okButton(final String namePrefix, final String tenant, final int amount, final int pollDelay,
                    final URL basePollUrl, final String gatewayToken, final Protocol protocol) {
                for (int index = 0; index < amount; index++) {
                    final String deviceId = namePrefix + index;
                    beanContainer.addBean(repository.add(deviceFactory.createSimulatedDevice(deviceId,
                            tenant.toLowerCase(), protocol, pollDelay, basePollUrl, gatewayToken)));
                    spSenderService.createOrUpdateThing(tenant, deviceId);
                }
            }
        }));
    }

    private Converter<String, Protocol> createProtocolConverter() {

        return new Converter<String, Protocol>() {

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
        };

    }

    private Converter<String, Status> createStatusConverter() {
        return new Converter<String, Status>() {
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
                            + ";\"color\":\"gray\";\">&#x"
                            + Integer.toHexString(FontAwesome.QUESTION_CIRCLE.getCodepoint()) + ";</span>";
                case PEDNING:
                    return "<span class=\"v-icon yellowicon\" style=\"font-family: " + FontAwesome.FONT_FAMILY
                            + ";\"color\":\"yellow\";\">&#x" + Integer.toHexString(FontAwesome.REFRESH.getCodepoint())
                            + ";</span>";
                case FINISH:
                    return "<span class=\"v-icon greenicon\" style=\"font-family: " + FontAwesome.FONT_FAMILY
                            + ";\"color\":\"green\";\">&#x"
                            + Integer.toHexString(FontAwesome.CHECK_CIRCLE.getCodepoint()) + ";</span>";
                case ERROR:
                    return "<span class=\"v-icon redicon\" style=\"font-family: " + FontAwesome.FONT_FAMILY
                            + ";\"color\":\"red\";\">&#x"
                            + Integer.toHexString(FontAwesome.EXCLAMATION_CIRCLE.getCodepoint()) + ";</span>";
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
        };
    }

}
