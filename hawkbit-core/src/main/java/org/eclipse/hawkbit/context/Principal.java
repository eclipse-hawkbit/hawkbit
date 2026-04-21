/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.context;

import java.io.Serial;
import java.io.Serializable;

import lombok.Value;
import lombok.experimental.NonFinal;

/**
 * Represent an actor in the scope of the tenant
 */
@Value
@NonFinal
public class Principal implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    String tenant;
    String actor;
}