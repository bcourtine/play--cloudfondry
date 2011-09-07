package play.modules.cloudfoundry;

import org.cloudfoundry.runtime.env.CloudEnvironment;
import org.cloudfoundry.runtime.env.MongoServiceInfo;
import org.cloudfoundry.runtime.env.MysqlServiceInfo;
import org.cloudfoundry.runtime.env.PostgresqlServiceInfo;
import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.db.DB;

import java.util.List;
import java.util.Properties;


/**
 * Configuration of Play MySQL DB from Cloud Foundry VCAP_SERVICES env variable, using the cloudfoundry-runtime library.
 *
 * TODO Manage all CloudFoundry services (Redis, RabbitMQ, etc.).
 *
 * @author Benoît Courtine.
 * @version 2011.09.07
 */
public class CloudFoundryDBPlugin extends PlayPlugin {

    public static final String MYSQL_DRIVER = "com.mysql.jdbc.Driver";

	public static final String POSTGRESQL_DRIVER = "org.postgresql.Driver";

    public CloudEnvironment cloudEnvironment = new CloudEnvironment();

    public List<MysqlServiceInfo> mysqlServices = cloudEnvironment.getServiceInfos(MysqlServiceInfo.class);

    public List<MongoServiceInfo> mongoServices = cloudEnvironment.getServiceInfos(MongoServiceInfo.class);

	public List<PostgresqlServiceInfo> pgsqlServices = cloudEnvironment.getServiceInfos(PostgresqlServiceInfo.class);

    /**
     * Update of Play configuration {@link Play#configuration} from Cloud Foundry env variable.
     * These configuration will be used by {@link play.db.DBPlugin} to configure the {@link DB#datasource}.
     */
    @Override
    public void onApplicationStart() {

        Properties p = Play.configuration;

        mysqlServiceConfig(p);
        mongoDBServiceConfig(p);
    }

    /**
     * Configuration of MySQL, if at least one CloudFoundry MySQL service is bound to the instance.
     *
     * @param p Play configuration.
     * @since 2011.05.04
     */
    private void mysqlServiceConfig(Properties p) {

        // Check that a MySQL service is available.
        if (mysqlServices.size() == 0) {
            Logger.info("[CloudFoundry] There is no MySQL service bound to this application instance.");
            return;
        }

        // We configure the Cloud Foundry MySQL database only if no other DB is configured.
        if (p.containsKey("db") || p.containsKey("db.url")) {
            Logger.warn("[CloudFoundry] A MySQL database configuration already exists. It will not be overriden.");
            return;
        }

        // Check that only one MySQL service is available. It is a non-blocking check.
        if (mysqlServices.size() > 1) {
            Logger.warn("[CloudFoundry] There is more than one MySQL service bind to this application instance. Only the first will be used.");
        }

        MysqlServiceInfo mysqlServiceInfo = mysqlServices.get(0);

        // Update of Play configuration. Theses properties will be used by the DBPlugin.
        p.put("db.driver", MYSQL_DRIVER);
        p.put("db.url", mysqlServiceInfo.getUrl());
        p.put("db.user", mysqlServiceInfo.getUserName());
        p.put("db.pass", mysqlServiceInfo.getPassword());
    }

    /**
     * Configuration of MongoDB, if at least one CloudFoundry MongoDB service is bound to the instance.
     *
     * @param p Play configuration.
     * @since 2011.07.11
     */
    private void mongoDBServiceConfig(Properties p) {

        // Check that a MongoDB service is available.
        if (mongoServices.size() == 0) {
            Logger.info("[CloudFoundry] There is no MongoDB service bound to this application instance.");
            return;
        }

        // We configure the Cloud Foundry MongoDB database only if no other DB is configured.
        if (p.containsKey("morphia.db.host")) {
            Logger.warn("[CloudFoundry] A MongoDB database configuration already exists. It will not be overriden.");
            return;
        }

        // Check that only one MongoDB service is available. It is a non-blocking check.
        if (mongoServices.size() > 1) {
            Logger.warn("[CloudFoundry] There is more than one MongoDB service bind to this application instance. Only the first will be used.");
        }

        MongoServiceInfo mongoServiceInfo = mongoServices.get(0);

        morphiaPluginConfig(p, mongoServiceInfo);
        mongoPluginConfig(p, mongoServiceInfo);
    }

