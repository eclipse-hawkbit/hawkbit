package org.eclipse.hawkbit.dmf.hono.model;

import java.util.Collection;

public abstract class HonoCredentials {
    private String authId;
    private String type;
    private boolean enabled = true;
    Collection<? extends HonoSecret> secrets;

    public String getType() {
        return type;
    }

    public String getAuthId() {
        return authId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Collection<? extends HonoSecret> getSecrets() {
        return secrets;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setAuthId(String authId) {
        this.authId = authId;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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
