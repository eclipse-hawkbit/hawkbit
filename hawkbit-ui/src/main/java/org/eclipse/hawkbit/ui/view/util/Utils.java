/**
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.view.util;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToLongFunction;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.IconFactory;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.QuerySortOrder;
import com.vaadin.flow.data.provider.CallbackDataProvider.FetchCallback;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LocalDateTimeRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.theme.lumo.LumoUtility;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtActionType;
import org.eclipse.hawkbit.ui.view.Constants;

@Slf4j
public class Utils {

    private Utils() {
        // prevent initialization
    }

    public static final String COMBO_NAME_ALLOWED_CHARS = "[0-9a-zA-Z-_./]";

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

    public static <T> ComboBox<T> nameComboBox(
            final String label,
            final Consumer<HasValue.ValueChangeEvent<T>> changeListener,
            final FetchCallback<T, String> listHandler) {
        final ComboBox<T> combo = new ComboBox<T>(label, changeListener::accept);
        combo.setAllowedCharPattern(Utils.COMBO_NAME_ALLOWED_CHARS);
        combo.setItemsWithFilterConverter(listHandler, nameFilter -> "name==*" + nameFilter + "*");
        return combo;
    }

    @SuppressWarnings("java:S119") // better readability
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
            final ConfirmDialog dialog = promptForDeleteConfirmation(removeHandler, selectionGrid);
            final Button removeBtn = tooltip(new Button(VaadinIcon.MINUS.create()), "Remove");
            removeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_CONTRAST);
            removeBtn.addClickListener(e -> dialog.open());

            layout.add(removeBtn);
        }
        return layout;
    }

    private static <T, ID> ConfirmDialog promptForDeleteConfirmation(Function<SelectionGrid<T, ID>, CompletionStage<Void>> removeHandler,
            SelectionGrid<T, ID> selectionGrid) {
        final ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Confirm Deletion");
        dialog.setText("Are you sure you want to delete the selected items? This action cannot be undone.");

        dialog.setCancelable(true);
        dialog.addCancelListener(event -> dialog.close());

        dialog.setConfirmButtonTheme(ButtonVariant.LUMO_ERROR.getVariantName());
        dialog.setConfirmText("Delete");
        dialog.addConfirmListener(event -> {
            removeHandler
                    .apply(selectionGrid)
                    .thenAccept(v -> selectionGrid.refreshGrid(false));
            dialog.close();
        });
        return dialog;
    }

    public static <T> void remove(final Collection<T> remove, final Set<T> from, final Function<T, ?> idFn) {
        remove.forEach(toRemove -> {
            final Object id = idFn.apply(toRemove);
            for (final Iterator<T> i = from.iterator(); i.hasNext();) {
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

    public static Icon iconColored(final IconFactory component, final String text, final String color) {
        var icon = tooltip(component.create(), text);
        icon.setColor(color);
        return icon;
    }

    public static Select<MgmtActionType> actionTypeControls(DateTimePicker forceTime) {

        Select<MgmtActionType> actionType = new Select<>();
        actionType.setLabel(Constants.ACTION_TYPE);
        actionType.setItems(MgmtActionType.values());
        actionType.setValue(MgmtActionType.FORCED);
        final ComponentRenderer<Component, MgmtActionType> actionTypeRenderer = new ComponentRenderer<>(actionTypeO -> switch (actionTypeO) {
            case SOFT -> new Text(Constants.SOFT);
            case FORCED -> new Text(Constants.FORCED);
            case DOWNLOAD_ONLY -> new Text(Constants.DOWNLOAD_ONLY);
            case TIMEFORCED -> forceTime;
        });
        actionType.addValueChangeListener(e -> actionType.setRenderer(actionTypeRenderer));
        actionType.setItemLabelGenerator(startTypeO -> switch (startTypeO) {
            case SOFT -> Constants.SOFT;
            case FORCED -> Constants.FORCED;
            case DOWNLOAD_ONLY -> Constants.DOWNLOAD_ONLY;
            case TIMEFORCED -> "Time Forced at " + (forceTime.isEmpty() ? "" : " " + forceTime.getValue());
        });
        actionType.setWidthFull();
        return actionType;
    }

    public static class BaseDialog<T> extends Dialog {

        protected final transient CompletableFuture<T> result = new CompletableFuture<>();

        protected BaseDialog(final String headerTitle) {
            setHeaderTitle(headerTitle);
            setMinWidth(640, Unit.PIXELS);

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

    private static ZoneId getZoneId() {
        CompletableFuture<ZoneId> zoneId = new CompletableFuture<>();
        UI.getCurrent().getPage().retrieveExtendedClientDetails(details -> zoneId.complete(ZoneId.of(details.getTimeZoneId())));
        try {
            return zoneId.get(1, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (TimeoutException | ExecutionException ignored) {
            log.warn("failed to get zone");
        }
        return ZoneId.systemDefault();
    }

    public static <G> LocalDateTimeRenderer<G> localDateTimeRenderer(ToLongFunction<G> f) {

        return new LocalDateTimeRenderer<>((e) -> LocalDateTime.ofInstant(Instant.ofEpochMilli(f.applyAsLong(e)), getZoneId()),
                () -> DateTimeFormatter.ofLocalizedDateTime(
                        FormatStyle.SHORT,
                        FormatStyle.MEDIUM).withLocale(UI.getCurrent().getLocale()));
    }

    public static String localDateTimeFromTs(long timestamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), getZoneId()).format(DateTimeFormatter.ofLocalizedDateTime(
                FormatStyle.SHORT,
                FormatStyle.MEDIUM).withLocale(UI.getCurrent().getLocale()));
    }

    public static String getSortParam(List<QuerySortOrder> querySortOrders) {
        return getSortParam(querySortOrders, null);
    }

    public static String getSortParam(List<QuerySortOrder> querySortOrders, String defaultSort) {
        if (!querySortOrders.isEmpty()) {
            QuerySortOrder firstSort = querySortOrders.get(0);
            String order = firstSort.getDirection() == SortDirection.ASCENDING ? "asc" : "desc";
            return String.format("%s:%s", firstSort.getSorted(), order);
        }
        return defaultSort;
    }

    public static String durationFromMillis(Long time) {
        var duration = Duration.between(Instant.ofEpochMilli(time), Instant.now());
        var day = duration.toDaysPart();
        if (day > 2) {
            return day + "d";
        }
        return duration.withNanos(0).toString()
                .substring(2)
                .replaceFirst("(^\\d+[HMS]\\d*M*)", "$1")
                .toLowerCase();
    }
}
