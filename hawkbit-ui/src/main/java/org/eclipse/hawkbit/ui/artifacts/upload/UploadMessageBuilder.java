package org.eclipse.hawkbit.ui.artifacts.upload;

import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

public class UploadMessageBuilder {

    private final UploadLogic uploadLogic;
    private final VaadinMessageSource i18n;

    public UploadMessageBuilder(final UploadLogic uploadLogic, final VaadinMessageSource i18n) {
        this.uploadLogic = uploadLogic;
        this.i18n = i18n;
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

    // Optional<String> buildCompositeMessage() {
    // final String duplicateMessage = getDuplicateFileValidationMessage();
    // final StringBuilder compositeMessage = new StringBuilder();
    // if (!StringUtils.isEmpty(duplicateMessage)) {
    // compositeMessage.append(duplicateMessage);
    // }
    // if (uploadLogic.hasDirectory()) {
    // if (compositeMessage.length() > 0) {
    // compositeMessage.append("<br>");
    // }
    // compositeMessage.append(i18n.getMessage("message.no.directory.upload"));
    // }
    //
    // final String message = compositeMessage.toString();
    // if (StringUtils.isEmpty(message)) {
    // return Optional.empty();
    // }
    // return Optional.of(message);
    // }
    //
    // String getDuplicateFileValidationMessage() {
    // final StringBuilder message = new StringBuilder();
    // if (uploadLogic.containsDuplicateFiles()) {
    // final List<String> duplicateFileNamesList =
    // uploadLogic.getDuplicateFileNamesList();
    // final String fileNames =
    // StringUtils.collectionToCommaDelimitedString(duplicateFileNamesList);
    // if (duplicateFileNamesList.size() == 1) {
    // message.append(i18n.getMessage("message.no.duplicateFile") + fileNames);
    //
    // } else if (duplicateFileNamesList.size() > 1) {
    // message.append(i18n.getMessage("message.no.duplicateFiles"));
    // }
    // }
    // return message.toString();
    // }
}
