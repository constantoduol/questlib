
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.quest.access.common;

import java.util.logging.Level;

/**
 *
 * @author connie
 */
public class io {
    public static void out(Object str){
        System.out.println(str);
    }
    
   public static void log(Object msg, Level level,Class cls){
        cls = cls == null ? io.class : cls;
    	java.util.logging.Logger.getLogger(cls.getName()).log(level,msg.toString());
        if(msg instanceof Exception){
            ((Exception)msg).printStackTrace();
        }
   }
}

