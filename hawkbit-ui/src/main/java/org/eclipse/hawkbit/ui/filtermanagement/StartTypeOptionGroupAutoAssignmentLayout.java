/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.filtermanagement;

import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.RadioButtonGroup;
import com.vaadin.ui.themes.ValoTheme;

public class StartTypeOptionGroupAutoAssignmentLayout extends HorizontalLayout {

    private static final long serialVersionUID = 1L;

    private final VaadinMessageSource i18n;
    private final String autoAssignmentStartTypeOptionsId;

    private RadioButtonGroup<AutoAssignmentStartOption> startTypeOptionGroup;

    public StartTypeOptionGroupAutoAssignmentLayout(final VaadinMessageSource i18n,
            final String autoAssignmentStartTypeOptionsId) {
        this.i18n = i18n;
        this.autoAssignmentStartTypeOptionsId = autoAssignmentStartTypeOptionsId;

        setSizeUndefined();
        createStartTypeOptionGroup();
    }

    private void createStartTypeOptionGroup() {
        startTypeOptionGroup = new RadioButtonGroup<>();
        startTypeOptionGroup.setId(autoAssignmentStartTypeOptionsId);
        startTypeOptionGroup.setSizeFull();
        startTypeOptionGroup.addStyleName(ValoTheme.OPTIONGROUP_HORIZONTAL);

        startTypeOptionGroup.setItemIconGenerator(
                startType -> startType == AutoAssignmentStartOption.MANUAL ? VaadinIcons.HAND : VaadinIcons.PLAY);
        startTypeOptionGroup.setItemCaptionGenerator(startType -> startType == AutoAssignmentStartOption.MANUAL
                ? i18n.getMessage("caption.auto.assignment.start.manual")
                : i18n.getMessage("caption.auto.assignment.start.auto"));
        startTypeOptionGroup.setItemDescriptionGenerator(startType -> startType == AutoAssignmentStartOption.MANUAL
                ? i18n.getMessage("caption.auto.assignment.start.manual.desc")
                : i18n.getMessage("caption.auto.assignment.start.auto.desc"));

        startTypeOptionGroup.setItems(AutoAssignmentStartOption.values());
        addComponent(startTypeOptionGroup);
    }

    /**
     * Auto assignment start options
     */
    public enum AutoAssignmentStartOption {
        MANUAL, AUTO_START
    }

    /**
     * @return Radio button group of start type
     */
    public RadioButtonGroup<AutoAssignmentStartOption> getStartTypeOptionGroup() {
        return startTypeOptionGroup;
    }
}
