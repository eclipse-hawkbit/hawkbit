/**
 * Copyright (c) Siemens AG, 2018
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.hawkbit.ui.management.miscs;

import org.eclipse.hawkbit.repository.MaintenanceScheduleHelper;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cronutils.descriptor.CronDescriptor;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

/**
 * {@link MaintenanceWindowLayout} defines UI layout that is used to specify the
 * maintenance schedule while assigning distribution set(s) to the target(s).
 */
public class MaintenanceWindowLayout extends VerticalLayout {
    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(MaintenanceWindowLayout.class);

    private final VaadinMessageSource i18n;
    private final TextField schedule;
    private final Label scheduleTranslator;
    private final transient CronDescriptor cronDescriptor;

    /**
     * Constructor for the control to specify the maintenance schedule.
     *
     * @param i18n
     *            (@link VaadinMessageSource} to get the localized resource
     *            strings.
     * @param schedule
     *          Schedule
     * @param duration
     *          Duration
     * @param timeZone
     *          Time zone
     * @param scheduleTranslator
     *          Schedule translator
     *
     */
    public MaintenanceWindowLayout(final VaadinMessageSource i18n, final TextField schedule, final TextField duration,
            final ComboBox<String> timeZone, final Label scheduleTranslator) {
        this.i18n = i18n;
        this.schedule = schedule;
        this.scheduleTranslator = scheduleTranslator;
        this.cronDescriptor = CronDescriptor.instance(HawkbitCommonUtil.getCurrentLocale());

        buildLayout(duration, timeZone);
        addValueChangeListeners();
    }

    private void buildLayout(final TextField duration, final ComboBox<String> timeZone) {
        setMargin(false);
        setSpacing(false);
        setStyleName("dist-window-maintenance-window-layout");
        setId(UIComponentIdProvider.MAINTENANCE_WINDOW_LAYOUT_ID);

        final HorizontalLayout maintenanceComponentsContainer = new HorizontalLayout();
        maintenanceComponentsContainer.setMargin(false);
        maintenanceComponentsContainer.setSpacing(false);

        maintenanceComponentsContainer.addComponent(schedule);
        maintenanceComponentsContainer.addComponent(duration);
        maintenanceComponentsContainer.addComponent(timeZone);

        addComponent(maintenanceComponentsContainer);

        addComponent(scheduleTranslator);
    }

    private void addValueChangeListeners() {
        schedule.addValueChangeListener(
                event -> scheduleTranslator.setValue(translateCronExpression(event.getValue())));
    }

    private String translateCronExpression(final String cronExpression) {
        try {
            return cronDescriptor.describe(MaintenanceScheduleHelper.getCronFromExpression(cronExpression));
        } catch (final IllegalArgumentException ex) {
            LOG.trace("Error in Cron Expression of Maintenance Window in UI: {}", ex.getMessage());
            return i18n.getMessage(UIMessageIdProvider.CRON_VALIDATION_ERROR);
        }
    }
}
