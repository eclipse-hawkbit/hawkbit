/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
