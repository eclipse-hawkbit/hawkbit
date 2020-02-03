/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.autoconfigure.mgmt.ui;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * Redirects for convenience. hawkBit's management UI is by default not
 * listening on / but on /UI.
 *
 */
@Controller
public class RedirectController {

    /**
     * @return redirect to the Management UI
     */
    @RequestMapping("/")
    public ModelAndView home() {
        return new ModelAndView("redirect:/UI/");
    }
}
