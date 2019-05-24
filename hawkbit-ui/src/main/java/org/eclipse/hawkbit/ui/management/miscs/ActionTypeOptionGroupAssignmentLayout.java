/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.miscs;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.TimeZone;

import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPDateTimeUtil;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.hene.flexibleoptiongroup.FlexibleOptionGroup;
import org.vaadin.hene.flexibleoptiongroup.FlexibleOptionGroupItemComponent;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.datefield.Resolution;
import com.vaadin.ui.DateField;
import com.vaadin.ui.Label;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Action type option group layout for manual assignment.
 */
public class ActionTypeOptionGroupAssignmentLayout extends AbstractActionTypeOptionGroupLayout {
    private static final long serialVersionUID = 1L;

    private DateField forcedTimeDateField;

    /**
     * Constructor
     * 
     * @param i18n
     *            VaadinMessageSource
     */
    public ActionTypeOptionGroupAssignmentLayout(final VaadinMessageSource i18n) {
        super(i18n);
        addValueChangeListener();
    }

    private void addValueChangeListener() {
        actionTypeOptionGroup.addValueChangeListener(new ValueChangeListener() {
            private static final long serialVersionUID = 1L;

            @Override
            // Vaadin is returning object so "==" might not work
            @SuppressWarnings("squid:S4551")
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

    @Override
    protected void createOptionGroup() {
        actionTypeOptionGroup = new FlexibleOptionGroup();
        actionTypeOptionGroup.addItem(ActionTypeOption.SOFT);
        actionTypeOptionGroup.addItem(ActionTypeOption.FORCED);
        actionTypeOptionGroup.addItem(ActionTypeOption.AUTO_FORCED);
        actionTypeOptionGroup.addItem(ActionTypeOption.DOWNLOAD_ONLY);
        selectDefaultOption();

        addForcedItemWithLabel();
        addSoftItemWithLabel();
        addAutoForceItemWithLabelAndDateField();
        addDownloadOnlyItemWithLabel();
    }

    private void addAutoForceItemWithLabelAndDateField() {
        final FlexibleOptionGroupItemComponent autoForceItem = actionTypeOptionGroup
                .getItemComponent(ActionTypeOption.AUTO_FORCED);
        autoForceItem.setStyleName(STYLE_DIST_WINDOW_ACTIONTYPE);
        autoForceItem.setId(UIComponentIdProvider.ACTION_TYPE_OPTION_GROUP_SAVE_TIMEFORCED);
        addComponent(autoForceItem);
        final Label autoForceLabel = new Label();
        autoForceLabel.setStyleName("statusIconPending");
        autoForceLabel.setIcon(FontAwesome.HISTORY);
        autoForceLabel.setCaption(i18n.getMessage(UIMessageIdProvider.CAPTION_ACTION_TIME_FORCED));
        autoForceLabel.setDescription(i18n.getMessage(UIMessageIdProvider.TOOLTIP_TIMEFORCED_ITEM));
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
        forcedTimeDateField.setLocale(HawkbitCommonUtil.getCurrentLocale());
        forcedTimeDateField.setResolution(Resolution.MINUTE);
        forcedTimeDateField.addStyleName(ValoTheme.DATEFIELD_SMALL);
        addComponent(forcedTimeDateField);
    }

    public DateField getForcedTimeDateField() {
        return forcedTimeDateField;
    }
}
