/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.hawkbit.ddi.json.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

/**
 * Test serializability of DDI api model 'DdiArtifactHash'
 */
@Feature("Unit Tests - Direct Device Integration API")
@Story("Serializability of DDI api Models")
public class DdiArtifactHashTest {

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    @Description("Verify the correct serialization and deserialization of the model")
    public void shouldSerializeAndDeserializeObject() throws IOException {
        // Setup
        String sha1Hash = "11111";
        String md5Hash = "22222";
        String sha256Hash = "33333";
        DdiArtifactHash DdiArtifact = new DdiArtifactHash(sha1Hash, md5Hash, sha256Hash);

        // Test
        String serializedDdiArtifact = mapper.writeValueAsString(DdiArtifact);
        DdiArtifactHash deserializedDdiArtifact = mapper.readValue(serializedDdiArtifact,
                DdiArtifactHash.class);

        assertThat(serializedDdiArtifact).contains(sha1Hash, md5Hash, sha256Hash);
        assertThat(deserializedDdiArtifact.getSha1()).isEqualTo(sha1Hash);
        assertThat(deserializedDdiArtifact.getMd5()).isEqualTo(md5Hash);
        assertThat(deserializedDdiArtifact.getSha256()).isEqualTo(sha256Hash);
    }

    @Test
    @Description("Verify the correct deserialization of a model with a additional unknown property")
    public void shouldDeserializeObjectWithUnknownProperty() throws IOException {
        // Setup
        String serializedDdiArtifact = "{\"sha1\": \"123\", \"md5\": \"456\",  \"sha256\": \"789\", \"unknownProperty\": \"test\"}";

        // Test
        DdiArtifactHash ddiArtifact = mapper.readValue(serializedDdiArtifact, DdiArtifactHash.class);

        assertThat(ddiArtifact.getSha1()).isEqualTo("123");
        assertThat(ddiArtifact.getMd5()).isEqualTo("456");
        assertThat(ddiArtifact.getSha256()).isEqualTo("789");
    }

    @Test
    @Description("Verify that deserialization fails for known properties with a wrong datatype")
    public void shouldFailForObjectWithWrongDataTypes() throws IOException {
        // Setup
        String serializedDdiArtifact = "{\"sha1\": [123], \"md5\": 456, \"sha256\": \"789\"";

        // Test
        assertThatExceptionOfType(MismatchedInputException.class).isThrownBy(
                () -> mapper.readValue(serializedDdiArtifact, DdiArtifactHash.class));
    }

}
