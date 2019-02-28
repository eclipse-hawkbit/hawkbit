package org.eclipse.hawkbit.ui.management;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.data.Property;
import com.vaadin.ui.Link;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.MaintenanceScheduleHelper;
import org.eclipse.hawkbit.repository.exception.InvalidMaintenanceScheduleException;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.model.RepositoryModelConstants;
import org.eclipse.hawkbit.repository.model.TargetWithActionType;
import org.eclipse.hawkbit.ui.UiProperties;
import org.eclipse.hawkbit.ui.common.confirmwindow.layout.ConfirmationTab;
import org.eclipse.hawkbit.ui.common.entity.DistributionSetIdName;
import org.eclipse.hawkbit.ui.common.entity.TargetIdName;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.management.event.PinUnpinEvent;
import org.eclipse.hawkbit.ui.management.event.SaveActionWindowEvent;
import org.eclipse.hawkbit.ui.management.miscs.ActionTypeOptionGroupLayout;
import org.eclipse.hawkbit.ui.management.miscs.ActionTypeOptionGroupLayout.ActionTypeOption;
import org.eclipse.hawkbit.ui.management.miscs.MaintenanceWindowLayout;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.slf4j.Logger;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.google.common.collect.Maps;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class AbstractDistributionTargetTable {

    public static void saveAllAssignments(final ManagementUIState managementUIState,
            final ActionTypeOptionGroupLayout actionTypeOptionGroupLayout,
            final MaintenanceWindowLayout maintenanceWindowLayout,
            final DeploymentManagement deploymentManagement, final UINotification notification,
            final UIEventBus eventBus, final VaadinMessageSource i18n, final Object eventSource) {
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
            if (distributionSetAssignmentResult.getAssigned() > 0) {
                notification.displaySuccess(i18n.getMessage("message.target.assignment",
                        distributionSetAssignmentResult.getAssigned()));
            }
            if (distributionSetAssignmentResult.getAlreadyAssigned() > 0) {
                notification.displaySuccess(i18n.getMessage("message.target.alreadyAssigned",
                        distributionSetAssignmentResult.getAlreadyAssigned()));
            }
        }
        refreshPinnedDetails(saveAssignedList, managementUIState, eventBus, eventSource);
        managementUIState.getAssignedList().clear();
        notification.displaySuccess(i18n.getMessage("message.target.ds.assign.success"));
        eventBus.publish(eventSource, SaveActionWindowEvent.SAVED_ASSIGNMENTS);
    }


    static private void refreshPinnedDetails(final Map<Long, List<TargetIdName>> saveAssignedList,
            final ManagementUIState managementUIState, final UIEventBus eventBus, final Object eventSource) {
        final Optional<Long> pinnedDist = managementUIState.getTargetTableFilters().getPinnedDistId();
        final Optional<TargetIdName> pinnedTarget = managementUIState.getDistributionTableFilters().getPinnedTarget();

        if (pinnedDist.isPresent()) {
            if (saveAssignedList.keySet().contains(pinnedDist.get())) {
                eventBus.publish(eventSource, PinUnpinEvent.PIN_DISTRIBUTION);
            }
        } else if (pinnedTarget.isPresent()) {
            final Set<TargetIdName> assignedTargetIds = managementUIState.getAssignedList().keySet();
            if (assignedTargetIds.contains(pinnedTarget.get())) {
                eventBus.publish(eventSource, PinUnpinEvent.PIN_TARGET);
            }
        }
    }

    public static boolean isMaintenanceWindowValid(final MaintenanceWindowLayout maintenanceWindowLayout,
            final Logger log, final UINotification notification) {
        if (maintenanceWindowLayout.isEnabled()) {
            try {
                MaintenanceScheduleHelper.validateMaintenanceSchedule(maintenanceWindowLayout.getMaintenanceSchedule(),
                        maintenanceWindowLayout.getMaintenanceDuration(),
                        maintenanceWindowLayout.getMaintenanceTimeZone());
            } catch (final InvalidMaintenanceScheduleException e) {
                log.error("Maintenance window is not valid", e);
                notification.displayValidationError(e.getMessage());
                return false;
            }
        }
        return true;
    }

    public static ConfirmationTab createAssignmentTab(final ActionTypeOptionGroupLayout actionTypeOptionGroupLayout,
            final MaintenanceWindowLayout maintenanceWindowLayout, final SaveButtonEnabler saveButtonEnabler,
            final VaadinMessageSource i18n, final UiProperties uiProperties) {

        final CheckBox maintenanceWindowControl = maintenanceWindowControl(i18n, maintenanceWindowLayout, saveButtonEnabler);
        final Link maintenanceWindowHelpLink = maintenanceWindowHelpLinkControl(uiProperties, i18n);
        final HorizontalLayout layout = createHorizontalLayout(maintenanceWindowControl, maintenanceWindowHelpLink);
        actionTypeOptionGroupLayout.selectDefaultOption();

        initMaintenanceWindow(maintenanceWindowLayout, saveButtonEnabler);
        addValueChangeListener(actionTypeOptionGroupLayout, maintenanceWindowControl, maintenanceWindowHelpLink);
        return createAssignmentTab(actionTypeOptionGroupLayout, layout, maintenanceWindowLayout);
    }

    private static HorizontalLayout createHorizontalLayout(CheckBox maintenanceWindowControl, Link maintenanceWindowHelpLink) {
        final HorizontalLayout layout = new HorizontalLayout();
        layout.addComponent(maintenanceWindowControl);
        layout.addComponent(maintenanceWindowHelpLink);
        return layout;
    }

    private static ConfirmationTab createAssignmentTab(final ActionTypeOptionGroupLayout actionTypeOptionGroupLayout,
             final HorizontalLayout layout, final MaintenanceWindowLayout maintenanceWindowLayout) {
        final ConfirmationTab assignmentTab = new ConfirmationTab();
        assignmentTab.addComponent(actionTypeOptionGroupLayout);
        assignmentTab.addComponent(layout);
        assignmentTab.addComponent(maintenanceWindowLayout);
        return assignmentTab;
    }

    private static void initMaintenanceWindow(final MaintenanceWindowLayout maintenanceWindowLayout,
            final SaveButtonEnabler saveButtonEnabler) {
        maintenanceWindowLayout.setVisible(false);
        maintenanceWindowLayout.setEnabled(false);
        maintenanceWindowLayout.getScheduleControl()
                .addTextChangeListener(
                        event -> saveButtonEnabler.setButtonEnabled(maintenanceWindowLayout.onScheduleChange(event)));
        maintenanceWindowLayout.getDurationControl()
                .addTextChangeListener(
                        event -> saveButtonEnabler.setButtonEnabled(maintenanceWindowLayout.onScheduleChange(event)));
    }

    public static CheckBox maintenanceWindowControl(final VaadinMessageSource i18n,
            final MaintenanceWindowLayout maintenanceWindowLayout, final SaveButtonEnabler saveButtonEnabler) {
        final CheckBox enableMaintenanceWindow = new CheckBox(i18n.getMessage("caption.maintenancewindow.enabled"));
        enableMaintenanceWindow.setId(UIComponentIdProvider.MAINTENANCE_WINDOW_ENABLED_ID);
        enableMaintenanceWindow.addStyleName(ValoTheme.CHECKBOX_SMALL);
        enableMaintenanceWindow.addStyleName("dist-window-maintenance-window-enable");
        enableMaintenanceWindow.addValueChangeListener(event -> {
            final Boolean isMaintenanceWindowEnabled = enableMaintenanceWindow.getValue();
            maintenanceWindowLayout.setVisible(isMaintenanceWindowEnabled);
            maintenanceWindowLayout.setEnabled(isMaintenanceWindowEnabled);
            saveButtonEnabler.setButtonEnabled(!isMaintenanceWindowEnabled);
            maintenanceWindowLayout.clearAllControls();
        });
        return enableMaintenanceWindow;
    }

    private static void addValueChangeListener(ActionTypeOptionGroupLayout actionTypeOptionGroupLayout, CheckBox enableMaintenanceWindowControl, Link maintenanceWindowHelpLinkControl) {
        actionTypeOptionGroupLayout.getActionTypeOptionGroup().addValueChangeListener(new Property.ValueChangeListener() {
            private static final long serialVersionUID = 1L;

            @Override
            public void valueChange(final com.vaadin.data.Property.ValueChangeEvent event) {
                if (event.getProperty().getValue().equals(ActionTypeOption.DOWNLOAD_ONLY)) {
                    enableMaintenanceWindowControl.setValue(false);
                    enableMaintenanceWindowControl.setEnabled(false);
                    maintenanceWindowHelpLinkControl.setEnabled(false);

                } else {
                    enableMaintenanceWindowControl.setEnabled(true);
                    maintenanceWindowHelpLinkControl.setEnabled(true);
                }

            }
        });
    }

    private static Link maintenanceWindowHelpLinkControl(UiProperties uiProperties, VaadinMessageSource i18n) {
        final String maintenanceWindowHelpUrl = uiProperties.getLinks().getDocumentation().getMaintenanceWindowView();
        Link maintenanceWindowLink = SPUIComponentProvider.getHelpLink(i18n, maintenanceWindowHelpUrl);
        return maintenanceWindowLink;
    }

}
