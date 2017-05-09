/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.footer;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.TimeZone;

import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPDateTimeUtil;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
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
 * Action type option group layout.
 * 
 *
 */
public class ActionTypeOptionGroupLayout extends HorizontalLayout {

    private static final long serialVersionUID = -5624576558669213864L;
    private static final String STYLE_DIST_WINDOW_ACTIONTYPE = "dist-window-actiontype";

    private final VaadinMessageSource i18n;

    private FlexibleOptionGroup actionTypeOptionGroup;

    private DateField forcedTimeDateField;

    public ActionTypeOptionGroupLayout(final VaadinMessageSource i18n) {
        this.i18n = i18n;

        createOptionGroup();
        addValueChangeListener();
        setStyleName("dist-window-actiontype-horz-layout");
        setSizeUndefined();
    }

    private void addValueChangeListener() {
        actionTypeOptionGroup.addValueChangeListener(new ValueChangeListener() {
            private static final long serialVersionUID = 1L;

            @Override
            public void valueChange(final ValueChangeEvent event) {
                if (event.getProperty().getValue().equals(ActionTypeOption.AUTO_FORCED)) {
                    forcedTimeDateField.setEnabled(true);
                    forcedTimeDateField.setRequired(true);
                } else {
                    forcedTimeDateField.setEnabled(false);
                    forcedTimeDateField.setRequired(false);
                }
            }
        });
    }

    private void createOptionGroup() {
        actionTypeOptionGroup = new FlexibleOptionGroup();
        actionTypeOptionGroup.addItem(ActionTypeOption.SOFT);
        actionTypeOptionGroup.addItem(ActionTypeOption.FORCED);
        actionTypeOptionGroup.addItem(ActionTypeOption.AUTO_FORCED);
        selectDefaultOption();

        final FlexibleOptionGroupItemComponent forceItem = actionTypeOptionGroup
                .getItemComponent(ActionTypeOption.FORCED);
        forceItem.setStyleName(STYLE_DIST_WINDOW_ACTIONTYPE);
        // set Id for Forced radio button.
        forceItem.setId("save.action.radio.forced");
        addComponent(forceItem);
        final Label forceLabel = new Label();
        forceLabel.setStyleName("statusIconPending");
        forceLabel.setIcon(FontAwesome.BOLT);
        forceLabel.setCaption("Forced");
        forceLabel.setDescription(i18n.getMessage("tooltip.forced.item"));
        forceLabel.setStyleName("padding-right-style");
        addComponent(forceLabel);

        final FlexibleOptionGroupItemComponent softItem = actionTypeOptionGroup.getItemComponent(ActionTypeOption.SOFT);
        softItem.setId(UIComponentIdProvider.ACTION_DETAILS_SOFT_ID);
        softItem.setStyleName(STYLE_DIST_WINDOW_ACTIONTYPE);
        addComponent(softItem);
        final Label softLabel = new Label();
        softLabel.setSizeFull();
        softLabel.setCaption("Soft");
        softLabel.setDescription(i18n.getMessage("tooltip.soft.item"));
        softLabel.setStyleName("padding-right-style");
        addComponent(softLabel);

        final FlexibleOptionGroupItemComponent autoForceItem = actionTypeOptionGroup
                .getItemComponent(ActionTypeOption.AUTO_FORCED);
        autoForceItem.setStyleName(STYLE_DIST_WINDOW_ACTIONTYPE);
        // setted Id for Time Forced radio button.
        autoForceItem.setId(UIComponentIdProvider.ACTION_TYPE_OPTION_GROUP_SAVE_TIMEFORCED);
        addComponent(autoForceItem);
        final Label autoForceLabel = new Label();
        autoForceLabel.setStyleName("statusIconPending");
        autoForceLabel.setIcon(FontAwesome.HISTORY);
        autoForceLabel.setCaption("Time Forced");
        autoForceLabel.setDescription(i18n.getMessage("tooltip.timeforced.item"));
        autoForceLabel.setStyleName(STYLE_DIST_WINDOW_ACTIONTYPE);
        addComponent(autoForceLabel);

        forcedTimeDateField = new DateField();
        forcedTimeDateField.setInvalidAllowed(false);
        forcedTimeDateField.setInvalidCommitted(false);
        forcedTimeDateField.setEnabled(false);
        forcedTimeDateField.setStyleName("dist-window-forcedtime");

        final TimeZone tz = SPDateTimeUtil.getBrowserTimeZone();
        forcedTimeDateField.setValue(
                Date.from(LocalDateTime.now().plusWeeks(2).atZone(SPDateTimeUtil.getTimeZoneId(tz)).toInstant()));
        forcedTimeDateField.setImmediate(true);
        forcedTimeDateField.setTimeZone(tz);
        forcedTimeDateField.setLocale(HawkbitCommonUtil.getLocale());
        forcedTimeDateField.setResolution(Resolution.MINUTE);
        forcedTimeDateField.addStyleName(ValoTheme.DATEFIELD_SMALL);
        addComponent(forcedTimeDateField);
    }

    /**
     * To Set Default option for save.
     */

    public void selectDefaultOption() {
        actionTypeOptionGroup.select(ActionTypeOption.FORCED);
    }

    public enum ActionTypeOption {
        FORCED(ActionType.FORCED), SOFT(ActionType.SOFT), AUTO_FORCED(ActionType.TIMEFORCED);

        private final ActionType actionType;

        ActionTypeOption(final ActionType actionType) {
            this.actionType = actionType;
        }

        /**
         * @return
         */
        public ActionType getActionType() {
            return actionType;
        }
    }

    public FlexibleOptionGroup getActionTypeOptionGroup() {
        return actionTypeOptionGroup;
    }

    public DateField getForcedTimeDateField() {
        return forcedTimeDateField;
    }

}
