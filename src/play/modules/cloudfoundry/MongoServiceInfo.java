package play.modules.cloudfoundry;

import org.cloudfoundry.runtime.env.AbstractServiceInfo;

import java.util.Map;


/**
 * Service info for Mongo.
 *
 * This class is a fork of {@link org.cloudfoundry.runtime.env.MongoServiceInfo}. It allows to extract the "dbName"
 * and the "userName" credential informations from the Cloud Foundry "VCAP_SERVICES" env variable.
 *
 * @author Ramnivas Laddad
 * @author Benoît Courtine, 2011-07-10 : added dbname and username informations.
 * @version 2011.07.10
 */
public class MongoServiceInfo extends AbstractServiceInfo {

    private String userName;

    private String dbName;

    /**
     * Extraction of MongoDB service informations from the serviceInfo {@link Map}.
     *
     * @param serviceInfo MongoDB informations, extracted from the "VCAP_SERVICES" JSON.
     */
    @SuppressWarnings("unchecked")
	public MongoServiceInfo(Map<String, Object> serviceInfo) {
		super(serviceInfo);

        Map<String,Object> credentials = (Map<String, Object>) serviceInfo.get("credentials");
        this.userName = (String) credentials.get("username");
        this.dbName = (String) credentials.get("db");
	}

    /**
     * @return MongoDB username for connection credentials.
     * @since 2011.07.10
     */
    public String getUserName() {
        return userName;
    }

    /**
     * @return MongoDB db name for connection credentials.
     * @since 2011.07.10
     */
    public String getDbName() {
        return dbName;
    }
}
