package edu.scripps.yates.pctsea.views.about;

import java.text.SimpleDateFormat;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.details.DetailsVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import edu.scripps.yates.pctsea.db.PctseaRunLog;
import edu.scripps.yates.pctsea.db.PctseaRunLogRepository;
import edu.scripps.yates.pctsea.util.PCTSEAConfigurationException;
import edu.scripps.yates.pctsea.util.PCTSEALocalConfiguration;
import edu.scripps.yates.pctsea.views.main.MainView;
import edu.scripps.yates.utilities.dates.DatesUtil;

@Route(value = "about", layout = MainView.class)
@PageTitle("About")
@CssImport("./styles/views/about/about-view.css")
public class AboutView extends Div {
	@Autowired
	private PctseaRunLogRepository runRepo;

	public AboutView() {
		setId("about-view");
		add(new Label("Proteomics Cell Type Set Enrichment Analysis (PCTSEA)"));

		final Image logo = new Image("images/yateslab.png", "pCtSEA Proteomics Cell Type Set Enrichment Analysis");
//		logo.setHeight("225px");
//		logo.setWidth("225px");

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
		add(logo);
		// link to github
		final String url = "https://github.com/proteomicsyates/pctsea-parent";
		final Anchor link = new Anchor(url, url);
		link.setTarget("_blank");
		final HorizontalLayout horizontal = new HorizontalLayout();
		horizontal.setPadding(true);
		horizontal.add("Source code available at: ");
		horizontal.add(link);
		add(horizontal);

		try {

			final String pctseaResultsViewerURL = PCTSEALocalConfiguration.getPCTSEAResultsViewerURL();
			final Details details = new Details();
			details.setSummaryText("Server status");
			details.addContent(new Text("Viewer of results located at: " + pctseaResultsViewerURL),
					new Text("Emails with the results will be sent by: " + PCTSEALocalConfiguration.getFromEmail()),
					new Text("Results are stored at: " + PCTSEALocalConfiguration.getPCTSEAResultsFolder()));
			details.addThemeVariants(DetailsVariant.SMALL);
			add(details);
		} catch (final PCTSEAConfigurationException e) {
			add(new Label("There is some error in this server: " + e.getMessage()));
		}
		// runs
		final Details detailsRuns = new Details();
		detailsRuns.setSummaryText("Runs log");
		final List<PctseaRunLog> runs = runRepo.findAll();
		final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
		for (int i = runs.size() - 1; i >= 0; i--) {
			final PctseaRunLog run = runs.get(i);
			final String started = dateFormat.format(run.getStarted());
			final String finished = dateFormat.format(run.getFinished());
			final String timeRunning = DatesUtil.getDescriptiveTimeFromMillisecs(run.getRunningTime());
			detailsRuns.addContent(new Text("Run " + run.getId() + ", started: " + started + ", finished: " + finished
					+ ", run time: " + timeRunning + " # input genes: " + run.getNumInputGenes()));
			detailsRuns.addThemeVariants(DetailsVariant.SMALL);
		}
		add(detailsRuns);
	}

}
