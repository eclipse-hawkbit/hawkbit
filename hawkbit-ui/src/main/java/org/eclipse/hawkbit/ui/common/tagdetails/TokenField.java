/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.tagdetails;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.hawkbit.ui.common.tagdetails.TagPanel.TagAssignmentListener;

import com.google.common.collect.Lists;
import com.vaadin.ui.Button;
import com.vaadin.ui.CssLayout;

public class TokenField extends CssLayout {

    private static final long serialVersionUID = 1L;

    private final transient Map<String, Button> tokens = new ConcurrentHashMap<>();
    private final transient List<TagAssignmentListener> listeners = Lists.newCopyOnWriteArrayList();

    public void addToken(final String tagName, final String tagColor) {
        if (!tokens.containsKey(tagName)) {
            final Button token = new Button(tagName);
            tokens.put(tagName, token);
            token.addClickListener(e -> removeTagAssignment(e.getButton().getCaption()));
            addComponent(token, getComponentCount());
        }
    }

    public void updateToken(final String tagName, final String tagColor) {
        final Button button = tokens.get(tagName);

        // set color
    }

    private void removeTagAssignment(final String tagName) {
        removeToken(tagName);
        notifyListenersTagAssignmentRemoved(tagName);
    }

    public void removeToken(final String tokenName) {
        final Button token = tokens.get(tokenName);
        if (token != null) {
            tokens.remove(tokenName);
            removeComponent(token);
        }
    }

    public void removeAllTokens() {
        removeAllComponents();
        tokens.clear();
    }

    public void addTagAssignmentListener(final TagAssignmentListener listener) {
        listeners.add(listener);
    }

    public void removeTagAssignmentListener(final TagAssignmentListener listener) {
        listeners.remove(listener);
    }

    private void notifyListenersTagAssignmentRemoved(final String tagName) {
        listeners.forEach(listener -> listener.unassignTag(tagName));
    }
}
