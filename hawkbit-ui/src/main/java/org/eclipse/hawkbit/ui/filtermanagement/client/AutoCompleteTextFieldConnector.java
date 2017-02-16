/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.filtermanagement.client;

import java.util.List;

import org.eclipse.hawkbit.ui.filtermanagement.TextFieldSuggestionBox;

import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.ui.MenuItem;
import com.vaadin.client.ComponentConnector;
import com.vaadin.client.ServerConnector;
import com.vaadin.client.extensions.AbstractExtensionConnector;
import com.vaadin.client.ui.VOverlay;
import com.vaadin.client.ui.VTextField;
import com.vaadin.shared.ui.Connect;

/**
 * Connector for the AutoCompleteTextField which automatically listens to
 * key-events to show pop-up panel with entered suggestions based on the
 * {@link TextFieldSuggestionBoxServerRpc} call.
 *
 */
@SuppressWarnings({ "deprecation", "squid:CallToDeprecatedMethod", "squid:S1604" })
// need to use VOverlay because otherwise it's not in the correct theme
// widget @see com.vaadin.client.ui.VOverlay.getOverlayContainer()
// GWT 2.7 does not support Java 8
@Connect(TextFieldSuggestionBox.class)
public class AutoCompleteTextFieldConnector extends AbstractExtensionConnector {

    private static final long serialVersionUID = 1L;

    private final transient SuggestionsSelectList select = new SuggestionsSelectList();
    private transient VTextField textFieldWidget;

    private final TextFieldSuggestionBoxServerRpc rpc = getRpcProxy(TextFieldSuggestionBoxServerRpc.class);

    private final transient VOverlay panel = new VOverlay(true, false, true);

    @Override
    protected void init() {
        super.init();

        registerRpc(TextFieldSuggestionBoxClientRpc.class, new TextFieldSuggestionBoxClientRpc() {
            private static final long serialVersionUID = 1L;

            @Override
            public void showSuggestions(final SuggestionContextDto suggestContext) {
                select.clearItems();
                if (suggestContext == null) {
                    panel.hide();
                    return;
                }
                final List<SuggestTokenDto> suggestions = suggestContext.getSuggestions();
                if (suggestions != null && !suggestions.isEmpty()) {
                    select.addItems(suggestions, textFieldWidget, panel, rpc);
                    panel.showRelativeTo(textFieldWidget);
                    select.moveSelectionDown();
                    return;
                }
                panel.hide();
            }
        });
    }

    @Override
    protected void extend(final ServerConnector target) {
        textFieldWidget = (VTextField) ((ComponentConnector) target).getWidget();
        textFieldWidget.setImmediate(true);
        textFieldWidget.textChangeEventMode = "EAGER";
        panel.setWidget(select);
        panel.setStyleName("suggestion-popup");
        panel.setOwner(textFieldWidget);

        textFieldWidget.addKeyUpHandler(new KeyUpHandler() {
            @Override
            public void onKeyUp(final KeyUpEvent event) {
                if (panel.isAttached()) {
                    handlePanelEventDelegation(event);
                } else if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
                    rpc.executeQuery(textFieldWidget.getValue(), textFieldWidget.getCursorPos());
                } else {
                    doAskForSuggestion();
                }
            }
        });
    }

    private void handlePanelEventDelegation(final KeyUpEvent event) {
        switch (event.getNativeKeyCode()) {
        case KeyCodes.KEY_DOWN:
            arrowKeyDown(event);
            break;
        case KeyCodes.KEY_UP:
            arrorKeyUp(event);
            break;
        case KeyCodes.KEY_ESCAPE:
            escapeKey();
            break;
        case KeyCodes.KEY_ENTER:
            enterKey();
            break;
        default:
            doAskForSuggestion();
        }
    }

    private void escapeKey() {
        panel.hide();
    }

    private void enterKey() {
        final MenuItem item = select.getSelectedItem();
        if (item != null) {
            item.getScheduledCommand().execute();
        }
    }

    private void arrorKeyUp(final KeyUpEvent event) {
        select.moveSelectionUp();
        event.preventDefault();
        event.stopPropagation();
    }

    private void arrowKeyDown(final KeyUpEvent event) {
        select.moveSelectionDown();
        event.preventDefault();
        event.stopPropagation();
    }

    private void doAskForSuggestion() {
        rpc.suggest(textFieldWidget.getValue(), textFieldWidget.getCursorPos());
    }
}
