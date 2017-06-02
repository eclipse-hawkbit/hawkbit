/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.grid;

import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.builder.TextFieldBuilder;
import org.eclipse.hawkbit.ui.components.SPUIButton;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmallNoBorder;
import org.eclipse.hawkbit.ui.rollout.state.RolloutUIState;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.util.StringUtils;

import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

/**
 * 
 * Abstract grid header.
 * 
 */
public abstract class AbstractGridHeader extends VerticalLayout {

    private static final long serialVersionUID = -24429876573255519L;

    private HorizontalLayout headerCaptionLayout;

    private TextField searchField;

    private SPUIButton searchResetIcon;

    private Button addButton;

    private Button closeButton;

    protected final SpPermissionChecker permissionChecker;

    protected final RolloutUIState rolloutUIState;

    protected final VaadinMessageSource i18n;

    protected AbstractGridHeader(final SpPermissionChecker permissionChecker, final RolloutUIState rolloutUIState,
            final VaadinMessageSource i18n) {
        this.permissionChecker = permissionChecker;
        this.rolloutUIState = rolloutUIState;
        this.i18n = i18n;
        createComponents();
        buildLayout();
        restoreState();
    }

    private void restoreState() {
        final String onLoadSearchBoxValue = onLoadSearchBoxValue();
        if (!StringUtils.isEmpty(onLoadSearchBoxValue)) {
            openSearchTextField();
            searchField.setValue(onLoadSearchBoxValue);
        }
        restoreCaption();
    }

    private void createComponents() {
        headerCaptionLayout = getHeaderCaptionLayout();
        if (isRollout()) {
            searchField = new TextFieldBuilder(getSearchBoxId()).createSearchField(event -> searchBy(event.getText()));
            searchResetIcon = createSearchResetIcon();
            addButton = createAddButton();
        }
        closeButton = createCloseButton();
    }

    private void buildLayout() {
        final HorizontalLayout titleFilterIconsLayout = createHeaderFilterIconLayout();
        titleFilterIconsLayout.addComponents(headerCaptionLayout);

        if (isAllowSearch() && isRollout()) {
            titleFilterIconsLayout.addComponents(searchField, searchResetIcon);
            titleFilterIconsLayout.setExpandRatio(headerCaptionLayout, 0.3F);
            titleFilterIconsLayout.setExpandRatio(searchField, 0.7F);
        }
        if (hasCreatePermission() && isRollout()) {
            titleFilterIconsLayout.addComponent(addButton);
            titleFilterIconsLayout.setComponentAlignment(addButton, Alignment.TOP_LEFT);
        }
        if (showCloseButton()) {
            titleFilterIconsLayout.addComponent(closeButton);
            titleFilterIconsLayout.setComponentAlignment(closeButton, Alignment.TOP_RIGHT);
        }

        titleFilterIconsLayout.setHeight("40px");
        addComponent(titleFilterIconsLayout);
        addStyleName("bordered-layout");
        addStyleName("no-border-bottom");
    }

    private static HorizontalLayout createHeaderFilterIconLayout() {
        final HorizontalLayout titleFilterIconsLayout = new HorizontalLayout();
        titleFilterIconsLayout.addStyleName(SPUIStyleDefinitions.WIDGET_TITLE);
        titleFilterIconsLayout.setSpacing(false);
        titleFilterIconsLayout.setMargin(false);
        titleFilterIconsLayout.setSizeFull();
        return titleFilterIconsLayout;
    }

    private SPUIButton createSearchResetIcon() {
        final SPUIButton button = (SPUIButton) SPUIComponentProvider.getButton(getSearchRestIconId(), "", "", null,
                false, FontAwesome.SEARCH, SPUIButtonStyleSmallNoBorder.class);
        button.addClickListener(event -> onSearchResetClick());
        button.setData(Boolean.FALSE);
        return button;
    }

    private Button createAddButton() {
        final Button button = SPUIComponentProvider.getButton(getAddIconId(), "", "", null, false, FontAwesome.PLUS,
                SPUIButtonStyleSmallNoBorder.class);
        button.addClickListener(this::addNewItem);
        return button;
    }

    private Button createCloseButton() {
        final Button button = SPUIComponentProvider.getButton(getCloseButtonId(), "", "", null, false,
                FontAwesome.TIMES, SPUIButtonStyleSmallNoBorder.class);
        button.addClickListener(this::onClose);
        return button;
    }

    private void onSearchResetClick() {
        final Boolean flag = isSearchFieldOpen();
        if (flag == null || Boolean.FALSE.equals(flag)) {
            // Clicked on search Icon
            openSearchTextField();
        } else {
            // Clicked on rest icon
            closeSearchTextField();
        }
    }

    protected Boolean isSearchFieldOpen() {
        return (Boolean) searchResetIcon.getData();
    }

    private void openSearchTextField() {
        searchResetIcon.addStyleName(SPUIDefinitions.FILTER_RESET_ICON);
        searchResetIcon.toggleIcon(FontAwesome.TIMES);
        searchResetIcon.setData(Boolean.TRUE);
        searchField.removeStyleName(SPUIDefinitions.FILTER_BOX_HIDE);
        searchField.setVisible(true);
        searchField.focus();
    }

    private void closeSearchTextField() {
        searchField.setValue("");
        searchField.addStyleName(SPUIDefinitions.FILTER_BOX_HIDE);
        searchField.setVisible(false);
        searchResetIcon.removeStyleName(SPUIDefinitions.FILTER_RESET_ICON);
        searchResetIcon.toggleIcon(FontAwesome.SEARCH);
        searchResetIcon.setData(Boolean.FALSE);

        resetSearchText();
    }

    /**
     * This method will be called when user resets the search text means on
     * click of (X) icon.
     */
    protected abstract void resetSearchText();

    /**
     * get Id of search text field.
     * 
     * @return Id of the text field.
     */
    protected abstract String getSearchBoxId();

    /**
     * Get search reset Icon Id.
     * 
     * @return Id of search reset icon.
     */
    protected abstract String getSearchRestIconId();

    /**
     * On search by text.
     * 
     * @param newSearchText
     *            search text
     */
    protected abstract void searchBy(String newSearchText);

    /**
     * Get Id of add Icon.
     * 
     * @return String of add Icon.
     */
    protected abstract String getAddIconId();

    /**
     * On click add button.
     * 
     * @param event
     *            add button click event
     */
    protected abstract void addNewItem(final Button.ClickEvent event);

    /**
     * On click of close button.
     * 
     * @param event
     *            close button click event
     */
    protected abstract void onClose(final Button.ClickEvent event);

    /**
     * Checks create permission.
     * 
     * @return boolean Returns true if user has create permission
     */
    protected abstract boolean hasCreatePermission();

    /**
     * Get close button id.
     * 
     * @return String button id
     */
    protected abstract String getCloseButtonId();

    /**
     * Checks if close button to be displayed.
     *
     * @return true if close button has to be displayed
     */
    protected abstract boolean showCloseButton();

    /**
     * Checks if search is allowed.
     * 
     * @return boolean if true search field is displayed.
     */
    protected abstract boolean isAllowSearch();

    /**
     * Get search box on load text value.
     * 
     * @return value of search box.
     */
    protected abstract String onLoadSearchBoxValue();

    /**
     * Checks for the rollout group.
     * 
     * @return boolean value for Rollout Group.
     */
    protected abstract boolean isRollout();

    /**
     * Get header caption layout.
     * 
     * @return layout with caption
     */
    protected abstract HorizontalLayout getHeaderCaptionLayout();

    /**
     * Restore caption details on refresh.
     */
    protected abstract void restoreCaption();
}
