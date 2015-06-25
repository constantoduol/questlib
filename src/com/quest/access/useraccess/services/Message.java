package com.quest.access.useraccess.services;

/**
 * This file defines messages that are used to communicate between the
 * server and client
 * @author conny
 */
public interface Message {
    
    /**
     * this message is sent to indicate that the requested request was successful
     */
    public final String SUCCESS="success";
    /**
     * this message is sent to indicate that the request requested by the client failed
     */
    public final String FAIL="fail";
    
    /**
     * this message is sent to the client to indicate that an exception was thrown while
     * carrying out the requested action
     */
    public final String EXCEPTION="exception";
    /**
     * sent to indicate that an error occurred while undertaking the operation sent by 
     * the client
     */
    public final String ERROR="error";
}
