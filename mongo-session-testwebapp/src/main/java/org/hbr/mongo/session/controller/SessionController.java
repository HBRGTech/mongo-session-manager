/**
 * 
 */
package org.hbr.mongo.session.controller;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

/**
 * Simple Test Controller that creates a new session and adds stuff to it.
 * 
 * @author Kevin Davis
 *
 */
@Controller
public class SessionController {
	
	/**
	 * Test the Session
	 * @return
	 */
	@RequestMapping(value = "/get-session", method = RequestMethod.GET)
	public ModelAndView getSession(HttpSession session) {
		/* add a property to the session */
		session.setAttribute("TEST", "TEST");
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("id", session.getId());
		return new ModelAndView("displaySession", model);
	}
}
