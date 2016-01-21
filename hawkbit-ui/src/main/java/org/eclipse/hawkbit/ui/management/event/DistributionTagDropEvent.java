/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.event;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetTagAssigmentResult;
import org.eclipse.hawkbit.repository.SpPermissionChecker;
import org.eclipse.hawkbit.repository.model.DistributionSetIdName;
import org.eclipse.hawkbit.ui.management.event.DistributionTagEvent.DistTagComponentEvent;
import org.eclipse.hawkbit.ui.management.state.DistributionTableFilters;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIComponetIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;

import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Component;
import com.vaadin.ui.DragAndDropWrapper;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.TableTransferable;

/**
 *
 *
 */
@SpringComponent
@ViewScope
public class DistributionTagDropEvent implements DropHandler {

    private static final long serialVersionUID = 7338133229709850212L;

    @Autowired
    private I18N i18n;

    @Autowired
    private transient UINotification notification;

    @Autowired
    private SpPermissionChecker permChecker;

    @Autowired
    private DistributionTableFilters distFilterParameters;

    @Autowired
    private transient DistributionSetManagement distributionSetManagement;

    @Autowired
    private transient EventBus.SessionEventBus eventBus;

    @Autowired
    private ManagementViewAcceptCriteria managementViewAcceptCriteria;

    @Autowired
    private ManagementUIState managementUIState;

    private static final String ITEMID = "itemId";

    @Override
    public void drop(final DragAndDropEvent event) {
        if (validate(event) && isNoTagAssigned(event)) {
            final TableTransferable tbl = (TableTransferable) event.getTransferable();
            final Table source = tbl.getSourceComponent();

            if (source.getId().equals(SPUIComponetIdProvider.DIST_TABLE_ID)) {
                processDistributionDrop(event);
            }

        }
    }

    private Boolean isNoTagAssigned(final DragAndDropEvent event) {
        final String tagName = ((DragAndDropWrapper) (event.getTargetDetails().getTarget())).getData().toString();
        if (tagName.equals(SPUIDefinitions.DISTRIBUTION_TAG_BUTTON)) {
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

            if (!validateIfSourceIsDs(source) && !checkForDSUpdatePermission()) {
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
    private boolean checkForDSUpdatePermission() {
        if (!permChecker.hasUpdateDistributionPermission()) {

            notification.displayValidationError(i18n.get("message.permission.insufficient"));
            return false;
        }

        return true;
    }

    /**
     * validate the source tables.
     *
     * @param source
     *            table
     * @return boolean
     */
    private boolean validateIfSourceIsDs(final Table source) {
        if (!source.getId().equals(SPUIComponetIdProvider.DIST_TABLE_ID)) {
            notification.displayValidationError(i18n.get(SPUILabelDefinitions.ACTION_NOT_ALLOWED));
            return false;
        }
        return true;
    }

    /**
     * Process target Drop event.
     *
     * @param event
     *            DragAndDropEvent
     */
    private void processDistributionDrop(final DragAndDropEvent event) {

        final com.vaadin.event.dd.TargetDetails targetDetails = event.getTargetDetails();

        final TableTransferable transferable = (TableTransferable) event.getTransferable();
        final Table source = transferable.getSourceComponent();

        @SuppressWarnings("unchecked")
        final Set<DistributionSetIdName> distSelected = (Set<DistributionSetIdName>) source.getValue();
        final Set<Long> distributionList = new HashSet<Long>();
        if (!distSelected.contains(transferable.getData(ITEMID))) {
            distributionList.add(((DistributionSetIdName) transferable.getData(ITEMID)).getId());
        } else {
            distributionList.addAll(distSelected.stream().map(t -> t.getId()).collect(Collectors.toList()));
        }

        final String distTagName = HawkbitCommonUtil.removePrefix(targetDetails.getTarget().getId(),
                SPUIDefinitions.DISTRIBUTION_TAG_ID_PREFIXS);

        final List<String> tagsClickedList = distFilterParameters.getDistSetTags();
        final DistributionSetTagAssigmentResult result = distributionSetManagement.toggleTagAssignment(distributionList,
                distTagName);

        notification.displaySuccess(HawkbitCommonUtil.getDistributionTagAssignmentMsg(distTagName, result, i18n));
        if (result.getUnassigned() >= 1 && !tagsClickedList.isEmpty()) {
            eventBus.publish(this, TargetFilterEvent.FILTER_BY_TAG);
        }
        updateTagLayoutInDetails(result, distTagName);
    }

    private void updateTagLayoutInDetails(final DistributionSetTagAssigmentResult result, final String distTagName) {
        if (result.getAssigned() > 0) {
            final List<Long> assignedDsNames = result.getAssignedDs().stream().map(t -> t.getId())
                    .collect(Collectors.toList());
            if (assignedDsNames.contains(managementUIState.getLastSelectedDsIdName().getId())) {
                eventBus.publish(this, new DistributionTagEvent(DistTagComponentEvent.ASSIGNED, distTagName));
            }
        } else if (result.getUnassigned() > 0) {
            final List<Long> unassignedDsNames = result.getUnassignedDs().stream().map(t -> t.getId())
                    .collect(Collectors.toList());
            if (unassignedDsNames.contains(managementUIState.getLastSelectedDsIdName().getId())) {
                eventBus.publish(this, new DistributionTagEvent(DistTagComponentEvent.UNASSIGNED, distTagName));
            }
        }
    }

    /**
     * Criteria.
     * 
     * @return AcceptCriterion as accept
     */
    @Override
    public AcceptCriterion getAcceptCriterion() {
        return managementViewAcceptCriteria;

    }
}
