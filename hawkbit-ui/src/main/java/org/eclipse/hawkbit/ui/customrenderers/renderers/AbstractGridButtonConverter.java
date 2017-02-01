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

import org.eclipse.hawkbit.ui.customrenderers.client.renderers.FontIconData;
import org.eclipse.hawkbit.ui.rollout.StatusFontIcon;

import com.vaadin.data.util.converter.Converter;

/**
 * Converter that adapts to a model and converts to a grid-button presentation.
 *
 * @param <T>
 *            The type the converter adapts to
 */
public abstract class AbstractGridButtonConverter<T> implements Converter<FontIconData, T> {

    private static final long serialVersionUID = 1L;

    private GridButtonAdapter<T> adapter;

    @Override
    public T convertToModel(final FontIconData meta, final Class<? extends T> targetType, final Locale locale) {
        // not needed
        return null;
    }

    @Override
    public FontIconData convertToPresentation(final T status, final Class<? extends FontIconData> targetType,
            final Locale locale) {
        if (adapter == null) {
            throw new IllegalStateException(
                    "Adapter must be set before usage! Convertion without adapter is not possible!");
        }
        return createFontIconData(adapter.adapt(status));
    }

    /**
     * Creates a data transport object for the icon meta data.
     *
     * @param meta
     *            icon metadata
     * @return icon metadata transport object
     */
    private static FontIconData createFontIconData(StatusFontIcon meta) {
        FontIconData result = new FontIconData();
        result.setFontIconHtml(meta.getFontIcon().getHtml());
        result.setTitle(meta.getTitle());
        result.setStyle(meta.getStyle());
        result.setId(meta.getId());
        result.setDisabled(meta.isDisabled());

        return result;
    }

    @Override
    public Class<FontIconData> getPresentationType() {
        return FontIconData.class;
    }

    /**
     * Adds an appropriate adapter to the converter. The adapter is mandatory
     * for the converter.
     *
     * @param adapter
     *            label-adapter that converts from model to label-presentation.
     * @return self for method-chaining
     */
    public AbstractGridButtonConverter<T> addAdapter(GridButtonAdapter<T> adapter) {
        this.adapter = adapter;
        return this;
    }

    /**
     * Adapts from model data to presentation data.
     *
     * @param <T>
     *            The type to adapt to
     */
    @FunctionalInterface
    public interface GridButtonAdapter<T> extends Serializable {

        /**
         * @param status
         *            model data
         * @return meta representation that is used as input for label
         *         generation.
         */
        StatusFontIcon adapt(final T status);
    }

}
