
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.quest.access.useraccess.services;

import com.quest.access.common.UniqueRandom;
import com.quest.access.common.io;
import com.quest.access.common.mysql.Database;
import com.quest.access.control.Server;
import com.quest.access.useraccess.NonExistentUserException;
import com.quest.access.useraccess.Serviceable;
import com.quest.access.useraccess.User;
import com.quest.access.useraccess.services.annotations.Endpoint;
import com.quest.access.useraccess.services.annotations.Model;
import com.quest.access.useraccess.services.annotations.Models;
import com.quest.access.useraccess.services.annotations.WebService;
import com.quest.access.useraccess.verification.UserAction;
import com.quest.servlets.ClientWorker;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletConfig;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 *
 * @author Connie
 */

@WebService(name = "open_data_service", level = 10, privileged = "no")
@Models(models = {
    @Model(
            database = "user_server", table = "BUSINESS_DATA",
            columns = {"ID VARCHAR(20) PRIMARY KEY",
                "BUSINESS_NAME TEXT",
                "COUNTRY TEXT",
                "CITY TEXT",
                "POSTAL_ADDRESS TEXT",
                "PHONE_NUMBER TEXT",
                "COMPANY_WEBSITE TEXT",
                "BUSINESS_TYPE VARCHAR(10)",
                "BUSINESS_OWNER TEXT",
                "BUSINESS_EXTRA_DATA TEXT",
                "CREATED DATETIME"
            }),
    @Model(
            database = "user_server", table = "BUSINESS_USERS",
            columns = {"ID VARCHAR(20) PRIMARY KEY",
                "USER_NAME TEXT",
                "BUSINESS_ID TEXT",
                "CREATED DATETIME"
            }),
      @Model(
                database = "user_server", table = "ACTIVATION_DATA",
                columns = {
                        "ACTIVATION_KEY VARCHAR(25)",
                        "BUSINESS_NAME TEXT",
                        "CREATED DATETIME"
                }),
      @Model(
            database = "user_server", table = "CONF_DATA",
            columns = {
                "CONF_KEY TEXT",
                "CONF_VALUE TEXT"
            })
        
}
)
public class OpenDataService implements Serviceable {
    
    private static final String SERVER_ENDPOINT = "https://quest-uza.appspot.com/server";
    
    private static final String NEXT_URL = "http://uza.questpico.com";
    
    private static final String USER_DATA = "user_server";
    
    
    @Endpoint(name = "fetch_settings")
    public void fetchSettings(Server serv, ClientWorker worker) {
        Database db = new Database(USER_DATA, worker.getSession());
        JSONObject data = db.query("SELECT * FROM CONF_DATA");
        worker.setResponseData(data);
        serv.messageToClient(worker);
    }

    
    
    @Endpoint(name = "save_settings")
    public void saveSettings(Server serv, ClientWorker worker) {
        Database db = new Database(USER_DATA, worker.getSession());
        JSONObject request = worker.getRequestData();
        request.remove("business_id");
        Iterator iter = request.keys();
        while (iter.hasNext()) {
            String key = iter.next().toString();
            String value = request.optString(key);
            //check that the key exists
            boolean exists = db.ifValueExists(key, "CONF_DATA", "CONF_KEY");
            if (exists) {
                db.query()
                        .update("CONF_DATA")
                        .set("CONF_VALUE='" + value + "'")
                        .where("CONF_KEY='" + key + "'")
                        .execute();
            } else {
                db.doInsert("CONF_DATA", new String[]{key, value});
            }
        }
        worker.setResponseData(Message.SUCCESS);
        serv.messageToClient(worker);
    }
    
