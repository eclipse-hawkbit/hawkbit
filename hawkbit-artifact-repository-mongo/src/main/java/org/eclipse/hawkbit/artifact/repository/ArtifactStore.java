/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.artifact.repository;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.artifact.repository.model.DbArtifact;
import org.eclipse.hawkbit.artifact.repository.model.DbArtifactHash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;

import com.google.common.io.BaseEncoding;
import com.google.common.io.ByteStreams;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClientException;
import com.mongodb.MongoException;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSFile;

/**
 * The file management which looks up all the file in the file tore.
 *
 */
public class ArtifactStore implements ArtifactRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArtifactStore.class);

    /**
     * The mongoDB field which holds the filename of the file to download. SP
     * Server uses the SHA hash as a filename and lookup in the mongoDB.
     */
    private static final String FILENAME = "filename";

    /**
     * The mongoDB field which automatically calculated by the mongoDB.
     */
    private static final String MD5 = "md5";

    /**
     * The mongoDB field which holds the SHA1 hash, stored in the meta data
     * object.
     */
    private static final String SHA1 = "sha1";

    private static final String ID = "_id";

    @Autowired
    private GridFsOperations gridFs;

    MongoTemplate mongoTemplate;

    /**
     * Retrieves a {@link GridFSDBFile} from the store by it's SHA1 hash.
     *
     * @param sha1Hash
     *            the sha1-hash of the file to lookup.
     * 
     * @return The DbArtifact object or {@code null} if no file exists.
     */
    @Override
    public DbArtifact getArtifactBySha1(final String sha1Hash) {
        return map(gridFs.findOne(new Query().addCriteria(Criteria.where(FILENAME).is(sha1Hash))));
    }

    /**
     * Retrieves a {@link GridFSDBFile} from the store by it's MD5 hash.
     * 
     * @param md5Hash
     *            the md5-hash of the file to lookup.
     * @return The gridfs file object or {@code null} if no file exists.
     */
    public DbArtifact getArtifactByMd5(final String md5Hash) {
        return map(gridFs.findOne(new Query().addCriteria(Criteria.where(MD5).is(md5Hash))));
    }

    /**
     * Retrieves a {@link GridFSDBFile} from the store by it's object id.
     * 
     * @param id
     *            the id of the file to lookup.
     * @return The gridfs file object or {@code null} if no file exists.
     */
    @Override
    public DbArtifact getArtifactById(final String id) {
        return map(gridFs.findOne(new Query().addCriteria(Criteria.where(ID).is(id))));
    }

    @Override
    public DbArtifact store(final InputStream content, final String filename, final String contentType) {
        return store(content, filename, contentType, null);
    }

    @Override
    public DbArtifact store(final InputStream content, final String filename, final String contentType,
            final DbArtifactHash hash) {
        File tempFile = null;
        try {
            LOGGER.debug("storing file {} of content {}", filename, contentType);
            tempFile = File.createTempFile("uploadFile", null);
            try (final BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(tempFile))) {
                try (BufferedInputStream bis = new BufferedInputStream(content)) {
                    return store(bis, contentType, bos, tempFile, hash);
                }
            }
        } catch (final IOException | MongoException e1) {
            throw new ArtifactStoreException(e1.getMessage(), e1);
        } finally {
            if (tempFile != null && !tempFile.delete()) {
                LOGGER.error("Could not delete temporary file: {}", tempFile);
            }
        }
    }

    @Override
    public void deleteById(final String artifactId) {
        try {
            deleteArtifact(gridFs.findOne(new Query().addCriteria(Criteria.where(ID).is(artifactId))));
        } catch (final MongoException e) {
            throw new ArtifactStoreException(e.getMessage(), e);
        }
    }

    @Override
    public void deleteBySha1(final String sha1Hash) {
        try {
            deleteArtifact(gridFs.findOne(new Query().addCriteria(Criteria.where(FILENAME).is(sha1Hash))));
        } catch (final MongoException e) {
            throw new ArtifactStoreException(e.getMessage(), e);
        }
    }

    private void deleteArtifact(final GridFSDBFile dbFile) {
        if (dbFile != null) {
            try {
                gridFs.delete(new Query().addCriteria(Criteria.where(ID).is(dbFile.getId())));
            } catch (final MongoClientException e) {
                throw new ArtifactStoreException(e.getMessage(), e);
            }
        }

    }

    private DbArtifact store(final InputStream content, final String contentType, final OutputStream os,
            final File tempFile, final DbArtifactHash hash) {
        final GridFsArtifact storedArtifact;
        try {
            final String sha1Hash = computeSHA1Hash(content, os, hash != null ? hash.getSha1() : null);
            // upload if it does not exist already, check if file exists, not
            // tenant specific.
            final GridFSDBFile result = gridFs.findOne(new Query().addCriteria(Criteria.where(FILENAME).is(sha1Hash)));
            if (null == result) {
                try (FileInputStream inputStream = new FileInputStream(tempFile)) {
                    final BasicDBObject metadata = new BasicDBObject();
                    metadata.put(SHA1, sha1Hash);
                    storedArtifact = map(gridFs.store(inputStream, sha1Hash, contentType, metadata));
                }
            } else {
                LOGGER.info("file with sha1 hash {} already exists in database, increase reference counter", sha1Hash);
                result.save();
                storedArtifact = map(result);
            }
        } catch (final NoSuchAlgorithmException | IOException e) {
            throw new ArtifactStoreException(e.getMessage(), e);
        }

        if (hash != null && hash.getMd5() != null
                && !storedArtifact.getHashes().getMd5().equalsIgnoreCase(hash.getMd5())) {
            throw new HashNotMatchException("The given md5 hash " + hash.getMd5()
                    + " not matching the calculated md5 hash " + storedArtifact.getHashes().getMd5(),
                    HashNotMatchException.MD5);
        }

        return storedArtifact;

    }

    private static String computeSHA1Hash(final InputStream stream, final OutputStream os, final String providedSHA1Sum)
            throws NoSuchAlgorithmException, IOException {
        String sha1Hash;
        // compute digest
        // Exception squid:S2070 - not used for hashing sensitive
        // data
        @SuppressWarnings("squid:S2070")
        final MessageDigest md = MessageDigest.getInstance("SHA-1");
        try (final DigestOutputStream dos = new DigestOutputStream(os, md)) {
            ByteStreams.copy(stream, dos);
        }
        sha1Hash = BaseEncoding.base16().lowerCase().encode(md.digest());
        if (providedSHA1Sum != null && !providedSHA1Sum.equalsIgnoreCase(sha1Hash)) {
            throw new HashNotMatchException(
                    "The given sha1 hash " + providedSHA1Sum + " not matching the calculated sha1 hash " + sha1Hash,
                    HashNotMatchException.SHA1);
        }
        return sha1Hash;
    }

    /**
     * Maps a list of {@link GridFSDBFile} to paged list of {@link DbArtifact}s.
     *
     * @param tenant
     *            the tenant
     * @param dbFiles
     *            the list of mongoDB gridFs files.
     * @return a paged list of artifacts mapped from the given dbFiles
     */
    private List<DbArtifact> map(final List<GridFSDBFile> dbFiles) {
        return dbFiles.stream().map(this::map).collect(Collectors.toList());
    }

    /**
     * Retrieves a list of {@link GridFSDBFile} from the store by all SHA1
     * hashes.
     * 
     * @param sha1Hashes
     *            the sha1-hashes of the files to lookup.
     * @return list of artifacts
     */
    @Override
    public List<DbArtifact> getArtifactsBySha1(final List<String> sha1Hashes) {
        return map(gridFs.find(new Query().addCriteria(Criteria.where(SHA1).in(sha1Hashes))));
    }

    /**
     * Retrieves a list of {@link GridFSDBFile} from the store by all ids.
     *
     * @param ids
     *            the ids of the files to lookup.
     * @return list of artfiacts
     */
    public List<DbArtifact> getArtifactsByIds(final List<String> ids) {
        return map(gridFs.find(new Query().addCriteria(Criteria.where(ID).in(ids))));
    }

    /**
     * Maps a single {@link GridFSFile} to {@link DbArtifact}.
     *
     * @param tenant
     *            the tenant
     * @param dbFile
     *            the mongoDB gridFs file.
     * @return a mapped artifact from the given dbFile
     */
    private GridFsArtifact map(final GridFSFile fsFile) {
        if (fsFile == null) {
            return null;
        }
        final GridFsArtifact artifact = new GridFsArtifact(fsFile);
        artifact.setArtifactId(fsFile.getId().toString());
        artifact.setSize(fsFile.getLength());
        artifact.setContentType(fsFile.getContentType());
        artifact.setHashes(new DbArtifactHash(fsFile.getFilename(), fsFile.getMD5()));
        return artifact;
    }
}
