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
import com.vaadin.shared.ui.combobox.FilteringMode;
import com.vaadin.ui.ComboBox;

/**
 * Creates a combobox in order to select the distribution set for a target
 * filter query auto assignment.
 */
public class DistributionSetSelectComboBox extends ComboBox {
    private static final long serialVersionUID = 1L;

    private final VaadinMessageSource i18n;
    private String selectedValueCaption;
    private Long previousValue;
    private String lastFilterString;

    DistributionSetSelectComboBox(final VaadinMessageSource i18n) {
        super();
        this.i18n = i18n;

        init();
        initDataSource();
    }

    private void init() {
        setScrollToSelectedItem(false);
        setNullSelectionAllowed(false);
        setSizeFull();
        setId(UIComponentIdProvider.DIST_SET_SELECT_COMBO_ID);
        setCaption(i18n.getMessage(UIMessageIdProvider.HEADER_DISTRIBUTION_SET));
    }

    private void initDataSource() {
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

    /**
     * The custom QueryView implementation is only needed to modify the behavior
     * when removing the filter (do not refresh the container). In all other
     * cases the default LazyQueryView implementation is being reused.
     */
    private static class DistributionSetFilterQueryView implements QueryView {
        private final QueryView defaultQueryView;

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
        }

        @Override
        public void removeAllItems() {
            defaultQueryView.removeAllItems();
        }

        /**
         * Default implementation of the combobox removes the filter each time
         * it builds the options during repaint. However, container should not
         * be refreshed here (default LazyQueryView implementation), as this
         * would clear all filtered cache entries following by multiple database
         * queries.
         */
        @Override
        public void removeFilter(final Filter filter) {
            defaultQueryView.getQueryDefinition().removeFilter(filter);
            // no refresh here
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
    }

    /**
     * Overriden in order to get the selected distibution set's option caption
     * (name:version) from container and preventing multiple calls by saving the
     * selected Id.
     * 
     * @param selectedItemId
     *            the Id of the selected distribution set
     */
    @Override
    public void setValue(final Object selectedItemId) {
        if (selectedItemId != null) {
            // Can happen during validation, leading to multiple database
            // queries, in order to get the caption property
            if (selectedItemId.equals(previousValue)) {
                return;
            }
            selectedValueCaption = Optional.ofNullable(getContainerProperty(selectedItemId, getItemCaptionPropertyId()))
                    .map(Property::getValue).map(String.class::cast).orElse("");
        }

        super.setValue(selectedItemId);
        previousValue = (Long) selectedItemId;
    }

    /**
     * Overriden in order to return the caption for the selected distribution
     * set from cache. Otherwise, it could lead to multiple database queries,
     * trying to retrieve the caption from container, when it is not present in
     * filtered options.
     * 
     * @param itemId
     *            the Id of the selected distribution set
     * @return the option caption (name:version) of the selected distribution
     *         set
     */
    @Override
    public String getItemCaption(final Object itemId) {
        if (itemId != null && itemId.equals(getValue())) {
            return selectedValueCaption;
        }

        return super.getItemCaption(itemId);
    }

    /**
     * Overriden not to update the filter when the filterstring (value of
     * combobox input) was not changed. Otherwise, it would lead to additional
     * database requests during combobox page change while scrolling instead of
     * retreiving items from container cache.
     * 
     * @param filterString
     *            value of combobox input
     * @param filteringMode
     *            the filtering mode (starts_with, contains)
     * @return SimpleStringFilter to transfer filterstring in container
     */
    @Override
    protected Filter buildFilter(final String filterString, final FilteringMode filteringMode) {
        if (filterStringIsNotChanged(filterString)) {
            return null;
        }

        final Filter filter = super.buildFilter(filterString, filteringMode);

        refreshContainerIfFilterStringBecomesEmpty(filterString);

        lastFilterString = filterString;
        return filter;
    }

    private boolean filterStringIsNotChanged(final String filterString) {
        return !StringUtils.isEmpty(filterString) && !StringUtils.isEmpty(lastFilterString)
                && filterString.equals(lastFilterString);
    }

    private void refreshContainerIfFilterStringBecomesEmpty(final String filterString) {
        if (StringUtils.isEmpty(filterString) && !StringUtils.isEmpty(lastFilterString)) {
            refreshContainer();
        }
    }

    /**
     * Before setting the value of the selected distribution set we need to
     * initialize the container and apply the right filter in order to limit the
     * number of entities and save them in container cache. Otherwise, combobox
     * will try to find the corresponding id from container, leading to multiple
     * database queries.
     * 
     * @param initialFilterString
     *            value of initial distribution set caption (name:version)
     * @return the size of filtered options
     */
    public int setInitialValueFilter(final String initialFilterString) {
        final Filter filter = buildFilter(initialFilterString, getFilteringMode());

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

    /**
     * Refreshes the underlying container, clearing all the caches.
     */
    public void refreshContainer() {
        final LazyQueryContainer container = (LazyQueryContainer) getContainerDataSource();
        container.refresh();
    }
}
