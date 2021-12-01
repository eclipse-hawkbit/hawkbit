/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.smtable;

import org.eclipse.hawkbit.repository.ArtifactEncryptionService;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleCreate;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.ui.common.AbstractAddNamedEntityWindowController;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.EntityWindowLayout;
import org.eclipse.hawkbit.ui.common.data.mappers.SoftwareModuleToProxyMapper;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxySoftwareModule;
import org.eclipse.hawkbit.ui.common.event.EventLayout;
import org.eclipse.hawkbit.ui.common.event.EventView;
import org.eclipse.hawkbit.ui.common.event.SelectionChangedEventPayload;
import org.eclipse.hawkbit.ui.common.event.SelectionChangedEventPayload.SelectionChangedEventType;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.springframework.util.StringUtils;

/**
 * Controller for populating and saving data in Add Software Module Window.
 */
public class AddSmWindowController
        extends AbstractAddNamedEntityWindowController<ProxySoftwareModule, ProxySoftwareModule, SoftwareModule> {

    private final SoftwareModuleManagement smManagement;
    private final SmWindowLayout layout;
    private final EventView view;
    private final ProxySmValidator validator;

    /**
     * Constructor
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param smManagement
     *            SoftwareModuleManagement
     * @param layout
     *            SoftwareModuleWindowLayout
     * @param view
     *            EventView
     */
    public AddSmWindowController(final CommonUiDependencies uiDependencies, final SoftwareModuleManagement smManagement,
            final SmWindowLayout layout, final EventView view) {
        super(uiDependencies);

        this.smManagement = smManagement;
        this.layout = layout;
        this.view = view;
        this.validator = new ProxySmValidator(uiDependencies);
    }

    @Override
    public EntityWindowLayout<ProxySoftwareModule> getLayout() {
        return layout;
    }

    @Override
    protected void adaptLayout(final ProxySoftwareModule proxyEntity) {
        if (!ArtifactEncryptionService.getInstance().isEncryptionSupported()) {
            layout.disableEncryptionField();
        }
    }

    @Override
    protected ProxySoftwareModule buildEntityFromProxy(final ProxySoftwareModule proxyEntity) {
        // We ignore the method parameter, because we are interested in the
        // empty object, that we can populate with defaults
        return new ProxySoftwareModule();
    }

    @Override
    protected SoftwareModule persistEntityInRepository(final ProxySoftwareModule entity) {
        final SoftwareModuleCreate smCreate = getEntityFactory().softwareModule().create()
                .type(entity.getTypeInfo().getKey()).name(entity.getName()).version(entity.getVersion())
                .vendor(entity.getVendor()).description(entity.getDescription()).encrypted(entity.isEncrypted());

        return smManagement.create(smCreate);
    }

    @Override
    protected String getDisplayableName(final SoftwareModule entity) {
        return HawkbitCommonUtil.getFormattedNameVersion(entity.getName(), entity.getVersion());
    }

    @Override
    protected String getDisplayableNameForFailedMessage(final ProxySoftwareModule entity) {
        return HawkbitCommonUtil.getFormattedNameVersion(entity.getName(), entity.getVersion());
    }

    @Override
    protected Class<? extends ProxyIdentifiableEntity> getEntityClass() {
        return ProxySoftwareModule.class;
    }

    @Override
    protected void selectPersistedEntity(final SoftwareModule entity) {
        final ProxySoftwareModule addedItem = new SoftwareModuleToProxyMapper().map(entity);
        publishSelectionEvent(new SelectionChangedEventPayload<>(SelectionChangedEventType.ENTITY_SELECTED, addedItem,
                EventLayout.SM_LIST, view));
    }

    @Override
    protected boolean isEntityValid(final ProxySoftwareModule entity) {
        final String trimmedName = StringUtils.trimWhitespace(entity.getName());
        final String trimmedVersion = StringUtils.trimWhitespace(entity.getVersion());
        final Long typeId = entity.getTypeInfo().getId();
        return validator.isEntityValid(entity,
                () -> smManagement.getByNameAndVersionAndType(trimmedName, trimmedVersion, typeId).isPresent());
    }
}
