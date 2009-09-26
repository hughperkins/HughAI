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

// handles between xml file config values, startscript option values, ...
//
// right, what's this going to do?
// the Config class will just hand-off any load/save stuff to this class
// or, should the Config class really be doing that?  but changing that
// would mean a _lot_ of work/refactoring
//
// what about our gui that lets us change the underlying xml config?
// how does it get to the underlying xml config?
// how does it know what is being overridden?
// maybe just provide a function like 'boolean isWritable(String fieldname)?'
public class ConfigController {
   PlayerObjects playerObjects;
   
   ReflectionHelper reflectionHelper;
   
   public ConfigController( PlayerObjects playerObjects ) {
      this.playerObjects = playerObjects;
      
      reflectionHelper = new ReflectionHelper( playerObjects );
      
      init();
   }
   
   void debug( Object message ) {
      playerObjects.getLogFile().writeLine( "ConfigController: " + message );
   }

   void init() {
      debug("init()");
      debug("restoring from xml...");
      restoreFromSource( ConfigSource.XmlFile );
      debug("restoring from startscript...");
      restoreFromSource( ConfigSource.StartScript );
      debug("restoring from working copy...");
      restoreFromSource( ConfigSource.WorkingCopy );
   }
   
   // copies config from sourceConfig to targetConfig
   // throws exception if canWriteBackToSource(targetconfig) is false
   public void copyConfig( ConfigSource sourceConfig, ConfigSource targetConfig ) {
      if( !canWriteBackToSource( targetConfig ) ) {
         throw new RuntimeException("Can't copy to config " + targetConfig );
      }
      
   }
   
   // this will only work for ConfigSource.XmlFile really
   // doesn't make sense for the others
   // only writes values that are non-null
   // values that are null are simply ignored, for better or worse
   // this lets us select which values will be written to this particular source
   public void writeConfigBackToSource( ConfigSource configSource ) {
      if( !canWriteBackToSource( configSource ) ) {
         throw new RuntimeException("Can't write to config source " + configSource );
      }
      switch( configSource ) {
         case XmlFile:
                                    
         default:
            throw new RuntimeException("Unhandled configsource " + configSource );
      }
   }
   
   // this can be used for all, I suppose...
   // except 'WorkingCopy'
   // reloads the config into memory from that source
   public void restoreFromSource( ConfigSource configSource ) {
      if( !canRestoreFromSource( configSource ) ) {
         debug("Can't restore from config source for config source " + configSource );
         throw new RuntimeException("Can't restore from config source for config source " + configSource );
      }
      if( !configBySource.containsKey( configSource ) ) {
         debug("creating new config for " + configSource );
         configBySource.put( configSource, new Config( playerObjects ) );
         // the startscript reader is going to have to wipe everything to null
         // first, but that is the startscript reader's issue
      }
      Config thisconfig = configBySource.get( configSource );
      debug("created blank config object");
      switch( configSource ) {
         case XmlFile:
            debug("creating configfilereadwriter...");
            ConfigFileReadWriter<Config> configFileReadWriter = new ConfigFileReadWriter<Config>( playerObjects );
            debug("calling loadconfig...");
            configFileReadWriter.loadConfig( thisconfig );
            break;
            
         case StartScript:
            ConfigStartScriptReader<Config> startScriptReader = new ConfigStartScriptReader<Config>( playerObjects );
            startScriptReader.loadConfig( thisconfig );    
            break;
                        
         case WorkingCopy:
            integrateSourcesToWorkingCopy( thisconfig );
            break;
            
         default:
            throw new RuntimeException("Unhandled configsource " + configSource );
      }
   }
   
   void integrateSourcesToWorkingCopy( Config workingcopy ) {
      debug("copying xmlfile config to workingcopy config...");
      reflectionHelper.deepCopy( getConfig( ConfigSource.XmlFile ), workingcopy );
      debug("copying startscript config to workingcopy config...");
      reflectionHelper.deepCopyNonNullOnly( getConfig( ConfigSource.StartScript ), workingcopy );
   }
   
   public enum ConfigSource { XmlFile, StartScript, WorkingCopy };
   
   // so, each config is a copy in memory of the original config, which
   // is in the startscript, or the xmlfile, or something
   HashMap<ConfigSource,Config> configBySource = new HashMap<ConfigSource, Config>();
   
   // can we write it back to its source?
   public boolean canWriteBackToSource( ConfigSource configSource ) {
      switch( configSource ) {
         case XmlFile:
            return true;
            
         default:
            return false;
      }
   }
   
   // can we restore it from somewhere?
   public boolean canRestoreFromSource( ConfigSource configSource ) {
      switch( configSource ) {
         case XmlFile:  // can restore from file
         case WorkingCopy: // can reread from everywhere
         case StartScript:  // startscript is never modified, but anyway, we can reread it
            return true;
            
         default:
            return false;
      }
   }
   
   public Config getConfig() {
      return getConfig( ConfigSource.WorkingCopy );
   }
   
   public Config getConfig( ConfigSource configSource ) {
      return configBySource.get( configSource );
   }
      
   // can we modify this value, or is it being overridden by the startscript?
   // for that matter, we have three levels of config (at least...):
   // - xml file contents
   // - start script contents
   // - memory contents
   // we could always make this somewhat explicit in the gui? like with 
   // a drop-down selector?
   // sounds somewhat unintuitive though?
   // or we could have a button 'writeToXmlFile', which would copy the stuff
   // from the startscript into the xml file
   // if we're overriding from the startscript do we care?  maybe...
   // ok, so if we have a drop-down, we can show the values in memory,
   // startscript, and in memory, then we can have a button, like "copy to memory"
   // "copy to xml configfile", "copy to startscript" (can't really have that last one
   // since it is read-only...)
//   public boolean isWritable( String fieldname ) {
//      
//   }
}
