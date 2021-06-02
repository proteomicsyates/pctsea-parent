package edu.scripps.yates.pctsea.views.analyze;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;

public class MyConfirmDialog extends Dialog {

	public MyConfirmDialog(String title, String message, String confirmButtonText) {
		super();

		final Div content = new Div();
		content.getStyle().set("font-weight", "bold").set("color", "red");
		content.setText(title);
		add(content);

		add(new Text(message));
		setCloseOnEsc(true);
		setCloseOnOutsideClick(true);

		final Button confirmButton = new Button(confirmButtonText, event -> {

			close();
		});

		// Cancel action on ENTER press
		Shortcuts.addShortcutListener(this, () -> {

			close();
		}, Key.ENTER);

		add(new Div(confirmButton));
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -8154992030293853935L;

}