    @Endpoint(name="business_info")
    public JSONObject getBusinessInfo(Server serv, ClientWorker worker) throws JSONException{
      //we need to get the business id
        Database db = new Database(USER_DATA,worker.getSession());
        JSONObject request = worker.getRequestData();
        String email = request.optString("username");
        JSONObject data = db.query("SELECT BUSINESS_ID,BUSINESS_NAME,BUSINESS_TYPE,BUSINESS_EXTRA_DATA from BUSINESS_USERS, "
                + "BUSINESS_DATA where BUSINESS_USERS.BUSINESS_ID = BUSINESS_DATA.ID AND BUSINESS_USERS.USER_NAME = ?",email);
        JSONObject response = new JSONObject();
        response.put("business_ids", data.optJSONArray("BUSINESS_ID"));
        response.put("business_names", data.optJSONArray("BUSINESS_NAME"));
        response.put("business_types", data.optJSONArray("BUSINESS_TYPE"));
        response.put("business_extra_data",data.optJSONArray("BUSINESS_EXTRA_DATA"));
        worker.setResponseData(response);
        serv.messageToClient(worker);
        return response;
    }
    
    
    @Endpoint(name = "business_data")
    public void businessData(Server serv, ClientWorker worker) throws JSONException {
        //we need to get the business id
        Database db = new Database(USER_DATA,worker.getSession());
        JSONObject request = worker.getRequestData();
        String id = request.optString("business_id");
        JSONObject data = db.query("SELECT * FROM BUSINESS_DATA WHERE ID = ? ORDER BY BUSINESS_NAME ASC",id);
        worker.setResponseData(data);
        serv.messageToClient(worker);
    }
    
    
    @Endpoint(name = "save_business")
    public void saveBusiness(Server serv, ClientWorker worker) {
        Database db = new Database(USER_DATA,worker.getSession());
        JSONObject request = worker.getRequestData();
        String name = request.optString("business_name");
        String country = request.optString("country");
        String city = request.optString("city");
        String pAddress = request.optString("postal_address");
        String pNumber = request.optString("phone_number");
        String web = request.optString("company_website");
        String type = request.optString("business_type");
        String owner = request.optString("business_owner");
        String saveType = request.optString("action_type");
        String currentBusId = request.optString("business_id");
        String bExtra = request.optString("business_extra_data");
        boolean exists = db.ifValueExists(new String[]{name,owner},"BUSINESS_DATA",new String[]{"BUSINESS_NAME","BUSINESS_OWNER"});
        //if this business exists under this owner do not create a new one, just update it
        if(saveType.equals("update")){
            Database.executeQuery("UPDATE BUSINESS_DATA SET BUSINESS_NAME=?, "
                    + "COUNTRY=?, CITY=?, POSTAL_ADDRESS=?, "
                    + "PHONE_NUMBER=?, COMPANY_WEBSITE=?, "
                    + "BUSINESS_TYPE=?, BUSINESS_EXTRA_DATA = ?  WHERE ID = ? ", db,
                    name, country, city,
                    pAddress, pNumber, web, type,bExtra,currentBusId);
        }
        else if(saveType.equals("create") && !exists){ 
            UniqueRandom rand = new UniqueRandom(20);
            String busId = rand.nextMixedRandom();
            db.doInsert("BUSINESS_DATA", new String[]{busId, name, country, city, pAddress, pNumber, web, type,owner,bExtra, "!NOW()"});
            db.doInsert("BUSINESS_USERS",new String[]{rand.nextMixedRandom(),owner,busId,"!NOW()"});
        }
        else if(saveType.equals("delete")){
            //delete user data
            //this is an open service so check for privileges manually here
            if (!hasPrivilege("pos_admin_service", worker)) {
                //no privilege found, so what next
                worker.setResponseData(Message.FAIL);
                worker.setReason("Insufficient privileges");
                serv.messageToClient(worker);
                return;
            }
            //if this is his only business don't delete it
            String email = worker.getSession().getAttribute("username").toString();
            JSONObject data = db.query("SELECT BUSINESS_OWNER FROM BUSINESS_DATA WHERE BUSINESS_OWNER = ?",email);
            int businesses = data.optJSONArray("BUSINESS_OWNER").length();
            if(businesses < 2){
               worker.setResponseData(Message.FAIL);
               worker.setReason("You cannot delete the only business you have!");
               serv.messageToClient(worker);  
               return;
            }
            db.query("DELETE FROM BUSINESS_DATA WHERE ID = ?",currentBusId);
            db.query("DELETE FROM BUSINESS_USERS WHERE BUSINESS_ID= ?",currentBusId);
        }
        worker.setResponseData(Message.SUCCESS);
        serv.messageToClient(worker);
    }
    
    
    private boolean hasPrivilege(String privilege,ClientWorker worker){
        JSONArray privs = (JSONArray) worker.getSession().getAttribute("privileges");
        io.out("privileges : "+privs);
        if(privs == null){
            return false;
        }
        return privs.toList().contains(privilege);
    }
     
