/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.dmf.json.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.hawkbit.dmf.amqp.api.EventTopic;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * JSON representation of a multi-action request.
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DmfMultiActionRequest {

    private List<DmfMultiActionElement> elements;

    public DmfMultiActionRequest() {
    }

    @JsonCreator
    public DmfMultiActionRequest(final List<DmfMultiActionElement> elements) {
        this.elements = elements;
    }

    @JsonValue
    public List<DmfMultiActionElement> getElements() {
        return elements;
    }

    public void addElement(final DmfMultiActionElement element) {
        if (elements == null) {
            elements = new ArrayList<>();
        }
        elements.add(element);
    }

    public void addElement(final EventTopic topic, final DmfActionRequest action, final int weight) {
        final DmfMultiActionElement element = new DmfMultiActionElement();
        element.setTopic(topic);
        element.setAction(action);
        element.setWeight(weight);
        addElement(element);
    }

    /**
     * Represents an element within a {@link DmfMultiActionRequest}.
     */
    public static class DmfMultiActionElement {

        @JsonProperty
        private EventTopic topic;

        @JsonProperty
        private DmfActionRequest action;

        @JsonProperty
        private int weight;

        public DmfActionRequest getAction() {
            return action;
        }

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "topic", defaultImpl = DmfActionRequest.class)
        @JsonSubTypes({ @Type(value = DmfDownloadAndUpdateRequest.class, name = "DOWNLOAD"),
                @Type(value = DmfDownloadAndUpdateRequest.class, name = "DOWNLOAD_AND_INSTALL") })
        public void setAction(final DmfActionRequest action) {
            this.action = action;
        }

        public EventTopic getTopic() {
            return topic;
        }

        public void setTopic(final EventTopic actionType) {
            this.topic = actionType;
        }

        public void setWeight(final int weight) {
            this.weight = weight;
        }

        public int getWeight() {
            return weight;
        }
    }

}
