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
import org.eclipse.hawkbit.dmf.amqp.api.EventTopic;

/**
 * JSON representation of a multi-action request.
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DmfMultiActionRequest {

    private final List<DmfMultiActionElement> elements;

    @JsonCreator
    public DmfMultiActionRequest(final List<DmfMultiActionElement> elements) {
        this.elements = elements;
    }

    @JsonValue
    public List<DmfMultiActionElement> getElements() {
        return elements;
    }

    /**
     * Represents an element within a {@link DmfMultiActionRequest}.
     */
    @Data
    public static class DmfMultiActionElement {

        private final EventTopic topic;
        private final DmfActionRequest action;
        private final int weight;

        @JsonCreator
        public DmfMultiActionElement(
                @JsonProperty("topic") final EventTopic topic,
                @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "topic", defaultImpl = DmfActionRequest.class)
                @JsonSubTypes({
                        @Type(value = DmfDownloadAndUpdateRequest.class, name = "DOWNLOAD"),
                        @Type(value = DmfDownloadAndUpdateRequest.class, name = "DOWNLOAD_AND_INSTALL") })
                @JsonProperty("action") final DmfActionRequest action,
                @JsonProperty("weight") final int weight) {
            this.topic = topic;
            this.action = action;
            this.weight = weight;
        }
    }
}
