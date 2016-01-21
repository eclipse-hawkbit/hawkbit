/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

/**
 * An interface for declaring the name of the field described in the database
 * which is used as string representation of the field, e.g. for sorting the
 * fields over REST.
 *
 */
@FunctionalInterface
public interface FieldNameProvider {

    /**
     * @return the string representation of the underlying persistence field
     *         name e.g. in case of sorting. Never {@code null}.
     */
    String getFieldName();
}
