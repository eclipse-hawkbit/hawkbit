/**
 * Copyright (c) 2011-2015 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.repository.utils;

/**
 * SuccessCondition Interface.
 * 
 * @author Dennis Melzer
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
