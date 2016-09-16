/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ddi.rest.resource.util;

import org.springframework.hateoas.MediaTypes;

/**
 * Constant class for MediaType HAL with encoding UTF-8. Necessary since Spring
 * version 4.3.2
 *
 */
public final class MediaType {

    public static final String APPLICATION_JSON_HAL_UTF = MediaTypes.HAL_JSON + ";charset=UTF-8";

    private MediaType() {

    }
}
