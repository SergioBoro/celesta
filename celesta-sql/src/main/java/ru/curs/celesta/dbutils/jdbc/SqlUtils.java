package ru.curs.celesta.dbutils.jdbc;

import ru.curs.celesta.CelestaException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class SqlUtils {

    private SqlUtils() {
        throw new AssertionError();
    }

    public static int executeUpdate(Connection conn, String sql) throws CelestaException {
        try (Statement stmt = conn.createStatement()) {
            return stmt.executeUpdate(sql);
        } catch (SQLException e) {
            throw new CelestaException(e);
        }
    }

    public static ResultSet executeQuery(Connection conn, String sql) throws CelestaException {
        Statement stmt = null;
        try  {
            stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            return (ResultSet)Proxy.newProxyInstance(rs.getClass().getClassLoader(), new Class<?>[] { ResultSet.class},
                    new ResultSetInvocationHandler(rs));
        } catch (SQLException e) {
            if (stmt != null)
                try {
                    stmt.close();
                } catch (SQLException ex) {
                    throw new CelestaException(ex);
                }
            throw new CelestaException(e);
        }
    }

    private static class ResultSetInvocationHandler implements InvocationHandler {

        private final ResultSet rs;

        public ResultSetInvocationHandler(ResultSet rs) {
            this.rs = rs;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.equals(ResultSet.class.getMethod("close"))) {
                Statement statement = rs.getStatement();
                if (!statement.isClosed())
                    statement.close();
            } else {
                return method.invoke(rs, args);
            }
            return null;
        }
    }
}
