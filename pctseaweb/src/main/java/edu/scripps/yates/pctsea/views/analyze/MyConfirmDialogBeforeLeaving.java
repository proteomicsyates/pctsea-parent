package edu.scripps.yates.pctsea.views.analyze;

import java.io.File;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.BeforeLeaveEvent.ContinueNavigationAction;

public class MyConfirmDialogBeforeLeaving extends Dialog {

	/**
	 * Dialog to wanr the user that if they leave, progress will be lost. If they
	 * are ok with leaving, the inputFile will be deleted.
	 * 
	 * @param title
	 * @param message
	 * @param confirmButtonText
	 * @param cancelButtonText
	 * @param action
	 * @param inputFile
	 */
	public MyConfirmDialogBeforeLeaving(String title, String message, String confirmButtonText, String cancelButtonText,
			ContinueNavigationAction action, File inputFile) {
		super();

		final Div content = new Div();
		content.getStyle().set("font-weight", "bold").set("color", "red");
		content.setText(title);
		add(content);

		add(new Text(message));
		setCloseOnEsc(true);
		setCloseOnOutsideClick(true);

		final Button confirmButton = new Button(confirmButtonText, event -> {
			if (inputFile != null) {
				inputFile.delete();
			}
			close();
			action.proceed();
		});
		final Button cancelButton = new Button(cancelButtonText, event -> {

			close();
		});
		// Cancel action on ENTER press
		Shortcuts.addShortcutListener(this, () -> {

			close();
		}, Key.ENTER);

		add(new HorizontalLayout(confirmButton, cancelButton));
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -8154992030293853935L;

}
