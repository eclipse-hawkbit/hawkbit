/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout.rollout;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.TimeZone;

import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPDateTimeUtil;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.vaadin.hene.flexibleoptiongroup.FlexibleOptionGroup;
import org.vaadin.hene.flexibleoptiongroup.FlexibleOptionGroupItemComponent;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.datefield.Resolution;
import com.vaadin.ui.DateField;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Rollout start types options layout
 */
public class AutoStartOptionGroupLayout extends HorizontalLayout {

    private static final long serialVersionUID = -8460459258964093525L;
    private static final String STYLE_DIST_WINDOW_AUTO_START = "dist-window-actiontype";

    private final I18N i18n;

    private FlexibleOptionGroup autoStartOptionGroup;

    private DateField startAtDateField;

    /**
     * Instantiates the auto start options layout
     * 
     * @param i18n
     *            the internationalization helper
     */
    public AutoStartOptionGroupLayout(final I18N i18n) {
        this.i18n = i18n;

        createOptionGroup();
        addValueChangeListener();
        setStyleName("dist-window-actiontype-horz-layout");
        setSizeUndefined();
    }

    private void addValueChangeListener() {
        autoStartOptionGroup.addValueChangeListener(new ValueChangeListener() {
            private static final long serialVersionUID = 1L;

            @Override
            public void valueChange(final ValueChangeEvent event) {
                if (event.getProperty().getValue().equals(AutoStartOption.SCHEDULED)) {
                    startAtDateField.setEnabled(true);
                    startAtDateField.setRequired(true);
                } else {
                    startAtDateField.setEnabled(false);
                    startAtDateField.setRequired(false);
                }
            }
        });
    }

    private void createOptionGroup() {
        autoStartOptionGroup = new FlexibleOptionGroup();
        autoStartOptionGroup.addItem(AutoStartOption.MANUAL);
        autoStartOptionGroup.addItem(AutoStartOption.AUTO_START);
        autoStartOptionGroup.addItem(AutoStartOption.SCHEDULED);
        selectDefaultOption();

        final FlexibleOptionGroupItemComponent manualItem = autoStartOptionGroup
                .getItemComponent(AutoStartOption.MANUAL);
        manualItem.setStyleName(STYLE_DIST_WINDOW_AUTO_START);
        // set Id for Forced radio button.
        manualItem.setId(UIComponentIdProvider.ROLLOUT_START_MANUAL_ID);
        addComponent(manualItem);
        final Label manualLabel = new Label();
        manualLabel.setStyleName("statusIconPending");
        manualLabel.setIcon(FontAwesome.HAND_PAPER_O);
        manualLabel.setCaption(i18n.get("caption.rollout.start.manual"));
        manualLabel.setDescription(i18n.get("caption.rollout.start.manual.desc"));
        manualLabel.setStyleName("padding-right-style");
        addComponent(manualLabel);

        final FlexibleOptionGroupItemComponent autoStartItem = autoStartOptionGroup
                .getItemComponent(AutoStartOption.AUTO_START);
        autoStartItem.setId(UIComponentIdProvider.ROLLOUT_START_AUTO_ID);
        autoStartItem.setStyleName(STYLE_DIST_WINDOW_AUTO_START);
        addComponent(autoStartItem);
        final Label autoStartLabel = new Label();
        autoStartLabel.setSizeFull();
        autoStartLabel.setIcon(FontAwesome.PLAY);
        autoStartLabel.setCaption(i18n.get("caption.rollout.start.auto"));
        autoStartLabel.setDescription(i18n.get("caption.rollout.start.auto.desc"));
        autoStartLabel.setStyleName("padding-right-style");
        addComponent(autoStartLabel);

        final FlexibleOptionGroupItemComponent scheduledItem = autoStartOptionGroup
                .getItemComponent(AutoStartOption.SCHEDULED);
        scheduledItem.setStyleName(STYLE_DIST_WINDOW_AUTO_START);
        // setted Id for Time Forced radio button.
        scheduledItem.setId(UIComponentIdProvider.ROLLOUT_START_SCHEDULED_ID);
        addComponent(scheduledItem);
        final Label scheduledLabel = new Label();
        scheduledLabel.setStyleName("statusIconPending");
        scheduledLabel.setIcon(FontAwesome.CLOCK_O);
        scheduledLabel.setCaption(i18n.get("caption.rollout.start.scheduled"));
        scheduledLabel.setDescription(i18n.get("caption.rollout.start.scheduled.desc"));
        scheduledLabel.setStyleName(STYLE_DIST_WINDOW_AUTO_START);
        addComponent(scheduledLabel);

        startAtDateField = new DateField();
        startAtDateField.setInvalidAllowed(false);
        startAtDateField.setInvalidCommitted(false);
        startAtDateField.setEnabled(false);
        startAtDateField.setStyleName("dist-window-forcedtime");

        final TimeZone tz = SPDateTimeUtil.getBrowserTimeZone();
        startAtDateField.setValue(
                Date.from(LocalDateTime.now().plusMinutes(30).atZone(SPDateTimeUtil.getTimeZoneId(tz)).toInstant()));
        startAtDateField.setImmediate(true);
        startAtDateField.setTimeZone(tz);
        startAtDateField.setLocale(i18n.getLocale());
        startAtDateField.setResolution(Resolution.MINUTE);
        startAtDateField.addStyleName(ValoTheme.DATEFIELD_SMALL);
        addComponent(startAtDateField);
    }

    /**
     * To Set Default option for save.
     */

    public void selectDefaultOption() {
        autoStartOptionGroup.select(AutoStartOption.MANUAL);
    }

    /**
     * Rollout start options
     */
    public enum AutoStartOption {
        MANUAL, AUTO_START, SCHEDULED;

    }

    public FlexibleOptionGroup getAutoStartOptionGroup() {
        return autoStartOptionGroup;
    }

    public DateField getStartAtDateField() {
        return startAtDateField;
    }

}
