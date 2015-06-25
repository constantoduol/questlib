
package com.quest.access.useraccess;

/**
 *
 * @author constant oduol
 * @version 1.0(12/1/2012)
 */

/**
 * <p>
 * This file defines an exception
 * a user exists exception is thrown incase someone  tries to recreate
 * a user that already exists
 * </p>
 * This exception is a type of exception that stops the execution of a 
 * program
 */
public class UserExistsException extends Exception {
     public UserExistsException(){
         super("The specified user already exists");
     }
}
