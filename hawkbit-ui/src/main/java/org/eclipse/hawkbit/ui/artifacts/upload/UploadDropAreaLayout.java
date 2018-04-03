package org.eclipse.hawkbit.ui.artifacts.upload;

import javax.servlet.MultipartConfigElement;

import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.ui.artifacts.state.ArtifactUploadState;
import org.eclipse.hawkbit.ui.dd.criteria.ServerItemIdClientCriterion;
import org.eclipse.hawkbit.ui.dd.criteria.ServerItemIdClientCriterion.Mode;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.event.dd.acceptcriteria.Not;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.DragAndDropWrapper;
import com.vaadin.ui.DragAndDropWrapper.WrapperTransferable;
import com.vaadin.ui.Html5File;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

public class UploadDropAreaLayout {

    private static AcceptCriterion acceptAllExceptBlacklisted = new Not(new ServerItemIdClientCriterion(Mode.PREFIX,
            UIComponentIdProvider.UPLOAD_SOFTWARE_MODULE_TABLE, UIComponentIdProvider.UPLOAD_TYPE_BUTTON_PREFIX));

    private DragAndDropWrapper dropAreaWrapper;

    private final VaadinMessageSource i18n;

    private final UINotification uiNotification;

    private final ArtifactUploadState artifactUploadState;

    private final MultipartConfigElement multipartConfigElement;

    private final SoftwareModuleManagement softwareManagement;

    private final UploadLogic uploadLogic;

    private final UploadMessageBuilder uploadMessageBuilder;

    public UploadDropAreaLayout(final VaadinMessageSource i18n, final UINotification uiNotification,
            final ArtifactUploadState artifactUploadState, final MultipartConfigElement multipartConfigElement,
            final SoftwareModuleManagement softwareManagement, final UploadLogic uploadLogic) {
        this.i18n = i18n;
        this.uiNotification = uiNotification;
        this.artifactUploadState = artifactUploadState;
        this.multipartConfigElement = multipartConfigElement;
        this.softwareManagement = softwareManagement;
        this.uploadLogic = uploadLogic;
        this.uploadMessageBuilder = new UploadMessageBuilder(uploadLogic, i18n);

        buildLayout();
    }

    private void buildLayout() {

        /* create drag-drop wrapper for drop area */
        dropAreaWrapper = new DragAndDropWrapper(createDropAreaLayout());
        dropAreaWrapper.setDropHandler(new DropAreahandler());
    }

    private static VerticalLayout createDropAreaLayout() {
        final VerticalLayout dropAreaLayout = new VerticalLayout();
        final Label dropHereLabel = new Label("Drop files to upload");
        dropHereLabel.setWidth(null);

        final Label dropIcon = new Label(FontAwesome.ARROW_DOWN.getHtml(), ContentMode.HTML);
        dropIcon.addStyleName("drop-icon");
        dropIcon.setWidth(null);

        dropAreaLayout.addComponent(dropIcon);
        dropAreaLayout.setComponentAlignment(dropIcon, Alignment.BOTTOM_CENTER);
        dropAreaLayout.addComponent(dropHereLabel);
        dropAreaLayout.setComponentAlignment(dropHereLabel, Alignment.TOP_CENTER);
        dropAreaLayout.setSizeFull();
        dropAreaLayout.setStyleName("upload-drop-area-layout-info");
        dropAreaLayout.setSpacing(false);
        return dropAreaLayout;
    }

    public DragAndDropWrapper getDropAreaWrapper() {
        return dropAreaWrapper;
    }

    private class DropAreahandler implements DropHandler {

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

                artifactUploadState.getSelectedBaseSwModuleId().ifPresent(selectedSwId -> {
                    final SoftwareModule softwareModule = softwareManagement.get(selectedSwId).orElse(null);

                    boolean isDirectory = false;
                    boolean isDuplicate = false;

                    for (final Html5File file : files) {

                        isDirectory = uploadLogic.isDirectory(file);
                        isDuplicate = uploadLogic.isFileInUploadState(file.getFileName(), softwareModule);

                        if (!isDirectory || !isDuplicate) {
                            artifactUploadState.incrementNumberOfFileUploadsExpected();
                            file.setStreamVariable(new FileTransferHandlerStreamVariable(file.getFileName(), file.getFileSize(), uploadLogic,
                                            multipartConfigElement.getMaxFileSize(), null, file.getType(),
                                            softwareModule, softwareManagement));
                        }
                    }
                    if (isDirectory && isDuplicate) {
                        uiNotification.displayValidationError(
                                uploadMessageBuilder.buildMessageForDuplicateFileErrorAndDirectoryUploadNotAllowed());
                    } else if (isDirectory) {
                        uiNotification.displayValidationError(
                                uploadMessageBuilder.buildMessageForDirectoryUploadNotAllowed());
                    } else if (isDuplicate) {
                        uiNotification.displayValidationError(uploadMessageBuilder.buildMessageForDuplicateFileError());
                    }
                });
            }
        }

        private boolean validate(final DragAndDropEvent event) {
            // check if drop is valid.If valid ,check if software module is
            // selected.
            if (!isFilesDropped(event)) {
                uiNotification.displayValidationError(i18n.getMessage("message.action.not.allowed"));
                return false;
            }
            return checkIfSoftwareModuleIsSelected();
        }

        private boolean isFilesDropped(final DragAndDropEvent event) {
            if (event.getTransferable() instanceof WrapperTransferable) {
                final Html5File[] files = ((WrapperTransferable) event.getTransferable()).getFiles();
                return files != null;
            }
            return false;
        }
    }

    boolean checkIfSoftwareModuleIsSelected() {
        if (!artifactUploadState.getSelectedBaseSwModuleId().isPresent()) {
            uiNotification.displayValidationError(i18n.getMessage("message.error.noSwModuleSelected"));
            return false;
        }
        return true;
    }

}
