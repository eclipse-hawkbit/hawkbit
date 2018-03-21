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
import java.util.Locale;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.MaintenanceScheduleHelper;
import org.eclipse.hawkbit.ui.common.builder.LabelBuilder;
import org.eclipse.hawkbit.ui.common.builder.TextFieldBuilder;
import org.eclipse.hawkbit.ui.utils.SPDateTimeUtil;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.util.StringUtils;

import com.cronutils.descriptor.CronDescriptor;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import com.vaadin.data.Validator;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
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

    private static final String CRON_VALIDATION_ERROR = "message.maintenancewindow.schedule.validation.error";

    private TextField schedule;
    private TextField duration;
    private ComboBox timeZone;
    private Label scheduleTranslator;

    /**
     * Constructor for the control to specify the maintenance schedule.
     *
     * @param i18n
     *            (@link VaadinMessageSource} to get the localized resource
     *            strings.
     */
    public MaintenanceWindowLayout(final VaadinMessageSource i18n) {

        this.i18n = i18n;

        createMaintenanceScheduleControl();
        createMaintenanceDurationControl();
        createMaintenanceTimeZoneControl();
        createMaintenanceScheduleTranslatorControl();

        final HorizontalLayout controlContainer = new HorizontalLayout();
        controlContainer.addComponent(schedule);
        controlContainer.addComponent(duration);
        controlContainer.addComponent(timeZone);
        addComponent(controlContainer);

        addComponent(scheduleTranslator);

        setStyleName("dist-window-maintenance-window-layout");
    }

    /**
     * Text field to specify the schedule.
     */
    private void createMaintenanceScheduleControl() {
        schedule = new TextFieldBuilder().id(UIComponentIdProvider.MAINTENANCE_WINDOW_SCHEDULE_ID)
                .caption(i18n.getMessage("caption.maintenancewindow.schedule")).immediate(true)
                .validator(new CronValidator()).prompt("0 0 3 ? * 6").required(true).buildTextComponent();
        schedule.addTextChangeListener(new CronTranslationListener());
    }

    /**
     * Validates if the maintenance schedule is a valid cron expression.
     */
    private class CronValidator implements Validator {
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
                throw new InvalidValueException(i18n.getMessage(CRON_VALIDATION_ERROR));
            }
        }
    }

    /**
     * Used for cron expression translation.
     */
    private class CronTranslationListener implements TextChangeListener {
        private static final long serialVersionUID = 1L;

        private final transient CronParser cronParser;
        private final transient CronDescriptor cronDescriptor;

        public CronTranslationListener() {
            final CronDefinition cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ);
            cronParser = new CronParser(cronDefinition);
            cronDescriptor = CronDescriptor.instance(Locale.UK);
        }

        @Override
        public void textChange(final TextChangeEvent event) {
            scheduleTranslator.setValue(translateCron(event.getText()));
        }

        private String translateCron(final String cronExpression) {
            try {
                return cronDescriptor.describe(cronParser.parse(cronExpression));
            } catch (final IllegalArgumentException ex) {
                return i18n.getMessage(CRON_VALIDATION_ERROR);
            }
        }
    }

    /**
     * Text field to specify the duration.
     */
    private void createMaintenanceDurationControl() {
        duration = new TextFieldBuilder().id(UIComponentIdProvider.MAINTENANCE_WINDOW_DURATION_ID)
                .caption(i18n.getMessage("caption.maintenancewindow.duration")).immediate(true)
                .validator(new DurationValidator()).prompt("hh:mm:ss").required(true).buildTextComponent();
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
                throw new InvalidValueException(i18n.getMessage("message.maintenancewindow.duration.validation.error"));
            }
        }
    }

    /**
     * Combo box to pick the time zone offset.
     */
    private void createMaintenanceTimeZoneControl() {
        // ComboBoxBuilder cannot be used here, because Builder do
        // 'comboBox.setItemCaptionPropertyId(SPUILabelDefinitions.VAR_NAME);'
        // which interferes our code: 'timeZone.addItems(getAllTimeZones());'
        timeZone = new ComboBox();
        timeZone.setId(UIComponentIdProvider.MAINTENANCE_WINDOW_TIME_ZONE_ID);
        timeZone.setCaption(i18n.getMessage("caption.maintenancewindow.timezone"));
        timeZone.addItems(getAllTimeZones());
        timeZone.setValue(getClientTimeZone());
        timeZone.addStyleName(ValoTheme.COMBOBOX_SMALL);
        timeZone.setTextInputAllowed(false);
        timeZone.setNullSelectionAllowed(false);
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
     * Get time zone of the browser client to be used as default.
     */
    private static String getClientTimeZone() {
        return ZonedDateTime.now(ZoneId.of(SPDateTimeUtil.getBrowserTimeZone().getID())).getOffset().getId()
                .replaceAll("Z", "+00:00");
    }

    /**
     * Label to translate the cron schedule to human readable format.
     */
    private void createMaintenanceScheduleTranslatorControl() {
        scheduleTranslator = new LabelBuilder().name(i18n.getMessage(CRON_VALIDATION_ERROR)).buildLabel();
        scheduleTranslator.addStyleName(ValoTheme.LABEL_TINY);
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

    public void clearAllControls() {
        schedule.setValue("");
        duration.setValue("");
        timeZone.setValue(getClientTimeZone());
    }
}