    private void createLocalAccount(Server serv, ClientWorker worker){
        //we manually need to check for privileges
        Database db = new Database(USER_DATA,worker.getSession());
        if(!hasPrivilege("user_service", worker)){
            //no privilege found
            worker.setResponseData(Message.FAIL);
            worker.setReason("Insufficient privileges");
            serv.messageToClient(worker);
            return;
        }
        JSONObject details = worker.getRequestData();
        UserService us = new UserService();
        User user = us.createUser(serv, worker);
        String email = details.optString("name");
        String busId = details.optString("business_id");
        if(user != null){
           String id = new UniqueRandom(20).nextMixedRandom();
           db.doInsert("BUSINESS_USERS",new String[]{id,email,busId,"!NOW()"});  
        }
    }
    
    
    private List listToLowerCase(List<String> list){
        ArrayList newList = new ArrayList();
        for(int x = 0; x < list.size(); x++){
            String str = list.get(x).toLowerCase();
            newList.add(str);
        }
        return newList;
    }
    
    @Endpoint(name="forgot_password")
    public void forgotPassword(Server serv, ClientWorker worker) throws JSONException {
        try {
            Database db = new Database(USER_DATA,worker.getSession());
            JSONObject details = worker.getRequestData();
            String email = details.optString("user_name");
            String bussName = details.optString("business_name");
            //check locally to see whether its valid
            User user = User.getExistingUser(email, db);
            worker.setPropagateResponse(false);
            JSONObject buss = getBusinessInfo(serv, worker);
            worker.setPropagateResponse(true);
            io.out(buss);
            if (!listToLowerCase(buss.optJSONArray("business_names").toList()).contains(bussName)) {
                //no business here
                worker.setResponseData(Message.FAIL);
                worker.setReason("The specified business does not exist for specified email address");
                serv.messageToClient(worker);
                return;
            }
            String body = serv.getEmailTemplate("forgot-password");
            String senderEmail = serv.getConfig().getInitParameter("sender-email");
            String[] from = new String[]{senderEmail, "Quest Pico"};
            String [] to = new String[]{email,email};
            body = body.replace("{user_name}", email);
            String pass = new UniqueRandom(6).nextMixedRandom();
            user.setPassWord(pass);
            user.setUserProperty("CHANGE_PASSWORD", "1", db);
            body = body.replace("{pass_word}", pass);
            body = body.replace("{change_link}",NEXT_URL+"/change.html?user_name="+email+"&pass_word="+pass);
            serv.sendEmail(from, to,"Password Reset", body);
            worker.setResponseData(Message.SUCCESS);
            serv.messageToClient(worker);
        } catch (NonExistentUserException ex) {
            worker.setResponseData(Message.FAIL);
            worker.setReason("The specified email address does not belong to any account");
            serv.messageToClient(worker);
        }
    }

    @Endpoint(name = "create_account")
    public void createAccount(Server serv, ClientWorker worker) throws Exception {
        createLocalAccount(serv, worker);
    }

