/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
/*
 * Copyright 2015 The original authors Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 * for the specific language governing permissions and limitations under the License.
 */

/**
 * copied and adapted by Bosch Software Innovations GmbH, Germany.
 *
 * @see https://github.com/peholmst/vaadin4spring/blob/master/addons/i18n/src/main/java/org/vaadin/spring/i18n/I18N.java
 */
package org.eclipse.hawkbit.ui.utils;

import java.io.Serializable;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Service;

import com.vaadin.ui.UI;

/**
 * Utility class leveraging Spring Boot auto configuration of
 * {@link MessageSource}.
 *
 *
 *
 *
 */
@Service
public class I18N implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(I18N.class);

    @Autowired
    private transient MessageSource source;

    /**
     * Tries to resolve the message.
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
     * @see #getLocale()
     */
    public String get(final String code, final Object... args) {
        return getMessage(code, args);
    }

    /**
     * Tries to resolve the message.
     *
     * @param code
     *            the code to lookup up.
     *
     * @return the resolved message, or the message code if the lookup fails.
     *
     * @see MessageSource#getMessage(String, Object[], Locale)
     * @see #getLocale()
     */
    public String get(final String code) {
        return getMessage(code, null);
    }

    private String getMessage(final String code, final Object[] args) {
        try {
            return source.getMessage(code, args, getLocale());
        } catch (final NoSuchMessageException ex) {
            LOG.error("Failed to retrieve message!", ex);
            return code;
        }
    }

    /**
     * Gets the locale of the current Vaadin UI. If the locale can not be
     * determinted, the default locale is returned instead.
     *
     * @return the current locale, never {@code null}.
     * @see com.vaadin.ui.UI#getLocale()
     * @see java.util.Locale#getDefault()
     */
    public Locale getLocale() {
        final UI currentUI = UI.getCurrent();
        Locale locale = currentUI == null ? null : currentUI.getLocale();
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return locale;
    }
}
