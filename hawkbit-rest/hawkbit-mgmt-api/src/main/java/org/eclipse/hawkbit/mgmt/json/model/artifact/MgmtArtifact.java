/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model.artifact;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.eclipse.hawkbit.mgmt.json.model.MgmtBaseEntity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A json annotated rest model for Artifact to RESTful API representation.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = """
    **_links**:
    * **download** - Download link of the artifact
    """, example = """
    {
      "createdBy" : "bumlux",
      "createdAt" : 1682408572660,
      "lastModifiedBy" : "bumlux",
      "lastModifiedAt" : 1682408572660,
      "hashes" : {
        "sha1" : "70686514bec4a9f8188f88d470fb3d7999728fad",
        "md5" : "f7c5b155e3636406cbc53c61f4692637",
        "sha256" : "efbbd71e3aa3c1db9ff3905c81f1220adb0e5db3c5438732eedf98ab006ca742"
      },
      "providedFilename" : "origFilename",
      "size" : 11,
      "_links" : {
        "self" : {
          "href" : "https://management-api.host.com/rest/v1/softwaremodules/1/artifacts/1"
        },
        "download" : {
          "href" : "https://management-api.host.com/rest/v1/softwaremodules/1/artifacts/1/download"
        }
      },
      "id" : 1
    }""")
public class MgmtArtifact extends MgmtBaseEntity {

    @JsonProperty("id")
    @Schema(description = "Artifact id", example = "3")
    private Long artifactId;

    @JsonProperty
    private MgmtArtifactHash hashes;

    @JsonProperty
    @Schema(example = "file1")
    private String providedFilename;

    @JsonProperty
    @Schema(description = "Size of the artifact", example = "3")
    private Long size;
}