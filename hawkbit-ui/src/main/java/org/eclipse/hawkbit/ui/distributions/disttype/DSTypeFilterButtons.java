/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.disttype;

import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.ui.common.DistributionSetTypeBeanQuery;
import org.eclipse.hawkbit.ui.common.filterlayout.AbstractFilterButtons;
import org.eclipse.hawkbit.ui.dd.criteria.DistributionsViewClientCriterion;
import org.eclipse.hawkbit.ui.distributions.event.DistributionSetTypeEvent;
import org.eclipse.hawkbit.ui.distributions.event.SaveActionWindowEvent;
import org.eclipse.hawkbit.ui.distributions.state.ManageDistUIState;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;

/**
 * Distribution Set Type filter buttons.
 */
public class DSTypeFilterButtons extends AbstractFilterButtons {

    private static final long serialVersionUID = 771251569981876005L;

    private final ManageDistUIState manageDistUIState;

    private final DistributionsViewClientCriterion distributionsViewClientCriterion;

    DSTypeFilterButtons(final UIEventBus eventBus, final ManageDistUIState manageDistUIState,
            final DistributionsViewClientCriterion distributionsViewClientCriterion,
            final DistributionSetTypeManagement distributionSetTypeManagement) {
        super(eventBus, new DSTypeFilterButtonClick(eventBus, manageDistUIState, distributionSetTypeManagement));
        this.manageDistUIState = manageDistUIState;
        this.distributionsViewClientCriterion = distributionsViewClientCriterion;
    }

    @Override
    protected String getButtonsTableId() {

        return UIComponentIdProvider.DISTRIBUTION_SET_TYPE_TABLE_ID;
    }

    @Override
    protected LazyQueryContainer createButtonsLazyQueryContainer() {
        return HawkbitCommonUtil.createLazyQueryContainer(new BeanQueryFactory<>(DistributionSetTypeBeanQuery.class));
    }

    @Override
    protected boolean isClickedByDefault(final String typeName) {
        return manageDistUIState.getManageDistFilters().getClickedDistSetType() != null
                && manageDistUIState.getManageDistFilters().getClickedDistSetType().getName().equals(typeName);
    }

    @Override
    protected String createButtonId(final String name) {

        return UIComponentIdProvider.DS_TYPE_FILTER_BTN_ID + name;
    }

    @Override
    protected DropHandler getFilterButtonDropHandler() {

        return new DropHandler() {
            private static final long serialVersionUID = 1L;

            @Override
            public AcceptCriterion getAcceptCriterion() {
                return distributionsViewClientCriterion;
            }

            @Override
            public void drop(final DragAndDropEvent event) {
                /* Not required */
            }
        };
    }

    @Override
    protected String getButtonWrapperData() {
        return null;
    }

    @Override
    protected String getButttonWrapperIdPrefix() {

        return SPUIDefinitions.DISTRIBUTION_SET_TYPE_ID_PREFIXS;
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final DistributionSetTypeEvent event) {
        if (event.getDistributionSetTypeEnum() == DistributionSetTypeEvent.DistributionSetTypeEnum.ADD_DIST_SET_TYPE
                || event.getDistributionSetTypeEnum() == DistributionSetTypeEvent.DistributionSetTypeEnum.UPDATE_DIST_SET_TYPE) {
            refreshTypeTable();
        }

    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final SaveActionWindowEvent event) {
        if (event == SaveActionWindowEvent.SAVED_DELETE_DIST_SET_TYPES) {
            refreshTypeTable();
        }

    }

    private void refreshTypeTable() {
        setContainerDataSource(createButtonsLazyQueryContainer());
    }
}
