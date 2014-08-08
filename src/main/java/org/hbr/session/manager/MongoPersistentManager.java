/**
 * 
 */
package org.hbr.session.manager;

import org.apache.catalina.Manager;
import org.apache.catalina.session.PersistentManagerBase;
import org.hbr.session.store.MongoStore;

/**
 * {@link Manager} implementation that uses a {@link MongoStore}
 * 
 * @author kdavis
 *
 */
public final class MongoPersistentManager extends PersistentManagerBase {

	/**
     * The descriptive information about this implementation.
     */
    private static final String info = "MongoPersistentManager/1.0";

    /**
     * The descriptive name of this Manager implementation (for logging).
     */
    protected static String name = "MongoPersistentManager";
    
    /**
     * Return descriptive information about this Manager implementation and
     * the corresponding version number, in the format
     * <code>&lt;description&gt;/&lt;version&gt;</code>.
     */
    @Override
    public String getInfo() {
        return (info);
    }

    /**
     * Return the descriptive short name of this Manager implementation.
     */
    @Override
    public String getName() {
        return (name);
    }
}
