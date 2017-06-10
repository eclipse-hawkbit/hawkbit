/**
 * Copyright (c) Siemens AG, 2017
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.offline.update;

import org.eclipse.hawkbit.offline.update.rest.resource.OfflineUpdateController;
import org.eclipse.hawkbit.rest.RestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Auto configuration for the {@link OfflineUpdateController}.
 */
@Configuration
@ComponentScan
@Import(RestConfiguration.class)
public class OfflineUpdateApiConfiguration {

}
