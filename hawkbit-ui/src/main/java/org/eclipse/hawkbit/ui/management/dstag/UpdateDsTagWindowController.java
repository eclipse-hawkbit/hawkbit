/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.dstag;

import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.repository.builder.TagUpdate;
import org.eclipse.hawkbit.repository.model.Tag;
import org.eclipse.hawkbit.ui.common.UIConfiguration;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyDistributionSet;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTag;
import org.eclipse.hawkbit.ui.common.tag.AbstractUpdateTagWindowController;
import org.eclipse.hawkbit.ui.management.tag.TagWindowLayout;

/**
 * Controller for update distribution set tag window
 */
public class UpdateDsTagWindowController extends AbstractUpdateTagWindowController {

    private final DistributionSetTagManagement dsTagManagement;

    /**
     * Constructor for UpdateDsTagWindowController
     *
     * @param uiConfig
     *            {@link UIConfiguration}
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
    public UpdateDsTagWindowController(final UIConfiguration uiConfig,
            final DistributionSetTagManagement dsTagManagement, final TagWindowLayout<ProxyTag> layout) {
        super(uiConfig, layout, ProxyDistributionSet.class, "caption.entity.distribution.tag");

        this.dsTagManagement = dsTagManagement;
    }

    @Override
    protected Tag updateEntityInRepository(final TagUpdate tagUpdate) {
        return dsTagManagement.update(tagUpdate);

    }

    @Override
    protected boolean existsEntityInRepository(final String trimmedName) {
        return dsTagManagement.getByName(trimmedName).isPresent();
    }
}
