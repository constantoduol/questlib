package com.quest.access.useraccess.verification;

/**
 * This exception is thrown when a user tries to verify an invalid serial
 * @author conny
 */
public class NonExistentSerialException extends Exception {

    public NonExistentSerialException() {
        super("The specified action serial does not exist");
    }
    
}

