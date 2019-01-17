/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.filtermanagement;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.hawkbit.ui.distributions.dstable.ManageDistBeanQuery;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.util.StringUtils;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.addons.lazyquerycontainer.LazyQueryDefinition;
import org.vaadin.addons.lazyquerycontainer.LazyQueryView;
import org.vaadin.addons.lazyquerycontainer.QueryDefinition;
import org.vaadin.addons.lazyquerycontainer.QueryView;

import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.util.filter.SimpleStringFilter;
import com.vaadin.shared.ui.combobox.FilteringMode;
import com.vaadin.ui.ComboBox;

public class DistributionSetSelectComboBox extends ComboBox {
    private static final long serialVersionUID = 1L;

    private final VaadinMessageSource i18n;
    private String selectedValueCaption;
    private Long previousValue;

    public void setSelectedValueCaption(final String selectedValueCaption) {
        this.selectedValueCaption = selectedValueCaption;
    }

    public String getSelectedValueCaption() {
        return selectedValueCaption;
    }

    @Override
    public void setValue(final Object selectedItemId) {
        if (selectedItemId != null) {
            // we do not want to set the same value multiple times during
            // validation, because it will lead to multiple database queries, in
            // order to get the caption property
            if (selectedItemId.equals(previousValue)) {
                return;
            }
            selectedValueCaption = Optional.ofNullable(getContainerProperty(selectedItemId, getItemCaptionPropertyId()))
                    .map(Property::getValue).map(String.class::cast).orElse(selectedValueCaption);
        }

        super.setValue(selectedItemId);
        previousValue = (Long) selectedItemId;
    }

    @Override
    public String getItemCaption(final Object itemId) {
        if (itemId != null && itemId.equals(getValue())) {
            return selectedValueCaption;
        }

        return super.getItemCaption(itemId);
    }

    DistributionSetSelectComboBox(final VaadinMessageSource i18n) {
        super();
        this.i18n = i18n;

        init();
        populateWithData();
    }

    private void init() {
        setScrollToSelectedItem(false);
        setNullSelectionAllowed(false);
        setSizeFull();
        setId(UIComponentIdProvider.DIST_SET_SELECT_COMBO_ID);
        setCaption(i18n.getMessage(UIMessageIdProvider.HEADER_DISTRIBUTION_SET));
    }

    private void populateWithData() {
        final Container container = createContainer();
        container.addContainerProperty(SPUILabelDefinitions.VAR_NAME_VERSION, String.class, null);

        setItemCaptionMode(ItemCaptionMode.PROPERTY);
        setItemCaptionPropertyId(SPUILabelDefinitions.VAR_NAME_VERSION);
        setFilteringMode(FilteringMode.CONTAINS);

        setContainerDataSource(container);
    }

    private Container createContainer() {
        final Map<String, Object> queryConfig = new HashMap<>();
        queryConfig.put(SPUIDefinitions.FILTER_BY_DS_COMPLETE, Boolean.TRUE);

        final BeanQueryFactory<ManageDistBeanQuery> distributionQF = new BeanQueryFactory<>(ManageDistBeanQuery.class);
        distributionQF.setQueryConfiguration(queryConfig);

        final LazyQueryDefinition distribtuinQD = new LazyQueryDefinition(false, SPUIDefinitions.PAGE_SIZE,
                SPUILabelDefinitions.VAR_ID);

        final QueryView distributionSetFilterLazyQueryView = new DistributionSetFilterQueryView(
                new LazyQueryView(distribtuinQD, distributionQF));
        distributionSetFilterLazyQueryView.sort(
                new Object[] { SPUILabelDefinitions.VAR_NAME, SPUILabelDefinitions.VAR_VERSION },
                new boolean[] { true, true });

        return new LazyQueryContainer(distributionSetFilterLazyQueryView);
    }

    private static class DistributionSetFilterQueryView implements QueryView {
        private final QueryView defaultQueryView;

        private String lastFilterString;

        DistributionSetFilterQueryView(final QueryView defaultQueryView) {
            this.defaultQueryView = defaultQueryView;
        }

        @Override
        public void addFilter(final Filter arg0) {
            defaultQueryView.addFilter(arg0);
        }

        @Override
        public int addItem() {
            return defaultQueryView.addItem();
        }

        @Override
        public void commit() {
            defaultQueryView.commit();
        }

