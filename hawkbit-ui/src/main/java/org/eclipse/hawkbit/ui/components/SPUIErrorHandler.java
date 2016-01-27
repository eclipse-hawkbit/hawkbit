/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.components;

import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SpringContextHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.server.DefaultErrorHandler;
import com.vaadin.server.ErrorEvent;
import com.vaadin.ui.Component;
import com.vaadin.ui.themes.ValoTheme;

/**
 * 
 * Default handler for SP UI.
 * 
 *
 * 
 *
 * 
 */
public class SPUIErrorHandler extends DefaultErrorHandler {

    /**
     * Comment for <code>serialVersionUID</code>.
     */
    private static final long serialVersionUID = 1877326479308824191L;
    /**
     * logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(SPUIErrorHandler.class);

    @Override
    public void error(final ErrorEvent event) {
        final SPNotificationMessage notification = new SPNotificationMessage();
        // Build error style
        final StringBuilder style = new StringBuilder(ValoTheme.NOTIFICATION_FAILURE);
        style.append(' ');
        style.append(ValoTheme.NOTIFICATION_SMALL);
        style.append(' ');
        style.append(ValoTheme.NOTIFICATION_CLOSABLE);
        final I18N i18n = SpringContextHelper.getBean(I18N.class);
        String exceptionName = null;
        // From the exception trace we get the expected exception class name
        for (Throwable error = event.getThrowable(); error != null; error = error.getCause()) {
            exceptionName = HawkbitCommonUtil.getLastSequenceBySplitByDot(error.getClass().getName());
            LOG.error("Error in SP-UI:", error);
        }
        final Component errorOrgin = findAbstractComponent(event);
        if (null != errorOrgin && errorOrgin.getUI() != null) {
            notification.showNotification(style.toString(), i18n.get("caption.error"),
                    i18n.get("message.error.temp", new Object[] { exceptionName }), false,
                    errorOrgin.getUI().getPage());
        }
    }
}
