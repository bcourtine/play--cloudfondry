package play.modules.cloudfoundry;

import org.cloudfoundry.runtime.env.AbstractServiceInfo;
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

        if (!checkPlaySQLConfig(p) || !checkServiceList(mysqlServices, "MySQL")) {
            return;
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

        // We configure the Cloud Foundry MongoDB database only if no other DB is configured.
        if (p.containsKey("morphia.db.host")) {
            Logger.warn("[CloudFoundry] A MongoDB database configuration already exists. It will not be overriden.");
            return;
        }

        if (!checkServiceList(mongoServices, "MongoDB")) {
            return;
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

        if (!checkPlaySQLConfig(p) || !checkServiceList(pgsqlServices, "PostgreSQL")) {
            return;
        }

        PostgresqlServiceInfo pgsqlServiceInfo = pgsqlServices.get(0);

        // Update of Play configuration. Theses properties will be used by the DBPlugin.
        p.put("db.driver", POSTGRESQL_DRIVER);
        p.put("db.url", pgsqlServiceInfo.getUrl());
        p.put("db.user", pgsqlServiceInfo.getUserName());
        p.put("db.pass", pgsqlServiceInfo.getPassword());
    }

    /**
     * Check the number of services available.
     *
     * @param serviceInfos The service list to check.
     * @param dbName The db name associated to the service list.
     * @return <code>true</code> if there is one or more services, <code>false</code> otherwise.
     * @since 2011.09.07
     */
    private boolean checkServiceList(List<? extends AbstractServiceInfo> serviceInfos, String dbName) {
        // Check that a service is available.
        if (serviceInfos.size() == 0) {
            Logger.info("[CloudFoundry] There is no %s service bound to this application instance.", dbName);
            return false;
        }

        // Check that only one service is available. It is a non-blocking check.
        if (serviceInfos.size() > 1) {
            Logger.warn("[CloudFoundry] There is more than one %s service bind to this application instance. " +
                    "Only the first will be used.", dbName);
        }

        return true;
    }

    /**
     * Check that no SQL database is configured.
     *
     * @param p Play configuration.
     * @return <code>true</code> if no Play SQL configuration is defined, <code>false</code> otherwise.
     * @since 2011.09.07
     */
    private boolean checkPlaySQLConfig(Properties p) {
        // We configure the Cloud Foundry SQL database only if no other Play DB is configured.
        if (p.containsKey("db") || p.containsKey("db.url")) {
            Logger.warn("[CloudFoundry] A SQL database configuration already exists. It will not be overriden.");
            return false;
        }

        return true;
    }
}
