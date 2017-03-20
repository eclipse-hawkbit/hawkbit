/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.utils;

import java.io.Serializable;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;

import com.vaadin.ui.UI;

/**
 * Utility class leveraging Spring Boot auto configuration of
 * {@link MessageSource} and with Vaadins {@link UI#getLocale()}.
 *
 */
public class VaadinMessageSource implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(VaadinMessageSource.class);

    private final transient MessageSource source;

    /**
     * @param source
     *            from Spring to resolve messages
     */
    public VaadinMessageSource(final MessageSource source) {
        this.source = source;
    }

    /**
     * Tries to resolve the message based on
     * {@link HawkbitCommonUtil#getLocale()}. Returns message code if fitting
     * message could not be found.
     *
     * @param code
     *            the code to lookup up.
     * @param args
     *            Array of arguments that will be filled in for params within
     *            the message.
     *
     * @return the resolved message, or the message code if the lookup fails.
     *
     * @see MessageSource#getMessage(String, Object[], Locale)
     * @see HawkbitCommonUtil#getLocale()
     */
    public String getMessage(final String code, final Object... args) {
        try {
            return source.getMessage(code, args, HawkbitCommonUtil.getLocale());
        } catch (final NoSuchMessageException ex) {
            LOG.error("Failed to retrieve message!", ex);
            return code;
        }
    }
}
