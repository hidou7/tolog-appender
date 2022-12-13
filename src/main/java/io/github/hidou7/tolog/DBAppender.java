package io.github.hidou7.tolog;

import ch.qos.logback.classic.spi.*;
import ch.qos.logback.core.CoreConstants;
import io.github.hidou7.tolog.classic.*;
import io.github.hidou7.tolog.util.SpringUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

import static ch.qos.logback.core.db.DBHelper.closeStatement;


public class DBAppender extends DBAppenderBase<ILoggingEvent> {

    protected String insertExceptionSQL;
    protected String insertSQL;

    private DBNameResolver dbNameResolver;

    static final int TIMESTMP_INDEX = 1;
    static final int FORMATTED_MESSAGE_INDEX = 2;
    static final int LOGGER_NAME_INDEX = 3;
    static final int LEVEL_STRING_INDEX = 4;
    static final int THREAD_NAME_INDEX = 5;
    static final int APP_NAME_INDEX = 6;
    static final int ARG0_INDEX = 7;
    static final int ARG1_INDEX = 8;
    static final int ARG2_INDEX = 9;
    static final int ARG3_INDEX = 10;
    static final int CALLER_FILENAME_INDEX = 11;
    static final int CALLER_CLASS_INDEX = 12;
    static final int CALLER_METHOD_INDEX = 13;
    static final int CALLER_LINE_INDEX = 14;
    static final int EVENT_ID_INDEX = 15;

    static final StackTraceElement EMPTY_CALLER_DATA = CallerData.naInstance();

    static String APP_NAME = null;

    public void setDbNameResolver(DBNameResolver dbNameResolver) {
        this.dbNameResolver = dbNameResolver;
    }

    @Override
    public void start() {
        if (dbNameResolver == null)
            dbNameResolver = new DefaultDBNameResolver();
        insertExceptionSQL = buildInsertExceptionSQL(dbNameResolver);
        insertSQL = buildInsertSQL(dbNameResolver);
        super.start();
    }

