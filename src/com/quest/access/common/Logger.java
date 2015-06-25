
package com.quest.access.common;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.logging.*;

/**
 *
 * @author constant oduol
 * @version 1.0(5/5/12)
 */

/**
 * This file implements a logging facility
 * and uses the built in capabilities of the java.util.logging.*
 * package to implement logging to xml files and text files, to log to
 * a file create an instance of the Logger class, if the filename specified in
 * the logger instance refers to a file that already exists it is not recreated, if the
 * filename refers to a non existent file a new empty file is created.
 * 
 */
public class Logger {
    /*
     * a file handler to write to files 
     */
    private FileHandler fhandler;
    /*
     * the file name we are writing to
     */
    private String fileName;
    
    /**
     * creates a logger object capable of writing to text files
     * @param fileName the name of the file we want to log to
     *        the file name can be a directory path eg. c:/temp/log.txt
     *        or just log.txt, if the file does not exist it will be created
     *        specifying %t/fileName will cause the log file to be written in the
     *        system temporary directory, on windows this is C:/Temp/fileName
     *        Therefore a file name such as %t/log.txt creates the file in the system
     *        temporary directory. Specifying %h/log.txt creates the file in a users home
     *        directory
     */
    public Logger(String fileName){
        try{
        this.fhandler=new FileHandler(fileName, true);
        this.fhandler.setLevel(Level.ALL);
        this.fhandler.setFormatter(new TextFormatter());
        this.fileName=fileName;
        }
        catch(Exception e){
        
        }
    }
    
    
    /**
     * this method logs events to a text file in plain text format
     * the text is written in this format
     * [CURRENT TIME][TITLE]: DESCRIPTION
     * @param descr this is the description of the event to be logged
     * @param title this is can be considered as the title of the description being logged 
     */
    public void logText( String title,String descr){
        if(title==null){
           title="EVENT";   
           }
         StringBuilder event=new StringBuilder();
          GregorianCalendar gc=new GregorianCalendar();
          event.append("[").append(gc.getTime()).append("]");
          event.append("[").append(title).append("]");
          String toWrite = event.append(":").append(descr).append(" \n").append("\n").toString();
         LogRecord record=new LogRecord(Level.ALL, toWrite);
         this.fhandler.publish(record);
        
       
    }
    
    /**
     * this method writes logs in form of xml
     * @param descr this is the description of the event to be logged
     * the description is wrapped in <message>description</message>
     * tags in the xml file
     */
    public void logXml(String descr){
        this.fhandler.setFormatter(new XMLFormatter());
        LogRecord record=new LogRecord(Level.ALL,descr);
        this.fhandler.publish(record);
        
    }
    
    /**
     * this method writes an xml file with custom tags
     * @param values this is an array of strings containing the values to be written
     *        in between the tags in this format <tag>value</tag>. If the length of the array containing
     *        values is not divisible with the length of the array containing tags the method returns without
     *        doing anything
     * @param tags this is an array of strings to be written in the xml file in this 
     *        format <tag>value</tag>
     */
    public void logXml(String[] values, String[] tags){
        if(values.length%tags.length!=0){
            return;
             }
        this.fhandler.setFormatter(new TextFormatter());
         StringBuilder xml=new StringBuilder();
          xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
          xml.append("<log>\n");
             int tracker=0;
             for(int x=0; x<values.length/tags.length; x++){
                xml.append("<record>\n");
              for(int y=0; y<tags.length; y++){
               xml.append("\t<").append(tags[y]).append(">");
               xml.append(values[tracker]);
               xml.append("</").append(tags[y]).append(">\n");
               tracker++;
              }
              xml.append("</record>\n");
            }
          xml.append("</log>\n");
          LogRecord record=new LogRecord(Level.ALL,xml.toString());
        this.fhandler.publish(record);
    }
    
    
    public static void toConsole(Object obj, java.lang.Class cls) {
        Date date = new Date();
        String className = "";
        if (cls != null) {
            className = cls.getSimpleName();
        }
        io.out("[" + date.toString() + " :: Quest][" + className + "]: " + obj);
        if (obj instanceof Exception) {
            Exception ex = (Exception) obj;
            ex.printStackTrace();
        }
    }

}

/**
 * this class defines a formatter used in the logger class
 * a text formatter provides a formatter for writing text files in 
 * a simple readable text format.
 * @author constant oduol
 * @version 1.0(5/5/12)
 */
class TextFormatter extends Formatter {

    @Override
    public String format(LogRecord record) {
       return record.getMessage();
    }
    
}
