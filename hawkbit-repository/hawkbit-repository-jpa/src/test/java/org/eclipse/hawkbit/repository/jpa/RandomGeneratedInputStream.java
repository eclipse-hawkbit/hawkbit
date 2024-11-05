/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.Random;

/**
 *
 */
public class RandomGeneratedInputStream extends InputStream {

    private final Random random = new SecureRandom();

    /** Target size of the stream. */
    private final long size;

    /** Internal counter. */
    private long index;

    /**
     * @param size target size of the stream [byte]
     */
    public RandomGeneratedInputStream(final long size) {
        this.size = size;
    }

    @Override
    public int read() throws IOException {
        if (index == size) {
            return -1;
        }

        index++;

        return random.nextInt(255);
    }

}
