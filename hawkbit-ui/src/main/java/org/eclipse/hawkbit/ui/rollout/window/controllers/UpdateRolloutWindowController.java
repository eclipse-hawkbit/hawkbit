/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout.window.controllers;

import java.util.List;

import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.RolloutGroupManagement;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.builder.RolloutUpdate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.EntityReadOnlyException;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.RepositoryModelConstants;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.ui.common.AbstractEntityWindowController;
import org.eclipse.hawkbit.ui.common.EntityWindowLayout;
import org.eclipse.hawkbit.ui.common.data.mappers.RolloutGroupToAdvancedDefinitionMapper;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyAdvancedRolloutGroup;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyRollout;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyRolloutWindow;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyRolloutWindow.GroupDefinitionMode;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload.EntityModifiedEventType;
import org.eclipse.hawkbit.ui.common.event.EventTopics;
import org.eclipse.hawkbit.ui.rollout.event.RolloutEvent;
import org.eclipse.hawkbit.ui.rollout.window.RolloutWindowDependencies;
import org.eclipse.hawkbit.ui.rollout.window.components.AutoStartOptionGroupLayout.AutoStartOption;
import org.eclipse.hawkbit.ui.rollout.window.layouts.UpdateRolloutWindowLayout;
import org.eclipse.hawkbit.ui.utils.SPDateTimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * Controller for populating and editing/saving data in Update Rollout Window.
 */
public class UpdateRolloutWindowController extends AbstractEntityWindowController<ProxyRollout, ProxyRolloutWindow> {
    private static final Logger LOG = LoggerFactory.getLogger(UpdateRolloutWindowController.class);

    private final TargetFilterQueryManagement targetFilterQueryManagement;
    protected final RolloutManagement rolloutManagement;
    private final RolloutGroupManagement rolloutGroupManagement;
    private final QuotaManagement quotaManagement;
    private final UpdateRolloutWindowLayout layout;

    private String nameBeforeEdit;

    /**
     * Constructor for UpdateRolloutWindowController
     *
     * @param dependencies
     *            RolloutWindowDependencies
     * @param layout
     *            UpdateRolloutWindowLayout
     */
    public UpdateRolloutWindowController(final RolloutWindowDependencies dependencies,
            final UpdateRolloutWindowLayout layout) {
        super(dependencies.getUiConfig());

        this.targetFilterQueryManagement = dependencies.getTargetFilterQueryManagement();
        this.rolloutManagement = dependencies.getRolloutManagement();
        this.rolloutGroupManagement = dependencies.getRolloutGroupManagement();
        this.quotaManagement = dependencies.getQuotaManagement();
        this.layout = layout;
    }

    @Override
    public EntityWindowLayout<ProxyRolloutWindow> getLayout() {
        return layout;
    }

    @Override
    protected ProxyRolloutWindow buildEntityFromProxy(final ProxyRollout proxyEntity) {
        final ProxyRolloutWindow proxyRolloutWindow = new ProxyRolloutWindow(proxyEntity);

        if (proxyRolloutWindow.getForcedTime() == null
                || RepositoryModelConstants.NO_FORCE_TIME.equals(proxyRolloutWindow.getForcedTime())) {
            proxyRolloutWindow.setForcedTime(SPDateTimeUtil.twoWeeksFromNowEpochMilli());
        }

        proxyRolloutWindow.setAutoStartOption(proxyRolloutWindow.getOptionByStartAt());
        if (AutoStartOption.SCHEDULED != proxyRolloutWindow.getAutoStartOption()) {
            proxyRolloutWindow.setStartAt(SPDateTimeUtil.halfAnHourFromNowEpochMilli());
        }

        setAdvancedGroups(proxyRolloutWindow);

        nameBeforeEdit = proxyRolloutWindow.getName();

        return proxyRolloutWindow;
    }

    private void setAdvancedGroups(final ProxyRolloutWindow proxyRolloutWindow) {
        proxyRolloutWindow.setGroupDefinitionMode(GroupDefinitionMode.ADVANCED);
        final RolloutGroupToAdvancedDefinitionMapper groupsMapper = new RolloutGroupToAdvancedDefinitionMapper(
                targetFilterQueryManagement);
        final List<ProxyAdvancedRolloutGroup> advancedGroupDefinitions = groupsMapper.loadRolloutGroupssFromBackend(
                proxyRolloutWindow.getId(), rolloutGroupManagement, quotaManagement.getMaxRolloutGroupsPerRollout());
        proxyRolloutWindow.setAdvancedRolloutGroupDefinitions(advancedGroupDefinitions);
    }

    @Override
    protected void adaptLayout(final ProxyRollout proxyEntity) {
        if (Rollout.RolloutStatus.READY == proxyEntity.getStatus()) {
            layout.adaptForPendingStatus();
        } else {
            layout.adaptForStartedStatus();
        }

        layout.setTotalTargets(proxyEntity.getTotalTargets());

        layout.resetValidation();
    }

    @Override
    protected void persistEntity(final ProxyRolloutWindow entity) {
        final RolloutUpdate rolloutUpdate = getEntityFactory().rollout().update(entity.getId()).name(entity.getName())
                .description(entity.getDescription()).set(entity.getDistributionSetId())
                .actionType(entity.getActionType())
                .forcedTime(entity.getActionType() == ActionType.TIMEFORCED ? entity.getForcedTime()
                        : RepositoryModelConstants.NO_FORCE_TIME)
                .startAt(entity.getStartAtByOption());

        try {
            final Rollout updatedRollout = rolloutManagement.update(rolloutUpdate);

            displaySuccess("message.update.success", updatedRollout.getName());
            getEventBus().publish(EventTopics.ENTITY_MODIFIED, this, new EntityModifiedEventPayload(
                    EntityModifiedEventType.ENTITY_UPDATED, ProxyRollout.class, updatedRollout.getId()));
        } catch (final EntityNotFoundException | EntityReadOnlyException e) {
            LOG.trace("Update of rollout failed in UI: {}", e.getMessage());
            final String entityType = getI18n().getMessage("caption.rollout");
            displayWarning("message.deleted.or.notAllowed", entityType, entity.getName());

            getEventBus().publish(this, RolloutEvent.SHOW_ROLLOUTS);
        }
    }

    @Override
    protected boolean isEntityValid(final ProxyRolloutWindow entity) {
        if (entity == null) {
            displayValidationError("message.save.fail", getI18n().getMessage("caption.rollout"));
            return false;
        }

        if (!StringUtils.hasText(entity.getName())) {
            displayValidationError("message.rollout.name.empty");
            return false;
        }

        final String trimmedName = StringUtils.trimWhitespace(entity.getName());
        if (!nameBeforeEdit.equals(trimmedName) && rolloutManagement.getByName(trimmedName).isPresent()) {
            displayValidationError("message.rollout.duplicate.check", trimmedName);
            return false;
        }

        return true;
    }
}
