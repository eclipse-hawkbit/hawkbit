/**
 * Copyright (c) 2023 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.security;

import org.springframework.security.core.context.SecurityContext;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;
import java.util.Objects;

public interface SecurityContextSerializer {

    /**
     * Return security context as string (could be just a reference)
     *
     * @param securityContext the security context
     * @return the securityContext as string
     */
    String serialize(SecurityContext securityContext);

    /**
     * Deserialize security context
     *
     * @param securityContextString string representing the security context
     * @return deserialized security context
     */
    SecurityContext deserialize(String securityContextString);

    class JavaSerialization implements SecurityContextSerializer {

        @Override
        public String serialize(final SecurityContext securityContext) {
            Objects.requireNonNull(securityContext);
            try (final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                 final ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                oos.writeObject(securityContext);
                oos.flush();
                return Base64.getEncoder().encodeToString(baos.toByteArray());
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public SecurityContext deserialize(String securityContextString) {
            Objects.requireNonNull(securityContextString);
            try (final ByteArrayInputStream bais = new ByteArrayInputStream(Base64.getDecoder().decode(securityContextString));
                 final ObjectInputStream ois = new ObjectInputStream(bais)) {
                return (SecurityContext) ois.readObject();
            } catch (final IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Empty implementation. Could be used if the serialization shall be skipped.
     */
    class Nop implements SecurityContextSerializer {

        @Override
        public String serialize(final SecurityContext securityContext) {
            return null;
        }

        @Override
        public SecurityContext deserialize(String securityContextString) {
            throw new UnsupportedOperationException();
        }
    }
}
