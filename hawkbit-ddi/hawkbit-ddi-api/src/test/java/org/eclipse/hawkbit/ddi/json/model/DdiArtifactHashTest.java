/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.hawkbit.ddi.json.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.junit.jupiter.api.Test;

/**
 * Test serializability of DDI api model 'DdiArtifactHash'
  * <p/>
 * Feature: Unit Tests - Direct Device Integration API<br/>
 * Story: Serializability of DDI api Models
 */
class DdiArtifactHashTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Verify the correct serialization and deserialization of the model
     */
    @Test
    void shouldSerializeAndDeserializeObject() throws IOException {
        // Setup
        final String sha1Hash = "11111";
        final String md5Hash = "22222";
        final String sha256Hash = "33333";
        final DdiArtifactHash ddiArtifact = new DdiArtifactHash(sha1Hash, md5Hash, sha256Hash);

        // Test
        final String serializedDdiArtifact = OBJECT_MAPPER.writeValueAsString(ddiArtifact);
        final DdiArtifactHash deserializedDdiArtifact = OBJECT_MAPPER.readValue(serializedDdiArtifact, DdiArtifactHash.class);
        assertThat(serializedDdiArtifact).contains(sha1Hash, md5Hash, sha256Hash);
        assertThat(deserializedDdiArtifact.getSha1()).isEqualTo(sha1Hash);
        assertThat(deserializedDdiArtifact.getMd5()).isEqualTo(md5Hash);
        assertThat(deserializedDdiArtifact.getSha256()).isEqualTo(sha256Hash);
    }

    /**
     * Verify the correct deserialization of a model with a additional unknown property
     */
    @Test
    void shouldDeserializeObjectWithUnknownProperty() throws IOException {
        // Setup
        final String serializedDdiArtifact = "{\"sha1\": \"123\", \"md5\": \"456\",  \"sha256\": \"789\", \"unknownProperty\": \"test\"}";

        // Test
        final DdiArtifactHash ddiArtifact = OBJECT_MAPPER.readValue(serializedDdiArtifact, DdiArtifactHash.class);
        assertThat(ddiArtifact.getSha1()).isEqualTo("123");
        assertThat(ddiArtifact.getMd5()).isEqualTo("456");
        assertThat(ddiArtifact.getSha256()).isEqualTo("789");
    }

    /**
     * Verify that deserialization fails for known properties with a wrong datatype
     */
    @Test
    void shouldFailForObjectWithWrongDataTypes() {
        // Setup
        final String serializedDdiArtifact = "{\"sha1\": [123], \"md5\": 456, \"sha256\": \"789\"";

        // Test
        assertThatExceptionOfType(MismatchedInputException.class)
                .isThrownBy(() -> OBJECT_MAPPER.readValue(serializedDdiArtifact, DdiArtifactHash.class));
    }
}