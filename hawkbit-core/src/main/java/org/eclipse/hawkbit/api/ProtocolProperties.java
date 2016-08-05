/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.api;

/**
 * Interface for declaring common properties through all supported protocols
 * pattern.
 */
public interface ProtocolProperties {
    /**
     * @return the hostname value to resolve in the pattern.
     */
    String getHostname();

    /**
     * @return the IP address value to resolve in the pattern.
     */
    String getIp();

    /**
     * @return the port value to resolve in the pattern.
     */
    String getPort();

    /**
     * @return the pattern to build the URL.
     */
    String getPattern();

    /**
     * @return <code>true</code> if the {@link ProtocolProperties} is enabled.
     */
    boolean isEnabled();
}
