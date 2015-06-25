
package com.quest.access.useraccess.verification;


import com.quest.access.control.Server;
import com.quest.servlets.ClientWorker;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.http.HttpSession;

/**
 * This class provides a verification process based on the number of users
 * required to verify an action
 * @author Conny
 */
public class NumberVerification extends UserVerification {
     private int number;
     public NumberVerification(int number){
         this.number=number;
         
     }
     
     @Override
      public void verify(ClientWorker worker,String serial, Server serv) throws NonExistentSerialException ,PendingVerificationException, IncompleteVerificationException{
          /* if number<=1 this means that this action requires only one user to carry out
                  therefore just return and allow for the action to take place
                * 
                */
                 if(number<=1){
                    return; 
                 } 
              
                //get all unverified actions before proceeding
                 ConcurrentHashMap actions=UserAction.getUserActions();
                 ConcurrentHashMap userNames=UserAction.getUserNames();
                 String userName=(String) userNames.get(serial);
                 System.out.println(serial+" in the final verification method");
                 /* 
                  * When we get here previous action Id should not be null because an action serial was
                  * generated and given to the end user, hence if it is null throw an exception to show
                  * that that is illegal
                  */
                 
                 
                  if(serial==null){
                     throw new IncompleteVerificationException();   
                   }
                  else{
                       //check to see whether such a username exists
                       if(userNames.containsKey(serial)){
                          // if it exists this means this action has not been verified
                          //check to see if the count equals number required otherwise throw an exception
                          int val=(Integer)((Object[]) actions.get(userName))[2];
                           val++;
                          ((Object[]) actions.get(userName))[2]=val;
                          //if val==number it means the required number of users has been reached
                          if(val==number){
                              //mark that the action has been done
                               HttpSession ses=worker.getSession();
                               ses.setAttribute("action_done",true);
                             //commit the action
                               serv.invokeService(worker);
                               System.out.println("service invoked finally....");
                               System.out.println(worker.getRequestData());
                               System.out.println("final service invoked: "+worker.getService());
                               System.out.println("final message invoked: "+worker.getMessage());
                               //always remember to remove the serial of an action that has been committed
                               actions.remove(userName);
                               userNames.remove(serial);
                              
                            }
                         

                       }
                        else{
                             // this means the serial entered does not exist so inform the user that the serial is invalid
                             throw new NonExistentSerialException();  
                       }
                  }
                 
     }
}
