/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.quest.servlets;

import com.quest.access.common.io;
import com.quest.access.common.mysql.Database;
import com.quest.access.control.Server;
import com.quest.access.useraccess.Service;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.json.JSONObject;


/** 
 *
 * @author connie
 */


public class ServerLink extends HttpServlet {
    
    private static Server server;

    /** 
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try { 
             response.setContentType("text/html;charset=UTF-8");
             response.setHeader("Access-Control-Allow-Origin", "*"); 
             String json = request.getParameter("json");
             if(json == null) return;
             HttpSession session = request.getSession();
            //session, request, response
             JSONObject obj = new JSONObject(json);
             JSONObject headers = obj.optJSONObject("request_header");
             String msg = headers.optString("request_msg");
             String sessionId = headers.optString("session_id");

             boolean authValid = sessionId != null && Server.getUserSessions().containsKey(sessionId);
             String service = headers.optString("request_svc");
             JSONObject requestData = (JSONObject)obj.optJSONObject("request_object");
             ClientWorker worker = new ClientWorker(msg, service, requestData, session,response,request);
             if(!authRequired(service,worker) || authValid){
                 worker.work();
             }
             else {
                String value = "to use this service you need a valid auth token"; 
                sendMessage(response,"auth_required",value);
             }
        } catch (Exception ex) {
            Logger.getLogger(ServerLink.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private boolean authRequired(String services,ClientWorker worker){
       StringTokenizer st = new StringTokenizer(services,",");
       HashMap allServices = Server.getServices();
       while(st.hasMoreTokens()){
          String service = st.nextToken();
           if (allServices != null) {
               ArrayList serviceList = (ArrayList) allServices.get(service);
               if (serviceList != null) {
                   String privState = serviceList.get(2).toString();
                   Service svc = (Service)serviceList.get(3);
                   svc.runOnPreExecute(server, worker);
                   if(privState.equals("yes")){
                       return true;
                   }
               }
           }
       }
       return false;
    }
    
    public static void sendMessage(HttpServletResponse response,String msgKey, String msgValue){
        try {
            JSONObject object=new JSONObject();  
            object.put("request_msg",msgKey);
            object.put("data",msgValue);
            response.getWriter().print(object);
        } catch (Exception ex) {
            Logger.getLogger(ServerLink.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
 
    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /** 
     * Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /** 
     * Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /** 
     * Returns a short description of the servlet.
     * @return a String containing servlet description
     */ 
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>
    
    @Override 
    public void init(){
         ServletConfig config = getServletConfig();
         String dbName=config.getInitParameter("database-name");
         String passExpires=config.getInitParameter("password-expires");
         String maxRetries=config.getInitParameter("max-password-retries");
         String clientTimeout=config.getInitParameter("client-timeout");
         String mLogin=config.getInitParameter("multiple-login");
         String eDir=config.getInitParameter("extension-dir");
         String defPass=config.getInitParameter("default-password");
         String rootUser = config.getInitParameter("root-user");
         String debug = config.getInitParameter("debug-mode");
         Database.setConfig(config);
         Database db = new Database("user_server");
         try {
            server = new Server(dbName);
            if(debug.equals("true")){
                server.setDebugMode(true);
            }
            else{
                server.setDebugMode(false);
            }
            server.setConfig(config);
            server.initDatabases();
            server.setPassWordLife(Integer.parseInt(passExpires));
            server.setMaxPasswordAttempts(Integer.parseInt(maxRetries));
            server.setClientTimeout(Double.parseDouble(clientTimeout));
            server.setMultipleLoginState(Boolean.parseBoolean(mLogin));
            server.setDefaultPassWord(defPass);
            server.createRootUser(rootUser,db);
            server.startAllServices(db);
        } catch (Exception ex) {
            ex.printStackTrace();
             try {
                 System.out.println(ex);
                 Thread.sleep(20000);
                 init();
             } catch (InterruptedException ex1) {
                 io.log(ex1, Level.SEVERE, this.getClass());
             }
            ex.printStackTrace();
      } 
    }
 
    public static Server getServerInstance(){
        return server;
    }
    
  
}
