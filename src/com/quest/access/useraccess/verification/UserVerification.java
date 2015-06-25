
package com.quest.access.useraccess.verification;

import com.quest.access.control.Server;
import com.quest.servlets.ClientWorker;



/**
 * This class is the super class of all the types of constraints that will
 * be used to determine how actions are verified on the system
 * @author Conny
 */
public class UserVerification {
   
    public UserVerification(){
       
    }
    
    /**
     * this method is called in user action to verify whether a user can carry
     * out an action, this method has no implementation here and classes
     * extending this class should provide an implementation for it
     * @param clientID this is the id of the client trying to commit the action
     * @param serviceName this is the name of the service that is currently being invoked
     * @param message this is the specific message to be sent to the service
     * @param serialID this is the id generated for an unverified action
     * @param param these are the parameters that the service to be invoked was to be invoked with
     * @param seerv this is the server the client is operating on
     */
    public void verify(ClientWorker worker,String userName,Server serv) throws NonExistentSerialException, PendingVerificationException, IncompleteVerificationException{}

    

}
