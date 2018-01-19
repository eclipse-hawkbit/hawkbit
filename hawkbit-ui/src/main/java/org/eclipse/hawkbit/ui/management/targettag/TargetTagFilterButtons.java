/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettag;

import java.util.List;
import java.util.Set;

import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.model.Tag;
import org.eclipse.hawkbit.repository.model.TargetTagAssignmentResult;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.filterlayout.AbstractFilterButtons;
import org.eclipse.hawkbit.ui.common.table.AbstractTable;
import org.eclipse.hawkbit.ui.components.RefreshableContainer;
import org.eclipse.hawkbit.ui.dd.criteria.ManagementViewClientCriterion;
import org.eclipse.hawkbit.ui.management.event.ManagementUIEvent;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.management.tag.TagIdName;
import org.eclipse.hawkbit.ui.management.targettable.TargetTable;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.data.Item;
import com.vaadin.event.Transferable;
import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.ui.Component;
import com.vaadin.ui.DragAndDropWrapper;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;

/**
 * Target Tag filter buttons table.
 */
public class TargetTagFilterButtons extends AbstractFilterButtons implements RefreshableContainer {
    private static final String NO_TAG = "NO TAG";

    private static final long serialVersionUID = 1L;

    private final ManagementUIState managementUIState;

    private final ManagementViewClientCriterion managementViewClientCriterion;

    private final VaadinMessageSource i18n;

    private final UINotification notification;

    private final SpPermissionChecker permChecker;

    private final transient EntityFactory entityFactory;

    TargetTagFilterButtons(final UIEventBus eventBus, final ManagementUIState managementUIState,
            final ManagementViewClientCriterion managementViewClientCriterion, final VaadinMessageSource i18n,
            final UINotification notification, final SpPermissionChecker permChecker,
            final EntityFactory entityFactory) {
        super(eventBus, new TargetTagFilterButtonClick(eventBus, managementUIState));
        this.managementUIState = managementUIState;
        this.managementViewClientCriterion = managementViewClientCriterion;
        this.i18n = i18n;
        this.notification = notification;
        this.permChecker = permChecker;
        this.entityFactory = entityFactory;

        addNewTargetTag(entityFactory.tag().create().name(NO_TAG).build());
    }

    @Override
    protected String getButtonsTableId() {
        return UIComponentIdProvider.TARGET_TAG_TABLE_ID;
    }

    @Override
    protected LazyQueryContainer createButtonsLazyQueryContainer() {
        return HawkbitCommonUtil.createDSLazyQueryContainer(new BeanQueryFactory<>(TargetTagBeanQuery.class));

    }

    @Override
    protected boolean isClickedByDefault(final String tagName) {
        return managementUIState.getTargetTableFilters().getClickedTargetTags() != null
                && managementUIState.getTargetTableFilters().getClickedTargetTags().contains(tagName);
    }

    @Override
    protected boolean isNoTagStateSelected() {
        return managementUIState.getTargetTableFilters().isNoTagSelected();
    }

    @Override
    protected String createButtonId(final String name) {
        if (NO_TAG.equals(name)) {
            return UIComponentIdProvider.NO_TAG_TARGET;
        }
        return name;
    }

    @Override
    protected DropHandler getFilterButtonDropHandler() {

        return new DropHandler() {
            private static final long serialVersionUID = 1L;

            @Override
            public AcceptCriterion getAcceptCriterion() {
                return managementViewClientCriterion;
            }

            @Override
            public void drop(final DragAndDropEvent event) {
                if (validate(event) && isNoTagAssigned(event)) {
                    final TableTransferable tbl = (TableTransferable) event.getTransferable();
                    final Table source = tbl.getSourceComponent();
                    if (source.getId().equals(UIComponentIdProvider.TARGET_TABLE_ID)) {
                        UI.getCurrent().access(() -> processTargetDrop(event));
                    }
                }
            }
        };
    }

    private Boolean isNoTagAssigned(final DragAndDropEvent event) {
        final String tagName = ((DragAndDropWrapper) (event.getTargetDetails().getTarget())).getData().toString();
        if (tagName.equals(SPUIDefinitions.TARGET_TAG_BUTTON)) {
            notification.displayValidationError(i18n.getMessage("message.tag.cannot.be.assigned",
                    new Object[] { i18n.getMessage("label.no.tag.assigned") }));
            return false;
        }
        return true;
    }

