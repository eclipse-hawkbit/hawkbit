/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.filterlayout;

import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.builder.LabelBuilder;
import org.eclipse.hawkbit.ui.common.event.FilterHeaderEvent;
import org.eclipse.hawkbit.ui.common.event.FilterHeaderEvent.FilterHeaderEnum;
import org.eclipse.hawkbit.ui.components.ConfigMenuBar;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleNoBorder;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.VerticalLayout;

/**
 * Parent class for filter button header layout.
 */
public abstract class AbstractFilterHeader extends VerticalLayout {

    private static final long serialVersionUID = 1L;

    private final SpPermissionChecker permChecker;

    private final transient EventBus.UIEventBus eventBus;

    private Label title;

    private Button hideIcon;

    private ConfigMenuBar menu;

    private final VaadinMessageSource i18n;

    private HorizontalLayout typeHeaderLayout;

    private Button cancelTagButton;

    protected AbstractFilterHeader(final SpPermissionChecker permChecker, final UIEventBus eventBus,
            final VaadinMessageSource i18n) {
        this.permChecker = permChecker;
        this.eventBus = eventBus;
        this.i18n = i18n;
        createComponents();
        buildLayout();
        if (doSubscribeToEventBus()) {
            eventBus.subscribe(this);
        }
    }

    /**
     * Subscribes the view to the eventBus. Method has to be overriden (return
     * false) if the view does not contain any listener to avoid Vaadin blowing
     * up our logs with warnings.
     */
    protected boolean doSubscribeToEventBus() {
        return true;
    }

    protected void removeMenuBarAndAddCancelButton() {
        typeHeaderLayout.removeComponent(menu);
        typeHeaderLayout.addComponent(createCancelButtonForUpdateOrDeleteTag(), 1);
    }

    private Button createCancelButtonForUpdateOrDeleteTag() {
        cancelTagButton = SPUIComponentProvider.getButton(UIComponentIdProvider.CANCEL_UPDATE_TAG_ID, "", "", null,
                false, FontAwesome.TIMES_CIRCLE, SPUIButtonStyleNoBorder.class);
        cancelTagButton.addClickListener(this::cancelUpdateOrDeleteTag);
        return cancelTagButton;
    }

    @SuppressWarnings("squid:S1172")
    protected void cancelUpdateOrDeleteTag(final ClickEvent event) {
        removeCancelButtonAndAddMenuBar();
    }

    private void createComponents() {
        title = createHeaderCaption();

        if (isAddTagRequired()) {
            menu = new ConfigMenuBar(permChecker.hasCreateRepositoryPermission(),
                    permChecker.hasUpdateRepositoryPermission(), permChecker.hasDeleteRepositoryPermission(),
                    getAddButtonCommand(), getUpdateButtonCommand(), getDeleteButtonCommand(), getMenuBarId(), i18n);
        }
        hideIcon = SPUIComponentProvider.getButton(getHideButtonId(), "",
                i18n.getMessage(UIMessageIdProvider.TOOLTIP_CLOSE), "", true, FontAwesome.TIMES,
                SPUIButtonStyleNoBorder.class);
        hideIcon.addClickListener(event -> hideFilterButtonLayout());
    }

    protected void processFilterHeaderEvent(final FilterHeaderEvent event) {
        if (FilterHeaderEnum.SHOW_MENUBAR == event.getFilterHeaderEnum()
                && typeHeaderLayout.getComponent(1).equals(cancelTagButton)) {
            removeCancelButtonAndAddMenuBar();
        } else if (FilterHeaderEnum.SHOW_CANCEL_BUTTON == event.getFilterHeaderEnum()) {
            removeMenuBarAndAddCancelButton();
        }
    }

    /**
     * Returns the id for the menubar element for configuring tags and types
     * 
     * @return String with id
     */
    protected abstract String getMenuBarId();

    /**
     * Command which should be executed when clicking on the delete tag button
     * in the menubar
     * 
     * @return Command
     */
    protected abstract Command getDeleteButtonCommand();

    /**
     * Command which should be executed when clicking on the update tag button
     * in the menubar
     * 
     * @return Command
     */
    protected abstract Command getUpdateButtonCommand();

    /**
     * Command which should be executed when clicking on the create tag button
     * in the menubar
     * 
     * @return Command
     */
    protected abstract Command getAddButtonCommand();

    private void buildLayout() {
        setStyleName("filter-btns-header-layout");
        typeHeaderLayout = new HorizontalLayout();
        typeHeaderLayout.setHeight(32, Unit.PIXELS);
        typeHeaderLayout.setWidth(100.0F, Unit.PERCENTAGE);
        typeHeaderLayout.addComponentAsFirst(title);
        typeHeaderLayout.addStyleName(SPUIStyleDefinitions.WIDGET_TITLE);
        typeHeaderLayout.setComponentAlignment(title, Alignment.TOP_LEFT);
        if (menu != null) {
            typeHeaderLayout.addComponent(menu);
            typeHeaderLayout.setComponentAlignment(menu, Alignment.TOP_RIGHT);
        }
        typeHeaderLayout.addComponent(hideIcon);
        typeHeaderLayout.setComponentAlignment(hideIcon, Alignment.TOP_RIGHT);
        typeHeaderLayout.setExpandRatio(title, 1.0F);
        addComponent(typeHeaderLayout);
    }

    private Label createHeaderCaption() {
        return new LabelBuilder().name(getTitle()).buildCaptionLabel();
    }

    protected void removeCancelButtonAndAddMenuBar() {
        typeHeaderLayout.removeComponent(cancelTagButton);
        typeHeaderLayout.addComponent(menu, 1);
    }

    /**
     * Returns the id of the hide filter button
     * 
     * @return String containing the id
     */
    protected abstract String getHideButtonId();

    /**
     * Get the title to be displayed on the header of filter button layout.
     * 
     * @return title to be displayed.
     */
    protected abstract String getTitle();

    /**
     * Space required to show drop hits in the filter layout header.
     * 
     * @return true to display drop hit.
     */
    protected abstract boolean dropHitsRequired();

    /**
     * This method will be called when hide button (X) is clicked.
     */
    protected abstract void hideFilterButtonLayout();

    /**
     * Add/update type button id.
     */
    protected abstract String getConfigureFilterButtonId();

    /**
     * Returns the information if the icon for configuring tags should be
     * visible
     * 
     * @return boolean
     */
    protected abstract boolean isAddTagRequired();

    protected SpPermissionChecker getPermChecker() {
        return permChecker;
    }

    protected EventBus.UIEventBus getEventBus() {
        return eventBus;
    }

    protected VaadinMessageSource getI18n() {
        return i18n;
    }

    protected HorizontalLayout getTypeHeaderLayout() {
        return typeHeaderLayout;
    }

}
