
package com.quest.access.useraccess.verification;

/**
 * An incomplete verification exception is thrown when an action performed by a user has
 * not been completely verified by  a sufficient number of users
 * @author Conny
 */
public class IncompleteVerificationException extends Exception {
    public IncompleteVerificationException() {
        super("This action has not been completely verified");
    }
    
}
