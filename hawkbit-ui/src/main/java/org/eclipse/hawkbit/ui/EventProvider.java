/**
 * Copyright (c) 2011-2016 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.ui;

import java.util.Set;

/**
 *
 */
public interface EventProvider {

    Set<Class<?>> getSingleEvents();

    Set<Class<?>> getBulkEvents();
}
