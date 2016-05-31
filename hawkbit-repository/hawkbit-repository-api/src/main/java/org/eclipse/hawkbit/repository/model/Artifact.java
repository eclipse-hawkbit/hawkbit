/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import com.google.common.io.BaseEncoding;

/**
 * Binaries for a {@link SoftwareModule} Note: the decision which artifacts have
 * to be downloaded are done on the device side. e.g. Full Package, Signatures,
 * binary deltas
 *
 */
public interface Artifact extends TenantAwareBaseEntity {

    /**
     * @return {@link SoftwareModule} this {@link Artifact} belongs to.
     */
    SoftwareModule getSoftwareModule();

    /**
     * @return MD5 hash of the artifact.
     */
    String getMd5Hash();

    /**
     * @return SHA-1 hash of the artifact in {@link BaseEncoding#base16()}
     *         format.
     */
    String getSha1Hash();

    /**
     * @return size of the artifact in bytes.
     */
    Long getSize();

}
