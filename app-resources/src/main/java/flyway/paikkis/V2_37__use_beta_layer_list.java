package flyway.paikkis;

import fi.nls.oskari.db.BundleHelper;
import fi.nls.oskari.domain.map.view.Bundle;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.map.view.AppSetupServiceMybatisImpl;
import fi.nls.oskari.map.view.ViewException;
import fi.nls.oskari.map.view.ViewService;
import fi.nls.oskari.util.FlywayHelper;
import fi.nls.oskari.util.PropertyUtil;
import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class V2_37__use_beta_layer_list implements JdbcMigration {
    private static final Logger LOG = LogFactory.getLogger(V2_37__use_beta_layer_list.class);
    private static final String MIGRATION_PROP_NAME = "flyway.paikkis.useBetaLayerList";
    private static final String LAYERSELECTOR_BUNDLE_NAME = "layerselector2";
    private static final String LAYERLIST_BUNDLE_NAME = "layerlist";
    private static final String LAYER_EDITOR_BUNDLE_NAME = "admin-layereditor";

    private ViewService viewService = null;

    public void migrate(Connection connection) throws SQLException, ViewException {
        final boolean proceed = PropertyUtil.getOptional(MIGRATION_PROP_NAME, false);
        if (!proceed) {
            LOG.info("Skipping migration to react ui");
            return;
        }
        viewService =  new AppSetupServiceMybatisImpl();
        createBundles(connection);
        updateDefaultAndUserViews(connection);
    }

    private void createBundles(Connection connection) throws SQLException {
        for (String bundleName : Arrays.asList(LAYER_EDITOR_BUNDLE_NAME, LAYERLIST_BUNDLE_NAME)) {
            Bundle bundle = new Bundle();
            bundle.setName(bundleName);
            BundleHelper.registerBundle(bundle, connection);
            LOG.info("Created " + bundleName + " bundle");
        }
    }

    private void updateDefaultAndUserViews(Connection connection) throws SQLException {
        Bundle newBundle = BundleHelper.getRegisteredBundle(LAYERLIST_BUNDLE_NAME, connection);
        List<Long> viewIds = FlywayHelper.getUserAndDefaultViewIds(connection);
        for(Long viewId : viewIds) {
            Bundle bundle = FlywayHelper.getBundleFromView(connection, LAYERSELECTOR_BUNDLE_NAME, viewId);
            if (bundle == null) {
                continue;
            }
            newBundle.setSeqNo(bundle.getSeqNo());
            newBundle.setConfig(bundle.getConfig());
            FlywayHelper.removeBundleFromView(connection, LAYERSELECTOR_BUNDLE_NAME, viewId);
            FlywayHelper.addBundleWithDefaults(connection, viewId, LAYERLIST_BUNDLE_NAME);
            FlywayHelper.updateBundleInView(connection, newBundle, viewId);
        }
    }
}
