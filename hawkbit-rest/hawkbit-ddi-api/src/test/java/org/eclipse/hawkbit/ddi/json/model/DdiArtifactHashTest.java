package org.eclipse.hawkbit.ddi.json.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

/**
 * Test serializability of DDI api model 'DdiArtifact'
 */
@Feature("Model Tests - Direct Device Integration API")
@Story("Serializability of DDI api Models")
public class DdiArtifactHashTest {

    private ObjectMapper mapper = new ObjectMapper();

    @Test
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
    public void shouldDeserializeObjectWithUnknownProperty() throws IOException {
        // Setup
        String serializedDdiArtifact = "{\"sha1\": \"123\", \"md5\": \"456\",  \"sha256\": \"789\", \"unknownProperty\": \"test\"}";

        // Test
        DdiArtifactHash DdiArtifact = mapper.readValue(serializedDdiArtifact, DdiArtifactHash.class);

        assertThat(DdiArtifact.getSha1()).isEqualTo("123");
        assertThat(DdiArtifact.getMd5()).isEqualTo("456");
        assertThat(DdiArtifact.getSha256()).isEqualTo("789");
    }

    @Test(expected = com.fasterxml.jackson.databind.exc.MismatchedInputException.class)
    public void shouldFailForObjectWithWrongDataTypes() throws IOException {
        // Setup
        String serializedDdiArtifact = "{\"sha1\": [123], \"md5\": 456, \"sha256\": \"789\"";

        // Test
        mapper.readValue(serializedDdiArtifact, DdiArtifactHash.class);
    }

}
