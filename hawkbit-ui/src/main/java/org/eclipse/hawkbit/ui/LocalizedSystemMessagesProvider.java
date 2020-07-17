/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui;

import java.util.Locale;

import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.server.CustomizedSystemMessages;
import com.vaadin.server.SystemMessages;
import com.vaadin.server.SystemMessagesInfo;
import com.vaadin.server.SystemMessagesProvider;

/**
 * {@link SystemMessagesProvider} that localizes Vaadin system messages.
 *
 */
public class LocalizedSystemMessagesProvider implements SystemMessagesProvider {
    private static final long serialVersionUID = 1L;

    private final VaadinMessageSource i18n;
    private final UiProperties uiProperties;

    /**
     * Constructor for LocalizedSystemMessagesProvider
     *
     * @param uiProperties
     *            Properties to determine the available Locales
     * @param i18n
     *            Message source used for localization
     */
    public LocalizedSystemMessagesProvider(final UiProperties uiProperties, final VaadinMessageSource i18n) {
        this.i18n = i18n;
        this.uiProperties = uiProperties;
    }

    private SystemMessages getLocalizedSystemMessages(final VaadinMessageSource i18n, final Locale local) {
        final CustomizedSystemMessages messages = new CustomizedSystemMessages();
        final Locale desiredLocale = HawkbitCommonUtil.getLocaleToBeUsed(uiProperties.getLocalization(), local);

        messages.setSessionExpiredCaption(
                i18n.getMessage(desiredLocale, UIMessageIdProvider.VAADIN_SYSTEM_SESSIONEXPIRED_CAPTION));
        messages.setSessionExpiredMessage(
                i18n.getMessage(desiredLocale, UIMessageIdProvider.VAADIN_SYSTEM_SESSIONEXPIRED_MESSAGE));
        messages.setCommunicationErrorCaption(
                i18n.getMessage(desiredLocale, UIMessageIdProvider.VAADIN_SYSTEM_COMMUNICATIONERROR_CAPTION));
        messages.setCommunicationErrorMessage(
                i18n.getMessage(desiredLocale, UIMessageIdProvider.VAADIN_SYSTEM_COMMUNICATIONERROR_MESSAGE));
        messages.setInternalErrorCaption(
                i18n.getMessage(desiredLocale, UIMessageIdProvider.VAADIN_SYSTEM_INTERNALERROR_CAPTION));
        messages.setInternalErrorMessage(
                i18n.getMessage(desiredLocale, UIMessageIdProvider.VAADIN_SYSTEM_INTERNALERROR_MESSAGE));

        return messages;
    }

    @Override
    public SystemMessages getSystemMessages(final SystemMessagesInfo systemMessagesInfo) {
        return getLocalizedSystemMessages(i18n, systemMessagesInfo.getLocale());
    }

}
