/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.quest.access.useraccess.verification;

/**
 * This exception is thrown when a user attempts to carry out an action and yet he has
 * an unverified action still pending
 * @author Conny
 */
public class PendingVerificationException extends Exception {

    public PendingVerificationException() {
        super("User has unverified actions, verify and retry");
    }
    
}
