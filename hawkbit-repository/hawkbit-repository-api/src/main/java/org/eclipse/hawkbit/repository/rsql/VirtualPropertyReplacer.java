/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.rsql;

import java.io.Serializable;

/**
 * Implementations map a placeholder to the associated value.
 * <p>
 * This is used in context of string replacement.
 *
 * @deprecated Since 0.10.0, used only in deprecated specification builders
 */
@Deprecated(since = "0.10.0", forRemoval = true)
@FunctionalInterface
public interface VirtualPropertyReplacer extends Serializable {

    /**
     * Looks up a placeholders and replaces them
     *
     * @param input the input string in which virtual properties should be replaced
     * @return the result of the replacement
     */
    String replace(String input);
}