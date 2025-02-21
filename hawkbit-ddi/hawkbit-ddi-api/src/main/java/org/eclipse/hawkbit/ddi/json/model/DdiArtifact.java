/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ddi.json.model;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.hateoas.RepresentationModel;

/**
 * Download information for all artifacts related to a specific {@link DdiChunk}
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = """
        **_links**:
        * **download** - HTTPs Download resource for artifacts. The resource supports partial download as specified by RFC7233 (range requests). Keep in mind that the target needs to have the artifact assigned in order to be granted permission to download.
        * **md5sum** - HTTPs Download resource for MD5SUM file is an optional auto generated artifact that is especially useful for Linux based devices on order to check artifact consistency after download by using the md5sum command line tool. The MD5 and SHA1 are in addition available as metadata in the deployment command itself.
        * **download-http** - HTTP Download resource for artifacts. The resource supports partial download as specified by RFC7233 (range requests). Keep in mind that the target needs to have the artifact assigned in order to be granted permission to download. (note: anonymous download needs to be enabled on the service account for non-TLS access)
        * **md5sum-http** - HTTP Download resource for MD5SUM file is an optional auto generated artifact that is especially useful for Linux based devices on order to check artifact consistency after download by using the md5sum command line tool. The MD5 and SHA1 are in addition available as metadata in the deployment command itself. (note: anonymous download needs to be enabled on the service account for non-TLS access)    
        """, example = """
        {
          "filename" : "binaryFile",
          "hashes" : {
            "sha1" : "e4e667b70ff652cb9d9c8a49f141bd68e06cec6f",
            "md5" : "13793b0e3a7830ed685d3ede7ff93048",
            "sha256" : "c51368bf045803b429a67bdf04539a373d9fb8caa310fe0431265e6871b4f07a"
          },
          "size" : 11,
          "_links" : {
            "download" : {
              "href" : "https://link-to-cdn.com/api/v1/TENANT_ID/download/controller/CONTROLLER_ID/softwaremodules/40/filename/binaryFile"
            },
            "download-http" : {
              "href" : "http://link-to-cdn.com/api/v1/TENANT_ID/download/controller/CONTROLLER_ID/softwaremodules/40/filename/binaryFile"
            },
            "md5sum-http" : {
              "href" : "http://link-to-cdn.com/api/v1/TENANT_ID/download/controller/CONTROLLER_ID/softwaremodules/40/filename/binaryFile.MD5SUM"
            },
            "md5sum" : {
              "href" : "https://link-to-cdn.com/api/v1/TENANT_ID/download/controller/CONTROLLER_ID/softwaremodules/40/filename/binaryFile.MD5SUM"
            }
          }
        }""")
public class DdiArtifact extends RepresentationModel<DdiArtifact> {

    @NotNull
    @Schema(description = "File name", example = "binary.tgz")
    private final String filename;

    @Schema(description = "Artifact hashes")
    private final DdiArtifactHash hashes;

    @Schema(description = "Artifact size", example = "3")
    private final Long size;

    @JsonCreator
    public DdiArtifact(
            @JsonProperty("filename") final String filename,
            @JsonProperty("hashes") final DdiArtifactHash hashes,
            @JsonProperty("size") final Long size) {
        this.filename = filename;
        this.hashes = hashes;
        this.size = size;
    }
}