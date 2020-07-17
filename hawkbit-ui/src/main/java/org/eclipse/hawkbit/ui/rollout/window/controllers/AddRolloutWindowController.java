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

import org.eclipse.hawkbit.repository.EntityFactory;
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
import org.eclipse.hawkbit.ui.common.AbstractEntityWindowController;
import org.eclipse.hawkbit.ui.common.EntityWindowLayout;
import org.eclipse.hawkbit.ui.common.data.mappers.AdvancedRolloutGroupDefinitionToCreateMapper;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyAdvancedRolloutGroup;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyRollout;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyRolloutWindow;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyRolloutWindow.GroupDefinitionMode;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload.EntityModifiedEventType;
import org.eclipse.hawkbit.ui.common.event.EventTopics;
import org.eclipse.hawkbit.ui.rollout.window.RolloutWindowDependencies;
import org.eclipse.hawkbit.ui.rollout.window.components.AutoStartOptionGroupLayout.AutoStartOption;
import org.eclipse.hawkbit.ui.rollout.window.layouts.AddRolloutWindowLayout;
import org.eclipse.hawkbit.ui.utils.SPDateTimeUtil;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.util.StringUtils;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Controller for populating and saving data in Add Rollout Window.
 */
public class AddRolloutWindowController extends AbstractEntityWindowController<ProxyRollout, ProxyRolloutWindow> {
    protected final VaadinMessageSource i18n;
    private final EntityFactory entityFactory;
    private final UIEventBus eventBus;
    private final UINotification uiNotification;

    private final RolloutManagement rolloutManagement;

    protected final AddRolloutWindowLayout layout;

    /**
     * Controller for AddRolloutWindowController
     *
     * @param dependencies
     *          RolloutWindowDependencies
     * @param layout
     *          AddRolloutWindowLayout
     */
    public AddRolloutWindowController(final RolloutWindowDependencies dependencies,
            final AddRolloutWindowLayout layout) {
        this.i18n = dependencies.getI18n();
        this.entityFactory = dependencies.getEntityFactory();
        this.eventBus = dependencies.getEventBus();
        this.uiNotification = dependencies.getUiNotification();

        this.rolloutManagement = dependencies.getRolloutManagement();

        this.layout = layout;
    }

    @Override
    public EntityWindowLayout<ProxyRolloutWindow> getLayout() {
        return layout;
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

    protected void setDefaultThresholds(final ProxyRolloutWindow proxyRolloutWindow) {
        final RolloutGroupConditions defaultRolloutGroupConditions = new RolloutGroupConditionBuilder().withDefaults()
                .build();
        proxyRolloutWindow.setTriggerThresholdPercentage(defaultRolloutGroupConditions.getSuccessConditionExp());
        proxyRolloutWindow.setErrorThresholdPercentage(defaultRolloutGroupConditions.getErrorConditionExp());
    }

    @Override
    protected void adaptLayout(final ProxyRollout proxyEntity) {
        layout.addAdvancedGroupRowAndValidate();
    }

    @Override
    protected void persistEntity(final ProxyRolloutWindow entity) {
        final RolloutGroupConditions conditions = new RolloutGroupConditionBuilder()
                .successAction(RolloutGroupSuccessAction.NEXTGROUP, null)
                .successCondition(RolloutGroupSuccessCondition.THRESHOLD, entity.getTriggerThresholdPercentage())
                .errorCondition(RolloutGroupErrorCondition.THRESHOLD, entity.getErrorThresholdPercentage())
                .errorAction(RolloutGroupErrorAction.PAUSE, null).build();

        final RolloutCreate rolloutCreate = entityFactory.rollout().create().name(entity.getName())
                .description(entity.getDescription()).set(entity.getDistributionSetId())
                .targetFilterQuery(entity.getTargetFilterQuery()).actionType(entity.getActionType())
                .forcedTime(entity.getActionType() == ActionType.TIMEFORCED ? entity.getForcedTime()
                        : RepositoryModelConstants.NO_FORCE_TIME)
                .startAt(entity.getStartAtByOption());

        Rollout rolloutToCreate;
        if (GroupDefinitionMode.SIMPLE == entity.getGroupDefinitionMode()) {
            rolloutToCreate = rolloutManagement.create(rolloutCreate, entity.getNumberOfGroups(), conditions);
        } else {
            rolloutToCreate = rolloutManagement.create(rolloutCreate,
                    getRolloutGroupsCreateFromDefinitions(entity.getAdvancedRolloutGroupDefinitions()), conditions);
        }

        uiNotification.displaySuccess(i18n.getMessage("message.save.success", rolloutToCreate.getName()));
        eventBus.publish(EventTopics.ENTITY_MODIFIED, this, new EntityModifiedEventPayload(
                EntityModifiedEventType.ENTITY_ADDED, ProxyRollout.class, rolloutToCreate.getId()));
    }

    private List<RolloutGroupCreate> getRolloutGroupsCreateFromDefinitions(
            final List<ProxyAdvancedRolloutGroup> advancedRolloutGroupDefinitions) {
        final AdvancedRolloutGroupDefinitionToCreateMapper mapper = new AdvancedRolloutGroupDefinitionToCreateMapper(
                entityFactory);

        return advancedRolloutGroupDefinitions.stream().map(mapper::map).collect(Collectors.toList());
    }

    @Override
    protected boolean isEntityValid(final ProxyRolloutWindow entity) {
        if (entity == null) {
            uiNotification
                    .displayValidationError(i18n.getMessage("message.save.fail", i18n.getMessage("caption.rollout")));
            return false;
        }

        if (!StringUtils.hasText(entity.getName())) {
            uiNotification.displayValidationError(i18n.getMessage("message.rollout.name.empty"));
            return false;
        }

        final String trimmedName = StringUtils.trimWhitespace(entity.getName());
        if (rolloutManagement.getByName(trimmedName).isPresent()) {
            uiNotification.displayValidationError(i18n.getMessage("message.rollout.duplicate.check", trimmedName));
            return false;
        }

        return true;
    }
}
