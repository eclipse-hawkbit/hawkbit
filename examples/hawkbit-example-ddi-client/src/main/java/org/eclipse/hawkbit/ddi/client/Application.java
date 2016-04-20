/**
 * Copyright (c) 2011-2016 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.ddi.client;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.netflix.feign.EnableFeignClients;

/**
 * @author Jonathan Knoblauch
 *
 */
@SpringBootApplication
@EnableFeignClients
public class Application {

    public static void main(final String[] args) {
        new SpringApplicationBuilder().showBanner(false).sources(Application.class).run(args);
    }

}
