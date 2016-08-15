/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.smtype;

import static org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleTypeEvent.SoftwareModuleTypeEnum.ADD_SOFTWARE_MODULE_TYPE;
import static org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleTypeEvent.SoftwareModuleTypeEnum.UPDATE_SOFTWARE_MODULE_TYPE;
import static org.eclipse.hawkbit.ui.utils.SPUIComponentIdProvider.SW_MODULE_TYPE_TABLE_ID;
import static org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions.VAR_NAME;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleTypeEvent;
import org.eclipse.hawkbit.ui.common.SoftwareModuleTypeBeanQuery;
import org.eclipse.hawkbit.ui.common.filterlayout.AbstractFilterButtons;
import org.eclipse.hawkbit.ui.distributions.event.DistributionsViewAcceptCriteria;
import org.eclipse.hawkbit.ui.distributions.event.SaveActionWindowEvent;
import org.eclipse.hawkbit.ui.distributions.state.ManageDistUIState;
import org.eclipse.hawkbit.ui.utils.SPUIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.addons.lazyquerycontainer.LazyQueryDefinition;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;

/**
 * Software Module Type filter buttons.
 */
@ViewScope
@SpringComponent
public class DistSMTypeFilterButtons extends AbstractFilterButtons {

    private static final long serialVersionUID = 6804534533362387433L;

    @Autowired
    private ManageDistUIState manageDistUIState;

    @Autowired
    private DistributionsViewAcceptCriteria distributionsViewAcceptCriteria;

    @Override
    protected String getButtonsTableId() {
        return SW_MODULE_TYPE_TABLE_ID;
    }

    @Override
    protected LazyQueryContainer createButtonsLazyQueryContainer() {
        final Map<String, Object> queryConfig = new HashMap<>();
        final BeanQueryFactory<SoftwareModuleTypeBeanQuery> typeQF = new BeanQueryFactory<>(
                SoftwareModuleTypeBeanQuery.class);
        typeQF.setQueryConfiguration(queryConfig);
        return new LazyQueryContainer(new LazyQueryDefinition(true, 20, VAR_NAME), typeQF);
    }

    @Override
    protected String getButtonWrapperData() {
        return null;
    }

    @Override
    protected boolean isClickedByDefault(final String typeName) {
        return manageDistUIState.getSoftwareModuleFilters().getSoftwareModuleType().isPresent() && manageDistUIState
                .getSoftwareModuleFilters().getSoftwareModuleType().get().getName().equals(typeName);
    }

    @Override
    protected String createButtonId(final String name) {
        return SPUIComponentIdProvider.SM_TYPE_FILTER_BTN_ID + name;
    }

    @Override
    protected DropHandler getFilterButtonDropHandler() {

        return new DropHandler() {
            private static final long serialVersionUID = 1L;

            @Override
            public AcceptCriterion getAcceptCriterion() {
                return distributionsViewAcceptCriteria;
            }

            @Override
            public void drop(final DragAndDropEvent event) {
                /* Not required */
            }
        };
    }

    @Override
    protected String getButttonWrapperIdPrefix() {
        return SPUIDefinitions.SOFTWARE_MODULE_TAG_ID_PREFIXS;
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final SoftwareModuleTypeEvent event) {
        if (isCreateOrUpdate(event) && event.getSoftwareModuleType() != null) {
            refreshTypeTable();
        }
    }

    private boolean isCreateOrUpdate(final SoftwareModuleTypeEvent event) {
        return event.getSoftwareModuleTypeEnum() == ADD_SOFTWARE_MODULE_TYPE
                || event.getSoftwareModuleTypeEnum() == UPDATE_SOFTWARE_MODULE_TYPE;
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final SaveActionWindowEvent event) {
        if (event == SaveActionWindowEvent.SAVED_DELETE_SW_MODULE_TYPES) {
            refreshTypeTable();
        }
    }

    private void refreshTypeTable() {
        setContainerDataSource(createButtonsLazyQueryContainer());
    }
}
