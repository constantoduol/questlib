
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
import com.quest.access.useraccess.Serviceable;
import com.quest.access.useraccess.User;
import com.quest.access.useraccess.UserExistsException;
import com.quest.access.useraccess.services.annotations.Endpoint;
import com.quest.access.useraccess.services.annotations.Model;
import com.quest.access.useraccess.services.annotations.Models;
import com.quest.access.useraccess.services.annotations.WebService;
import com.quest.servlets.ClientWorker;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
    
    private static JSONObject busSettings;

    private static final String USER_DATA = "user_server";

    
    public static String getSetting(String key) {
        if (busSettings == null) {
            return "_notexist_";
        }
        List keys = busSettings.optJSONArray("CONF_KEY").toList();
        List values = busSettings.optJSONArray("CONF_VALUE").toList();
        int index = keys.indexOf(key);
        return index == -1 ? "_notexist_" : values.get(index).toString();
    }
    
    @Endpoint(name = "fetch_settings")
    public void fetchSettings(Server serv, ClientWorker worker) {
        Database db = new Database(USER_DATA);
        JSONObject data = db.query("SELECT * FROM CONF_DATA");
        worker.setResponseData(data);
        serv.messageToClient(worker);
    }

    @Endpoint(name = "save_settings")
    public void saveSettings(Server serv, ClientWorker worker) {
        Database db = new Database(USER_DATA);
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
        
        busSettings = db.query("SELECT * FROM CONF_DATA");
        worker.setResponseData(Message.SUCCESS);
        serv.messageToClient(worker);
    }

    @Endpoint(name = "business_info")
    public JSONObject getBusinessInfo(Server serv, ClientWorker worker) throws JSONException {
        //we need to get the business id
        Database db = new Database(USER_DATA);
        JSONObject request = worker.getRequestData();
        String email = request.optString("username");
        JSONObject data = db.query("SELECT BUSINESS_ID,BUSINESS_NAME,BUSINESS_TYPE,BUSINESS_EXTRA_DATA from BUSINESS_USERS, "
                + "BUSINESS_DATA where BUSINESS_USERS.BUSINESS_ID = BUSINESS_DATA.ID AND BUSINESS_USERS.USER_NAME = ?", email);
        JSONObject response = new JSONObject();
        response.put("business_ids", data.optJSONArray("BUSINESS_ID"));
        response.put("business_names", data.optJSONArray("BUSINESS_NAME"));
        response.put("business_types", data.optJSONArray("BUSINESS_TYPE"));
        response.put("business_extra_data", data.optJSONArray("BUSINESS_EXTRA_DATA"));
        worker.setResponseData(response);
        serv.messageToClient(worker);
        return response;
    }

    @Endpoint(name = "business_data")
    public void businessData(Server serv, ClientWorker worker) throws JSONException {
        //we need to get the business id
        Database db = new Database(USER_DATA);
        JSONObject request = worker.getRequestData();
        String id = request.optString("business_id");
        JSONObject data = db.query("SELECT * FROM BUSINESS_DATA WHERE ID = ? ORDER BY BUSINESS_NAME ASC", id);
        worker.setResponseData(data);
        serv.messageToClient(worker);
    }

    @Endpoint(name = "save_business")
    public void saveBusiness(Server serv, ClientWorker worker) {
        Database db = new Database(USER_DATA);
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
        boolean exists = db.ifValueExists(new String[]{name, owner}, "BUSINESS_DATA", new String[]{"BUSINESS_NAME", "BUSINESS_OWNER"});
        //if this business exists under this owner do not create a new one, just update it
        if (saveType.equals("update")) {
            Database.executeQuery("UPDATE BUSINESS_DATA SET BUSINESS_NAME=?, "
                    + "COUNTRY=?, CITY=?, POSTAL_ADDRESS=?, "
                    + "PHONE_NUMBER=?, COMPANY_WEBSITE=?, "
                    + "BUSINESS_TYPE=?, BUSINESS_EXTRA_DATA = ?  WHERE ID = ? ", db,
                    name, country, city,
                    pAddress, pNumber, web, type, bExtra, currentBusId);
        } else if (saveType.equals("create") && !exists) {
            UniqueRandom rand = new UniqueRandom(20);
            String busId = rand.nextMixedRandom();
            db.doInsert("BUSINESS_DATA", new String[]{busId, name, country, city, pAddress, pNumber, web, type, owner, bExtra, "!NOW()"});
            db.doInsert("BUSINESS_USERS", new String[]{rand.nextMixedRandom(), owner, busId, "!NOW()"});
        } else if (saveType.equals("delete")) {
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
            JSONObject data = db.query("SELECT BUSINESS_OWNER FROM BUSINESS_DATA WHERE BUSINESS_OWNER = ?", email);
            int businesses = data.optJSONArray("BUSINESS_OWNER").length();
            if (businesses < 2) {
                worker.setResponseData(Message.FAIL);
                worker.setReason("You cannot delete the only business you have!");
                serv.messageToClient(worker);
                return;
            }
            db.query("DELETE FROM BUSINESS_DATA WHERE ID = ?", currentBusId);
            db.query("DELETE FROM BUSINESS_USERS WHERE BUSINESS_ID= ?", currentBusId);
        }
        worker.setResponseData(Message.SUCCESS);
        serv.messageToClient(worker);
    }

    private boolean hasPrivilege(String privilege, ClientWorker worker) {
        JSONArray privs = (JSONArray) worker.getSession().getAttribute("privileges");
        io.out("privileges : " + privs);
        if (privs == null) {
            return false;
        }
        return privs.toList().contains(privilege);
    }

    private void createLocalAccount(Server serv, ClientWorker worker) {
        //we manually need to check for privileges
        Database db = new Database(USER_DATA);
        if (!hasPrivilege("user_service", worker)) {
            //no privilege found
            worker.setResponseData(Message.FAIL);
            worker.setReason("Insufficient privileges");
            serv.messageToClient(worker);
            return;
        }
        JSONObject details = worker.getRequestData();
        UserService us = new UserService();
        try {
            worker.setPropagateResponse(false);
            User user = us.createUser(serv, worker);
            worker.setPropagateResponse(true);
            String email = details.optString("name");
            String busId = details.optString("business_id");
            String id = new UniqueRandom(20).nextMixedRandom();
            if(user.getUserProperty("USER_ID").length() > 0){
                db.doInsert("BUSINESS_USERS", new String[]{id, email, busId, "!NOW()"});
                worker.setResponseData(Message.SUCCESS);
            }
            else {
                worker.setResponseData(Message.FAIL);
                worker.setReason("User "+email+" already exists");
            }
            serv.messageToClient(worker);
        } catch (UserExistsException e) {
           
        }

    }

    private List listToLowerCase(List<String> list) {
        ArrayList newList = new ArrayList();
        for (int x = 0; x < list.size(); x++) {
            String str = list.get(x).toLowerCase();
            newList.add(str);
        }
        return newList;
    }

    @Endpoint(name = "create_account")
    public void createAccount(Server serv, ClientWorker worker) throws Exception {
        createLocalAccount(serv, worker);
    }


    @Endpoint(name = "logout")
    public void logout(Server serv, ClientWorker worker) {
        JSONObject requestData = worker.getRequestData();
        String userName = requestData.optString("user_name");
        serv.doLogOut(worker, userName);
        worker.setResponseData("success");
        serv.messageToClient(worker);
    }

    @Endpoint(name = "login")
    public void login(Server serv, ClientWorker worker) throws JSONException, UnknownHostException {
        JSONObject requestData = worker.getRequestData();
        String remoteAddr = worker.getRequest().getRemoteAddr();
        requestData.put("clientip", remoteAddr);
        serv.doLogin(worker);
    }

    @Endpoint(name = "changepass")
    public void changePass(Server serv, ClientWorker worker) {
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
    public void onStart(Server serv) {
        try {
            Database db = new Database("user_server");
            busSettings = db.query("SELECT * FROM CONF_DATA");
            createNativeBusiness(serv);
            loadInitSettings(serv);
        } catch (JSONException ex) {
            Logger.getLogger(OpenDataService.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void createNativeBusiness(Server serv) throws JSONException {
        JSONObject request = new JSONObject();
        request.put("business_name", "Quest Test Business");
        request.put("country", "Kenya");
        request.put("city", "Nairobi");
        request.put("postal_address", "30178");
        request.put("phone_number", "0729936172");
        request.put("company_website", "www.questpico.com");
        request.put("business_type", "goods");
        request.put("business_owner", "root@questpico.com");
        request.put("action_type", "create");
        ClientWorker worker = new ClientWorker("save_business", "open_data_service", request, null, null, null);
        worker.setPropagateResponse(false);
        saveBusiness(serv, worker);
    }
    
     private void loadInitSettings(Server serv) throws JSONException {
        io.out("user_interface : "+getSetting("user_interface"));
        if(!getSetting("user_interface").equals("_notexist_")) return;
            //settings have been initialized
        io.out("settings initialized");
        JSONObject request = new JSONObject();
        request.put("enable_undo_sales", "1");
        request.put("add_tax", "1");
        request.put("add_comm", "1");
        request.put("add_discounts", "1");
        request.put("add_purchases", "0");
        request.put("track_stock", "1");
        request.put("user_interface", "desktop");
        request.put("no_of_receipts", "1");
        request.put("receipt_header", "");
        request.put("allow_discounts", "0");
        request.put("receipt_footer", "");
        request.put("currency", "KES");
        ClientWorker worker = new ClientWorker("save_settings", "open_data_service", request, null, null, null);
        worker.setPropagateResponse(false);
        saveSettings(serv, worker);
    }
     

    @Endpoint(name = "activate_product")
    public void activateProduct(Server serv, ClientWorker worker) {
        Activation ac = new Activation();
        ac.validateKey(serv, worker);
    }
}
