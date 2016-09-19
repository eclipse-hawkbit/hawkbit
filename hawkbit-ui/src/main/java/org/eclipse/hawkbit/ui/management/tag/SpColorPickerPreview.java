/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.tag;

import java.lang.reflect.Field;

import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;

import com.google.common.base.Throwables;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.shared.ui.colorpicker.Color;
import com.vaadin.ui.TextField;
import com.vaadin.ui.components.colorpicker.ColorPickerPreview;

/**
 *
 *
 *
 * Tag ColorPicker Preview field CssLayout cannot be removed because of the deep
 * inheritance issue, since protected method fireEvent() of the superclass of
 * CssLayout is used in textChange() overridden method
 *
 *
 */
public final class SpColorPickerPreview extends ColorPickerPreview implements TextChangeListener {
    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     * 
     * @param color
     *            of the picker
     */
    public SpColorPickerPreview(final Color color) {
        super(color);

        try {
            final Field textField = ColorPickerPreview.class.getDeclaredField("field");
            textField.setAccessible(true);
            ((TextField) textField.get(this)).setId(UIComponentIdProvider.COLOR_PREVIEW_FIELD);
            ((TextField) textField.get(this)).addTextChangeListener(this);
        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
            Throwables.propagate(e);
        }
    }

    @Override
    public void textChange(final TextChangeEvent event) {
        super.valueChange(new ValueChangeEvent() {
            private static final long serialVersionUID = 1L;

            @Override
            public Property<String> getProperty() {
                return new EventHolder(event);
            }

        });

    }

    private static final class EventHolder implements Property<String> {
        private static final long serialVersionUID = 1L;
        private final TextChangeEvent event;

        private EventHolder(final TextChangeEvent event) {
            this.event = event;
        }

        @Override
        public String getValue() {
            return event.getText();
        }

        @Override
        public void setValue(final String newValue) {
            // not needed as this property is only a hull for TextChangeEvent
            // payload
        }

        @Override
        public Class<String> getType() {
            return String.class;
        }

        @Override
        public boolean isReadOnly() {
            return false;
        }

        @Override
        public void setReadOnly(final boolean newStatus) {
            // not needed as this property is only a hull for TextChangeEvent
            // payload
        }
    }

}
