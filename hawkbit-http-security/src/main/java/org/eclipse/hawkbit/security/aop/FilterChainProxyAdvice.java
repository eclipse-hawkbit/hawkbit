/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.security.aop;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.eclipse.hawkbit.exception.SpServerError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.firewall.RequestRejectedException;
import org.springframework.security.web.firewall.StrictHttpFirewall;

/**
 * Workaround for issue
 * https://github.com/spring-projects/spring-security/issues/5007
 *
 * Advice class for the {@link FilterChainProxy} to handle the
 * {@link RequestRejectedException} thrown by the {@link StrictHttpFirewall} and
 * logged by Jetty/Tomcat.
 *
 * Using AspectJ advice around the {@link FilterChainProxy} makes it possible to
 * directly return the error code through the ServletResponse (2nd argument in
 * the doFilter() method). It is not possible to advise Jetty or Tomcat because
 * they are not managed by Spring.
 * 
 * Results in an error code 400 BAD_REQUEST instead of the (default) 500.
 */
@Aspect
public class FilterChainProxyAdvice {

    private static final Logger LOG = LoggerFactory.getLogger(FilterChainProxyAdvice.class);
    /**
     * Creates an advice for the {@link FilterChainProxy} doFilter method to
     * catch the {@link RequestRejectedException} thrown by the
     * {@link StrictHttpFirewall} and sends a 400 BAD REQUEST response
     *
     * @param pjp
     *            the {@link ProceedingJoinPoint} around the {@link FilterChainProxy}.doFilter() method
     */
    @Around("execution(public void org.springframework.security.web.FilterChainProxy.doFilter("
            + "javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain))")
    @SuppressWarnings("squid:S1166") // Exception should not be (fully) logged nor rethrown
    public void handleRequestRejectedExceptionInFilterChainProxy(final ProceedingJoinPoint pjp) throws Throwable {
        try {
            pjp.proceed();
        } catch (RequestRejectedException exception) {
            LOG.debug("Caught a RequestRejectedException with reason phrase: [{}], sending response error BAD_REQUEST",
                    exception.getMessage());
            sendBadRequestResponse((HttpServletRequest) pjp.getArgs()[0], (HttpServletResponse) pjp.getArgs()[1]);
        }
    }

    private static void sendBadRequestResponse(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(constructResponseBody(request));
    }

    private static String constructResponseBody(final HttpServletRequest request) {
        return String.format("{\"message\":\"%s\", \"path\": \"%s\"}",
                SpServerError.SP_MALICIOUS_URL_STRING.getMessage(), request.getRequestURI());
    }
}
