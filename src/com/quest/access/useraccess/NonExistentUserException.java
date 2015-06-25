
package com.quest.access.useraccess;

/**
 *
 * @author constant oduol
 * @version 1.0(9/1/2012)
 */

/**
 * This file defines an exception
 * this exception is thrown when an instance of an existing user is required
 * but the user does not exist
 * @see User#getExistingUser(java.lang.String, com.qaccess.net.Server) 
 */
public class NonExistentUserException extends Exception{
    public NonExistentUserException(){
        super("The specified user does not exist");
    }
}