    /**
     * Validate the drop.
     *
     * @param event
     *            DragAndDropEvent reference
     * @return Boolean
     */
    private Boolean validate(final DragAndDropEvent event) {
        final Transferable transferable = event.getTransferable();
        final Component compsource = transferable.getSourceComponent();
        if (!(compsource instanceof AbstractTable)) {
            notification.displayValidationError(i18n.getMessage(SPUILabelDefinitions.ACTION_NOT_ALLOWED));
            return false;
        }

        final TableTransferable tabletransferable = (TableTransferable) transferable;

        final AbstractTable<?> source = (AbstractTable<?>) tabletransferable.getSourceComponent();

        if (!validateIfSourceisTargetTable(source) && !hasTargetUpdatePermission()) {
            return false;
        }

        final Set<Long> deletedEntityByTransferable = source.getDeletedEntityByTransferable(tabletransferable);
        if (deletedEntityByTransferable.isEmpty()) {
            final String actionDidNotWork = i18n.getMessage("message.action.did.not.work", new Object[] {});
            notification.displayValidationError(actionDidNotWork);
            return false;
        }

        return true;
    }

    /**
     * validate the update permission.
     *
     * @return boolean
     */
    private boolean hasTargetUpdatePermission() {
        if (!permChecker.hasUpdateTargetPermission()) {
            notification.displayValidationError(
                    i18n.getMessage("message.permission.insufficient", SpPermission.UPDATE_TARGET));
            return false;
        }

        return true;
    }

    private void processTargetDrop(final DragAndDropEvent event) {
        final com.vaadin.event.dd.TargetDetails targetDetails = event.getTargetDetails();
        final TableTransferable transferable = (TableTransferable) event.getTransferable();

        final TargetTable targetTable = (TargetTable) transferable.getSourceComponent();
        final Set<Long> targetList = targetTable.getDeletedEntityByTransferable(transferable);
        final String targTagName = HawkbitCommonUtil.removePrefix(targetDetails.getTarget().getId(),
                SPUIDefinitions.TARGET_TAG_ID_PREFIXS);

        if (!hasTargetUpdatePermission()) {
            return;
        }

        final TargetTagAssignmentResult result = targetTable.toggleTagAssignment(targetList, targTagName);

        publishAssignTargetTagEvent(result);

        publishUnAssignTargetTagEvent(targTagName, result);

    }

    private void publishUnAssignTargetTagEvent(final String targTagName, final TargetTagAssignmentResult result) {
        final List<String> tagsClickedList = managementUIState.getTargetTableFilters().getClickedTargetTags();
        final boolean isTargetTagUnAssigned = result.getUnassigned() >= 1 && !tagsClickedList.isEmpty()
                && tagsClickedList.contains(targTagName);

        if (!isTargetTagUnAssigned) {
            return;
        }
        eventBus.publish(this, ManagementUIEvent.UNASSIGN_TARGET_TAG);
    }

    private void publishAssignTargetTagEvent(final TargetTagAssignmentResult result) {
        final boolean isNewTargetTagAssigned = result.getAssigned() >= 1
                && managementUIState.getTargetTableFilters().isNoTagSelected();
        if (!isNewTargetTagAssigned) {
            return;
        }
        eventBus.publish(this, ManagementUIEvent.ASSIGN_TARGET_TAG);
    }

    private boolean validateIfSourceisTargetTable(final Table source) {
        if (!source.getId().equals(UIComponentIdProvider.TARGET_TABLE_ID)) {
            notification.displayValidationError(i18n.getMessage(SPUILabelDefinitions.ACTION_NOT_ALLOWED));
            return false;
        }
        return true;
    }

    @Override
    protected String getButttonWrapperIdPrefix() {

        return SPUIDefinitions.TARGET_TAG_ID_PREFIXS;
    }

    @Override
    public void refreshContainer() {
        removeGeneratedColumn(FILTER_BUTTON_COLUMN);
        ((LazyQueryContainer) getContainerDataSource()).refresh();
        addNewTargetTag(entityFactory.tag().create().name(NO_TAG).build());
        addColumn();
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final ManagementUIEvent event) {
        if (event == ManagementUIEvent.RESET_SIMPLE_FILTERS
                && !managementUIState.getTargetTableFilters().getClickedTargetTags().isEmpty()) {
            ((TargetTagFilterButtonClick) filterButtonClickBehaviour).clearTargetTagFilters();
        }
    }

    @SuppressWarnings("unchecked")
    private void addNewTargetTag(final Tag newTargetTag) {
        final LazyQueryContainer targetTagContainer = (LazyQueryContainer) getContainerDataSource();
        final Object addItem = targetTagContainer.addItem();
        final Item item = targetTagContainer.getItem(addItem);
        item.getItemProperty(SPUILabelDefinitions.VAR_ID).setValue(newTargetTag.getId());
        item.getItemProperty(SPUILabelDefinitions.VAR_NAME).setValue(newTargetTag.getName());
        item.getItemProperty(SPUILabelDefinitions.VAR_DESC).setValue(newTargetTag.getDescription());
        item.getItemProperty(SPUILabelDefinitions.VAR_COLOR).setValue(newTargetTag.getColour());
        item.getItemProperty("tagIdName").setValue(new TagIdName(newTargetTag.getName(), newTargetTag.getId()));
    }

    @Override
    protected String getButtonWrapperData() {
        return SPUIDefinitions.TARGET_TAG_BUTTON;
    }
}
