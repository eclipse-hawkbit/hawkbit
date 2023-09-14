/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.filtermanagement.client;

import com.vaadin.shared.communication.ClientRpc;

/**
 * Client RPC for the AutocompleteTextField. The Client RPC interface is used to
 * make server to client calls in Vaadin. Only void methods are allowed in
 * ClientRpc calls.
 *
 */
@FunctionalInterface
public interface TextFieldSuggestionBoxClientRpc extends ClientRpc {

    /**
     * Notifies the client about showing the given suggestions in the suggestion
     * box.
     * 
     * @param suggestionContext
     *            the suggestion context which contains all informations about
     *            showing suggestions
     */
    void showSuggestions(final SuggestionContextDto suggestionContext);

}
