package cn.viewcn.nessusrm.orm;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import java.sql.SQLException;

public class DatabaseConnect {
    private static final String DATABASE_URL = "jdbc:sqlite:nessus_vul_db.sqlite";
    private static ConnectionSource connectionSource;

    private static Dao<PluginTranslation, String> pluginDao;

    static {
        try {
            connectionSource = new JdbcConnectionSource(DATABASE_URL);
            // 创建表（如果不存在）
            TableUtils.createTableIfNotExists(connectionSource, PluginTranslation.class);
            pluginDao = DaoManager.createDao(connectionSource, PluginTranslation.class);
        } catch (SQLException e) {
            System.err.println("数据库初始化失败: " + e.getMessage());
        }
    }

    public static void saveOrUpdate(PluginTranslation translation) {
        try {
            pluginDao.createOrUpdate(translation);
        } catch (SQLException e) {
            System.err.println("保存数据失败: " + e.getMessage());
        }
    }

    public static PluginTranslation getByPluginId(String pluginId) {
        try {
            return pluginDao.queryForId(pluginId);
        } catch (SQLException e) {
            System.err.println("查询数据失败: " + e.getMessage());
            return null;
        }
    }

    public static void close() {
        try {
            if (connectionSource != null) {
                connectionSource.close();
            }
        } catch (Exception e) {
            System.err.println("关闭数据库连接失败: " + e.getMessage());
        }
    }
}