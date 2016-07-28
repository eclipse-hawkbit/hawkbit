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
import com.vaadin.server.DefaultErrorHandler;
import com.vaadin.server.ErrorEvent;
import com.vaadin.server.Page;
import com.vaadin.ui.Component;

/**
 * Default handler for SP UI.
 */
public class HawkbitUIErrorHandler extends DefaultErrorHandler {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(HawkbitUIErrorHandler.class);

    private static final String STYLE = NOTIFICATION_FAILURE + " " + NOTIFICATION_SMALL + " " + NOTIFICATION_CLOSABLE;

    @Override
    public void error(final ErrorEvent event) {

        LOG.error("Error in UI: ", event.getThrowable());

        final Optional<Page> originError = getPageOriginError(event);

        if (originError.isPresent()) {
            final HawkbitNotificationMessage message = buildNotification(getRootExceptionFrom(event));
            message.show(originError.get());
        }
    }

    private Throwable getRootExceptionFrom(final ErrorEvent event) {

        return getRootCauseOf(event.getThrowable());
    }

    private Throwable getRootCauseOf(final Throwable exception) {

        if (exception.getCause() != null) {
            return getRootCauseOf(exception.getCause());
        }

        return exception;
    }

    private Optional<Page> getPageOriginError(final ErrorEvent event) {

        final Component errorOrigin = findAbstractComponent(event);

        if (errorOrigin != null && errorOrigin.getUI() != null) {
            return Optional.fromNullable(errorOrigin.getUI().getPage());
        }

        return Optional.absent();
    }

    protected HawkbitNotificationMessage buildNotification(final Throwable exception) {

        final HawkbitNotificationMessage notification = new HawkbitNotificationMessage();
        final I18N i18n = SpringContextHelper.getBean(I18N.class);
        notification.decorateWith(STYLE, i18n.get("caption.error"),
                i18n.get("message.error.temp", exception.getClass().getSimpleName()), false);

        return notification;
    }

}
