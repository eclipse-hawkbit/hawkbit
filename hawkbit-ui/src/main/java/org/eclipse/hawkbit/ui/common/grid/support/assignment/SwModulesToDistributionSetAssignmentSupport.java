/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.grid.support.assignment;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyDistributionSet;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxySoftwareModule;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload.EntityModifiedEventType;
import org.eclipse.hawkbit.ui.common.event.EventTopics;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.util.CollectionUtils;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Support for assigning software modules to distribution set.
 * 
 */
public class SwModulesToDistributionSetAssignmentSupport
        extends DeploymentAssignmentSupport<ProxySoftwareModule, ProxyDistributionSet> {

    private final UIEventBus eventBus;
    private final SpPermissionChecker permChecker;

    private final TargetManagement targetManagement;
    private final DistributionSetManagement dsManagement;
    private final SoftwareModuleManagement smManagement;
    private final DistributionSetTypeManagement dsTypeManagement;
    private final SoftwareModuleTypeManagement smTypeManagement;

    /**
     * Constructor for SwModulesToDistributionSetAssignmentSupport
     *
     * @param notification
     *            UINotification
     * @param i18n
     *            VaadinMessageSource
     * @param eventBus
     *            UIEventBus
     * @param permChecker
     *            SpPermissionChecker
     * @param targetManagement
     *            TargetManagement
     * @param dsManagement
     *            DistributionSetManagement
     * @param smManagement
     *            SoftwareModuleManagement
     * @param dsTypeManagement
     *            DistributionSetTypeManagement
     * @param smTypeManagement
     *            SoftwareModuleTypeManagement
     */
    public SwModulesToDistributionSetAssignmentSupport(final UINotification notification,
            final VaadinMessageSource i18n, final UIEventBus eventBus, final SpPermissionChecker permChecker,
            final TargetManagement targetManagement, final DistributionSetManagement dsManagement,
            final SoftwareModuleManagement smManagement, final DistributionSetTypeManagement dsTypeManagement,
            final SoftwareModuleTypeManagement smTypeManagement) {
        super(notification, i18n);

        this.eventBus = eventBus;
        this.permChecker = permChecker;

        this.targetManagement = targetManagement;
        this.dsManagement = dsManagement;
        this.smManagement = smManagement;
        this.dsTypeManagement = dsTypeManagement;
        this.smTypeManagement = smTypeManagement;
    }

    @Override
    protected List<ProxySoftwareModule> getFilteredSourceItems(final List<ProxySoftwareModule> sourceItemsToAssign,
            final ProxyDistributionSet targetItem) {
        final DistributionSetType dsType = dsTypeManagement.get(targetItem.getTypeInfo().getId()).orElse(null);

        if (!isTargetDsValid(targetItem, dsType)) {
            return Collections.emptyList();
        }

        final Collection<Long> smIdsAlreadyAssignedToDs = getSmIdsByDsId(targetItem.getId());

        return sourceItemsToAssign.stream()
                .filter(sm -> checkDuplicateSmToDsAssignment(sm, targetItem, smIdsAlreadyAssignedToDs)
                        && checkValidTypeAssignment(sm, targetItem, dsType))
                .collect(Collectors.toList());
    }

    private boolean isTargetDsValid(final ProxyDistributionSet ds, final DistributionSetType dsType) {
        if (dsType == null) {
            notification.displayValidationError(i18n.getMessage("message.dist.type.notfound", ds.getNameVersion()));
            return false;
        }

        if (targetManagement.existsByInstalledOrAssignedDistributionSet(ds.getId())) {
            /* Distribution is already assigned/installed */
            notification.displayValidationError(i18n.getMessage("message.dist.inuse", ds.getNameVersion()));
            return false;
        }

        if (dsManagement.isInUse(ds.getId())) {
            notification.displayValidationError(
                    i18n.getMessage("message.error.notification.ds.target.assigned", ds.getName(), ds.getVersion()));
            return false;
        }

        return true;
    }

    private Collection<Long> getSmIdsByDsId(final Long dsId) {
        return HawkbitCommonUtil.getEntitiesByPageableProvider(query -> smManagement.findByAssignedTo(query, dsId))
                .stream().map(SoftwareModule::getId).collect(Collectors.toList());
    }

    private boolean checkDuplicateSmToDsAssignment(final ProxySoftwareModule sm, final ProxyDistributionSet ds,
            final Collection<Long> smIdsAlreadyAssignedToDs) {
        if (!CollectionUtils.isEmpty(smIdsAlreadyAssignedToDs) && smIdsAlreadyAssignedToDs.contains(sm.getId())) {
            notification.displayValidationError(i18n.getMessage("message.software.dist.already.assigned",
                    sm.getNameAndVersion(), ds.getNameVersion()));
            return false;
        }

        return true;
    }

    private boolean checkValidTypeAssignment(final ProxySoftwareModule sm, final ProxyDistributionSet ds,
            final DistributionSetType dsType) {
        if (!dsType.containsModuleType(sm.getTypeInfo().getId())) {
            final String smTypeName = smTypeManagement.get(sm.getTypeInfo().getId()).map(SoftwareModuleType::getName)
                    .orElse("");

            notification.displayValidationError(i18n.getMessage("message.software.dist.type.notallowed",
                    sm.getNameAndVersion(), ds.getNameVersion(), smTypeName));
            return false;
        }

        return true;
    }

    @Override
    public List<String> getMissingPermissionsForDrop() {
        return permChecker.hasUpdateRepositoryPermission() ? Collections.emptyList()
                : Collections.singletonList(SpPermission.UPDATE_REPOSITORY);
    }

    @Override
    protected void performAssignment(final List<ProxySoftwareModule> sourceItemsToAssign,
            final ProxyDistributionSet targetItem) {
        final List<String> softwareModuleNames = sourceItemsToAssign.stream()
                .map(ProxySoftwareModule::getNameAndVersion).collect(Collectors.toList());
        openConfirmationWindowForAssignments(softwareModuleNames, targetItem.getNameVersion(), null, () -> true,
                () -> assignSwModulesToDistribution(sourceItemsToAssign, targetItem));
    }

    private void assignSwModulesToDistribution(final List<ProxySoftwareModule> swModules,
            final ProxyDistributionSet ds) {
        final Set<Long> swModuleIdsToAssign = swModules.stream().map(ProxySoftwareModule::getId)
                .collect(Collectors.toSet());
        dsManagement.assignSoftwareModules(ds.getId(), swModuleIdsToAssign);

        notification.displaySuccess(i18n.getMessage("message.software.assignment", swModuleIdsToAssign.size()));
        eventBus.publish(EventTopics.ENTITY_MODIFIED, this, new EntityModifiedEventPayload(
                EntityModifiedEventType.ENTITY_UPDATED, ProxyDistributionSet.class, ds.getId()));
    }

    @Override
    protected String sourceEntityTypeSing() {
        return i18n.getMessage("caption.software.module");
    }

    @Override
    protected String sourceEntityTypePlur() {
        return i18n.getMessage("caption.softwaremodules");
    }

    @Override
    protected String targetEntityType() {
        return i18n.getMessage("distribution.details.header");
    }

    @Override
    protected String confirmationWindowId() {
        return UIComponentIdProvider.SOFT_MODULE_TO_DIST_ASSIGNMENT_CONFIRM_ID;
    }
}
