package edu.scripps.yates.pctsea.views.home;

import java.net.URL;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;

import edu.scripps.yates.pctsea.util.PCTSEALocalConfiguration;
import edu.scripps.yates.pctsea.views.main.MainView;

@Route(value = "home", layout = MainView.class)
@PageTitle("Home")
@CssImport("./styles/views/home/home-view.css")
@RouteAlias(value = "", layout = MainView.class)
public class HomeView extends Div {

	public HomeView() {
		setId("home-view");
		add(new H3("Welcome to the Proteomics Cell Type Set Enrichment Analysis (pCtSEA) web tool"));
		add(new Label("Go to Analyze tab to analyze your data"));

		String url = PCTSEALocalConfiguration.getPCTSEAResultsViewerURL();
		if (url != null) {
			try {
				final HorizontalLayout horizontal = new HorizontalLayout();
				url = new URL(url).toURI().toString();
				final Anchor link = new Anchor(url, url);
				link.setTarget("_blank");
				horizontal.removeAll();
				horizontal.setPadding(true);
				horizontal.add("If you already have your zipped results, you can go here: ");
				horizontal.add(link);
				add(horizontal);
			} catch (final Exception e) {

			}
		}

	}

}
