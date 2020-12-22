package edu.scripps.yates.pctsea.views.about;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
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

		final String labURL = "http://www.scripps.edu/yates/";
		final Anchor lablink = new Anchor(labURL, "John Yates's laboratory");
		lablink.setTarget("_blank");

		final String scrippsURL = "http://www.scripps.edu/";
		final Anchor scrippsLink = new Anchor(scrippsURL, "Scripps Research Institute");
		scrippsLink.setTarget("_blank");

		final HorizontalLayout horizontalLinks = new HorizontalLayout();
		horizontalLinks.setPadding(true);
		horizontalLinks.add("Developed at the ");
		horizontalLinks.add(lablink);
		horizontalLinks.add(" at the ");
		horizontalLinks.add(scrippsLink);
		add(horizontalLinks);
		// link to github
		final String url = "https://github.com/proteomicsyates/pctsea-parent";
		final Anchor link = new Anchor(url, url);
		link.setTarget("_blank");
		final HorizontalLayout horizontal = new HorizontalLayout();
		horizontal.setPadding(true);
		horizontal.add("Source code available at: ");
		horizontal.add(link);
		add(horizontal);

	}

}
