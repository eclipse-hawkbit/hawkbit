/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.sdk.ca;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Objects;

import javax.security.auth.x500.X500Principal;

import lombok.Data;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.eclipse.hawkbit.sdk.Certificate;

@Data
public class CA {

    public static final String DEFAULT_CA_DN = "CN=CA, O=hawkBit, L=Sofia, C=BG";
    public static final long DEFAULT_NOT_BEFORE_DAYS_OFFSET = 1;
    public static final long DEFAULT_NOT_AFTER_DAYS_OFFSET = 30;

    private static final String SHA_256_WITH_RSA_ENCRYPTION = "SHA256WithRSAEncryption";

    private final Certificate certificate;
    private long nextSerial = System.currentTimeMillis();

    // creates a self-signed CA with defaults
    public CA() throws CertificateException {
        this(null, null, null);
    }

    // creates a self-signed CA
    public CA(final String caDN, final Date notBefore, final Date notAfter) throws CertificateException {
        this(selfSign(caDN, notBefore, notAfter));
    }

    // create a CA with a key and certificate chain
    public CA(final Certificate certificate) {
        this(certificate, 0);
    }

    // create a CA with a key and certificate chain
    public CA(final Certificate certificate, final long nextSerial) {
        this.certificate = certificate;
        this.nextSerial = nextSerial;
    }

    // generate key and issue a certificate with defaults
    public Certificate issue(final String subject) throws CertificateException {
        return issue(subject, null, null);
    }

    // generate key and issue a certificate
    public Certificate issue(final String subject, final Date notBefore, final Date notAfter) throws CertificateException {
        Objects.requireNonNull(subject);
        try {
            final KeyPair keyPair = genKey();
            final X509Certificate[] certificateChain = certificate.getCertificateChain();
            final ContentSigner signer = new JcaContentSignerBuilder(SHA_256_WITH_RSA_ENCRYPTION).build(certificate.getKeyPair().getPrivate());
            final X509v3CertificateBuilder caCertBuilder = new JcaX509v3CertificateBuilder(
                    certificateChain[0].getSubjectX500Principal(),
                    BigInteger.valueOf(nextSerial++), notBefore(notBefore), notAfter(notAfter), new X500Principal(subject),
                    keyPair.getPublic());
            final X509Certificate[] subjectCertificateChain = new X509Certificate[certificateChain.length + 1];
            certificateChain[0] = new JcaX509CertificateConverter().getCertificate(caCertBuilder.build(signer));
            System.arraycopy(certificateChain, 0, subjectCertificateChain, 1, certificateChain.length);
            return new Certificate(keyPair, subjectCertificateChain);
        } catch (final NoSuchAlgorithmException | OperatorCreationException e) {
            throw new CertificateException(e);
        }
    }

    public String getFingerprint() {
        try {
            final X509Certificate[] certificateChain = certificate.getCertificateChain();
            return toHex(MessageDigest.getInstance("SHA-256").digest(certificateChain[certificateChain.length - 1].getEncoded()));
        } catch (final NoSuchAlgorithmException | CertificateEncodingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static String toHex(final byte[] bytes) {
        final StringBuilder sb = new StringBuilder();
        for (final byte b : bytes) {
            sb.append(String.format("%02x", b)).append(':');
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    private static Certificate selfSign(final String caDN, final Date notBefore, final Date notAfter) throws CertificateException {
        try {
            final KeyPair keyPair = genKey();
            final X500Principal caPrincipal = new X500Principal(caDN == null ? DEFAULT_CA_DN : caDN);
            final ContentSigner selfSigner = new JcaContentSignerBuilder(SHA_256_WITH_RSA_ENCRYPTION).build(keyPair.getPrivate());
            final X509v3CertificateBuilder caCertBuilder = new JcaX509v3CertificateBuilder(
                    caPrincipal, BigInteger.valueOf(0L), notBefore(notBefore), notAfter(notAfter), caPrincipal, keyPair.getPublic());
            return new Certificate(keyPair, new X509Certificate[] { new JcaX509CertificateConverter().getCertificate(caCertBuilder.build(selfSigner)) });
        } catch (final NoSuchAlgorithmException | OperatorCreationException e) {
            throw new CertificateException(e);
        }
    }

    private static KeyPair genKey() throws NoSuchAlgorithmException {
        final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        return keyPairGenerator.generateKeyPair();
    }

    private static Date notBefore(final Date notBefore) {
        return notBefore == null ? new Date(System.currentTimeMillis() - DEFAULT_NOT_BEFORE_DAYS_OFFSET * 24 * 3600_000L) : notBefore;
    }

    private static Date notAfter(final Date notAfter) {
        return notAfter == null ? new Date(System.currentTimeMillis() + DEFAULT_NOT_AFTER_DAYS_OFFSET * 24 * 3600_000L) : notAfter;
    }
}