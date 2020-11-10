package edu.scripps.yates.pctsea.views.about;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import edu.scripps.yates.pctsea.views.main.MainView;

@Route(value = "about", layout = MainView.class)
@PageTitle("About")
@CssImport("./styles/views/about/about-view.css")
public class AboutView extends Div {

	public AboutView() {
		setId("about-view");
		add(new Label("Proteomics Cell Type Set Enrichment Analysis (pCtSEA)"));
	}

}
