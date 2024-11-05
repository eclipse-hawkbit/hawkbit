/**
 * Copyright (c) 2023 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.sdk;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;

import feign.Contract;
import feign.MethodMetadata;
import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.cloud.openfeign.hateoas.WebConvertersCustomizer;
import org.springframework.cloud.openfeign.support.HttpMessageConverterCustomizer;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.hateoas.config.WebConverters;
import org.springframework.http.MediaType;

@Slf4j
@Configuration
@EnableConfigurationProperties({ HawkbitServer.class, Tenant.class })
@EnableHypermediaSupport(type = EnableHypermediaSupport.HypermediaType.HAL)
@Import(FeignClientsConfiguration.class)
@PropertySource("classpath:/hawkbit-sdk-defaults.properties")
public class HawkbitSDKConfigurtion {

    /**
     * An feign request interceptor to set the defined {@code Accept} and {@code Content-Type} headers for each request
     * to {@code application/json}.
     *
     * TODO - is this needed?
     */
    @Bean
    @Primary
    public RequestInterceptor jsonHeaderInterceptorOverride() {
        return template -> template
                .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE);
    }

    // takes place only when spring app is started in non-web-app mode
    // in that case org.springframework.cloud.openfeign.hateoas.FeignHalAutoConfiguration
    // is explicitly disabled and HAL/HATEOAS support doesn't work
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnNotWebApplication
    @ConditionalOnClass({ WebConverters.class })
    public HttpMessageConverterCustomizer webConvertersCustomizerOverrider(WebConverters webConverters) {
        return new WebConvertersCustomizer(webConverters);
    }
    // another option would be something like (need to import io.github.openfeign:feign-jackson
    // @Bean @Primary @ConditionalOnNotWebApplication
    // public Decoder feignDecoderOverride() {
    //   return new ResponseEntityDecoder(new JacksonDecoder(new ObjectMapper().registerModule(new Jackson2HalModule())));
    // }

    /**
     * Own implementation of the {@link SpringMvcContract} which catches the {@link IllegalStateException} which occurs
     * due multiple produces and consumes values in the request-mapping
     * annotation.https://github.com/spring-cloud/spring-cloud-netflix/issues/808
     *
     * TODO - is this needed?
     */
    @Bean
    @Primary
    public Contract feignContractOverride() {
        return new SpringMvcContract() {

            @Override
            protected void processAnnotationOnMethod(final MethodMetadata data, final Annotation methodAnnotation, final Method method) {
                try {
                    super.processAnnotationOnMethod(data, methodAnnotation, method);
                } catch (final IllegalStateException e) {
                    // ignore illegalstateexception here because it's thrown because of
                    // multiple consumers and produces, see
                    // https://github.com/spring-cloud/spring-cloud-netflix/issues/808
                    log.trace(e.getMessage(), e);

                    // This line from super is mandatory to avoid that access to the
                    // expander causes a nullpointer.
                    data.indexToExpander(new LinkedHashMap<>());
                }
            }
        };
    }
}