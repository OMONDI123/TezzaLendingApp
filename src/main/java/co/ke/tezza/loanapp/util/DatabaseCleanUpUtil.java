package co.ke.tezza.loanapp.util;


import java.sql.*;
import java.util.*;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class DatabaseCleanUpUtil {

    @Autowired
    private DataSource dataSource;

    public ResponseEntity<?> clearTables() {
        try (Connection conn = dataSource.getConnection()) {
            int truncatedTables = clearAllTables(conn);
            String message = "You have successfully truncated " + truncatedTables + " table(s)";
            return new ResponseEntity<>(message, 200, null);
        } catch (Exception e) {
            throw new IllegalStateException("Database cleanup failed", e);
        }
    }

    /* ===================== PUBLIC API ===================== */

    public int clearAllTables(Connection conn) throws SQLException {
        int truncatedCount = 0;

        DatabaseType dbType = detectDatabase(conn);
        List<String> tables = getAllTables(conn, dbType);

        if (tables.isEmpty()) {
            return 0;
        }

        try (Statement stmt = conn.createStatement()) {

            switch (dbType) {

                case POSTGRES:
                    stmt.execute("TRUNCATE TABLE " + String.join(", ", tables) + " CASCADE");
                    truncatedCount = tables.size();
                    break;

                case MYSQL:
                    stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
                    for (String table : tables) {
                        stmt.execute("TRUNCATE TABLE " + quoteTable(table, dbType));
                        truncatedCount++; // increment for each table
                    }
                    stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
                    break;

                default:
                    for (String table : tables) {
                        stmt.execute("DELETE FROM " + table);
                        truncatedCount++; // increment for each table
                    }
            }
        }

        return truncatedCount;
    }

    /* ===================== TABLE DISCOVERY ===================== */

    private List<String> getAllTables(Connection conn, DatabaseType dbType) throws SQLException {

        DatabaseMetaData metaData = conn.getMetaData();

        String catalog = null;
        String schema = null;

        if (dbType == DatabaseType.MYSQL) {
            catalog = conn.getCatalog();
        } else {
            schema = resolveSchema(conn, dbType);
        }

        ResultSet rs = metaData.getTables(catalog, schema, "%", new String[] { "TABLE" });

        List<String> tables = new ArrayList<>();

        while (rs.next()) {
            String table = rs.getString("TABLE_NAME");

            if (!isExcludedTable(table)) {
                tables.add(schema == null ? table : schema + "." + table);
            }
        }

        return tables;
    }

    /* ===================== EXCLUSIONS ===================== */

    private static final Set<String> EXCLUDED_TABLES = new HashSet<>(
            Arrays.asList("flyway_schema_history", "audit_log", "hibernate_sequence","AD_Sub_Menu","AD_Menu","AD_Org","AD_Client"));

    private boolean isExcludedTable(String table) {
        return EXCLUDED_TABLES.contains(table.toLowerCase());
    }

    /* ===================== HELPERS ===================== */

    private String quoteTable(String table, DatabaseType dbType) {
        switch (dbType) {
            case MYSQL:
                return "`" + table.replace("`", "") + "`";
            default:
                return table;
        }
    }

    private DatabaseType detectDatabase(Connection conn) throws SQLException {
        String product = conn.getMetaData().getDatabaseProductName().toLowerCase();

        if (product.contains("postgres"))
            return DatabaseType.POSTGRES;
        if (product.contains("mysql") || product.contains("mariadb"))
            return DatabaseType.MYSQL;
        if (product.contains("oracle"))
            return DatabaseType.ORACLE;
        if (product.contains("sql server"))
            return DatabaseType.SQLSERVER;

        return DatabaseType.UNKNOWN;
    }

    private String resolveSchema(Connection conn, DatabaseType dbType) throws SQLException {
        switch (dbType) {
            case POSTGRES:
                return "public";
            case SQLSERVER:
                return "dbo";
            case ORACLE:
                return conn.getMetaData().getUserName().toUpperCase();
            default:
                return null;
        }
    }

    private enum DatabaseType {
        POSTGRES, MYSQL, ORACLE, SQLSERVER, UNKNOWN
    }
}