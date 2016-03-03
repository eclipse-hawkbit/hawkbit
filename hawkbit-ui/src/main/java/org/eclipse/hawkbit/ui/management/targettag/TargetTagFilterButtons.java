/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettag;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PreDestroy;

import org.eclipse.hawkbit.eventbus.event.TargetTagCreatedBulkEvent;
import org.eclipse.hawkbit.eventbus.event.TargetTagDeletedEvent;
import org.eclipse.hawkbit.eventbus.event.TargetTagUpdateEvent;
import org.eclipse.hawkbit.repository.SpPermissionChecker;
import org.eclipse.hawkbit.repository.TagManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.model.TargetIdName;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.model.TargetTagAssigmentResult;
import org.eclipse.hawkbit.ui.common.filterlayout.AbstractFilterButtons;
import org.eclipse.hawkbit.ui.management.event.DragEvent;
import org.eclipse.hawkbit.ui.management.event.ManagementUIEvent;
import org.eclipse.hawkbit.ui.management.event.ManagementViewAcceptCriteria;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.management.tag.TagIdName;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIComponetIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.data.Item;
import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.server.Page;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Component;
import com.vaadin.ui.DragAndDropWrapper;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;

/**
 * Target Tag filter buttons table.
 */
@SpringComponent
@ViewScope
public class TargetTagFilterButtons extends AbstractFilterButtons {

    private static final long serialVersionUID = 5049554600376508073L;

    @Autowired
    private transient EventBus.SessionEventBus eventBus;

    @Autowired
    private ManagementUIState managementUIState;

    @Autowired
    private transient TagManagement tagMgmtService;

    @Autowired
    private ManagementViewAcceptCriteria managementViewAcceptCriteria;

    @Autowired
    private I18N i18n;

    @Autowired
    private transient UINotification notification;

    @Autowired
    private SpPermissionChecker permChecker;

    @Autowired
    private transient TargetManagement targetManagement;

    private static final String ITEMID = "itemId";

    TargetTagFilterButtonClick filterButtonClickBehaviour;

    /**
     * Initialize component.
     * 
     * @param filterButtonClickBehaviour
     *            the clickable behaviour.
     */

    public void init(final TargetTagFilterButtonClick filterButtonClickBehaviour) {
        this.filterButtonClickBehaviour = filterButtonClickBehaviour;
        super.init(filterButtonClickBehaviour);
        addNewTargetTag(new TargetTag("NO TAG"));
        eventBus.subscribe(this);
    }

    @PreDestroy
    void destroy() {
        eventBus.unsubscribe(this);
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final DragEvent dragEvent) {
        /*
         * this has some flickering issue for addStyleName(
         * "show-filter-drop-hint" ); Hence, doing with Javascripts
         */
        if (dragEvent == DragEvent.TARGET_DRAG) {
            UI.getCurrent().access(
                    () -> Page.getCurrent().getJavaScript().execute(HawkbitCommonUtil.dispTargetTagsDropHintScript()));
        } else {
            UI.getCurrent().access(
                    () -> Page.getCurrent().getJavaScript().execute(HawkbitCommonUtil.hideTargetTagsDropHintScript()));
        }
    }

    @Override
    protected String getButtonsTableId() {
        return SPUIComponetIdProvider.TARGET_TAG_TABLE_ID;
    }

    @Override
    protected LazyQueryContainer createButtonsLazyQueryContainer() {
        return HawkbitCommonUtil
                .createDSLazyQueryContainer(new BeanQueryFactory<TargetTagBeanQuery>(TargetTagBeanQuery.class));

    }

    @Override
    protected boolean isClickedByDefault(final Long buttonId) {
        final TargetTag newTagClickedObj = tagMgmtService.findTargetTagById(buttonId);
        return managementUIState.getTargetTableFilters().getClickedTargetTags() != null && managementUIState
                .getTargetTableFilters().getClickedTargetTags().contains(newTagClickedObj.getName());
    }

    @Override
    protected boolean isNoTagSateSelected() {
        return managementUIState.getTargetTableFilters().isNoTagSelected();
    }

    @Override
    protected String createButtonId(final String name) {

        return name;
    }

    @Override
    protected DropHandler getFilterButtonDropHandler() {

        return new DropHandler() {
            private static final long serialVersionUID = 1L;

            @Override
            public AcceptCriterion getAcceptCriterion() {
                return managementViewAcceptCriteria;
            }

            @Override
            public void drop(final DragAndDropEvent event) {
                if (validate(event) && isNoTagAssigned(event)) {
                    final TableTransferable tbl = (TableTransferable) event.getTransferable();
                    final Table source = tbl.getSourceComponent();
                    if (source.getId().equals(SPUIComponetIdProvider.TARGET_TABLE_ID)) {
                        processTargetDrop(event);
                    }
                }
            }
        };
    }

