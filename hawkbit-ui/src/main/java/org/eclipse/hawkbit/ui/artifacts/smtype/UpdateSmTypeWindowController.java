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
import org.eclipse.hawkbit.ui.common.AbstractUpdateEntityWindowController;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.EntityWindowLayout;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxySoftwareModule;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyType;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyType.SmTypeAssign;
import org.springframework.util.StringUtils;

/**
 * Controller for update software module type window
 */
public class UpdateSmTypeWindowController
        extends AbstractUpdateEntityWindowController<ProxyType, ProxyType, SoftwareModuleType> {

    private final SoftwareModuleTypeManagement smTypeManagement;
    private final SmTypeWindowLayout layout;

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
    protected String getDisplayableName(final ProxyType entity) {
        return entity.getName();
    }

    @Override
    protected String getDisplayableEntityTypeMessageKey() {
        return "caption.entity.software.module.type";
    }

    @Override
    protected Long getId(final SoftwareModuleType entity) {
        return entity.getId();
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
        if (!StringUtils.hasText(entity.getName()) || !StringUtils.hasText(entity.getKey())
                || entity.getSmTypeAssign() == null) {
            displayValidationError("message.error.missing.typenameorkeyorsmtype");
            return false;
        }

        final String trimmedName = StringUtils.trimWhitespace(entity.getName());
        final String trimmedKey = StringUtils.trimWhitespace(entity.getKey());
        if (!nameBeforeEdit.equals(trimmedName) && smTypeManagement.getByName(trimmedName).isPresent()) {
            displayValidationError("message.type.duplicate.check", trimmedName);
            return false;
        }
        if (!keyBeforeEdit.equals(trimmedKey) && smTypeManagement.getByKey(trimmedKey).isPresent()) {
            displayValidationError("message.type.key.swmodule.duplicate.check", trimmedKey);
            return false;
        }

        return true;
    }
}
