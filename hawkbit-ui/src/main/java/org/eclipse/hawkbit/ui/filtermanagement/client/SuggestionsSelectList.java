/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.filtermanagement.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.PopupPanel;
import com.vaadin.client.WidgetUtil;
import com.vaadin.client.ui.VTextField;

/**
 * The suggestion list within the suggestion pop-up panel.
 */
// Exception squid:S1604 - GWT 2.7 does not support Java 8
@SuppressWarnings("squid:S1604")
public class SuggestionsSelectList extends MenuBar {

    public static final String CLASSNAME = "autocomplete";
    private final Map<String, TokenStartEnd> tokenMap = new HashMap<>();

    /**
     * Constructor.
     */
    public SuggestionsSelectList() {
        super(true);
        setFocusOnHoverEnabled(false);
    }

    /**
     * Adds suggestions to the suggestion menu bar.
     * 
     * @param suggestions
     *            the suggestions to be added
     * @param textFieldWidget
     *            the text field which the suggestion is attached to to bring
     *            back the focus after selection
     * @param popupPanel
     *            pop-up panel where the menu bar is shown to hide it after
     *            selection
     * @param suggestionServerRpc
     *            server RPC to ask for new suggestion after a selection
     */
    public void addItems(final List<SuggestTokenDto> suggestions, final VTextField textFieldWidget,
            final PopupPanel popupPanel, final TextFieldSuggestionBoxServerRpc suggestionServerRpc) {
        for (int index = 0; index < suggestions.size(); index++) {
            final SuggestTokenDto suggestToken = suggestions.get(index);
            final MenuItem mi = new MenuItem(suggestToken.getSuggestion(), true, new ScheduledCommand() {
                @Override
                public void execute() {
                    final String tmpSuggestion = suggestToken.getSuggestion();
                    final TokenStartEnd tokenStartEnd = tokenMap.get(tmpSuggestion);
                    final String text = textFieldWidget.getValue();
                    final StringBuilder builder = new StringBuilder(text);
                    builder.replace(tokenStartEnd.getStart(), tokenStartEnd.getEnd() + 1, tmpSuggestion);
                    textFieldWidget.setValue(builder.toString(), true);
                    popupPanel.hide();
                    textFieldWidget.setFocus(true);
                    suggestionServerRpc.suggest(builder.toString(), textFieldWidget.getCursorPos());
                }
            });
            tokenMap.put(suggestToken.getSuggestion(),
                    new TokenStartEnd(suggestToken.getStart(), suggestToken.getEnd()));
            Roles.getListitemRole().set(mi.getElement());
            WidgetUtil.sinkOnloadForImages(mi.getElement());
            addItem(mi);
        }
    }

    @Override
    public void setStyleName(final String style) {
        super.setStyleName(style + "-" + CLASSNAME);
    }

    @Override
    public MenuItem getSelectedItem() {
        return super.getSelectedItem();
    }

    /**
     * Suggestion Token start and end index.
     *
     */
    public static final class TokenStartEnd {
        final int start;
        final int end;

        /**
         * Constructor.
         * 
         * @param start
         * @param end
         */
        public TokenStartEnd(final int start, final int end) {
            this.start = start;
            this.end = end;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }
    }
}
