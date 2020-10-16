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
import org.eclipse.hawkbit.ui.common.AbstractEntityWindowLayout;
import org.eclipse.hawkbit.ui.common.AbstractUpdateNamedEntityWindowController;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyDistributionSet;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTag;
import org.eclipse.hawkbit.ui.common.tag.ProxyTagValidator;
import org.eclipse.hawkbit.ui.management.tag.TagWindowLayout;
import org.springframework.util.StringUtils;

/**
 * Controller for update distribution set tag window
 */
public class UpdateDsTagWindowController extends AbstractUpdateNamedEntityWindowController<ProxyTag, ProxyTag, Tag> {

    private final DistributionSetTagManagement dsTagManagement;
    private final TagWindowLayout<ProxyTag> layout;
    private final ProxyTagValidator validator;

    private String nameBeforeEdit;

    /**
     * Constructor for UpdateDsTagWindowController
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param dsTagManagement
     *            DistributionSetTagManagement
     * @param layout
     *            Tag window layout
     */
    public UpdateDsTagWindowController(final CommonUiDependencies uiDependencies,
            final DistributionSetTagManagement dsTagManagement, final TagWindowLayout<ProxyTag> layout) {
        super(uiDependencies);

        this.dsTagManagement = dsTagManagement;
        this.layout = layout;
        this.validator = new ProxyTagValidator(uiDependencies);
    }

    @Override
    public AbstractEntityWindowLayout<ProxyTag> getLayout() {
        return layout;
    }

    @Override
    protected ProxyTag buildEntityFromProxy(final ProxyTag proxyEntity) {
        final ProxyTag dsTag = new ProxyTag();

        dsTag.setId(proxyEntity.getId());
        dsTag.setName(proxyEntity.getName());
        dsTag.setDescription(proxyEntity.getDescription());
        dsTag.setColour(proxyEntity.getColour());

        nameBeforeEdit = proxyEntity.getName();

        return dsTag;
    }

    @Override
    protected void adaptLayout(final ProxyTag proxyEntity) {
        layout.disableTagName();
    }

    @Override
    protected Tag persistEntityInRepository(final ProxyTag entity) {
        final TagUpdate tagUpdate = getEntityFactory().tag().update(entity.getId()).name(entity.getName())
                .description(entity.getDescription()).colour(entity.getColour());
        return dsTagManagement.update(tagUpdate);
    }

    @Override
    protected Class<? extends ProxyIdentifiableEntity> getEntityClass() {
        return ProxyTag.class;
    }

    @Override
    protected Class<? extends ProxyIdentifiableEntity> getParentEntityClass() {
        return ProxyDistributionSet.class;
    }

    @Override
    protected boolean isEntityValid(final ProxyTag entity) {
        final String trimmedName = StringUtils.trimWhitespace(entity.getName());
        return validator.isEntityValid(entity,
                () -> hasNamedChanged(trimmedName) && dsTagManagement.getByName(trimmedName).isPresent());
    }

    private boolean hasNamedChanged(final String trimmedName) {
        return !nameBeforeEdit.equals(trimmedName);
    }
}
