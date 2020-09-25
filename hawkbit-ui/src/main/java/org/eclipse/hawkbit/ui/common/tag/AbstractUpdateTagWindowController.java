/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.tag;

import java.util.Optional;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.builder.TagUpdate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.EntityReadOnlyException;
import org.eclipse.hawkbit.repository.model.Tag;
import org.eclipse.hawkbit.ui.common.AbstractEntityWindowController;
import org.eclipse.hawkbit.ui.common.AbstractEntityWindowLayout;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTag;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload.EntityModifiedEventType;
import org.eclipse.hawkbit.ui.common.event.EventTopics;
import org.eclipse.hawkbit.ui.management.tag.TagWindowLayout;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Abstract base class for update tag window controller.
 */
public abstract class AbstractUpdateTagWindowController extends AbstractEntityWindowController<ProxyTag, ProxyTag> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractUpdateTagWindowController.class);

    private final VaadinMessageSource i18n;
    private final EntityFactory entityFactory;
    private final UIEventBus eventBus;
    private final UINotification uiNotification;
    private final TagWindowLayout<ProxyTag> layout;
    private final Class<? extends ProxyIdentifiableEntity> parentType;
    private final String keyForEntityTypeInNotifications;

    private String nameBeforeEdit;

    /**
     * Constructor for AbstractUpdateTagWindowController.
     *
     * @param i18n
     *            VaadinMessageSource
     * @param entityFactory
     *            EntityFactory
     * @param eventBus
     *            UIEventBus
     * @param uiNotification
     *            UINotification
     * @param layout
     *            TagWindowLayout
     * @param parentType
     *            parent type for publishing event
     * @param keyForEntityTypeInNotifications
     *            i18n key to get the entity type info to be used in
     *            notifications
     */
    public AbstractUpdateTagWindowController(final VaadinMessageSource i18n, final EntityFactory entityFactory,
            final UIEventBus eventBus, final UINotification uiNotification, final TagWindowLayout<ProxyTag> layout,
            final Class<? extends ProxyIdentifiableEntity> parentType, final String keyForEntityTypeInNotifications) {
        this.i18n = i18n;
        this.entityFactory = entityFactory;
        this.eventBus = eventBus;
        this.uiNotification = uiNotification;
        this.layout = layout;
        this.parentType = parentType;
        this.keyForEntityTypeInNotifications = keyForEntityTypeInNotifications;
    }

    @Override
    public AbstractEntityWindowLayout<ProxyTag> getLayout() {
        return layout;
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
        final TagUpdate tagUpdate = entityFactory.tag().update(entity.getId()).name(entity.getName())
                .description(entity.getDescription()).colour(entity.getColour());

        try {
            final Tag updatedTag = updateEntityInRepository(tagUpdate);

            uiNotification.displaySuccess(i18n.getMessage("message.update.success", updatedTag.getName()));
            eventBus.publish(EventTopics.ENTITY_MODIFIED, this, new EntityModifiedEventPayload(
                    EntityModifiedEventType.ENTITY_UPDATED, parentType, ProxyTag.class, updatedTag.getId()));
        } catch (final EntityNotFoundException | EntityReadOnlyException e) {
            final String entityType = i18n.getMessage(keyForEntityTypeInNotifications);
            LOG.trace("Update of {} failed in UI: {}", entityType, e.getMessage());
            getUiNotification()
                    .displayWarning(getI18NMessage("message.deleted.or.notAllowed", entityType, entity.getName()));
        }
    }

    protected abstract Tag updateEntityInRepository(TagUpdate tagUpdate);

    protected String getI18NMessage(final String code, final Object... args) {
        return i18n.getMessage(code, args);
    }

    /**
     * Return {@link UINotification}
     *
     * @return the uiNotification
     */
    public UINotification getUiNotification() {
        return uiNotification;
    }

    @Override
    protected boolean isEntityValid(final ProxyTag entity) {
        if (!StringUtils.hasText(entity.getName())) {
            uiNotification.displayValidationError(i18n.getMessage("message.error.missing.tagname"));
            return false;
        }

        final String trimmedName = StringUtils.trimWhitespace(entity.getName());
        if (!nameBeforeEdit.equals(trimmedName) && getTagByNameFromRepository(trimmedName).isPresent()) {
            uiNotification.displayValidationError(i18n.getMessage("message.tag.duplicate.check", trimmedName));
            return false;
        }

        return true;
    }

    protected abstract <T extends Tag> Optional<T> getTagByNameFromRepository(String trimmedName);
}
