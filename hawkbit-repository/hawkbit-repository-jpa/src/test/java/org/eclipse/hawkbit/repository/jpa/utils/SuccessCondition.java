/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.utils;

/**
 * SuccessCondition Interface.
 *
 * @param <T>
 *            type of the value to get verified
 */
public interface SuccessCondition<T> {

    /**
     * The implementation of the success condition.
     * 
     * @param result
     * @return
     */
    boolean success(final T result);

}
