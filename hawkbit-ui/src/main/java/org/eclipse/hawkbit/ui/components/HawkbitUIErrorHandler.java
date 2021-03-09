/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.components;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.ui.UiErrorDetails;
import org.eclipse.hawkbit.ui.UiErrorDetailsExtractor;
import org.eclipse.hawkbit.ui.common.notification.ParallelNotification;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.ClientConnector.ConnectorErrorEvent;
import com.vaadin.server.DefaultErrorHandler;
import com.vaadin.server.ErrorEvent;
import com.vaadin.server.ErrorHandler;
import com.vaadin.server.Page;
import com.vaadin.shared.Connector;
import com.vaadin.ui.Component;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;

/**
 * Default handler for Hawkbit UI.
 */
public class HawkbitUIErrorHandler implements ErrorHandler {
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(HawkbitUIErrorHandler.class);

    private final VaadinMessageSource i18n;
    private final transient List<UiErrorDetailsExtractor> uiErrorDetailsExtractor;

    public HawkbitUIErrorHandler(final VaadinMessageSource i18n,
            final List<UiErrorDetailsExtractor> uiErrorDetailsExtractor) {
        this.i18n = i18n;
        this.uiErrorDetailsExtractor = uiErrorDetailsExtractor;
    }

    @Override
    public void error(final ErrorEvent event) {
        final Page currentPage = getPageFrom(event);
        final List<UiErrorDetails> errorDetails = extractErrorDetails(event);

        if (errorDetails.isEmpty()) {
            showGenericErrorNotification(currentPage, event);
            return;
        }

        errorDetails.stream().filter(UiErrorDetails::isPresent)
                .forEach(details -> showSpecificErrorNotification(currentPage, details));
    }

    protected Page getPageFrom(final ErrorEvent event) {
        if (event instanceof ConnectorErrorEvent) {
            final Connector connector = ((ConnectorErrorEvent) event).getConnector();
            if (connector instanceof UI) {
                final UI uiInstance = (UI) connector;
                return uiInstance.getPage();
            }
        }

        final Optional<Page> originError = getPageOriginError(event);
        if (originError.isPresent()) {
            return originError.get();
        }

        return Page.getCurrent();
    }

    private static Optional<Page> getPageOriginError(final ErrorEvent event) {
        final Component errorOrigin = DefaultErrorHandler.findAbstractComponent(event);

        if (errorOrigin != null && errorOrigin.getUI() != null) {
            return Optional.ofNullable(errorOrigin.getUI().getPage());
        }

        return Optional.empty();
    }

    private List<UiErrorDetails> extractErrorDetails(final ErrorEvent event) {
        return uiErrorDetailsExtractor.stream()
                .map(extractor -> extractor.extractErrorDetailsFrom(event.getThrowable())).flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private void showGenericErrorNotification(final Page page, final ErrorEvent event) {
        LOG.warn("Unexpected Ui error occured", event.getThrowable());

        final Notification notification = buildErrorNotification(i18n.getMessage("caption.error"),
                i18n.getMessage("message.error"));
        showErrorNotification(page, notification);
    }

    private void showSpecificErrorNotification(final Page page, final UiErrorDetails details) {
        final Notification notification = buildErrorNotification(details.getCaption(), details.getDescription());
        showErrorNotification(page, notification);
    }

    /**
     * Method to build a error notification based on caption and description.
     * 
     * @param caption
     *            Caption
     * @param description
     *            Description
     * @return a hawkbit error notification message
     */
    protected static ParallelNotification buildErrorNotification(final String caption, final String description) {
        return UINotification.buildNotification(SPUIStyleDefinitions.SP_NOTIFICATION_ERROR_MESSAGE_STYLE, caption,
                description, VaadinIcons.EXCLAMATION_CIRCLE, true);
    }

    protected void showErrorNotification(final Page page, final Notification notification) {
        page.getUI().access(() -> notification.show(page));
    }
}
