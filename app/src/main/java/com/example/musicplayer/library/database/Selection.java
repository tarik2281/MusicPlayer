package com.example.musicplayer.library.database;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by 19tar on 06.09.2017.
 */

class Selection {
    private static final String ADD_LIKE_STATEMENT = " LIKE ('%' || ? || '%')";

    private StringBuilder mBuilder;
    private List<String> mArgs;

    public Selection() {
        mBuilder = new StringBuilder();
        mArgs = new ArrayList<>();
    }

    public StringBuilder getBuilder() {
        return mBuilder;
    }

    public void beginCond() {
        mBuilder.append('(');
    }

    public void endCond() {
        mBuilder.append(')');
    }

    public void addConjunction() {
        if (mBuilder.length() > 0)
            mBuilder.append(" AND ");
    }

    public void addDisjunction() {
        if (mBuilder.length() > 0)
            mBuilder.append(" OR ");
    }

    private void addColumn(String table, Enum column) {
        mBuilder.append(table).append('.').append(column.name());
    }

    public void addEqualsStatement(String table, String arg, Enum column) {
        addColumn(table, column);
        mBuilder.append("=?");
        addArg(arg);
    }

    public void addEqualsStatement(String table, String arg, Enum column, boolean invert) {
        String operator = invert ? "!=?" : "=?";
        addColumn(table, column);
        mBuilder.append(operator);
        addArg(arg);
    }

    public void addNullCheck(String table, Enum column, boolean invert) {
        addColumn(table, column);
        mBuilder.append(" IS ");
        if (invert)
            mBuilder.append("NOT ");
        mBuilder.append("NULL");
    }

    public void addLikeStatement(String table, String arg, Enum column) {
        addColumn(table, column);
        mBuilder.append(ADD_LIKE_STATEMENT);
        addArg(arg);
    }

    public void addLikeConditions(String table, String arg, Enum... columns) {
        addConjunction();

        // (table.column LIKE ('%' || ? || '%') OR ...)
        beginCond();

        for (int i = 0; i < columns.length;) {
            addLikeStatement(table, arg, columns[i]);
            if (++i < columns.length)
                addDisjunction();
        }

        endCond();
    }

    public String getSelection() {
        if (mBuilder.length() == 0)
            return null;

        return mBuilder.toString();
    }

    public String[] getArgs() {
        if (mArgs.isEmpty())
            return null;

        return mArgs.toArray(new String[mArgs.size()]);
    }

    public void addArg(String arg) {
        mArgs.add(arg);
    }
}
