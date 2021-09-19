package com.danikvitek.kvadratutils.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class StatementConnectionTuple {
    public Connection connection;
    public PreparedStatement preparedStatement;

    public StatementConnectionTuple(Connection connection, PreparedStatement preparedStatement) {
        this.connection = connection;
        this.preparedStatement = preparedStatement;
    }
}
