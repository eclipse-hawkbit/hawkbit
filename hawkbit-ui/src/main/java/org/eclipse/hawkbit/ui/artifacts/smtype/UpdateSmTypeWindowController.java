/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.smtype;

import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleTypeUpdate;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.ui.common.AbstractUpdateNamedEntityWindowController;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.EntityWindowLayout;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxySoftwareModule;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyType;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyType.SmTypeAssign;
import org.eclipse.hawkbit.ui.common.type.ProxyTypeValidator;
import org.springframework.util.StringUtils;

/**
 * Controller for update software module type window
 */
public class UpdateSmTypeWindowController
        extends AbstractUpdateNamedEntityWindowController<ProxyType, ProxyType, SoftwareModuleType> {

    private final SoftwareModuleTypeManagement smTypeManagement;
    private final SmTypeWindowLayout layout;
    private final ProxyTypeValidator validator;

    private String nameBeforeEdit;
    private String keyBeforeEdit;

    /**
     * Constructor for UpdateSmTypeWindowController
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param smTypeManagement
     *            SoftwareModuleTypeManagement
     * @param layout
     *            SmTypeWindowLayout
     */
    public UpdateSmTypeWindowController(final CommonUiDependencies uiDependencies,
            final SoftwareModuleTypeManagement smTypeManagement, final SmTypeWindowLayout layout) {
        super(uiDependencies);

        this.smTypeManagement = smTypeManagement;
        this.layout = layout;
        this.validator = new ProxyTypeValidator(uiDependencies);
    }

    @Override
    protected ProxyType buildEntityFromProxy(final ProxyType proxyEntity) {
        final ProxyType smType = new ProxyType();

        smType.setId(proxyEntity.getId());
        smType.setName(proxyEntity.getName());
        smType.setDescription(proxyEntity.getDescription());
        smType.setColour(proxyEntity.getColour());
        smType.setKey(proxyEntity.getKey());
        smType.setSmTypeAssign(getSmTypeAssignById(proxyEntity.getId()));

        nameBeforeEdit = proxyEntity.getName();
        keyBeforeEdit = proxyEntity.getKey();

        return smType;
    }

    private SmTypeAssign getSmTypeAssignById(final Long id) {
        return smTypeManagement.get(id)
                .map(smType -> smType.getMaxAssignments() == 1 ? SmTypeAssign.SINGLE : SmTypeAssign.MULTI)
                .orElse(SmTypeAssign.SINGLE);
    }

    @Override
    public EntityWindowLayout<ProxyType> getLayout() {
        return layout;
    }

    @Override
    protected void adaptLayout(final ProxyType proxyEntity) {
        layout.disableTagName();
        layout.disableTypeKey();
        layout.disableTypeAssignOptionGroup();
    }

    @Override
    protected SoftwareModuleType persistEntityInRepository(final ProxyType entity) {
        final SoftwareModuleTypeUpdate smTypeUpdate = getEntityFactory().softwareModuleType().update(entity.getId())
                .description(entity.getDescription()).colour(entity.getColour());
        return smTypeManagement.update(smTypeUpdate);
    }

    @Override
    protected Class<? extends ProxyIdentifiableEntity> getEntityClass() {
        return ProxyType.class;
    }

    @Override
    protected Class<? extends ProxyIdentifiableEntity> getParentEntityClass() {
        return ProxySoftwareModule.class;
    }

    @Override
    protected boolean isEntityValid(final ProxyType entity) {
        final String trimmedName = StringUtils.trimWhitespace(entity.getName());
        final String trimmedKey = StringUtils.trimWhitespace(entity.getKey());
        return validator.isSmTypeValid(entity,
                () -> hasKeyChanged(trimmedKey) && smTypeManagement.getByKey(trimmedKey).isPresent(),
                () -> hasNameChanged(trimmedName) && smTypeManagement.getByName(trimmedName).isPresent());
    }

    private boolean hasNameChanged(final String trimmedName) {
        return !nameBeforeEdit.equals(trimmedName);
    }

    private boolean hasKeyChanged(final String trimmedKey) {
        return !keyBeforeEdit.equals(trimmedKey);
    }
}
