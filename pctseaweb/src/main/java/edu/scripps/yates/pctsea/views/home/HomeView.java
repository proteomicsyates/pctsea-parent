package edu.scripps.yates.pctsea.views.home;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;

import edu.scripps.yates.pctsea.views.main.MainView;

@Route(value = "home", layout = MainView.class)
@PageTitle("Home")
@CssImport("./styles/views/home/home-view.css")
@RouteAlias(value = "", layout = MainView.class)
public class HomeView extends Div {

    public HomeView() {
        setId("home-view");
        add(new Label("Content placeholder"));
    }

}
