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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.hateoas.RepresentationModel;

/**
 * Update action resource.
 */
@NoArgsConstructor // needed for json create
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({ "id", "deployment", "actionHistory" })
@Schema(example = """
        {
          "id" : "8",
          "deployment" : {
            "download" : "forced",
            "update" : "forced",
            "maintenanceWindow" : "available",
            "chunks" : [ {
              "part" : "jvm",
              "version" : "1.0.75",
              "name" : "oneapp runtime",
              "artifacts" : [ {
                "filename" : "binary.tgz",
                "hashes" : {
                  "sha1" : "986a1ade8b8a2f758ce951340cc5e21335cc2a00",
                  "md5" : "d04440e6533863247655ac5fd4345bcc",
                  "sha256" : "b3a04740a19e36057ccf258701922f3cd2f1a880536be53a3ca8d50f6b615975"
                },
                "size" : 13,
                "_links" : {
                  "download" : {
                    "href" : "https://link-to-cdn.com/api/v1/TENANT_ID/download/controller/CONTROLLER_ID/softwaremodules/23/filename/binary.tgz"
                  },
                  "download-http" : {
                    "href" : "http://link-to-cdn.com/api/v1/TENANT_ID/download/controller/CONTROLLER_ID/softwaremodules/23/filename/binary.tgz"
                  },
                  "md5sum-http" : {
                    "href" : "http://link-to-cdn.com/api/v1/TENANT_ID/download/controller/CONTROLLER_ID/softwaremodules/23/filename/binary.tgz.MD5SUM"
                  },
                  "md5sum" : {
                    "href" : "https://link-to-cdn.com/api/v1/TENANT_ID/download/controller/CONTROLLER_ID/softwaremodules/23/filename/binary.tgz.MD5SUM"
                  }
                }
              }, {
                "filename" : "file.signature",
                "hashes" : {
                  "sha1" : "986a1ade8b8a2f758ce951340cc5e21335cc2a00",
                  "md5" : "d04440e6533863247655ac5fd4345bcc",
                  "sha256" : "b3a04740a19e36057ccf258701922f3cd2f1a880536be53a3ca8d50f6b615975"
                },
                "size" : 13,
                "_links" : {
                  "download" : {
                    "href" : "https://link-to-cdn.com/api/v1/TENANT_ID/download/controller/CONTROLLER_ID/softwaremodules/23/filename/file.signature"
                  },
                  "download-http" : {
                    "href" : "http://link-to-cdn.com/api/v1/TENANT_ID/download/controller/CONTROLLER_ID/softwaremodules/23/filename/file.signature"
                  },
                  "md5sum-http" : {
                    "href" : "http://link-to-cdn.com/api/v1/TENANT_ID/download/controller/CONTROLLER_ID/softwaremodules/23/filename/file.signature.MD5SUM"
                  },
                  "md5sum" : {
                    "href" : "https://link-to-cdn.com/api/v1/TENANT_ID/download/controller/CONTROLLER_ID/softwaremodules/23/filename/file.signature.MD5SUM"
                  }
                }
              } ]
            }, {
              "part" : "os",
              "version" : "1.0.79",
              "name" : "one Firmware",
              "artifacts" : [ {
                "filename" : "binary.tgz",
                "hashes" : {
                  "sha1" : "574cd34be20f75d101ed23518339cc38c5157bdb",
                  "md5" : "a0637c1ccb9fd53e2ba6f45712516989",
                  "sha256" : "498014801aab66be1d7fbea56b1aa5959651b6fd710308e196a8c414029e7291"
                },
                "size" : 13,
                "_links" : {
                  "download" : {
                    "href" : "https://link-to-cdn.com/api/v1/TENANT_ID/download/controller/CONTROLLER_ID/softwaremodules/24/filename/binary.tgz"
                  },
                  "download-http" : {
                    "href" : "http://link-to-cdn.com/api/v1/TENANT_ID/download/controller/CONTROLLER_ID/softwaremodules/24/filename/binary.tgz"
                  },
                  "md5sum-http" : {
                    "href" : "http://link-to-cdn.com/api/v1/TENANT_ID/download/controller/CONTROLLER_ID/softwaremodules/24/filename/binary.tgz.MD5SUM"
                  },
                  "md5sum" : {
                    "href" : "https://link-to-cdn.com/api/v1/TENANT_ID/download/controller/CONTROLLER_ID/softwaremodules/24/filename/binary.tgz.MD5SUM"
                  }
                }
              }, {
                "filename" : "file.signature",
                "hashes" : {
                  "sha1" : "574cd34be20f75d101ed23518339cc38c5157bdb",
                  "md5" : "a0637c1ccb9fd53e2ba6f45712516989",
                  "sha256" : "498014801aab66be1d7fbea56b1aa5959651b6fd710308e196a8c414029e7291"
                },
                "size" : 13,
                "_links" : {
                  "download" : {
                    "href" : "https://link-to-cdn.com/api/v1/TENANT_ID/download/controller/CONTROLLER_ID/softwaremodules/24/filename/file.signature"
                  },
                  "download-http" : {
                    "href" : "http://link-to-cdn.com/api/v1/TENANT_ID/download/controller/CONTROLLER_ID/softwaremodules/24/filename/file.signature"
                  },
                  "md5sum-http" : {
                    "href" : "http://link-to-cdn.com/api/v1/TENANT_ID/download/controller/CONTROLLER_ID/softwaremodules/24/filename/file.signature.MD5SUM"
                  },
                  "md5sum" : {
                    "href" : "https://link-to-cdn.com/api/v1/TENANT_ID/download/controller/CONTROLLER_ID/softwaremodules/24/filename/file.signature.MD5SUM"
                  }
                }
              } ]
            }, {
              "part" : "bApp",
              "version" : "1.0.91",
              "name" : "oneapplication",
              "artifacts" : [ {
                "filename" : "binary.tgz",
                "hashes" : {
                  "sha1" : "e3ba7ff5839c210c98e254dde655147ffc49f5c9",
                  "md5" : "020017c498e6b0b8f76168fd55fa6fd1",
                  "sha256" : "80406288820379a82bbcbfbf7e8690146e46256f505de1c6d430c0168a74f6dd"
                },
                "size" : 11,
                "_links" : {
                  "download" : {
                    "href" : "https://link-to-cdn.com/api/v1/TENANT_ID/download/controller/CONTROLLER_ID/softwaremodules/22/filename/binary.tgz"
                  },
                  "download-http" : {
                    "href" : "http://link-to-cdn.com/api/v1/TENANT_ID/download/controller/CONTROLLER_ID/softwaremodules/22/filename/binary.tgz"
                  },
                  "md5sum-http" : {
                    "href" : "http://link-to-cdn.com/api/v1/TENANT_ID/download/controller/CONTROLLER_ID/softwaremodules/22/filename/binary.tgz.MD5SUM"
                  },
                  "md5sum" : {
                    "href" : "https://link-to-cdn.com/api/v1/TENANT_ID/download/controller/CONTROLLER_ID/softwaremodules/22/filename/binary.tgz.MD5SUM"
                  }
                }
              }, {
                "filename" : "file.signature",
                "hashes" : {
                  "sha1" : "e3ba7ff5839c210c98e254dde655147ffc49f5c9",
                  "md5" : "020017c498e6b0b8f76168fd55fa6fd1",
                  "sha256" : "80406288820379a82bbcbfbf7e8690146e46256f505de1c6d430c0168a74f6dd"
                },
                "size" : 11,
                "_links" : {
                  "download" : {
                    "href" : "https://link-to-cdn.com/api/v1/TENANT_ID/download/controller/CONTROLLER_ID/softwaremodules/22/filename/file.signature"
                  },
                  "download-http" : {
                    "href" : "http://link-to-cdn.com/api/v1/TENANT_ID/download/controller/CONTROLLER_ID/softwaremodules/22/filename/file.signature"
                  },
                  "md5sum-http" : {
                    "href" : "http://link-to-cdn.com/api/v1/TENANT_ID/download/controller/CONTROLLER_ID/softwaremodules/22/filename/file.signature.MD5SUM"
                  },
                  "md5sum" : {
                    "href" : "https://link-to-cdn.com/api/v1/TENANT_ID/download/controller/CONTROLLER_ID/softwaremodules/22/filename/file.signature.MD5SUM"
                  }
                }
              } ],
              "metadata" : [ {
                "key" : "aMetadataKey",
                "value" : "Metadata value as defined in software module"
              } ]
            } ]
          },
          "actionHistory" : {
            "status" : "RUNNING",
            "messages" : [ "Reboot", "Write firmware", "Download done", "Download failed. ErrorCode #5876745. Retry", "Started download", "Assignment initiated by user 'TestPrincipal'" ]
          }
        }""")
public class DdiDeploymentBase extends RepresentationModel<DdiDeploymentBase> {

    @JsonProperty("id")
    @NotNull
    @Schema(description = "Id of the action", example = "8")
    private String id;

    @JsonProperty("deployment")
    @NotNull
    @Schema(description = "Detailed deployment operation")
    private DdiDeployment deployment;

    /**
     * Action history containing current action status and a list of feedback
     * messages received earlier from the controller.
     */
    @JsonProperty("actionHistory")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Current deployment state")
    private DdiActionHistory actionHistory;

    /**
     * Constructor.
     *
     * @param id of the update action
     * @param deployment details
     * @param actionHistory containing current action status and a list of feedback
     *         messages received earlier from the controller.
     */
    public DdiDeploymentBase(final String id, final DdiDeployment deployment, final DdiActionHistory actionHistory) {
        this.id = id;
        this.deployment = deployment;
        this.actionHistory = actionHistory;
    }
}