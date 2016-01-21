package org.eclipse.hawkbit.rest.resource;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.rest.resource.model.system.SystemConfigurationRequestBodyPut;
import org.eclipse.hawkbit.rest.resource.model.system.SystemConfigurationRest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(RestConstants.SYSTEM_V1_REQUEST_MAPPING)
public class SystemResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(SystemResource.class);

    @Autowired
    private SystemManagement systemManagement;

    @Autowired
    private DistributionSetManagement distributionSetManagement;

    @RequestMapping(method = RequestMethod.GET, value = "/conf", produces = { "application/hal+json",
            MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<SystemConfigurationRest> getSystemConfiguration() {

        return new ResponseEntity<>(SystemMapper.toResponse(systemManagement), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/conf", consumes = { "application/hal+json",
            MediaType.APPLICATION_JSON_VALUE }, produces = { "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<SystemConfigurationRest> updateSoftwareModuleType(
            @RequestBody final SystemConfigurationRequestBodyPut systemConReq) {

        systemManagement.updateTenantConfiguration(systemConReq);

        return new ResponseEntity<>(SystemMapper.toResponse(systemManagement), HttpStatus.OK);
    }

}
