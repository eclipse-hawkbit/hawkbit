/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.dstable;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.ui.common.AbstractAddNamedEntityWindowController;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.EntityWindowLayout;
import org.eclipse.hawkbit.ui.common.data.mappers.DistributionSetToProxyDistributionMapper;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyDistributionSet;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTypeInfo;
import org.eclipse.hawkbit.ui.common.event.EventLayout;
import org.eclipse.hawkbit.ui.common.event.EventView;
import org.eclipse.hawkbit.ui.common.event.SelectionChangedEventPayload;
import org.eclipse.hawkbit.ui.common.event.SelectionChangedEventPayload.SelectionChangedEventType;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.springframework.util.StringUtils;

/**
 * Controller for add distribution set window
 */
public class AddDsWindowController
        extends AbstractAddNamedEntityWindowController<ProxyDistributionSet, ProxyDistributionSet, DistributionSet> {

    private final SystemManagement systemManagement;
    private final DistributionSetManagement dsManagement;
    private final DsWindowLayout layout;
    private final EventView view;
    private final ProxyDsValidator validator;

    /**
     * Constructor for AddDsWindowController
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param systemManagement
     *            SystemManagement
     * @param dsManagement
     *            DistributionSetManagement
     * @param layout
     *            DsWindowLayout
     * @param view
     *            EventView
     */
    public AddDsWindowController(final CommonUiDependencies uiDependencies, final SystemManagement systemManagement,
            final DistributionSetManagement dsManagement, final DsWindowLayout layout, final EventView view) {
        super(uiDependencies);

        this.systemManagement = systemManagement;
        this.dsManagement = dsManagement;
        this.layout = layout;
        this.view = view;
        this.validator = new ProxyDsValidator(uiDependencies);
    }

    @Override
    public EntityWindowLayout<ProxyDistributionSet> getLayout() {
        return layout;
    }

    @Override
    protected ProxyDistributionSet buildEntityFromProxy(final ProxyDistributionSet proxyEntity) {
        // We ignore the method parameter, because we are interested in the
        // empty object, that we can populate with defaults
        final ProxyDistributionSet newDs = new ProxyDistributionSet();

        final ProxyTypeInfo newType = new ProxyTypeInfo();
        newType.setId(systemManagement.getTenantMetadata().getDefaultDsType().getId());
        newDs.setTypeInfo(newType);

        return newDs;
    }

    @Override
    protected DistributionSet persistEntityInRepository(final ProxyDistributionSet entity) {
        return dsManagement.create(getEntityFactory().distributionSet().create().type(entity.getTypeInfo().getKey())
                .name(entity.getName()).version(entity.getVersion()).description(entity.getDescription())
                .requiredMigrationStep(entity.isRequiredMigrationStep()));
    }

    @Override
    protected String getDisplayableName(final DistributionSet entity) {
        return HawkbitCommonUtil.getFormattedNameVersion(entity.getName(), entity.getVersion());
    }

    @Override
    protected String getDisplayableNameForFailedMessage(final ProxyDistributionSet entity) {
        return HawkbitCommonUtil.getFormattedNameVersion(entity.getName(), entity.getVersion());
    }

    @Override
    protected Class<? extends ProxyIdentifiableEntity> getEntityClass() {
        return ProxyDistributionSet.class;
    }

    @Override
    protected void selectPersistedEntity(final DistributionSet entity) {
        final ProxyDistributionSet addedItem = new DistributionSetToProxyDistributionMapper().map(entity);
        publishSelectionEvent(new SelectionChangedEventPayload<>(SelectionChangedEventType.ENTITY_SELECTED, addedItem,
                EventLayout.DS_LIST, view));
    }

    @Override
    protected boolean isEntityValid(final ProxyDistributionSet entity) {
        final String trimmedName = StringUtils.trimWhitespace(entity.getName());
        final String trimmedVersion = StringUtils.trimWhitespace(entity.getVersion());
        return validator.isEntityValid(entity,
                () -> dsManagement.getByNameAndVersion(trimmedName, trimmedVersion).isPresent());
    }
}
