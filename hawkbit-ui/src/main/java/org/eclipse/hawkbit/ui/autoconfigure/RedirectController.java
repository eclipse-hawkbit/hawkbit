/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.autoconfigure;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
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
    @GetMapping("/")
    public ModelAndView home() {
        return new ModelAndView("redirect:/UI/");
    }
}
