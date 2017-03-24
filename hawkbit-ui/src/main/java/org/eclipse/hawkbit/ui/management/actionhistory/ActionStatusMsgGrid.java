/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.actionhistory;

import java.util.Map;

import org.eclipse.hawkbit.ui.common.grid.AbstractGrid;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.addons.lazyquerycontainer.LazyQueryDefinition;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.google.common.collect.Maps;
import com.vaadin.data.Item;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.themes.ValoTheme;

/**
 * This grid presents the messages for a selected action-status.
 */
public class ActionStatusMsgGrid extends AbstractGrid<LazyQueryContainer> {
    private static final long serialVersionUID = 1L;

    private static final String[] rightAlignedColumns = new String[] { ProxyMessage.PXY_MSG_ID };

    private final String noMsgText;

    private final AlignCellStyleGenerator alignGenerator;

    private final BeanQueryFactory<ActionStatusMsgBeanQuery> targetQF = new BeanQueryFactory<>(
            ActionStatusMsgBeanQuery.class);

    /**
     * Constructor.
     *
     * @param i18n
     * @param eventBus
     */
    protected ActionStatusMsgGrid(final VaadinMessageSource i18n, final UIEventBus eventBus) {
        super(i18n, eventBus, null);
        noMsgText = createNoMessageProxy(i18n);

        setSingleSelectionSupport(new SingleSelectionSupport());
        setDetailsSupport(new DetailsSupport());

        alignGenerator = new AlignCellStyleGenerator(null, null, rightAlignedColumns);
        addStyleName(SPUIStyleDefinitions.ACTION_HISTORY_MESSAGE_GRID);

        setDetailsGenerator(new MessageDetailsGenerator());

        this.addItemClickListener(new ItemClickListener() {
            private static final long serialVersionUID = 1L;

            @Override
            public void itemClick(final ItemClickEvent event) {
                final Object itemId = event.getItemId();
                setDetailsVisible(itemId, !isDetailsVisible(itemId));
            }
        });

        init();
    }

    @Override
    protected LazyQueryContainer createContainer() {
        configureQueryFactory();
        return new LazyQueryContainer(new LazyQueryDefinition(true, SPUIDefinitions.PAGE_SIZE, null), targetQF);
    }

    @Override
    public void refreshContainer() {
        for (final Object itemId : getContainerDataSource().getItemIds()) {
            setDetailsVisible(itemId, false);
        }
        configureQueryFactory();
        super.refreshContainer();
    }

    protected void configureQueryFactory() {
        // ADD all the filters to the query config
        final Map<String, Object> queryConfig = Maps.newHashMapWithExpectedSize(2);
        queryConfig.put(SPUIDefinitions.MESSAGES_BY_ACTIONSTATUS, getDetailsSupport().getMasterDataId());
        queryConfig.put(SPUIDefinitions.NO_MSG_PROXY, noMsgText);
        // Create ActionBeanQuery factory with the query config.
        targetQF.setQueryConfiguration(queryConfig);
    }

    /**
     * Gets type-save access to LazyQueryContainer.
     *
     * @return LazyQueryContainer
     */
    private LazyQueryContainer getLazyQueryContainer() {
        return (LazyQueryContainer) getContainerDataSource();
    }

    @Override
    protected void addContainerProperties() {
        getLazyQueryContainer().addContainerProperty(ProxyMessage.PXY_MSG_ID, String.class, null, true, false);
        getLazyQueryContainer().addContainerProperty(ProxyMessage.PXY_MSG_VALUE, String.class, null, true, false);
    }

    @Override
    protected void setColumnExpandRatio() {
        getColumn(ProxyMessage.PXY_MSG_ID).setExpandRatio(0);
        getColumn(ProxyMessage.PXY_MSG_VALUE).setExpandRatio(1);
    }

    @Override
    protected void setColumnHeaderNames() {
        getColumn(ProxyMessage.PXY_MSG_ID).setHeaderCaption("##");
        getColumn(ProxyMessage.PXY_MSG_VALUE).setHeaderCaption(SPUIDefinitions.ACTION_HIS_TBL_MSGS);
    }

    @Override
    protected String getGridId() {
        return UIComponentIdProvider.ACTION_HISTORY_MESSAGE_GRID_ID;
    }

    @Override
    protected void setColumnProperties() {
        clearSortOrder();
        setColumns(ProxyMessage.PXY_MSG_ID, ProxyMessage.PXY_MSG_VALUE);
        setFrozenColumnCount(2);
        alignColumns();
    }

    @Override
    protected void addColumnRenderes() {
        // no specific column renderers
    }

    @Override
    protected void setHiddenColumns() {
        getColumn(ProxyMessage.PXY_MSG_ID).setHidable(false);
        getColumn(ProxyMessage.PXY_MSG_VALUE).setHidable(false);
    }

    @Override
    protected CellDescriptionGenerator getDescriptionGenerator() {
        return null;
    }

    /**
     * Sets the alignment cell-style-generator that handles the alignment for
     * the grid cells.
     */
    private void alignColumns() {
        setCellStyleGenerator(alignGenerator);
    }

    /**
     * Creates the default text when no message is available for action-status
     *
     * @param i18n
     * @return default text
     */
    private static String createNoMessageProxy(final VaadinMessageSource i18n) {
        return i18n.getMessage("message.no.available");
    }

    protected class MessageDetailsGenerator implements DetailsGenerator {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getDetails(final RowReference rowReference) {
            // Find the bean to generate details for
            final Item item = rowReference.getItem();
            final String message = (String) item.getItemProperty(ProxyMessage.PXY_MSG_VALUE).getValue();

            final TextArea textArea = new TextArea();
            textArea.addStyleName(ValoTheme.TEXTAREA_BORDERLESS);
            textArea.addStyleName(ValoTheme.TEXTAREA_TINY);
            textArea.addStyleName("inline-icon");
            textArea.setHeight(120, Unit.PIXELS);
            textArea.setWidth(100, Unit.PERCENTAGE);
            textArea.setValue(message);
            textArea.setReadOnly(Boolean.TRUE);
            return textArea;
        }
    }

    /**
     * CellStyleGenerator that concerns about cutting text.
     */
    protected static class TextCutCellStyleGenerator implements CellStyleGenerator {
        private static final long serialVersionUID = 1L;

        @Override
        public String getStyle(final CellReference cellReference) {
            if (ProxyMessage.PXY_MSG_VALUE.equals(cellReference.getPropertyId())) {
                return "text-cut";
            }
            return null;
        }
    }
}
