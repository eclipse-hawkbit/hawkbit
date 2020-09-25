/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.dstag;

import java.util.Optional;

import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.builder.TagUpdate;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.Tag;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyDistributionSet;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTag;
import org.eclipse.hawkbit.ui.common.tag.AbstractUpdateTagWindowController;
import org.eclipse.hawkbit.ui.management.tag.TagWindowLayout;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Controller for update distribution set tag window
 */
public class UpdateDsTagWindowController extends AbstractUpdateTagWindowController {
    private static final Logger LOG = LoggerFactory.getLogger(UpdateDsTagWindowController.class);

    private final DistributionSetTagManagement dsTagManagement;

    /**
     * Constructor for UpdateDsTagWindowController
     *
     * @param i18n
     *            VaadinMessageSource
     * @param entityFactory
     *            EntityFactory
     * @param eventBus
     *            UIEventBus
     * @param uiNotification
     *            UINotification
     * @param dsTagManagement
     *            DistributionSetTagManagement
     * @param layout
     *            Tag window layout
     */
    public UpdateDsTagWindowController(final VaadinMessageSource i18n, final EntityFactory entityFactory,
            final UIEventBus eventBus, final UINotification uiNotification,
            final DistributionSetTagManagement dsTagManagement, final TagWindowLayout<ProxyTag> layout) {
        super(i18n, entityFactory, eventBus, uiNotification, layout, ProxyDistributionSet.class,
                "caption.entity.distribution.tag");

        this.dsTagManagement = dsTagManagement;
    }

    @Override
    protected Tag updateEntityInRepository(final TagUpdate tagUpdate) {
        return dsTagManagement.update(tagUpdate);

    }

    @Override
    protected Optional<DistributionSetTag> getTagByNameFromRepository(final String trimmedName) {
        return dsTagManagement.getByName(trimmedName);
    }
}
