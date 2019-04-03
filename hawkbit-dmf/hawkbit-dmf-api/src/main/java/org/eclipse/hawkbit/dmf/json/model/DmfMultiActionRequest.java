/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.dmf.json.model;

import java.util.Arrays;
import java.util.List;

import org.eclipse.hawkbit.dmf.amqp.api.EventTopic;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * JSON representation of a multi-action request.
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DmfMultiActionRequest {

    private List<DmfMultiActionElement> elements;

    @JsonValue
    public List<DmfMultiActionElement> getElements() {
        return elements;
    }

    public void addElement(final DmfMultiActionElement element) {
        if (elements != null) {
            elements.add(element);
        } else {
            elements = Arrays.asList(element);
        }
    }

    public void addElement(final EventTopic actionType, final DmfActionRequest action) {
        final DmfMultiActionElement element = new DmfMultiActionElement();
        element.setActionType(actionType);
        element.setAction(action);
        addElement(element);
    }

    public static class DmfMultiActionElement {

        @JsonProperty
        private EventTopic actionType;

        @JsonProperty
        private DmfActionRequest action;

        public DmfActionRequest getAction() {
            return action;
        }

        public void setAction(final DmfActionRequest action) {
            this.action = action;
        }

        public EventTopic getActionType() {
            return actionType;
        }

        public void setActionType(final EventTopic actionType) {
            this.actionType = actionType;
        }

    }

}
