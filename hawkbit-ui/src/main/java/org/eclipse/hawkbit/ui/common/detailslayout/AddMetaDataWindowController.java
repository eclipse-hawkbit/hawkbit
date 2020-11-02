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
import org.eclipse.hawkbit.ui.common.AbstractAddEntityWindowController;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.EntityWindowLayout;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyMetaData;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload;
import org.springframework.util.StringUtils;

/**
 * Controller to add meta data window
 */
public class AddMetaDataWindowController
        extends AbstractAddEntityWindowController<ProxyMetaData, ProxyMetaData, MetaData> {
    private final MetaDataAddUpdateWindowLayout layout;

    private final Function<ProxyMetaData, MetaData> createMetaDataCallback;
    private final Predicate<String> duplicateCheckCallback;
    private final ProxyMetadataValidator validator;

    /**
     * Constructor for AddMetaDataWindowController
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param layout
     *            MetaDataAddUpdateWindowLayout
     * @param createMetaDataCallback
     *            Create meta data callback
     * @param duplicateCheckCallback
     *            Duplicate check callback
     */
    public AddMetaDataWindowController(final CommonUiDependencies uiDependencies,
            final MetaDataAddUpdateWindowLayout layout, final Function<ProxyMetaData, MetaData> createMetaDataCallback,
            final Predicate<String> duplicateCheckCallback) {
        super(uiDependencies);

        this.layout = layout;
        this.createMetaDataCallback = createMetaDataCallback;
        this.duplicateCheckCallback = duplicateCheckCallback;
        this.validator = new ProxyMetadataValidator(uiDependencies);
    }

    @Override
    protected ProxyMetaData buildEntityFromProxy(final ProxyMetaData proxyEntity) {
        // We ignore the method parameter, because we are interested in the
        // empty object, that we can populate with defaults
        return new ProxyMetaData();
    }

    @Override
    public EntityWindowLayout<ProxyMetaData> getLayout() {
        return layout;
    }

    @Override
    protected void adaptLayout(final ProxyMetaData proxyEntity) {
        layout.enableMetadataKey();
    }

    @Override
    protected String getPersistSuccessMessageKey() {
        return "message.metadata.saved";
    }

    @Override
    protected MetaData persistEntityInRepository(final ProxyMetaData entity) {
        return createMetaDataCallback.apply(entity);
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
    protected void publishModifiedEvent(final EntityModifiedEventPayload eventPayload) {
        // do not publish entity created because it is already managed by the
        // update callback that sends the event for parent entity of metadata
    }

    @Override
    protected Long getId(final MetaData entity) {
        return entity.getEntityId();
    }

    @Override
    protected Class<? extends ProxyIdentifiableEntity> getEntityClass() {
        return ProxyMetaData.class;
    }

    @Override
    protected boolean isEntityValid(final ProxyMetaData entity) {
        final String trimmedKey = StringUtils.trimWhitespace(entity.getKey());
        return validator.isEntityValid(entity, () -> duplicateCheckCallback.test(trimmedKey));
    }

    @Override
    protected boolean closeWindowAfterSave() {
        return false;
    }
}
