package edu.scripps.yates.pctsea.views.results;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import edu.scripps.yates.pctsea.views.main.MainView;

@Route(value = "results", layout = MainView.class)
@PageTitle("Results")
@CssImport("./styles/views/results/results-view.css")
public class ResultsView extends Div {

//	private final Grid<SingleCell> grid;
	private final Grid<String> grid;

//	private final TextField firstName = new TextField();
//	private final TextField lastName = new TextField();
//	private final TextField email = new TextField();
//	private final TextField phone = new TextField();
//	private final DatePicker dateOfBirth = new DatePicker();
//	private final TextField occupation = new TextField();

//	private final Button cancel = new Button("Cancel");
//	private final Button save = new Button("Save");

//	private final Binder<SingleCell> binder;

//	private final SingleCell singleCell = new SingleCell();

//	private final SingleCellMongoRepository singleCellMongoRepository;

	public ResultsView(
//			@Autowired SingleCellMongoRepository personService
	) {
		setId("results-view");
//		this.singleCellMongoRepository = personService;
		// Configure Grid
//		grid = new Grid<>(SingleCell.class);
		grid = new Grid<String>();
		grid.setColumns("firstName", "lastName", "email", "phone", "dateOfBirth", "occupation");
//		grid.setDataProvider(new CrudServiceDataProvider<SingleCell, Void>(personService));
		grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
		grid.setHeightFull();

		// when a row is selected or deselected, populate form
//		grid.asSingleSelect().addValueChangeListener(event -> {
//			if (event.getValue() != null) {
//				final Optional<SingleCell> personFromBackend = personService.get(event.getValue().getId());
//				// when a row is selected but the data is no longer available, refresh grid
//				if (personFromBackend.isPresent()) {
//					populateForm(personFromBackend.get());
//				} else {
//					refreshGrid();
//				}
//			} else {
//				clearForm();
//			}
//		});

		// Configure Form
//		binder = new Binder<>(SingleCell.class);

		// Bind fields. This where you'd define e.g. validation rules
//		binder.bindInstanceFields(this);

//		cancel.addClickListener(e -> {
//			clearForm();
//			refreshGrid();
//		});
//
//		save.addClickListener(e -> {
//			try {
//				if (this.singleCell == null) {
//					this.singleCell = new SingleCell();
//				}
//				binder.writeBean(this.singleCell);
//				personService.update(this.singleCell);
//				clearForm();
//				refreshGrid();
//				Notification.show("SingleCell details stored.");
//			} catch (final ValidationException validationException) {
//				Notification.show("An exception happened while trying to store the SingleCell details.");
//			}
//		});

		final SplitLayout splitLayout = new SplitLayout();
		splitLayout.setSizeFull();

		createGridLayout(splitLayout);
//		createEditorLayout(splitLayout);

		add(splitLayout);
	}

//	private void createEditorLayout(SplitLayout splitLayout) {
//		final Div editorLayoutDiv = new Div();
//		editorLayoutDiv.setId("editor-layout");
//
//		final Div editorDiv = new Div();
//		editorDiv.setId("editor");
//		editorLayoutDiv.add(editorDiv);
//
//		final FormLayout formLayout = new FormLayout();
//		addFormItem(editorDiv, formLayout, firstName, "First name");
//		addFormItem(editorDiv, formLayout, lastName, "Last name");
//		addFormItem(editorDiv, formLayout, email, "Email");
//		addFormItem(editorDiv, formLayout, phone, "Phone");
//		addFormItem(editorDiv, formLayout, dateOfBirth, "Date of birth");
//		addFormItem(editorDiv, formLayout, occupation, "Occupation");
//		createButtonLayout(editorLayoutDiv);
//
//		splitLayout.addToSecondary(editorLayoutDiv);
//	}

//	private void createButtonLayout(Div editorLayoutDiv) {
//		final HorizontalLayout buttonLayout = new HorizontalLayout();
//		buttonLayout.setId("button-layout");
//		buttonLayout.setWidthFull();
//		buttonLayout.setSpacing(true);
//		cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
//		save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
//		buttonLayout.add(save, cancel);
//		editorLayoutDiv.add(buttonLayout);
//	}

	private void createGridLayout(SplitLayout splitLayout) {
		final Div wrapper = new Div();
		wrapper.setId("grid-wrapper");
		wrapper.setWidthFull();
		splitLayout.addToPrimary(wrapper);
		wrapper.add(grid);
	}

	private void addFormItem(Div wrapper, FormLayout formLayout, AbstractField field, String fieldName) {
		formLayout.addFormItem(field, fieldName);
		wrapper.add(formLayout);
		field.getElement().getClassList().add("full-width");
	}

	private void refreshGrid() {
		grid.select(null);
		grid.getDataProvider().refreshAll();
	}

//	private void clearForm() {
//		populateForm(null);
//	}

//	private void populateForm(SingleCell value) {
//		this.singleCell = value;
//		binder.readBean(this.singleCell);
//	}
}
