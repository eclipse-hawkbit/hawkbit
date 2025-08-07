/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.security.SecurityTokenGenerator;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A singleton bean which holds the {@link SecurityTokenGenerator} and make it
 * accessible to beans which are not managed by spring, e.g. JPA entities.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("java:S6548") // java:S6548 - singleton holder ensures static access to spring resources in some places
public final class SecurityTokenGeneratorHolder {

    private static final SecurityTokenGeneratorHolder SINGLETON = new SecurityTokenGeneratorHolder();

    private SecurityTokenGenerator securityTokenGenerator;

    /**
     * @return a singleton instance of the security token generator holder.
     */
    public static SecurityTokenGeneratorHolder getInstance() {
        return SINGLETON;
    }

    @Autowired // spring setter injection
    public void setSecurityTokenGenerator(final SecurityTokenGenerator securityTokenGenerator) {
        this.securityTokenGenerator = securityTokenGenerator;
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