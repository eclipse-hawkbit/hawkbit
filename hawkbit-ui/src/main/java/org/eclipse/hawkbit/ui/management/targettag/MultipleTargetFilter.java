/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettag;

import javax.annotation.PostConstruct;

import org.eclipse.hawkbit.repository.SpPermissionChecker;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmallNoBorder;
import org.eclipse.hawkbit.ui.management.event.ManagementUIEvent;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;

import com.vaadin.server.FontAwesome;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Accordion;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.TabSheet.SelectedTabChangeListener;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Target filter tabsheet with 'simple' and 'complex' filter options.
 * 
 *
 *
 */
@SpringComponent
@ViewScope
public class MultipleTargetFilter extends Accordion implements SelectedTabChangeListener {

    private static final long serialVersionUID = -2887693289126893943L;

    @Autowired
    private TargetTagFilterButtons filterByButtons;

    @Autowired
    private TargetTagFilterButtonClick filterButtonClick;

    @Autowired
    private TargetFilterQueryButtons targetFilterQueryButtonsTab;

    @Autowired
    private FilterByStatusLayout filterByStatusFotter;

    @Autowired
    private CustomTargetTagFilterButtonClick customTargetTagFilterButtonClick;

    @Autowired
    private CreateUpdateTargetTagLayoutWindow createUpdateTargetTagLayout;

    @Autowired
    private SpPermissionChecker permChecker;

    @Autowired
    private ManagementUIState managementUIState;

    @Autowired
    private I18N i18n;

    @Autowired
    private transient EventBus.SessionEventBus eventBus;

    VerticalLayout simpleFilterTab;

    private Button config;

    /**
     * Intialize component.
     */
    @PostConstruct
    public void init() {
        filterByButtons.init(filterButtonClick);

        filterByButtons.addStyleName(SPUIStyleDefinitions.NO_TOP_BORDER);
        targetFilterQueryButtonsTab.init(customTargetTagFilterButtonClick);
        if (permChecker.hasCreateTargetPermission() || permChecker.hasUpdateTargetPermission()) {
            config = SPUIComponentProvider.getButton(UIComponentIdProvider.ADD_TARGET_TAG, "", "", "", true,
                    FontAwesome.COG, SPUIButtonStyleSmallNoBorder.class);
            config.addClickListener(event -> settingsIconClicked());
        }
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
        this.addTab(getSimpleFilterTab());
        this.addTab(getComplexFilterTab());
    }

    private Component getSimpleFilterTab() {
        simpleFilterTab = new VerticalLayout();
        final VerticalLayout targetTagTableLayout = new VerticalLayout();
        targetTagTableLayout.setSizeFull();
        if (null != config) {
            targetTagTableLayout.addComponent(config);
            targetTagTableLayout.setComponentAlignment(config, Alignment.TOP_RIGHT);
        }
        targetTagTableLayout.addComponent(filterByButtons);
        targetTagTableLayout.setComponentAlignment(filterByButtons, Alignment.MIDDLE_CENTER);
        targetTagTableLayout.addStyleName("target-tag-drop-hint");
        targetTagTableLayout.setExpandRatio(filterByButtons, 1.0F);
        simpleFilterTab.setCaption(i18n.get("caption.filter.simple"));
        simpleFilterTab.addComponent(targetTagTableLayout);
        simpleFilterTab.setExpandRatio(targetTagTableLayout, 1.0F);
        simpleFilterTab.addComponent(filterByStatusFotter);
        simpleFilterTab.setComponentAlignment(filterByStatusFotter, Alignment.MIDDLE_CENTER);
        simpleFilterTab.setSizeFull();
        simpleFilterTab.addStyleName(SPUIStyleDefinitions.SIMPLE_FILTER_HEADER);
        return simpleFilterTab;
    }

    private Component getComplexFilterTab() {
        targetFilterQueryButtonsTab.setCaption(i18n.get("caption.filter.custom"));
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
        if (i18n.get("caption.filter.simple").equals(getSelectedTab().getCaption())) {
            managementUIState.setCustomFilterSelected(false);
            eventBus.publish(this, ManagementUIEvent.RESET_TARGET_FILTER_QUERY);
        } else {
            managementUIState.setCustomFilterSelected(true);
            eventBus.publish(this, ManagementUIEvent.RESET_SIMPLE_FILTERS);
        }
    }

    protected void settingsIconClicked() {
        final Window addUpdateWindow = createUpdateTargetTagLayout.getWindow();
        UI.getCurrent().addWindow(addUpdateWindow);
        addUpdateWindow.setVisible(true);
    }

}
