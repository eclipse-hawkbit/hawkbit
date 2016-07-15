package org.eclipse.hawkbit.ui.common;

import org.eclipse.hawkbit.ui.utils.I18N;

import com.vaadin.event.FieldEvents.BlurListener;
import com.vaadin.ui.AbstractLayout;
import com.vaadin.ui.Component;
import com.vaadin.ui.Button.ClickListener;

public class CustomCommonDialogWindow extends CommonDialogWindow {
	private static final long serialVersionUID = -4453608850403359992L;

	public CustomCommonDialogWindow(final String caption, final Component content, final String helpLink,
			final ClickListener saveButtonClickListener, final ClickListener cancelButtonClickListener,
			final AbstractLayout layout, final I18N i18n) {
		super(caption, content, helpLink, saveButtonClickListener, cancelButtonClickListener, layout, i18n);
	}
	
	
	@Override
	protected void addListeners() {
		addComponenetListeners();
		addCloseListenerForCancelButton();
	}

}