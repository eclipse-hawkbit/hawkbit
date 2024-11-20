/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.model.helper;

import org.eclipse.hawkbit.security.SecurityTokenGenerator;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A singleton bean which holds the {@link SecurityTokenGenerator} and make it
 * accessible to beans which are not managed by spring, e.g. JPA entities.
 */
public final class SecurityTokenGeneratorHolder {

    private static final SecurityTokenGeneratorHolder INSTANCE = new SecurityTokenGeneratorHolder();

    @Autowired
    private SecurityTokenGenerator securityTokenGenerator;

    /**
     * private constructor.
     */
    private SecurityTokenGeneratorHolder() {

    }

    /**
     * @return a singleton instance of the security token generator holder.
     */
    public static SecurityTokenGeneratorHolder getInstance() {
        return INSTANCE;
    }

    /**
     * delegates to {@link SecurityTokenGenerator#generateToken()}.
     *
     * @return the result {@link SecurityTokenGenerator#generateToken()}
     */
    public String generateToken() {
        return securityTokenGenerator.generateToken();
    }

}
