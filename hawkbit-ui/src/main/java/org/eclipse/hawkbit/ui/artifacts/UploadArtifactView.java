/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts;

import java.util.EnumMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.MultipartConfigElement;

import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.ui.AbstractHawkbitUI;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.artifacts.details.ArtifactDetailsGridLayout;
import org.eclipse.hawkbit.ui.artifacts.smtable.SoftwareModuleGridLayout;
import org.eclipse.hawkbit.ui.artifacts.smtype.filter.SMTypeFilterLayout;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.event.EventLayout;
import org.eclipse.hawkbit.ui.common.event.EventView;
import org.eclipse.hawkbit.ui.common.event.EventViewAware;
import org.eclipse.hawkbit.ui.common.layout.listener.LayoutResizeListener;
import org.eclipse.hawkbit.ui.common.layout.listener.LayoutResizeListener.ResizeHandler;
import org.eclipse.hawkbit.ui.common.layout.listener.LayoutVisibilityListener;
import org.eclipse.hawkbit.ui.common.layout.listener.LayoutVisibilityListener.VisibilityHandler;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.navigator.View;
import com.vaadin.server.Page;
import com.vaadin.server.Page.BrowserWindowResizeEvent;
import com.vaadin.server.Page.BrowserWindowResizeListener;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;

/**
 * Display artifacts upload view.
 */
@UIScope
@SpringView(name = UploadArtifactView.VIEW_NAME, ui = AbstractHawkbitUI.class)
public class UploadArtifactView extends VerticalLayout implements View, BrowserWindowResizeListener {
    private static final long serialVersionUID = 1L;

    public static final String VIEW_NAME = "spUpload";

    private final SpPermissionChecker permChecker;
    private final ArtifactUploadState artifactUploadState;

    private final SMTypeFilterLayout smTypeFilterLayout;
    private final SoftwareModuleGridLayout smGridLayout;
    private final ArtifactDetailsGridLayout artifactDetailsGridLayout;

    private HorizontalLayout mainLayout;

    private final transient LayoutVisibilityListener layoutVisibilityListener;
    private final transient LayoutResizeListener layoutResizeListener;

    @Autowired
    UploadArtifactView(final UIEventBus eventBus, final SpPermissionChecker permChecker, final VaadinMessageSource i18n,
            final UINotification uiNotification, final ArtifactUploadState artifactUploadState,
            final EntityFactory entityFactory, final SoftwareModuleManagement softwareModuleManagement,
            final SoftwareModuleTypeManagement softwareModuleTypeManagement,
            final MultipartConfigElement multipartConfigElement, final ArtifactManagement artifactManagement) {
        this.permChecker = permChecker;
        this.artifactUploadState = artifactUploadState;

        final CommonUiDependencies uiDependencies = new CommonUiDependencies(i18n, entityFactory, eventBus, uiNotification,
                permChecker);

        if (permChecker.hasReadRepositoryPermission()) {
            this.smTypeFilterLayout = new SMTypeFilterLayout(uiDependencies, softwareModuleTypeManagement,
                    artifactUploadState.getSmTypeFilterLayoutUiState(), EventView.UPLOAD);
            this.smGridLayout = new SoftwareModuleGridLayout(uiDependencies, softwareModuleManagement,
                    softwareModuleTypeManagement, artifactUploadState.getSmTypeFilterLayoutUiState(),
                    artifactUploadState.getSmGridLayoutUiState());
            this.artifactDetailsGridLayout = new ArtifactDetailsGridLayout(uiDependencies, artifactUploadState,
                    artifactUploadState.getArtifactDetailsGridLayoutUiState(), artifactManagement,
                    softwareModuleManagement, multipartConfigElement);

            final Map<EventLayout, VisibilityHandler> layoutVisibilityHandlers = new EnumMap<>(EventLayout.class);
            layoutVisibilityHandlers.put(EventLayout.SM_TYPE_FILTER,
                    new VisibilityHandler(this::showSmTypeLayout, this::hideSmTypeLayout));
            this.layoutVisibilityListener = new LayoutVisibilityListener(eventBus, new EventViewAware(EventView.UPLOAD),
                    layoutVisibilityHandlers);

            final Map<EventLayout, ResizeHandler> layoutResizeHandlers = new EnumMap<>(EventLayout.class);
            layoutResizeHandlers.put(EventLayout.SM_LIST,
                    new ResizeHandler(this::maximizeSmGridLayout, this::minimizeSmGridLayout));
            layoutResizeHandlers.put(EventLayout.ARTIFACT_LIST,
                    new ResizeHandler(this::maximizeArtifactGridLayout, this::minimizeArtifactGridLayout));
            this.layoutResizeListener = new LayoutResizeListener(eventBus, new EventViewAware(EventView.UPLOAD),
                    layoutResizeHandlers);
        } else {
            this.smTypeFilterLayout = null;
            this.smGridLayout = null;
            this.artifactDetailsGridLayout = null;
            this.layoutVisibilityListener = null;
            this.layoutResizeListener = null;
        }
    }

