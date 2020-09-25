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
import org.eclipse.hawkbit.repository.builder.TagCreate;
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
import org.springframework.util.StringUtils;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Abstract base class for add tag window controller.
 */
public abstract class AbstractAddTagWindowController extends AbstractEntityWindowController<ProxyTag, ProxyTag> {

    private final VaadinMessageSource i18n;
    private final EntityFactory entityFactory;
    private final UIEventBus eventBus;
    private final UINotification uiNotification;
    private final TagWindowLayout<ProxyTag> layout;
    private final Class<? extends ProxyIdentifiableEntity> parentType;

    /**
     * Constructor for AbstractDsTagWindowController.
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
     */
    public AbstractAddTagWindowController(final VaadinMessageSource i18n, final EntityFactory entityFactory,
            final UIEventBus eventBus, final UINotification uiNotification, final TagWindowLayout<ProxyTag> layout,
            final Class<? extends ProxyIdentifiableEntity> parentType) {
        this.i18n = i18n;
        this.entityFactory = entityFactory;
        this.eventBus = eventBus;
        this.uiNotification = uiNotification;
        this.layout = layout;
        this.parentType = parentType;
    }

    @Override
    protected void persistEntity(final ProxyTag entity) {
        final TagCreate tagCreate = entityFactory.tag().create().name(entity.getName())
                .description(entity.getDescription()).colour(entity.getColour());
        final Tag newTag = createEntityInRepository(tagCreate);

        uiNotification.displaySuccess(i18n.getMessage("message.save.success", newTag.getName()));
        eventBus.publish(EventTopics.ENTITY_MODIFIED, this, new EntityModifiedEventPayload(
                EntityModifiedEventType.ENTITY_ADDED, parentType, ProxyTag.class, newTag.getId()));
    }

    protected abstract Tag createEntityInRepository(TagCreate tagCreate);

    @Override
    public AbstractEntityWindowLayout<ProxyTag> getLayout() {
        return layout;
    }

    @Override
    protected ProxyTag buildEntityFromProxy(final ProxyTag proxyEntity) {
        // We ignore the method parameter, because we are interested in the
        // empty object, that we can populate with defaults
        return new ProxyTag();
    }

    @Override
    protected boolean isEntityValid(final ProxyTag entity) {
        if (!StringUtils.hasText(entity.getName())) {
            uiNotification.displayValidationError(i18n.getMessage("message.error.missing.tagname"));
            return false;
        }

        final String trimmedName = StringUtils.trimWhitespace(entity.getName());
        if (getTagByNameFromRepository(trimmedName).isPresent()) {
            uiNotification.displayValidationError(i18n.getMessage("message.tag.duplicate.check", trimmedName));
            return false;
        }

        return true;
    }

    protected abstract <T extends Tag> Optional<T> getTagByNameFromRepository(String trimmedName);
}
