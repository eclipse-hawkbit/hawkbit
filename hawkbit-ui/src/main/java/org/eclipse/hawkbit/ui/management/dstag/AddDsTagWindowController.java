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
import org.eclipse.hawkbit.repository.builder.TagCreate;
import org.eclipse.hawkbit.repository.model.Tag;
import org.eclipse.hawkbit.ui.common.UIConfiguration;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyDistributionSet;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTag;
import org.eclipse.hawkbit.ui.common.tag.AbstractAddTagWindowController;
import org.eclipse.hawkbit.ui.management.tag.TagWindowLayout;

/**
 * Controller for add distribution tag window
 */
public class AddDsTagWindowController extends AbstractAddTagWindowController {

    private final DistributionSetTagManagement dsTagManagement;

    /**
     * Constructor for AddDsTagWindowController
     *
     * @param uiConfig
     *            {@link UIConfiguration}
     * @param dsTagManagement
     *            DistributionSetTagManagement
     * @param layout
     *            Tag window layout
     */
    public AddDsTagWindowController(final UIConfiguration uiConfig, final DistributionSetTagManagement dsTagManagement,
            final TagWindowLayout<ProxyTag> layout) {
        super(uiConfig, layout, ProxyDistributionSet.class);
        this.dsTagManagement = dsTagManagement;
    }

    @Override
    protected Tag createEntityInRepository(final TagCreate tagCreate) {
        return dsTagManagement.create(tagCreate);
    }

    @Override
    protected boolean existsEntityInRepository(final String trimmedName) {
        return dsTagManagement.getByName(trimmedName).isPresent();
    }
}