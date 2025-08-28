/**
 * Copyright (c) 2025 Bosch Digital GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.repository.artifact.model;

/**
 * Database representation of artifact hash.
 */
public record DbArtifactHash(String sha1, String md5, String sha256) {}