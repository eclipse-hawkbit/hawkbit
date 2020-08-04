/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.grid.header.support;

import java.util.function.BooleanSupplier;

import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleNoBorder;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;

/**
 * Class for bulk upload
 */
public class BulkUploadHeaderSupport implements HeaderSupport {
    private final VaadinMessageSource i18n;

    private final Button bulkUploadIcon;
    private final Runnable bulkUploadCallback;
    private final BooleanSupplier bulkUploadInProgressStateSupplier;
    private final BooleanSupplier maximizedStateSupplier;

    /**
     * Constructor for BulkUploadHeaderSupport
     *
     * @param i18n
     *            VaadinMessageSource
     * @param bulkUploadCallback
     *            Runnable
     * @param bulkUploadInProgressStateSupplier
     *            BooleanSupplier
     * @param maximizedStateSupplier
     *            BooleanSupplier
     */
    public BulkUploadHeaderSupport(final VaadinMessageSource i18n, final Runnable bulkUploadCallback,
            final BooleanSupplier bulkUploadInProgressStateSupplier, final BooleanSupplier maximizedStateSupplier) {
        this.i18n = i18n;

        this.bulkUploadCallback = bulkUploadCallback;
        this.bulkUploadInProgressStateSupplier = bulkUploadInProgressStateSupplier;
        this.maximizedStateSupplier = maximizedStateSupplier;

        this.bulkUploadIcon = createBulkUploadIcon();
    }

    private Button createBulkUploadIcon() {
        final Button bulkUploadButton = SPUIComponentProvider.getButton(
                UIComponentIdProvider.TARGET_TBL_BULK_UPLOAD_ICON_ID, "",
                i18n.getMessage(UIMessageIdProvider.TOOLTIP_BULK_UPLOAD), null, false, VaadinIcons.UPLOAD,
                SPUIButtonStyleNoBorder.class);

        bulkUploadButton.addClickListener(event -> bulkUploadCallback.run());

        return bulkUploadButton;
    }

    /**
     * Disable bulk upload
     */
    public void disableBulkUpload() {
        bulkUploadIcon.setEnabled(false);
    }

    /**
     * Enable bulk upload
     */
    public void enableBulkUpload() {
        bulkUploadIcon.setEnabled(true);
    }

    /**
     * Hide bulk upload
     */
    public void hideBulkUpload() {
        bulkUploadIcon.setVisible(false);
    }

    /**
     * Show bulk upload
     */
    public void showBulkUpload() {
        bulkUploadIcon.setVisible(true);
    }

    /**
     * Show upload progress indicator
     */
    public void showProgressIndicator() {
        bulkUploadIcon.addStyleName(SPUIStyleDefinitions.BULK_UPLOAD_PROGRESS_INDICATOR_STYLE);
        bulkUploadIcon.setIcon(null);
    }

    /**
     * Hide upload progress indicator
     */
    public void hideProgressIndicator() {
        bulkUploadIcon.removeStyleName(SPUIStyleDefinitions.BULK_UPLOAD_PROGRESS_INDICATOR_STYLE);
        bulkUploadIcon.setIcon(VaadinIcons.UPLOAD);
    }

    @Override
    public Component getHeaderComponent() {
        return bulkUploadIcon;
    }

    @Override
    public void restoreState() {
        if (maximizedStateSupplier.getAsBoolean()) {
            hideBulkUpload();
        }

        if (bulkUploadInProgressStateSupplier.getAsBoolean()) {
            showProgressIndicator();
        }
    }
}
