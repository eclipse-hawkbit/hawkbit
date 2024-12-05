/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.dmf.json.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.dmf.amqp.api.EventTopic;

/**
 * JSON representation of a multi-action request.
 */
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DmfMultiActionRequest {

    private List<DmfMultiActionElement> elements;

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
    @Data
    public static class DmfMultiActionElement {

        @JsonProperty
        private EventTopic topic;

        @JsonProperty
        private DmfActionRequest action;

        @JsonProperty
        private int weight;

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "topic", defaultImpl = DmfActionRequest.class)
        @JsonSubTypes({ @Type(value = DmfDownloadAndUpdateRequest.class, name = "DOWNLOAD"),
                @Type(value = DmfDownloadAndUpdateRequest.class, name = "DOWNLOAD_AND_INSTALL") })
        public void setAction(final DmfActionRequest action) {
            this.action = action;
        }
    }
}
