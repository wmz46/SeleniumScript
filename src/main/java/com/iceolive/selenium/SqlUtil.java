package com.iceolive.selenium;

import com.alibaba.druid.pool.DruidDataSourceFactory;
import com.alibaba.druid.util.JdbcConstants;
import com.iceolive.util.StringUtil;
import lombok.Data;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author:wangmianzhe
 **/
public class SqlUtil {


    public static Connection getConnection(String url, String username, String password) {
        try {
            DataSource dataSource = createDataSource(url, username, password);
            return dataSource.getConnection();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<Map<String, Object>> querySql(Connection conn, String sql, Map params) {
        if (null != conn) {
            try {
                List<SqlAndParams> sqlAndParamsList = getSqlAndParams(sql, params);
                SqlAndParams sqlAndParams = sqlAndParamsList.get(sqlAndParamsList.size() - 1);
                // 指定返回生成的主键
                PreparedStatement ps = conn.prepareStatement(sqlAndParams.getSql());
                addParams(sqlAndParams, ps);
                ResultSet rs = ps.executeQuery();
                ResultSetMetaData rsmd = rs.getMetaData();
                int columnCount = rsmd.getColumnCount();
                List<Map<String, Object>> list = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 0; i < columnCount; i++) {
                        String name = rsmd.getColumnName(i + 1);
                        Object value = rs.getObject(i + 1);
                        if (value == null) {
                            row.put(name, null);
                        } else if (value.getClass().getName().equals("java.sql.Timestamp")) {
                            row.put(name, StringUtil.format(value, "yyyy-MM-dd HH:mm:ss"));
                        } else {
                            row.put(name, value);
                        }
                    }
                    list.add(row);
                }
                rs.close();
                ps.close();
                return list;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            throw new RuntimeException("连接失败");
        }
        return null;
    }

    public static ExecResult execSql(Connection conn, String sql, Map params) {
        ExecResult result = new ExecResult();
        result.setCount(0);
        if (null != conn) {
            try {
                List<SqlAndParams> list = getSqlAndParams(sql, params);
                if (list.size() == 1) {
                    SqlAndParams sqlAndParams = list.get(0);
                    PreparedStatement ps = conn.prepareStatement(sqlAndParams.getSql(), Statement.RETURN_GENERATED_KEYS);
                    addParams(sqlAndParams, ps);
                    int count = ps.executeUpdate();
                    ResultSet rs = ps.getGeneratedKeys();
                    Long id = null;
                    if (rs.next()) {
                        id = rs.getLong(1);
                    }
                    result.setPrimaryKey(id);
                    result.setCount(count);
                    rs.close();
                    ps.close();
                    return result;
                } else {
                    conn.setAutoCommit(false);
                    int count = 0;
                    for (SqlAndParams sqlAndParams : list) {
                        PreparedStatement ps = conn.prepareStatement(sqlAndParams.getSql(), Statement.NO_GENERATED_KEYS);
                        addParams(sqlAndParams, ps);
                        count += ps.executeUpdate();
                        ps.close();
                    }
                    conn.commit();
                    conn.setAutoCommit(true);
                    result.setCount(count);

                    return result;
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            throw new RuntimeException("连接失败");
        }
        return result;
    }

    private static void addParams(SqlAndParams sqlAndParams, PreparedStatement ps) throws SQLException {
        if (sqlAndParams.getParams() != null && !sqlAndParams.getParams().isEmpty()) {
            for (int i = 0; i < sqlAndParams.getParams().size(); i++) {
                Object param = sqlAndParams.getParams().get(i);
                if (param != null) {
                    Class<?> clazz = param.getClass();
                    if (Arrays.asList("java.util.Date", "java.time.LocalDateTime", "java.time.LocalDate").contains(clazz.getName())) {
                        ps.setString(i + 1, StringUtil.format(param, "yyyy-MM-dd HH:mm:ss"));
                    } else {
                        ps.setObject(i + 1, param);
                    }
                } else {
                    ps.setObject(i + 1, null);
                }
            }
        }
    }

    private static List<SqlAndParams> getSqlAndParams(String sql, Map params) {
        String[] sqls = sql.trim().split(";(?=([^\\']*\\'[^\\']*\\')*[^\\']*$)");
        List<SqlAndParams> list = new ArrayList<>();
        for (String str : sqls) {
            Matcher matcher = Pattern.compile("[#$]\\{(.*?)}").matcher(str);
            String newSql = str;
            List<Object> newParams = new ArrayList<>();
            while (matcher.find()) {
                newSql = Pattern.compile(matcher.group(0), Pattern.LITERAL).matcher(
                        newSql).replaceFirst("?");
                Object value = params.get(matcher.group(1).trim());
                newParams.add(value);
            }
            SqlAndParams sqlAndParams = new SqlAndParams();
            sqlAndParams.setSql(newSql);
            sqlAndParams.setParams(newParams);
            list.add(sqlAndParams);
        }
        return list;
    }

    @Data
    private static class SqlAndParams {
        private String sql;
        private List<Object> params;
    }

    @Data
    public static class ExecResult {
        private int count;
        private Long primaryKey;
    }

    private static DataSource createDataSource(String url, String username, String password) throws Exception {
        Map<String, String> properties = new HashMap<>();
        properties.put("url", url);
        properties.put("username", username);
        properties.put("password", password);
        properties.put("driverClassName", getDriverClassName(url));
        return DruidDataSourceFactory.createDataSource(properties);
    }

    private static String getDriverClassName(String url) {
        if (url.startsWith("jdbc:mysql:")) {
            return JdbcConstants.MYSQL_DRIVER_6;
        } else if (url.startsWith("jdbc:h2:")) {
            return JdbcConstants.H2_DRIVER;

        } else if (url.startsWith("jdbc:sqlserver:")) {
            return JdbcConstants.SQL_SERVER_DRIVER_SQLJDBC4;

        } else if (url.startsWith("jdbc:dm:")) {
            return JdbcConstants.DM_DRIVER;
        } else {
            return "";
        }
    }


}
