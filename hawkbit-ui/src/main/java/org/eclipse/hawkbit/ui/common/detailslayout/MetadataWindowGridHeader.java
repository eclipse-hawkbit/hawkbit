/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.detailslayout;

import java.util.function.BooleanSupplier;

import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.grid.header.AbstractGridHeader;
import org.eclipse.hawkbit.ui.common.grid.header.support.AddHeaderSupport;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;

import com.vaadin.ui.Component;

/**
 * Target table header layout.
 */
public class MetadataWindowGridHeader extends AbstractGridHeader {
    private static final long serialVersionUID = 1L;

    private final transient AddHeaderSupport addHeaderSupport;

    /**
     * Constructor for MetadataWindowGridHeader
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param hasMetadataChangePermission
     *            checks the permission allowing to change metadata entities
     * @param addNewItemCallback
     *            Runnable
     */
    public MetadataWindowGridHeader(final CommonUiDependencies uiDependencies,final BooleanSupplier hasMetadataChangePermission, final Runnable addNewItemCallback) {
        super(uiDependencies.getI18n(), uiDependencies.getPermChecker(), uiDependencies.getEventBus());

        if (hasMetadataChangePermission.getAsBoolean()) {
            this.addHeaderSupport = new AddHeaderSupport(i18n, UIComponentIdProvider.METADATA_ADD_ICON_ID,
                    addNewItemCallback, () -> false);
        } else {
            this.addHeaderSupport = null;
        }

        addHeaderSupport(addHeaderSupport);

        buildHeader();
    }

    @Override
    protected void init() {
        super.init();

        setWidth("100%");
        setHeight("30px");
        addStyleName("metadata-table-margin");
    }

    @Override
    protected Component getHeaderCaption() {
        return SPUIComponentProvider.generateCaptionLabel(i18n, "caption.metadata");
    }
}
