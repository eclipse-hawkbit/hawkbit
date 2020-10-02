/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettag;

import org.eclipse.hawkbit.repository.TargetTagManagement;
import org.eclipse.hawkbit.repository.builder.TagCreate;
import org.eclipse.hawkbit.repository.model.Tag;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTag;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTarget;
import org.eclipse.hawkbit.ui.common.tag.AbstractAddTagWindowController;
import org.eclipse.hawkbit.ui.management.tag.TagWindowLayout;

/**
 * Controller for add target tag window
 */
public class AddTargetTagWindowController extends AbstractAddTagWindowController {

    private final TargetTagManagement targetTagManagement;

    /**
     * Constructor for AddTargetTagWindowController
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param targetTagManagement
     *            TargetTagManagement
     * @param layout
     *            TagWindowLayout
     */
    public AddTargetTagWindowController(final CommonUiDependencies uiDependencies, final TargetTagManagement targetTagManagement,
            final TagWindowLayout<ProxyTag> layout) {
        super(uiDependencies, layout, ProxyTarget.class);

        this.targetTagManagement = targetTagManagement;
    }

    @Override
    protected Tag createEntityInRepository(final TagCreate tagCreate) {
        return targetTagManagement.create(tagCreate);
    }

    @Override
    protected boolean existsEntityInRepository(final String trimmedName) {
        return targetTagManagement.getByName(trimmedName).isPresent();
    }
}