/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.tag;

import org.eclipse.hawkbit.repository.builder.TagUpdate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.EntityReadOnlyException;
import org.eclipse.hawkbit.repository.model.Tag;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTag;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload.EntityModifiedEventType;
import org.eclipse.hawkbit.ui.common.event.EventTopics;
import org.eclipse.hawkbit.ui.management.tag.TagWindowLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * Abstract base class for update tag window controller.
 */
public abstract class AbstractUpdateTagWindowController extends AbstractTagWindowController {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractUpdateTagWindowController.class);

    private final String keyForEntityTypeInNotifications;

    private String nameBeforeEdit;

    /**
     * Constructor for AbstractUpdateTagWindowController.
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param layout
     *            TagWindowLayout
     * @param parentType
     *            parent type for publishing event
     * @param keyForEntityTypeInNotifications
     *            i18n key to get the entity type info to be used in
     *            notifications
     */
    public AbstractUpdateTagWindowController(final CommonUiDependencies uiDependencies, final TagWindowLayout<ProxyTag> layout,
            final Class<? extends ProxyIdentifiableEntity> parentType, final String keyForEntityTypeInNotifications) {
        super(uiDependencies, layout, parentType);

        this.keyForEntityTypeInNotifications = keyForEntityTypeInNotifications;
    }

    @Override
    protected void adaptLayout(final ProxyTag proxyEntity) {
        layout.disableTagName();
    }

    @Override
    protected ProxyTag buildEntityFromProxy(final ProxyTag proxyEntity) {
        final ProxyTag proxyTag = new ProxyTag();

        proxyTag.setId(proxyEntity.getId());
        proxyTag.setName(proxyEntity.getName());
        proxyTag.setDescription(proxyEntity.getDescription());
        proxyTag.setColour(proxyEntity.getColour());

        nameBeforeEdit = proxyEntity.getName();

        return proxyTag;
    }

    @Override
    protected void persistEntity(final ProxyTag entity) {
        final TagUpdate tagUpdate = getEntityFactory().tag().update(entity.getId()).name(entity.getName())
                .description(entity.getDescription()).colour(entity.getColour());

        try {
            final Tag updatedTag = updateEntityInRepository(tagUpdate);

            displaySuccess("message.update.success", updatedTag.getName());
            getEventBus().publish(EventTopics.ENTITY_MODIFIED, this, new EntityModifiedEventPayload(
                    EntityModifiedEventType.ENTITY_UPDATED, parentType, ProxyTag.class, updatedTag.getId()));
        } catch (final EntityNotFoundException | EntityReadOnlyException e) {
            final String entityType = getI18n().getMessage(keyForEntityTypeInNotifications);
            LOG.trace("Update of {} failed in UI: {}", entityType, e.getMessage());
            displayWarning("message.deleted.or.notAllowed", entityType, entity.getName());
        }
    }

    protected abstract Tag updateEntityInRepository(TagUpdate tagUpdate);

    @Override
    protected boolean isEntityValid(final ProxyTag entity) {
        if (!StringUtils.hasText(entity.getName())) {
            displayValidationError("message.error.missing.tagname");
            return false;
        }

        final String trimmedName = StringUtils.trimWhitespace(entity.getName());
        if (!nameBeforeEdit.equals(trimmedName) && existsEntityInRepository(trimmedName)) {
            displayValidationError("message.tag.duplicate.check", trimmedName);
            return false;
        }

        return true;
    }
}
