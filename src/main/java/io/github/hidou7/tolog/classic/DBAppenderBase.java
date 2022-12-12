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
    protected boolean cnxSupportsBatchUpdates = false;
    protected SQLDialect sqlDialect;

    protected abstract String getInsertSQL();

    @Override
    public void start() {

        if (connectionSource == null) {
            throw new IllegalStateException("DBAppender cannot function without a connection source");
        }

        sqlDialect = DBUtil.getDialectFromCode(connectionSource.getSQLDialectCode());
        cnxSupportsBatchUpdates = connectionSource.supportsBatchUpdates();
        if (sqlDialect == null) {
            throw new IllegalStateException(
                    "DBAppender cannot function without a specific SQL dialect");
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

            insertStatement = connection.prepareStatement(getInsertSQL());
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
