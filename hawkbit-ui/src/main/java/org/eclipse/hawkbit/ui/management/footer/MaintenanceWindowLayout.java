/**
 * Copyright (c) Siemens AG, 2018
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.hawkbit.ui.management.footer;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.MaintenanceScheduleHelper;
import org.eclipse.hawkbit.ui.utils.SPDateTimeUtil;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.util.StringUtils;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.Validator;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

/**
 * {@link MaintenanceWindowLayout} defines UI layout that is used to specify the
 * maintenance schedule while assigning distribution set(s) to the target(s).
 */
public class MaintenanceWindowLayout extends VerticalLayout {

    private static final long serialVersionUID = 722511089585562455L;

    private final VaadinMessageSource i18n;

    private CheckBox maintenanceWindowSelection;
    private TextField schedule;
    private TextField duration;
    private ComboBox timeZone;

    /**
     * Constructor for the control to specify the maintenance schedule.
     *
     * @param i18n
     *            (@link VaadinMessageSource} to get the localized resource
     *            strings.
     */
    public MaintenanceWindowLayout(final VaadinMessageSource i18n) {

        HorizontalLayout optionContainer;
        HorizontalLayout controlContainer;

        this.i18n = i18n;

        optionContainer = new HorizontalLayout();
        controlContainer = new HorizontalLayout();
        addComponent(optionContainer);
        addComponent(controlContainer);

        createMaintenanceWindowOption();
        createMaintenanceScheduleControl();
        createMaintenanceDurationControl();
        createMaintenanceTimeZoneControl();

        optionContainer.addComponent(maintenanceWindowSelection);
        controlContainer.addComponent(schedule);
        controlContainer.addComponent(duration);
        controlContainer.addComponent(timeZone);

        addValueChangeListener();
        maintenanceWindowSelection.setValue(false);
        setStyleName("dist-window-maintenance-window-layout");
    }

    /**
     * Validates if the maintenance schedule is a valid cron expression.
     */
    class CronValidation implements Validator {
        private static final long serialVersionUID = 1L;

        @Override
        public void validate(final Object value) throws InvalidValueException {
            try {
                final String expr = (String) value;
                if (!expr.isEmpty()) {
                    MaintenanceScheduleHelper.validateMaintenanceSchedule((String) value, "00:00:00",
                            getClientTimeZone());
                }
            } catch (final IllegalArgumentException e) {
                Notification.show(e.getMessage());
                throw e;
            }
        }
    }

    /**
     * Validates if the duration is specified in expected format.
     */
    class DurationValidator implements Validator {
        private static final long serialVersionUID = 1L;

        @Override
        public void validate(final Object value) {
            try {
                final String expr = (String) value;
                if (!StringUtils.isEmpty(expr)) {
                    MaintenanceScheduleHelper.convertToISODuration((String) value);
                }
            } catch (final DateTimeParseException e) {
                Notification.show(e.getMessage());
                throw e;
            }
        }
    }

    /**
     * Create check box to enable or disable maintenance window.
     */
    private void createMaintenanceWindowOption() {
        maintenanceWindowSelection = new CheckBox(i18n.getMessage("caption.maintenancewindow.enable"));
        maintenanceWindowSelection.addStyleName(ValoTheme.CHECKBOX_SMALL);
    }

    /**
     * Text field to specify the schedule.
     */
    private void createMaintenanceScheduleControl() {
        schedule = new TextField();
        schedule.setCaption(i18n.getMessage("caption.maintenancewindow.schedule"));
        schedule.addValidator(new CronValidation());
        schedule.setEnabled(false);
        schedule.addStyleName(ValoTheme.TEXTFIELD_SMALL);
    }

    /**
     * Text field to specify the duration.
     */
    private void createMaintenanceDurationControl() {
        duration = new TextField();
        duration.setCaption(i18n.getMessage("caption.maintenancewindow.duration"));
        duration.addValidator(new DurationValidator());
        duration.setEnabled(false);
        schedule.addStyleName(ValoTheme.TEXTFIELD_SMALL);
    }

    /**
     * Combo box to pick the time zone offset.
     */
    private void createMaintenanceTimeZoneControl() {
        timeZone = new ComboBox();
        timeZone.setCaption(i18n.getMessage("caption.maintenancewindow.timezone"));

        timeZone.addItems(getAllTimeZones());
        timeZone.setTextInputAllowed(false);
        timeZone.setValue(getClientTimeZone());

        timeZone.setEnabled(false);
        timeZone.addStyleName(ValoTheme.COMBOBOX_SMALL);
    }

    /**
     * Get time zone of the browser client to be used as default.
     */
    private static String getClientTimeZone() {
        return ZonedDateTime.now(ZoneId.of(SPDateTimeUtil.getBrowserTimeZone().getID())).getOffset().getId()
                .replaceAll("Z", "+00:00");
    }

    /**
     * Get list of all time zone offsets supported.
     */
    private static List<String> getAllTimeZones() {
        final List<String> lst = ZoneId.getAvailableZoneIds().stream()
                .map(id -> ZonedDateTime.now(ZoneId.of(id)).getOffset().getId().replace("Z", "+00:00")).distinct()
                .collect(Collectors.toList());
        lst.sort(null);
        return lst;
    }

    /**
     * Create a listener to enable and disable maintenance schedule controls.
     */
    private void addValueChangeListener() {
        maintenanceWindowSelection.addValueChangeListener(new ValueChangeListener() {
            private static final long serialVersionUID = 1L;

            @Override
            public void valueChange(final ValueChangeEvent event) {
                schedule.setEnabled(maintenanceWindowSelection.getValue());
                schedule.setRequired(maintenanceWindowSelection.getValue());
                schedule.setValue("");

                duration.setEnabled(maintenanceWindowSelection.getValue());
                duration.setRequired(maintenanceWindowSelection.getValue());
                duration.setValue("");

                timeZone.setEnabled(maintenanceWindowSelection.getValue());
                timeZone.setRequired(maintenanceWindowSelection.getValue());
                timeZone.setValue(getClientTimeZone());
            }
        });
    }

    /**
     * Get whether the maintenance schedule option is enabled or not.
     *
     * @return boolean.
     */
    public boolean getMaintenanceOption() {
        return maintenanceWindowSelection.getValue();
    }

    /**
     * Get the cron expression for maintenance schedule.
     *
     * @return {@link String}.
     */
    public String getMaintenanceSchedule() {
        return schedule.getValue();
    }

    /**
     * Get the maintenance window duration.
     *
     * @return {@link String}.
     */
    public String getMaintenanceDuration() {
        return duration.getValue();
    }

    /**
     * Get the cron expression for maintenance window timezone.
     *
     * @return {@link String}.
     */

    public String getMaintenanceTimeZone() {
        return timeZone.getValue().toString();
    }
}