        @Override
        public void discard() {
            defaultQueryView.discard();
        }

        @Override
        public List<Item> getAddedItems() {
            return defaultQueryView.getAddedItems();
        }

        @Override
        public Collection<Filter> getFilters() {
            return defaultQueryView.getFilters();
        }

        @Override
        public Item getItem(final int arg0) {
            return defaultQueryView.getItem(arg0);
        }

        @Override
        public List<?> getItemIdList() {
            return defaultQueryView.getItemIdList();
        }

        @Override
        public int getMaxCacheSize() {
            return defaultQueryView.getMaxCacheSize();
        }

        @Override
        public List<Item> getModifiedItems() {
            return defaultQueryView.getModifiedItems();
        }

        @Override
        public QueryDefinition getQueryDefinition() {
            return defaultQueryView.getQueryDefinition();
        }

        @Override
        public List<Item> getRemovedItems() {
            return defaultQueryView.getRemovedItems();
        }

        @Override
        public boolean isModified() {
            return defaultQueryView.isModified();
        }

        @Override
        public void refresh() {
            defaultQueryView.refresh();
            lastFilterString = null;
        }

        @Override
        public void removeAllItems() {
            defaultQueryView.removeAllItems();
        }

        // combobox removes filter after getting the filtered options, but we do
        // not want to call refresh() as in default queryView, because it
        // will clear all filtered cache entries and we would need to search
        // item properies (distribution set name and version) in database based
        // on ids. Additionally we remember the last filter string not to add
        // the same filter once more when the filter string was not changed
        @Override
        public void removeFilter(final Filter filter) {
            defaultQueryView.getQueryDefinition().removeFilter(filter);
            lastFilterString = Optional.ofNullable(filter).filter(SimpleStringFilter.class::isInstance)
                    .map(SimpleStringFilter.class::cast).map(SimpleStringFilter::getFilterString)
                    .orElse(lastFilterString);
        }

        @Override
        public void removeFilters() {
            defaultQueryView.removeFilters();
        }

        @Override
        public void removeItem(final int arg0) {
            defaultQueryView.removeItem(arg0);
        }

        @Override
        public void setMaxCacheSize(final int arg0) {
            defaultQueryView.setMaxCacheSize(arg0);
        }

        @Override
        public int size() {
            return defaultQueryView.size();
        }

        @Override
        public void sort(final Object[] arg0, final boolean[] arg1) {
            defaultQueryView.sort(arg0, arg1);
        }

        public String getLastFilterString() {
            return lastFilterString;
        }
    }

    @Override
    protected Filter buildFilter(final String filterString, final FilteringMode filteringMode) {
        final Filter filter = super.buildFilter(filterString, filteringMode);

        final LazyQueryContainer container = (LazyQueryContainer) getContainerDataSource();
        final DistributionSetFilterQueryView queryView = (DistributionSetFilterQueryView) container.getQueryView();
        final String lastFilterString = queryView.getLastFilterString();

        // we do not want to update the filter when the filterString (value of
        // combobox) was not changed, because it would lead to additional
        // database
        // requests during combobox page change while scrolling instead of
        // retreiving items from container cache
        if (filter != null && !StringUtils.isEmpty(lastFilterString) && filterString.equals(lastFilterString)) {
            return null;
        }

        // in order to refresh if the filterstring becomes empty
        if (filter == null && !StringUtils.isEmpty(lastFilterString)) {
            return new SimpleStringFilter(getItemCaptionPropertyId(), filterString, true, false);
        }

        return filter;
    }

    // before setting the value of the selected distribution set we need
    // to initialize the container and apply the right filter in order to limit
    // the number of entities and save them in container cache. If we do not do
    // this, combobox will try to find the corresponding id from container that
    // will lead to multiple database queries
    public int setInitialValueFilter(final String initialFilterString) {
        final Filter filter = super.buildFilter(initialFilterString, getFilteringMode());

        if (filter != null) {
            final LazyQueryContainer container = (LazyQueryContainer) getContainerDataSource();
            try {
                container.addContainerFilter(filter);
                return container.size();
            } finally {
                container.removeContainerFilter(filter);
            }
        }

        return 0;
    }

    public void refreshContainer() {
        final LazyQueryContainer container = (LazyQueryContainer) getContainerDataSource();
        container.refresh();
    }
}
