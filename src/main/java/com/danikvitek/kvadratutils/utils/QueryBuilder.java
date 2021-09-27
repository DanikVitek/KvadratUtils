package com.danikvitek.kvadratutils.utils;

public class QueryBuilder {
    private final StringBuilder query = new StringBuilder();

    public CreateTableQuery createTable(final String tableName) {
        query.append("CREATE TABLE IF NOT EXISTS ").append(tableName).append(" (");
        return new CreateTableQuery();
    }

    public InsertQuery insert(final String tableName) {
        query.append("INSERT INTO ").append(tableName).append(" ");
        return new InsertQuery();
    }

    public SelectQuery select(final String tableName) {
        query.append("SELECT ");
        return new SelectQuery(tableName);
    }

    public String build() {
        return query.toString();
    }

    public class CreateTableQuery extends QueryBuilder {
        private int fields = 0;

        public CreateTableQuery addAttribute(String attribute, String type) {
            query.append(fields++ > 0 ? "," : "").append(attribute).append(" ").append(type);
            return this;
        }

        public CreateTableQuery setPrimaryKeys(String ...keys) {
            if (keys.length > fields)
                throw new IllegalArgumentException("Too many keys");
            query.append(fields > 0 ? "," : "").append("PRIMARY KEY (").append(String.join(", ", keys)).append(")");
            return this;
        }

        @Override
        public String build() {
            return query.append(");").toString();
        }
    }

    public class InsertQuery extends QueryBuilder {
        String[] columns, values;

        public InsertQuery setColumns(String ...columns) {
            this.columns = columns;
            query.append("(").append(String.join(", ", columns)).append(") ");
            return this;
        }

        public InsertQuery setValues(String ...values) {
            this.values = values;
            query.append("VALUES(").append(String.join(", ", values)).append(")");
            return this;
        }

        public InsertQuery onDuplicateKeyUpdate() {
            query.append(" ON DUPLICATE KEY UPDATE ");
            for (int i = 0; i < columns.length; i++)
                query.append(columns[i]).append(" = ").append(values[i]).append(", ");
            query.delete(query.length() - 2, query.length());
            return this;
        }

        public InsertQuery onDuplicateKeyUpdate(String[] columns, String[] values) {
            query.append(" ON DUPLICATE KEY UPDATE ");
            for (int i = 0; i < columns.length; i++)
                query.append(columns[i]).append(" = ").append(values[i]).append(", ");
            query.delete(query.length() - 2, query.length());
            return this;
        }

        @Override
        public String build() {
            return query.append(";").toString();
        }
    }

    public class SelectQuery extends QueryBuilder {
        private final String tableName;

        public SelectQuery(String tableName) {
            this.tableName = tableName;
        }

        public SelectQuery what(final String columns) {
            query.append(columns);
            return this;
        }

        public SelectQuery from() {
            query.append(" FROM ").append(tableName);
            return this;
        }

        public SelectQuery where(final String condition) {
            query.append(" WHERE ").append(condition);
            return this;
        }

        @Override
        public String build() {
            return query.append(";").toString();
        }
    }
}