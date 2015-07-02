package com.quest.access.useraccess;

import com.quest.access.common.Logger;
import com.quest.access.common.UniqueRandom;
import com.quest.access.common.io;
import com.quest.access.common.mysql.Database;
import com.quest.access.crypto.Security;
import com.quest.access.useraccess.verification.Action;
import com.quest.access.useraccess.verification.UserAction;
import java.util.logging.Level;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author constant oduol
 * @version 1.0(17/3/12)
 */
/**
 * <p>
 * A user is a person or object that has been assigned access rights to server
 * resources, a user is created by specifying the user name, password, host,
 * server the user belongs to and the privileges assignable to the user. When a
 * user is created he is assigned a unique ten digit user id and the user's
 * privileges are saved in the PRIVILEGES table. A user can also be created as
 * having access to a given service. Once a user is created he can log in and
 * access the resources that are accessible to him
 * </p>
 * <p>
 * A user can be renamed after creation provided there is no existing user with
 * the same user name. Users can also be temporarily deleted i.e their details
 * are moved to the USER_HISTORY table Users can also be disabled to prevent
 * access to server resources.
 * </p>
 * <p>
 * When a user logs in to a server a new session is created to keep track of the
 * user logging out the user destroys the user's session Every user has a
 * temporary privilege associated with them, the system can assign users
 * resources that they don't have access to temporarily through their temporary
 * privileges
 * </p>
 *
 */
public class User {

    private JSONObject userProperties = new JSONObject();

    private String userName;
    /*
     * this are the  resource groups the user has access to
     */
    private JSONArray priv;

    private Database database;

    /**
     * constructs a user object, the user's details are stored in the server's
     * database in the USERS table
     *
     * @param userName the desired userName of the new user
     * @param pass the desired password of the new user, if this is not provided
     * the default password of the server is used
     * @param host the host from which this user is expected to connect from
     * @param server the server in which this user is expected to operate
     * @param priv the permanent privileges that are accessible to this user.
     */
    public User(String userName, String pass, String host, Database db, Action action, String userInterface, String... privs) throws UserExistsException, NonExistentUserException {
        this(userName, pass, host, db, null, action, userInterface, privs);
    }

    /**
     * constructs a user object, the user's details are stored in the server's
     * database in the USERS table
     *
     * @param userName the desired userName of the new user
     * @param pass the desired password of the new user, if this is not provided
     * the default password of the server is used
     * @param host the host from which this user is expected to connect from
     * @param server the server in which this user is expected to operate
     * @param group the user group that this user is being assigned to
     * @param priv the permanent privileges that are accessible to this user.
     * @throws UserExistsException
     */
    public User(String userName, String pass, String host, Database db, String group, Action action, String userInterface, String... privs) throws UserExistsException {
        this.userName = userName;
        this.database = db;
        this.priv = new JSONArray();
        if (userInterface.equals("touch")) {
            createTouchUser(userName, pass, host, db, group, action);
        } else {
            createUser(userName, pass, host, db, group, action);
        }
        grantPrivileges(privs);
    }

    public User(String userName, String pass, String host, Database db, String group, String userInterface, Action action) throws UserExistsException {
        this.userName = userName;
        this.database = db;
        this.priv = new JSONArray();
        if (userInterface.equals("touch")) {
            createTouchUser(userName, pass, host, db, group, action);
        } else {
            createUser(userName, pass, host, db, group, action);
        }
    }

    /**
     * constructs a user object, the user's details are stored in the server's
     * database in the USERS table
     *
     * @param userName the desired userName of the new user
     * @param pass the desired password of the new user, if this is not provided
     * the default password of the server is used
     * @param host the host from which this user is expected to connect from
     * @param server the server in which this user is expected to operate
     * @param group the user group that this user is being assigned to
     * @param service the services this user is being assigned access to
     * @throws UserExistsException
     */
    public User(String userName, String pass, String host, Database db, String group, Action action, String userInterface, Service... service) throws UserExistsException {
        String[] privs = new String[service.length];
        for (int x = 0; x < service.length; x++) {
            privs[x] = service[x].getServicePrivilege();
        }
        User user = new User(userName, pass, host, db, group, action, userInterface, privs);
    }

    /**
     * constructs a user object, the user's details are stored in the server's
     * database in the USERS table
     *
     * @param userName the desired userName of the new user
     * @param pass the desired password of the new user, if this is not provided
     * the default password of the server is used
     * @param host the host from which this user is expected to connect from
     * @param server the server in which this user is expected to operate
     * @param service the services accessible to this user
     * @throws UserExistsException
     */
    public User(String userName, String pass, String host, Database db, UserAction action, String userInterface, Service... service) throws UserExistsException {
        this(userName, pass, host, db, null, action, userInterface, service);
    }

