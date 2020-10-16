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
import org.eclipse.hawkbit.repository.builder.DistributionSetUpdate;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.ui.common.AbstractUpdateNamedEntityWindowController;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.EntityWindowLayout;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyDistributionSet;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.springframework.util.StringUtils;

/**
 * Controller for update distribution set window
 */
public class UpdateDsWindowController
        extends AbstractUpdateNamedEntityWindowController<ProxyDistributionSet, ProxyDistributionSet, DistributionSet> {

    private final DistributionSetManagement dsManagement;
    private final DsWindowLayout layout;
    private final ProxyDsValidator validator;

    private String nameBeforeEdit;
    private String versionBeforeEdit;

    /**
     * Constructor for UpdateDsWindowController
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param dsManagement
     *            DistributionSetManagement
     * @param layout
     *            DsWindowLayout
     */
    public UpdateDsWindowController(final CommonUiDependencies uiDependencies,
            final DistributionSetManagement dsManagement, final DsWindowLayout layout) {
        super(uiDependencies);

        this.dsManagement = dsManagement;
        this.layout = layout;
        this.validator = new ProxyDsValidator(uiDependencies);
    }

    @Override
    protected ProxyDistributionSet buildEntityFromProxy(final ProxyDistributionSet proxyEntity) {
        final ProxyDistributionSet ds = new ProxyDistributionSet();

        ds.setId(proxyEntity.getId());
        ds.setTypeInfo(proxyEntity.getTypeInfo());
        ds.setName(proxyEntity.getName());
        ds.setVersion(proxyEntity.getVersion());
        ds.setDescription(proxyEntity.getDescription());
        ds.setRequiredMigrationStep(proxyEntity.isRequiredMigrationStep());

        nameBeforeEdit = proxyEntity.getName();
        versionBeforeEdit = proxyEntity.getVersion();

        return ds;
    }

    @Override
    public EntityWindowLayout<ProxyDistributionSet> getLayout() {
        return layout;
    }

    @Override
    protected void adaptLayout(final ProxyDistributionSet proxyEntity) {
        layout.disableDsTypeSelect();
    }

    @Override
    protected DistributionSet persistEntityInRepository(final ProxyDistributionSet entity) {
        final DistributionSetUpdate dsUpdate = getEntityFactory().distributionSet().update(entity.getId())
                .name(entity.getName()).version(entity.getVersion()).description(entity.getDescription())
                .requiredMigrationStep(entity.isRequiredMigrationStep());
        return dsManagement.update(dsUpdate);
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
    protected boolean isEntityValid(final ProxyDistributionSet entity) {
        final String trimmedName = StringUtils.trimWhitespace(entity.getName());
        final String trimmedVersion = StringUtils.trimWhitespace(entity.getVersion());
        return validator.isEntityValid(entity, () -> hasNameOrVersionChanged(trimmedName, trimmedVersion)
                && dsManagement.getByNameAndVersion(trimmedName, trimmedVersion).isPresent());
    }

    private boolean hasNameOrVersionChanged(final String trimmedName, final String trimmedVersion) {
        return !nameBeforeEdit.equals(trimmedName) || !versionBeforeEdit.equals(trimmedVersion);
    }
}
