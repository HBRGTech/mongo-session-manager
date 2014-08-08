/**
 * 
 */
package org.hbr.session.store;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.apache.catalina.Container;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Session;
import org.apache.catalina.Store;
import org.apache.catalina.session.StoreBase;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
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
 * @author kdavis
 *
 */
public class MongoStore extends StoreBase {

	/**
	 * Property used to store the Session's last modified date.
	 */
	protected static final String lastModifiedProperty = "lastModified";
	
	/**
	 * Default Name of the Collection where the Sessions will be stored. 
	 */
	protected static final String collectionName = "tomcat.sessions";
	
	/**
     * The descriptive information about this implementation.
     */
    protected static final String info = "MongoStore/1.0";

    /**
     * Context or Web Application name associated with this Store
     */
    private String name = null;

    /**
     * Name to register for this Store, used for logging.
     */
    protected static String storeName = "MongoStore";

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
    protected String dbConnectionUri;
    
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
    protected String dbHosts;
    
    /**
     * Name of the MongoDB Database to use.
     */
    protected String dbName;
    
    /**
     * Name of the MongoDB Collection to store the sessions.
     * Defaults to <em>tomcat.sessions</em>
     */
    protected String dbCollectionName = collectionName;
    
    /**
     * MongoDB User.  Used if MongoDB is in <em>Secure</em> mode.
     */
    protected String dbUsername;
    
    /**
     * MongoDB password.  Used if MongoDB is in <em>Secure</em> mode
     */
    protected String dbPassword;
    
    /**
     * Connection Timeout in milliseconds.  Defaults to 0, or no timeout
     */
    protected int dbConnectionTimeoutMs = 0;
    
    /**
     * Connection Wait Timeout in milliseconds.  Defaults to 0, or no timeout
     */
    protected int dbConnectionWaitTimeoutMs = 0;
    
    /**
     * Minimum Number of connections the MongoClient will manage.
     * Defaults to 10.
     */
    protected int dbMinPoolSize = 10;
    
    /**
     * Maximum Number of connections the MongoClient will manage.
     * Defaults to 20
     */
    protected int dbMaxPoolSize = 20;
    