    @PostConstruct
    void init() {
        if (permChecker.hasReadRepositoryPermission()) {
            buildLayout();
            restoreState();
            Page.getCurrent().addBrowserWindowResizeListener(this);
        }
    }

    private void buildLayout() {
        setMargin(false);
        setSpacing(false);
        setSizeFull();

        createMainLayout();

        addComponent(mainLayout);
        setExpandRatio(mainLayout, 1.0F);
    }

    private void createMainLayout() {
        mainLayout = new HorizontalLayout();
        mainLayout.setSizeFull();
        mainLayout.setMargin(false);
        mainLayout.setSpacing(true);

        mainLayout.addComponent(smTypeFilterLayout);
        mainLayout.addComponent(smGridLayout);
        mainLayout.addComponent(artifactDetailsGridLayout);

        mainLayout.setExpandRatio(smTypeFilterLayout, 0F);
        mainLayout.setExpandRatio(smGridLayout, 0.5F);
        mainLayout.setExpandRatio(artifactDetailsGridLayout, 0.5F);
    }

    private void restoreState() {
        if (artifactUploadState.getSmTypeFilterLayoutUiState().isHidden()
                || artifactUploadState.getArtifactDetailsGridLayoutUiState().isMaximized()) {
            hideSmTypeLayout();
        } else {
            showSmTypeLayout();
        }
        smTypeFilterLayout.restoreState();

        if (artifactUploadState.getSmGridLayoutUiState().isMaximized()) {
            maximizeSmGridLayout();
        }
        smGridLayout.restoreState();

        if (artifactUploadState.getArtifactDetailsGridLayoutUiState().isMaximized()) {
            maximizeArtifactGridLayout();
        }
        artifactDetailsGridLayout.restoreState();
    }

    private void showSmTypeLayout() {
        smTypeFilterLayout.setVisible(true);
        smGridLayout.hideSmTypeHeaderIcon();
    }

    private void hideSmTypeLayout() {
        smTypeFilterLayout.setVisible(false);
        smGridLayout.showSmTypeHeaderIcon();
    }

    private void maximizeSmGridLayout() {
        artifactDetailsGridLayout.setVisible(false);

        mainLayout.setExpandRatio(smTypeFilterLayout, 0F);
        mainLayout.setExpandRatio(smGridLayout, 1.0F);
        mainLayout.setExpandRatio(artifactDetailsGridLayout, 0F);

        smGridLayout.maximize();
    }

    private void minimizeSmGridLayout() {
        artifactDetailsGridLayout.setVisible(true);

        mainLayout.setExpandRatio(smTypeFilterLayout, 0F);
        mainLayout.setExpandRatio(smGridLayout, 0.5F);
        mainLayout.setExpandRatio(artifactDetailsGridLayout, 0.5F);

        smGridLayout.minimize();
    }

    private void maximizeArtifactGridLayout() {
        smTypeFilterLayout.setVisible(false);
        smGridLayout.setVisible(false);

        mainLayout.setExpandRatio(smTypeFilterLayout, 0F);
        mainLayout.setExpandRatio(smGridLayout, 0F);
        mainLayout.setExpandRatio(artifactDetailsGridLayout, 1.0F);

        artifactDetailsGridLayout.maximize();
    }

    private void minimizeArtifactGridLayout() {
        if (!artifactUploadState.getSmTypeFilterLayoutUiState().isHidden()) {
            smTypeFilterLayout.setVisible(true);
        }
        smGridLayout.setVisible(true);

        mainLayout.setExpandRatio(smTypeFilterLayout, 0F);
        mainLayout.setExpandRatio(smGridLayout, 0.5F);
        mainLayout.setExpandRatio(artifactDetailsGridLayout, 0.5F);

        artifactDetailsGridLayout.minimize();
    }

    /**
     * Show or hide the filter button based on the event width
     *
     * @param event
     *            BrowserWindowResizeEvent
     */
    @Override
    public void browserWindowResized(final BrowserWindowResizeEvent event) {
        showOrHideFilterButtons(event.getWidth());
    }

    private void showOrHideFilterButtons(final int browserWidth) {
        if (artifactUploadState.getArtifactDetailsGridLayoutUiState().isMaximized()) {
            return;
        }

        if (browserWidth < SPUIDefinitions.REQ_MIN_BROWSER_WIDTH) {
            if (!artifactUploadState.getSmTypeFilterLayoutUiState().isHidden()) {
                hideSmTypeLayout();
            }
        } else {
            if (artifactUploadState.getSmTypeFilterLayoutUiState().isHidden()) {
                showSmTypeLayout();
            }
        }
    }

    @PreDestroy
    void destroy() {
        if (permChecker.hasReadRepositoryPermission()) {
            layoutVisibilityListener.unsubscribe();
            layoutResizeListener.unsubscribe();

            smTypeFilterLayout.unsubscribeListener();
            smGridLayout.unsubscribeListener();
            artifactDetailsGridLayout.unsubscribeListener();
        }
    }
}
