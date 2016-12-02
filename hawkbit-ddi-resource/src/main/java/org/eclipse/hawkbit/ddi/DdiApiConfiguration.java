package org.eclipse.hawkbit.ddi;

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
@ComponentScan("org.eclipse.hawkbit.ddi.rest.resource")
@Import(RestConfiguration.class)
public class DdiApiConfiguration {

}
