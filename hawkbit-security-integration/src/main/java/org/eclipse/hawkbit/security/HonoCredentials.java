/**
 * Copyright (c) 2019 Kiwigrid GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.MessageDigestPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Collection;

public class HonoCredentials {
    private String deviceId;
    private String type;
    private String authId;
    private boolean enabled;
    private Collection<? extends HonoSecret> secrets;

    HonoCredentials(String deviceId, String type, String authId, boolean enabled, Collection<? extends HonoSecret> secrets) {
        this.deviceId = deviceId;
        this.type = type;
        this.authId = authId;
        this.enabled = enabled;
        this.secrets = secrets;
    }

    public boolean matches(final String providedSecret) {
        if (enabled) {
            for (HonoSecret secret : secrets) {
                if (secret.isValid() && secret.matches(providedSecret)) {
                    return true;
                }
            }
        }

        return false;
    }
}

abstract class HonoSecret {
    private LocalDateTime notBefore;
    private LocalDateTime notAfter;

    HonoSecret(String notBefore, String notAfter) {
        if (notBefore != null) {
            this.notBefore = LocalDateTime.parse(notBefore);
        }
        if (notAfter != null) {
            this.notAfter = LocalDateTime.parse(notAfter);
        }
    }

    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return (notBefore == null || now.compareTo(notBefore) >= 0) && (notAfter == null || now.compareTo(notAfter) <= 0);
    }

    public abstract boolean matches(final String password);
}

class HonoPasswordSecret extends HonoSecret {
    private String hashFunction;
    private String salt;
    private String pwdHash;

    HonoPasswordSecret(String notBefore, String notAfter, String hashFunction, String salt, String pwdHash) {
        super(notBefore, notAfter);

        this.hashFunction = hashFunction;
        this.salt = salt;
        this.pwdHash = pwdHash;
    }

    @Override
    public boolean matches(final String password) {
        PasswordEncoder encoder;
        if (hashFunction.equals("bcrypt")) {
            encoder = new BCryptPasswordEncoder();
        }
        else if(hashFunction.equals("sha-256")) {
            encoder = new MessageDigestPasswordEncoder("SHA-256");
        }
        else if(hashFunction.equals("sha-512")) {
            encoder = new MessageDigestPasswordEncoder("SHA-512");
        }
        else {
            return false;
        }

        return encoder.matches(password + (salt != null ? salt : ""), pwdHash);
    }
}

class HonoPreSharedKey extends HonoSecret {
    private String key;

    HonoPreSharedKey(String notBefore, String notAfter, String key) {
        super(notBefore, notAfter);

        this.key = key;
    }

    @Override
    public boolean matches(final String key) {
        return this.key.equals(key);
    }
}

class HonoX509Certificate extends HonoSecret {

    HonoX509Certificate(String notBefore, String notAfter) {
        super(notBefore, notAfter);
    }

    @Override
    public boolean matches(String password) {
        // TODO: implement!
        return false;
    }
}
