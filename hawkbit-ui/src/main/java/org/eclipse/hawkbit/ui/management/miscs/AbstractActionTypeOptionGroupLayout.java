/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.miscs;

import java.util.Arrays;
import java.util.Optional;

import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.hene.flexibleoptiongroup.FlexibleOptionGroup;
import org.vaadin.hene.flexibleoptiongroup.FlexibleOptionGroupItemComponent;

import com.vaadin.server.FontAwesome;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;

/**
 * Action type option group abstract layout.
 */
public abstract class AbstractActionTypeOptionGroupLayout extends HorizontalLayout {
    private static final long serialVersionUID = 1L;

    protected static final String STYLE_DIST_WINDOW_ACTIONTYPE = "dist-window-actiontype";
    private static final String STYLE_DIST_WINDOW_ACTIONTYPE_LAYOUT = "dist-window-actiontype-horz-layout";

    protected final VaadinMessageSource i18n;
    protected FlexibleOptionGroup actionTypeOptionGroup;

    /**
     * Constructor
     * 
     * @param i18n
     *            VaadinMessageSource
     */
    protected AbstractActionTypeOptionGroupLayout(final VaadinMessageSource i18n) {
        this.i18n = i18n;
        init();
    }

    private void init() {
        createOptionGroup();
        setStyleName(STYLE_DIST_WINDOW_ACTIONTYPE_LAYOUT);
        setSizeUndefined();
    }

    protected abstract void createOptionGroup();

    protected void addForcedItemWithLabel() {
        final FlexibleOptionGroupItemComponent forceItem = actionTypeOptionGroup
                .getItemComponent(ActionTypeOption.FORCED);
        forceItem.setStyleName(STYLE_DIST_WINDOW_ACTIONTYPE);
        forceItem.setId(UIComponentIdProvider.SAVE_ACTION_RADIO_FORCED);
        addComponent(forceItem);
        final Label forceLabel = new Label();
        forceLabel.setStyleName("statusIconPending");
        forceLabel.setIcon(FontAwesome.BOLT);
        forceLabel.setCaption(i18n.getMessage(UIMessageIdProvider.CAPTION_ACTION_FORCED));
        forceLabel.setDescription(i18n.getMessage(UIMessageIdProvider.TOOLTIP_FORCED_ITEM));
        forceLabel.setStyleName("padding-right-style");
        addComponent(forceLabel);
    }

    protected void addSoftItemWithLabel() {
        final FlexibleOptionGroupItemComponent softItem = actionTypeOptionGroup.getItemComponent(ActionTypeOption.SOFT);
        softItem.setId(UIComponentIdProvider.ACTION_DETAILS_SOFT_ID);
        softItem.setStyleName(STYLE_DIST_WINDOW_ACTIONTYPE);
        addComponent(softItem);
        final Label softLabel = new Label();
        softLabel.setSizeFull();
        softLabel.setCaption(i18n.getMessage(UIMessageIdProvider.CAPTION_ACTION_SOFT));
        softLabel.setDescription(i18n.getMessage(UIMessageIdProvider.TOOLTIP_SOFT_ITEM));
        softLabel.setStyleName("padding-right-style");
        softLabel.setIcon(FontAwesome.STEP_FORWARD);
        addComponent(softLabel);
    }

    protected void addDownloadOnlyItemWithLabel() {
        final FlexibleOptionGroupItemComponent downloadOnlyItem = actionTypeOptionGroup
                .getItemComponent(ActionTypeOption.DOWNLOAD_ONLY);
        downloadOnlyItem.setId(UIComponentIdProvider.ACTION_DETAILS_DOWNLOAD_ONLY_ID);
        downloadOnlyItem.setStyleName(STYLE_DIST_WINDOW_ACTIONTYPE);
        addComponent(downloadOnlyItem);
        final Label downloadOnlyLabel = new Label();
        downloadOnlyLabel.setSizeFull();
        downloadOnlyLabel.setCaption(i18n.getMessage(UIMessageIdProvider.CAPTION_ACTION_DOWNLOAD_ONLY));
        downloadOnlyLabel.setDescription(i18n.getMessage(UIMessageIdProvider.TOOLTIP_DOWNLOAD_ONLY_ITEM));
        downloadOnlyLabel.setStyleName("padding-right-style");
        downloadOnlyLabel.setIcon(FontAwesome.DOWNLOAD);
        addComponent(downloadOnlyLabel);
    }

    /**
     * To Set Default option for save.
     */
    public void selectDefaultOption() {
        actionTypeOptionGroup.select(ActionTypeOption.FORCED);
    }

    /**
     * Enum which described the options for the action type
     *
     */
    public enum ActionTypeOption {
        FORCED(ActionType.FORCED), SOFT(ActionType.SOFT), AUTO_FORCED(ActionType.TIMEFORCED), DOWNLOAD_ONLY(
                ActionType.DOWNLOAD_ONLY);

        private final ActionType actionType;

        ActionTypeOption(final ActionType actionType) {
            this.actionType = actionType;
        }

        public ActionType getActionType() {
            return actionType;
        }

        /**
         * Matches the action type to the option
         * 
         * @param actionType
         *            the action type to get option for
         * @return action type option if matches, otherwise empty Optional
         */
        public static Optional<ActionTypeOption> getOptionForActionType(final ActionType actionType) {
            return Arrays.stream(ActionTypeOption.values()).filter(option -> option.getActionType() == actionType)
                    .findFirst();
        }
    }

    public FlexibleOptionGroup getActionTypeOptionGroup() {
        return actionTypeOptionGroup;
    }
}
