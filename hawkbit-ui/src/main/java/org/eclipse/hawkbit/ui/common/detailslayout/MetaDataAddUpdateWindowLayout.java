/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.detailslayout;

import java.util.function.BooleanSupplier;

import org.eclipse.hawkbit.ui.common.AbstractEntityWindowLayout;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyMetaData;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

/**
 * Class for metadata add/update window layout.
 */
public class MetaDataAddUpdateWindowLayout extends AbstractEntityWindowLayout<ProxyMetaData> {
    private final BooleanSupplier hasMetadataChangePermission;
    protected final MetaDataAddUpdateWindowLayoutComponentBuilder metaDataComponentBuilder;

    private final TextField metadataKey;
    private final TextArea metadataValue;

    /**
     * Constructor for AbstractTagWindowLayout
     * 
     * @param i18n
     *            VaadinMessageSource
     * @param hasMetadataChangePermission
     *            checks the permission allowing to change metadata entities
     */
    public MetaDataAddUpdateWindowLayout(final VaadinMessageSource i18n,
            final BooleanSupplier hasMetadataChangePermission) {
        super();

        this.hasMetadataChangePermission = hasMetadataChangePermission;
        this.metaDataComponentBuilder = new MetaDataAddUpdateWindowLayoutComponentBuilder(i18n);

        this.metadataKey = metaDataComponentBuilder.createKeyField(binder);
        this.metadataValue = metaDataComponentBuilder.createValueField(binder);
    }

    @Override
    public ComponentContainer getRootComponent() {
        final VerticalLayout addUpdateLayout = getMetadataAddUpdateLayout();

        if (!hasMetadataChangePermission.getAsBoolean()) {
            disableMetadataInputComponents();
        }

        return addUpdateLayout;
    }

    protected VerticalLayout getMetadataAddUpdateLayout() {
        final VerticalLayout addUpdateLayout = new VerticalLayout();
        addUpdateLayout.setSpacing(true);
        addUpdateLayout.setMargin(false);
        addUpdateLayout.setSizeFull();

        addUpdateLayout.addComponent(metadataKey);
        metadataKey.focus();

        addUpdateLayout.addComponent(metadataValue);
        addUpdateLayout.setExpandRatio(metadataValue, 1.0F);

        return addUpdateLayout;
    }

    protected void disableMetadataInputComponents() {
        metadataKey.setEnabled(false);
        metadataValue.setEnabled(false);
    }

    /**
     * Enable meta data key
     */
    public void enableMetadataKey() {
        if (hasMetadataChangePermission.getAsBoolean()) {
            metadataKey.setEnabled(true);
        }
    }

    /**
     * Disable meta data key
     */
    public void disableMetadataKey() {
        metadataKey.setEnabled(false);
    }
}
