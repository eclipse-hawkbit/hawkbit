/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.filtermanagement;

import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.builder.LabelBuilder;
import org.eclipse.hawkbit.ui.common.builder.TextFieldBuilder;
import org.eclipse.hawkbit.ui.components.SPUIButton;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmallNoBorder;
import org.eclipse.hawkbit.ui.filtermanagement.event.CustomFilterUIEvent;
import org.eclipse.hawkbit.ui.filtermanagement.state.FilterManagementUIState;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

/**
 * Layout for Custom Filter view
 */
public class TargetFilterHeader extends VerticalLayout {

    private static final long serialVersionUID = -7022704971955491673L;

    private final transient EventBus.UIEventBus eventBus;

    private final FilterManagementUIState filterManagementUIState;

    private final SpPermissionChecker permissionChecker;

    private Label headerCaption;

    private Button createfilterButton;

    private TextField searchField;

    private SPUIButton searchResetIcon;

    /**
     * Constructor for TargetFilterHeader
     * 
     * @param eventBus
     *            UIEventBus
     * @param filterManagementUIState
     *            FilterManagementUIState
     * @param permissionChecker
     *            SpPermissionChecker
     */
    public TargetFilterHeader(final UIEventBus eventBus, final FilterManagementUIState filterManagementUIState,
            final SpPermissionChecker permissionChecker) {
        this.eventBus = eventBus;
        this.filterManagementUIState = filterManagementUIState;
        this.permissionChecker = permissionChecker;

        createComponents();
        buildLayout();
    }

    private void createComponents() {
        headerCaption = createHeaderCaption();
        searchField = createSearchField();
        searchResetIcon = createSearchResetIcon();
        createfilterButton = createAddButton();
    }

    private static Label createHeaderCaption() {
        return new LabelBuilder().name(SPUIDefinitions.TARGET_FILTER_LIST_HEADER_CAPTION).buildCaptionLabel();
    }

    private void buildLayout() {
        final HorizontalLayout titleFilterIconsLayout = createHeaderFilterIconLayout();
        titleFilterIconsLayout.addComponents(headerCaption, searchField, searchResetIcon);
        if (permissionChecker.hasCreateTargetPermission()) {
            titleFilterIconsLayout.addComponent(createfilterButton);
            titleFilterIconsLayout.setComponentAlignment(createfilterButton, Alignment.TOP_LEFT);
        }
        titleFilterIconsLayout.setExpandRatio(headerCaption, 0.3F);
        titleFilterIconsLayout.setExpandRatio(searchField, 0.7F);
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

    private Button createAddButton() {
        final Button button = SPUIComponentProvider.getButton(UIComponentIdProvider.TARGET_FILTER_ADD_ICON_ID, "", "",
                null, false, FontAwesome.PLUS, SPUIButtonStyleSmallNoBorder.class);
        button.addClickListener(event -> addNewFilter());
        return button;
    }

    private void addNewFilter() {
        filterManagementUIState.setTfQuery(null);
        filterManagementUIState.setFilterQueryValue(null);
        filterManagementUIState.setCreateFilterBtnClicked(true);
        eventBus.publish(this, CustomFilterUIEvent.CREATE_NEW_FILTER_CLICK);
    }

    private TextField createSearchField() {
        final TextField campSearchTextField = new TextFieldBuilder(UIComponentIdProvider.TARGET_FILTER_SEARCH_TEXT)
                .createSearchField(event -> searchBy(event.getText()));
        campSearchTextField.setWidth(500.0F, Unit.PIXELS);
        return campSearchTextField;
    }

    protected void searchBy(final String newSearchText) {
        filterManagementUIState.setCustomFilterSearchText(newSearchText);
        eventBus.publish(this, CustomFilterUIEvent.FILTER_BY_CUST_FILTER_TEXT);
    }

    private SPUIButton createSearchResetIcon() {
        final SPUIButton button = (SPUIButton) SPUIComponentProvider.getButton(
                UIComponentIdProvider.TARGET_FILTER_TBL_SEARCH_RESET_ID, "", "", null, false, FontAwesome.SEARCH,
                SPUIButtonStyleSmallNoBorder.class);
        button.addClickListener(event -> onSearchResetClick());
        return button;
    }

    private void onSearchResetClick() {
        final Boolean flag = (Boolean) searchResetIcon.getData();
        if (flag == null || Boolean.FALSE.equals(flag)) {
            // Clicked on search Icon
            openSearchTextField();
        } else {
            // Clicked on rest icon
            closeSearchTextField();
        }
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
        searchResetIcon.removeStyleName(SPUIDefinitions.FILTER_RESET_ICON);
        searchResetIcon.toggleIcon(FontAwesome.SEARCH);
        searchResetIcon.setData(Boolean.FALSE);
        resetSearchText();

    }

    protected void resetSearchText() {
        filterManagementUIState.setCustomFilterSearchText(null);
        eventBus.publish(this, CustomFilterUIEvent.FILTER_BY_CUST_FILTER_TEXT_REMOVE);
    }

}
