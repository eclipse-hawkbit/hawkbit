/*
 * Copyright (c) 2020 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

function recreateStylesheet(styleId) {
	var stylesheet = document.getElementById(styleId);
    if (stylesheet) { 
    	document.head.removeChild(stylesheet); 
    }

    stylesheet = document.createElement('style'); 
    stylesheet.id=styleId;  
    document.head.appendChild(stylesheet);

    return stylesheet;
}

function addStyleRule(stylesheet, selector, rule) {
	if (stylesheet) { 
		if (stylesheet.addRule) {
			stylesheet.addRule(selector, rule);
		} else if (stylesheet.insertRule) {
			stylesheet.insertRule(selector + ' { ' + rule + ' }', stylesheet.cssRules.length);
		}
	}
}