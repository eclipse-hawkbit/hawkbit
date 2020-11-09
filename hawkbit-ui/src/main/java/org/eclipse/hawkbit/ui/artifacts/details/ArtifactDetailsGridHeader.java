/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.details;

import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxySoftwareModule;
import org.eclipse.hawkbit.ui.common.event.CommandTopics;
import org.eclipse.hawkbit.ui.common.event.EventLayout;
import org.eclipse.hawkbit.ui.common.event.EventView;
import org.eclipse.hawkbit.ui.common.event.LayoutResizeEventPayload;
import org.eclipse.hawkbit.ui.common.event.LayoutResizeEventPayload.ResizeType;
import org.eclipse.hawkbit.ui.common.grid.header.AbstractMasterAwareGridHeader;
import org.eclipse.hawkbit.ui.common.grid.header.support.ResizeHeaderSupport;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;

/**
 * Header for ArtifactDetails with maximize-support.
 */
public class ArtifactDetailsGridHeader extends AbstractMasterAwareGridHeader<ProxySoftwareModule> {
    private static final long serialVersionUID = 1L;

    private final ArtifactDetailsGridLayoutUiState artifactDetailsGridLayoutUiState;

    private final transient ResizeHeaderSupport resizeHeaderSupport;

    /**
     * Constructor
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param artifactDetailsGridLayoutUiState
     *            ArtifactDetailsGridLayoutUiState
     */
    public ArtifactDetailsGridHeader(final CommonUiDependencies uiDependencies,
            final ArtifactDetailsGridLayoutUiState artifactDetailsGridLayoutUiState) {
        super(uiDependencies.getI18n(), uiDependencies.getPermChecker(), uiDependencies.getEventBus());

        this.artifactDetailsGridLayoutUiState = artifactDetailsGridLayoutUiState;

        this.resizeHeaderSupport = new ResizeHeaderSupport(i18n, SPUIDefinitions.EXPAND_ARTIFACT_DETAILS,
                this::maximizeTable, this::minimizeTable, this::onLoadIsTableMaximized);
        addHeaderSupport(resizeHeaderSupport);

        buildHeader();
    }

    @Override
    protected String getEntityDetailsCaptionMsgKey() {
        return UIMessageIdProvider.CAPTION_ARTIFACT_DETAILS;
    }

    @Override
    protected String getMasterEntityDetailsCaptionId() {
        return UIComponentIdProvider.ARTIFACT_DETAILS_HEADER_LABEL_ID;
    }

    @Override
    protected String getMasterEntityName(final ProxySoftwareModule masterEntity) {
        return masterEntity.getNameAndVersion();
    }

    @Override
    protected String getEntityDetailsCaptionOfMsgKey() {
        return UIMessageIdProvider.CAPTION_ARTIFACT_DETAILS_OF;
    }

    private void maximizeTable() {
        eventBus.publish(CommandTopics.RESIZE_LAYOUT, this,
                new LayoutResizeEventPayload(ResizeType.MAXIMIZE, EventLayout.ARTIFACT_LIST, EventView.UPLOAD));

        artifactDetailsGridLayoutUiState.setMaximized(true);
    }

    private void minimizeTable() {
        eventBus.publish(CommandTopics.RESIZE_LAYOUT, this,
                new LayoutResizeEventPayload(ResizeType.MINIMIZE, EventLayout.ARTIFACT_LIST, EventView.UPLOAD));

        artifactDetailsGridLayoutUiState.setMaximized(false);
    }

    private Boolean onLoadIsTableMaximized() {
        return artifactDetailsGridLayoutUiState.isMaximized();
    }
}
