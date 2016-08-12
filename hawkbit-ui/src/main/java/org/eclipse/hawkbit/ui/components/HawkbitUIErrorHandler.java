/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.components;

import static com.vaadin.ui.themes.ValoTheme.NOTIFICATION_CLOSABLE;
import static com.vaadin.ui.themes.ValoTheme.NOTIFICATION_FAILURE;
import static com.vaadin.ui.themes.ValoTheme.NOTIFICATION_SMALL;

import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SpringContextHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.vaadin.server.ClientConnector.ConnectorErrorEvent;
import com.vaadin.server.DefaultErrorHandler;
import com.vaadin.server.ErrorEvent;
import com.vaadin.server.Page;
import com.vaadin.shared.Connector;
import com.vaadin.ui.Component;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;

/**
 * Default handler for Hawkbit UI.
 */
public class HawkbitUIErrorHandler extends DefaultErrorHandler {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(HawkbitUIErrorHandler.class);

    private static final String STYLE = NOTIFICATION_FAILURE + " " + NOTIFICATION_SMALL + " " + NOTIFICATION_CLOSABLE;

    @Override
    public void error(final ErrorEvent event) {

        final HawkbitErrorNotificationMessage message = buildNotification(getRootExceptionFrom(event));
        if (event instanceof ConnectorErrorEvent) {
            final Connector connector = ((ConnectorErrorEvent) event).getConnector();
            if (connector instanceof UI) {
                final UI uiInstance = (UI) connector;
                uiInstance.access(() -> message.show(uiInstance.getPage()));
                return;
            }
        }

        final Optional<Page> originError = getPageOriginError(event);
        if (originError.isPresent()) {
            message.show(originError.get());
            return;
        }

        HawkbitErrorNotificationMessage.show(message.getCaption(), message.getDescription(), Type.HUMANIZED_MESSAGE);
    }

    private static Throwable getRootExceptionFrom(final ErrorEvent event) {
        return getRootCauseOf(event.getThrowable());
    }

    private static Throwable getRootCauseOf(final Throwable ex) {

        if (ex.getCause() != null) {
            return getRootCauseOf(ex.getCause());
        }

        return ex;
    }

    private static Optional<Page> getPageOriginError(final ErrorEvent event) {

        final Component errorOrigin = findAbstractComponent(event);

        if (errorOrigin != null && errorOrigin.getUI() != null) {
            return Optional.fromNullable(errorOrigin.getUI().getPage());
        }

        return Optional.absent();
    }

    /**
     * Method to build a notification based on an exception.
     * 
     * @param ex
     *            the throwable
     * @return a hawkbit error notification message
     */
    protected HawkbitErrorNotificationMessage buildNotification(final Throwable ex) {
        LOG.error("Error in UI: ", ex);
        final I18N i18n = SpringContextHelper.getBean(I18N.class);
        return new HawkbitErrorNotificationMessage(STYLE, i18n.get("caption.error"),
                i18n.get("message.error.temp", ex.getClass().getSimpleName()), false);
    }

}
