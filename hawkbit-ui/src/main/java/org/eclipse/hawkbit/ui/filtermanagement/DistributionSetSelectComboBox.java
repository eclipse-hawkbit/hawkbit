/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.filtermanagement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.hawkbit.repository.event.remote.DistributionSetDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetCreatedEvent;
import org.eclipse.hawkbit.ui.distributions.dstable.ManageDistBeanQuery;
import org.eclipse.hawkbit.ui.distributions.state.ManageDistUIState;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.addons.lazyquerycontainer.LazyQueryDefinition;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.data.Container;
import com.vaadin.shared.ui.combobox.FilteringMode;
import com.vaadin.ui.ComboBox;

public class DistributionSetSelectComboBox extends ComboBox {
    private static final long serialVersionUID = 1L;
    // private static final Logger LOG =
    // LoggerFactory.getLogger(DistributionSetSelectComboBox.class);

    private final VaadinMessageSource i18n;
    private final ManageDistUIState manageDistUIState;

    DistributionSetSelectComboBox(final VaadinMessageSource i18n, final UIEventBus eventBus,
            final ManageDistUIState manageDistUIState) {
        super();
        this.i18n = i18n;
        this.manageDistUIState = manageDistUIState;

        setNullSelectionAllowed(false);
        setSizeFull();
        setId(UIComponentIdProvider.DIST_SET_SELECT_TABLE_ID);
        setCaption("Distribution Set:");

        populateWithData();
        eventBus.subscribe(this);
    }

    private void populateWithData() {
        final Container container = createContainer();
        container.addContainerProperty(SPUILabelDefinitions.NAME, String.class, null);

        setItemCaptionMode(ItemCaptionMode.PROPERTY);
        setItemCaptionPropertyId(SPUILabelDefinitions.NAME);
        setFilteringMode(FilteringMode.CONTAINS);

        setContainerDataSource(container);
    }

    private Container createContainer() {
        final Map<String, Object> queryConfig = new HashMap<>();
        queryConfig.put(SPUIDefinitions.FILTER_BY_DS_COMPLETE, Boolean.TRUE);

        final BeanQueryFactory<ManageDistBeanQuery> distributionQF = new BeanQueryFactory<>(ManageDistBeanQuery.class);
        distributionQF.setQueryConfiguration(queryConfig);

        return new LazyQueryContainer(
                new LazyQueryDefinition(true, SPUIDefinitions.PAGE_SIZE, SPUILabelDefinitions.VAR_ID), distributionQF);
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvents(final List<?> events) {
        final Object firstEvent = events.get(0);
        if (DistributionSetCreatedEvent.class.isInstance(firstEvent)
                || DistributionSetDeletedEvent.class.isInstance(firstEvent)) {
            refreshDistributions();
        }
    }

    private void refreshDistributions() {
        final LazyQueryContainer dsContainer = (LazyQueryContainer) getContainerDataSource();
        final int size = dsContainer.size();
        if (size < SPUIDefinitions.MAX_TABLE_ENTRIES) {
            dsContainer.refresh();
            setValue(getItemIdToSelect());
        }
        if (size != 0) {
            setData(i18n.getMessage(UIMessageIdProvider.MESSAGE_DATA_AVAILABLE));
        }
    }

    private Object getItemIdToSelect() {
        return manageDistUIState.getSelectedDistributions().isEmpty() ? null
                : manageDistUIState.getSelectedDistributions();
    }
}
