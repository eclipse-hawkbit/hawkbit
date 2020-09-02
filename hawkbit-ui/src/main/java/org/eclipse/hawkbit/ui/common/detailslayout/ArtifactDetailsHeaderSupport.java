/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.detailslayout;

import org.eclipse.hawkbit.ui.common.grid.header.support.HeaderSupport;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleNoBorder;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;

/**
 * Add and toggle icon in artifact detail header
 */
public class ArtifactDetailsHeaderSupport implements HeaderSupport {
    private final VaadinMessageSource i18n;

    private final String artifactDetailsIconId;
    private final Runnable showArtifactDetailsCallback;

    private final Button artifactDetailsIcon;

    /**
     * Constructor for ArtifactDetailsHeaderSupport
     *
     * @param i18n
     *          VaadinMessageSource
     * @param artifactDetailsIconId
     *          Id of artifacts details ccon
     * @param showArtifactDetailsCallback
     *          Runnable
     */
    public ArtifactDetailsHeaderSupport(final VaadinMessageSource i18n, final String artifactDetailsIconId,
            final Runnable showArtifactDetailsCallback) {
        this.i18n = i18n;
        this.artifactDetailsIconId = artifactDetailsIconId;
        this.showArtifactDetailsCallback = showArtifactDetailsCallback;

        this.artifactDetailsIcon = createArtifactDetailsIcon();
    }

    private Button createArtifactDetailsIcon() {
        final Button artifactDetailsIconButton = SPUIComponentProvider.getButton(artifactDetailsIconId, "",
                i18n.getMessage(UIMessageIdProvider.TOOLTIP_ARTIFACT_ICON), null, false, VaadinIcons.FILE_O,
                SPUIButtonStyleNoBorder.class);

        artifactDetailsIconButton.addClickListener(event -> showArtifactDetailsCallback.run());
        artifactDetailsIconButton.setEnabled(false);

        return artifactDetailsIconButton;
    }

    @Override
    public Component getHeaderComponent() {
        return artifactDetailsIcon;
    }

    /**
     * Enable artifact detail icon
     */
    public void enableArtifactDetailsIcon() {
        artifactDetailsIcon.setEnabled(true);
    }

    /**
     * Disable artifact detail icon
     */
    public void disableArtifactDetailsIcon() {
        artifactDetailsIcon.setEnabled(false);
    }
}
