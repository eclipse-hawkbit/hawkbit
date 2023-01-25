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
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.RepositoryModelConstants;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.ui.common.AbstractUpdateEntityWindowController;
import org.eclipse.hawkbit.ui.common.EntityWindowLayout;
import org.eclipse.hawkbit.ui.common.data.mappers.RolloutGroupToAdvancedDefinitionMapper;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyAdvancedRolloutGroup;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyRollout;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyRolloutWindow;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyRolloutWindow.GroupDefinitionMode;
import org.eclipse.hawkbit.ui.rollout.event.RolloutEvent;
import org.eclipse.hawkbit.ui.rollout.window.RolloutWindowDependencies;
import org.eclipse.hawkbit.ui.rollout.window.components.AutoStartOptionGroupLayout.AutoStartOption;
import org.eclipse.hawkbit.ui.rollout.window.layouts.UpdateRolloutWindowLayout;
import org.eclipse.hawkbit.ui.utils.SPDateTimeUtil;
import org.springframework.util.StringUtils;

/**
 * Controller for populating and editing/saving data in Update Rollout Window.
 */
public class UpdateRolloutWindowController
        extends AbstractUpdateEntityWindowController<ProxyRollout, ProxyRolloutWindow, Rollout> {

    private final TargetFilterQueryManagement targetFilterQueryManagement;
    protected final RolloutManagement rolloutManagement;
    private final RolloutGroupManagement rolloutGroupManagement;
    private final QuotaManagement quotaManagement;
    private final UpdateRolloutWindowLayout layout;
    private final ProxyRolloutValidator validator;

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
        super(dependencies.getuiDependencies());

        this.targetFilterQueryManagement = dependencies.getTargetFilterQueryManagement();
        this.rolloutManagement = dependencies.getRolloutManagement();
        this.rolloutGroupManagement = dependencies.getRolloutGroupManagement();
        this.quotaManagement = dependencies.getQuotaManagement();
        this.layout = layout;
        this.validator = new ProxyRolloutValidator(dependencies.getuiDependencies());
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
        final List<ProxyAdvancedRolloutGroup> advancedGroupDefinitions = groupsMapper.loadRolloutGroupsFromBackend(
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
    protected Rollout persistEntityInRepository(final ProxyRolloutWindow entity) {
        final RolloutUpdate rolloutUpdate = getEntityFactory().rollout().update(entity.getId()).name(entity.getName())
                .description(entity.getDescription()).set(entity.getDistributionSetId())
                .actionType(entity.getActionType())
                .forcedTime(entity.getActionType() == ActionType.TIMEFORCED ? entity.getForcedTime()
                        : RepositoryModelConstants.NO_FORCE_TIME)
                .startAt(entity.getStartAtByOption());
        return rolloutManagement.update(rolloutUpdate);
    }

    @Override
    protected void handleEntityPersistFailed(final ProxyRolloutWindow entity, final RuntimeException ex) {
        super.handleEntityPersistFailed(entity, ex);

        getEventBus().publish(this, RolloutEvent.SHOW_ROLLOUTS);
    }

    @Override
    protected String getDisplayableName(final Rollout entity) {
        return entity.getName();
    }

    @Override
    protected String getDisplayableNameForFailedMessage(final ProxyRolloutWindow entity) {
        return entity.getName();
    }

    @Override
    protected Long getId(final Rollout entity) {
        return entity.getId();
    }

    @Override
    protected Class<? extends ProxyIdentifiableEntity> getEntityClass() {
        return ProxyRollout.class;
    }

    @Override
    protected boolean isEntityValid(final ProxyRolloutWindow entity) {
        final String trimmedName = StringUtils.trimWhitespace(entity.getName());
        return validator.isEntityValid(entity,
                () -> hasNamedChanged(trimmedName) && rolloutManagement.getByName(trimmedName).isPresent());
    }

    private boolean hasNamedChanged(final String trimmedName) {
        return !nameBeforeEdit.equals(trimmedName);
    }
}
