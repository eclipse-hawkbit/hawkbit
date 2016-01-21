/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.api.client;

import static org.fest.assertions.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.hawkbit.app.Start;
import org.eclipse.hawkbit.rest.resource.model.target.TargetPagedList;
import org.eclipse.hawkbit.rest.resource.model.target.TargetRequestBody;
import org.eclipse.hawkbit.rest.resource.model.target.TargetRest;
import org.eclipse.hawkbit.rest.resource.model.target.TargetsRest;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Description;

import feign.Feign;
import feign.Logger;
import feign.auth.BasicAuthRequestInterceptor;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Example Tests - Management RESTful API Client")
@Stories("Target Resource")
public class TargetTest {

    @BeforeClass
    public static void startupServer() {
        SpringApplication.run(Start.class, new String[0]);
    }

    // disabled as this runs not on CI environments.
    @Test
    @Description("Simple request of target by ID")
    @Ignore
    public void getTarget() {
        final TargetResource targetResource = createTargetResource();
        final TargetsRest result = createTargets(targetResource, 1);

        assertThat(targetResource.getTarget("test0").getName()).isEqualTo("testDevice");

        deleteTargets(targetResource, result);
    }

    // disabled as this runs not on CI environments.
    @Test
    @Description("Simple request of all targets with defined page sizing information (i.e. offset and limit).")
    @Ignore
    public void getTargetsAsPagedListWithDefinedPageSizing() {
        final TargetResource targetResource = createTargetResource();
        final TargetsRest created = createTargets(targetResource, 20);

        final TargetPagedList queryResult = targetResource.getTargets(0, 10);

        assertThat(queryResult.getContent()).hasSize(10);
        assertThat(queryResult.getTotal()).isEqualTo(20);
        assertThat(queryResult.getSize()).isEqualTo(10);

        deleteTargets(targetResource, created);
    }

    // disabled as this runs not on CI environments.
    @Test
    @Description("Simple request of all targets with defualt paging parameters.")
    @Ignore
    public void getTargetsAsPagedListWithDefaultPageSizing() {
        final TargetResource targetResource = createTargetResource();
        final TargetsRest created = createTargets(targetResource, 20);

        final TargetPagedList queryResult = targetResource.getTargets();

        assertThat(queryResult.getContent()).hasSize(20);
        assertThat(queryResult.getTotal()).isEqualTo(20);
        assertThat(queryResult.getSize()).isEqualTo(20);

        deleteTargets(targetResource, created);
    }

    private void deleteTargets(final TargetResource targetResource, final List<TargetRest> targets) {
        for (final TargetRest targetRest : targets) {
            targetResource.deleteTarget(targetRest.getControllerId());
        }
    }

    private TargetsRest createTargets(final TargetResource targetResource, final int number) {

        final List<TargetRequestBody> targets = new ArrayList<>();
        for (int i = 0; i < number; i++) {

            targets.add(new TargetRequestBody().setControllerId("test" + i).setName("testDevice"));
        }

        final TargetsRest result = targetResource.createTargets(targets);

        assertThat(result).hasSize(number);
        return result;
    }

    private TargetResource createTargetResource() {
        final TargetResource targetResource = Feign.builder().logger(new Logger.ErrorLogger())
                .logLevel(Logger.Level.BASIC).decoder(new JacksonDecoder()).encoder(new JacksonEncoder())
                .requestInterceptor(new BasicAuthRequestInterceptor("admin", "admin"))
                .target(TargetResource.class, "http://localhost:8080");
        return targetResource;
    }

}
