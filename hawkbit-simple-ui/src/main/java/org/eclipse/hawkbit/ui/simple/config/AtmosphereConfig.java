package org.eclipse.hawkbit.ui.simple.config;

import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Restrict Atmosphere annotation/classpath scanning to the application's packages to avoid expensive
 * filesystem-wide scans (which can be triggered in container environments and cause startup stalls).
 */
@Configuration
public class AtmosphereConfig {

    @Bean
    public ServletContextInitializer atmosphereInit() {
        return servletContext -> {
            // Limit annotation scanning to the application's packages only. Adjust packages as needed.
            servletContext.setInitParameter("org.atmosphere.cpr.packages",
                    "org.eclipse.hawkbit.ui.simple,com.vaadin.flow.server,com.vaadin.flow.component");
        };
    }
}

