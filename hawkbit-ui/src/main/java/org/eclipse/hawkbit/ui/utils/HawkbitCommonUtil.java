/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import org.eclipse.hawkbit.ui.UiProperties;
import org.eclipse.hawkbit.ui.UiProperties.Localization;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import com.vaadin.ui.UI;

/**
 * Common util class.
 */
public final class HawkbitCommonUtil {
    public static final String SP_STRING_PIPE = " | ";

    private HawkbitCommonUtil() {

    }

    /**
     * Concatenate the given text all the string arguments with the given
     * delimiter.
     *
     * @param delimiter
     *            the delimiter text to be used while concatenation.
     * @param texts
     *            all these string values will be concatenated with the given
     *            delimiter.
     * @return null in case no text arguments to be compared. just concatenation
     *         of all texts arguments if "delimiter" is null or empty.
     *         concatenation of all texts arguments with "delimiter" if it not
     *         null.
     */
    public static String concatStrings(final String delimiter, final String... texts) {
        final String delim = delimiter == null ? "" : delimiter;
        final StringBuilder conCatStrBldr = new StringBuilder();
        if (null != texts) {
            for (final String text : texts) {
                conCatStrBldr.append(delim);
                conCatStrBldr.append(text);
            }
        }
        final String conCatedStr = conCatStrBldr.toString();
        return delim.length() > 0 && conCatedStr.startsWith(delim) ? conCatedStr.substring(1) : conCatedStr;
    }

    /**
     * Get concatenated string of software module name and version.
     *
     * @param name
     *            Name
     * @param version
     *            Version
     * @return String concatenated string
     */
    public static String getFormattedNameVersion(final String name, final String version) {
        return name + ":" + version;
    }

    /**
     * Gets the locale of the current Vaadin UI. If the locale can not be
     * determined, the default locale is returned instead.
     *
     * @return the current locale, never {@code null}.
     * @see com.vaadin.ui.UI#getLocale()
     * @see java.util.Locale#getDefault()
     */
    public static Locale getCurrentLocale() {
        final UI currentUI = UI.getCurrent();
        return currentUI == null ? Locale.getDefault() : currentUI.getLocale();
    }

    /**
     * Determine the language that should be used considering localization
     * properties and a desired Locale
     * 
     * @param localizationProperties
     *            UI Localization settings
     * @param desiredLocale
     *            desired Locale
     * @return Locale to be used according to UI and properties
     */
    public static Locale getLocaleToBeUsed(final UiProperties.Localization localizationProperties,
            final Locale desiredLocale) {
        final List<Locale> availableLocals = localizationProperties.getAvailableLocals();
        // ckeck if language code of UI locale matches an available local.
        // Country, region and variant are ignored. "availableLocals" must only
        // contain language codes without country or other extensions.
        if (availableLocals.contains(desiredLocale)) {
            return desiredLocale;
        }
        return localizationProperties.getDefaultLocal();
    }

    /**
     * Set localization considering properties and UI settings.
     * 
     * @param ui
     *            UI to setup
     * @param localizationProperties
     *            UI localization settings
     * @param i18n
     *            Localization message source
     */
    public static void initLocalization(final UI ui, final Localization localizationProperties,
            final VaadinMessageSource i18n) {
        ui.setLocale(HawkbitCommonUtil.getLocaleToBeUsed(localizationProperties, ui.getSession().getLocale()));
        ui.getReconnectDialogConfiguration()
                .setDialogText(i18n.getMessage(UIMessageIdProvider.VAADIN_SYSTEM_TRYINGRECONNECT));
    }

    /**
     * Gets the entities by pageable
     *
     * @param provider
     *            Pageable provider
     * @param <T>
     *            Generic type
     *
     * @return Entities
     */
    public static <T> List<T> getEntitiesByPageableProvider(final Function<Pageable, Slice<T>> provider) {
        Pageable query = PageRequest.of(0, SPUIDefinitions.PAGE_SIZE);
        Slice<T> slice;
        final List<T> entities = new ArrayList<>();

        do {
            slice = provider.apply(query);
            entities.addAll(slice.getContent());
        } while ((query = slice.nextPageable()) != Pageable.unpaged());

        return entities;
    }

    /**
     * Verify target
     *
     * @param count
     *            Total target
     *
     * @return True if at least one target is present else false
     */
    public static boolean atLeastOnePresent(final Long count) {
        return count != null && count > 0L;
    }

    /**
     * Gets the count of total targets
     *
     * @param countList
     *            List of target per group
     *
     * @return Total count
     */
    public static Long getSumOf(final Collection<Long> countList) {
        return countList.stream().mapToLong(Long::longValue).sum();
    }

    /**
     * Escapes potentially malicious html string.
     *
     * @param html
     *            potentially malicious html string
     *
     * @return sanitized html
     */
    public static String sanitizeHtml(final String html) {
        if (StringUtils.isEmpty(html)) {
            return "";
        }

        return HtmlUtils.htmlEscape(html).replace("\n", "</br>");
    }
}
