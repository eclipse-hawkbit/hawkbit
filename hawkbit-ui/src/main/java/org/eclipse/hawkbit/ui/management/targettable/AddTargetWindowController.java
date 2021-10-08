/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettable;

import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.ui.common.AbstractAddNamedEntityWindowController;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.EntityWindowLayout;
import org.eclipse.hawkbit.ui.common.data.mappers.TargetToProxyTargetMapper;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTarget;
import org.eclipse.hawkbit.ui.common.event.EventLayout;
import org.eclipse.hawkbit.ui.common.event.EventView;
import org.eclipse.hawkbit.ui.common.event.SelectionChangedEventPayload;
import org.eclipse.hawkbit.ui.common.event.SelectionChangedEventPayload.SelectionChangedEventType;
import org.springframework.util.StringUtils;

/**
 * Controller for add target window
 */
public class AddTargetWindowController
        extends AbstractAddNamedEntityWindowController<ProxyTarget, ProxyTarget, Target> {

    private final TargetManagement targetManagement;
    private final TargetWindowLayout layout;
    private final EventView view;
    private final ProxyTargetValidator proxyTargetValidator;

    /**
     * Constructor for AddTargetWindowController
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param targetManagement
     *            TargetManagement
     * @param layout
     *            TargetWindowLayout
     * @param view
     *            EventView
     */
    public AddTargetWindowController(final CommonUiDependencies uiDependencies, final TargetManagement targetManagement,
            final TargetWindowLayout layout, final EventView view) {
        super(uiDependencies);

        this.targetManagement = targetManagement;
        this.layout = layout;
        this.view = view;
        this.proxyTargetValidator = new ProxyTargetValidator(uiDependencies);
    }

    @Override
    protected ProxyTarget buildEntityFromProxy(final ProxyTarget proxyEntity) {
        // We ignore the method parameter, because we are interested in the
        // empty object, that we can populate with defaults
        return new ProxyTarget();
    }

    @Override
    public EntityWindowLayout<ProxyTarget> getLayout() {
        return layout;
    }

    @Override
    protected void adaptLayout(final ProxyTarget proxyEntity) {
        layout.setControllerIdEnabled(true);
        layout.setNameRequired(false);
    }

    @Override
    protected Target persistEntityInRepository(final ProxyTarget entity) {
        return targetManagement.create(getEntityFactory().target().create().controllerId(entity.getControllerId())
                .name(entity.getName()).description(entity.getDescription())
                .targetType(entity.getTypeInfo() != null ? entity.getTypeInfo().getId() : null));
    }

    @Override
    protected String getDisplayableName(final Target entity) {
        return entity.getName() == null ? entity.getControllerId() : entity.getName();
    }

    @Override
    protected String getDisplayableNameForFailedMessage(final ProxyTarget entity) {
        return entity.getName() == null ? entity.getControllerId() : entity.getName();
    }

    @Override
    protected Class<ProxyTarget> getEntityClass() {
        return ProxyTarget.class;
    }

    @Override
    protected void selectPersistedEntity(final Target entity) {
        final ProxyTarget addedItem = new TargetToProxyTargetMapper(getI18n()).map(entity);
        publishSelectionEvent(new SelectionChangedEventPayload<>(SelectionChangedEventType.ENTITY_SELECTED, addedItem,
                EventLayout.TARGET_LIST, view));
    }

    @Override
    protected boolean isEntityValid(final ProxyTarget entity) {
        final String trimmedControllerId = StringUtils.trimWhitespace(entity.getControllerId());
        return proxyTargetValidator.isEntityValid(entity,
                () -> targetManagement.getByControllerID(trimmedControllerId).isPresent());
    }

}
