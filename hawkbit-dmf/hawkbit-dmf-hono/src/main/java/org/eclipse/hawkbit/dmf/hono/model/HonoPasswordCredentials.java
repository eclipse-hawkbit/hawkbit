package org.eclipse.hawkbit.dmf.hono.model;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.MessageDigestPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collection;

@JsonTypeName("hashed-password")
public class HonoPasswordCredentials extends HonoCredentials {
    public void setSecrets(Collection<Secret> secrets) {
        this.secrets = secrets;
    }

    public static class Secret extends HonoSecret {
        private String hashFunction;
        private String salt;
        private String pwdHash;

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

        public String getHashFunction() {
            return hashFunction;
        }

        public String getSalt() {
            return salt;
        }

        public String getPwdHash() {
            return pwdHash;
        }

        public void setHashFunction(String hashFunction) {
            this.hashFunction = hashFunction;
        }

        public void setSalt(String salt) {
            this.salt = salt;
        }

        public void setPwdHash(String pwdHash) {
            this.pwdHash = pwdHash;
        }
    }
}
