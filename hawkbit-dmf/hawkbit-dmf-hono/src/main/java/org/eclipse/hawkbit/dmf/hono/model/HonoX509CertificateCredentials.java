package org.eclipse.hawkbit.dmf.hono.model;

import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.Collection;

@JsonTypeName("x509-cert")
public class HonoX509CertificateCredentials extends HonoCredentials {
    public void setSecrets(Collection<Secret> secrets) {
        this.secrets = secrets;
    }

    public static class Secret extends HonoSecret {
        @Override
        public boolean matches(String password) {
            return false;
        }
    }
}
