/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.filterlayout;

import java.util.Collection;

import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.builder.GridComponentBuilder;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyFilterButton;
import org.eclipse.hawkbit.ui.common.grid.AbstractGrid;
import org.eclipse.hawkbit.ui.common.grid.support.DeleteSupport;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUITagButtonStyle;
import org.eclipse.hawkbit.ui.rollout.ProxyFontIcon;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.util.StringUtils;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.StyleGenerator;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Parent class for filter button layout.
 * 
 * @param <T>
 *            The type of the Filter Button
 * @param <F>
 *            The filter-type used by the grid
 */
public abstract class AbstractFilterButtons<T extends ProxyFilterButton, F> extends AbstractGrid<T, F> {
    private static final long serialVersionUID = 1L;

    protected static final String DEFAULT_GREEN = "rgb(44,151,32)";

    protected static final String FILTER_BUTTON_COLUMN_ID = "filterButton";
    protected static final String FILTER_BUTTON_EDIT_ID = "filterButtonEdit";
    protected static final String FILTER_BUTTON_DELETE_ID = "filterButtonDelete";

    protected final transient DeleteSupport<T> filterButtonDeleteSupport;

    protected AbstractFilterButtons(final UIEventBus eventBus, final VaadinMessageSource i18n,
            final UINotification notification, final SpPermissionChecker permChecker) {
        super(i18n, eventBus, permChecker);

        this.filterButtonDeleteSupport = new DeleteSupport<>(this, i18n, notification, getMessageKeyEntityTypeSing(),
                getMessageKeyEntityTypePlur(), ProxyFilterButton::getName, this::deleteFilterButtons,
                UIComponentIdProvider.FILTER_BUTTON_DELETE_CONFIRMATION_DIALOG);
    }

    @Override
    public void init() {
        super.init();

        setHeaderVisible(false);
        setStyleName("type-button-layout");
        addStyleName(ValoTheme.TABLE_NO_STRIPES);
        addStyleName(ValoTheme.TABLE_NO_HORIZONTAL_LINES);
        addStyleName(ValoTheme.TABLE_NO_VERTICAL_LINES);
        addStyleName(ValoTheme.TABLE_BORDERLESS);
        addStyleName(ValoTheme.TABLE_COMPACT);
    }

    protected abstract String getMessageKeyEntityTypeSing();

    protected abstract String getMessageKeyEntityTypePlur();

    protected abstract boolean deleteFilterButtons(Collection<T> filterButtonsToDelete);

    @Override
    public void addColumns() {
        final StyleGenerator<T> filterStyleGenerator = item -> {
            if (getFilterButtonClickBehaviour().isFilterPreviouslyClicked(item)) {
                return SPUIStyleDefinitions.SP_FILTER_BTN_CLICKED_STYLE;
            } else {
                return null;
            }
        };
        GridComponentBuilder.addComponentColumn(this, this::buildFilterButtonLayout, filterStyleGenerator)
                .setId(FILTER_BUTTON_COLUMN_ID);
        GridComponentBuilder.addIconColumn(this, this::buildEditFilterButton, FILTER_BUTTON_EDIT_ID, null)
                .setHidden(true).setWidth(25D);

        GridComponentBuilder.addDeleteColumn(this, i18n, FILTER_BUTTON_DELETE_ID, filterButtonDeleteSupport,
                getFilterButtonIdPrefix() + ".delete.", e -> isDeletionAllowed()).setHidden(true).setWidth(25D);
    }

    private HorizontalLayout buildFilterButtonLayout(final T clickedFilter) {
        final Label colourIcon = buildColourIcon(clickedFilter.getId(), clickedFilter.getColour());
        final Button filterName = buildFilterNameButton(clickedFilter);

        final HorizontalLayout filterButtonLayout = new HorizontalLayout();
        filterButtonLayout.setSpacing(false);
        filterButtonLayout.setMargin(false);
        filterButtonLayout.setSizeFull();
        filterButtonLayout.addStyleName(SPUIStyleDefinitions.FILTER_BUTTON_WRAPPER);

        filterButtonLayout.addComponent(colourIcon);
        filterButtonLayout.setComponentAlignment(colourIcon, Alignment.TOP_LEFT);
        filterButtonLayout.setExpandRatio(colourIcon, 0.0F);

        filterButtonLayout.addComponent(filterName);
        filterButtonLayout.setComponentAlignment(filterName, Alignment.TOP_LEFT);
        filterButtonLayout.setExpandRatio(filterName, 1.0F);

        return filterButtonLayout;
    }

    private final Label buildColourIcon(final Long clickedFilterId, final String colour) {
        final ProxyFontIcon colourFontIcon = new ProxyFontIcon(VaadinIcons.CIRCLE, ValoTheme.LABEL_TINY, "",
                !StringUtils.isEmpty(colour) ? colour : DEFAULT_GREEN);
        final String colourIconId = new StringBuilder(getFilterButtonIdPrefix()).append(".colour-icon.")
                .append(clickedFilterId).toString();

        return SPUIComponentProvider.getLabelIcon(colourFontIcon, colourIconId);
    }

    private Button buildFilterNameButton(final T clickedFilter) {
        final String filterNameId = new StringBuilder(getFilterButtonIdPrefix()).append(".")
                .append(clickedFilter.getId()).toString();

        final Button filterNameButton = SPUIComponentProvider.getButton(filterNameId, clickedFilter.getName(),
                clickedFilter.getName(), null, false, null, SPUITagButtonStyle.class);

        filterNameButton.addClickListener(event -> getFilterButtonClickBehaviour().processFilterClick(clickedFilter));

        return filterNameButton;
    }

    /**
     * Get prefix Id of Button Wrapper to be used for drag and drop, delete and
     * test cases.
     *
     * @return prefix Id of Button Wrapper
     */
    protected abstract String getFilterButtonIdPrefix();

    protected abstract AbstractFilterButtonClickBehaviour<T> getFilterButtonClickBehaviour();

    private Button buildEditFilterButton(final T clickedFilter) {
        return GridComponentBuilder.buildActionButton(i18n, clickEvent -> editButtonClickListener(clickedFilter),
                VaadinIcons.EDIT, SPUIDefinitions.EDIT, SPUIStyleDefinitions.STATUS_ICON_NEUTRAL,
                getFilterButtonIdPrefix() + ".edit." + clickedFilter.getId(), isEditAllowed());
    }

    protected abstract void editButtonClickListener(final T clickedFilter);

    protected abstract boolean isDeletionAllowed();

    protected abstract boolean isEditAllowed();

    /**
     * Hides the edit and delete icon next to the filter tags in target,
     * distribution and software module tags/types layouts.
     */
    public void hideActionColumns() {
        getColumn(FILTER_BUTTON_EDIT_ID).setHidden(true);
        getColumn(FILTER_BUTTON_DELETE_ID).setHidden(true);
        recalculateColumnWidths();
    }

    /**
     * Show delete column in the grid
     */
    public void showDeleteColumn() {
        getColumn(FILTER_BUTTON_DELETE_ID).setHidden(false);
        getColumn(FILTER_BUTTON_EDIT_ID).setHidden(true);
        recalculateColumnWidths();
    }

    /**
     * Show edit column in the grid
     */
    public void showEditColumn() {
        getColumn(FILTER_BUTTON_EDIT_ID).setHidden(false);
        getColumn(FILTER_BUTTON_DELETE_ID).setHidden(true);
        recalculateColumnWidths();
    }
}
