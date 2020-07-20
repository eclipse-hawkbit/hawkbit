/* 
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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