/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.dstag;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.hawkbit.eventbus.event.DistributionSetTagCreatedBulkEvent;
import org.eclipse.hawkbit.eventbus.event.DistributionSetTagDeletedEvent;
import org.eclipse.hawkbit.eventbus.event.DistributionSetTagUpdateEvent;
import org.eclipse.hawkbit.repository.TagManagement;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.ui.common.filterlayout.AbstractFilterButtonClickBehaviour;
import org.eclipse.hawkbit.ui.common.filterlayout.AbstractFilterButtons;
import org.eclipse.hawkbit.ui.management.event.DistributionTagDropEvent;
import org.eclipse.hawkbit.ui.management.event.DragEvent;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.management.tag.TagIdName;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIComponetIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.data.Item;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.UI;

/**
 *
 *
 */
@SpringComponent
@ViewScope
public class DistributionTagButtons extends AbstractFilterButtons {

    private static final long serialVersionUID = -8151483237450892057L;

    @Autowired
    private DistributionTagDropEvent spDistTagDropEvent;

    @Autowired
    private TagManagement tagManagement;

    @Autowired
    private ManagementUIState managementUIState;

    @Override
    public void init(final AbstractFilterButtonClickBehaviour filterButtonClickBehaviour) {
        super.init(filterButtonClickBehaviour);
        addNewTag(tagManagement.generateDistributionSetTag("NO TAG"));
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    // Exception squid:S1172 - event not needed
    @SuppressWarnings({ "squid:S1172" })
    void onDistributionSetTagCreatedBulkEvent(final DistributionSetTagCreatedBulkEvent event) {
        refreshTagTable();
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    // Exception squid:S1172 - event not needed
    @SuppressWarnings({ "squid:S1172" })
    void onDistributionSetTagDeletedEvent(final DistributionSetTagDeletedEvent event) {
        refreshTagTable();
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    // Exception squid:S1172 - event not needed
    @SuppressWarnings({ "squid:S1172" })
    void onDistributionSetTagUpdateEvent(final DistributionSetTagUpdateEvent event) {
        refreshTagTable();
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final DragEvent dragEvent) {
        if (dragEvent == DragEvent.DISTRIBUTION_DRAG) {
            UI.getCurrent().access(() -> addStyleName(SPUIStyleDefinitions.SHOW_DROP_HINT_FILTER_BUTTON));
        } else {
            UI.getCurrent().access(() -> removeStyleName(SPUIStyleDefinitions.SHOW_DROP_HINT_FILTER_BUTTON));
        }
    }

    @Override
    protected String getButtonsTableId() {
        return SPUIComponetIdProvider.DISTRIBUTION_TAG_TABLE_ID;
    }

    @Override
    protected LazyQueryContainer createButtonsLazyQueryContainer() {
        final Map<String, Object> queryConfig = new HashMap<>();
        final BeanQueryFactory<DistributionTagBeanQuery> tagQF = new BeanQueryFactory<>(DistributionTagBeanQuery.class);
        tagQF.setQueryConfiguration(queryConfig);
        return HawkbitCommonUtil.createDSLazyQueryContainer(
                new BeanQueryFactory<DistributionTagBeanQuery>(DistributionTagBeanQuery.class));

    }

    @Override
    protected String getButtonWrapperData() {
        return SPUIDefinitions.DISTRIBUTION_TAG_BUTTON;
    }

    @Override
    protected boolean isClickedByDefault(final String tagName) {
        return null != managementUIState.getDistributionTableFilters().getDistSetTags()
                && managementUIState.getDistributionTableFilters().getDistSetTags().contains(tagName);
    }

    @Override
    protected boolean isNoTagSateSelected() {
        return managementUIState.getDistributionTableFilters().isNoTagSelected();
    }

    @Override
    protected String createButtonId(final String name) {
        return name;
    }

    @Override
    protected DropHandler getFilterButtonDropHandler() {
        return spDistTagDropEvent;
    }

    @Override
    protected String getButttonWrapperIdPrefix() {
        return SPUIDefinitions.DISTRIBUTION_TAG_ID_PREFIXS;
    }

    private void refreshTagTable() {
        ((LazyQueryContainer) getContainerDataSource()).refresh();
        removeGeneratedColumn(FILTER_BUTTON_COLUMN);
        addNewTag(tagManagement.generateDistributionSetTag("NO TAG"));
        addColumn();
    }

    private void addNewTag(final DistributionSetTag daTag) {
        final LazyQueryContainer targetTagContainer = (LazyQueryContainer) getContainerDataSource();
        final Object addItem = targetTagContainer.addItem();
        final Item item = targetTagContainer.getItem(addItem);

        item.getItemProperty(SPUILabelDefinitions.VAR_ID).setValue(daTag.getId());
        item.getItemProperty(SPUILabelDefinitions.VAR_NAME).setValue(daTag.getName());
        item.getItemProperty(SPUILabelDefinitions.VAR_DESC).setValue(daTag.getDescription());
        item.getItemProperty(SPUILabelDefinitions.VAR_COLOR).setValue(daTag.getColour());
        item.getItemProperty("tagIdName").setValue(new TagIdName(daTag.getName(), daTag.getId()));
    }
}
