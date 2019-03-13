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
import org.eclipse.hawkbit.ui.management.miscs.AbstractActionTypeOptionGroupLayout;
import org.eclipse.hawkbit.ui.management.miscs.ActionTypeOptionGroupAssignmentLayout;
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

public final class TargetAssignmentOperations {

    private TargetAssignmentOperations() {
    }

    /**
     * Save all target(s)-distributionSet assignments
     *  @param managementUIState
     *            the management UI state
     * @param actionTypeOptionGroupLayout
     *            the action Type Option Group Layout
     * @param maintenanceWindowLayout
 *            the Maintenance Window Layout
     * @param deploymentManagement
*            the Deployment Management
     * @param notification
*            the UI Notification
     * @param eventBus
*            the UI Event Bus
     * @param i18n
*            the Vaadin Message Source for multi language
     * @param eventSource
     */
    public static void saveAllAssignments(final ManagementUIState managementUIState,
            final ActionTypeOptionGroupAssignmentLayout actionTypeOptionGroupLayout,
            final MaintenanceWindowLayout maintenanceWindowLayout, final DeploymentManagement deploymentManagement,
            final UINotification notification, final UIEventBus eventBus, final VaadinMessageSource i18n,
            final Object eventSource) {
        final Set<TargetIdName> itemIds = managementUIState.getAssignedList().keySet();
        Long distId;
        List<TargetIdName> targetIdSetList;
        List<TargetIdName> tempIdList;
        final ActionType actionType = ((AbstractActionTypeOptionGroupLayout.ActionTypeOption) actionTypeOptionGroupLayout
                .getActionTypeOptionGroup().getValue()).getActionType();
        final long forcedTimeStamp = (actionTypeOptionGroupLayout.getActionTypeOptionGroup().getValue()
                == AbstractActionTypeOptionGroupLayout.ActionTypeOption.AUTO_FORCED)
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
                notification.displaySuccess(
                        i18n.getMessage("message.target.assignment", distributionSetAssignmentResult.getAssigned()));
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

    /**
     * Check wether the maintenance window is valid or not
     * 
     * @param maintenanceWindowLayout
     *            the maintenance window layout
     * @param log
     *            the logger for the calling class
     * @param notification
     *            the UI Notification
     * @return boolean if maintenance window is valid or not
     */
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

    /**
     * Create the Assignment Confirmation Tab
     *
     * @param actionTypeOptionGroupLayout
     *            the action Type Option Group Layout
     * @param maintenanceWindowLayout
     *            the Maintenance Window Layout
     * @param saveButtonEnabler
     *            The event listener to derimne if save button should be enabled
     *            or not
     * @param i18n
     *            the Vaadin Message Source for multi language
     * @param uiProperties
     *            the UI Properties
     * @return the Assignment Confirmation tab
     */
    public static ConfirmationTab createAssignmentTab(final ActionTypeOptionGroupAssignmentLayout actionTypeOptionGroupLayout,
            final MaintenanceWindowLayout maintenanceWindowLayout, final SaveButtonEnabler saveButtonEnabler,
            final VaadinMessageSource i18n, final UiProperties uiProperties) {

        final CheckBox maintenanceWindowControl = maintenanceWindowControl(i18n, maintenanceWindowLayout,
                saveButtonEnabler);
        final Link maintenanceWindowHelpLink = maintenanceWindowHelpLinkControl(uiProperties, i18n);
        final HorizontalLayout layout = createHorizontalLayout(maintenanceWindowControl, maintenanceWindowHelpLink);
        actionTypeOptionGroupLayout.selectDefaultOption();

        initMaintenanceWindow(maintenanceWindowLayout, saveButtonEnabler);
        addValueChangeListener(actionTypeOptionGroupLayout, maintenanceWindowControl, maintenanceWindowHelpLink);
        return createAssignmentTab(actionTypeOptionGroupLayout, layout, maintenanceWindowLayout);
    }

    private static HorizontalLayout createHorizontalLayout(final CheckBox maintenanceWindowControl,
            final Link maintenanceWindowHelpLink) {
        final HorizontalLayout layout = new HorizontalLayout();
        layout.addComponent(maintenanceWindowControl);
        layout.addComponent(maintenanceWindowHelpLink);
        return layout;
    }

    private static ConfirmationTab createAssignmentTab(final ActionTypeOptionGroupAssignmentLayout actionTypeOptionGroupLayout,
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
        maintenanceWindowLayout.getScheduleControl().addTextChangeListener(
                event -> saveButtonEnabler.setButtonEnabled(maintenanceWindowLayout.onScheduleChange(event)));
        maintenanceWindowLayout.getDurationControl().addTextChangeListener(
                event -> saveButtonEnabler.setButtonEnabled(maintenanceWindowLayout.onScheduleChange(event)));
    }

    private static CheckBox maintenanceWindowControl(final VaadinMessageSource i18n,
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

    private static void addValueChangeListener(final ActionTypeOptionGroupAssignmentLayout actionTypeOptionGroupLayout,
            final CheckBox enableMaintenanceWindowControl, final Link maintenanceWindowHelpLinkControl) {
        actionTypeOptionGroupLayout.getActionTypeOptionGroup()
                .addValueChangeListener(new Property.ValueChangeListener() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void valueChange(final com.vaadin.data.Property.ValueChangeEvent event) {
                        if (event.getProperty().getValue().equals(AbstractActionTypeOptionGroupLayout.ActionTypeOption.DOWNLOAD_ONLY)) {
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

    private static Link maintenanceWindowHelpLinkControl(final UiProperties uiProperties,
            final VaadinMessageSource i18n) {
        final String maintenanceWindowHelpUrl = uiProperties.getLinks().getDocumentation().getMaintenanceWindowView();
        return SPUIComponentProvider.getHelpLink(i18n, maintenanceWindowHelpUrl);
    }

}
