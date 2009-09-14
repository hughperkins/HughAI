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
import java.io.FileReader;

import hughai.PlayerObjects;

// should probably start using log4j or similar so wee don't have to
// create an instance passing in PlayerObjects, but can just have static
// methods
public class FileHelper {
   PlayerObjects playerObjects;
   
   public FileHelper( PlayerObjects playerObjects ) {
      this.playerObjects = playerObjects;
   }
   
   // returns the contents of file filename
   // or null if not found
   public String readFile( String filename ) {
      try {
         FileReader fileReader = new FileReader( filename );
         BufferedReader bufferedReader = new BufferedReader( fileReader );
         StringBuilder stringBuilder = new StringBuilder();
         String line = bufferedReader.readLine();
         while( line != null ) {
            stringBuilder.append( line );
            stringBuilder.append( "\n" );
            line = bufferedReader.readLine();
         }
         bufferedReader.close();
         fileReader.close();
         return stringBuilder.toString();
      } catch( Exception e ) {
         playerObjects.getLogFile().WriteLine( Formatting.exceptionToStackTrace( e ) );
         return null;
      }
   }
}
