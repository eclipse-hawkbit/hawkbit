/**
 * Copyright (c) 2022 Bosch.IO GmbH and others
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
@JsonPropertyOrder({ "id", "confirmation", "actionHistory" })
@Schema(description = """
        The response body includes the detailed information about the action awaiting confirmation in the same format as
        for the deploymentBase operation.""",
        example = """
                {
                  "id" : "6",
                  "confirmation" : {
                    "download" : "forced",
                    "update" : "forced",
                    "maintenanceWindow" : "available",
                    "chunks" : [ {
                      "part" : "jvm",
                      "version" : "1.0.62",
                      "name" : "oneapp runtime",
                      "artifacts" : [ {
                        "filename" : "binary.tgz",
                        "hashes" : {
                          "sha1" : "3dceccec02e7626184bdbba12b247b67ff04c363",
                          "md5" : "a9a7df0aa4c72b3b03b654c42d29744b",
                          "sha256" : "971d8db88fef8e7a3e6d5bbf501d69b07d0c300d9be948aff8b52960ef039358"
                        },
                        "size" : 11,
                        "_links" : {
                          "download" : {
                            "href" : "https://link-to-cdn.com/api/v1/TENANT_ID/download/controller/CONTROLLER_ID/softwaremodules/17/filename/binary.tgz"
                          },
                          "download-http" : {
                            "href" : "http://link-to-cdn.com/api/v1/TENANT_ID/download/controller/CONTROLLER_ID/softwaremodules/17/filename/binary.tgz"
                          },
                          "md5sum-http" : {
                            "href" : "http://link-to-cdn.com/api/v1/TENANT_ID/download/controller/CONTROLLER_ID/softwaremodules/17/filename/binary.tgz.MD5SUM"
                          },
                          "md5sum" : {
                            "href" : "https://link-to-cdn.com/api/v1/TENANT_ID/download/controller/CONTROLLER_ID/softwaremodules/17/filename/binary.tgz.MD5SUM"
                          }
                        }
                      }, {
                        "filename" : "file.signature",
                        "hashes" : {
                          "sha1" : "3dceccec02e7626184bdbba12b247b67ff04c363",
                          "md5" : "a9a7df0aa4c72b3b03b654c42d29744b",
                          "sha256" : "971d8db88fef8e7a3e6d5bbf501d69b07d0c300d9be948aff8b52960ef039358"
                        },
                        "size" : 11,
                        "_links" : {
                          "download" : {
                            "href" : "https://link-to-cdn.com/api/v1/TENANT_ID/download/controller/CONTROLLER_ID/softwaremodules/17/filename/file.signature"
                          },
                          "download-http" : {
                            "href" : "http://link-to-cdn.com/api/v1/TENANT_ID/download/controller/CONTROLLER_ID/softwaremodules/17/filename/file.signature"
                          },
                          "md5sum-http" : {
                            "href" : "http://link-to-cdn.com/api/v1/TENANT_ID/download/controller/CONTROLLER_ID/softwaremodules/17/filename/file.signature.MD5SUM"
                          },
                          "md5sum" : {
                            "href" : "https://link-to-cdn.com/api/v1/TENANT_ID/download/controller/CONTROLLER_ID/softwaremodules/17/filename/file.signature.MD5SUM"
                          }
                        }
                      } ]
                    }, {
                      "part" : "bApp",
                      "version" : "1.0.96",
                      "name" : "oneapplication",
                      "artifacts" : [ {
                        "filename" : "binary.tgz",
                        "hashes" : {
                          "sha1" : "701c0c0fcbee5e96fa5c5b819cb519686940ade3",
                          "md5" : "f0f6a34c4c9e79d07c2d92c3c3d88560",
                          "sha256" : "cff472a07c3143741fb03ac6c577acabef72a186a8bfaab00bbb47ca5ebbe554"
                        },
                        "size" : 11,
                        "_links" : {
                          "download" : {
                            "href" : "https://link-to-cdn.com/api/v1/TENANT_ID/download/controller/CONTROLLER_ID/softwaremodules/16/filename/binary.tgz"
                          },
                          "download-http" : {
                            "href" : "http://link-to-cdn.com/api/v1/TENANT_ID/download/controller/CONTROLLER_ID/softwaremodules/16/filename/binary.tgz"
                          },
                          "md5sum-http" : {
                            "href" : "http://link-to-cdn.com/api/v1/TENANT_ID/download/controller/CONTROLLER_ID/softwaremodules/16/filename/binary.tgz.MD5SUM"
                          },
                          "md5sum" : {
                            "href" : "https://link-to-cdn.com/api/v1/TENANT_ID/download/controller/CONTROLLER_ID/softwaremodules/16/filename/binary.tgz.MD5SUM"
                          }
                        }
                      }, {
                        "filename" : "file.signature",
                        "hashes" : {
                          "sha1" : "701c0c0fcbee5e96fa5c5b819cb519686940ade3",
                          "md5" : "f0f6a34c4c9e79d07c2d92c3c3d88560",
                          "sha256" : "cff472a07c3143741fb03ac6c577acabef72a186a8bfaab00bbb47ca5ebbe554"
                        },
                        "size" : 11,
                        "_links" : {
                          "download" : {
                            "href" : "https://link-to-cdn.com/api/v1/TENANT_ID/download/controller/CONTROLLER_ID/softwaremodules/16/filename/file.signature"
                          },
                          "download-http" : {
                            "href" : "http://link-to-cdn.com/api/v1/TENANT_ID/download/controller/CONTROLLER_ID/softwaremodules/16/filename/file.signature"
                          },
                          "md5sum-http" : {
                            "href" : "http://link-to-cdn.com/api/v1/TENANT_ID/download/controller/CONTROLLER_ID/softwaremodules/16/filename/file.signature.MD5SUM"
                          },
                          "md5sum" : {
                            "href" : "https://link-to-cdn.com/api/v1/TENANT_ID/download/controller/CONTROLLER_ID/softwaremodules/16/filename/file.signature.MD5SUM"
                          }
                        }
                      } ]
                    }, {
                      "part" : "os",
                      "version" : "1.0.44",
                      "name" : "one Firmware",
                      "artifacts" : [ {
                        "filename" : "binary.tgz",
                        "hashes" : {
                          "sha1" : "2b09765e953cd138b7da8f4725e48183dab62aec",
                          "md5" : "9b0aa2f51379cb4a5e0b7d026c2605c9",
                          "sha256" : "618faa741070b3f8148bad06f088e537a8f7913e734df4dde61fb163725cb4ee"
                        },
                        "size" : 15,
                        "_links" : {
                          "download" : {
                            "href" : "https://link-to-cdn.com/api/v1/TENANT_ID/download/controller/CONTROLLER_ID/softwaremodules/18/filename/binary.tgz"
                          },
                          "download-http" : {
                            "href" : "http://link-to-cdn.com/api/v1/TENANT_ID/download/controller/CONTROLLER_ID/softwaremodules/18/filename/binary.tgz"
                          },
                          "md5sum-http" : {
                            "href" : "http://link-to-cdn.com/api/v1/TENANT_ID/download/controller/CONTROLLER_ID/softwaremodules/18/filename/binary.tgz.MD5SUM"
                          },
                          "md5sum" : {
                            "href" : "https://link-to-cdn.com/api/v1/TENANT_ID/download/controller/CONTROLLER_ID/softwaremodules/18/filename/binary.tgz.MD5SUM"
                          }
                        }
                      }, {
                        "filename" : "file.signature",
                        "hashes" : {
                          "sha1" : "2b09765e953cd138b7da8f4725e48183dab62aec",
                          "md5" : "9b0aa2f51379cb4a5e0b7d026c2605c9",
                          "sha256" : "618faa741070b3f8148bad06f088e537a8f7913e734df4dde61fb163725cb4ee"
                        },
                        "size" : 15,
                        "_links" : {
                          "download" : {
                            "href" : "https://link-to-cdn.com/api/v1/TENANT_ID/download/controller/CONTROLLER_ID/softwaremodules/18/filename/file.signature"
                          },
                          "download-http" : {
                            "href" : "http://link-to-cdn.com/api/v1/TENANT_ID/download/controller/CONTROLLER_ID/softwaremodules/18/filename/file.signature"
                          },
                          "md5sum-http" : {
                            "href" : "http://link-to-cdn.com/api/v1/TENANT_ID/download/controller/CONTROLLER_ID/softwaremodules/18/filename/file.signature.MD5SUM"
                          },
                          "md5sum" : {
                            "href" : "https://link-to-cdn.com/api/v1/TENANT_ID/download/controller/CONTROLLER_ID/softwaremodules/18/filename/file.signature.MD5SUM"
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
                    "status" : "WAIT_FOR_CONFIRMATION",
                    "messages" : [ "Assignment initiated by user 'TestPrincipal'", "Waiting for the confirmation by the device before processing with the deployment" ]
                  }
                }""")
public class DdiConfirmationBaseAction extends RepresentationModel<DdiConfirmationBaseAction> {

    @JsonProperty("id")
    @NotNull
    @Schema(description = "Id of the action", example = "6")
    private String id;

    @JsonProperty("confirmation")
    @NotNull
    @Schema(description = "Deployment confirmation operation")
    private DdiDeployment confirmation;

    /**
     * Action history containing current action status and a list of feedback
     * messages received earlier from the controller.
     */
    @JsonProperty("actionHistory")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = """
            (Optional) GET parameter to retrieve a given number of messages which are previously
            provided by the device. Useful if the devices sent state information to the feedback channel and never
            stored them locally""")
    private DdiActionHistory actionHistory;

    /**
     * Constructor.
     *
     * @param id of the update action
     * @param confirmation chunk details
     * @param actionHistory containing current action status and a list of feedback messages
     *         received earlier from the controller.
     */
    public DdiConfirmationBaseAction(final String id, final DdiDeployment confirmation,
            final DdiActionHistory actionHistory) {
        this.id = id;
        this.confirmation = confirmation;
        this.actionHistory = actionHistory;
    }
}