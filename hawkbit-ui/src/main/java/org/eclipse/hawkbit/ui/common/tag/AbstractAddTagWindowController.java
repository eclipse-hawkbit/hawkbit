/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.tag;

import org.eclipse.hawkbit.repository.builder.TagCreate;
import org.eclipse.hawkbit.repository.model.Tag;
import org.eclipse.hawkbit.ui.common.UIConfiguration;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTag;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload.EntityModifiedEventType;
import org.eclipse.hawkbit.ui.common.event.EventTopics;
import org.eclipse.hawkbit.ui.management.tag.TagWindowLayout;
import org.springframework.util.StringUtils;

/**
 * Abstract base class for add tag window controller.
 */
public abstract class AbstractAddTagWindowController extends AbstractTagWindowController {

    /**
     * Constructor for AbstractDsTagWindowController.
     *
     * @param uiConfig
     *            {@link UIConfiguration}
     * @param layout
     *            TagWindowLayout
     * @param parentType
     *            parent type for publishing event
     */
    public AbstractAddTagWindowController(final UIConfiguration uiConfig, final TagWindowLayout<ProxyTag> layout,
            final Class<? extends ProxyIdentifiableEntity> parentType) {
        super(uiConfig, layout, parentType);
    }

    @Override
    protected void persistEntity(final ProxyTag entity) {
        final TagCreate tagCreate = getEntityFactory().tag().create().name(entity.getName())
                .description(entity.getDescription()).colour(entity.getColour());
        final Tag newTag = createEntityInRepository(tagCreate);

        getUiNotification().displaySuccess(getI18n().getMessage("message.save.success", newTag.getName()));
        getEventBus().publish(EventTopics.ENTITY_MODIFIED, this, new EntityModifiedEventPayload(
                EntityModifiedEventType.ENTITY_ADDED, parentType, ProxyTag.class, newTag.getId()));
    }

    protected abstract Tag createEntityInRepository(TagCreate tagCreate);

    @Override
    protected ProxyTag buildEntityFromProxy(final ProxyTag proxyEntity) {
        // We ignore the method parameter, because we are interested in the
        // empty object, that we can populate with defaults
        return new ProxyTag();
    }

    @Override
    protected boolean isEntityValid(final ProxyTag entity) {
        if (!StringUtils.hasText(entity.getName())) {
            getUiNotification().displayValidationError(getI18n().getMessage("message.error.missing.tagname"));
            return false;
        }

        final String trimmedName = StringUtils.trimWhitespace(entity.getName());
        if (existsEntityInRepository(trimmedName)) {
            getUiNotification()
                    .displayValidationError(getI18n().getMessage("message.tag.duplicate.check", trimmedName));
            return false;
        }

        return true;
    }
}