    /**
     * Configuration of Play Morphia plugin for MongoDB.
     *
     * <strong>The Morphia plugin uses the db name at compile time. In order to work, the db must be
     * configured in Play configuration file: <code>morphia.db.name=db</code></strong>
     *
     * @param p Play configuration to update.
     * @param mongoServiceInfo Information about Cloud Foundry MongoDB service.
     * @since 2011.07.11
     */
    private void morphiaPluginConfig(Properties p, MongoServiceInfo mongoServiceInfo) {
        p.put("morphia.db.host", mongoServiceInfo.getHost());
        p.put("morphia.db.port", Integer.toString(mongoServiceInfo.getPort()));
        p.put("morphia.db.name", mongoServiceInfo.getDatabase());
        p.put("morphia.db.username", mongoServiceInfo.getUserName());
        p.put("morphia.db.password", mongoServiceInfo.getPassword());

        // id type configuration to "ObjectId" (default), if not defined (can also be "Long").
        if (!p.containsKey("morphia.id.type")) {
            p.put("morphia.id.type", "ObjectId");
        }

        // Default write concern : http://api.mongodb.org/java/current/com/mongodb/class-use/WriteConcern.html
        if (!p.containsKey("morphia.defaultWriteConcern")) {
            p.put("morphia.defaultWriteConcern", "safe");
        }
    }

    /**
     * Configuration of Play Mongo plugin for MongoDB.
     *
     * @param p Play configuration to update.
     * @param mongoServiceInfo Information about Cloud Foundry MongoDB service.
     * @since 2011.07.11
     */
    private void mongoPluginConfig(Properties p, MongoServiceInfo mongoServiceInfo) {
        p.put("mongo.host", mongoServiceInfo.getHost());
        p.put("mongo.port", Integer.toString(mongoServiceInfo.getPort()));
        p.put("mongo.database", mongoServiceInfo.getDatabase());
        p.put("mongo.username", mongoServiceInfo.getUserName());
        p.put("mongo.password", mongoServiceInfo.getPassword());
    }

	/**
     * Configuration of PostgreSQL, if at least one CloudFoundry PostgreSQL service is bound to the instance.
     *
     * @param p Play configuration.
     * @since 2011.09.07
     */
    private void pgsqlServiceConfig(Properties p) {

        // Check that a PostgreSQL service is available.
        if (pgsqlServices.size() == 0) {
            Logger.info("[CloudFoundry] There is no PostgreSQL service bound to this application instance.");
            return;
        }

        // We configure the Cloud Foundry PostgreSQL database only if no other DB is configured.
        if (p.containsKey("db") || p.containsKey("db.url")) {
            Logger.warn("[CloudFoundry] A PostgreSQL database configuration already exists. It will not be overriden.");
            return;
        }

        // Check that only one PostgreSQL service is available. It is a non-blocking check.
        if (pgsqlServices.size() > 1) {
            Logger.warn("[CloudFoundry] There is more than one PostgreSQL service bind to this application instance. Only the first will be used.");
        }

        PostgresqlServiceInfo pgsqlServiceInfo = pgsqlServices.get(0);

        // Update of Play configuration. Theses properties will be used by the DBPlugin.
        p.put("db.driver", POSTGRESQL_DRIVER);
        p.put("db.url", pgsqlServiceInfo.getUrl());
        p.put("db.user", pgsqlServiceInfo.getUserName());
        p.put("db.pass", pgsqlServiceInfo.getPassword());
    }
}
