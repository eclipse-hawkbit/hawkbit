/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.sdk;

import java.io.IOException;
import java.io.OutputStream;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Objects;

import lombok.Data;

@Data
public class Certificate {

    private final KeyPair keyPair;
    private final X509Certificate[] certificateChain;

    // create holder for the certificate - key pair and the certificate chain
    public Certificate(final KeyPair keyPair, final X509Certificate[] certificateChain) {
        Objects.requireNonNull(keyPair, "keyPair must not be null");
        Objects.requireNonNull(certificateChain, "certificateChain must not be null");
        if (certificateChain.length == 0) {
            throw new IllegalArgumentException("certificateChain must not be empty");
        }
        this.keyPair = keyPair;
        this.certificateChain = certificateChain;
    }

    public KeyStore toKeyStore(final String pass) throws KeyStoreException {
        try {
            final KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(null, null); // init
            keyStore.setKeyEntry("alias", keyPair.getPrivate(), pass.toCharArray(), certificateChain);
            return keyStore;
        } catch (final NoSuchAlgorithmException | CertificateException | IOException e) {
            throw new KeyStoreException(e);
        }
    }

    public void writeToOS(final String pass, final OutputStream os) throws KeyStoreException {
        try {
            toKeyStore(pass).store(os, pass.toCharArray());
        } catch (final NoSuchAlgorithmException | CertificateException | IOException e) {
            throw new KeyStoreException(e);
        }
    }

    public StringBuilder printPem(final StringBuilder sb) throws CertificateException {
        for (final X509Certificate certificate : certificateChain) {
            sb.append("-----BEGIN CERTIFICATE-----\n");
            sb.append(toPem(certificate.getEncoded()));
            sb.append("-----END CERTIFICATE-----\n");
        }
        sb.append('\n');

        sb.append("-----BEGIN PRIVATE KEY-----\n");
        sb.append(toPem(keyPair.getPrivate().getEncoded()));
        sb.append("-----END PRIVATE KEY-----\n");

        return sb;
    }

    private static String toPem(final byte[] ba) {
        final String base64 = Base64.getEncoder().encodeToString(ba);
        final StringBuilder formatted = new StringBuilder();
        for (int off = 0, len = base64.length(); off < len; off += 64) {
            formatted.append(base64, off, Math.min(off + 64, len)).append("\n");
        }
        return formatted.toString();
    }
}