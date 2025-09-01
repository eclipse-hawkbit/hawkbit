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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.springframework.util.ObjectUtils;

public class Filter extends Div {

    private transient Rsql rsql;
    private final transient Rsql secondaryRsql;
    private final transient Rsql primaryRsql;
    private final transient Div filtersDiv;

    public Filter(final Consumer<String> changeListener, final Rsql primaryRsql, final Rsql secondaryOptionalRsql) {
        rsql = primaryRsql;
        this.primaryRsql = primaryRsql;
        secondaryRsql = secondaryOptionalRsql;

        final HorizontalLayout layout = new HorizontalLayout();

        setWidthFull();
        addClassNames(LumoUtility.Padding.Horizontal.NONE, LumoUtility.Padding.Vertical.SMALL,
                LumoUtility.BoxSizing.BORDER);

        filtersDiv = new Div();
        filtersDiv.setWidthFull();
        filtersDiv.add(primaryRsql.components());
        filtersDiv.addClassName(LumoUtility.Gap.SMALL);

        final Button searchBtn = Utils.tooltip(new Button(VaadinIcon.SEARCH.create()), "Search (Enter)");
        searchBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        searchBtn.addClickListener(e -> changeListener.accept(rsql.filter()));
        searchBtn.addClickShortcut(Key.ENTER);
        final Button resetBtn = Utils.tooltip(new Button(VaadinIcon.REFRESH.create()), "Reset");
        resetBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        resetBtn.addClickListener(e -> {
            clear(layout.getChildren());
            changeListener.accept(primaryRsql.filter());
        });

        final HorizontalLayout actions = new HorizontalLayout(searchBtn, resetBtn);
        actions.addClassNames(LumoUtility.Gap.SMALL);

        layout.setPadding(true);
        layout.setAlignItems(FlexComponent.Alignment.BASELINE);
        layout.add(filtersDiv);
        layout.add(actions);

        if (secondaryOptionalRsql != null) {
            final Button toggleBtn = Utils.tooltip(new Button(VaadinIcon.FLIP_V.create()), "Toggle Search");
            toggleBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            toggleBtn.addClickListener(e -> {
                toggle();
                changeListener.accept(primaryRsql.filter());
            });
            layout.add(toggleBtn);
        }
        add(layout);

        changeListener.accept(primaryRsql.filter());
    }

    private void toggle() {
        // toggle
        filtersDiv.removeAll();
        synchronized (this) {
            rsql = rsql == primaryRsql ? secondaryRsql : primaryRsql;
        }
        filtersDiv.add(rsql.components());
    }

    public void setFilter(String string, boolean allowToggle) {
        var otherFilter = rsql == primaryRsql ? secondaryRsql : primaryRsql;
        Stream<Filter.Rsql> rsqlFIlter;
        // logic to find the filter to use
        if (allowToggle) {
            rsqlFIlter = Stream.of(this.rsql);
        } else {
            rsqlFIlter = Stream.of(this.rsql, otherFilter);
        }
        rsqlFIlter.filter(RsqlRw.class::isInstance).findFirst().map(RsqlRw.class::cast).ifPresent(f -> {
            if (f == otherFilter) {
                toggle();
            }
            f.setFilter(string);
        });
    }

    public static String filter(final Map<Object, Object> keyToValues) {
        final Map<Object, Object> normalized = new HashMap<>(keyToValues)
                .entrySet()
                .stream()
                .filter(e -> !ObjectUtils.isEmpty(e.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (normalized.isEmpty()) {
            return null;
        } else {
            final StringBuilder sb = new StringBuilder();
            normalized.forEach((k, v) -> {
                if (k instanceof Collection<?> keyList) {
                    sb.append('(').append(
                            keyList.stream().map(subKey -> filter((String) subKey, v))
                                    .collect(Collectors.joining(" or "))).append(")");
                } else if (k instanceof String key) {
                    if (v instanceof Collection<?>) {
                        sb.append('(').append(filter(key, v)).append(')');
                    } else {
                        sb.append(filter(key, v));
                    }
                }
                sb.append(';');
            });
            return sb.substring(0, sb.length() - 1);
        }
    }

    private static String filter(final String key, final Object value) {
        if (value == null || (value instanceof Collection<?> coll && coll.isEmpty())) {
            return null;
        }

        if (value instanceof Collection<?> coll) {
            final StringBuilder sb = new StringBuilder();
            coll.stream().forEach(next -> sb.append(key).append("==").append(next).append(','));
            return sb.substring(0, sb.length() - 1);
        } else if (value instanceof Optional<?> opt) {
            if (opt.isEmpty()) {
                return null;
            } else {
                return key + "==" + opt.get();
            }
        } else {
            return key + "==" + value;
        }
    }

    private static void clear(final Stream<Component> components) {
        if (components == null) {
            return;
        }

        components.forEach(component -> {
            if (component instanceof HasValue<?, ?> hasValue) {
                hasValue.clear();
            }
            clear(component.getChildren());
        });
    }

    public interface Rsql {

        List<Component> components();

        String filter();
    }

    public interface RsqlRw {

        void setFilter(String filter);
    }
}
