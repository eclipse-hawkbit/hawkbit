/**
 * Copyright (c) Siemens AG, 2017
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.hawkbit.extensions.offline.update;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.fileUpload;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.RandomStringUtils;
import org.eclipse.hawkbit.exception.SpServerError;
import org.eclipse.hawkbit.offline.update.OfflineUpdateApiConfiguration;
import org.eclipse.hawkbit.offline.update.model.Artifact;
import org.eclipse.hawkbit.offline.update.model.OfflineUpdateData;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.test.util.HashGeneratorUtils;
import org.eclipse.hawkbit.rest.AbstractRestIntegrationTest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.annotation.Description;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Component Tests - Offline Update API")
@Stories("Offline software update resources")
@ContextConfiguration(classes = OfflineUpdateApiConfiguration.class)
public class OfflineUpdateTest extends AbstractRestIntegrationTest {

    private static final String OFFLINE_DATA_URL = "/rest/v1/distributionsets/offlineInstall";
    private static final String MODULE_NAME = "c1Module";
    private static final String MODULE_TYPE_OS = "os";
    private static final String MODULE_DESCRIPTION = "some description";
    private static final boolean MIGRATION_STEP_REQUIRED = true;
    private static final String MODULE_VERSION = "1.0.0";
    private static final String MODULE_TYPE_NEW_OS = "new_os";
    private static final String MODULE_TYPE_APP = "application";
    private final String fileName = "file.txt";
    private MockMultipartFile file;
    private String[] knownTargetIds;
    private String md5sum = "";
    private String sha1sum = "";
    private Target target;

    @Before
    public void before() throws Exception {
        super.before();
        knownTargetIds = new String[] { "11" };

        final byte random[] = RandomStringUtils.random(5 * 1024).getBytes();
        md5sum = HashGeneratorUtils.generateMD5(random);
        sha1sum = HashGeneratorUtils.generateSHA1(random);
        target = targetManagement.createTarget(
                entityFactory.target().create().controllerId(MODULE_NAME).name(MODULE_NAME).description(MODULE_NAME));
        // create test file
        file = new MockMultipartFile(fileName, "file", null, random);

        final JSONArray list = new JSONArray();
        for (final String targetId : knownTargetIds) {
            testdataFactory.createTarget(targetId);
            list.put(new JSONObject().put("id", Long.valueOf(targetId)));
        }
    }

    @Test
    @Description("Ensures that a valid offline software update request is successful.")
    public void offlineUpdateOSTypeModule() throws Exception {
        OfflineUpdateData softwareModuleData = new OfflineUpdateData();
        softwareModuleData.setControllerIds(Arrays.asList(knownTargetIds));
        softwareModuleData.setMigrationStepRequired(MIGRATION_STEP_REQUIRED);
        List<org.eclipse.hawkbit.offline.update.model.SoftwareModuleInfo> softwareModulesList = new ArrayList<>();
        softwareModulesList.add(getSoftwareModule(MODULE_NAME, MODULE_TYPE_OS, fileName, md5sum, sha1sum));
        softwareModuleData.setSoftwareModules(softwareModulesList);

        String contentDataOS = getSoftwareModuleContent(softwareModuleData);

        mvc.perform(fileUpload(OFFLINE_DATA_URL).file(file)
                .param("offlineUpdateInfo", contentDataOS).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.modules[0].name", equalTo(MODULE_NAME)))
                .andExpect(jsonPath("$.modules[0].type", equalTo(osType.getKey())))
                .andExpect(jsonPath("$.modules[0].description", equalTo(MODULE_DESCRIPTION)))
                .andExpect(jsonPath("$.version", equalTo(MODULE_VERSION)))
                .andExpect(jsonPath("$.requiredMigrationStep", equalTo(MIGRATION_STEP_REQUIRED)));
    }

    @Test
    @Description("Ensures that an offline software update request with an invalid software module type throws appropriate error.")
    public void offlineInvalidSoftwareModuleType() throws Exception {
        SoftwareModuleType fwType = softwareModuleTypeManagement.createSoftwareModuleType(
                entityFactory.softwareModuleType().create().name("fwSMType").key("fwSMType").description("fwSMType"));

        Collection<SoftwareModuleType> mandatory = new ArrayList<>();
        mandatory.add(fwType);

        distributionSetTypeManagement.createDistributionSetType(entityFactory.distributionSetType().create()
                .key("testKey1").name("TestName1").description("Desc1").colour("col")
                .mandatory(mandatory.stream().map(SoftwareModuleType::getId).collect(Collectors.toList())));

        OfflineUpdateData softwareModuleData = new OfflineUpdateData();
        softwareModuleData.setControllerIds(Arrays.asList(target.getName()));
        softwareModuleData.setMigrationStepRequired(MIGRATION_STEP_REQUIRED);

        List<org.eclipse.hawkbit.offline.update.model.SoftwareModuleInfo> softwareModulesList = new ArrayList<>();
        softwareModulesList.add(getSoftwareModule(MODULE_NAME, MODULE_TYPE_NEW_OS, fileName, md5sum, sha1sum));
        softwareModuleData.setSoftwareModules(softwareModulesList);

        String contentDataOS = getSoftwareModuleContent(softwareModuleData);
        mvc.perform(fileUpload(OFFLINE_DATA_URL).file(file)
                .param("offlineUpdateInfo", contentDataOS).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.errorCode", equalTo(SpServerError.SP_REPO_ENTITY_NOT_EXISTS.getKey())));
    }

    @Test
    @Description("Ensures that an offline software update request with an empty software module type throws appropriate error.")
    public void offlineUpdateEmptySoftwareModule() throws Exception {
        OfflineUpdateData softwareModuleData = new OfflineUpdateData();
        softwareModuleData.setControllerIds(Arrays.asList(knownTargetIds));
        softwareModuleData.setMigrationStepRequired(MIGRATION_STEP_REQUIRED);
        List<org.eclipse.hawkbit.offline.update.model.SoftwareModuleInfo> softwareModulesList = new ArrayList<>();

        softwareModuleData.setSoftwareModules(softwareModulesList);
        String contentDataOS = getSoftwareModuleContent(softwareModuleData);
        mvc.perform(fileUpload(OFFLINE_DATA_URL).file(file)
                .param("offlineUpdateInfo", contentDataOS)).andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.errorCode", equalTo(SpServerError.SP_DS_INCOMPLETE.getKey())));
    }

    @Test
    @Description("Ensures that an offline software update request with multiple software module types is successful.")
    public void offlineUpdateMultipleOSTypeModule() throws Exception {
        OfflineUpdateData softwareModuleData = new OfflineUpdateData();
        softwareModuleData.setControllerIds(Arrays.asList(knownTargetIds));
        softwareModuleData.setMigrationStepRequired(MIGRATION_STEP_REQUIRED);

        final byte random[] = RandomStringUtils.random(5 * 1024).getBytes();
        String md5sum2 = HashGeneratorUtils.generateMD5(random);
        String sha1sum2 = HashGeneratorUtils.generateSHA1(random);

        String fileName2 = "os";
        MockMultipartFile file2 = new MockMultipartFile(fileName2, "file", null, random);

        List<org.eclipse.hawkbit.offline.update.model.SoftwareModuleInfo> softwareModulesList = new ArrayList<>();
        softwareModulesList.add(getSoftwareModule(MODULE_NAME, MODULE_TYPE_OS, fileName, md5sum, sha1sum));
        softwareModulesList.add(getSoftwareModule(MODULE_NAME, MODULE_TYPE_APP, fileName2, md5sum2, sha1sum2));
        softwareModuleData.setSoftwareModules(softwareModulesList);

        String contentDataOS = getSoftwareModuleContent(softwareModuleData);
        mvc.perform(fileUpload(OFFLINE_DATA_URL).file(file).file(file2)
                .param("offlineUpdateInfo", contentDataOS)).andExpect(status().isOk())
                .andExpect(jsonPath("$.modules[0].name", equalTo(MODULE_NAME)))
                .andExpect(jsonPath("$.modules[0].description", equalTo(MODULE_DESCRIPTION)))
                .andExpect(jsonPath("$.version", equalTo(MODULE_VERSION)))
                .andExpect(jsonPath("$.requiredMigrationStep", equalTo(MIGRATION_STEP_REQUIRED)))
                .andExpect(jsonPath("$.modules[0].type", anyOf(equalTo(osType.getKey()), equalTo(appType.getKey()))));
    }

    @Test
    @Description("Ensures that an offline software update request with valid software module type but with distribution set type has another mandatory module type throws appropriate error.")
    public void offlineUpdateMissingSoftwareModuleType() throws Exception {
        SoftwareModuleType swType = softwareModuleTypeManagement.createSoftwareModuleType(
                entityFactory.softwareModuleType().create().name("swSMType").key("swSMType").description("swSMType"));
        SoftwareModuleType fwType = softwareModuleTypeManagement.createSoftwareModuleType(
                entityFactory.softwareModuleType().create().name("fwSMType").key("fwSMType").description("fwSMType"));

        Collection<SoftwareModuleType> mandatory = new ArrayList<>();
        mandatory.add(fwType);
        mandatory.add(swType);

        distributionSetTypeManagement.createDistributionSetType(entityFactory.distributionSetType().create()
                .key("testKey1").name("TestName1").description("Desc1").colour("col")
                .mandatory(mandatory.stream().map(SoftwareModuleType::getId).collect(Collectors.toList())));

        OfflineUpdateData softwareModuleData = new OfflineUpdateData();
        softwareModuleData.setControllerIds(Arrays.asList(target.getName()));
        softwareModuleData.setMigrationStepRequired(MIGRATION_STEP_REQUIRED);

        List<org.eclipse.hawkbit.offline.update.model.SoftwareModuleInfo> softwareModulesList = new ArrayList<>();
        softwareModulesList.add(getSoftwareModule(MODULE_NAME, "fwSMType", fileName, md5sum, sha1sum));
        softwareModuleData.setSoftwareModules(softwareModulesList);

        String contentDataOS = getSoftwareModuleContent(softwareModuleData);
        mvc.perform(fileUpload(OFFLINE_DATA_URL).file(file)
                .param("offlineUpdateInfo", contentDataOS)).andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.errorCode", equalTo(SpServerError.SP_REPO_ENTITY_NOT_EXISTS.getKey())));
    }

    @Test
    @Description("Ensures that an offline software update request with invalid artifact details throws appropriate error.")
    public void offlineUpdateExceptionInFileUpload() throws Exception {
        SoftwareModuleType fwType = softwareModuleTypeManagement.createSoftwareModuleType(
                entityFactory.softwareModuleType().create().name("fwSMType").key("fwSMType").description("fwSMType"));

        Collection<SoftwareModuleType> mandatory = new ArrayList<>();
        mandatory.add(fwType);

        distributionSetTypeManagement.createDistributionSetType(entityFactory.distributionSetType().create()
                .key("testKey1").name("TestName1").description("Desc1").colour("col")
                .mandatory(mandatory.stream().map(SoftwareModuleType::getId).collect(Collectors.toList())));

        OfflineUpdateData softwareModuleData = new OfflineUpdateData();
        softwareModuleData.setControllerIds(Arrays.asList(target.getName()));
        softwareModuleData.setMigrationStepRequired(MIGRATION_STEP_REQUIRED);

        List<org.eclipse.hawkbit.offline.update.model.SoftwareModuleInfo> softwareModulesList = new ArrayList<>();
        softwareModulesList
                .add(getSoftwareModule(MODULE_NAME, "fwSMType", "invalidFilename", "invalidmd5", "invalidsha1"));
        softwareModuleData.setSoftwareModules(softwareModulesList);

        final byte random[] = null;
        MockMultipartFile fileOther1 = new MockMultipartFile("file", "other1", null, random);
        MockMultipartFile fileOther2 = new MockMultipartFile("file", "other2", null, random);
        String contentDataOS = getSoftwareModuleContent(softwareModuleData);
        mvc.perform(fileUpload(OFFLINE_DATA_URL).file(fileOther1).file(fileOther2)
                .param("offlineUpdateInfo", contentDataOS)).andExpect(status().is5xxServerError());
    }

    /**
     * Returns an object of type
     * {@link org.eclipse.hawkbit.offline.update.model.SoftwareModuleInfo} based
     * on the parameters.
     *
     * @param moduleName
     *            name of the module
     * @param moduleType
     *            type of the module
     * @param fileName
     *            filename of the artifact to be uploaded for a module
     * @param md5Hash
     *            md5Hash of the file
     * @param sha1Hash
     *            sha1Hash of the file
     * @return {@link org.eclipse.hawkbit.offline.update.model.SoftwareModuleInfo}.
     */
    private org.eclipse.hawkbit.offline.update.model.SoftwareModuleInfo getSoftwareModule(String moduleName,
            String moduleType, String fileName, String md5Hash, String sha1Hash) {
        org.eclipse.hawkbit.offline.update.model.SoftwareModuleInfo softwareModule = new org.eclipse.hawkbit.offline.update.model.SoftwareModuleInfo();
        softwareModule.setName(moduleName);
        softwareModule.setDescription(MODULE_DESCRIPTION);
        softwareModule.setType(moduleType);
        softwareModule.setVersion(MODULE_VERSION);

        List<Artifact> artifactDataList = new ArrayList<>();
        Artifact artifacts = new Artifact();
        artifacts.setFilename(fileName);
        artifacts.setVersion("1.0");
        artifacts.setMd5Hash(md5Hash);
        artifacts.setSha1Hash(sha1Hash);

        artifacts.setHref("https://localhost/rest/v1/softwaremodules/3/artifacts");

        artifactDataList.add(artifacts);
        softwareModule.setArtifacts(artifactDataList);
        return softwareModule;
    }

    /**
     * Returns the JSON String for a {@link OfflineUpdateData}.
     *
     * @param softwareModuleData
     *            {@link OfflineUpdateData} to be parsed for JSON
     * @return JSON String
     * @throws {@link
     *             JSONException}.
     */
    private String getSoftwareModuleContent(final OfflineUpdateData softwareModuleData) throws JSONException {
        JSONObject contentBuilder = new JSONObject();
        contentBuilder.put("controllerIds", softwareModuleData.getControllerIds())
                .put("migrationStepRequired", softwareModuleData.isMigrationStepRequired())
                .put("softwareModules", softwareModuleData.getSoftwareModules());
        return contentBuilder.toString();
    }

}
