// Copyright Hugh Perkins 2009
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

package hughai.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;

import hughai.PlayerObjects;

//should probably start using log4j or similar so wee don't have to
//create an instance passing in PlayerObjects, but can just have static
//methods
public class Exec {
   PlayerObjects playerObjects;

   public Exec( PlayerObjects playerObjects ) {
      this.playerObjects = playerObjects;
   }

   // executes commandstring, and returns a string with output
   // from errorstream and inversely named "input"stream
   // commandstring includes arguments
   public String exec( String commandstring, String dir ) throws Exception {
      StringBuilder output = new StringBuilder();
      output.append( "Executing " + commandstring + "\nin " + dir + "\n" );
      Process process  = null;
      try {
         process = 
            Runtime.getRuntime().exec( commandstring, null,
                  new File( dir ) );
      } catch( Exception e ) {
         playerObjects.getLogFile().WriteLine( Formatting.exceptionToStackTrace( e ) );
         throw new RuntimeException( e );
      }
      output.append("executed" + "\n");
      try {
         process.waitFor();
      } catch( Exception e ) {
         playerObjects.getLogFile().WriteLine( Formatting.exceptionToStackTrace( e ) );
         throw new RuntimeException( e );
      }
      output.append("exit value: " + process.exitValue() + "\n" );
      InputStreamReader errorStreamReader = new InputStreamReader( process.getErrorStream() ); 
      BufferedReader errorReader = new BufferedReader(
            errorStreamReader );
      String line;
      //      if( errorReader.ready() ) {
      try {
         line = errorReader.readLine();
         while( line != null ) {
            output.append( "error stream: " + line + "\n" );
            line = null;
            //            if( errorReader.ready() ) {
            line = errorReader.readLine();
            //            }
         }
         //      }
         output.append("errorstream read" + "\n");
         InputStreamReader outStreamReader = new InputStreamReader( process.getInputStream() ); 
         BufferedReader outReader = new BufferedReader(
               outStreamReader );
         line = outReader.readLine();
         while( line != null ) {
            output.append( "out stream: " + line + "\n" );
            line = errorReader.readLine();
         }
      } catch( Exception e ) {
         playerObjects.getLogFile().WriteLine( Formatting.exceptionToStackTrace( e ) );
         throw new RuntimeException( e );
      }
      output.append("outputstream read" + "\n");   
      if( process.exitValue() != 0 ) {
         throw new Exception( output.toString() );
      }

      return output.toString();
   }   
}
