/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.upload;

import javax.servlet.MultipartConfigElement;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent;
import org.eclipse.hawkbit.ui.artifacts.state.ArtifactUploadState;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.dd.criteria.ServerItemIdClientCriterion;
import org.eclipse.hawkbit.ui.dd.criteria.ServerItemIdClientCriterion.Mode;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.event.dd.acceptcriteria.Not;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.DragAndDropWrapper;
import com.vaadin.ui.DragAndDropWrapper.WrapperTransferable;
import com.vaadin.ui.Html5File;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

/**
 * Container for drag and drop area in the upload view.
 */
public class UploadDropAreaLayout extends AbstractComponent {

    private static final long serialVersionUID = 1L;

    private static AcceptCriterion acceptAllExceptBlacklisted = new Not(new ServerItemIdClientCriterion(Mode.PREFIX,
            UIComponentIdProvider.UPLOAD_SOFTWARE_MODULE_TABLE, UIComponentIdProvider.UPLOAD_TYPE_BUTTON_PREFIX));

    private DragAndDropWrapper dropAreaWrapper;

    private final VaadinMessageSource i18n;

    private final UINotification uiNotification;

    private final ArtifactUploadState artifactUploadState;

    private final transient MultipartConfigElement multipartConfigElement;

    private final transient SoftwareModuleManagement softwareManagement;

    private final transient ArtifactManagement artifactManagement;

    private final UploadProgressButtonLayout uploadButtonLayout;

    /**
     * Creates a new {@link UploadDropAreaLayout} instance.
     * 
     * @param i18n
     *            the {@link VaadinMessageSource}
     * @param eventBus
     *            the {@link EventBus} used to send/retrieve events
     * @param uiNotification
     *            {@link UINotification} for showing notifications
     * @param artifactUploadState
     *            the {@link ArtifactUploadState} for state information
     * @param multipartConfigElement
     *            the {@link MultipartConfigElement}
     * @param softwareManagement
     *            the {@link SoftwareModuleManagement} for retrieving the
     *            {@link SoftwareModule}
     * @param artifactManagement
     *            the {@link ArtifactManagement} for storing the uploaded
     *            artifacts
     */
    public UploadDropAreaLayout(final VaadinMessageSource i18n, final UIEventBus eventBus,
            final UINotification uiNotification, final ArtifactUploadState artifactUploadState,
            final MultipartConfigElement multipartConfigElement, final SoftwareModuleManagement softwareManagement,
            final ArtifactManagement artifactManagement) {
        this.i18n = i18n;
        this.uiNotification = uiNotification;
        this.artifactUploadState = artifactUploadState;
        this.multipartConfigElement = multipartConfigElement;
        this.softwareManagement = softwareManagement;
        this.artifactManagement = artifactManagement;
        this.uploadButtonLayout = new UploadProgressButtonLayout(i18n, eventBus, artifactUploadState,
                multipartConfigElement, artifactManagement, softwareManagement);

        buildLayout();

        eventBus.subscribe(this);
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final SoftwareModuleEvent event) {
        final BaseEntityEventType eventType = event.getEventType();
        if (eventType == BaseEntityEventType.SELECTED_ENTITY) {
            UI.getCurrent().access(() -> {
                if (artifactUploadState.isNoSoftwareModuleSelected()
                        || artifactUploadState.isMoreThanOneSoftwareModulesSelected()) {
                    dropAreaWrapper.setEnabled(false);
                } else if (artifactUploadState.areAllUploadsFinished()) {
                    dropAreaWrapper.setEnabled(true);
                }
            });
        }
    }

    private void buildLayout() {
        dropAreaWrapper = new DragAndDropWrapper(createDropAreaLayout());
        dropAreaWrapper.setDropHandler(new DropAreaHandler());
    }

