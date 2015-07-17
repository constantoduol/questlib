package com.quest.access.common.mysql;

/**
 *
 * @author constant oduol
 * @version 1.0(4/1/2012)
 */
import com.quest.access.common.Logger;
import com.quest.access.common.io;
import com.quest.access.control.Server;
import java.net.SocketException;

import java.sql.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpSession;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This file has implementations of mysql database connectivity methods a
 * database object represents a database in the mysql server connection to the
 * database needs to be set up by specifying the mysql user account we want to
 * connect with. This is done in the setDefaultConnection() method
 *
 * @see Database#setDefaultConnection(java.lang.String, java.lang.String,
 * java.lang.String) if the specified mysql account does not exist or if any of
 * the connection details are null a NoDefaultAccountException is thrown.Each
 * database object created is assigned a unique id starting from 1,2,3 and so on
 *
 *
 *
 */
public class Database {
    
    private static ServletConfig config;
    /*
     * the password to the default root account
     */

    private static String defaultPass;
    /*
     * the primary root username
     * this username is used for creating the very first connection to the database
     */
    private static String defaultUserName;
    /*
     * the default url to the root account eg. localhost
     */
    private static String defaultUrl;

    /*
     * the name of the database
     */
    private String name;

    /**
     * this method constructs a database in the mysql server specified by the
     * name if the database exists it is not recreated. the following sql code
     * is normally executed <code>CREATE DATABASE IF NOT EXISTS NAME</code>
     *
     * @param name - the name of the database we want to create
     * @param session
     */
            
    public Database(String name) {
        this.name = name;
    }
    
    
    public Connection getConnection(){
       return ConnectionPool.getConnectionPool().get(this.name);
    }
    
    private void closeConn(String dbName){
        try {
            Connection conn = ConnectionPool.getConnectionPool().get(dbName); 
            if (conn != null) {
                conn.close();
                ConnectionPool.getConnectionPool().remove(dbName); 
            }
        } catch (Exception e) {

        }
    }
    
    /**
     * this method returns the name of the current database
     *
     */
    public String getDatabaseName() {
        return this.name;
    }

    
    public static void setConfig(ServletConfig conf){
        config = conf;
        String status = config.getInitParameter("production-status");
        String url = "";
        if(status.equals("online")){
            String uName = config.getInitParameter("online-database-username");
            String pass = config.getInitParameter("online-database-password");
            url = config.getInitParameter("online-jdbc");
            defaultUserName = uName;
            defaultPass = pass;
        }
        else if(status.equals("offline")){
            String uName = config.getInitParameter("offline-database-username");
            String pass = config.getInitParameter("offline-database-password");
            url = config.getInitParameter("offline-jdbc");
            defaultUserName = uName;
            defaultPass = pass;
        }
        defaultUrl = url;
    }

 

    public JSONObject query(String sql) {
        JSONObject json = new JSONObject();
        try {
            ResultSet set = Database.executeQuery(sql, this);
            if(set == null) return new JSONObject();
            String[] labels = new String[set.getMetaData().getColumnCount() + 1];
            for (int x = 1; x < labels.length; x++) {
                labels[x] = set.getMetaData().getColumnLabel(x);
                try {
                    json.put(labels[x], new JSONArray());
                } catch (JSONException ex) {

                }
            }
            while (set.next()) {
                for (int x = 1; x < labels.length; x++) {
                    try {
                        String value = set.getString(x);
                        ((JSONArray) json.get(labels[x])).put(value);
                    } catch (Exception e) {

                    }
                }
            }
            set.close();
            //closeConn(name);
            return json;
        } catch (Exception ex) {
            return json;
        }
    }

    public JSONObject query(String psql, String... params) {
        JSONObject json = new JSONObject();
        try {
            ResultSet set = Database.executeQuery(psql, this, params);
            if(set == null) return new JSONObject();
            String[] labels = new String[set.getMetaData().getColumnCount() + 1];
            for (int x = 1; x < labels.length; x++) {
                labels[x] = set.getMetaData().getColumnLabel(x);
                try {
                    json.put(labels[x], new JSONArray());
                } catch (JSONException ex) {

                }
            }
            while (set.next()) {
                for (int x = 1; x < labels.length; x++) {
                    try {
                        String value = set.getString(x);
                        ((JSONArray) json.get(labels[x])).put(value);
                    } catch (Exception e) {

                    }
                }
            }
            set.close();
            //closeConn(name);
            return json;
        } catch (SQLException ex) {
            java.util.logging.Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
            return json;
        }

    }

