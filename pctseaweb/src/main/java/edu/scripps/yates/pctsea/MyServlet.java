package edu.scripps.yates.pctsea;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;

import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.SessionDestroyEvent;
import com.vaadin.flow.server.SessionDestroyListener;
import com.vaadin.flow.server.SessionInitEvent;
import com.vaadin.flow.server.SessionInitListener;
import com.vaadin.flow.server.VaadinServlet;

public class MyServlet extends VaadinServlet implements SessionInitListener, SessionDestroyListener {
	private final static Logger log = Logger.getLogger(MyServlet.class);
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	protected void servletInitialized() throws ServletException {
		super.servletInitialized();
		getService().addSessionInitListener(this);
		getService().addSessionDestroyListener(this);
	}

	@Override
	public void sessionDestroy(SessionDestroyEvent event) {
		// TODO Auto-generated method stub
		log.info("Destroying session " + event.getSession().getSession().getId());
	}

	@Override
	public void sessionInit(SessionInitEvent event) throws ServiceException {
		// TODO Auto-generated method stub
		log.info("Init new session " + event.getSession().getSession().getId());
	}

}
