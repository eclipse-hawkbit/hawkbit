package org.eclipse.hawkbit.ui.management;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.model.RepositoryModelConstants;
import org.eclipse.hawkbit.repository.model.TargetWithActionType;
import org.eclipse.hawkbit.ui.common.entity.DistributionSetIdName;
import org.eclipse.hawkbit.ui.common.entity.TargetIdName;
import org.eclipse.hawkbit.ui.management.event.SaveActionWindowEvent;
import org.eclipse.hawkbit.ui.management.miscs.ActionTypeOptionGroupLayout;
import org.eclipse.hawkbit.ui.management.miscs.ActionTypeOptionGroupLayout.ActionTypeOption;
import org.eclipse.hawkbit.ui.management.miscs.MaintenanceWindowLayout;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.management.targettable.TargetTable;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus;

import com.google.common.collect.Maps;

public class AbstractDistributionTargetTable {

    public static void saveAllAssigmentsTargetTable(final ManagementUIState managementUIState,
            final ActionTypeOptionGroupLayout actionTypeOptionGroupLayout,
            final MaintenanceWindowLayout maintenanceWindowLayout, final DeploymentManagement deploymentManagement,
            final TargetTable targetTable, final UINotification uiNotificationMethod,
            final EventBus.UIEventBus getEventBusMethod,
            final VaadinMessageSource getI18nMethod) {

        saveAllAssigmentsTargetTable(managementUIState, actionTypeOptionGroupLayout, maintenanceWindowLayout,
                deploymentManagement, targetTable);
        // TODO pass EventBus as paramenter, as well as language and
        // notifications



        managementUIState.getAssignedList().clear();
        uiNotificationMethod.displaySuccess(getI18nMethod.getMessage("message.target.ds.assign.success"));
        getEventBusMethod.publish(targetTable, SaveActionWindowEvent.SAVED_ASSIGNMENTS);

    }

    private static void saveAllAssignments(final ManagementUIState managementUIState,
            final ActionTypeOptionGroupLayout actionTypeOptionGroupLayout,
            final MaintenanceWindowLayout maintenanceWindowLayout,
            final DeploymentManagement deploymentManagement, final VaadinMessageSource getI18nMethod) {
        final Set<TargetIdName> itemIds = managementUIState.getAssignedList().keySet();
        Long distId;
        List<TargetIdName> targetIdSetList;
        List<TargetIdName> tempIdList;
        final ActionType actionType = ((ActionTypeOptionGroupLayout.ActionTypeOption) actionTypeOptionGroupLayout
                .getActionTypeOptionGroup().getValue()).getActionType();
        final long forcedTimeStamp = (((ActionTypeOptionGroupLayout.ActionTypeOption) actionTypeOptionGroupLayout
                .getActionTypeOptionGroup().getValue()) == ActionTypeOption.AUTO_FORCED)
                        ? actionTypeOptionGroupLayout.getForcedTimeDateField().getValue().getTime()
                        : RepositoryModelConstants.NO_FORCE_TIME;

        final Map<Long, List<TargetIdName>> saveAssignedList = Maps.newHashMapWithExpectedSize(itemIds.size());

        for (final TargetIdName itemId : itemIds) {
            final DistributionSetIdName distitem = managementUIState.getAssignedList().get(itemId);
            distId = distitem.getId();

            if (saveAssignedList.containsKey(distId)) {
                targetIdSetList = saveAssignedList.get(distId);
            } else {
                targetIdSetList = new ArrayList<>();
            }
            targetIdSetList.add(itemId);
            saveAssignedList.put(distId, targetIdSetList);
        }

        final String maintenanceSchedule = maintenanceWindowLayout.getMaintenanceSchedule();
        final String maintenanceDuration = maintenanceWindowLayout.getMaintenanceDuration();
        final String maintenanceTimeZone = maintenanceWindowLayout.getMaintenanceTimeZone();

        for (final Map.Entry<Long, List<TargetIdName>> mapEntry : saveAssignedList.entrySet()) {
            tempIdList = saveAssignedList.get(mapEntry.getKey());
            final DistributionSetAssignmentResult distributionSetAssignmentResult = deploymentManagement
                    .assignDistributionSet(mapEntry.getKey(),
                            tempIdList.stream().map(t -> maintenanceWindowLayout.isEnabled()
                                    ? new TargetWithActionType(t.getControllerId(), actionType, forcedTimeStamp,
                                            maintenanceSchedule, maintenanceDuration, maintenanceTimeZone)
                                    : new TargetWithActionType(t.getControllerId(), actionType, forcedTimeStamp))
                                    .collect(Collectors.toList()));
            // TODO Check on Monday whether it is possible/make sence to
            // abstract since the called once musst be defined in both classes
            // so we would use different instances and i dont know what this
            // does to the rest of the code
            if (distributionSetAssignmentResult.getAssigned() > 0) {
                getNotification().displaySuccess(getI18nMethod.getMessage("message.target.assignment",
                        distributionSetAssignmentResult.getAssigned()));
            }
            if (distributionSetAssignmentResult.getAlreadyAssigned() > 0) {
                getNotification().displaySuccess(getI18nMethod.getMessage("message.target.alreadyAssigned",
                        distributionSetAssignmentResult.getAlreadyAssigned()));
            }
        }
        resfreshPinnedDetails(saveAssignedList);
        //
        // managementUIState.getAssignedList().clear();
        // getNotification().displaySuccess(getI18n().getMessage("message.target.ds.assign.success"));
        // getEventBus().publish(this, SaveActionWindowEvent.SAVED_ASSIGNMENTS);
    }

}
