/**
 * Copyright (c) 2019 Kiwigrid GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.dmf.hono.model;

import java.time.LocalDateTime;

public abstract class HonoSecret {
    private String id;
    private boolean enabled = true;
    private LocalDateTime notBefore;
    private LocalDateTime notAfter;

    public String getId() {
        return id;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public LocalDateTime getNotBefore() {
        return notBefore;
    }

    public LocalDateTime getNotAfter() {
        return notAfter;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setNotBefore(LocalDateTime notBefore) {
        this.notBefore = notBefore;
    }

    public void setNotBefore(String notBefore) {
        this.notBefore = LocalDateTime.parse(notBefore);
    }

    public void setNotAfter(LocalDateTime notAfter) {
        this.notAfter = notAfter;
    }

    public void setNotAfter(String notAfter) {
        this.notAfter = LocalDateTime.parse(notAfter);
    }

    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return (notBefore == null || now.compareTo(notBefore) >= 0) && (notAfter == null || now.compareTo(notAfter) <= 0);
    }

    public abstract boolean matches(final String password);
}
