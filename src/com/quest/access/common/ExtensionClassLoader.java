package com.quest.access.common;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * this class loads classes that are extensions of a specific server
 * @author Conny
 */
public class ExtensionClassLoader extends ClassLoader implements java.io.Serializable {
     private String path;
     public ExtensionClassLoader(String path){
       this.path=path;
    }
    
    @Override
  public Class findClass(String name) {
    byte[] classData = null;
    try {
      String classPath=completeClassName(name);
       classPath=path+"\\"+classPath;
      FileInputStream f = new FileInputStream(classPath);
      int num = f.available();
      classData = new byte[num];
      f.read(classData);
    } catch (IOException e) {
      System.out.println(e);
    }
     Class x = defineClass(name, classData, 0, classData.length);
    return x;
  }
    
  private static String completeClassName(String name){
     StringTokenizer tk=new StringTokenizer(name,".");
     StringBuilder sb=new StringBuilder();
     ArrayList list=new ArrayList();
      while(tk.hasMoreTokens()){
         list.add(tk.nextToken()); 
       }   
      for(int x=0; x<list.size()-1; x++){
         sb.append(list.get(x)).append("\\");
      }
      sb.append(list.get(list.size()-1)).append(".class");
      return sb.toString();
  }
  
  
  

}