    private Boolean isNoTagAssigned(final DragAndDropEvent event) {
        final String tagName = ((DragAndDropWrapper) (event.getTargetDetails().getTarget())).getData().toString();
        if (tagName.equals(SPUIDefinitions.TARGET_TAG_BUTTON)) {
            notification.displayValidationError(
                    i18n.get("message.tag.cannot.be.assigned", new Object[] { i18n.get("label.no.tag.assigned") }));
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
        final Component compsource = event.getTransferable().getSourceComponent();
        if (!(compsource instanceof Table)) {

            notification.displayValidationError(i18n.get(SPUILabelDefinitions.ACTION_NOT_ALLOWED));
            return false;
        } else {
            final Table source = ((TableTransferable) event.getTransferable()).getSourceComponent();

            if (!validateIfSourceisTargetTable(source) && !checkForTargetUpdatePermission()) {
                return false;
            }
        }
        return true;
    }

    /**
     * validate the update permission.
     *
     * @return boolean
     */
    private boolean checkForTargetUpdatePermission() {
        if (!permChecker.hasUpdateTargetPermission()) {

            notification.displayValidationError(i18n.get("message.permission.insufficient"));
            return false;
        }

        return true;
    }

    private void processTargetDrop(final DragAndDropEvent event) {

        final com.vaadin.event.dd.TargetDetails targetDetails = event.getTargetDetails();
        final TableTransferable transferable = (TableTransferable) event.getTransferable();
        final Table source = transferable.getSourceComponent();

        final Set<TargetIdName> targetSelected = HawkbitCommonUtil.getSelectedTargetDetails(source);
        final Set<String> targetList = new HashSet<>();
        if (transferable.getData(ITEMID) != null) {
            if (!targetSelected.contains(transferable.getData(ITEMID))) {
                targetList.add(((TargetIdName) transferable.getData(ITEMID)).getControllerId());
            } else {
                targetList.addAll(targetSelected.stream().map(t -> t.getControllerId()).collect(Collectors.toList()));
            }

            final String targTagName = HawkbitCommonUtil.removePrefix(targetDetails.getTarget().getId(),
                    SPUIDefinitions.TARGET_TAG_ID_PREFIXS);

            final List<String> tagsClickedList = managementUIState.getTargetTableFilters().getClickedTargetTags();

            final TargetTagAssigmentResult result = targetManagement.toggleTagAssignment(targetList, targTagName);
            notification.displaySuccess(HawkbitCommonUtil.getTargetTagAssigmentMsg(targTagName, result, i18n));

            if (result.getAssigned() >= 1 && managementUIState.getTargetTableFilters().isNoTagSelected()) {
                eventBus.publish(this, ManagementUIEvent.ASSIGN_TARGET_TAG);
            }
            if (result.getUnassigned() >= 1 && !tagsClickedList.isEmpty() && tagsClickedList.contains(targTagName)) {
                eventBus.publish(this, ManagementUIEvent.UNASSIGN_TARGET_TAG);
            }
        }
    }

    private boolean validateIfSourceisTargetTable(final Table source) {
        if (!source.getId().equals(SPUIComponetIdProvider.TARGET_TABLE_ID)) {
            notification.displayValidationError(i18n.get(SPUILabelDefinitions.ACTION_NOT_ALLOWED));
            return false;
        }
        return true;
    }

    @Override
    protected String getButttonWrapperIdPrefix() {

        return SPUIDefinitions.TARGET_TAG_ID_PREFIXS;
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final TargetTagUpdateEvent event) {
        refreshContainer();
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEventTargetTagCreated(final TargetTagCreatedBulkEvent event) {
        refreshContainer();
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEventTargetDeletedEvent(final TargetTagDeletedEvent event) {
        refreshContainer();
    }

    private void refreshContainer() {
        removeGeneratedColumn(FILTER_BUTTON_COLUMN);
        ((LazyQueryContainer) getContainerDataSource()).refresh();
        addNewTargetTag(new TargetTag("NO TAG"));
        addColumn();
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final ManagementUIEvent event) {
        if (event == ManagementUIEvent.RESET_SIMPLE_FILTERS
                && !managementUIState.getTargetTableFilters().getClickedTargetTags().isEmpty()) {
            filterButtonClickBehaviour.clearTargetTagFilters();
        }
    }

    private void addNewTargetTag(final TargetTag newTargetTag) {
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
