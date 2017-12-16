/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.dstable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetMetadata;
import org.eclipse.hawkbit.repository.model.MetaData;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.AbstractMetadataPopupLayout;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.data.domain.PageRequest;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Pop up layout to display distribution metadata.
 */
public class DsMetadataPopupLayout extends AbstractMetadataPopupLayout<DistributionSet, MetaData> {

    private static final long serialVersionUID = -7778944849012048106L;

    private final transient DistributionSetManagement distributionSetManagement;

    private final transient EntityFactory entityFactory;

    public DsMetadataPopupLayout(final VaadinMessageSource i18n, final UINotification uiNotification,
            final UIEventBus eventBus, final DistributionSetManagement distributionSetManagement,
            final EntityFactory entityFactory, final SpPermissionChecker permChecker) {
        super(i18n, uiNotification, eventBus, permChecker);
        this.distributionSetManagement = distributionSetManagement;
        this.entityFactory = entityFactory;
    }

    @Override
    protected boolean checkForDuplicate(final DistributionSet entity, final String value) {
        return distributionSetManagement.getMetaDataByDistributionSetId(entity.getId(), value).isPresent();
    }

    @Override
    protected DistributionSetMetadata createMetadata(final DistributionSet entity, final String key,
            final String value) {
        final DistributionSetMetadata dsMetaData = distributionSetManagement
                .createMetaData(entity.getId(), Arrays.asList(entityFactory.generateMetadata(key, value))).get(0);
        setSelectedEntity(dsMetaData.getDistributionSet());
        return dsMetaData;
    }

    @Override
    protected DistributionSetMetadata updateMetadata(final DistributionSet entity, final String key,
            final String value) {
        final DistributionSetMetadata dsMetaData = distributionSetManagement.updateMetaData(entity.getId(),
                entityFactory.generateMetadata(key, value));
        setSelectedEntity(dsMetaData.getDistributionSet());
        return dsMetaData;
    }

    @Override
    protected List<MetaData> getMetadataList() {
        return Collections.unmodifiableList(distributionSetManagement
                .findMetaDataByDistributionSetId(new PageRequest(0, 500), getSelectedEntity().getId()).getContent());
    }

    @Override
    protected void deleteMetadata(final DistributionSet entity, final String key) {
        distributionSetManagement.deleteMetaData(entity.getId(), key);
    }

    @Override
    protected boolean hasCreatePermission() {
        return permChecker.hasCreateRepositoryPermission();
    }

    @Override
    protected boolean hasUpdatePermission() {
        return permChecker.hasUpdateRepositoryPermission();
    }
}
