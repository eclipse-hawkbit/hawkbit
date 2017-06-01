/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.customrenderers.renderers;

import java.io.Serializable;
import java.util.Locale;

import org.eclipse.hawkbit.ui.rollout.StatusFontIcon;
import org.springframework.util.StringUtils;

import com.vaadin.data.util.converter.Converter;

/**
 * Converter that adapts to a model and converts to a label presentation.
 *
 * @param <T>
 *            The type the converter adapts to
 */
public abstract class AbstractHtmlLabelConverter<T> implements Converter<String, T> {

    private static final long serialVersionUID = 1L;

    private LabelAdapter<T> adapter;

    @Override
    public T convertToModel(final String value, final Class<? extends T> targetType, final Locale locale) {
        // not needed
        return null;
    }

    @Override
    public String convertToPresentation(final T status, final Class<? extends String> targetType, final Locale locale) {
        return convert(status);
    }

    @Override
    public Class<String> getPresentationType() {
        return String.class;
    }

    /**
     * Adds an appropriate adapter to the converter. The adapter is mandatory
     * for the converter.
     *
     * @param adapter
     *            label-adapter that converts from model to label-presentation.
     * @return self for method-chaining
     */
    public AbstractHtmlLabelConverter<T> addAdapter(final LabelAdapter<T> adapter) {
        this.adapter = adapter;
        return this;
    }

    /**
     * Converts the model data to a string representation of the presentation
     * label that is used on client-side to style the label.
     *
     * @param status
     *            model data
     * @return string representation of label
     */
    private String convert(final T status) {
        if (adapter == null) {
            throw new IllegalStateException(
                    "Adapter must be set before usage! Convertion without adapter is not possible!");
        }
        final StatusFontIcon statusProps = adapter.adapt(status);
        // fail fast
        if (statusProps == null) {
            return "";
        }
        final String codePoint = getCodePoint(statusProps);
        final String title = statusProps.getTitle();
        return getStatusLabelDetailsInString(codePoint, statusProps.getStyle(), title, statusProps.getId(),
                statusProps.isDisabled());
    }

    /**
     * Creates a key:value map represented by a comma-separated list.
     *
     * @param fontIcon
     *            the font representing the icon
     * @param style
     *            the style
     * @param title
     *            the title shown as tooltip
     * @param id
     *            the id for direct access
     * @param disabled
     *            disabled-state of the icon
     * @return string representation of key:value map
     */
    private static String getStatusLabelDetailsInString(final String value, final String style, final String title,
            final String id, final boolean disabled) {
        final StringBuilder val = new StringBuilder();
        if (!StringUtils.isEmpty(value)) {
            val.append("value:").append(value).append(",");
        }
        if (!StringUtils.isEmpty(style)) {
            val.append("style:").append(style).append(",");
        }
        if (!StringUtils.isEmpty(title)) {
            val.append("title:").append(title).append(",");
        }
        if (disabled) {
            val.append("disabled:true").append(",");
        }
        return val.append("id:").append(id).toString();
    }

    /**
     * Retrieves the codepoint from the font-icon.
     *
     * @param statusFontIcon
     *            the label-metadata that holds the font-icon
     *
     */
    private static String getCodePoint(final StatusFontIcon statusFontIcon) {
        return statusFontIcon.getFontIcon() != null ? Integer.toString(statusFontIcon.getFontIcon().getCodepoint())
                : null;
    }

    /**
     * Adapts from model data to presentation data.
     *
     * @param <T>
     *            The type to adapt to
     */
    @FunctionalInterface
    public interface LabelAdapter<T> extends Serializable {

        /**
         * @param status
         *            model data
         * @return meta representation that is used as input for label
         *         generation.
         */
        StatusFontIcon adapt(final T status);
    }

}
