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
        "spring.data.mongodb.port=0", "spring.mongodb.embedded.version=3.4.4" })
public class MongoDBArtifactStoreTest {
    private static final String TENANT = "test_tenant";
    private static final String TENANT2 = "test_tenant2";

    @Autowired
    private MongoDBArtifactStore artifactStoreUnderTest;

    @Test
    @Description("Ensures that search by SHA1 hash (which is used by hawkBit as artifact ID) finds the expected results.")
    public void findArtifactBySHA1Hash() throws NoSuchAlgorithmException {

        final String sha1 = storeRandomArifactAndVerify(TENANT);
        final String sha2 = storeRandomArifactAndVerify(TENANT2);

        assertThat(artifactStoreUnderTest.getArtifactBySha1(TENANT2, sha1)).isNull();
        assertThat(artifactStoreUnderTest.getArtifactBySha1(TENANT, sha2)).isNull();
    }

    @Step
    private String storeRandomArifactAndVerify(final String tenant) throws NoSuchAlgorithmException {
        final int filelengthBytes = 128;
        final String filename = "testfile.json";
        final String contentType = "application/json";

        final DigestInputStream digestInputStream = digestInputStream(generateInputStream(filelengthBytes), "SHA-1");
        artifactStoreUnderTest.store(tenant, digestInputStream, filename, contentType);

        final String sha1 = BaseEncoding.base16().lowerCase().encode(digestInputStream.getMessageDigest().digest());
        assertThat(artifactStoreUnderTest.getArtifactBySha1(tenant, sha1)).isNotNull();
        return sha1;
    }

    @Test
    @Description("Deletes file from repository identified by SHA1 hash as filename.")
    public void deleteArtifactBySHA1Hash() throws NoSuchAlgorithmException {

        final String sha1 = storeRandomArifactAndVerify(TENANT);

        artifactStoreUnderTest.deleteBySha1(TENANT, sha1);
        assertThat(artifactStoreUnderTest.getArtifactBySha1(TENANT, sha1)).isNull();
    }

    @Test
    @Description("Verfies that all data of a tenant is erased if repository is asked to do so. "
            + "Data of other tenants is not affected.")
    public void deleteTenant() throws NoSuchAlgorithmException {

        final String shaDeleted = storeRandomArifactAndVerify(TENANT);
        final String shaUndeleted = storeRandomArifactAndVerify("another_tenant");

        artifactStoreUnderTest.deleteByTenant("tenant_that_does_not_exist");
        artifactStoreUnderTest.deleteByTenant(TENANT);
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
