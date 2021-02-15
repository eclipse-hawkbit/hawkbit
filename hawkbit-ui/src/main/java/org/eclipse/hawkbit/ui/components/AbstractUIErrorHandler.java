/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.components;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.ClientConnector;
import com.vaadin.server.DefaultErrorHandler;
import com.vaadin.server.ErrorEvent;
import com.vaadin.server.Page;
import com.vaadin.shared.Connector;
import com.vaadin.ui.Component;
import com.vaadin.ui.UI;
import org.eclipse.hawkbit.ui.common.notification.ParallelNotification;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.SpringContextHolder;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.util.StringUtils;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Abstract error handler for the UI.
 */
public abstract class AbstractUIErrorHandler extends DefaultErrorHandler {

    private static final long serialVersionUID = 1L;
    private final transient Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Create and print a notification by the root cause of the problem.
     * 
     * @param event the error event
     */
    protected void showNotification(final ErrorEvent event) {
        showNotification(event, buildNotification(event));
    }

    /**
     * Print the given notification.
     * 
     * @param event
     *            the error event
     * @param notification
     *            the notification
     */
    protected void showNotification(final ErrorEvent event, final ParallelNotification notification) {
        if (event instanceof ClientConnector.ConnectorErrorEvent) {
            final Connector connector = ((ClientConnector.ConnectorErrorEvent) event).getConnector();
            if (connector instanceof UI) {
                final UI uiInstance = (UI) connector;
                uiInstance.access(() -> notification.show(uiInstance.getPage()));
                return;
            }
        }

        final Optional<Page> originError = getPageOriginError(event);
        if (originError.isPresent()) {
            notification.show(originError.get());
            return;
        }

        notification.show(Page.getCurrent());
    }

    protected static Throwable getRootExceptionFrom(final ErrorEvent event) {
        return getRootCauseOf(event.getThrowable());
    }

    protected static Throwable getRootCauseOf(final Throwable ex) {
        return NestedExceptionUtils.getRootCause(ex);
    }

    protected static Optional<Page> getPageOriginError(final ErrorEvent event) {

        final Component errorOrigin = findAbstractComponent(event);

        if (errorOrigin != null && errorOrigin.getUI() != null) {
            return Optional.ofNullable(errorOrigin.getUI().getPage());
        }

        return Optional.empty();
    }

    /**
     * Method to build a notification based on an {@link ErrorEvent}.
     *
     * @param event
     *            the error event
     * @return a hawkbit error notification message
     */
    protected ParallelNotification buildNotification(final ErrorEvent event) {
        final Throwable rootException = getRootExceptionFrom(event);

        logger.error("Error in UI: ", rootException);

        final String errorMessage = extractMessageFrom(rootException);
        final VaadinMessageSource i18n = SpringContextHolder.getInstance().getBean(VaadinMessageSource.class);

        return buildErrorNotification(i18n.getMessage("caption.error"), errorMessage);
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

    private static String extractMessageFrom(final Throwable ex) {

        if (!(ex instanceof ConstraintViolationException)) {
            if (!StringUtils.isEmpty(ex.getMessage())) {
                return ex.getMessage();
            }
            return ex.getClass().getSimpleName();
        }

        final Set<ConstraintViolation<?>> violations = ((ConstraintViolationException) ex).getConstraintViolations();

        if (violations == null) {
            return ex.getClass().getSimpleName();
        } else {
            return violations.stream().map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                    .collect(Collectors.joining(System.lineSeparator()));
        }
    }
}
