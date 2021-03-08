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
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.Identifiable;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyNamedEntity;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTag;
import org.eclipse.hawkbit.ui.common.layout.MasterEntityAwareComponent;
import org.eclipse.hawkbit.ui.common.tagdetails.TagPanelLayout.TagAssignmentListener;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.util.CollectionUtils;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Abstract class for target/ds tag token layout.
 *
 * @param <T>
 *            the special entity
 */
public abstract class AbstractTagToken<T extends ProxyNamedEntity>
        implements TagAssignmentListener, MasterEntityAwareComponent<T> {
    protected TagPanelLayout tagPanelLayout;

    protected final SpPermissionChecker checker;
    protected final VaadinMessageSource i18n;
    protected final UINotification uiNotification;
    protected final UIEventBus eventBus;

    private T masterEntity;

    protected AbstractTagToken(final CommonUiDependencies uiDependencies) {
        this.checker = uiDependencies.getPermChecker();
        this.i18n = uiDependencies.getI18n();
        this.uiNotification = uiDependencies.getUiNotification();
        this.eventBus = uiDependencies.getEventBus();

        buildTagPanel();
        tagPanelLayout.setVisible(false);
    }

    private void buildTagPanel() {
        tagPanelLayout = new TagPanelLayout(i18n, !isToggleTagAssignmentAllowed());
        tagPanelLayout.addTagAssignmentListener(this);

        tagPanelLayout.setSpacing(false);
        tagPanelLayout.setMargin(false);
        tagPanelLayout.setSizeFull();
    }

    @Override
    public void masterEntityChanged(final T changedMasterEntity) {
        if (changedMasterEntity == null && masterEntity == null) {
            return;
        }

        masterEntity = changedMasterEntity;

        if (changedMasterEntity == null) {
            tagPanelLayout.initializeTags(Collections.emptyList(), Collections.emptyList());
            tagPanelLayout.setVisible(false);
        } else {
            tagPanelLayout.initializeTags(getAllTags(), getAssignedTags());
            tagPanelLayout.setVisible(true);
        }
    }

    /**
     * @return Master entity
     */
    public Optional<T> getMasterEntity() {
        return Optional.ofNullable(masterEntity);
    }

    protected void tagCreated(final ProxyTag tagData) {
        tagPanelLayout.tagCreated(tagData);
    }

    protected void tagUpdated(final ProxyTag tagData) {
        tagPanelLayout.tagUpdated(tagData);
    }

    protected void tagDeleted(final Long tagId) {
        tagPanelLayout.tagDeleted(tagId);
    }

    protected boolean checkAssignmentResult(final List<? extends Identifiable<Long>> assignedEntities,
            final Long expectedAssignedEntityId) {
        if (!CollectionUtils.isEmpty(assignedEntities) && expectedAssignedEntityId != null) {
            final List<Long> assignedDsIds = assignedEntities.stream().map(Identifiable::getId)
                    .collect(Collectors.toList());
            if (assignedDsIds.contains(expectedAssignedEntityId)) {
                return true;
            }
        }
        return false;
    }

    protected boolean checkUnassignmentResult(final Identifiable<Long> unAssignedEntity,
            final Long expectedUnAssignedEntityId) {
        return unAssignedEntity != null && expectedUnAssignedEntityId != null
                && unAssignedEntity.getId().equals(expectedUnAssignedEntityId);
    }

    protected String getAssignmentMsgFor(final String assignmentMsgKey, final String assignedEntityType,
            final String assignedEntityName, final String tagName) {
        return i18n.getMessage(assignmentMsgKey, assignedEntityType, assignedEntityName, i18n.getMessage("caption.tag"),
                tagName);
    }

    protected abstract Boolean isToggleTagAssignmentAllowed();

    protected abstract List<ProxyTag> getAllTags();

    protected abstract List<ProxyTag> getAssignedTags();

    /**
     * Add tags
     *
     * @param entityIds
     *            List of entity id
     */
    public void onTagsAdded(final Collection<Long> entityIds) {
        getTagsById(entityIds).forEach(this::tagCreated);
    }

    protected abstract List<ProxyTag> getTagsById(final Collection<Long> entityIds);

    /**
     * Update tags
     *
     * @param entityIds
     *            List of entity id
     */
    public void onTagsUpdated(final Collection<Long> entityIds) {
        getTagsById(entityIds).forEach(this::tagUpdated);
    }

    /**
     * Delete tags
     *
     * @param entityIds
     *            List of entity id
     */
    public void onTagsDeleted(final Collection<Long> entityIds) {
        entityIds.forEach(this::tagDeleted);
    }

    /**
     * @return Tag panel layout
     */
    public TagPanelLayout getTagPanel() {
        return tagPanelLayout;
    }
}
