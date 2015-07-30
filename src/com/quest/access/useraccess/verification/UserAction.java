package com.quest.access.useraccess.verification;

import com.quest.access.common.UniqueRandom;
import com.quest.access.common.io;
import com.quest.access.common.mysql.Database;
import com.quest.access.control.Server;
import com.quest.servlets.ClientWorker;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.http.HttpSession;
import org.json.JSONObject;

/**
 *
 * @author constant oduol
 * @version 1.0(23/6/12)
 */
/**
 * This class tries to store information concerning actions of users in the
 * system this is done using a verification process where users with the
 * relevant privileges or relevant authority can carry out actions in the
 * system.
 *
 * @author Conny
 */
public class UserAction implements Action {

    private String actionID;
    private Database database;
    private String description;
    private static ConcurrentHashMap actions = new ConcurrentHashMap();
    private static ConcurrentHashMap userNames = new ConcurrentHashMap();
    private HttpSession clientSession;
    private static final String[] excludeNames = new String[]{"root"}; // this marks people who need no verification

    static {
        Arrays.sort(excludeNames);
    }

    /**
     * Constructs a user action object that describes what a user has done the
     * action is not saved until the instance method saveAction is called
     * @param worker
     * @param description
     */

    public UserAction(ClientWorker worker, String description)  {
        UniqueRandom ur = new UniqueRandom(50);
        actionID = ur.nextMixedRandom();
        this.database = worker.getDatabase();
        this.description = description;
        this.clientSession = worker.getSession();

    }
    /**
     *
     * @return a string representing the id of this action
     */
    @Override
    public String getActionID() {
        return this.actionID;
    }

    /**
     * this method is called to commit an action as performed by a specific user
     */
    @Override
    public void saveAction() {
        String userName = (String) this.clientSession.getAttribute("username");
        String userID = (String) this.clientSession.getAttribute("userid");
        database.query("INSERT INTO USER_ACTIONS"
                + " VALUES(?,?,?,NOW(),?)", this.actionID, userID, userName, this.description);
    }

    public static HashMap getActionDetails(String actionId, Database db) {
        JSONObject set = db.query("SELECT * FROM USER_ACTIONS WHERE ACTION_ID=?", actionId);
        HashMap details = new HashMap();
        details.put("USER_ID", set.optString("USER_ID"));
        details.put("ACTION_TIME", set.optString("ACTION_TIME"));
        details.put("USER_NAME", set.optString("USER_NAME"));
        details.put("ACTION_DESCRIPTION", set.optString("ACTION_DESCRIPTION"));

        return details;

    }

    public static void verifyAction(String serialID, ClientWorker worker, Server serv) throws PendingVerificationException, IncompleteVerificationException, NonExistentSerialException {
        //get the stored user verification object
        String userName = (String) userNames.get(serialID);
        Object[] obj = (Object[]) actions.get(userName);
        if (obj != null) {
            UserVerification ver = (UserVerification) obj[1];
            ClientWorker work = (ClientWorker) obj[0];
            worker.setRequestData(work.getRequestData()); //take the request data from the first request
            worker.setMessage(work.getMessage());
            ver.verify(worker, serialID, serv);
        }
    }

    /**
     *
     * @return the currently unverified actions
     */
    public static ConcurrentHashMap getUserActions() {
        return actions;
    }

    /**
     *
     * @return usernames of users with pending actions
     */
    public static ConcurrentHashMap getUserNames() {
        return userNames;
    }

}
