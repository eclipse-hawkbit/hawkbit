/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.smtable;

import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleUpdate;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.ui.common.AbstractUpdateEntityWindowController;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.EntityWindowLayout;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxySoftwareModule;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.springframework.util.StringUtils;

/**
 * Controller for update software module window
 */
public class UpdateSmWindowController
        extends AbstractUpdateEntityWindowController<ProxySoftwareModule, ProxySoftwareModule, SoftwareModule> {

    private final SoftwareModuleManagement smManagement;
    private final SmWindowLayout layout;

    /**
     * Constructor for UpdateSmWindowController
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param smManagement
     *            SoftwareModuleManagement
     * @param layout
     *            SmWindowLayout
     */
    public UpdateSmWindowController(final CommonUiDependencies uiDependencies,
            final SoftwareModuleManagement smManagement, final SmWindowLayout layout) {
        super(uiDependencies);

        this.smManagement = smManagement;
        this.layout = layout;
    }

    @Override
    protected ProxySoftwareModule buildEntityFromProxy(final ProxySoftwareModule proxyEntity) {
        final ProxySoftwareModule sm = new ProxySoftwareModule();

        sm.setId(proxyEntity.getId());
        sm.setTypeInfo(proxyEntity.getTypeInfo());
        sm.setName(proxyEntity.getName());
        sm.setVersion(proxyEntity.getVersion());
        sm.setVendor(proxyEntity.getVendor());
        sm.setDescription(proxyEntity.getDescription());

        return sm;
    }

    @Override
    public EntityWindowLayout<ProxySoftwareModule> getLayout() {
        return layout;
    }

    @Override
    protected void adaptLayout(final ProxySoftwareModule proxyEntity) {
        layout.disableSmTypeSelect();
        layout.disableNameField();
        layout.disableVersionField();
    }

    @Override
    protected SoftwareModule persistEntityInRepository(final ProxySoftwareModule entity) {
        final SoftwareModuleUpdate smUpdate = getEntityFactory().softwareModule().update(entity.getId())
                .vendor(entity.getVendor()).description(entity.getDescription());
        return smManagement.update(smUpdate);
    }

    @Override
    protected String getDisplayableName(final ProxySoftwareModule entity) {
        return HawkbitCommonUtil.getFormattedNameVersion(entity.getName(), entity.getVersion());
    }

    @Override
    protected String getDisplayableEntityTypeMessageKey() {
        return "caption.software.module";
    }

    @Override
    protected Long getId(final SoftwareModule entity) {
        return entity.getId();
    }

    @Override
    protected Class<? extends ProxyIdentifiableEntity> getEntityClass() {
        return ProxySoftwareModule.class;
    }

    @Override
    protected boolean isEntityValid(final ProxySoftwareModule entity) {
        if (!StringUtils.hasText(entity.getName()) || !StringUtils.hasText(entity.getVersion())
                || entity.getTypeInfo() == null) {
            displayValidationError("message.error.missing.nameorversionortype");
            return false;
        }

        return true;
    }
}
