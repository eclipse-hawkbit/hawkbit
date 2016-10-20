/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.artifact.repository;

import static org.fest.assertions.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import org.eclipse.hawkbit.artifact.TestConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.io.BaseEncoding;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Component Tests - Repository")
@Stories("Artifact Store MongoDB")
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = { ArtifactStoreAutoConfiguration.class, TestConfiguration.class })
@TestPropertySource(properties = { "spring.data.mongodb.port=0", "spring.mongodb.embedded.version=3.2.7" })
public class ArtifactStoreTest {

    @Autowired
    private ArtifactStore artifactStoreUnderTest;

    @Autowired
    private GridFsOperations gridFs;

    @Test
    @Description("Ensures that search by SHA1 hash (which is used by hawkBit as artifact ID) finds the expected results.")
    public void findArtifactBySHA1Hash() throws NoSuchAlgorithmException {
        final int filelengthBytes = 128;
        final String filename = "testfile.json";
        final String contentType = "application/json";

        final DigestInputStream digestInputStream = digestInputStream(generateInputStream(filelengthBytes), "SHA-1");
        artifactStoreUnderTest.store(digestInputStream, filename, contentType);
        assertThat(artifactStoreUnderTest.getArtifactBySha1(
                BaseEncoding.base16().lowerCase().encode(digestInputStream.getMessageDigest().digest()))).isNotNull();
    }

    @Test
    @Description("Ensures that search by MD5 hash finds the expected results.")
    public void findArtifactByMD5Hash() throws NoSuchAlgorithmException {
        final int filelengthBytes = 128;
        final String filename = "testfile.json";
        final String contentType = "application/json";

        final DigestInputStream digestInputStream = digestInputStream(generateInputStream(filelengthBytes), "MD5");
        artifactStoreUnderTest.store(digestInputStream, filename, contentType);
        assertThat(artifactStoreUnderTest.getArtifactByMd5(
                BaseEncoding.base16().lowerCase().encode(digestInputStream.getMessageDigest().digest()))).isNotNull();
    }

    private static ByteArrayInputStream generateInputStream(final int length) {
        final byte[] bytes = new byte[length];
        new Random().nextBytes(bytes);
        return new ByteArrayInputStream(bytes);
    }

    private static DigestInputStream digestInputStream(final ByteArrayInputStream stream, final String digest)
            throws NoSuchAlgorithmException {
        return new DigestInputStream(stream, MessageDigest.getInstance(digest));
    }

}
