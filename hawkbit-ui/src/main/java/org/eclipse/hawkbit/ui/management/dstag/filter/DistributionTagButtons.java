/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.dstag.filter;

import java.util.Collections;
import java.util.Optional;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.Tag;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.event.DistributionSetTagFilterHeaderEvent;
import org.eclipse.hawkbit.ui.common.event.FilterHeaderEvent.FilterHeaderEnum;
import org.eclipse.hawkbit.ui.common.filterlayout.AbstractFilterButtons;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.dd.criteria.ManagementViewClientCriterion;
import org.eclipse.hawkbit.ui.management.dstag.UpdateDistributionSetTagLayout;
import org.eclipse.hawkbit.ui.management.event.DistributionSetTagTableEvent;
import org.eclipse.hawkbit.ui.management.event.DistributionTagDropEvent;
import org.eclipse.hawkbit.ui.management.state.DistributionTableFilters;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.management.tag.TagIdName;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.data.Item;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.ui.Button.ClickEvent;

/**
 * Class for defining the tag buttons of the distribution sets on the Deployment
 * View.
 */
public class DistributionTagButtons extends AbstractFilterButtons {

    private static final long serialVersionUID = 1L;

    private final DistributionTagDropEvent spDistTagDropEvent;

    private final ManagementUIState managementUIState;

    private final transient EntityFactory entityFactory;

    private final SpPermissionChecker permChecker;

    private final UINotification uiNotification;

    private final transient DistributionSetTagManagement distributionSetTagManagement;

    public DistributionTagButtons(final UIEventBus eventBus, final ManagementUIState managementUIState,
            final EntityFactory entityFactory, final VaadinMessageSource i18n, final UINotification uiNotification,
            final SpPermissionChecker permChecker, final DistributionTableFilters distFilterParameters,
            final DistributionSetManagement distributionSetManagement,
            final ManagementViewClientCriterion managementViewClientCriterion,
            final DistributionSetTagManagement distributionSetTagManagement) {
        super(eventBus, new DistributionTagButtonClick(eventBus, managementUIState), i18n);
        this.spDistTagDropEvent = new DistributionTagDropEvent(i18n, uiNotification, permChecker, distFilterParameters,
                distributionSetManagement, eventBus, managementViewClientCriterion);
        this.managementUIState = managementUIState;
        this.entityFactory = entityFactory;
        this.permChecker = permChecker;
        this.uiNotification = uiNotification;
        this.distributionSetTagManagement = distributionSetTagManagement;

        if (permChecker.hasReadRepositoryPermission()) {
            addNewTag(entityFactory.tag().create().name(getNoTagLabel()).build());
        }
    }

    @Override
    protected boolean doSubscribeToEventBus() {
        return false;
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
        return getI18n().getMessage(UIMessageIdProvider.CAPTION_DISTRIBUTION_TAG);
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
        if (getNoTagLabel().equals(name)) {
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
    public void refreshTable() {
        ((LazyQueryContainer) getContainerDataSource()).refresh();
        removeGeneratedColumn(FILTER_BUTTON_COLUMN);
        removeUpdateAndDeleteColumn();
        addNewTag(entityFactory.tag().create().name(getNoTagLabel()).build());
        addColumn();
    }

    @Override
    protected void addEditButtonClickListener(final ClickEvent event) {
        new UpdateDistributionSetTagLayout(getI18n(), distributionSetTagManagement, entityFactory, getEventBus(),
                permChecker, uiNotification, getEntityId(event), getCloseListenerForEditAndDeleteTag(
                        new DistributionSetTagFilterHeaderEvent(FilterHeaderEnum.SHOW_MENUBAR)));
    }

    @Override
    protected void addDeleteButtonClickListener(final ClickEvent event) {
        openConfirmationWindowForDeletion(getEntityId(event),
                getI18n().getMessage(UIMessageIdProvider.CAPTION_DISTRIBUTION_TAG),
                new DistributionSetTagFilterHeaderEvent(FilterHeaderEnum.SHOW_MENUBAR));
    }

    @Override
    protected void deleteEntity(final String entityToDelete) {
        final Optional<DistributionSetTag> tagToDelete = distributionSetTagManagement.getByName(entityToDelete);
        tagToDelete.ifPresent(tag -> {
            if (managementUIState.getDistributionTableFilters().getDistSetTags().contains(entityToDelete)) {
                uiNotification.displayValidationError(getI18n().getMessage("message.tag.delete", entityToDelete));
                removeUpdateAndDeleteColumn();
            } else {
                distributionSetTagManagement.delete(entityToDelete);
                getEventBus().publish(this, new DistributionSetTagTableEvent(BaseEntityEventType.REMOVE_ENTITY, tag));
                uiNotification.displaySuccess(getI18n().getMessage("message.delete.success", entityToDelete));
            }
        });
    }
}
