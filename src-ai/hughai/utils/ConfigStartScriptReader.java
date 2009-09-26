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
import hughai.utils.ReflectionHelper.Exclude;

public class ConfigStartScriptReader<T> implements ConfigSourceReadWriter<T> {
   PlayerObjects playerObjects;
   
   ReflectionHelper reflectionHelper;
   
   public ConfigStartScriptReader( PlayerObjects playerObjects ) {
      this.playerObjects = playerObjects;
      
      this.reflectionHelper = new ReflectionHelper( playerObjects );
   }
   
   @Override
   public boolean canRead(){
      return true;
   }
   @Override
   public boolean canWrite() {
      return false;
   }

   void debug( Object message ) {
      playerObjects.getLogFile().WriteLine( "" + this.getClass().getSimpleName() + ": " + message );
   }
   
   @Override
   public void loadConfig( T config ) {
      OptionsFromStartScript optionsFromStartScript = playerObjects.getOptionsFromStartScript();
      
      // just handle primitives for now
      // maybe that is all startscript can handle anyway?
      for( Field field : config.getClass().getDeclaredFields() ) {
         Exclude exclude = field.getAnnotation( Exclude.class );
         if( exclude == null ) { // if it's not null, we've excluded it
            // ok, so if the field is in the startscript, we retrieve the value
            // otherwise we overwrite this config object value with null
            // to show that it wasn't in the start script
            String fieldname = field.getName();
            String optionstringfromstartscript = optionsFromStartScript.getOption( fieldname );
            Object fieldvalue = null;
            if( optionstringfromstartscript != null ) {
               fieldvalue = reflectionHelper.stringTofieldValue( field.getType(), optionstringfromstartscript );
               debug( "found value in startscript: " + fieldname + " = " + fieldvalue );
            }
            try {
               field.set( config, fieldvalue );
            } catch( Exception e ) {
               playerObjects.getLogFile().writeStackTrace( e );
               throw new RuntimeException( e );
            }
         }
      }
   }
   @Override
   public void saveConfig( T config ) {
      throw new RuntimeException("can't write to start script config");  
   }
}
