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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.catalina.Container;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Loader;
import org.apache.catalina.Session;
import org.apache.catalina.Store;
import org.apache.catalina.session.StandardSession;
import org.apache.catalina.session.StoreBase;
import org.apache.catalina.util.CustomObjectInputStream;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoException;
import com.mongodb.ReadPreference;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;

/**
 * Tomcat {@link Store} implementation backed by MongoDB.
 * 
 * @author <a href="mailto:kdavis@hbr.org">Kevin Davis</a>
 *
 */
public class MongoStore extends StoreBase {

	/**
	 * Property used to store the Session's last modified date.
	 */
	protected static final String appContextProperty = "app";
	
	/**
	 * Property used to store the Session's last modified date.
	 */
	protected static final String lastModifiedProperty = "lastModified";
	
	/**
	 * Property used to store the Session's creation date.
	 */
	protected static final String creationTimeProperty = "creationTime";
	
	/**
	 * Property used to store the Session's data.
	 */
	protected static final String sessionDataProperty = "data";
	
	/**
	 * Default Name of the Collection where the Sessions will be stored. 
	 */
	protected static final String sessionCollectionName = "tomcat.sessions";
	
	/**
     * The descriptive information about this implementation.
     */
    protected static final String info = "MongoStore/1.0";
    
    /**
     * Name to register for this Store, used for logging.
     */
    protected static String storeName = "MongoStore";

    /**
     * Context or Web Application name associated with this Store
     */
    private String name = null;  

    /**
     * Name to register for the background thread.
     */
    protected String threadName = "MongoStore";   
    
    /**
     * MongoDB Connection URI.  This will override all other connection settings
     * specified.  For more information, please see
     * <a href="http://api.mongodb.org/java/current/com/mongodb/MongoClientURI.html">http://api.mongodb.org/java/current/com/mongodb/MongoClientURI.html</a>
     * @see MongoClientURI
     */
    protected String connectionUri;
    
    /**
     * MongoDB Hosts.  This can be a single host or a comma separated list
     * using a [host:port] format.  Lists are typically used for replica sets.
     * This value is ignored if the <em>dbConnectionUri</em> is provided.
     * <p>
     * 	<pre>
     * 		127.0.0.1:27001,127.0.0.1:27002
     * 	</pre>
     * </p> 
     */
    protected String hosts;
    
    /**
     * Name of the MongoDB Database to use.
     */
    protected String dbName;
    
    /**
     * Name of the MongoDB Collection to store the sessions.
     * Defaults to <em>tomcat.sessions</em>
     */
    protected String collectionName = sessionCollectionName;
    
    /**
     * MongoDB User.  Used if MongoDB is in <em>Secure</em> mode.
     */
    protected String username;
    
    /**
     * MongoDB password.  Used if MongoDB is in <em>Secure</em> mode
     */
    protected String password;
    
    /**
     * Connection Timeout in milliseconds.  Defaults to 0, or no timeout
     */
    protected int connectionTimeoutMs = 0;
    
    /**
     * Connection Wait Timeout in milliseconds.  Defaults to 0, or no timeout
     */
    protected int connectionWaitTimeoutMs = 0;
    
    /**
     * Minimum Number of connections the MongoClient will manage.
     * Defaults to 10.
     */
    protected int minPoolSize = 10;
    
    /**
     * Maximum Number of connections the MongoClient will manage.
     * Defaults to 20
     */
    protected int maxPoolSize = 20;
    
    /**
     * MongoDB replica set name.
     */
    protected String replicaSet;
    
    /**
     * Time to Live for the data in Mongo
     */
    protected int timeToLive = -1;
    
    /**
     * Controls if the MongoClient will use SSL.  Defaults to false.
     */
    protected boolean useSecureConnection = false;
    
    /**
     * Controls if the MongoClient will write to slaves.  Equivalent
     * to <em>slaveOk</em>.  Defaults to false.
     */
    protected boolean useSlaves = false;
    
