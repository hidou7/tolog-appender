package io.github.hidou7.tolog.classic;

public interface DBNameResolver {

    <N extends Enum<?>> String getTableName(N tableName);

    <N extends Enum<?>> String getColumnName(N columnName);
}