    /**
     * this method sets the connection used to connect to the database and the
     * actions that can be executed on the database such as UPDATE, SELECT,
     * DELETE etc depend on the level of privilege the specified user has, if
     * any of the values are null an exception is thrown
     *
     * @param name the username of the user who wants to connect
     * @param host the host name the user is allowed to connect from eg.
     * localhost, 192.168.1.67
     * @param pass the password of the user
     */
    public static void setDefaultConnection(String name, String host, String pass) {
        defaultUserName = name;
        defaultUrl = host;
        defaultPass = pass;
    }

    /**
     * this method is used to execute a query on the database depending on the
     * executed statement a result set containing results from the database is
     * returned UPDATE, INSERT do not return any result set, SELECT returns a
     * result set with the required data It is important that once a result set
     * object is used it is closed
     *
     * @param sql - this is the sql statement that is executed on the database
     * @param dbName - the database we are executing a query on
     * @see #executeQuery(java.lang.String)
     * @see #executeQuery(java.lang.String, java.lang.String,
     * java.lang.String[])
     */
    public static ResultSet executeQuery(String sql,Database db) {
        Statement statement = null;
        Connection conn = null;
        try {
            String debug = config.getInitParameter("debug-mode");
            conn = ConnectionPool.getConnection(db, defaultUserName, defaultUrl, defaultPass);
            statement = conn.createStatement();
            statement.execute(sql);
            if (debug != null && debug.equals("true")) {
                System.out.println(sql);
            }
            return statement.getResultSet();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * this method executes a query on the current database
     *
     * @param sql the query to be executed on the current database
     * @see #executeQuery(java.lang.String, java.lang.String)
     * @see #executeQuery(java.lang.String, java.lang.String,
     * java.lang.String[])
     */
    public ResultSet execute(String sql) {
        return executeQuery(sql, this);

    }

    /**
     * this method sets a value in a given column depending on a specific
     * condition the sql query that is made
     * is<code>UPDATE tableName SET columnName=value WHERE condition</code> if
     * the condition matches more than one row then all the rows have the
     * specified values updated to the new value
     *
     * @param serv the server we want to connect to
     * @param tableName the name of the table we want to set a value
     * @param columnName the name of the column where a value is to be set
     * @param value
     * @param condition
     */
    public static void setValue(Database db, String tableName, String columnName, String value, String condition) {
        db.execute("UPDATE " + tableName + " SET " + columnName + "='" + value + "' WHERE " + condition + "");
    }
    
    
    public void setValue(String tableName, String columnName, String value, String condition){
         setValue(this, tableName, columnName, value, condition);
    }

    /**
     * this method executes prepared statements with strings as parameters
     *
     * @param psql this is the sql statement to be executed as a prepared
     * statement eg. INSERT INTO table1 (?) VALUES(?), CREATE TABLE ? (col1 INT)
     * @param dbName the name of the database to execute an sql statement
     * @param params these are the parameters that are replaced in the prepared
     * statement eg a statement such as INSERT INTO table1 (?) VALUES(?) the
     * first value in params replaces the first ? the second value replaces the
     * second ? the number of values under params determines how many times the
     * method pstatement.setString() is called
     * @return the value returned is a ResultSet or null, executing a SELECT
     * query returns a ResultSet while executing an UPDATE, INSERT or DELETE
     * returns null
     * @see #executeQuery(java.lang.String)
     * @see #executeQuery(java.lang.String, java.lang.String)
     */
    public static ResultSet executeQuery(String psql, Database db, String... params) {
        PreparedStatement pstatement = null;
        ResultSet set = null;
        Connection conn = null;
        try {
            String debug = config.getInitParameter("debug-mode");
            conn = ConnectionPool.getConnection(db, defaultUserName, defaultUrl, defaultPass);
            pstatement = conn.prepareStatement(psql);
            for (int x = 0; x < params.length; x++) {
                pstatement.setString(x + 1, params[x]);
            }
            //check to see if it is a select statement
            if (psql.toUpperCase().startsWith("SELECT")) {
                set = pstatement.executeQuery();
            } else {
                pstatement.executeUpdate();
            }
            if(debug != null && debug.equals("true") ){
               System.out.println(pstatement);
            }
            return set;

        } catch (Exception e) {
             throw new RuntimeException(e);
        }
    }




   
    /**
     * this method checks whether a specified value exists in the column
     * specified by column name
     *
     * @param columnName the name of the column we want to check if the value
     * exists
     * @param tableName the name of the table the column is contained in
     * @param dbName the name of the database the column is contained in
     * @param value the value we want to know whether it exists
     *
     */
    public static boolean ifValueExists(String value, String tableName, String columnName,Database db) {
        ResultSet set = Database.executeQuery("SELECT " + columnName + " FROM " + tableName + " WHERE " + columnName + "='" + value + "' ", db);
        try {
            while (set.next()) {
                return set.getString(columnName) != null ? true : false;
            }
            set.close();
        } catch (SQLException e) {
            return false;

        }
        return false;
    }
    

    /**
     * this method checks to see if the specified value exists in the specified
     * column
     *
     * @param value the value we want to ascertain its existence
     * @param tableName the table where the specified column is found
     * @param columnName the name of the column where the value is checked in
     * @return true or false depending on whether the specified value exists or
     * not
     */
    public boolean ifValueExists(String value, String tableName, String columnName) {
        return ifValueExists(value, tableName, columnName,this);
    }

    /**
     * checks to see if the specified values exist on the same row of any row in
     * the table
     *
     * @return
     */
    public boolean ifValueExists(String[] values, String tableName, String[] columnNames) {
        try {
            StringBuilder sql = new StringBuilder("SELECT * FROM " + tableName + " WHERE ");
            for (int x = 0; x < values.length; x++) {
                if (x == values.length - 1) {
                    sql.append(columnNames[x]).append("=?");
                } else {
                    sql.append(columnNames[x]).append("=? AND ");
                }
            }
            ResultSet set = Database.executeQuery(sql.toString(), this, values);
            while (set.next()) {
                for (int x = 1; x < values.length + 1; x++) {
                    if (set.getString(x) == null) {
                        set.close();
                        return false;
                    } else if (set.getString(x) != null && x == values.length - 1) {
                        set.close();
                        return true;
                    }
                }
            }
            set.close();
            return false;
            //nothing in the result set so it does not exist;
        } catch (Exception e) {
            return false;
        }
    }
    
    public String getValue(String toFind, String table, String key, String keyValue){
        return getValue(toFind, table, key, keyValue, this);
    }

    /**
     * this method gets a specific value from the database where a given
     * condition is true
     *
     * @param toFind this is the value we want to find from the database and is
     * usually a database column
     * @param table this is the table where the specific value is found
     * @param key this is the value that is used as a key to retrieve what we
     * are looking for
     * @param keyValue this is the value that matches what we want to find
     * @param serv this is the server where we want the value found
     * @return returns the value that has been found as per the specified
     * condition or an empty string if no value is found
     */
    public static String getValue(String toFind, String table, String key, String keyValue, Database db) {
        String val = "";
        ResultSet rs = db.execute("SELECT " + toFind + " FROM " + table + " WHERE " + key + "='" + keyValue + "'");
        try {
            while (rs.next()) {
                val = rs.getString("" + toFind + "");
                return val;
            }
            rs.close();
            return val;
        } catch (SQLException e) {
            return val;
        }
    }


  

    /**
     * this method is used to select data from the database in place of the
     * traditional "select" query the query is in this form SELECT columnNames
     * FROM tableNames WHERE conditions
     *
     * @param dbName the database to select data from
     * @param columnNames an array of strings representing the name of columns
     * to be selected
     * @param tableNames the names of the tables to select data from
     * @param conditions the conditions to be added after the where clause e.g.
     * user='root', age=20 etc
     * @return a result set containing the requested data
     */
    public static ResultSet doSelect(Database db, String[] columnNames, String[] tableNames, String[] conditions) {
        //Select col1,col2 from table1 where 
        StringBuilder builder = new StringBuilder("SELECT");
        for (int x = 0; x < columnNames.length - 1; x++) {
            builder.append(" ").append(columnNames[x]).append(" ,");
        }
        builder.append(" ").append(columnNames[columnNames.length - 1]).append(" ");
        builder.append("FROM ");
        for (int x = 0; x < tableNames.length - 1; x++) {
            builder.append(" ").append(tableNames[x]).append(" ,");
        }
        builder.append(" ").append(tableNames[tableNames.length - 1]).append(" ");
        if (conditions.length > 0) {
            builder.append("WHERE ");
            for (int x = 0; x < conditions.length - 1; x++) {
                builder.append(" ").append(conditions[x]).append(" AND");
            }
            builder.append(" ").append(conditions[conditions.length - 1]);
        }
        String sql = builder.toString();

        return executeQuery(sql, db);

    }

    /**
     * @see Database#doSelect(java.lang.String, java.lang.String[],
     * java.lang.String[], java.lang.String[])
     */
    public ResultSet doSelect(String[] columnNames, String[] tableNames, String[] conditions) {
        return doSelect(this, columnNames, tableNames, conditions);
    }

    /**
     * this method is used to insert data into the database the query executed
     * is INSERT INTO table (values), it uses the ! mark as an escape character
     * for values that should not have 'value' and should be passed as they are
     *
     * @param dbName the name of the database to insert into
     * @param table the name of the table the data is inserted into
     * @param values the values to insert into the database
     */
    public static void doInsert(Database db, String table, String[] values) {
        StringBuilder builder = new StringBuilder("INSERT INTO ").append(table).append(" ").append("VALUES");
        ArrayList<String> ps = new ArrayList();
        builder.append("(");
        for (int x = 0; x < values.length - 1; x++) {
            if (values[x].startsWith("!")) {
                builder.append(values[x].substring(1)).append(" ,");

            } else {
                builder.append(" ? ").append(" ,");
                ps.add(values[x]);
            }
        }
        if (values[values.length - 1].startsWith("!")) {
            builder.append(values[values.length - 1].substring(1)).append(" )");
        } else {
            builder.append(" ? ").append(")");
            ps.add(values[values.length - 1]);
        }
        String sql = builder.toString();
        String[] vals = new String[ps.size()];
        for (int x = 0; x < ps.size(); x++) {
            vals[x] = ps.get(x);
        }
        executeQuery(sql, db, vals);

    }

    /**
     * @see Database#doInsert(java.lang.String, java.lang.String,
     * java.lang.String[])
     */
    public void doInsert(String table, String[] values) {
        doInsert(this, table, values);
    }

    /**
     * this method is used to carry out a database update on the specified
     * database the various components of the update statements are split into
     * UPDATE tableNames SET values WHERE conditions
     *
     * @param dbName the name of the database to update
     * @param tableNames the table names that are to be updated
     * @param values the values that are to be updated in case the specified
     * conditions are true
     * @param conditions the conditions to be met in order for the data to be
     * updated
     */
    public static void doUpdate(Database db, String[] tableNames, String[] values, String[] conditions) {
        StringBuilder builder = new StringBuilder("UPDATE");
        for (int x = 0; x < tableNames.length - 1; x++) {
            builder.append(" ").append(tableNames[x]).append(" ,");
        }
        builder.append(" ").append(tableNames[tableNames.length - 1]).append(" ");
        builder.append("SET ");

        if (values.length > 0) {
            for (int x = 0; x < values.length - 1; x++) {
                builder.append(" ").append(values[x]).append(" AND");
            }
            builder.append(" ").append(values[values.length - 1]);
        }

        if (conditions.length > 0) {
            builder.append(" WHERE");
            for (int x = 0; x < conditions.length - 1; x++) {
                builder.append(" ").append(conditions[x]).append(" AND");
            }
            builder.append(" ").append(conditions[conditions.length - 1]);
        }
        String sql = builder.toString();
        executeQuery(sql, db);
    }

    /**
     * @see Database#doUpdate(java.lang.String, java.lang.String[],
     * java.lang.String[], java.lang.String[])
     */
    public void doUpdate(String[] tableNames, String[] values, String[] conditions) {
        doUpdate(this, tableNames, values, conditions);
    }

    //SELECT STUDENT_NAME FROM STUDENT_DATA WHERE STUDENT_NAME = ? AND STUDENT_CLASS = ?
    //db.query().select(class_id,book_id).where(name=20 and age=30).execute();
    //db.query().insert()
    //db.query().update()
    public QueryBuilder query() {
        return new QueryBuilder();
    }

    public class QueryBuilder {

        private String[] options;
        private String[] sqlStart = new String[]{
            "SELECT ",
            "INSERT INTO ",
            "UPDATE ",
            "DELETE ",
            " SET ",
            " FROM ",
            " WHERE ",
            " ORDER BY ",
            " LIMIT ",
            " ( ",
            " VALUES ("
        };

        //options = new String[]{select,insert,update,where,from,limit,order}

        public QueryBuilder() {
            options = new String[]{"", "", "", "", "", "", "", "", "", "", ""};
        }

        /**
         *
         * @param select these are the columns to be selected, this is a comma
         * separated string e.g name,age,location ... example of a select query          <code>  
         * String sql = db
         * .query()
         * .select("STUDENT_NAME, STUDENT_CLASS")
         * .from("STUDENT_DATA")
         * .order("STUDENT_NAME DESC")
         * .limit("1")
         * .toString();
         * </code>
         * @return a querybuilder object that can be used to add more options to
         * the query
         */
        public QueryBuilder select(String select) {
            options[0] = select;
            return this;
        }

        /**
         *
         * @param table this is the table to insert data into example of an
         * INSERT          <code>
         *    String sql=db.query()
         * .insert("STUDENT_DATA")
         * .columns("STUDENT_NAME,STUDENT_CLASS,AGE")
         * .values("john,2,20")
         * .toString();
         * </code>
         * @return a querybuilder object that can be used to add more options to
         * the query
         */
        public QueryBuilder insert(String table) {
            options[1] = table;
            return this;
        }

        /**
         *
         * @param update these are the columns to be selected, this is a comma
         * separated string e.g name,age,location ... example of an update          <code>
         *    String sql=db
         * .query()
         * .update("STUDENT_DATA")
         * .set("STUDENT_NAME='connie'")
         * .where("STUDENT_NAME='derrick'")
         * .toString();
         * </code>
         * @return a querybuilder object that can be used to add more options to
         * the query
         */
        public QueryBuilder update(String update) {
            options[2] = update;
            return this;
        }

        /**
         * example of a DELETE          <code>
         *   String sql = db.query()
         * .delete()
         * .from("STUDENT_DATA")
         * .where("STUDENT_NAME='QEJNVFKE'")
         * .toString();
         * </code>
         *
         * @return a querybuilder object that can be used to add more options to
         * the query
         */
        public QueryBuilder delete() {
            options[3] = " ";
            return this;
        }

        /**
         * @param set this is used in an update statement only e.g NAME='conie',
         * AGE = 20 this is only relevant for an UPDATE
         * @return a querybuilder object that can be used to add more options to
         * the query
         */
        public QueryBuilder set(String set) {
            options[4] = set;
            return this;
        }

        /**
         *
         * @param from this is a string representing the tables to select from
         * e.g student_data, names, authors etc this is only relevant for a
         * SELECT
         * @return a querybuilder object that can be used to add more options to
         * the query
         */
        public QueryBuilder from(String from) {
            options[5] = from;
            return this;
        }

        /**
         *
         * @param where this is a string representing the where part of an sql
         * statement e.g age > 20 and name like '%n'
         * @return a querybuilder object that can be used to add more options to
         * the query
         */
        public QueryBuilder where(String where) {
            options[6] = where;
            return this;
        }

        /**
         * @param order the order by which the result set should be in e.g NAME
         * ASC, AGE DESC etc this is only relevant for a SELECT
         * @return a querybuilder object that can be used to add more options to
         * the query
         */
        public QueryBuilder order(String order) {
            options[7] = order;
            return this;
        }

        /**
         * @param limit this is the number of results to be returned, this is
         * just a string e.g 10,20 etc this is only relevant for a SELECT
         * @return a querybuilder object that can be used to add more options to
         * the query
         */
        public QueryBuilder limit(String limit) {
            options[8] = limit;
            return this;
        }

        /**
         * @param columns these are the columns to be inserted into, this is
         * specified as (NAME,AGE,LOCATION...) this is only relevant for an
         * insert
         * @return a querybuilder object that can be used to add more options to
         * the query
         */
        public QueryBuilder columns(String columns) {
            options[9] = columns;
            return this;
        }

        /**
         * @param values these are the values to be inserted into the previously
         * specified table e.g (JOHN,20,KENYA) this is only relevant for an
         * insert
         * @return a querybuilder object that can be used to add more options to
         * the query
         */
        public QueryBuilder values(String values) {
            options[10] = values;
            return this;
        }

        /**
         * executes the query generated by this builder, this method is
         * generally called as the last method
         *
         * @return a json object with the results of the query
         */
        public JSONObject execute() {
            String sql = generateQuery();
            return Database.this.query(sql);
        }

        private String generateQuery() {
            StringBuilder sql = new StringBuilder();
            for (int x = 0; x < options.length; x++) {
                if (!options[x].isEmpty()) {
                    sql.append(sqlStart[x]);
                    StringTokenizer tokens = new StringTokenizer(options[x], ",");
                    int tokenCount = tokens.countTokens();
                    int count = 0;
                    while (tokens.hasMoreTokens()) {
                        count++;
                        if (tokenCount == count) {
                            if (sqlStart[x].trim().equals("(") || sqlStart[x].trim().equals("VALUES (")) {
                                sql.append(tokens.nextToken()).append(" )");
                            } else {
                                sql.append(tokens.nextToken());
                            }
                        } else {
                            sql.append(tokens.nextToken()).append(" ,");
                        }
                    }
                }
            }
            return sql.toString();
        }

        @Override
        public String toString() {
            return generateQuery();
        }

    }

}