    /**
     * privately creates a user object which is used by the method
     * getExistingUser()
     *
     * @param userName the name of the user
     * @param serv the server the user belongs to
     * @see #getExistingUser(java.lang.String, com.quest.access.net.Server)
     */
    private User(String userName, Database db, JSONObject data) {
        this.userName = userName;
        this.database = db;
        this.userProperties = data;
    }

    public JSONObject getUserProperties() {
        return this.userProperties;
    }

    public String getUserProperty(String key) {
        JSONArray props = this.userProperties.optJSONArray(key);
        if (props != null) {
            return props.optString(0);
        }
        return "";
    }

    public void setUserProperty(String key, String value, Database db) {
        JSONArray props = this.userProperties.optJSONArray(key);
        if (props != null) {
            props.put(value);
        } else {
            try {
                props = new JSONArray();
                props.put(value);
                this.userProperties.put(key, props);
            } catch (JSONException ex) {
                java.util.logging.Logger.getLogger(User.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        db.setValue("USERS", key, value, "USER_NAME='" + this.userName + "'");
    }

    /**
     * this method is used to modify the password of a user
     *
     * @param newPass the new password the user will use to log in
     */
    public void setPassWord(String newPass) {
        try {
            byte[] bytes = Security.makePasswordDigest("PINS_ARE_WEIRD", newPass.toCharArray());
            String passw = Security.toBase64(bytes);
            this.database.query("UPDATE USERS SET PASS_WORD='" + passw + "' WHERE USER_NAME=?", this.userName);
        } catch (Exception e) {
            Logger.toConsole(e, this.getClass());
        }
    }

    /**
     * this method returns the server a user belongs to
     */
    public Database getDatabase() {
        return this.database;
    }

    /**
     * This method deletes a user from the users table and moves his details to
     * the USER_HISTORY table if the user had a mysql account the account is
     * dropped
     *
     * @param userName the name of the user to be deleted
     * @param serv the server where this user was created
     */
    public static void deleteUser(String userName, Database db) throws NonExistentUserException {
        User user = User.getExistingUser(userName, db);
        db.query("DELETE FROM USERS WHERE USER_ID=?", user.getUserProperty("USER_ID"));
        db.query("DELETE FROM PRIVILEGES WHERE USER_ID=?", user.getUserProperty("USER_ID"));
    }

    /**
     * this method returns the history of a users actions from the database it
     * gives a view of what a user has been doing, this actions only represent
     * those actions that the system chooses to record. specifying a limit of 0
     * returns all the records, caution should be used when specifying a limit
     * of 0 since it could be slow
     */
    public static JSONObject getActionHistory(String userName, Database db, int limit) {
        JSONObject set = null;
        if (limit == 0) {
            set = db.query("SELECT * FROM USER_ACTIONS WHERE USER_NAME=? ORDER BY ACTION_TIME DESC", userName);
        } else if (limit > 0) {
            set = db.query("SELECT * FROM USER_ACTIONS WHERE USER_NAME=? ORDER BY ACTION_TIME DESC LIMIT " + limit + "", userName);
        }
        return set;
    }

    /**
     * this method returns the privileges of a user as stored in the database
     * this method is called when a user logs in in order to determine which
     * privileges the user has, privilege information is stored in the
     * PRIVILEGES table. the privilege information is returned in a hash map
     * containing the RESOURCE GROUP NAMES as the keys of the hash map and an
     * array list containing the users resource names
     *
     * @return
     */
    public JSONArray getUserPrivileges() {
        String id = this.getUserProperty("USER_ID");
        String psql = "SELECT  PRIVILEGES.GROUP_ID as RESOURCE_GROUP_NAME "
                + " FROM PRIVILEGES WHERE PRIVILEGES.USER_ID=?";
        return this.database.query(psql, id).optJSONArray("RESOURCE_GROUP_NAME");
    }

    public static boolean changePassword(Database db, String userName, String oldPass, String newPass) {
        try {
            String old_pass = Security.toBase64(Security.makePasswordDigest("PINS_ARE_WEIRD", oldPass.toCharArray()));
            String pass_stored = db.getValue("PASS_WORD", "USERS", "USER_NAME", userName);
            if (old_pass.equals(pass_stored)) {
                byte[] bytes = Security.makePasswordDigest(userName, newPass.toCharArray());
                String passw = Security.toBase64(bytes);
                long time = System.currentTimeMillis();
                db.query("UPDATE USERS SET PASS_WORD='" + passw + "',IS_PASSWORD_EXPIRED='" + time + "', CHANGE_PASSWORD = '0' WHERE USER_NAME=?", userName);
                return true;
            } else {
                return false;
            }

        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * this method returns login details for the specified user , each time a
     * user logs in the login details at that time are inserted in one table row
     * of the LOGIN table specifying a limit of 0 returns all the results,
     * specifying a limit of zero should be used with caution since it could be
     * slow. details return include the login time, login id etc. an arraylist
     * containing hashmaps of the login data is returned the keys in the hash
     * map are
     * <ol>
     * <li>SERVER_IP the ip address of the server machine the user logged in
     * from</li>
     * <li>CLIENT_IP the ip address of the client machine the user logged in
     * from</li>
     * <li>LOGIN_ID the system generated log in id </li>
     * <li>LOGIN_TIME the time the user logged in</li>
     * </ol>
     *
     * @param userName the username of the user we want to retrieve the login
     * details
     * @param serv the server this user was originally created in
     * @param limit the number of rows to be retrieved
     * @return an arraylist containing login details
     */
    public static JSONObject getLoginLog(String userName, Database db, int limit) {
        JSONObject set = null;
        if (limit == 0) {
            set = db.query("SELECT * FROM LOGIN WHERE USER_NAME=? ORDER BY LOGIN_TIME DESC", userName);
        } else if (limit > 0) {
            set = db.query("SELECT * FROM LOGIN WHERE USER_NAME=? ORDER BY LOGIN_TIME DESC LIMIT " + limit + "", userName);
        }
        return set;
    }

    /**
     * this method returns the details of a user who has successfully logged out
     * of the system such details include the logout time, logout id etc. an
     * arraylist containing hashmaps of the logout data is returned the keys in
     * the hash map are
     * <ol>
     * <li>SERVER_IP the ip address of the server machine the user logged out
     * from</li>
     * <li>CLIENT_IP the ip address of the client machine the user logged out
     * from</li>
     * <li>LOGOUT_ID the system generated log out id which is the same as the
     * login id</li>
     * <li>LOGOUT_TIME the time the user logged out</li>
     * </ol>
     *
     * @param userName the username of the user we want to retrieve the logout
     * details
     * @param serv the server this user was originally created in
     * @param limit the number of rows to be retrieved
     * @return an arraylist containing logout details
     */
    public static JSONObject getLogoutLog(String userName, Database db, int limit) {
        JSONObject set = null;
        if (limit == 0) {
            set = db.query("SELECT * FROM LOGOUT WHERE USER_NAME=? ORDER BY LOGOUT_TIME DESC", userName);
        } else if (limit > 0) {
            set = db.query("SELECT * FROM LOGOUT WHERE USER_NAME=? ORDER BY LOGOUT_TIME DESC LIMIT " + limit + "", userName);
        }
        return set;
    }

    /**
     * this method returns an instance of an existing user without trying to
     * recreate the user, the method gets the details of the user and creates a
     * user object for this user that already exists
     *
     * @param userName
     * @param serv
     * @return
     * @throws com.quest.access.useraccess.NonExistentUserException
     */
    public static User getExistingUser(String userName, Database db) throws NonExistentUserException {
        JSONObject data = db.query("SELECT * FROM USERS WHERE USER_NAME = ?", userName);
        if (data.optJSONArray("USER_NAME").length() == 0) {
            throw new NonExistentUserException();
        }
        // make sure the user exists 
        return new User(userName, db, data);
    }

    public static User getExistingUserUsingPin(String pinHash, Database db) throws NonExistentUserException {
        JSONObject data = db.query("SELECT * FROM USERS WHERE PASS_WORD = ?", pinHash);
        if (data.optJSONArray("USER_NAME").length() == 0) {
            throw new NonExistentUserException();
        }
        String userName = data.optJSONArray("USER_NAME").optString(0);
        // make sure the user exists 
        return new User(userName, db, data);
    }

    /**
     * this method assigns the specified privileges to the specified user
     *
     * @param privs
     */
    public void grantPrivileges(String... privs) {
        for (String privilege : privs) {
            if (privilege == null) {
                continue;
                // this means no such resource group exists
            }
            JSONObject data = this.database.query("SELECT * FROM PRIVILEGES WHERE USER_ID=? AND GROUP_ID=?", this.getUserProperty("USER_ID"), privilege);
            if (data.optJSONArray("USER_ID").length() == 0) {
                //doesnt exist so insert
                String id = getUserProperty("USER_ID");
                if (!id.trim().isEmpty()) {
                    this.database.query("INSERT INTO PRIVILEGES VALUES(?,?)", id, privilege);
                }
                Logger.toConsole("user privileges saved", this.getClass());
            }
        }
    }

    /**
     * this method revokes the specified privileges from the specified users
     *
     * @param privs
     */
    public void revokePrivileges(String... privs) {
        try {
            for (int x = 0; x < privs.length; x++) {
                String name = privs[x];
                if (name == null) {
                    // this means no such privilege exists
                    continue;
                }
                this.database.query("DELETE FROM PRIVILEGES WHERE USER_ID=? AND GROUP_ID=? ", getUserProperty("USER_ID"), name);
                Logger.toConsole("user privileges revoked", this.getClass());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * returns a string representation of a user
     */
    @Override
    public String toString() {
        return "User[" + this.userName + " : " + this.getUserProperty("USER_ID") + "]";
    }

    private void setUserProperty(String key, String value) {
        try {
            this.userProperties.put(key, new JSONArray().put(value));
        } catch (JSONException ex) {
            java.util.logging.Logger.getLogger(User.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /*----------------private implementation---------------------*/
    private void createTouchUser(String userName, String pass, String host, Database db, String group, Action action) {
        if (group == null) {
            group = "unassigned";
        }
        UniqueRandom ur = new UniqueRandom(20);
        String nextRandom = ur.nextRandom();
        // check to ensure the user name is always unique
        try {
            String passw = Security.toBase64(Security.makePasswordDigest("PINS_ARE_WEIRD", pass.toCharArray()));
            if (db.ifValueExists(passw, "USERS", "PASS_WORD")) {
                throw new UserExistsException();
            }
            Long time = System.currentTimeMillis();
            db.query("INSERT INTO USERS VALUES(?,?,?,'" + passw + "',?,NOW(),0,0,?,?,NOW(),?,?)", nextRandom, userName, userName, host, time.toString(), "0", group, action.getActionID());
            setUserProperty("USER_ID", nextRandom);
            setUserProperty("USER_NAME", userName);
            setUserProperty("REAL_NAME", userName);
            setUserProperty("PASS_WORD", passw);
            setUserProperty("HOST", host);
            setUserProperty("LAST_LOGIN", "");
            setUserProperty("IS_LOGGED_IN", "0");
            setUserProperty("IS_DISABLED", "0");
            setUserProperty("IS_PASSWORD_EXPIRED", ((Long) System.currentTimeMillis()).toString());
            setUserProperty("CHANGE_PASSWORD", "0");
            setUserProperty("GROUPS", group);
            setUserProperty("ACTION_ID", action.getActionID());
            action.saveAction();
            Logger.toConsole("new user created", this.getClass());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createUser(String userName, String pass, String host, Database db, String group, Action action) throws UserExistsException {
        if (group == null) {
            group = "unassigned";
        }
        UniqueRandom ur = new UniqueRandom(20);
        String nextRandom = ur.nextRandom();
        // check to ensure the user name is always unique
        try {
            if (db.ifValueExists(userName, "USERS", "USER_NAME")) {
                throw new UserExistsException();
            }
            byte[] bytes = Security.makePasswordDigest("PINS_ARE_WEIRD", pass.toCharArray());
            String passw = Security.toBase64(bytes);
            Long time = System.currentTimeMillis();
            db.query("INSERT INTO USERS VALUES(?,?,?,'" + passw + "',?,NOW(),0,0,?,?,NOW(),?,?)", nextRandom, userName, userName, host, time.toString(), "0", group, action.getActionID());
            setUserProperty("USER_ID", nextRandom);
            setUserProperty("USER_NAME", userName);
            setUserProperty("REAL_NAME", userName);
            setUserProperty("PASS_WORD", passw);
            setUserProperty("HOST", host);
            setUserProperty("LAST_LOGIN", "");
            setUserProperty("IS_LOGGED_IN", "0");
            setUserProperty("IS_DISABLED", "0");
            setUserProperty("IS_PASSWORD_EXPIRED", ((Long) System.currentTimeMillis()).toString());
            setUserProperty("CHANGE_PASSWORD", "0");
            setUserProperty("GROUPS", group);
            setUserProperty("ACTION_ID", action.getActionID());
            action.saveAction();
            Logger.toConsole("new user created", this.getClass());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*-------------------end private implementation--------------*/
}
