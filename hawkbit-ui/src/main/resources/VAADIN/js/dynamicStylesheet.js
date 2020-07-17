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