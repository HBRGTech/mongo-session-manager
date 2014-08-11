/**
 * Copyright 2014 Harvard Business Publishing
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * 		http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
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
 * @author <a href="mailto:kdavis@hbr.org">Kevin Davis</a>
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
