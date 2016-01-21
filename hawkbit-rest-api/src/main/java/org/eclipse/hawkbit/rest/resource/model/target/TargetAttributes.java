/**
 * Copyright (c) 2011-2015 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.rest.resource.model.target;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.hawkbit.rest.resource.model.doc.ApiModelProperties;

import io.swagger.annotations.ApiModel;

/**
 * {@link Map} with attribtes of SP Target.
 *
 *
 *
 *
 */
@ApiModel(ApiModelProperties.TARGET_ATTRIBUTES)
public class TargetAttributes extends HashMap<String, String> {

}