    private VerticalLayout createDropAreaLayout() {
        final VerticalLayout dropAreaLayout = new VerticalLayout();
        final Label dropHereLabel = new Label(i18n.getMessage(UIMessageIdProvider.LABEL_DROP_AREA_UPLOAD));
        dropHereLabel.setWidth(null);

        final Label dropIcon = new Label(FontAwesome.ARROW_DOWN.getHtml(), ContentMode.HTML);
        dropIcon.addStyleName("drop-icon");
        dropIcon.setWidth(null);

        dropAreaLayout.addComponent(dropIcon);
        dropAreaLayout.setComponentAlignment(dropIcon, Alignment.TOP_CENTER);
        dropAreaLayout.addComponent(dropHereLabel);
        dropAreaLayout.setComponentAlignment(dropHereLabel, Alignment.TOP_CENTER);

        uploadButtonLayout.setWidth(null);
        uploadButtonLayout.addStyleName("upload-button");
        dropAreaLayout.addComponent(uploadButtonLayout);
        dropAreaLayout.setComponentAlignment(uploadButtonLayout, Alignment.BOTTOM_CENTER);

        dropAreaLayout.setSizeFull();
        dropAreaLayout.setStyleName("upload-drop-area-layout-info");
        dropAreaLayout.setSpacing(false);
        return dropAreaLayout;
    }

    public DragAndDropWrapper getDropAreaWrapper() {
        return dropAreaWrapper;
    }

    private class DropAreaHandler implements DropHandler {

        private static final long serialVersionUID = 1L;

        @Override
        public AcceptCriterion getAcceptCriterion() {
            return acceptAllExceptBlacklisted;
        }

        @Override
        public void drop(final DragAndDropEvent event) {
            if (validate(event)) {
                final Html5File[] files = ((WrapperTransferable) event.getTransferable()).getFiles();
                // selected software module at the time of file drop is
                // considered for upload
                artifactUploadState.getSelectedBaseSwModuleId()
                        .ifPresent(selectedSwId -> uploadFilesForSoftwareModule(files, selectedSwId));
            }
        }

        private void uploadFilesForSoftwareModule(final Html5File[] files, final Long softwareModuleId) {
            final SoftwareModule softwareModule = softwareManagement.get(softwareModuleId).orElse(null);

            boolean isDirectory = false;
            boolean isDuplicate = false;

            for (final Html5File file : files) {

                isDirectory = isDirectory(file);
                isDuplicate = artifactUploadState.isFileInUploadState(file.getFileName(), softwareModule);

                if (!isDirectory && !isDuplicate) {
                    file.setStreamVariable(new FileTransferHandlerStreamVariable(file.getFileName(), file.getFileSize(),
                            multipartConfigElement.getMaxFileSize(), file.getType(), softwareModule, artifactManagement,
                            i18n));
                }
            }
            if (isDirectory && isDuplicate) {
                uiNotification.displayValidationError(i18n.getMessage("message.no.duplicateFiles") + "<br>"
                        + i18n.getMessage("message.no.directory.upload"));
            } else if (isDirectory) {
                uiNotification.displayValidationError(i18n.getMessage("message.no.directory.upload"));
            } else if (isDuplicate) {
                uiNotification.displayValidationError(i18n.getMessage("message.no.duplicateFiles"));
            }
        }

        private boolean isDirectory(final Html5File file) {
            return StringUtils.isBlank(file.getType()) && file.getFileSize() % 4096 == 0;
        }

        private boolean validate(final DragAndDropEvent event) {
            // check if drop is valid.If valid ,check if software module is
            // selected.
            if (!isFilesDropped(event)) {
                uiNotification.displayValidationError(i18n.getMessage(UIMessageIdProvider.MESSAGE_ACTION_NOT_ALLOWED));
                return false;
            }
            return validateSoftwareModuleSelection();
        }

        private boolean isFilesDropped(final DragAndDropEvent event) {
            if (event.getTransferable() instanceof WrapperTransferable) {
                final Html5File[] files = ((WrapperTransferable) event.getTransferable()).getFiles();
                return files != null;
            }
            return false;
        }

        private boolean validateSoftwareModuleSelection() {
            if (artifactUploadState.isNoSoftwareModuleSelected()) {
                uiNotification.displayValidationError(i18n.getMessage("message.error.noSwModuleSelected"));
                return false;
            }
            if (artifactUploadState.isMoreThanOneSoftwareModulesSelected()) {
                uiNotification.displayValidationError(i18n.getMessage("message.error.multiSwModuleSelected"));
                return false;
            }
            return true;
        }
    }

    public UploadProgressButtonLayout getUploadButtonLayout() {
        return uploadButtonLayout;
    }

}
