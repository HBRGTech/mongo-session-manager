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
package org.hbr.session.store;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.apache.catalina.Session;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.session.StandardSession;
import org.hbr.session.manager.MongoPersistentManager;
import org.junit.Before;
import org.junit.Test;

/**
 * JUnit Test Case for {@link MongoStore}.  Uses an embedded MongoDB for testing.
 * 
 * @author <a href="mailto:kdavis@hbr.org">Kevin Davis</a>
 *
 */
public class MongoStoreTest {
	
	/** Random Session */
	private String sessionId = UUID.randomUUID().toString();
	
	/** Mongo Store */
	private MongoStore mongoStore;
	
	/** Test Session Instance */
	private StandardSession testSession;
	
	/** Session Manager */
	private MongoPersistentManager manager = new MongoPersistentManager();

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		/* set up the manager */
		this.manager.setContainer(new StandardContext());
		this.manager.getContainer().setName("test");
		this.manager.getContainer().setParent(new StandardEngine());
		this.manager.getContainer().getParent().setName("parent");
		
		
		/* create the store */
		this.mongoStore = new MongoStore();
		this.mongoStore.setHosts("127.0.0.1:27017");
		this.mongoStore.setDbName("unitest");
		this.mongoStore.setManager(manager);
		
		this.manager.setStore(mongoStore);
		
		/* initialize the store */
		this.manager.start();
		
		/* create the test session */
		this.testSession = (StandardSession)this.manager.createSession(this.sessionId);
		
		/* add some data */
		this.testSession.setAttribute("test", "test", false);		
	}

	/**
	 * Test method for {@link org.hbr.session.store.MongoStore#getSize()}.
	 */
	@Test
	public void testGetSize() throws Exception {
		/* get the size.  this test passes as long as there is no exception */
		this.mongoStore.getSize();		
	}

	/**
	 * Test method for {@link org.hbr.session.store.MongoStore#keys()}.
	 */
	@Test
	public void testKeys() throws Exception {
		/* get the keys.  this should always return a list, even if it's empty */
		String[] keys = this.mongoStore.keys();
		assertNotNull(keys);
	}

	/**
	 * Test method for {@link org.hbr.session.store.MongoStore#load(java.lang.String)}.
	 */
	@Test
	public void testLoadNotFound() throws Exception {
		/* load a session we know does not exist */
		Session session = this.mongoStore.load("0000000000");
		assertNull(session);
	}

	/**
	 * Test method for {@link org.hbr.session.store.MongoStore#remove(java.lang.String)}.
	 */
	@Test
	public void testRemove() throws Exception {
		/* remove the test session, this should succeed with no exception */
		this.mongoStore.remove(this.sessionId);
	}

	/**
	 * Test method for {@link org.hbr.session.store.MongoStore#clear()}.
	 */
	@Test
	public void testClear() throws Exception {
		/* clear the store */
		this.mongoStore.clear();
		
		/* verify the store is empty */
		int size = this.mongoStore.getSize();
		assertTrue(size == 0);
	}

	/**
	 * Test method for {@link org.hbr.session.store.MongoStore#save(org.apache.catalina.Session)}.
	 * and {@link org.hbr.session.store.MongoStore#load(java.lang.String)}
	 */
	@Test
	public void testLoadAndSave() throws Exception {
		/* save our session */
		this.mongoStore.save(this.testSession);
		
		/* load the session */
		Session session = this.mongoStore.load(this.sessionId);
		assertNotNull(session);
		assertNotNull(((StandardSession)session).getAttribute("test"));
	}
}
