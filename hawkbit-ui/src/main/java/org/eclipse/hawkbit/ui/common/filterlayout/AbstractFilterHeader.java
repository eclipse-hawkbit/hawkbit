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
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmallNoBorder;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Parent class for filter button header layout.
 */
public abstract class AbstractFilterHeader extends VerticalLayout {

    private static final long serialVersionUID = 1L;

    protected SpPermissionChecker permChecker;

    protected transient EventBus.UIEventBus eventBus;

    private Label title;

    private Button hideIcon;

    private MenuBar menu;

    protected final VaadinMessageSource i18n;

    protected AbstractFilterHeader(final SpPermissionChecker permChecker, final UIEventBus eventBus,
            final VaadinMessageSource i18n) {
        this.permChecker = permChecker;
        this.eventBus = eventBus;
        this.i18n = i18n;
        createComponents();
        buildLayout();
    }

    /**
     * Create required components.
     */
    private void createComponents() {
        title = createHeaderCaption();

        if (hasCreateUpdatePermission() && isAddTagRequired()) {
            menu = new MenuBar();
            menu.setStyleName(ValoTheme.MENUBAR_BORDERLESS);
            menu.addStyleName("menubar-position");
            final MenuItem configure = menu.addItem("", FontAwesome.COG, null);
            configure.setStyleName("tags");
            if (hasCreateUpdatePermission()) {
                configure.addItem("create", FontAwesome.PLUS, addButtonClicked());
                configure.addItem("update", FontAwesome.EDIT, updateButtonClicked());
            }
            if (permChecker.hasDeleteRepositoryPermission()) {
                configure.addItem("delete", FontAwesome.TRASH_O, deleteButtonClicked());
            }
        }
        hideIcon = SPUIComponentProvider.getButton(getHideButtonId(), "", "", "", true, FontAwesome.TIMES,
                SPUIButtonStyleSmallNoBorder.class);
        hideIcon.addClickListener(event -> hideFilterButtonLayout());
    }

    protected abstract Command deleteButtonClicked();

    protected abstract Command updateButtonClicked();

    protected abstract Command addButtonClicked();

    /**
     * Build layout.
     */
    private void buildLayout() {
        setStyleName("filter-btns-header-layout");
        final HorizontalLayout typeHeaderLayout = new HorizontalLayout();
        typeHeaderLayout.setWidth(100.0F, Unit.PERCENTAGE);
        typeHeaderLayout.addComponentAsFirst(title);
        typeHeaderLayout.addStyleName(SPUIStyleDefinitions.WIDGET_TITLE);
        typeHeaderLayout.setComponentAlignment(title, Alignment.TOP_LEFT);
        if (menu != null && hasCreateUpdatePermission()) {
            typeHeaderLayout.addComponent(menu);
            typeHeaderLayout.setComponentAlignment(menu, Alignment.TOP_LEFT);
        }
        typeHeaderLayout.addComponent(hideIcon);
        typeHeaderLayout.setComponentAlignment(hideIcon, Alignment.TOP_RIGHT);
        typeHeaderLayout.setExpandRatio(title, 1.0F);
        addComponent(typeHeaderLayout);
    }

    private Label createHeaderCaption() {
        return new LabelBuilder().name(getTitle()).buildCaptionLabel();
    }

    /**
     * 
     * @return
     */
    protected abstract String getHideButtonId();

    /**
     * Check if user is authorized for this action.
     * 
     * @return true if user has permission otherwise false.
     */
    protected boolean hasCreateUpdatePermission() {
        return permChecker.hasCreateRepositoryPermission() || permChecker.hasUpdateRepositoryPermission();
    }

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
     * @return
     */
    protected abstract boolean isAddTagRequired();

}
