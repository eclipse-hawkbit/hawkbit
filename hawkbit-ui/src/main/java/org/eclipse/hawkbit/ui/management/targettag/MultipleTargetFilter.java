/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettag;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.TargetTagManagement;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.components.ConfigMenuBar;
import org.eclipse.hawkbit.ui.dd.criteria.ManagementViewClientCriterion;
import org.eclipse.hawkbit.ui.management.event.ManagementUIEvent;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.ui.Accordion;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.TabSheet.SelectedTabChangeListener;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Target filter tabsheet with 'simple' and 'complex' filter options.
 */
public class MultipleTargetFilter extends Accordion implements SelectedTabChangeListener {

    private static final long serialVersionUID = 1L;

    private final TargetTagFilterButtons filterByButtons;

    private final TargetFilterQueryButtons targetFilterQueryButtonsTab;

    private final FilterByStatusLayout filterByStatusFooter;

    private final CustomTargetTagFilterButtonClick customTargetTagFilterButtonClick;

    private final SpPermissionChecker permChecker;

    private final ManagementUIState managementUIState;

    private final VaadinMessageSource i18n;

    private final transient EventBus.UIEventBus eventBus;

    private VerticalLayout simpleFilterTab;

    private ConfigMenuBar menu;

    private final UINotification uiNotification;

    private final transient EntityFactory entityFactory;

    private final transient TargetTagManagement targetTagManagement;

    MultipleTargetFilter(final SpPermissionChecker permChecker, final ManagementUIState managementUIState,
            final VaadinMessageSource i18n, final UIEventBus eventBus,
            final ManagementViewClientCriterion managementViewClientCriterion, final UINotification notification,
            final EntityFactory entityFactory, final TargetFilterQueryManagement targetFilterQueryManagement,
            final TargetTagManagement targetTagManagement) {
        this.filterByButtons = new TargetTagFilterButtons(eventBus, managementUIState, managementViewClientCriterion,
                i18n, notification, permChecker, entityFactory);
        this.targetFilterQueryButtonsTab = new TargetFilterQueryButtons(managementUIState, eventBus);
        this.filterByStatusFooter = new FilterByStatusLayout(i18n, eventBus, managementUIState);
        this.customTargetTagFilterButtonClick = new CustomTargetTagFilterButtonClick(eventBus, managementUIState,
                targetFilterQueryManagement);
        this.permChecker = permChecker;
        this.managementUIState = managementUIState;
        this.i18n = i18n;
        this.eventBus = eventBus;
        this.uiNotification = notification;
        this.entityFactory = entityFactory;
        this.targetTagManagement = targetTagManagement;
        buildComponents();
    }

    public TargetTagFilterButtons getFilterByButtons() {
        return filterByButtons;
    }

    /**
     * Intialize component.
     */
    private void buildComponents() {
        filterByStatusFooter.init();

        filterByButtons.addStyleName(SPUIStyleDefinitions.NO_TOP_BORDER);
        targetFilterQueryButtonsTab.init(customTargetTagFilterButtonClick);
        menu = new ConfigMenuBar(permChecker.hasCreateTargetPermission(), permChecker.hasUpdateTargetPermission(),
                permChecker.hasDeleteRepositoryPermission(), getAddButtonCommand(), getUpdateButtonCommand(),
                getDeleteButtonCommand());
        addStyleName(ValoTheme.ACCORDION_BORDERLESS);
        addTabs();
        setSizeFull();
        switchToTabSelectedOnLoad();
        addSelectedTabChangeListener(this);
    }

    /**
     *
     */
    private void switchToTabSelectedOnLoad() {
        if (managementUIState.isCustomFilterSelected()) {
            this.setSelectedTab(targetFilterQueryButtonsTab);
        } else {
            this.setSelectedTab(simpleFilterTab);
        }
    }

    /**
     * Add tabs.
     */
    private void addTabs() {
        this.addTab(getSimpleFilterTab()).setId(UIComponentIdProvider.SIMPLE_FILTER_ACCORDION_TAB);
        this.addTab(getComplexFilterTab()).setId(UIComponentIdProvider.CUSTOM_FILTER_ACCORDION_TAB);
    }

    private Component getSimpleFilterTab() {
        simpleFilterTab = new VerticalLayout();
        final VerticalLayout targetTagTableLayout = new VerticalLayout();
        targetTagTableLayout.setSizeFull();
        if (menu != null) {
            targetTagTableLayout.addComponent(menu);
            targetTagTableLayout.setComponentAlignment(menu, Alignment.TOP_RIGHT);
        }
        targetTagTableLayout.addComponent(filterByButtons);
        targetTagTableLayout.setComponentAlignment(filterByButtons, Alignment.MIDDLE_CENTER);
        targetTagTableLayout.setId(UIComponentIdProvider.TARGET_TAG_DROP_AREA_ID);
        targetTagTableLayout.setExpandRatio(filterByButtons, 1.0F);
        simpleFilterTab.setCaption(i18n.getMessage("caption.filter.simple"));
        simpleFilterTab.addComponent(targetTagTableLayout);
        simpleFilterTab.setExpandRatio(targetTagTableLayout, 1.0F);
        simpleFilterTab.addComponent(filterByStatusFooter);
        simpleFilterTab.setComponentAlignment(filterByStatusFooter, Alignment.MIDDLE_CENTER);
        simpleFilterTab.setSizeFull();
        simpleFilterTab.addStyleName(SPUIStyleDefinitions.SIMPLE_FILTER_HEADER);
        return simpleFilterTab;
    }

    private Component getComplexFilterTab() {
        targetFilterQueryButtonsTab.setCaption(i18n.getMessage("caption.filter.custom"));
        return targetFilterQueryButtonsTab;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.vaadin.ui.TabSheet.SelectedTabChangeListener#selectedTabChange(com
     * .vaadin.ui.TabSheet.SelectedTabChangeEvent)
     */
    @Override
    public void selectedTabChange(final SelectedTabChangeEvent event) {
        if (i18n.getMessage("caption.filter.simple").equals(getSelectedTab().getCaption())) {
            managementUIState.setCustomFilterSelected(false);
            eventBus.publish(this, ManagementUIEvent.RESET_TARGET_FILTER_QUERY);
        } else {
            managementUIState.setCustomFilterSelected(true);
            eventBus.publish(this, ManagementUIEvent.RESET_SIMPLE_FILTERS);
        }
    }

    protected Command getAddButtonCommand() {
        return new MenuBar.Command() {

            private static final long serialVersionUID = 1L;

            @Override
            public void menuSelected(final MenuItem selectedItem) {
                new CreateTargetTagLayoutw(i18n, targetTagManagement, entityFactory, eventBus, permChecker,
                        uiNotification);
            }
        };
    }

    protected Command getDeleteButtonCommand() {
        return new MenuBar.Command() {

            private static final long serialVersionUID = 1L;

            @Override
            public void menuSelected(final MenuItem selectedItem) {
                new DeleteTargetTagLayout(i18n, targetTagManagement, entityFactory, eventBus, permChecker,
                        uiNotification, managementUIState.getTargetTableFilters().getClickedTargetTags());
            }
        };
    }

    protected Command getUpdateButtonCommand() {
        return new MenuBar.Command() {

            private static final long serialVersionUID = 1L;

            @Override
            public void menuSelected(final MenuItem selectedItem) {
                new UpdateTargetTagLayout(i18n, targetTagManagement, entityFactory, eventBus, permChecker,
                        uiNotification);
            }
        };
    }

}
