/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.artifact.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import org.eclipse.hawkbit.artifact.TestConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.io.BaseEncoding;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Component Tests - Repository")
@Stories("Artifact Store MongoDB")
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = { MongoDBArtifactStoreAutoConfiguration.class, TestConfiguration.class }, properties = {
        "spring.data.mongodb.port=0", "spring.mongodb.embedded.version=3.4.4",
        "logging.level.org.springframework.data.mongodb=DEBUG" }, webEnvironment = WebEnvironment.NONE)
public class MongoDBArtifactStoreTest {
    private static final String TENANT = "test_tenant";

    @Autowired
    private MongoDBArtifactStore artifactStoreUnderTest;

    @Test
    @Description("Ensures that search by SHA1 hash (which is used by hawkBit as artifact ID) finds the expected results.")
    public void findArtifactBySHA1Hash() throws NoSuchAlgorithmException {
        final int filelengthBytes = 128;
        final String filename = "testfile.json";
        final String contentType = "application/json";

        storeAndVerify(TENANT, filelengthBytes, filename, contentType);
    }

    @Step
    private String storeAndVerify(final String tenant, final int filelengthBytes, final String filename,
            final String contentType) throws NoSuchAlgorithmException {

        final DigestInputStream digestInputStream = digestInputStream(generateInputStream(filelengthBytes), "SHA-1");
        artifactStoreUnderTest.store(tenant, digestInputStream, filename, contentType);

        final String sha1 = BaseEncoding.base16().lowerCase().encode(digestInputStream.getMessageDigest().digest());
        assertThat(artifactStoreUnderTest.getArtifactBySha1(tenant, sha1)).isNotNull();
        return sha1;
    }

    @Test
    @Description("Deltes file from repoeitory identified ny SHA1 hash as filename.")
    public void deleteArtifactBySHA1Hash() throws NoSuchAlgorithmException {
        final int filelengthBytes = 128;
        final String filename = "testfile.json";
        final String contentType = "application/json";

        final String sha1 = storeAndVerify(TENANT, filelengthBytes, filename, contentType);

        artifactStoreUnderTest.deleteBySha1(TENANT, sha1);
        assertThat(artifactStoreUnderTest.getArtifactBySha1(TENANT, sha1)).isNull();
    }

    @Test
    @Description("Verfies that all data of a tenant is erased if repositoty is asked to do so. "
            + "Data of other tenants is not affected.")
    public void deleteTenant() throws NoSuchAlgorithmException {
        final int filelengthBytes = 128;
        final String filename = "testfile.json";
        final String contentType = "application/json";

        final String shaDeleted = storeAndVerify(TENANT, filelengthBytes, filename, contentType);
        final String shaUndeleted = storeAndVerify("another_tenant", filelengthBytes, filename, contentType);

        artifactStoreUnderTest.deleteTenant(TENANT);
        assertThat(artifactStoreUnderTest.getArtifactBySha1(TENANT, shaDeleted)).isNull();
        assertThat(artifactStoreUnderTest.getArtifactBySha1("another_tenant", shaUndeleted)).isNotNull();
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
