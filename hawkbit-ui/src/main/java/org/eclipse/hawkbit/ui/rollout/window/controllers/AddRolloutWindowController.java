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
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.builder.RolloutCreate;
import org.eclipse.hawkbit.repository.builder.RolloutGroupCreate;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.RepositoryModelConstants;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupErrorAction;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupErrorCondition;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupSuccessAction;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupSuccessCondition;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditionBuilder;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditions;
import org.eclipse.hawkbit.ui.common.AbstractAddEntityWindowController;
import org.eclipse.hawkbit.ui.common.EntityWindowLayout;
import org.eclipse.hawkbit.ui.common.data.mappers.AdvancedRolloutGroupDefinitionToCreateMapper;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyAdvancedRolloutGroup;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyRollout;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyRolloutWindow;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyRolloutWindow.GroupDefinitionMode;
import org.eclipse.hawkbit.ui.rollout.window.RolloutWindowDependencies;
import org.eclipse.hawkbit.ui.rollout.window.components.AutoStartOptionGroupLayout.AutoStartOption;
import org.eclipse.hawkbit.ui.rollout.window.layouts.AddRolloutWindowLayout;
import org.eclipse.hawkbit.ui.utils.SPDateTimeUtil;
import org.springframework.util.StringUtils;

/**
 * Controller for populating and saving data in Add Rollout Window.
 */
public class AddRolloutWindowController
        extends AbstractAddEntityWindowController<ProxyRollout, ProxyRolloutWindow, Rollout> {
    private final RolloutManagement rolloutManagement;
    protected final AddRolloutWindowLayout layout;
    private final ProxyRolloutValidator validator;

    /**
     * Controller for AddRolloutWindowController
     *
     * @param dependencies
     *            RolloutWindowDependencies
     * @param layout
     *            AddRolloutWindowLayout
     */
    public AddRolloutWindowController(final RolloutWindowDependencies dependencies,
            final AddRolloutWindowLayout layout) {
        super(dependencies.getuiDependencies());

        this.rolloutManagement = dependencies.getRolloutManagement();
        this.layout = layout;
        this.validator = new ProxyRolloutValidator(dependencies.getuiDependencies());
    }

    @Override
    protected ProxyRolloutWindow buildEntityFromProxy(final ProxyRollout proxyEntity) {
        // We ignore the method parameter, because we are interested in the
        // empty object, that we can populate with defaults
        final ProxyRolloutWindow proxyRolloutWindow = new ProxyRolloutWindow();

        proxyRolloutWindow.setActionType(ActionType.FORCED);
        proxyRolloutWindow.setForcedTime(SPDateTimeUtil.twoWeeksFromNowEpochMilli());
        proxyRolloutWindow.setAutoStartOption(AutoStartOption.MANUAL);
        proxyRolloutWindow.setStartAt(SPDateTimeUtil.halfAnHourFromNowEpochMilli());

        proxyRolloutWindow.setGroupDefinitionMode(GroupDefinitionMode.SIMPLE);
        setDefaultThresholds(proxyRolloutWindow);

        return proxyRolloutWindow;
    }

    protected static void setDefaultThresholds(final ProxyRolloutWindow proxyRolloutWindow) {
        final RolloutGroupConditions defaultRolloutGroupConditions = new RolloutGroupConditionBuilder().withDefaults()
                .build();
        proxyRolloutWindow.setTriggerThresholdPercentage(defaultRolloutGroupConditions.getSuccessConditionExp());
        proxyRolloutWindow.setErrorThresholdPercentage(defaultRolloutGroupConditions.getErrorConditionExp());
    }

    @Override
    public EntityWindowLayout<ProxyRolloutWindow> getLayout() {
        return layout;
    }

    @Override
    protected void adaptLayout(final ProxyRollout proxyEntity) {
        layout.addAdvancedGroupRowAndValidate();
    }

    @Override
    protected Rollout persistEntityInRepository(final ProxyRolloutWindow entity) {
        final RolloutGroupConditions conditions = new RolloutGroupConditionBuilder()
                .successAction(RolloutGroupSuccessAction.NEXTGROUP, null)
                .successCondition(RolloutGroupSuccessCondition.THRESHOLD, entity.getTriggerThresholdPercentage())
                .errorCondition(RolloutGroupErrorCondition.THRESHOLD, entity.getErrorThresholdPercentage())
                .errorAction(RolloutGroupErrorAction.PAUSE, null).build();

        final RolloutCreate rolloutCreate = getEntityFactory().rollout().create().name(entity.getName())
                .description(entity.getDescription()).set(entity.getDistributionSetId())
                .targetFilterQuery(entity.getTargetFilterQuery()).actionType(entity.getActionType())
                .forcedTime(entity.getActionType() == ActionType.TIMEFORCED ? entity.getForcedTime()
                        : RepositoryModelConstants.NO_FORCE_TIME)
                .startAt(entity.getStartAtByOption());

        Rollout rolloutToCreate;
        if (GroupDefinitionMode.SIMPLE == entity.getGroupDefinitionMode()) {
            rolloutToCreate = rolloutManagement.create(rolloutCreate, entity.getNumberOfGroups(),
                    entity.isConfirmationRequired(), conditions);
        } else {
            rolloutToCreate = rolloutManagement.create(rolloutCreate,
                    getRolloutGroupsCreateFromDefinitions(entity.getAdvancedRolloutGroupDefinitions()), conditions);
        }

        return rolloutToCreate;
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

    private List<RolloutGroupCreate> getRolloutGroupsCreateFromDefinitions(
            final List<ProxyAdvancedRolloutGroup> advancedRolloutGroupDefinitions) {
        final AdvancedRolloutGroupDefinitionToCreateMapper mapper = new AdvancedRolloutGroupDefinitionToCreateMapper(
                getEntityFactory());

        return advancedRolloutGroupDefinitions.stream().map(mapper::map).collect(Collectors.toList());
    }

    @Override
    protected boolean isEntityValid(final ProxyRolloutWindow entity) {
        final String trimmedName = StringUtils.trimWhitespace(entity.getName());
        return validator.isEntityValid(entity, () -> rolloutManagement.getByName(trimmedName).isPresent());
    }
}
