/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.rsql;

/**
 * Implementations map a placeholder to the associated value.
 * <p>
 * This is used in context of string replacement.
 */
@FunctionalInterface
public interface VirtualPropertyReplacer {

    /**
     * Looks up a placeholders and replaces them
     *
     * @param input
     *            the input string in which virtual properties should be
     *            replaced
     * @return the result of the replacement
     */
    String replace(String input);
}
