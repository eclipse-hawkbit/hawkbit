/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.dstag;

import java.util.Collections;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.model.Tag;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.filterlayout.AbstractFilterButtons;
import org.eclipse.hawkbit.ui.components.RefreshableContainer;
import org.eclipse.hawkbit.ui.dd.criteria.ManagementViewClientCriterion;
import org.eclipse.hawkbit.ui.management.event.DistributionTagDropEvent;
import org.eclipse.hawkbit.ui.management.state.DistributionTableFilters;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.management.tag.TagIdName;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.data.Item;
import com.vaadin.event.dd.DropHandler;

/**
 * Class for defining the tag buttons of the distribution sets on the Deployment
 * View.
 */
public class DistributionTagButtons extends AbstractFilterButtons implements RefreshableContainer {

    private static final long serialVersionUID = 1L;

    private static final String NO_TAG = "NO TAG";

    private final DistributionTagDropEvent spDistTagDropEvent;

    private final ManagementUIState managementUIState;

    private final transient EntityFactory entityFactory;

    DistributionTagButtons(final UIEventBus eventBus, final ManagementUIState managementUIState,
            final EntityFactory entityFactory, final VaadinMessageSource i18n, final UINotification notification,
            final SpPermissionChecker permChecker, final DistributionTableFilters distFilterParameters,
            final DistributionSetManagement distributionSetManagement,
            final ManagementViewClientCriterion managementViewClientCriterion) {
        super(eventBus, new DistributionTagButtonClick(eventBus, managementUIState));
        this.spDistTagDropEvent = new DistributionTagDropEvent(i18n, notification, permChecker, distFilterParameters,
                distributionSetManagement, eventBus, managementViewClientCriterion);
        this.managementUIState = managementUIState;
        this.entityFactory = entityFactory;

        if (permChecker.hasReadRepositoryPermission()) {
            addNewTag(entityFactory.tag().create().name(NO_TAG).build());
        }
    }

    @Override
    protected String getButtonsTableId() {
        return UIComponentIdProvider.DISTRIBUTION_TAG_TABLE_ID;
    }

    @Override
    protected LazyQueryContainer createButtonsLazyQueryContainer() {
        final BeanQueryFactory<DistributionTagBeanQuery> tagQF = new BeanQueryFactory<>(DistributionTagBeanQuery.class);
        tagQF.setQueryConfiguration(Collections.emptyMap());
        return HawkbitCommonUtil.createDSLazyQueryContainer(new BeanQueryFactory<>(DistributionTagBeanQuery.class));
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
    protected boolean isNoTagStateSelected() {
        return managementUIState.getDistributionTableFilters().isNoTagSelected();
    }

    @Override
    protected String createButtonId(final String name) {
        if (NO_TAG.equals(name)) {
            return UIComponentIdProvider.NO_TAG_DISTRIBUTION_SET;
        }
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

    private void addNewTag(final Tag daTag) {
        final LazyQueryContainer targetTagContainer = (LazyQueryContainer) getContainerDataSource();
        final Object addItem = targetTagContainer.addItem();
        final Item item = targetTagContainer.getItem(addItem);

        item.getItemProperty(SPUILabelDefinitions.VAR_ID).setValue(daTag.getId());
        item.getItemProperty(SPUILabelDefinitions.VAR_NAME).setValue(daTag.getName());
        item.getItemProperty(SPUILabelDefinitions.VAR_DESC).setValue(daTag.getDescription());
        item.getItemProperty(SPUILabelDefinitions.VAR_COLOR).setValue(daTag.getColour());
        item.getItemProperty("tagIdName").setValue(new TagIdName(daTag.getName(), daTag.getId()));
    }

    @Override
    public void refreshContainer() {
        ((LazyQueryContainer) getContainerDataSource()).refresh();
        removeGeneratedColumn(FILTER_BUTTON_COLUMN);
        addNewTag(entityFactory.tag().create().name(NO_TAG).build());
        addColumn();
    }
}
