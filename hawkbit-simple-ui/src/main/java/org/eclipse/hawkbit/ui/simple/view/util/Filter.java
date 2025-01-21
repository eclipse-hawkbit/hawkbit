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
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;

public class Filter extends Div {

    private transient Rsql rsql;

    public Filter(final Consumer<String> changeListener, final Rsql primaryRsql, final Rsql secondaryOptionalRsql) {
        rsql = primaryRsql;

        final HorizontalLayout layout = new HorizontalLayout();

        setWidthFull();
        addClassNames(LumoUtility.Padding.Horizontal.NONE, LumoUtility.Padding.Vertical.SMALL,
                LumoUtility.BoxSizing.BORDER);

        final Div filtersDiv = new Div();
        filtersDiv.setWidthFull();
        filtersDiv.add(primaryRsql.components());
        filtersDiv.addClassName(LumoUtility.Gap.SMALL);

        final Button searchBtn = Utils.tooltip(new Button(VaadinIcon.REFRESH.create()), "Search");
        searchBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        searchBtn.addClickListener(e -> changeListener.accept(rsql.filter()));
        final Button resetBtn = Utils.tooltip(new Button(VaadinIcon.ERASER.create()), "Reset");
        resetBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        resetBtn.addClickListener(e -> {
            clear(layout.getChildren());
            changeListener.accept(primaryRsql.filter());
        });

        final Div actionDiv = new Div();
        actionDiv.add(searchBtn, resetBtn);
        actionDiv.addClassNames(LumoUtility.Gap.SMALL);

        layout.setPadding(true);
        layout.setAlignItems(FlexComponent.Alignment.BASELINE);
        layout.add(filtersDiv);

        layout.add(actionDiv);
        if (secondaryOptionalRsql != null) {
            final Button toggleBtn = Utils.tooltip(new Button(VaadinIcon.FLIP_V.create()), "Toggle Search");
            toggleBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            toggleBtn.addClickListener(e -> {
                filtersDiv.removeAll();
                synchronized (this) { // toggle
                    rsql = rsql == primaryRsql ? secondaryOptionalRsql : primaryRsql;
                }
                filtersDiv.add(rsql.components());
                changeListener.accept(primaryRsql.filter());
            });
            layout.add(toggleBtn);
        }
        add(layout);

        changeListener.accept(primaryRsql.filter());
    }

    public static String filter(final Map<String, Object> keyToValues) {
        final Map<String, Object> normalized =
                new HashMap<>(keyToValues)
                        .entrySet()
                        .stream()
                        .filter(e -> {
                            if (e.getValue() instanceof Optional<?> opt) {
                                return opt.isPresent();
                            } else {
                                return e.getValue() != null;
                            }
                        })
                        .filter(e -> !(e.getValue() instanceof Collection<?> coll && coll.isEmpty()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (normalized.isEmpty()) {
            return null;
        } else if (normalized.size() == 1) {
            return normalized.entrySet().stream()
                    .findFirst().map(e -> filter(e.getKey(), e.getValue())).orElse(null); // never return null!
        } else {
            final StringBuilder sb = new StringBuilder();
            normalized.forEach((k, v) -> {
                if (v instanceof Collection<?>) {
                    sb.append('(').append(filter(k, v)).append(')');
                } else if (v instanceof Optional<?> opt) {
                    sb.append(filter(k, opt.get()));
                } else {
                    sb.append(filter(k, v));
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
}
