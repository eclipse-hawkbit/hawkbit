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
import org.eclipse.hawkbit.ui.common.builder.ComboBoxBuilder;
import org.eclipse.hawkbit.ui.common.builder.TextFieldBuilder;
import org.eclipse.hawkbit.ui.utils.SPDateTimeUtil;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.vaadin.data.Validator;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextField;

/**
 * {@link MaintenanceWindowLayout} defines UI layout that is used to specify the
 * maintenance schedule while assigning distribution set(s) to the target(s).
 */
public class MaintenanceWindowLayout extends HorizontalLayout {

    private static final long serialVersionUID = 722511089585562455L;

    private final VaadinMessageSource i18n;

    private final UINotification uiNotification;

    private static final Logger LOGGER = LoggerFactory.getLogger(MaintenanceWindowLayout.class);

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
    public MaintenanceWindowLayout(final VaadinMessageSource i18n, final UINotification uiNotification) {

        this.i18n = i18n;
        this.uiNotification = uiNotification;

        createMaintenanceScheduleControl();
        createMaintenanceDurationControl();
        createMaintenanceTimeZoneControl();

        addComponent(schedule);
        addComponent(duration);
        addComponent(timeZone);

        setStyleName("dist-window-maintenance-window-layout");
    }

    /**
     * Validates if the maintenance schedule is a valid cron expression.
     */
    private class CronValidation implements Validator {
        private static final long serialVersionUID = 1L;

        @Override
        public void validate(final Object value) {
            try {

                final String expr = (String) value;
                if (!expr.isEmpty()) {
                    MaintenanceScheduleHelper.validateMaintenanceSchedule((String) value, "00:00:00",
                            getClientTimeZone());
                }
            } catch (final IllegalArgumentException e) {
                uiNotification
                        .displayValidationError(i18n.getMessage("message.maintenancewindow.schedule.validation.error"));
            }
        }
    }

    /**
     * Validates if the duration is specified in expected format.
     */
    private class DurationValidator implements Validator {
        private static final long serialVersionUID = 1L;

        @Override
        public void validate(final Object value) {
            try {
                final String expression = (String) value;
                if (StringUtils.hasText(expression)) {
                    MaintenanceScheduleHelper.convertToISODuration(expression);
                }
            } catch (final DateTimeParseException e) {
                uiNotification
                        .displayValidationError(i18n.getMessage("message.maintenancewindow.duration.validation.error"));
            }
        }
    }

    /**
     * Text field to specify the schedule.
     */
    private void createMaintenanceScheduleControl() {
        schedule = new TextFieldBuilder().id(UIComponentIdProvider.MAINTENANCE_WINDOW_SCHEDULE_ID)
                .caption(i18n.getMessage("caption.maintenancewindow.schedule")).immediate(true)
                .validator(new CronValidation()).prompt("Cron Expression").buildTextComponent();
    }

    /**
     * Text field to specify the duration.
     */
    private void createMaintenanceDurationControl() {
        duration = new TextFieldBuilder().id(UIComponentIdProvider.MAINTENANCE_WINDOW_DURATION_ID)
                .caption(i18n.getMessage("caption.maintenancewindow.duration")).immediate(true)
                .validator(new DurationValidator()).prompt("hh:mm:ss").buildTextComponent();
    }

    /**
     * Combo box to pick the time zone offset.
     */
    private void createMaintenanceTimeZoneControl() {
        timeZone = new ComboBoxBuilder().setId(UIComponentIdProvider.MAINTENANCE_WINDOW_TIME_ZONE_ID)
                .setCaption(i18n.getMessage("caption.maintenancewindow.timezone")).buildCombBox();
        timeZone.addItems(getAllTimeZones());
        timeZone.setTextInputAllowed(false);
        timeZone.setValue(getClientTimeZone());
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
