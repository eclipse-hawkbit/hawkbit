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
import org.eclipse.hawkbit.repository.builder.TagUpdate;
import org.eclipse.hawkbit.repository.model.Tag;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTag;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTarget;
import org.eclipse.hawkbit.ui.common.tag.AbstractUpdateTagWindowController;
import org.eclipse.hawkbit.ui.management.tag.TagWindowLayout;

/**
 * Controller for Update target tag window
 */
public class UpdateTargetTagWindowController extends AbstractUpdateTagWindowController {

    private final TargetTagManagement targetTagManagement;

    /**
     * Constructor for UpdateTargetTagWindowController
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param targetTagManagement
     *            TargetTagManagement
     * @param layout
     *            TagWindowLayout
     */
    public UpdateTargetTagWindowController(final CommonUiDependencies uiDependencies,
            final TargetTagManagement targetTagManagement, final TagWindowLayout<ProxyTag> layout) {
        super(uiDependencies, layout, ProxyTarget.class, "caption.entity.target.tag");

        this.targetTagManagement = targetTagManagement;
    }

    @Override
    protected Tag updateEntityInRepository(final TagUpdate tagUpdate) {
        return targetTagManagement.update(tagUpdate);
    }

    @Override
    protected boolean existsEntityInRepository(final String trimmedName) {
        return targetTagManagement.getByName(trimmedName).isPresent();
    }
}
