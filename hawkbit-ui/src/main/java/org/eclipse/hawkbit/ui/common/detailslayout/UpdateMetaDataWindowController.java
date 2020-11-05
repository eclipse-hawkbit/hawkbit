/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.detailslayout;

import java.util.function.Function;
import java.util.function.Predicate;

import org.eclipse.hawkbit.repository.model.MetaData;
import org.eclipse.hawkbit.ui.common.AbstractUpdateEntityWindowController;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.EntityWindowLayout;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyMetaData;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload;
import org.springframework.util.StringUtils;

/**
 * Controller for update meta data window
 */
public class UpdateMetaDataWindowController
        extends AbstractUpdateEntityWindowController<ProxyMetaData, ProxyMetaData, MetaData> {

    private final MetaDataAddUpdateWindowLayout layout;
    private final Function<ProxyMetaData, MetaData> updateMetaDataCallback;
    private final Predicate<String> duplicateCheckCallback;
    private final ProxyMetadataValidator validator;

    private String keyBeforeEdit;

    /**
     * Constructor for UpdateMetaDataWindowController
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param layout
     *            MetaDataAddUpdateWindowLayout
     * @param updateMetaDataCallback
     *            Update meta data call back function for event listener
     * @param duplicateCheckCallback
     *            call back to check if entity already exists in repository
     */
    public UpdateMetaDataWindowController(final CommonUiDependencies uiDependencies,
            final MetaDataAddUpdateWindowLayout layout, final Function<ProxyMetaData, MetaData> updateMetaDataCallback,
            final Predicate<String> duplicateCheckCallback) {
        super(uiDependencies);

        this.layout = layout;
        this.updateMetaDataCallback = updateMetaDataCallback;
        this.duplicateCheckCallback = duplicateCheckCallback;
        this.validator = new ProxyMetadataValidator(uiDependencies);
    }

    @Override
    protected ProxyMetaData buildEntityFromProxy(final ProxyMetaData proxyEntity) {
        final ProxyMetaData metaData = new ProxyMetaData();

        metaData.setKey(proxyEntity.getKey());
        metaData.setValue(proxyEntity.getValue());
        metaData.setEntityId(proxyEntity.getEntityId());
        metaData.setVisibleForTargets(proxyEntity.isVisibleForTargets());

        keyBeforeEdit = proxyEntity.getKey();

        return metaData;
    }

    @Override
    public EntityWindowLayout<ProxyMetaData> getLayout() {
        return layout;
    }

    @Override
    protected void adaptLayout(final ProxyMetaData proxyEntity) {
        layout.disableMetadataKey();
    }

    @Override
    protected boolean closeWindowAfterSave() {
        return false;
    }

    @Override
    protected MetaData persistEntityInRepository(final ProxyMetaData entity) {
        return updateMetaDataCallback.apply(entity);
    }

    @Override
    protected String getPersistSuccessMessageKey() {
        return "message.metadata.updated";
    }

    @Override
    protected String getPersistFailureMessageKey() {
        return "message.key.deleted.or.notAllowed";
    }

    @Override
    protected String getDisplayableName(final MetaData entity) {
        return entity.getKey();
    }

    @Override
    protected String getDisplayableNameForFailedMessage(final ProxyMetaData entity) {
        return entity.getKey();
    }

    @Override
    protected Long getId(final MetaData entity) {
        return entity.getEntityId();
    }

    @Override
    protected void publishModifiedEvent(final EntityModifiedEventPayload eventPayload) {
        // do not publish entity updated because it is already managed by the
        // update callback that sends the event for parent entity of metadata
    }

    @Override
    protected Class<? extends ProxyIdentifiableEntity> getEntityClass() {
        return ProxyMetaData.class;
    }

    @Override
    protected boolean isEntityValid(final ProxyMetaData entity) {
        final String trimmedKey = StringUtils.trimWhitespace(entity.getKey());
        return validator.isEntityValid(entity,
                () -> hasKeyChanged(trimmedKey) && duplicateCheckCallback.test(trimmedKey));
    }

    private boolean hasKeyChanged(final String trimmedKey) {
        return !keyBeforeEdit.equals(trimmedKey);
    }
}
