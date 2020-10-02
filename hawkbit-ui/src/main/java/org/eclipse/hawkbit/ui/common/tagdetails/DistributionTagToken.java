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

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.mappers.TagToProxyTagMapper;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyDistributionSet;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTag;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload.EntityModifiedEventType;
import org.eclipse.hawkbit.ui.common.event.EventTopics;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;

/**
 * Implementation of target/ds tag token layout.
 *
 */
public class DistributionTagToken extends AbstractTagToken<ProxyDistributionSet> {
    private final DistributionSetTagManagement distributionSetTagManagement;
    private final DistributionSetManagement distributionSetManagement;

    private final TagToProxyTagMapper<DistributionSetTag> tagMapper;

    /**
     * Constructor for DistributionTagToken
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param distributionSetTagManagement
     *            DistributionSetTagManagement
     * @param distributionSetManagement
     *            DistributionSetManagement
     */
    public DistributionTagToken(final CommonUiDependencies uiDependencies,
            final DistributionSetTagManagement distributionSetTagManagement,
            final DistributionSetManagement distributionSetManagement) {
        super(uiDependencies);

        this.distributionSetTagManagement = distributionSetTagManagement;
        this.distributionSetManagement = distributionSetManagement;

        this.tagMapper = new TagToProxyTagMapper<>();
    }

    @Override
    public void assignTag(final ProxyTag tagData) {
        getMasterEntity().ifPresent(masterEntity -> {
            final Long masterEntityId = masterEntity.getId();

            final List<DistributionSet> assignedDistributionSets = distributionSetManagement
                    .assignTag(Collections.singleton(masterEntityId), tagData.getId());
            if (checkAssignmentResult(assignedDistributionSets, masterEntityId)) {
                uiNotification.displaySuccess(getAssignmentMsgFor("message.assigned.one",
                        i18n.getMessage("caption.distribution"), masterEntity.getName(), tagData.getName()));
                eventBus.publish(EventTopics.ENTITY_MODIFIED, this, new EntityModifiedEventPayload(
                        EntityModifiedEventType.ENTITY_UPDATED, ProxyDistributionSet.class, masterEntityId));
            }
        });
    }

    @Override
    public void unassignTag(final ProxyTag tagData) {
        getMasterEntity().ifPresent(masterEntity -> {
            final Long masterEntityId = masterEntity.getId();

            final DistributionSet unAssignedDistributionSet = distributionSetManagement.unAssignTag(masterEntityId,
                    tagData.getId());
            if (checkUnassignmentResult(unAssignedDistributionSet, masterEntityId)) {
                uiNotification.displaySuccess(getAssignmentMsgFor("message.unassigned.one",
                        i18n.getMessage("caption.distribution"), masterEntity.getName(), tagData.getName()));
                eventBus.publish(EventTopics.ENTITY_MODIFIED, this, new EntityModifiedEventPayload(
                        EntityModifiedEventType.ENTITY_UPDATED, ProxyDistributionSet.class, masterEntityId));
            }
        });
    }

    @Override
    protected Boolean isToggleTagAssignmentAllowed() {
        return checker.hasUpdateRepositoryPermission();
    }

    @Override
    protected List<ProxyTag> getAllTags() {
        return HawkbitCommonUtil.getEntitiesByPageableProvider(distributionSetTagManagement::findAll).stream()
                .map(tagMapper::map).collect(Collectors.toList());
    }

    @Override
    protected List<ProxyTag> getAssignedTags() {
        return getMasterEntity().map(masterEntity -> HawkbitCommonUtil
                .getEntitiesByPageableProvider(
                        p -> distributionSetTagManagement.findByDistributionSet(p, masterEntity.getId()))
                .stream().map(tagMapper::map).collect(Collectors.toList())).orElse(Collections.emptyList());
    }

    @Override
    protected List<ProxyTag> getTagsById(final Collection<Long> entityIds) {
        return distributionSetTagManagement.get(entityIds).stream().map(tagMapper::map).collect(Collectors.toList());
    }
}
