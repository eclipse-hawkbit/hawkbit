/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts;

import javax.annotation.PreDestroy;

import org.eclipse.hawkbit.repository.SpPermissionChecker;
import org.eclipse.hawkbit.ui.HawkbitUI;
import org.eclipse.hawkbit.ui.artifacts.details.ArtifactDetailsLayout;
import org.eclipse.hawkbit.ui.artifacts.event.ArtifactDetailsEvent;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent;
import org.eclipse.hawkbit.ui.artifacts.footer.SMDeleteActionsLayout;
import org.eclipse.hawkbit.ui.artifacts.smtable.SoftwareModuleTableLayout;
import org.eclipse.hawkbit.ui.artifacts.smtype.SMTypeFilterLayout;
import org.eclipse.hawkbit.ui.artifacts.state.ArtifactUploadState;
import org.eclipse.hawkbit.ui.artifacts.upload.UploadLayout;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.management.event.DragEvent;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.event.MouseEvents.ClickListener;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.Page;
import com.vaadin.server.Page.BrowserWindowResizeEvent;
import com.vaadin.server.Page.BrowserWindowResizeListener;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.DragAndDropWrapper;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

/**
 * Display artifacts upload view.
 *
 *
 */
@SpringView(name = UploadArtifactView.VIEW_NAME, ui = HawkbitUI.class)
@ViewScope
public class UploadArtifactView extends VerticalLayout implements View, BrowserWindowResizeListener {

    public static final String VIEW_NAME = "spUpload";
    private static final long serialVersionUID = 8754632011301553682L;

    @Autowired
    private transient EventBus.SessionEventBus eventBus;

    @Autowired
    private SpPermissionChecker permChecker;

    @Autowired
    private I18N i18n;

    @Autowired
    private transient UINotification uiNotification;

    @Autowired
    private ArtifactUploadState artifactUploadState;

    @Autowired
    private SMTypeFilterLayout filterByTypeLayout;

    @Autowired
    private SoftwareModuleTableLayout smTableLayout;

    @Autowired
    private ArtifactDetailsLayout artifactDetailsLayout;

    @Autowired
    private UploadLayout uploadLayout;

    @Autowired
    private SMDeleteActionsLayout deleteActionsLayout;

    private VerticalLayout detailAndUploadLayout;

    private HorizontalLayout uplaodButtonsLayout;

    private GridLayout mainLayout;
    private DragAndDropWrapper dadw;

    @Override
    public void enter(final ViewChangeEvent event) {
        buildLayout();
        restoreState();
        checkNoDataAvaialble();
        eventBus.subscribe(this);
        Page.getCurrent().addBrowserWindowResizeListener(this);
        showOrHideFilterButtons(Page.getCurrent().getBrowserWindowWidth());
    }

    @PreDestroy
    void destroy() {
        eventBus.unsubscribe(this);
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final SoftwareModuleEvent event) {
        if (BaseEntityEventType.MINIMIZED == event.getEventType()) {
            minimizeSwTable();
        } else if (BaseEntityEventType.MAXIMIZED == event.getEventType()) {
            maximizeSwTable();
        }
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final ArtifactDetailsEvent event) {
        if (event == ArtifactDetailsEvent.MINIMIZED) {
            minimizeArtifactoryDetails();
        } else if (event == ArtifactDetailsEvent.MAXIMIZED) {
            maximizeArtifactoryDetails();
        }
    }

    private void restoreState() {
        if (artifactUploadState.isSwModuleTableMaximized()) {
            maximizeSwTable();
        }
        if (artifactUploadState.isArtifactDetailsMaximized()) {
            maximizeArtifactoryDetails();
        }
    }

    private void buildLayout() {
        if (permChecker.hasReadDistributionPermission() || permChecker.hasCreateDistributionPermission()) {
            setSizeFull();
            createMainLayout();
            addComponents(mainLayout);
            setExpandRatio(mainLayout, 1);
            hideDropHints();
        }
    }

    private VerticalLayout createDetailsAndUploadLayout() {
        detailAndUploadLayout = new VerticalLayout();

        detailAndUploadLayout.addComponent(artifactDetailsLayout);
        detailAndUploadLayout.setComponentAlignment(artifactDetailsLayout, Alignment.MIDDLE_CENTER);

        if (permChecker.hasCreateDistributionPermission()) {
            dadw = uploadLayout.getDropAreaWrapper();
            detailAndUploadLayout.addComponent(dadw);
            detailAndUploadLayout.setComponentAlignment(dadw, Alignment.MIDDLE_CENTER);
        }

        detailAndUploadLayout.setExpandRatio(artifactDetailsLayout, 1.0F);
        detailAndUploadLayout.setSizeFull();
        detailAndUploadLayout.addStyleName("group");
        detailAndUploadLayout.setSpacing(true);
        return detailAndUploadLayout;

    }

