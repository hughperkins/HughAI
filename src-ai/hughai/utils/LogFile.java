// Copyright Hugh Perkins 2006, 2009, Robin Vobruba 2008
// hughperkins@gmail.com http://manageddreams.com
//
// This program is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by the
// Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
// or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
//  more details.
//
// You should have received a copy of the GNU General Public License along
// with this program in the file licence.txt; if not, write to the
// Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-
// 1307 USA
// You can find the licence also on the web at:
// http://www.opensource.org/licenses/gpl-license.php
//
// ======================================================================================
//

// file logging functions
// ====================
//
// just call WriteLog( "my message" ) to write to the log
// log appears in same directory as dll (same directory as tasclient.exe), named "csharpai_teamX.log"
// where X is team number, in case we are running multiple AIs (one per team)
//
// Migrated to be based heavily on Robin Vobruba's code that uses the 
// Java Logger class, following migration to Java, by Hugh Perkins
// This makes it a bit more standard, and may allow our static classes to
// actually be static, and still log things.

package hughai.utils;

import java.io.*;
import java.text.*;
import java.util.Date;
import java.util.logging.*;

import javax.management.RuntimeErrorException;

import hughai.*;

public class LogFile
{
   public boolean AutoFlushOn = true;

   Logger logger = Logger.getLogger( "general" ); // note: name may be migrated
   // to something like this.getClass().getName() in the future

   //	PrintWriter printWriter;

   PlayerObjects playerObjects;
   //TimeHelper timeHelper;

   public LogFile( PlayerObjects playerObjects ) {
      this.playerObjects = playerObjects;
   }

   public LogFile Init( String logfilepath )
   {
      try {
         System.out.println("logfile init: [" + logfilepath + "]");

         //         printWriter = new PrintWriter( logfilepath );

         setupLogger( logfilepath );

         //CSAI.GetInstance().RegisterVoiceCommand( "flushlog", new CSAI.VoiceCommandHandler( this.VCFlushLog ) );
      } catch( Exception e ) {
         // well, we're kind of screwed if we can't use the logger,
         // so just print to stdout?
         e.printStackTrace();
         throw new RuntimeException( e ); // and throw up the stack ...
      }
      return this;
   }

   void setupLogger( String logfilepath ) throws Exception {
      FileHandler fileHandler = new FileHandler( logfilepath );
      logger.addHandler( fileHandler );

      StdInfoHandler consoleHandler = new StdInfoHandler();
      logger.addHandler( consoleHandler );

      for( Handler handler : logger.getHandlers() ) {
         handler.setFormatter( new AILogFormatter() );
      }
      
      logger.setUseParentHandlers( false );
   }

   public void Flush()
   {
//      printWriter.flush();
      for( Handler handler : logger.getHandlers() ) {
         handler.flush();
      }
   }

   // arguably we shouldnt auto-flush. because it slows writes, up to you
   public void WriteLine( Object message )
   {
      //sw.WriteLine(DateTime.Now.toString("hh:mm:ss.ff") + ": " + message);
//      printWriter.println(playerObjects.getTimeHelper().GetCurrentGameTimeString() + ": " + message);
//      System.out.println(playerObjects.getTimeHelper().GetCurrentGameTimeString() + ": " + message);
      
      logger.info( "" + message );
      
      if( AutoFlushOn )
      {
         Flush();
//         printWriter.flush();
      }
      //sw.Flush();
   }
   
   public void writeLine( Object message ) {
      WriteLine( message );
   }
   
   public void writeStackTrace( Exception e ) {
      String stacktrace = Formatting.exceptionToStackTrace( e );
      WriteLine( stacktrace );
   }

   public void Shutdown()
   {
//      printWriter.flush();
//      printWriter.close();
      
      Flush();
      
      LogManager.getLogManager().reset();  // hopefully prevents memory leaking?
   }

   // heavily based on MyCustomLogFormatter, by Robin Vobruba
   private class AILogFormatter extends java.util.logging.Formatter {

//      private DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");

      public String format(LogRecord record) {
         StringBuffer sb = new StringBuffer();

//         Date date = new Date(record.getMillis());
//         sb.append(dateFormat.format(date));
         sb.append( playerObjects.getTimeHelper().GetCurrentGameTimeString() );
         sb.append(" ");

         sb.append(record.getLevel().getName());
         sb.append(": ");

         sb.append(formatMessage(record));
         sb.append("\n");

         return sb.toString();
      }
   }

   // created by Hugh Perkins 2009, so logging just goes to stdout, rather
   // than to stderr, and black intead of bright red...
   static class StdInfoHandler extends ConsoleHandler {
      public StdInfoHandler() {
         super();
         setOutputStream( System.out );
      }
   }
}
