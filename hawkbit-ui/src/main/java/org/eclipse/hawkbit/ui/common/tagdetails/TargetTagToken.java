/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.tagdetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TargetTagManagement;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.mappers.TagToProxyTagMapper;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTag;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTarget;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload.EntityModifiedEventType;
import org.eclipse.hawkbit.ui.common.event.EventTopics;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;

/**
 * Implementation of Target tag token.
 *
 *
 */
public class TargetTagToken extends AbstractTagToken<ProxyTarget> {
    private final TargetTagManagement targetTagManagement;
    private final TargetManagement targetManagement;

    private final TagToProxyTagMapper<TargetTag> tagMapper;

    /**
     * Constructor for TargetTagToken
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param targetTagManagement
     *            TargetTagManagement
     * @param targetManagement
     *            TargetManagement
     */
    public TargetTagToken(final CommonUiDependencies uiDependencies, final TargetTagManagement targetTagManagement,
            final TargetManagement targetManagement) {
        super(uiDependencies);

        this.targetTagManagement = targetTagManagement;
        this.targetManagement = targetManagement;

        this.tagMapper = new TagToProxyTagMapper<>();
    }

    @Override
    public void assignTag(final ProxyTag tagData) {
        getMasterEntity().ifPresent(masterEntity -> {
            final Long masterEntityId = masterEntity.getId();

            final List<Target> assignedTargets = targetManagement
                    .assignTag(Collections.singleton(masterEntity.getControllerId()), tagData.getId());
            if (checkAssignmentResult(assignedTargets, masterEntityId)) {
                uiNotification.displaySuccess(getAssignmentMsgFor("message.assigned.one",
                        i18n.getMessage("caption.target"), masterEntity.getName(), tagData.getName()));
                eventBus.publish(EventTopics.ENTITY_MODIFIED, this, new EntityModifiedEventPayload(
                        EntityModifiedEventType.ENTITY_UPDATED, ProxyTarget.class, masterEntityId));
            }
        });
    }

    @Override
    public void unassignTag(final ProxyTag tagData) {
        getMasterEntity().ifPresent(masterEntity -> {
            final Long masterEntityId = masterEntity.getId();

            final Target unassignedTarget = targetManagement.unAssignTag(masterEntity.getControllerId(),
                    tagData.getId());
            if (checkUnassignmentResult(unassignedTarget, masterEntityId)) {
                uiNotification.displaySuccess(getAssignmentMsgFor("message.unassigned.one",
                        i18n.getMessage("caption.target"), masterEntity.getName(), tagData.getName()));
                eventBus.publish(EventTopics.ENTITY_MODIFIED, this, new EntityModifiedEventPayload(
                        EntityModifiedEventType.ENTITY_UPDATED, ProxyTarget.class, masterEntityId));
            }
        });
    }

    @Override
    protected Boolean isToggleTagAssignmentAllowed() {
        return checker.hasUpdateTargetPermission();
    }

    @Override
    protected List<ProxyTag> getAllTags() {
        return HawkbitCommonUtil.getEntitiesByPageableProvider(targetTagManagement::findAll).stream()
                .map(tag -> new ProxyTag(tag.getId(), tag.getName(), tag.getColour())).collect(Collectors.toList());
    }

    @Override
    protected List<ProxyTag> getAssignedTags() {
        return getMasterEntity().map(masterEntity -> HawkbitCommonUtil
                .getEntitiesByPageableProvider(p -> targetTagManagement.findByTarget(p, masterEntity.getControllerId()))
                .stream().map(tagMapper::map).collect(Collectors.toList())).orElse(Collections.emptyList());
    }

    @Override
    protected List<ProxyTag> getTagsById(final Collection<Long> entityIds) {
        return targetTagManagement.get(entityIds).stream().map(tagMapper::map).collect(Collectors.toList());
    }
}