    private GridLayout createMainLayout() {
        createDetailsAndUploadLayout();
        createUploadButtonLayout();
        mainLayout = new GridLayout(3, 2);
        mainLayout.setSizeFull();
        mainLayout.setSpacing(true);
        mainLayout.addComponent(filterByTypeLayout, 0, 0);
        mainLayout.addComponent(smTableLayout, 1, 0);
        mainLayout.addComponent(detailAndUploadLayout, 2, 0);
        mainLayout.addComponent(deleteActionsLayout, 1, 1);
        mainLayout.addComponent(uplaodButtonsLayout, 2, 1);
        mainLayout.setRowExpandRatio(0, 1.0F);
        mainLayout.setColumnExpandRatio(1, 0.5F);
        mainLayout.setColumnExpandRatio(2, 0.5F);
        mainLayout.setComponentAlignment(deleteActionsLayout, Alignment.BOTTOM_CENTER);
        mainLayout.setComponentAlignment(uplaodButtonsLayout, Alignment.BOTTOM_CENTER);
        return mainLayout;
    }

    private void createUploadButtonLayout() {
        uplaodButtonsLayout = new HorizontalLayout();
        if (permChecker.hasCreateDistributionPermission()) {
            uplaodButtonsLayout = uploadLayout.getFileUploadLayout();
        }

    }

    private void minimizeSwTable() {
        mainLayout.addComponent(detailAndUploadLayout, 2, 0);
        addOtherComponents();
    }

    private void maximizeSwTable() {
        mainLayout.removeComponent(detailAndUploadLayout);
        removeOtherComponents();
        mainLayout.setColumnExpandRatio(1, 1F);
        mainLayout.setColumnExpandRatio(2, 0F);
    }

    private void minimizeArtifactoryDetails() {
        mainLayout.setSpacing(true);
        detailAndUploadLayout.addComponent(dadw);
        mainLayout.addComponent(filterByTypeLayout, 0, 0);
        mainLayout.addComponent(smTableLayout, 1, 0);
        addOtherComponents();
    }

    private void maximizeArtifactoryDetails() {
        mainLayout.setSpacing(false);
        mainLayout.removeComponent(filterByTypeLayout);
        mainLayout.removeComponent(smTableLayout);
        detailAndUploadLayout.removeComponent(dadw);
        removeOtherComponents();
        mainLayout.setColumnExpandRatio(1, 0F);
        mainLayout.setColumnExpandRatio(2, 1F);
    }

    private void addOtherComponents() {
        mainLayout.addComponent(deleteActionsLayout, 1, 1);
        mainLayout.addComponent(uplaodButtonsLayout, 2, 1);
        mainLayout.setColumnExpandRatio(1, 0.5F);
        mainLayout.setColumnExpandRatio(2, 0.5F);
        mainLayout.setComponentAlignment(deleteActionsLayout, Alignment.BOTTOM_CENTER);
        mainLayout.setComponentAlignment(uplaodButtonsLayout, Alignment.BOTTOM_CENTER);
    }

    private void removeOtherComponents() {
        mainLayout.removeComponent(deleteActionsLayout);
        mainLayout.removeComponent(uplaodButtonsLayout);

    }

    private void hideDropHints() {
        UI.getCurrent().addClickListener(new ClickListener() {
            private static final long serialVersionUID = 1L;

            @Override
            public void click(final com.vaadin.event.MouseEvents.ClickEvent event) {
                eventBus.publish(this, DragEvent.HIDE_DROP_HINT);
            }
        });
    }

    private void checkNoDataAvaialble() {
        if (artifactUploadState.isNoDataAvilableSoftwareModule()) {

            uiNotification.displayValidationError(i18n.get("message.no.data"));
        }
    }

    @Override
    public void browserWindowResized(final BrowserWindowResizeEvent event) {
        showOrHideFilterButtons(event.getWidth());
    }

    private void showOrHideFilterButtons(final int browserWidth) {
        if (browserWidth < SPUIDefinitions.REQ_MIN_BROWSER_WIDTH) {

            filterByTypeLayout.setVisible(false);
            smTableLayout.setShowFilterButtonVisible(true);
        } else if (!artifactUploadState.isSwTypeFilterClosed()) {
            filterByTypeLayout.setVisible(true);
            smTableLayout.setShowFilterButtonVisible(false);
        }
    }

}
