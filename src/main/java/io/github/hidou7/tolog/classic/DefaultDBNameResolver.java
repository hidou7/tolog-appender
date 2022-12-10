package io.github.hidou7.tolog.classic;

public class DefaultDBNameResolver implements DBNameResolver{

    public <N extends Enum<?>> String getTableName(N tableName) {
        return tableName.toString().toLowerCase();
    }

    public <N extends Enum<?>> String getColumnName(N columnName) {
        return columnName.toString().toLowerCase();
    }
}
