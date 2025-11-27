/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.event.remote;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@Getter
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class AbstractRemoteEvent extends ApplicationEvent {

    private final String id;

    // for serialization libs like jackson
    protected AbstractRemoteEvent() {
        this("_empty_default_");
    }

    protected AbstractRemoteEvent(Object source) {
        super(source);
        this.id = UUID.randomUUID().toString();
    }
}