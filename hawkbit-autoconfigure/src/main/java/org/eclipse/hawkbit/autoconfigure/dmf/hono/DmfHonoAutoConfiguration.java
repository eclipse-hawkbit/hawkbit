package org.eclipse.hawkbit.autoconfigure.dmf.hono;

import org.eclipse.hawkbit.dmf.hono.DmfHonoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * The Eclipse Hono based device Management Federation API (DMF) auto configuration.
 */
@Configuration
@ConditionalOnClass(DmfHonoConfiguration.class)
@Import(DmfHonoConfiguration.class)
public class DmfHonoAutoConfiguration {
}