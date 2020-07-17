/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.miscs;

import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.RadioButtonGroup;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Action type option group abstract layout.
 */
public abstract class AbstractActionTypeOptionGroupLayout extends HorizontalLayout {
    private static final long serialVersionUID = 1L;

    protected final VaadinMessageSource i18n;

    protected RadioButtonGroup<ActionType> actionTypeOptionGroup;

    private final String actionTypeOptionGroupId;

    /**
     * Constructor
     * 
     * @param i18n
     *            VaadinMessageSource
     */
    protected AbstractActionTypeOptionGroupLayout(final VaadinMessageSource i18n,
            final String actionTypeOptionGroupId) {
        this.i18n = i18n;
        this.actionTypeOptionGroupId = actionTypeOptionGroupId;

        init();
    }

    private void init() {
        setSizeUndefined();
        createOptionGroup();
        addOptionGroup();
    }

    private void createOptionGroup() {
        actionTypeOptionGroup = new RadioButtonGroup<>();
        actionTypeOptionGroup.setId(actionTypeOptionGroupId);
        actionTypeOptionGroup.setSizeFull();
        actionTypeOptionGroup.addStyleName(ValoTheme.OPTIONGROUP_HORIZONTAL);

        actionTypeOptionGroup.setItemIconGenerator(item -> {
            switch (item) {
            case FORCED:
                return VaadinIcons.BOLT;
            case SOFT:
                return VaadinIcons.USER_CHECK;
            case TIMEFORCED:
                return VaadinIcons.USER_CLOCK;
            case DOWNLOAD_ONLY:
                return VaadinIcons.DOWNLOAD;
            default:
                return null;
            }
        });

        actionTypeOptionGroup.setItemCaptionGenerator(item -> {
            switch (item) {
            case FORCED:
                return i18n.getMessage(UIMessageIdProvider.CAPTION_ACTION_FORCED);
            case SOFT:
                return i18n.getMessage(UIMessageIdProvider.CAPTION_ACTION_SOFT);
            case TIMEFORCED:
                return i18n.getMessage(UIMessageIdProvider.CAPTION_ACTION_TIME_FORCED);
            case DOWNLOAD_ONLY:
                return i18n.getMessage(UIMessageIdProvider.CAPTION_ACTION_DOWNLOAD_ONLY);
            default:
                return null;
            }
        });

        actionTypeOptionGroup.setItemDescriptionGenerator(item -> {
            switch (item) {
            case FORCED:
                return i18n.getMessage(UIMessageIdProvider.TOOLTIP_FORCED_ITEM);
            case SOFT:
                return i18n.getMessage(UIMessageIdProvider.TOOLTIP_SOFT_ITEM);
            case TIMEFORCED:
                return i18n.getMessage(UIMessageIdProvider.TOOLTIP_TIMEFORCED_ITEM);
            case DOWNLOAD_ONLY:
                return i18n.getMessage(UIMessageIdProvider.TOOLTIP_DOWNLOAD_ONLY_ITEM);
            default:
                return null;
            }
        });
    }

    protected abstract void addOptionGroup();

    /**
     * @return Radio button group of action type
     */
    public RadioButtonGroup<ActionType> getActionTypeOptionGroup() {
        return actionTypeOptionGroup;
    }
}
