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

package hughai.utils;

import java.io.*;
import java.io.ObjectInputStream.GetField;
import java.util.*;
import java.lang.reflect.*;
import java.lang.annotation.*;
import org.w3c.dom.*;

import hughai.*;
import hughai.basictypes.*;
import hughai.utils.*;

// if you create a file called 'debug.flg' in the AI's directory
// then the config will not be loaded, but simply use the default
// values in the Config class
// also, debug will be forced to true.
public class ConfigFileReadWriter<T> implements ConfigSourceReadWriter<T> {
//   boolean overwritefileconfig = false; // overrides other stuff, and turns on debug
//   
//   public interface IConfig {
//      public String getConfigPath();
//      public void setDebug( boolean debug );
//   }
   
   @Override
   public boolean canRead(){
      return true;
   }
   
   @Override
   public boolean canWrite(){
      return true;
   }
   
   PlayerObjects playerObjects;
   
   ReflectionHelper reflectionHelper;

   public ConfigFileReadWriter( PlayerObjects playerObjects ) {
      this.playerObjects = playerObjects;
      reflectionHelper = new ReflectionHelper( playerObjects );
     // loadConfig();
   }

   void debug( Object message ) {
      playerObjects.getLogFile().writeLine( "ConfigFileReadWrite: " + message );
   }

   @Override
   public void loadConfig( T config ) {
      String configpath = getConfigPath();
//      boolean overwritefileconfig = false;
//      if( new File( playerObjects.getCSAI().getAIDirectoryPath() + "debug.flg" ).exists() ) {
//         overwritefileconfig = true;
//      }
//      if( overwritefileconfig ) {
//         config.setDebug( true );
//      }
//      if( !overwritefileconfig && new File( configpath ).exists() ) {
      if( new File( configpath ).exists() ) {
         reflectionHelper.loadObjectFromFile( configpath, config );
         validate();
      }
      reflectionHelper.saveObjectToFile( configpath, config );
   }

   void validate() {
   }

   @Override
   public void saveConfig( T config ) {
      reflectionHelper.saveObjectToFile( getConfigPath(), config );
   }
   
   String getConfigPath() {
      debug("getconfigpath()");
      String configPath = playerObjects.getCSAI().getAIDirectoryPath() + playerObjects.getAicallback()
         .getMod().getShortName() + ".xml";
      debug( "Config path: " + configPath );
      return configPath;
   }
}
