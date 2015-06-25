
package com.quest.access.useraccess;

import com.quest.access.control.Server;
import com.quest.servlets.ClientWorker;

/**
 *
 * @author constant oduol
 * @version 1.0(10/5/12)
 */

/**
 * This file defines an interface used by service providing classes
 * when a class implements this interface it means that it provides a
 * specific service to a client this interface forces the service classes
 * to implement the method service() which is called when a client makes a
 * request to a service. This interface is also implemented by dynamic proxies in
 * order to control access to a specific service. The service() method of a 
 * particular service is only invoked if the current user was assigned the permanent privilege
 * associated with that service
 */
public interface Serviceable {
 
    
    
    /**
     * dummy method
     */
    public void service();
        
    /**
     * this method is called the first time this service is invoked by the server.
     * This method can be used to invoke database connectivity methods of this service
     * @param serv
     */
    public void onStart(Server serv);
    
    /**
     * this method is called before the service 
     * @param serv
     * @param worker
     */
    
    public void onPreExecute(Server serv,ClientWorker worker);
}
