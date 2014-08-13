Tomcat MongoDB Persistent Session Manager
=====================

# Overview

This is an Apache Tomcat Persistent Session Manager implementation backed by MongoDB.  This work borrows heavily from the concepts laid out in the [Mongo-Tomcat-Sessions project by David Dawson](https://github.com/naviance/Mongo-Tomcat-Sessions) and applies them using Tomcat's `org.apache.catalina.session.PersistenceManager` and `org.apache.catalina.Store` interfaces.

## Quick Start

### Requirements

*  Tomcat 7.x (tested with Tomcat 7.0.47)
*  Java 1.6 or higher (tested with Java 1.7)
*  MongoDB Driver 2.11 or higher (tested with MongoDB Driver 2.11.4)

### Usage

* Copy the `mongo-session-manager.jar` and the MongoDB Driver into the `/lib` directory of your Tomcat installation.
* Configure the session manager:

Session Manager's are configured in Tomcat's `server.xml` or an application's `context.xml`.  The `MongoPersistentManager` supports all of the `PersistentManager` attributes.  For more information, please see the [Tomcat Documentation](http://tomcat.apache.org/tomcat-7.0-doc/config/manager.html)

```xml
<Manager className="org.hbr.session.manager.MongoPersistentManager"
    	distributable="true" maxIdleBackup="30">
	<Store className="org.hbr.session.store.MongoStore"
		hosts="localhost:27017"
		dbName="sessiontest"
		maxPoolSize="25"
	/>
</Manager>
```

#### Manager Properties.

For more information regarding the MongoDB specific properties, please see the [MongoDB Documentation](http://docs.mongodb.org/manual/reference/connection-string/).  **bold** properties are required.

 Attribute | Description |
 --------- | ----------- |
 connectionUri | A MongoDB Connection String.  If present, this will override all other connection options
 **hosts** | A comma separated list of MongoDB hosts and ports in `host:port` syntax 
 **dbName** | MongoDB Database name to use 
 collectionName | Name of the Collection to use.  Defaults to **tomcat.sessions** 
 username | If MongoDB is operating in Secure Mode, the username to authenticate with 
 password | If MongoDB is operating in Secure Mode, the password to authenticate with 
 connectionTimeoutMs | MongoDB Connection Timeout in Milliseconds.  Defaults to 0 or no timeout 
 connectionWaitTimeoutMs | MongoDB Wait Timeout in Milliseconds.  Controls how long the `MongoClient` waits for a free connection.  Defaults to 0 or no timeout 
 minPoolSize | Minimum Number of MongoDB Connections for this manager.  Defaults to 10 
 maxPoolSize | Maximum Number of MongoDB Connections for this manager.  Defaults to 25 
 replicaSet | Name of the MongoDB Replica Set 

## API Docs
API Docs are available here:

http://hbrgtech.github.io/mongo-session-manager/apidocs/

License: [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)

