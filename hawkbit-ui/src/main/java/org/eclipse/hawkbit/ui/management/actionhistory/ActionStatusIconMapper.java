/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.actionhistory;

import java.util.Map;

import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;

import com.google.common.collect.Maps;
import com.vaadin.server.FontAwesome;

/**
 * Mapping utility for {@link ActionStatus} to icon in action history table.
 *
 */
public final class ActionStatusIconMapper {
    static final Map<Action.Status, ActionStatusIconMapper> MAPPINGS = Maps.newEnumMap(Action.Status.class);

    static {
        MAPPINGS.put(Action.Status.FINISHED, new ActionStatusIconMapper("label.finished",
                SPUIStyleDefinitions.STATUS_ICON_GREEN, FontAwesome.CHECK_CIRCLE));
        MAPPINGS.put(Action.Status.CANCELED, new ActionStatusIconMapper("label.cancelled",
                SPUIStyleDefinitions.STATUS_ICON_GREEN, FontAwesome.TIMES_CIRCLE));

        MAPPINGS.put(Action.Status.ERROR, new ActionStatusIconMapper("label.error",
                SPUIStyleDefinitions.STATUS_ICON_RED, FontAwesome.EXCLAMATION_CIRCLE));

        MAPPINGS.put(Action.Status.WARNING, new ActionStatusIconMapper("label.warning",
                SPUIStyleDefinitions.STATUS_ICON_ORANGE, FontAwesome.EXCLAMATION_CIRCLE));
        MAPPINGS.put(Action.Status.CANCEL_REJECTED, new ActionStatusIconMapper("label.warning",
                SPUIStyleDefinitions.STATUS_ICON_ORANGE, FontAwesome.EXCLAMATION_CIRCLE));

        MAPPINGS.put(Action.Status.RUNNING, new ActionStatusIconMapper("label.running",
                SPUIStyleDefinitions.STATUS_ICON_PENDING, FontAwesome.ADJUST));
        MAPPINGS.put(Action.Status.CANCELING, new ActionStatusIconMapper("label.cancelling",
                SPUIStyleDefinitions.STATUS_ICON_PENDING, FontAwesome.TIMES_CIRCLE));
        MAPPINGS.put(Action.Status.RETRIEVED, new ActionStatusIconMapper("label.retrieved",
                SPUIStyleDefinitions.STATUS_ICON_PENDING, FontAwesome.CIRCLE_O));
        MAPPINGS.put(Action.Status.DOWNLOAD, new ActionStatusIconMapper("label.download",
                SPUIStyleDefinitions.STATUS_ICON_PENDING, FontAwesome.CLOUD_DOWNLOAD));
        MAPPINGS.put(Action.Status.SCHEDULED, new ActionStatusIconMapper("label.scheduled",
                SPUIStyleDefinitions.STATUS_ICON_PENDING, FontAwesome.HOURGLASS_1));
    }

    private final String descriptionI18N;
    private final String styleName;
    private final FontAwesome icon;

    private ActionStatusIconMapper(final String descriptionI18N, final String styleName, final FontAwesome icon) {
        this.descriptionI18N = descriptionI18N;
        this.styleName = styleName;
        this.icon = icon;
    }

    String getDescriptionI18N() {
        return descriptionI18N;
    }

    String getStyleName() {
        return styleName;
    }

    FontAwesome getIcon() {
        return icon;
    }

}
