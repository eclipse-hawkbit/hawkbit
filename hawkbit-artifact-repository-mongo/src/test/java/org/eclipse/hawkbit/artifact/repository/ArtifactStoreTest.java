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
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import org.apache.commons.io.IOUtils;
import org.eclipse.hawkbit.artifact.MongoDBTestRule;
import org.eclipse.hawkbit.artifact.TestConfiguration;
import org.eclipse.hawkbit.artifact.repository.model.DbArtifact;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.io.BaseEncoding;
import com.mongodb.gridfs.GridFSDBFile;

import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Component Tests - Repository")
@Stories("Artifact Store MongoDB")
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = { ArtifactStoreAutoConfiguration.class, TestConfiguration.class })
public class ArtifactStoreTest {

    @ClassRule
    public static final MongoDBTestRule mongoDBRule = new MongoDBTestRule();

    @Autowired
    private ArtifactStore artifactStoreUnderTest;

    @Autowired
    private GridFsOperations gridFs;

    @Test
    public void storeArtifactInMongoDB() {
        final int filelengthBytes = 128;
        final String filename = "testfile.json";
        final String contentType = "application/json";
        final DbArtifact storedFile = artifactStoreUnderTest.store(generateInputStream(filelengthBytes), filename,
                contentType);

        assertThat(storedFile).isNotNull();
        assertThat(artifactStoreUnderTest.getArtifactById(storedFile.getArtifactId())).isNotNull();
    }

    @Test
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
    public void findArtifactByMD5Hash() throws NoSuchAlgorithmException {
        final int filelengthBytes = 128;
        final String filename = "testfile.json";
        final String contentType = "application/json";

        final DigestInputStream digestInputStream = digestInputStream(generateInputStream(filelengthBytes), "MD5");
        artifactStoreUnderTest.store(digestInputStream, filename, contentType);
        assertThat(artifactStoreUnderTest.getArtifactByMd5(
                BaseEncoding.base16().lowerCase().encode(digestInputStream.getMessageDigest().digest()))).isNotNull();
    }

    @Test
    public void getInputStreamFromArtifact() throws IOException {
        final int filelengthBytes = 128;
        final String filename = "testfile.json";
        final String contentType = "application/json";

        final ByteArrayInputStream inputStream = generateInputStream(filelengthBytes);
        final DbArtifact artifact = artifactStoreUnderTest
                .getArtifactById(artifactStoreUnderTest.store(inputStream, filename, contentType).getArtifactId());
        inputStream.reset();

        final byte[] artifactBytes = new byte[filelengthBytes];
        final byte[] artifactStoredBytes = new byte[filelengthBytes];
        IOUtils.readFully(inputStream, artifactBytes);
        IOUtils.readFully(artifact.getFileInputStream(), artifactStoredBytes);

        assertThat(artifactBytes).isEqualTo(artifactStoredBytes);
    }

    @Test
    public void deleteArtifactWithOnlyOneTenantLast() throws NoSuchAlgorithmException {
        final int filelengthBytes = 128;
        final String filename = "testfile.json";
        final String contentType = "application/json";

        final DigestInputStream digestInputStream = digestInputStream(generateInputStream(filelengthBytes), "SHA-1");
        final DbArtifact store = artifactStoreUnderTest.store(digestInputStream, filename, contentType);

        artifactStoreUnderTest.deleteById(store.getArtifactId());

        final GridFSDBFile findOne = gridFs
                .findOne(new Query().addCriteria(Criteria.where("_id").is(store.getArtifactId())));
        assertThat(findOne).isNull();
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
