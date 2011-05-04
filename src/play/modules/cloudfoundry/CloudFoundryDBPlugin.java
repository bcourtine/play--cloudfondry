package play.modules.cloudfoundry;

import org.cloudfoundry.runtime.env.CloudEnvironment;
import org.cloudfoundry.runtime.env.MysqlServiceInfo;
import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.db.DB;

import java.util.List;
import java.util.Properties;


/**
 * Configuration of Play MySQL DB from Cloud Foundry VCAP_SERVICES env variable, using the cloudfoundry-runtime library.
 *
 * TODO Manage all CloudFoundry services (MongoDB, Redis, RabbitMQ, etc.).
 *
 * @author Benoît Courtine.
 * @since 2011-05-04.
 */
public class CloudFoundryDBPlugin extends PlayPlugin {

    public static final String MYSQL_DRIVER = "com.mysql.jdbc.Driver";

    public CloudEnvironment cloudEnvironment = new CloudEnvironment();

    public List<MysqlServiceInfo> mysqlServices = cloudEnvironment.getServiceInfos(MysqlServiceInfo.class);

    /**
     * Update of Play configuration {@link Play#configuration} from Cloud Foundry env variable.
     * These configuration will be used by {@link play.db.DBPlugin} to configure the {@link DB#datasource}.
     */
    @Override
    public void onApplicationStart() {

        Properties p = Play.configuration;

        // We configure the Cloud Foundry MySQL database only if no other DB is configured.
        if (p.containsKey("db") || p.containsKey("db.url")) {
            Logger.warn("[CloudFoundry] A database configuration already exists");
            return;
        }

        // Check that a MySQL service is available.
        if (mysqlServices.size() == 0) {
            Logger.warn("[CloudFoundry] There is no MySQL service bind to this application instance");
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
}
