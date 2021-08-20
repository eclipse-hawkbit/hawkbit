/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettag.targettype;

import org.eclipse.hawkbit.repository.TargetTypeManagement;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.ui.common.AbstractAddNamedEntityWindowController;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.EntityWindowLayout;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTarget;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyType;
import org.eclipse.hawkbit.ui.common.type.ProxyTypeValidator;
import org.springframework.util.StringUtils;

/**
 * Add distribution set type window controller
 */
public class AddTargetTypeWindowController
        extends AbstractAddNamedEntityWindowController<ProxyType, ProxyType, TargetType> {

    private final TargetTypeWindowLayout layout;
    private final ProxyTypeValidator validator;
    private TargetTypeManagement targetTypeManagement;

    /**
     * Constructor for AddTargetTypeWindowController
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param targetTypeManagement
     *            targetTypeManagement
     * @param layout
     *            TargetTypeWindowLayout
     */
    public AddTargetTypeWindowController(final CommonUiDependencies uiDependencies,
                                         final TargetTypeManagement targetTypeManagement, final TargetTypeWindowLayout layout) {
        super(uiDependencies);

        this.targetTypeManagement = targetTypeManagement;
        this.layout = layout;
        this.validator = new ProxyTypeValidator(uiDependencies);
    }

    @Override
    public EntityWindowLayout<ProxyType> getLayout() {
        return layout;
    }

    @Override
    protected ProxyType buildEntityFromProxy(final ProxyType proxyEntity) {
        // We ignore the method parameter, because we are interested in the
        // empty object, that we can populate with defaults
        return new ProxyType();
    }

    @Override
    protected TargetType persistEntityInRepository(final ProxyType entity) {
        return targetTypeManagement.create(getEntityFactory().targetType().create()
                .name(entity.getName()).description(entity.getDescription()).colour(entity.getColour()));
    }

    @Override
    protected Class<? extends ProxyIdentifiableEntity> getEntityClass() {
        return ProxyType.class;
    }

    @Override
    protected Class<? extends ProxyIdentifiableEntity> getParentEntityClass() {
        return ProxyTarget.class;
    }

    @Override
    protected boolean isEntityValid(final ProxyType entity) {
        final String trimmedName = StringUtils.trimWhitespace(entity.getName());
        final String trimmedKey = StringUtils.trimWhitespace(entity.getKey());
        //TODO: Add the validator, check in DsType
        return true;
    }
}
