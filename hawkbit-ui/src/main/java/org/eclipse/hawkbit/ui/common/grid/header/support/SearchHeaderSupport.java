/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.grid.header.support;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.eclipse.hawkbit.ui.common.builder.TextFieldBuilder;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleNoBorder;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.util.StringUtils;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextField;

/**
 * Header support for search functionality
 */
public class SearchHeaderSupport implements HeaderSupport {
    private static final String MODE_SEARCH_INPUT = "mode-search-input";

    private final VaadinMessageSource i18n;

    private final String searchFieldId;
    private final String searchResetIconId;
    private final Supplier<String> searchStateSupplier;
    private final Consumer<String> searchByCallback;

    private final TextField searchField;
    private final Button searchResetIcon;

    private boolean isSearchInputActive;

    /**
     * Constructor for SearchHeaderSupport
     *
     * @param i18n
     *            VaadinMessageSource
     * @param searchFieldId
     *            Search field id
     * @param searchResetIconId
     *            Value supplier for search box
     * @param searchStateSupplier
     *            Search state supplier
     * @param searchByCallback
     *            Callback for search event
     */
    public SearchHeaderSupport(final VaadinMessageSource i18n, final String searchFieldId,
            final String searchResetIconId, final Supplier<String> searchStateSupplier,
            final Consumer<String> searchByCallback) {
        this.i18n = i18n;

        this.searchFieldId = searchFieldId;
        this.searchResetIconId = searchResetIconId;
        this.searchStateSupplier = searchStateSupplier;
        this.searchByCallback = searchByCallback;

        this.searchField = createSearchField();
        this.searchResetIcon = createSearchResetIcon();
    }

    private TextField createSearchField() {
        final TextField searchInput = new TextFieldBuilder(64).id(searchFieldId).style(SPUIDefinitions.FILTER_BOX)
                .styleName("text-style").buildTextComponent();
        searchInput.setWidthFull();

        searchInput.addValueChangeListener(event -> {
            // we do not want to send the event during state restore, so we
            // react only on user input
            if (event.isUserOriginated()) {
                searchByCallback.accept(event.getValue());
            }
        });

        return searchInput;
    }

    private Button createSearchResetIcon() {
        final Button searchResetButton = SPUIComponentProvider.getButton(searchResetIconId, "",
                i18n.getMessage(UIMessageIdProvider.TOOLTIP_SEARCH), null, false, VaadinIcons.SEARCH,
                SPUIButtonStyleNoBorder.class);

        searchResetButton.addClickListener(event -> onSearchResetClick());

        return searchResetButton;
    }

    private void onSearchResetClick() {
        if (isSearchInputActive) {
            // Clicked on reset search icon
            closeSearchTextField();
        } else {
            // Clicked on search icon
            openSearchTextField();
        }
    }

    private void openSearchTextField() {
        searchResetIcon.setIcon(VaadinIcons.CLOSE);
        searchResetIcon.setDescription(i18n.getMessage(UIMessageIdProvider.TOOLTIP_RESET));
        searchResetIcon.addStyleNames(SPUIDefinitions.FILTER_RESET_ICON, MODE_SEARCH_INPUT);

        searchField.setVisible(true);
        searchField.focus();

        isSearchInputActive = true;
    }

    private void closeSearchTextField() {
        searchResetIcon.setIcon(VaadinIcons.SEARCH);
        searchResetIcon.setDescription(i18n.getMessage(UIMessageIdProvider.TOOLTIP_SEARCH));
        searchResetIcon.removeStyleNames(SPUIDefinitions.FILTER_RESET_ICON, MODE_SEARCH_INPUT);

        searchField.setValue("");
        searchField.setVisible(false);

        searchByCallback.accept(null);

        isSearchInputActive = false;
    }

    /**
     * Update the search and trigger the callback to refresh the grid.
     * 
     * @param searchQuery
     *            search query
     */
    public void setAndTriggerSearch(final String searchQuery) {
        if (!StringUtils.isEmpty(searchQuery)) {
            openSearchTextField();
            searchField.setValue(searchQuery);
            searchByCallback.accept(searchQuery);
        }
    }

    @Override
    public void restoreState() {
        final String onLoadSearchBoxValue = searchStateSupplier.get();

        if (!StringUtils.isEmpty(onLoadSearchBoxValue)) {
            openSearchTextField();
            searchField.setValue(onLoadSearchBoxValue);
        }
    }

    /**
     * Disable search icon
     */
    public void disableSearch() {
        searchResetIcon.setEnabled(false);
    }

    /**
     * Enable search icon
     */
    public void enableSearch() {
        searchResetIcon.setEnabled(true);
    }

    /**
     * Reset the search value to empty and close the search bar
     */
    public void resetSearch() {
        if (isSearchInputActive) {
            closeSearchTextField();
        }
    }

    @Override
    public Component getHeaderComponent() {
        final HorizontalLayout headerIconLayout = new HorizontalLayout();
        headerIconLayout.setMargin(false);
        headerIconLayout.setSpacing(false);
        headerIconLayout.setWidthFull();

        headerIconLayout.addComponent(searchField);
        headerIconLayout.setComponentAlignment(searchField, Alignment.TOP_RIGHT);
        headerIconLayout.setExpandRatio(searchField, 1.0F);

        headerIconLayout.addComponent(searchResetIcon);
        headerIconLayout.setComponentAlignment(searchResetIcon, Alignment.TOP_RIGHT);
        headerIconLayout.setExpandRatio(searchResetIcon, 0F);

        // hidden by default
        searchField.setVisible(false);

        return headerIconLayout;
    }

    @Override
    public float getExpandRation() {
        return 0.6F;
    }
}
