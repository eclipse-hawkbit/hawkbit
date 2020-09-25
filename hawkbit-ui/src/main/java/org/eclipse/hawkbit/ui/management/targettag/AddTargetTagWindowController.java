/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettag;

import java.util.Optional;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.TargetTagManagement;
import org.eclipse.hawkbit.repository.builder.TagCreate;
import org.eclipse.hawkbit.repository.model.Tag;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTag;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTarget;
import org.eclipse.hawkbit.ui.common.tag.AbstractAddTagWindowController;
import org.eclipse.hawkbit.ui.management.tag.TagWindowLayout;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Controller for add target tag window
 */
public class AddTargetTagWindowController extends AbstractAddTagWindowController {

    private final TargetTagManagement targetTagManagement;

    /**
     * Constructor for AddTargetTagWindowController
     *
     * @param i18n
     *            VaadinMessageSource
     * @param entityFactory
     *            EntityFactory
     * @param eventBus
     *            UIEventBus
     * @param uiNotification
     *            UINotification
     * @param targetTagManagement
     *            TargetTagManagement
     * @param layout
     *            TagWindowLayout
     */
    public AddTargetTagWindowController(final VaadinMessageSource i18n, final EntityFactory entityFactory,
            final UIEventBus eventBus, final UINotification uiNotification,
            final TargetTagManagement targetTagManagement, final TagWindowLayout<ProxyTag> layout) {
        super(i18n, entityFactory, eventBus, uiNotification, layout, ProxyTarget.class);

        this.targetTagManagement = targetTagManagement;
    }

    @Override
    protected Tag createEntityInRepository(final TagCreate tagCreate) {
        return targetTagManagement.create(tagCreate);
    }

    @Override
    protected Optional<TargetTag> getTagByNameFromRepository(final String trimmedName) {
        return targetTagManagement.getByName(trimmedName);
    }
}