    @Endpoint(name="activate_account")
    public void activateAccount(Server serv, ClientWorker worker) throws IOException, NonExistentUserException {
        Database db = new Database(USER_DATA,worker.getSession());
        JSONObject details = worker.getRequestData();
        String email = details.optString("user_name");
        String actionId = details.optString("action_id");
        String nextUrl = details.optString("next_url");
        String busId = details.optString("business_id");
        User user = User.getExistingUser(email, db);
        //first check if the specified user is already activated
        boolean userExists = !user.getUserProperty("USER_NAME").isEmpty();
        if(!userExists){
            worker.setResponseData(Message.FAIL);
            worker.setReason("User account seems to be invalid");
            serv.messageToClient(worker);
            return;
        }
        
        boolean userDisabled = user.getUserProperty("IS_DISABLED").equals("1");
        if (!userDisabled) {
            //if the user is not disabled, it means this account has already been activated
            worker.setResponseData(Message.FAIL);
            worker.setReason("User account has already been activated");
            serv.messageToClient(worker);
            return;
        }
        //here we are dealing with a disabled user
        //check that the action id matches what we have
        HashMap actionDetails = UserAction.getActionDetails(actionId, db);
        String userName = actionDetails.get("ACTION_DESCRIPTION").toString();
        //if userName === email then we are happy
        if(userName.equals(email)){
            //well this is a valid activation,do something cool
            //send a redirect to the next url
            //add to specified business
            //enable the user
            user.setUserProperty("IS_DISABLED", "0", db);
            String id = new UniqueRandom(20).nextMixedRandom();
            if(!busId.isEmpty()){
                db.doInsert("BUSINESS_USERS",new String[]{id,email,busId,"!NOW()"}); 
            }
            worker.getResponse().sendRedirect(nextUrl);
        }

    }
    
    
    @Endpoint(name="logout")
    public void logout(Server serv,ClientWorker worker){
        JSONObject requestData = worker.getRequestData();
        String userName = requestData.optString("user_name");
        serv.doLogOut(worker, userName);
        worker.setResponseData("success");
        serv.messageToClient(worker);
    }
    
    @Endpoint(name="login")
    public void login(Server serv,ClientWorker worker) throws JSONException, UnknownHostException{
        JSONObject requestData = worker.getRequestData();
        String remoteAddr = worker.getRequest().getRemoteAddr();
        requestData.put("clientip",remoteAddr);
        serv.doLogin(worker);
    }
    
    @Endpoint(name="changepass")
    public void changePass(Server serv,ClientWorker worker){
        JSONObject requestData = worker.getRequestData();
        String userName = requestData.optString("user_name");
        String oldPass = requestData.optString("old_password");
        String newPass = requestData.optString("new_password");
        Boolean change = User.changePassword(worker.getDatabase(), userName, oldPass, newPass);
        worker.setResponseData(change);
        serv.messageToClient(worker);
    }

    @Override
    public void service() {
     
    }

    
    @Override
    public void onPreExecute(Server serv, ClientWorker worker) {
        
    }

    @Override
    public void onStart(Server serv)  {
        try {
            createNativeBusiness(serv);
        } catch (JSONException ex) {
            Logger.getLogger(OpenDataService.class.getName()).log(Level.SEVERE, null, ex);
        }
       
    }
    
    private void createNativeBusiness(Server serv) throws JSONException{
        JSONObject request = new JSONObject();
        request.put("business_name","Quest Test Business");
        request.put("country","Kenya");
        request.put("city","Nairobi");
        request.put("postal_address","30178");
        request.put("phone_number","0729936172");
        request.put("company_website","www.questpico.com");
        request.put("business_type","goods");
        request.put("business_owner","root@questpico.com");
        request.put("action_type", "create");
        ClientWorker worker = new ClientWorker("save_business", "open_data_service",request, null, null, null);
        worker.setPropagateResponse(false);
        saveBusiness(serv, worker);
    }
    
    @Endpoint(name = "activate_product")
    public void activateProduct(Server serv,ClientWorker worker){
        Activation ac = new Activation();
        ac.validateKey(serv,worker);
        
    }
}
