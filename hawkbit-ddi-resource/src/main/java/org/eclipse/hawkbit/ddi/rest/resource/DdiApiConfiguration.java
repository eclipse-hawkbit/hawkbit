/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ddi.rest.resource;

import org.eclipse.hawkbit.rest.RestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Controller;

/**
 * Enable {@link ComponentScan} in the resource package to setup all
 * {@link Controller} annotated classes and setup the REST-Resources for the
 * Direct Device Integration API.
 */
@Configuration
@ComponentScan
@Import(RestConfiguration.class)
public class DdiApiConfiguration {

}
