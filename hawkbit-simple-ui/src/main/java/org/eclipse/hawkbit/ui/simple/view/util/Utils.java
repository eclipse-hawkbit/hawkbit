/**
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.simple.view.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.theme.lumo.LumoUtility;

public class Utils {

    private Utils() {
        // prevent initialization
    }

    public static TextField textField(final String label) {
        return textField(label, null);
    }

    public static TextField textField(final String label, final Consumer<HasValue.ValueChangeEvent<String>> changeListener) {
        final TextField textField = new TextField(label);
        textField.setWidthFull();
        if (changeListener != null) {
            textField.setRequired(true);
            textField.addValueChangeListener(changeListener::accept);
            textField.setValueChangeMode(ValueChangeMode.TIMEOUT);
            textField.setValueChangeTimeout(200);
        }
        return textField;
    }

    public static NumberField numberField(final String label) {
        return numberField(label, null);
    }

    public static NumberField numberField(final String label, final Consumer<HasValue.ValueChangeEvent<Double>> changeListener) {
        final NumberField numberField = new NumberField(label);
        numberField.setWidthFull();
        if (changeListener != null) {
            numberField.setRequired(true);
            numberField.addValueChangeListener(changeListener::accept);
            numberField.setValueChangeMode(ValueChangeMode.TIMEOUT);
            numberField.setValueChangeTimeout(200);
        }
        return numberField;
    }

    public static <T, ID> HorizontalLayout addRemoveControls(
            final Function<SelectionGrid<T, ID>, CompletionStage<Void>> addHandler,
            final Function<SelectionGrid<T, ID>, CompletionStage<Void>> removeHandler,
            final SelectionGrid<T, ID> selectionGrid, final boolean noPadding) {
        if (addHandler == null && removeHandler == null) {
            throw new IllegalArgumentException("At least one of add or remove handlers must not be null!");
        }

        final HorizontalLayout layout = new HorizontalLayout();
        layout.setWidthFull();
        if (!noPadding) {
            layout.addClassNames(LumoUtility.Padding.Horizontal.XLARGE, LumoUtility.Padding.Vertical.SMALL, LumoUtility.BoxSizing.BORDER);
        }
        layout.setAlignItems(FlexComponent.Alignment.BASELINE);
        layout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        if (addHandler != null) {
            final Button addBtn = tooltip(new Button(VaadinIcon.PLUS.create()), "Add");
            addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            addBtn.addClickListener(e -> addHandler
                    .apply(selectionGrid)
                    .thenAccept(v -> selectionGrid.refreshGrid(true)));
            layout.add(addBtn);
        }
        if (removeHandler != null) {
            final Button removeBtn = tooltip(new Button(VaadinIcon.MINUS.create()), "Remove");
            removeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_CONTRAST);
            removeBtn.addClickListener(e -> removeHandler
                    .apply(selectionGrid)
                    .thenAccept(v -> selectionGrid.refreshGrid(false)));
            layout.add(removeBtn);
        }
        return layout;
    }

    public static <T> void remove(final Collection<T> remove, final Set<T> from, final Function<T, ?> idFn) {
        remove.forEach(toRemove -> {
            final Object id = idFn.apply(toRemove);
            for (final Iterator<T> i = from.iterator(); i.hasNext(); ) {
                if (idFn.apply(i.next()).equals(id)) {
                    i.remove();
                }
            }
        });
    }

    public static void errorNotification(final Throwable t) {
        if (UI.getCurrent() == null) {
            return;
        }

        final Notification notification = new Notification();
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);

        final Div error = new Div(new Text("Error: " + t.getMessage()));

        final Button closeButton = tooltip(new Button(VaadinIcon.CLOSE.create()), "Close");
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        closeButton.addClickListener(event -> notification.close());

        final HorizontalLayout layout = new HorizontalLayout(error, closeButton);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);

        notification.add(layout);
        notification.open();
    }

    public static <T extends Component> T tooltip(final T component, final String text) {
        Tooltip.forComponent(component)
                .withText(text)
                .withPosition(Tooltip.TooltipPosition.TOP_START);
        return component;
    }

    public static class BaseDialog<T> extends Dialog {

        protected final transient CompletableFuture<T> result = new CompletableFuture<>();

        protected BaseDialog(final String headerTitle) {
            setHeaderTitle(headerTitle);
            setHeightFull();

            setModal(true);
            setDraggable(true);
            setResizable(true);

            setCloseOnEsc(true);
            setCloseOnOutsideClick(true);

            final Button closeBtn = tooltip(new Button(VaadinIcon.CLOSE.create()), "Close");
            closeBtn.addClickListener(e -> {
                result.complete(null);
                super.close();
            });
            getHeader().add(closeBtn);
        }

        public CompletionStage<T> result() {
            return result;
        }

        @Override
        public void close() {
            if (!result.isDone()) {
                result.complete(null);
            }
            super.close();
        }
    }
}
