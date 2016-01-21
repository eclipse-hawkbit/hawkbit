/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.footer;

import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIComponetIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;

import com.vaadin.ui.Component;
import com.vaadin.ui.DragAndDropWrapper;

/**
 * Delete actions layout helper class.
 *
 */
public final class DeleteActionsLayoutHelper {

    private DeleteActionsLayoutHelper() {

    }

    /**
     * Checks if component is a target tag.
     * 
     * @param source
     *            Component dropped
     * @return true if it component is target tag
     */
    public static boolean isTargetTag(final Component source) {
        if (source instanceof DragAndDropWrapper) {
            final String wrapperData = ((DragAndDropWrapper) source).getData().toString();
            final String id = wrapperData.replace(SPUIDefinitions.TARGET_TAG_BUTTON, "");
            if (wrapperData.contains(SPUIDefinitions.TARGET_TAG_BUTTON) && !id.trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if component is distribution tag.
     * 
     * @param source
     *            component dropped
     * @return true if it component is distribution tag
     */
    public static boolean isDistributionTag(final Component source) {
        if (source instanceof DragAndDropWrapper) {
            final String wrapperData = ((DragAndDropWrapper) source).getData().toString();
            final String id = wrapperData.replace(SPUIDefinitions.DISTRIBUTION_TAG_BUTTON, "");
            if (wrapperData.contains(SPUIDefinitions.DISTRIBUTION_TAG_BUTTON) && !id.trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if component is target table.
     * 
     * @param source
     *            component dropped
     * @return true if it component is target table
     */
    public static boolean isTargetTable(final Component source) {
        return HawkbitCommonUtil.bothSame(source.getId(), SPUIComponetIdProvider.TARGET_TABLE_ID);
    }

    /**
     * Checks id component is distribution table.
     * 
     * @param source
     *            component dropped
     * @return true if it component is distribution table
     */
    public static boolean isDistributionTable(final Component source) {
        return HawkbitCommonUtil.bothSame(source.getId(), SPUIComponetIdProvider.DIST_TABLE_ID);
    }

    /**
     * Check if dropped component can be deleted.
     * 
     * @param source
     *            component dropped
     * @return true if component can be deleted
     */
    public static Boolean isComponentDeletable(final Component source) {
        if (isTargetTable(source) || isDistributionTable(source) || isTargetTag(source) || isDistributionTag(source)) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

}
