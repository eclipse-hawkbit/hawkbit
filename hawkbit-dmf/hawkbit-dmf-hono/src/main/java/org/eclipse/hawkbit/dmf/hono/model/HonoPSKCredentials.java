package org.eclipse.hawkbit.dmf.hono.model;

import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.Collection;

@JsonTypeName("psk")
public class HonoPSKCredentials extends HonoCredentials {
    public void setSecrets(Collection<Secret> secrets) {
        this.secrets = secrets;
    }

    public static class Secret extends HonoSecret {
        private String key;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        @Override
        public boolean matches(final String key) {
            return key != null && key.equals(this.key);
        }
    }
}
