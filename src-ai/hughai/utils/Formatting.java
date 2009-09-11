package hughai.utils;

import java.io.*;

public class Formatting {
   // returns value as a string like 1.31524MB
   public static String longToMeg( long value ) {
      float valueasmeg = value / 1024f / 1024f;
      return "" + valueasmeg + "MB";
   }
   
   public static String exceptionToStackTrace( Exception e ) {
      StringWriter stringWriter = new StringWriter();
      PrintWriter printWriter = new PrintWriter( stringWriter );
      e.printStackTrace( printWriter );
      return stringWriter.toString();
   }
}
