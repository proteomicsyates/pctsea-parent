package edu.scripps.yates.pctsea.views.main;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabVariant;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.PWA;

import edu.scripps.yates.pctsea.util.PCTSEALocalConfiguration;
import edu.scripps.yates.pctsea.util.VaadinUtil;
import edu.scripps.yates.pctsea.views.about.AboutView;
import edu.scripps.yates.pctsea.views.analyze.AnalyzeView;
import edu.scripps.yates.pctsea.views.home.HomeView;
import edu.scripps.yates.pctsea.views.results.CompareResultsView;

/**
 * The main view is a top-level placeholder for other views.
 */
@JsModule("./styles/shared-styles.js")
@CssImport(value = "./styles/views/main/main-view.css", themeFor = "vaadin-app-layout")
@CssImport("./styles/views/main/main-view.css")
@PWA(name = "PCTSEAweb", shortName = "PCTSEAweb", enableInstallPrompt = false)
@Push
public class MainView extends AppLayout {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8555562304002467379L;
	private final Tabs menu;

	public MainView() {
		final HorizontalLayout header = createHeader();
		menu = createMenuTabs();
		addToNavbar(createTopBar(header, menu));
	}

	private VerticalLayout createTopBar(HorizontalLayout header, Tabs menu) {
		final VerticalLayout layout = new VerticalLayout();
		layout.getThemeList().add("dark");
		layout.setWidthFull();
		layout.setSpacing(false);
		layout.setPadding(false);
		layout.setAlignItems(FlexComponent.Alignment.CENTER);
		layout.add(header, menu);
		return layout;
	}

	private HorizontalLayout createHeader() {
		final HorizontalLayout header = new HorizontalLayout();
		header.setPadding(false);
		header.setSpacing(false);
		header.setWidthFull();
		header.setAlignItems(FlexComponent.Alignment.CENTER);
		header.setId("header");
		final Image logo = new Image("images/logo.png", "pCtSEA Proteomics Cell Type Set Enrichment Analysis");
		logo.setId("logo");
		header.add(logo);
//        Image avatar = new Image("images/user.svg", "Avatar");
//        avatar.setId("avatar");
		header.add(new H1("PCTSEA: Proteomics Cell Type Set Enrichment Analysis"));
//        header.add(avatar);
		return header;
	}

	private static Tabs createMenuTabs() {
		final Tabs tabs = new Tabs();
		tabs.getStyle().set("max-width", "100%");
		tabs.add(getAvailableTabs());
		return tabs;
	}

	private static Tab[] getAvailableTabs() {
		return new Tab[] { createTab("Home", HomeView.class), createTab("Analyze", AnalyzeView.class),
				createTab("Compare results", CompareResultsView.class), createTab("About", AboutView.class) };
	}

	private static Tab createTab(String text, Class<? extends Component> navigationTarget) {
		final Tab tab = new Tab();
		tab.addThemeVariants(TabVariant.LUMO_ICON_ON_TOP);
		tab.add(new RouterLink(text, navigationTarget));
		ComponentUtil.setData(tab, Class.class, navigationTarget);
		return tab;
	}

	@Override
	protected void afterNavigation() {
		super.afterNavigation();
		getTabForComponent(getContent()).ifPresent(menu::setSelectedTab);
	}

	private Optional<Tab> getTabForComponent(Component component) {
		return menu.getChildren().filter(tab -> ComponentUtil.getData(tab, Class.class).equals(component.getClass()))
				.findFirst().map(Tab.class::cast);
	}

	@Override
	protected void onAttach(AttachEvent attachEvent) {
		// check configuration
		final String pctseaResultsViewerURL = PCTSEALocalConfiguration.getPCTSEAResultsViewerURL();
		if (pctseaResultsViewerURL == null) {
			final String text = "Error in configuration input file. \n pCtSEA final configuration error: Property '"
					+ PCTSEALocalConfiguration.resultsViewerURLProperty + "' on configuration file '"
					+ PCTSEALocalConfiguration.PCTSEA_CONF_FILE_NAME + "' is not found";
			VaadinUtil.showErrorDialog(text);
			throw new IllegalArgumentException(text);
		}
		try {
			new URL(pctseaResultsViewerURL).toURI().toString();
		} catch (MalformedURLException | URISyntaxException e) {
			VaadinUtil.showErrorDialog("Error in configuration input file: " + e.getMessage() + "\n"
					+ "pCtSEA configuration error: Property '" + PCTSEALocalConfiguration.resultsViewerURLProperty
					+ "' on configuration file '" + PCTSEALocalConfiguration.PCTSEA_CONF_FILE_NAME
					+ "' is a malformed URL");
			throw new RuntimeException(e);

		}
		final String pctseaResultsComparisonsURL = PCTSEALocalConfiguration.getPCTSEAResultsComparisonURL();
		if (pctseaResultsComparisonsURL == null) {
			final String text = "Error in configuration input file\n" + "pCtSEA configuration error: Property '"
					+ PCTSEALocalConfiguration.resultsComparisonsURLProperty + "' on configuration file '"
					+ PCTSEALocalConfiguration.PCTSEA_CONF_FILE_NAME + "' is not found";
			VaadinUtil.showErrorDialog(text);
			throw new IllegalArgumentException(text);
		}
		try {
			new URL(pctseaResultsComparisonsURL).toURI().toString();
		} catch (MalformedURLException | URISyntaxException e) {
			VaadinUtil.showErrorDialog("Error in configuration input file: " + e.getMessage() + "\n"
					+ "pCtSEA configuration error: Property '" + PCTSEALocalConfiguration.resultsComparisonsURLProperty
					+ "' on configuration file '" + PCTSEALocalConfiguration.PCTSEA_CONF_FILE_NAME
					+ "' is a malformed URL");
			throw new RuntimeException(e);

		}
		super.onAttach(attachEvent);
	}

	@Override
	protected void onDetach(DetachEvent detachEvent) {
		super.onDetach(detachEvent);
	}
}
