/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.quest.access.common.mysql;

import com.quest.access.common.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 *
 * @author connie
 */
public class ConnectionPool {
    
    //use the session id to store the connection object
    //disconnect the connection when the client returns
    private static final ConcurrentHashMap<String, ConcurrentHashMap<String,Connection>> connections = new ConcurrentHashMap();

    public static Connection getConnection(Database db, String userName, String host, String pass) {
        String id;
        if(db.getUserSession() == null){
           //no session specified
           //use an anonymous id
            id = "anonymous";
        }
        else {
            id = db.getUserSession().getId(); 
        }
        boolean connectionExists = false;
        if(connections.containsKey(id)){
            connectionExists = connections.get(id).containsKey(db.getDatabaseName());
        }
        
        if (connectionExists && !id.equals("anonymous")) { //recycle only known connections
            try {
                Connection conn = connections.get(id).get(db.getDatabaseName());
                if (conn.isClosed()) {
                    conn = createConnection(db.getDatabaseName(), host, userName, pass);
                    connections.get(id).put(db.getDatabaseName(),conn);
                    return conn;
                }
                return conn;
            } catch (SQLException ex) {
                java.util.logging.Logger.getLogger(ConnectionPool.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        } else {
            Connection conn = createConnection(db.getDatabaseName(), host, userName, pass);
            ConcurrentHashMap<String, Connection> cons = new ConcurrentHashMap<>();
            cons.put(db.getDatabaseName(), conn);
            connections.put(id, cons);
            return conn;
        }
    }

    private static Connection createConnection(String dbName, String url, String userName, String pass) {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            Connection conn = DriverManager.getConnection(url+dbName, userName, pass);
            return conn;
        } catch (Exception e) {
            Logger.toConsole(e, ConnectionPool.class);
            throw new RuntimeException(e);
        }
    }
    
    public static ConcurrentHashMap<String, ConcurrentHashMap<String,Connection>> getConnectionPool(){
       return connections;
    }

}