/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.quest.servlets;

import com.quest.access.common.UniqueRandom;
import com.quest.access.common.mysql.Database;
import com.quest.access.control.Server;
import com.quest.access.useraccess.NonExistentUserException;
import com.quest.access.useraccess.User;
import java.io.PrintWriter;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.json.JSONObject;

/**
 *
 * @author connie
 */
public class ClientWorker {

    private String msg;
    private String service;
    private JSONObject requestData;
    private Object responseData;
    private HttpSession session;
    private String reason;
    private String id;
    private String rootId;
    private HttpServletResponse response;
    private HttpServletRequest request;
    private static Server server = ServerLink.getServerInstance();
    private Database database;
    private static UniqueRandom randomGen = new UniqueRandom(20);
    private boolean propagateResponse;

    /**
     *
     * @param msg the message specifying the method to be invoked on the server
     * @param service the service specifies the class containing the method to
     * be invoked on the server
     * @param requestData this is data the client is sending to the server
     * @param session this is a session object representing the current client
     * @param ctx this is the asynchronous context bound to the current request
     * @param response this is the response where the data will be sent back to
     * the client
     * @param request this is the http servlet request object
     */
    public ClientWorker(String msg, String service, JSONObject requestData,
            HttpSession session, HttpServletResponse response, HttpServletRequest request) {
        this.msg = msg;
        this.service = service;
        this.requestData = requestData;
        this.session = session;
        this.response = response;
        this.id = randomGen.nextMixedRandom();
        this.propagateResponse = true; //this is true if we wish that this client worker always responds to the client
        this.request = request;
        this.database = new Database("user_server");
    }

    @Override
    public String toString() {
        return "Message : " + msg + ", Service : " + service + " , Request_data : " + requestData;
    }

    public void work() {
        try {
            server.invokeMultipleServices(this);
        } catch (Exception e) {
            this.setResponseData(e);
            this.setReason(e.getMessage());
            server.exceptionToClient(this);
        }
    }

    /**
     *
     * @return this is the message used to know the method to be invoked on the
     * server
     */
    public String getMessage() {
        return this.msg;
    }

    /**
     *
     * @return this is used to determine the class containing the method to be
     * invoked
     */
    public String getService() {
        return this.service;
    }

    /**
     *
     * @return returns the data sent to the server represented by this client
     * worker object
     */
    public JSONObject getRequestData() {
        return this.requestData;
    }

    public Object getResponseData() {
        return this.responseData;
    }

    public HttpServletResponse getResponse() {
        return this.response;
    }

    public HttpServletRequest getRequest() {
        return this.request;
    }

    /**
     * this method is used to enable or disable response propagation to the end
     * client
     *
     * @param propResponse true or false depending on whether we wish to
     * propagate or not
     */
    public void setPropagateResponse(boolean propResponse) {
        this.propagateResponse = propResponse;
    }

    /**
     * this returns the state of response propagation to the end client
     *
     * @return true or false depending on propagation mode selected
     */
    public boolean getPropagateResponse() {
        return this.propagateResponse;
    }

    public HttpSession getSession() {
        return this.session;
    }

    public void setMessage(String msg) {
        this.msg = msg;
    }

    public void setService(String svc) {
        this.service = svc;
    }

    public String getID() {
        return this.id;
    }

    /**
     * this sets the id of the initial worker that spawned this worker
     *
     * @param rootId
     */
    public void setRootWorkerID(String rootId) {
        this.rootId = rootId;
    }

    public Database getDatabase() {
        return this.database;
    }

    /**
     * this gets the id of the initial worker that spawned this worker
     */
    public String getRootWorkerID() {
        return this.rootId;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return this.reason;
    }

    public void setRequestData(JSONObject data) {
        this.requestData = data;
    }

    public void setResponseData(Object data) {
        this.responseData = data;
    }

    /**
     * @param resp this is a json object containing the data we are sending to
     * the client when the data is sent to the client the asynchronous context
     * is completed
     */
    public void toClient(JSONObject resp) {
        try {
            JSONObject toClient = new JSONObject();
            toClient.put("response", resp);
            toClient.put("message", msg);
            PrintWriter writer = response.getWriter();
            writer.println(toClient);
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }

    private double compareDate(long last, long current) {
        long duration = current - last;
        return (double) duration / 60000.0;
    }

    private boolean checkTimeout() {
        String clientID = session.getId();
        ConcurrentHashMap<String, Long> lastClientRequest = server.getTimeoutData();
        if (server.getClientTimeout() != 0) { // server is set to timeout clients
            boolean contains = lastClientRequest.containsKey(clientID);
            long currentRequest = new Date().getTime();
            if (contains) {  // client has made a request before 
                long lastRequest = lastClientRequest.get(clientID);
                if (compareDate(lastRequest, currentRequest) > server.getClientTimeout()) { // client has timed out
                    if (session != null) {
                        try {
                            String name = (String) session.getAttribute("username");
                            User user = User.getExistingUser(name, database);
                            boolean userLoggedIn = user.getUserProperty("IS_LOGGED_IN").equals("1");
                            if (userLoggedIn) {
                                server.doLogOut(this, name);
                                lastClientRequest.replace(clientID, currentRequest);
                                server.resetTimeout(clientID);
                                server.resetLoginAttempts(name);
                                this.setResponseData(new Exception("User has been timed out"));
                                server.exceptionToClient(this);
                                return true;
                            } else {
                                this.setResponseData(new Exception("User has been timed out"));
                                server.exceptionToClient(this);
                                return true;
                            }
                        } catch (NonExistentUserException ex) {
                            Logger.getLogger(ClientWorker.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    return true;
                }
                lastClientRequest.replace(clientID, currentRequest);
                return false;
            } else { // this is a clients first request
                lastClientRequest.put(clientID, currentRequest);
                return false;
            }

        }
        return false;

    }

}
