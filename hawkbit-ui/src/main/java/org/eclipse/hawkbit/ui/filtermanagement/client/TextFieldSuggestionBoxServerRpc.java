/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.filtermanagement.client;

import com.vaadin.shared.communication.ServerRpc;

/**
 * Server RPC for the AutoCompleteTextField. The Server RPC interface is used to
 * make client to server calls in Vaadin. Only void methods are allowed in
 * ServerRpc calls.
 */
public interface TextFieldSuggestionBoxServerRpc extends ServerRpc {

    /**
     * Parses the given RSQL based query and try finding suggestions at the
     * current given cursor position. When suggestions are possible the
     * {@link TextFieldSuggestionBoxClientRpc#showSuggestions(org.eclipse.hawkbit.rsql.SuggestionContext)}
     * is called as a callback mechanism back to the client.
     * 
     * @param text
     *            the current entered text e.g. in a text field to retrieve
     *            suggestion for
     * @param cursor
     *            the current cursor position
     */
    void suggest(final String text, final int cursor);

    /**
     * Executes the query text to get the filtered data.
     * 
     * @param text
     *            the current entered text e.g. in a text field to retrieve
     *            suggestion for
     * @param cursor
     *            the current cursor position
     */
    void executeQuery(final String text, final int cursor);
}