    static String buildInsertSQL(DBNameResolver dbNameResolver) {
        StringBuilder sqlBuilder = new StringBuilder("INSERT INTO ");
        sqlBuilder.append(dbNameResolver.getTableName(TableName.LOGGING_EVENT)).append(" (");
        sqlBuilder.append(dbNameResolver.getColumnName(ColumnName.TIMESTMP)).append(", ");
        sqlBuilder.append(dbNameResolver.getColumnName(ColumnName.FORMATTED_MESSAGE)).append(", ");
        sqlBuilder.append(dbNameResolver.getColumnName(ColumnName.LOGGER_NAME)).append(", ");
        sqlBuilder.append(dbNameResolver.getColumnName(ColumnName.LEVEL_STRING)).append(", ");
        sqlBuilder.append(dbNameResolver.getColumnName(ColumnName.THREAD_NAME)).append(", ");
        sqlBuilder.append(dbNameResolver.getColumnName(ColumnName.APP_NAME)).append(", ");
        sqlBuilder.append(dbNameResolver.getColumnName(ColumnName.ARG0)).append(", ");
        sqlBuilder.append(dbNameResolver.getColumnName(ColumnName.ARG1)).append(", ");
        sqlBuilder.append(dbNameResolver.getColumnName(ColumnName.ARG2)).append(", ");
        sqlBuilder.append(dbNameResolver.getColumnName(ColumnName.ARG3)).append(", ");
        sqlBuilder.append(dbNameResolver.getColumnName(ColumnName.CALLER_FILENAME)).append(", ");
        sqlBuilder.append(dbNameResolver.getColumnName(ColumnName.CALLER_CLASS)).append(", ");
        sqlBuilder.append(dbNameResolver.getColumnName(ColumnName.CALLER_METHOD)).append(", ");
        sqlBuilder.append(dbNameResolver.getColumnName(ColumnName.CALLER_LINE)).append(", ");
        sqlBuilder.append(dbNameResolver.getColumnName(ColumnName.EVENT_ID)).append(") ");
        sqlBuilder.append("VALUES (?, ?, ? ,?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        return sqlBuilder.toString();
    }

    static String buildInsertExceptionSQL(DBNameResolver dbNameResolver) {
        StringBuilder sqlBuilder = new StringBuilder("INSERT INTO ");
        sqlBuilder.append(dbNameResolver.getTableName(TableName.LOGGING_EVENT_EXCEPTION)).append(" (");
        sqlBuilder.append(dbNameResolver.getColumnName(ColumnName.EVENT_ID)).append(", ");
        sqlBuilder.append(dbNameResolver.getColumnName(ColumnName.I)).append(", ");
        sqlBuilder.append(dbNameResolver.getColumnName(ColumnName.TRACE_LINE)).append(") ");
        sqlBuilder.append("VALUES (?, ?, ?)");
        return sqlBuilder.toString();
    }

    @Override
    public String subAppend(ILoggingEvent event, Connection connection, PreparedStatement insertStatement) throws Throwable {

        bindLoggingEventWithInsertStatement(insertStatement, event);
        bindLoggingEventArgumentsWithPreparedStatement(insertStatement, event.getArgumentArray());

        // This is expensive... should we do it every time?
        String eventId = bindCallerDataWithPreparedStatement(insertStatement, event.getCallerData());

        int updateCount = insertStatement.executeUpdate();
        if (updateCount != 1) {
            addWarn("Failed to insert loggingEvent");
        }
        return eventId;
    }

    @Override
    protected void secondarySubAppend(ILoggingEvent event, Connection connection, String eventId) throws Throwable {
        if (event.getThrowableProxy() != null) {
            insertThrowable(event.getThrowableProxy(), connection, eventId);
        }
    }

    void bindLoggingEventWithInsertStatement(PreparedStatement stmt, ILoggingEvent event) throws SQLException {
        stmt.setLong(TIMESTMP_INDEX, event.getTimeStamp());
        stmt.setString(FORMATTED_MESSAGE_INDEX, event.getFormattedMessage());
        stmt.setString(LOGGER_NAME_INDEX, event.getLoggerName());
        stmt.setString(LEVEL_STRING_INDEX, event.getLevel().toString());
        stmt.setString(THREAD_NAME_INDEX, event.getThreadName());
        if(APP_NAME == null){
            APP_NAME = SpringUtil.getProperty("spring.application.name");
        }
        stmt.setString(APP_NAME_INDEX, APP_NAME);
    }

    void bindLoggingEventArgumentsWithPreparedStatement(PreparedStatement stmt, Object[] argArray) throws SQLException {

        int arrayLen = argArray != null ? argArray.length : 0;

        for (int i = 0; i < arrayLen && i < 4; i++) {
            stmt.setString(ARG0_INDEX + i, asStringTruncatedTo254(argArray[i]));
        }
        if (arrayLen < 4) {
            for (int i = arrayLen; i < 4; i++) {
                stmt.setString(ARG0_INDEX + i, null);
            }
        }
    }

    String asStringTruncatedTo254(Object o) {
        String s = null;
        if (o != null) {
            s = o.toString();
        }

        if (s == null) {
            return null;
        }
        if (s.length() <= 254) {
            return s;
        } else {
            return s.substring(0, 254);
        }
    }

    String bindCallerDataWithPreparedStatement(PreparedStatement stmt, StackTraceElement[] callerDataArray) throws SQLException {

        StackTraceElement caller = extractFirstCaller(callerDataArray);

        stmt.setString(CALLER_FILENAME_INDEX, caller.getFileName());
        stmt.setString(CALLER_CLASS_INDEX, caller.getClassName());
        stmt.setString(CALLER_METHOD_INDEX, caller.getMethodName());
        stmt.setString(CALLER_LINE_INDEX, Integer.toString(caller.getLineNumber()));
        String eventId = UUID.randomUUID().toString().replace("-", "");
        stmt.setString(EVENT_ID_INDEX, eventId);
        return eventId;
    }

    private StackTraceElement extractFirstCaller(StackTraceElement[] callerDataArray) {
        StackTraceElement caller = EMPTY_CALLER_DATA;
        if (hasAtLeastOneNonNullElement(callerDataArray))
            caller = callerDataArray[0];
        return caller;
    }

    private boolean hasAtLeastOneNonNullElement(StackTraceElement[] callerDataArray) {
        return callerDataArray != null && callerDataArray.length > 0 && callerDataArray[0] != null;
    }

    @Override
    protected String getInsertSQL() {
        return insertSQL;
    }

    /**
     * Add an exception statement either as a batch or execute immediately if
     * batch updates are not supported.
     */
    void updateExceptionStatement(PreparedStatement exceptionStatement, String txt, short i, String eventId) throws SQLException {
        exceptionStatement.setString(1, eventId);
        exceptionStatement.setShort(2, i);
        exceptionStatement.setString(3, txt);
        if (cnxSupportsBatchUpdates) {
            exceptionStatement.addBatch();
        } else {
            exceptionStatement.execute();
        }
    }

    short buildExceptionStatement(IThrowableProxy tp, short baseIndex, PreparedStatement insertExceptionStatement, String eventId) throws SQLException {

        StringBuilder buf = new StringBuilder();
        ThrowableProxyUtil.subjoinFirstLine(buf, tp);
        updateExceptionStatement(insertExceptionStatement, buf.toString(), baseIndex++, eventId);

        int commonFrames = tp.getCommonFrames();
        StackTraceElementProxy[] stepArray = tp.getStackTraceElementProxyArray();
        for (int i = 0; i < stepArray.length - commonFrames; i++) {
            StringBuilder sb = new StringBuilder();
            sb.append(CoreConstants.TAB);
            ThrowableProxyUtil.subjoinSTEP(sb, stepArray[i]);
            updateExceptionStatement(insertExceptionStatement, sb.toString(), baseIndex++, eventId);
        }

        if (commonFrames > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append(CoreConstants.TAB).append("... ").append(commonFrames).append(" common frames omitted");
            updateExceptionStatement(insertExceptionStatement, sb.toString(), baseIndex++, eventId);
        }

        return baseIndex;
    }

    protected void insertThrowable(IThrowableProxy tp, Connection connection, String eventId) throws SQLException {

        PreparedStatement exceptionStatement = null;
        try {
            exceptionStatement = connection.prepareStatement(insertExceptionSQL);

            short baseIndex = 0;
            while (tp != null) {
                baseIndex = buildExceptionStatement(tp, baseIndex, exceptionStatement, eventId);
                tp = tp.getCause();
            }

            if (cnxSupportsBatchUpdates) {
                exceptionStatement.executeBatch();
            }
        } finally {
            closeStatement(exceptionStatement);
        }
    }
}