    /**
     * Controls what {@link WriteConcern} the MongoClient will use.
     * Defaults to "SAFE"
     */
    protected WriteConcern writeConcern = WriteConcern.SAFE;
    
    /**
     * {@link MongoClient} instance to use.
     */
    protected MongoClient mongoClient;
    
    /**
     * Mongo DB reference
     */
    protected DB db;
    
    /**
     * Mongo Collection for the Sessions
     */
    protected DBCollection collection;
    
    /**
     * Retrieve the unique Context name for this Manager.  This will
     * be used to separate out sessions from different application
     * Contexts.
     * 
     * @return String unique name for this application Context
     */
    protected String getName() {
    	/* intialize the app name for this context */
    	if (this.name == null) {
    		/* get the container */
            Container container = this.manager.getContainer();
            
            /* determine the context name from the container */
            String contextName = container.getName();
            if (!contextName.startsWith("/")) {
                contextName = "/" + contextName;
            }
            
            String hostName = "";
            String engineName = "";

            /* if this is a sub container, get the parent name */
            if (container.getParent() != null) {
                Container host = container.getParent();
                hostName = host.getName();
                if (host.getParent() != null) {
                    engineName = host.getParent().getName();
                }
            }
            
            /* construct the unique context name */
            this.name = "/" + engineName + "/" + hostName + contextName;
        }
    	/* return the name */
        return this.name;
    }
    
    
	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getSize() throws IOException {		
		/* count the items in this collection for this app */
		Long count = this.collection.count(new BasicDBObject(appContextProperty, this.getName()));
		return count.intValue();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String[] keys() throws IOException {
		/* create the empty array list */
		List<String> keys = new ArrayList<String>();
		
		/* build the query */
		BasicDBObject sessionKeyQuery = new BasicDBObject();
		sessionKeyQuery.put(appContextProperty, this.getName());
		
		/* get the list */
		DBCursor mongoSessionKeys = this.collection.find(sessionKeyQuery, new BasicDBObject("_id", 1));
		while(mongoSessionKeys.hasNext()) {
			String id = mongoSessionKeys.next().get("_id").toString();
			keys.add(id);
		}
		
		/* return the array */
		return keys.toArray(new String[keys.size()]);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Session load(String id) throws ClassNotFoundException, IOException {
		/* default session */
		StandardSession session = null;
		
		/* get a reference to the container */
		Container container = manager.getContainer();
		
		/* store a reference to the old class loader, as we will change this thread's
		 * current context if we need to load custom classes
		 */
		ClassLoader managerContextLoader = Thread.currentThread().getContextClassLoader();
		ClassLoader appContextLoader = null;
		
		/* locate the session, by id, in the collection */
		BasicDBObject sessionQuery = new BasicDBObject();
		sessionQuery.put("_id", id);
		sessionQuery.put(appContextProperty, this.getName());
		
		/* lookup the session */
		DBObject mongoSession = this.collection.findOne(sessionQuery);
		if (mongoSession != null) {
			/* get the properties from mongo */			
			byte[] data = (byte[])mongoSession.get(sessionDataProperty);
			
			if (data != null) {
				BufferedInputStream bis = null;
				ObjectInputStream ois = null;
				try {
					/* load the data into an input stream */					
					bis = new BufferedInputStream(new ByteArrayInputStream(data));					
					
					/* determine which class loader to use when reading the object */
					Loader loader = null;
					if (container != null) {
						loader = container.getLoader();
						if (loader != null) {
							/* get the class loader for the container */
							appContextLoader = loader.getClassLoader();
							
							/* update the thread's class loader before reading the 
							 * object
							 */
							Thread.currentThread().setContextClassLoader(appContextLoader);

							/* use a custom object stream to read our object */
                            ois = new CustomObjectInputStream(bis,
                            		appContextLoader);
						} else {
							/* regular input stream */
							ois = new ObjectInputStream(bis);
						}
					}
					
					/* create a new session */
					session = (StandardSession)this.manager.createEmptySession();
					session.readObjectData(ois);
					session.setManager(this.manager);
				} finally {
					if (ois != null) {
						try {
							ois.close();
							ois = null;
						} catch (Exception e) {}
					}
					if (bis != null) {
						try {
							bis.close();
							bis = null;
						} catch (Exception e) {}
					}
					
					/* restore the class loader */
					Thread.currentThread().setContextClassLoader(managerContextLoader);
				}
			}
		}
		
		/* return the session */
		return session;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void remove(String id) throws IOException {
		/* build up the query, looking for all sessions with this app context property and id */
		BasicDBObject sessionQuery = new BasicDBObject();
		sessionQuery.put("_id", id);
		sessionQuery.put(appContextProperty, this.getName());
		
		/* remove all sessions for this context and id */
		try {
			this.collection.remove(sessionQuery);
		} catch (MongoException e) {
			/* for some reason we couldn't remove the data */
			this.manager.getContainer().getLogger().fatal(
					"Unable to remove sessions for [" + id + ":" + this.getName() + "] from MongoDB", e);
			throw e;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clear() throws IOException {
		/* build up the query, looking for all sessions with this app context property */
		BasicDBObject sessionQuery = new BasicDBObject();
		sessionQuery.put(appContextProperty, this.getName());
		
		/* remove all sessions for this context */
		try {
			this.collection.remove(sessionQuery);
		} catch (MongoException e) {
			/* for some reason we couldn't save the data */
			this.manager.getContainer().getLogger().fatal("Unable to remove sessions for [" + this.getName() + "] from MongoDB", e);
			throw e;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void save(Session session) throws IOException {
		/* we will store the session data as a byte array in Mongo, so
		 * we need to set up our output streams
		 */
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(bos);
		
		/* serialize the session using the object output stream */		
		((StandardSession)session).writeObjectData(oos);
		
		/* get the byte array of the data */
		byte[] data = bos.toByteArray();
		
		/* close the streams */
		try {
			oos.close();
			oos = null;
		} catch (Exception e) {}
		
		/* create the DBObject */
		BasicDBObject mongoSession = new BasicDBObject();
		mongoSession.put("_id", session.getIdInternal());
		mongoSession.put(appContextProperty, this.getName());
		mongoSession.put(creationTimeProperty, session.getCreationTime());
		mongoSession.put(sessionDataProperty, data);
		mongoSession.put(lastModifiedProperty, Calendar.getInstance().getTime());
		
		/* create our upsert lookup */
		BasicDBObject sessionQuery = new BasicDBObject();
		sessionQuery.put("_id", session.getId());
		try {
			/* update the object in the collection, inserting if necessary */
			this.collection.update(sessionQuery, mongoSession, true, false);
		} catch (MongoException e) {
			/* for some reason we couldn't save the data */
			this.manager.getContainer().getLogger().fatal("Unable to save session to MongoDB", e);
			throw e;
		} finally {
			if (oos != null) {
				try {
					oos.close();
					oos = null;
				} catch (Exception e) {}
			}
			if (bos != null) {
				try {
					bos.close();
					bos = null;
				} catch (Exception e) {}
			}
		}
	}

	/**
	 * Initialize this Store by connecting to the MongoDB using the
	 * configuration parameters supplied.
	 */
	@Override
	protected void initInternal()  {
		super.initInternal();
		try {
			this.getConnection();
		} catch (LifecycleException le) {
			throw new RuntimeException(le);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void destroyInternal() {		
		super.destroyInternal();
		
		/* close the mongo client */
		this.mongoClient.close();		
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected synchronized void startInternal() throws LifecycleException {		
		super.startInternal();
		
		/* verify that the collection reference is valid */
		if (this.collection == null) {
			this.getConnection();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected synchronized void stopInternal() throws LifecycleException {		
		super.stopInternal();
	}

	/**
     * Return the name for this Store, used for logging.
     */
    @Override
    public String getStoreName() {
        return (storeName);
    }
	
	/**
	 * Create the {@link MongoClient}.
	 * @throws LifecycleException
	 */
	private void getConnection() throws LifecycleException {
		try {
			/* create our MongoClient */
			if (this.connectionUri != null) {
				manager.getContainer().getLogger().info(getStoreName() + "[" + this.getName() + "]: Connecting to MongoDB [" + this.connectionUri + "]");
				this.mongoClient = new MongoClient(this.connectionUri);
			} else {
				/* create the client using the Mongo options */
				ReadPreference readPreference = ReadPreference.primaryPreferred();
				if (this.useSlaves) {
					readPreference = ReadPreference.secondaryPreferred();
				}
				MongoClientOptions options = MongoClientOptions.builder()
					.connectTimeout(connectionTimeoutMs)
					.maxWaitTime(connectionWaitTimeoutMs)
					.connectionsPerHost(maxPoolSize)
					.writeConcern(writeConcern)
					.readPreference(readPreference)
					.build();
				
				/* build up the host list */
				List<ServerAddress> hosts = new ArrayList<ServerAddress>();
				String[] dbHosts = this.hosts.split(",");
				for(String dbHost: dbHosts) {
					String[] hostInfo = dbHost.split(":");
					ServerAddress address = new ServerAddress(hostInfo[0], Integer.parseInt(hostInfo[1]));
					hosts.add(address);
				}
				
				this.manager.getContainer().getLogger().info(getStoreName() + "[" + this.getName() + "]: Connecting to MongoDB [" + this.hosts + "]");
				
				/* connect */				
				this.mongoClient = new MongoClient(hosts, options);
			}
			
			/* get a connection to our db */
			this.manager.getContainer().getLogger().info(getStoreName() + "[" + this.getName() + "]: Using Database [" + this.dbName + "]");
			this.db = this.mongoClient.getDB(this.dbName);
			
			/* see if we need to authenticate */
			if (this.username != null || this.password != null) {
				this.manager.getContainer().getLogger().info(getStoreName() + "[" + this.getName() + "]: Authenticating using [" + this.username + "]");
				if (!this.db.authenticate(this.username, this.password.toCharArray())) {
					throw new RuntimeException("MongoDB Authentication Failed");
				}
			}
			
			/* get a reference to the collection */
			this.collection = this.db.getCollection(this.collectionName);			
			this.manager.getContainer().getLogger().info(getStoreName() + "[" + this.getName() + "]: Preparing indexes");
			
			/* drop any existing indexes */
			this.collection.dropIndex(new BasicDBObject(lastModifiedProperty, 1));
			this.collection.dropIndex(new BasicDBObject(appContextProperty, 1));
			
			/* make sure the last modified and app name indexes exists */			
			this.collection.ensureIndex(new BasicDBObject(appContextProperty, 1));
			
			/* determine if we need to expire our db sessions */
			if (this.timeToLive != -1) {
				/* use the time to live set */
				this.collection.ensureIndex(new BasicDBObject(lastModifiedProperty, 1), 
						new BasicDBObject("lastModifiedProperty", this.timeToLive));	
			} else {
				/* no custom time to live specified, use the manager's settings */
				if (this.manager.getMaxInactiveInterval() != -1) {
					/* create a ttl index on the app property */
					this.collection.ensureIndex(new BasicDBObject(lastModifiedProperty, 1), 
							new BasicDBObject("lastModifiedProperty", this.manager.getMaxInactiveInterval()));	
				} else {
					/* create a regular index */
					this.collection.ensureIndex(new BasicDBObject(lastModifiedProperty, 1));
				}
			}
			
			this.manager.getContainer().getLogger().info(getStoreName() + "[" + this.getName() + "]: Store ready.");
		} catch (UnknownHostException uhe) {
			this.manager.getContainer().getLogger().error("Unable to Connect to MongoDB", uhe);
			throw new LifecycleException(uhe);
		} catch (MongoException me) {
			this.manager.getContainer().getLogger().error("Unable to Connect to MongoDB", me);
			throw new LifecycleException(me);
		}
	}


	/**
	 * @return the connectionUri
	 */
	public String getConnectionUri() {
		return connectionUri;
	}


	/**
	 * @param connectionUri the connectionUri to set
	 */
	public void setConnectionUri(String connectionUri) {
		this.connectionUri = connectionUri;
	}


	/**
	 * @return the hosts
	 */
	public String getHosts() {
		return hosts;
	}


	/**
	 * @param hosts the hosts to set
	 */
	public void setHosts(String hosts) {
		this.hosts = hosts;
	}


	/**
	 * @return the dbName
	 */
	public String getDbName() {
		return dbName;
	}


	/**
	 * @param dbName the dbName to set
	 */
	public void setDbName(String dbName) {
		this.dbName = dbName;
	}


	/**
	 * @return the collectionName
	 */
	public String getCollectionName() {
		return collectionName;
	}


	/**
	 * @param collectionName the collectionName to set
	 */
	public void setCollectionName(String collectionName) {
		this.collectionName = collectionName;
	}


	/**
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}


	/**
	 * @param username the username to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}


	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}


	/**
	 * @param password the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}


	/**
	 * @return the connectionTimeoutMs
	 */
	public int getConnectionTimeoutMs() {
		return connectionTimeoutMs;
	}


	/**
	 * @param connectionTimeoutMs the connectionTimeoutMs to set
	 */
	public void setConnectionTimeoutMs(int connectionTimeoutMs) {
		this.connectionTimeoutMs = connectionTimeoutMs;
	}


	/**
	 * @return the connectionWaitTimeoutMs
	 */
	public int getConnectionWaitTimeoutMs() {
		return connectionWaitTimeoutMs;
	}


	/**
	 * @param connectionWaitTimeoutMs the connectionWaitTimeoutMs to set
	 */
	public void setConnectionWaitTimeoutMs(int connectionWaitTimeoutMs) {
		this.connectionWaitTimeoutMs = connectionWaitTimeoutMs;
	}


	/**
	 * @return the minPoolSize
	 */
	public int getMinPoolSize() {
		return minPoolSize;
	}


	/**
	 * @param minPoolSize the minPoolSize to set
	 */
	public void setMinPoolSize(int minPoolSize) {
		this.minPoolSize = minPoolSize;
	}


	/**
	 * @return the maxPoolSize
	 */
	public int getMaxPoolSize() {
		return maxPoolSize;
	}


	/**
	 * @param maxPoolSize the maxPoolSize to set
	 */
	public void setMaxPoolSize(int maxPoolSize) {
		this.maxPoolSize = maxPoolSize;
	}


	/**
	 * @return the replicaSet
	 */
	public String getReplicaSet() {
		return replicaSet;
	}


	/**
	 * @param replicaSet the replicaSet to set
	 */
	public void setReplicaSet(String replicaSet) {
		this.replicaSet = replicaSet;
	}


	/**
	 * @return the useSecureConnection
	 */
	public boolean isUseSecureConnection() {
		return useSecureConnection;
	}


	/**
	 * @param useSecureConnection the useSecureConnection to set
	 */
	public void setUseSecureConnection(boolean useSecureConnection) {
		this.useSecureConnection = useSecureConnection;
	}


	/**
	 * @return the useSlaves
	 */
	public boolean isUseSlaves() {
		return useSlaves;
	}


	/**
	 * @param useSlaves the useSlaves to set
	 */
	public void setUseSlaves(boolean useSlaves) {
		this.useSlaves = useSlaves;
	}


	/**
	 * @return the writeConcern
	 */
	public WriteConcern getWriteConcern() {
		return writeConcern;
	}


	/**
	 * @param writeConcern the writeConcern to set
	 */
	public void setWriteConcern(WriteConcern writeConcern) {
		this.writeConcern = writeConcern;
	}


	/**
	 * @return the timeToLive
	 */
	public int getTimeToLive() {
		return timeToLive;
	}


	/**
	 * @param timeToLive the timeToLive to set
	 */
	public void setTimeToLive(int timeToLive) {
		this.timeToLive = timeToLive;
	}	
}
