package org.eclipse.hawkbit.ui.artifacts.upload;

import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

public class UploadMessageBuilder {

    private final UploadLogic uploadLogic;
    private final VaadinMessageSource i18n;

    public UploadMessageBuilder(final UploadLogic uploadLogic, final VaadinMessageSource i18n) {
        this.uploadLogic = uploadLogic;
        this.i18n = i18n;
    }

    String buildMessageForFileSizeExceeded(final long maxSize) {
        return i18n.getMessage("message.uploadedfile.size.exceeded", maxSize);
    }

    String buildMessageForUploadAbortedByUser() {
        return i18n.getMessage("message.uploadedfile.aborted");
    }

    String buidlMessageForGenericUploadFailed() {
        return i18n.getMessage("message.upload.failed");
    }

    String buildMessageForDuplicateFileError() {
        return i18n.getMessage("message.no.duplicateFiles");
    }

    String buildMessageForDirectoryUploadNotAllowed() {
        return i18n.getMessage("message.no.directory.upload");
    }

    String buildMessageForDuplicateFileErrorAndDirectoryUploadNotAllowed(){
        return buildMessageForDuplicateFileError() + "<br>" + buildMessageForDirectoryUploadNotAllowed();
    }
}
