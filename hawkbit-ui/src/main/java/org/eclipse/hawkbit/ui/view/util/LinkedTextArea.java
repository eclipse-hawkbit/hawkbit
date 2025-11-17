/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.view.util;

import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.card.CardVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

public class LinkedTextArea extends Div {

    String queryPrefix;
    Card card;

    public LinkedTextArea(String title, String queryPrefix) {
        super();
        card = new Card();
        card.setTitle(title);
        this.queryPrefix = queryPrefix;
    }

    public void setValueWithLink(String value, String query) {
        var span = new Span(value);
        span.setWhiteSpace(WhiteSpace.PRE_WRAP);
        card.add(span);
        card.addThemeVariants(CardVariant.LUMO_ELEVATED);
        if (query != null) {
            var a = new Anchor(queryPrefix + query, card);
            a.addClassName("nocolor");
            add(a);
        } else {
            add(card);
        }
    }
}
