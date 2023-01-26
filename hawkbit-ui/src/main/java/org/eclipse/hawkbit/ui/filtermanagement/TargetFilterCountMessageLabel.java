/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.filtermanagement;

import java.util.function.IntSupplier;

import org.eclipse.hawkbit.ui.common.layout.AbstractFooterSupport;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

/**
 * Count message label which display current total filtered targets count.
 */
public class TargetFilterCountMessageLabel extends AbstractFooterSupport {

    private int totalFilteredTargetsCount;

    /**
     * Constructor for TargetFilterCountMessageLabel
     *
     * @param i18n
     *            VaadinMessageSource
     */
    public TargetFilterCountMessageLabel(final VaadinMessageSource i18n, final UINotification notification) {
        super(i18n, notification);
    }

    @Override
    protected void init() {
        super.init();

        totalFilteredTargetsCount = 0;
        updateTotalFilteredTargetsCountLabel();
    }

    /**
     * Update the total count of filtered targets asynchronously.
     * 
     * @param fetchTotalFilteredTargetsCount
     *            total filtered targets count provider
     *
     */
    public void updateTotalFilteredTargetsCount(final IntSupplier fetchTotalFilteredTargetsCount) {
        updateCountAsynchronously(() -> totalFilteredTargetsCount = fetchTotalFilteredTargetsCount.getAsInt(),
                this::updateTotalFilteredTargetsCountLabel);
    }

    private void updateTotalFilteredTargetsCountLabel() {
        final StringBuilder targetMessage = new StringBuilder(i18n.getMessage("label.target.filtered.total"));
        targetMessage.append(": ");
        targetMessage.append(totalFilteredTargetsCount);

        countLabel.setCaption(targetMessage.toString());
    }
}
