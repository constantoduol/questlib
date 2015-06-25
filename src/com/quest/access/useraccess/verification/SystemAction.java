package com.quest.access.useraccess.verification;

import com.quest.access.common.UniqueRandom;
import com.quest.access.common.mysql.Database;
import com.quest.access.control.Server;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import org.json.JSONObject;

/**
 *
 * @author constant oduol
 * @version 1.0(23/6/12)
 */

/**
 * This class tries to store information concerning actions of users in the system
 * this is done using a verification process where users with the relevant privileges
 * or relevant authority can carry out actions in the system.
 * @author Conny
 */
  public class SystemAction implements Action {  
      private String actionID;
      private Database database;
      private String description;  
      public SystemAction(Database db, String description) throws Exception{
                UniqueRandom ur=new UniqueRandom(50);
                actionID=ur.nextMixedRandom();
                this.database=db;
                this.description=description;
             
     }   
      /**
       * 
       * @return a string representing the id of this action
       */
    @Override
      public String getActionID(){
          return this.actionID;
      }
      
      /**
       * this method is called to commit an action as performed by a specific user
       */
    @Override
      public void saveAction(){
          database.query("INSERT INTO USER_ACTIONS VALUES(?,?,?,NOW(),?)", this.actionID,"SYSTEM_000","SYSTEM_000",this.description);
      }
      
      /**
       * this method is called to get details concerning a specific action is string
       */
      public static JSONObject getActionDetails(String actionId,Database db){
        JSONObject set = db.query("SELECT * FROM USER_ACTIONS WHERE ACTION_ID=?",actionId);
        HashMap details=new HashMap();
        details.put("USER_ID",set.optString("USER_ID"));
              details.put("ACTION_TIME",set.optString("ACTION_TIME"));
              details.put("USER_NAME",set.optString("USER_NAME"));
              details.put("ACTION_DESCRIPTION",set.optString("ACTION_DESCRIPTION"));
        
              return set;
            
      
      }
      
}