    /**
     * MongoDB replica set name.
     */
    protected String dbReplicaSet;
    
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
		Long count = this.collection.count();
		return count.intValue();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String[] keys() throws IOException {
		/* select all the ids from the collection */
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Session load(String id) throws ClassNotFoundException, IOException {
		/* locate the session, by id, in the collection */
		
		/* serialize into a session */
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void remove(String id) throws IOException {
		/* remove the session from the collection, by id */
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clear() throws IOException {
		/* remove all sessions for this context */

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void save(Session session) throws IOException {
		/* save the session into the collection */
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
	 * Create the {@link MongoClient}.
	 * @throws LifecycleException
	 */
	private void getConnection() throws LifecycleException {
		try {
			/* create our MongoClient */
			if (this.dbConnectionUri != null) {
				this.mongoClient = new MongoClient(this.dbConnectionUri);
			} else {
				/* create the client using the Mongo options */
				ReadPreference readPreference = ReadPreference.primaryPreferred();
				if (this.useSlaves) {
					readPreference = ReadPreference.secondaryPreferred();
				}
				MongoClientOptions options = MongoClientOptions.builder()
					.connectTimeout(dbConnectionTimeoutMs)
					.maxWaitTime(dbConnectionWaitTimeoutMs)
					.connectionsPerHost(dbMaxPoolSize)
					.writeConcern(writeConcern)
					.readPreference(readPreference)
					.build();
				
				/* build up the host list */
				List<ServerAddress> hosts = new ArrayList<ServerAddress>();
				String[] dbHosts = this.dbHosts.split(",");
				for(String dbHost: dbHosts) {
					String[] hostInfo = dbHost.split(":");
					ServerAddress address = new ServerAddress(hostInfo[0], Integer.parseInt(hostInfo[1]));
					hosts.add(address);
				}
				/* connect */				
				this.mongoClient = new MongoClient(hosts, options);
			}
			
			/* get a connection to our db */
			this.db = this.mongoClient.getDB(this.dbName);
			
			/* see if we need to authenticate */
			if (this.dbUsername != null || this.dbPassword != null) {
				if (!this.db.authenticate(this.dbUsername, this.dbPassword.toCharArray())) {
					throw new RuntimeException("MongoDB Authentication Failed");
				}
			}
			
			/* get a reference to the collection */
			this.collection = this.db.getCollection(this.dbCollectionName);
			
			/* make sure the last modified index exists */
			this.collection.ensureIndex(new BasicDBObject(lastModifiedProperty, 1));
		} catch (UnknownHostException uhe) {
			this.manager.getContainer().getLogger().error("Unable to Connect to MongoDB", uhe);
			throw new LifecycleException(uhe);
		} catch (MongoException me) {
			this.manager.getContainer().getLogger().error("Unable to Connect to MongoDB", me);
			throw new LifecycleException(me);
		}
	}


	/**
	 * @return the dbConnectionUri
	 */
	public String getDbConnectionUri() {
		return dbConnectionUri;
	}


	/**
	 * @param dbConnectionUri the dbConnectionUri to set
	 */
	public void setDbConnectionUri(String dbConnectionUri) {
		this.dbConnectionUri = dbConnectionUri;
	}


	/**
	 * @return the dbHosts
	 */
	public String getDbHosts() {
		return dbHosts;
	}


	/**
	 * @param dbHosts the dbHosts to set
	 */
	public void setDbHosts(String dbHosts) {
		this.dbHosts = dbHosts;
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
	 * @return the dbCollectionName
	 */
	public String getDbCollectionName() {
		return dbCollectionName;
	}


	/**
	 * @param dbCollectionName the dbCollectionName to set
	 */
	public void setDbCollectionName(String dbCollectionName) {
		this.dbCollectionName = dbCollectionName;
	}


	/**
	 * @return the dbUsername
	 */
	public String getDbUsername() {
		return dbUsername;
	}


	/**
	 * @param dbUsername the dbUsername to set
	 */
	public void setDbUsername(String dbUsername) {
		this.dbUsername = dbUsername;
	}


	/**
	 * @return the dbPassword
	 */
	public String getDbPassword() {
		return dbPassword;
	}


	/**
	 * @param dbPassword the dbPassword to set
	 */
	public void setDbPassword(String dbPassword) {
		this.dbPassword = dbPassword;
	}


	/**
	 * @return the dbConnectionTimeoutMs
	 */
	public int getDbConnectionTimeoutMs() {
		return dbConnectionTimeoutMs;
	}


	/**
	 * @param dbConnectionTimeoutMs the dbConnectionTimeoutMs to set
	 */
	public void setDbConnectionTimeoutMs(int dbConnectionTimeoutMs) {
		this.dbConnectionTimeoutMs = dbConnectionTimeoutMs;
	}


	/**
	 * @return the dbConnectionWaitTimeoutMs
	 */
	public int getDbConnectionWaitTimeoutMs() {
		return dbConnectionWaitTimeoutMs;
	}


	/**
	 * @param dbConnectionWaitTimeoutMs the dbConnectionWaitTimeoutMs to set
	 */
	public void setDbConnectionWaitTimeoutMs(int dbConnectionWaitTimeoutMs) {
		this.dbConnectionWaitTimeoutMs = dbConnectionWaitTimeoutMs;
	}


	/**
	 * @return the dbMinPoolSize
	 */
	public int getDbMinPoolSize() {
		return dbMinPoolSize;
	}


	/**
	 * @param dbMinPoolSize the dbMinPoolSize to set
	 */
	public void setDbMinPoolSize(int dbMinPoolSize) {
		this.dbMinPoolSize = dbMinPoolSize;
	}


	/**
	 * @return the dbMaxPoolSize
	 */
	public int getDbMaxPoolSize() {
		return dbMaxPoolSize;
	}


	/**
	 * @param dbMaxPoolSize the dbMaxPoolSize to set
	 */
	public void setDbMaxPoolSize(int dbMaxPoolSize) {
		this.dbMaxPoolSize = dbMaxPoolSize;
	}


	/**
	 * @return the dbReplicaSet
	 */
	public String getDbReplicaSet() {
		return dbReplicaSet;
	}


	/**
	 * @param dbReplicaSet the dbReplicaSet to set
	 */
	public void setDbReplicaSet(String dbReplicaSet) {
		this.dbReplicaSet = dbReplicaSet;
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
	public void setWriteConcern(String writeConcern) {
		WriteConcern concern = WriteConcern.valueOf(writeConcern);
		if (concern != null) {
			this.writeConcern = concern;
		}
	}
}
