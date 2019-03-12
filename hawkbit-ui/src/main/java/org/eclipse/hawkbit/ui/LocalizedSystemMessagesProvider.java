package org.eclipse.hawkbit.ui;

import java.util.Locale;

import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.server.CustomizedSystemMessages;
import com.vaadin.server.SystemMessages;
import com.vaadin.server.SystemMessagesInfo;
import com.vaadin.server.SystemMessagesProvider;

public class LocalizedSystemMessagesProvider implements SystemMessagesProvider {
    private static final long serialVersionUID = 1L;

    private final VaadinMessageSource i18n;

    public LocalizedSystemMessagesProvider(final VaadinMessageSource i18n) {
        this.i18n = i18n;
    }

    private SystemMessages getLocalizedSystemMessages(final VaadinMessageSource i18n, final Locale local) {
        final CustomizedSystemMessages messages = new CustomizedSystemMessages();

        messages.setSessionExpiredCaption(
                i18n.getMessage(local, UIMessageIdProvider.VAADIN_SYSTEM_SESSIONEXPIRED_CAPTION));
        messages.setSessionExpiredMessage(
                i18n.getMessage(local, UIMessageIdProvider.VAADIN_SYSTEM_SESSIONEXPIRED_MESSAGE));
        messages.setCommunicationErrorCaption(
                i18n.getMessage(local, UIMessageIdProvider.VAADIN_SYSTEM_COMMUNICATIONERROR_CAPTION));
        messages.setCommunicationErrorMessage(
                i18n.getMessage(local, UIMessageIdProvider.VAADIN_SYSTEM_COMMUNICATIONERROR_MESSAGE));
        messages.setInternalErrorCaption(
                i18n.getMessage(local, UIMessageIdProvider.VAADIN_SYSTEM_INTERNALERROR_CAPTION));
        messages.setInternalErrorMessage(
                i18n.getMessage(local, UIMessageIdProvider.VAADIN_SYSTEM_INTERNALERROR_MESSAGE));

        return messages;
    }

    @Override
    public SystemMessages getSystemMessages(final SystemMessagesInfo systemMessagesInfo) {
        return getLocalizedSystemMessages(i18n, systemMessagesInfo.getLocale());
    }

}
