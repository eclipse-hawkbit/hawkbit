/**
 * Copyright (c) 2011-2015 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.rest.resource;

/**
 * 
 * @author Dennis Melzer
 *
 * @param <T>
 */
public interface SuccessCondition<T> {

    /**
     * 
     * @param result
     * @return
     */
    boolean success(final T result);

}
