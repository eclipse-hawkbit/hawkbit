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
import com.vaadin.ui.VerticalLayout;

/**
 * Parent class for filter button header layout.
 */
public abstract class AbstractFilterHeader extends VerticalLayout {

    private static final long serialVersionUID = -1388340600522323332L;

    protected SpPermissionChecker permChecker;

    protected transient EventBus.UIEventBus eventBus;

    private Label title;

    private Button config;

    private Button hideIcon;

    protected final VaadinMessageSource i18n;

    protected AbstractFilterHeader(final SpPermissionChecker permChecker, final UIEventBus eventBus, final VaadinMessageSource i18n) {
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
            config = SPUIComponentProvider.getButton(getConfigureFilterButtonId(), "", "", "", true, FontAwesome.COG,
                    SPUIButtonStyleSmallNoBorder.class);
            config.addClickListener(this::settingsIconClicked);
        }
        hideIcon = SPUIComponentProvider.getButton(getHideButtonId(), "", "", "", true, FontAwesome.TIMES,
                SPUIButtonStyleSmallNoBorder.class);
        hideIcon.addClickListener(event -> hideFilterButtonLayout());
    }

    /**
     * Build layout.
     */
    private void buildLayout() {
        setStyleName("filter-btns-header-layout");
        final HorizontalLayout typeHeaderLayout = new HorizontalLayout();
        typeHeaderLayout.setWidth(100.0f, Unit.PERCENTAGE);
        typeHeaderLayout.addComponentAsFirst(title);
        typeHeaderLayout.addStyleName(SPUIStyleDefinitions.WIDGET_TITLE);
        typeHeaderLayout.setComponentAlignment(title, Alignment.TOP_LEFT);
        if (config != null && hasCreateUpdatePermission()) {
            typeHeaderLayout.addComponent(config);
            typeHeaderLayout.setComponentAlignment(config, Alignment.TOP_RIGHT);
        }
        typeHeaderLayout.addComponent(hideIcon);
        typeHeaderLayout.setComponentAlignment(hideIcon, Alignment.TOP_RIGHT);
        typeHeaderLayout.setExpandRatio(title, 1.0f);
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
    protected abstract boolean hasCreateUpdatePermission();

    /**
     * Get the title to be displayed on the header of filter button layout.
     * 
     * @return title to be displayed.
     */
    protected abstract String getTitle();

    /**
     * This method will be called when settings icon (or) clicked on the header.
     * 
     * @param event
     *            reference of {@link Button.ClicEvent}.
     */
    protected abstract void settingsIconClicked(final Button.ClickEvent event);

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
