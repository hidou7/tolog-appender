package io.github.hidou7.tolog.classic;

import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.db.ConnectionSource;
import ch.qos.logback.core.db.DBHelper;
import ch.qos.logback.core.db.dialect.DBUtil;
import ch.qos.logback.core.db.dialect.SQLDialect;
import ch.qos.logback.core.db.dialect.SQLDialectCode;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;

public abstract class DBAppenderBase<E> extends UnsynchronizedAppenderBase<E> {

    protected ConnectionSource connectionSource;
    protected boolean cnxSupportsGetGeneratedKeys = false;
    protected boolean cnxSupportsBatchUpdates = false;
    protected SQLDialect sqlDialect;

    protected abstract Method getGeneratedKeysMethod();

    protected abstract String getInsertSQL();

    @Override
    public void start() {

        if (connectionSource == null) {
            throw new IllegalStateException("DBAppender cannot function without a connection source");
        }

        sqlDialect = DBUtil.getDialectFromCode(connectionSource.getSQLDialectCode());
        if (getGeneratedKeysMethod() != null) {
            cnxSupportsGetGeneratedKeys = connectionSource.supportsGetGeneratedKeys();
        } else {
            cnxSupportsGetGeneratedKeys = false;
        }
        cnxSupportsBatchUpdates = connectionSource.supportsBatchUpdates();
        if (!cnxSupportsGetGeneratedKeys && (sqlDialect == null)) {
            throw new IllegalStateException(
                    "DBAppender cannot function if the JDBC driver does not support getGeneratedKeys method *and* without a specific SQL dialect");
        }

        // all nice and dandy on the eastern front
        super.start();
    }

    /**
     * @return Returns the connectionSource.
     */
    public ConnectionSource getConnectionSource() {
        return connectionSource;
    }

    /**
     * @param connectionSource
     *          The connectionSource to set.
     */
    public void setConnectionSource(ConnectionSource connectionSource) {
        this.connectionSource = connectionSource;
    }

    @Override
    public void append(E eventObject) {
        Connection connection = null;
        PreparedStatement insertStatement = null;
        try {
            connection = connectionSource.getConnection();
            connection.setAutoCommit(false);

            if (cnxSupportsGetGeneratedKeys) {
                String EVENT_ID_COL_NAME = "EVENT_ID";
                // see
                if (connectionSource.getSQLDialectCode() == SQLDialectCode.POSTGRES_DIALECT) {
                    EVENT_ID_COL_NAME = EVENT_ID_COL_NAME.toLowerCase();
                }
                insertStatement = connection.prepareStatement(getInsertSQL(), new String[] { EVENT_ID_COL_NAME });
            } else {
                insertStatement = connection.prepareStatement(getInsertSQL());
            }
            String eventId = subAppend(eventObject, connection, insertStatement);
            secondarySubAppend(eventObject, connection, eventId);
            connection.commit();
        } catch (Throwable sqle) {
            addError("problem appending event", sqle);
            System.err.println("DBAppender error: " + sqle.getMessage());
        } finally {
            DBHelper.closeStatement(insertStatement);
            DBHelper.closeConnection(connection);
        }
    }

    protected abstract String subAppend(E eventObject, Connection connection, PreparedStatement statement) throws Throwable;

    protected abstract void secondarySubAppend(E eventObject, Connection connection, String eventId) throws Throwable;

    @Override
    public void stop() {
        super.stop();
    }
}
