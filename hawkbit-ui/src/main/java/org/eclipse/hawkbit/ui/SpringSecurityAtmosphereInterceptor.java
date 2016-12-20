/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui;

import org.atmosphere.config.service.AtmosphereInterceptorService;
import org.atmosphere.cpr.Action;
import org.atmosphere.cpr.AtmosphereInterceptor;
import org.atmosphere.cpr.AtmosphereInterceptorAdapter;
import org.atmosphere.cpr.AtmosphereResource;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

/**
 * An {@link AtmosphereInterceptor} implementation which retrieves the
 * {@link SecurityContext} from the http-session and set in into the
 * {@link SecurityContextHolder}. This is necessary due that websocket requests
 * are not going through the spring security filter chain and the
 * {@link SecurityContext} will not be present in the current Thread.
 */
@AtmosphereInterceptorService
public class SpringSecurityAtmosphereInterceptor extends AtmosphereInterceptorAdapter {

    @Override
    public Action inspect(final AtmosphereResource r) {
        final SecurityContext context = (SecurityContext) r.getRequest().getSession()
                .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        SecurityContextHolder.setContext(context);
        return Action.CONTINUE;
    }

    @Override
    public void postInspect(final AtmosphereResource r) {
        SecurityContextHolder.clearContext();
    }
}
