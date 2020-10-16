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
import org.eclipse.hawkbit.ui.common.AbstractEntityWindowLayout;
import org.eclipse.hawkbit.ui.common.AbstractUpdateNamedEntityWindowController;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTag;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTarget;
import org.eclipse.hawkbit.ui.common.tag.ProxyTagValidator;
import org.eclipse.hawkbit.ui.management.tag.TagWindowLayout;
import org.springframework.util.StringUtils;

/**
 * Controller for Update target tag window
 */
public class UpdateTargetTagWindowController
        extends AbstractUpdateNamedEntityWindowController<ProxyTag, ProxyTag, Tag> {

    private final TargetTagManagement targetTagManagement;
    private final TagWindowLayout<ProxyTag> layout;
    private final ProxyTagValidator validator;

    private String nameBeforeEdit;

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
        super(uiDependencies);

        this.targetTagManagement = targetTagManagement;
        this.layout = layout;
        this.validator = new ProxyTagValidator(uiDependencies);
    }

    @Override
    public AbstractEntityWindowLayout<ProxyTag> getLayout() {
        return layout;
    }

    @Override
    protected ProxyTag buildEntityFromProxy(final ProxyTag proxyEntity) {
        final ProxyTag targetTag = new ProxyTag();

        targetTag.setId(proxyEntity.getId());
        targetTag.setName(proxyEntity.getName());
        targetTag.setDescription(proxyEntity.getDescription());
        targetTag.setColour(proxyEntity.getColour());

        nameBeforeEdit = proxyEntity.getName();

        return targetTag;
    }

    @Override
    protected void adaptLayout(final ProxyTag proxyEntity) {
        layout.disableTagName();
    }

    @Override
    protected Tag persistEntityInRepository(final ProxyTag entity) {
        final TagUpdate tagUpdate = getEntityFactory().tag().update(entity.getId()).name(entity.getName())
                .description(entity.getDescription()).colour(entity.getColour());
        return targetTagManagement.update(tagUpdate);
    }

    @Override
    protected Class<? extends ProxyIdentifiableEntity> getEntityClass() {
        return ProxyTag.class;
    }

    @Override
    protected Class<? extends ProxyIdentifiableEntity> getParentEntityClass() {
        return ProxyTarget.class;
    }

    @Override
    protected boolean isEntityValid(final ProxyTag entity) {
        final String trimmedName = StringUtils.trimWhitespace(entity.getName());
        return validator.isEntityValid(entity,
                () -> hasNamedChanged(trimmedName) && targetTagManagement.getByName(trimmedName).isPresent());
    }

    private boolean hasNamedChanged(final String trimmedName) {
        return !nameBeforeEdit.equals(trimmedName);
    }
}
