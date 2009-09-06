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

public class ConfigHelper<T extends ConfigHelper.IConfig> {
   boolean overwritefileconfig = true; // overrides other stuff, and turns on debug
   
   public interface IConfig {
      public String getConfigPath();
      public void setDebug( boolean debug );
   }
   
   PlayerObjects playerObjects;
   
   ReflectionHelper<T> reflectionHelper;

   public ConfigHelper( PlayerObjects playerObjects ) {
      this.playerObjects = playerObjects;
      reflectionHelper = new ReflectionHelper<T>( playerObjects );
     // loadConfig();
   }

   public void loadConfig( T config ) {
      String configpath = config.getConfigPath();
      reflectionHelper.loadObjectFromFile( configpath, config );
   }

   void validate() {
   }

   public void saveConfig( T config ) {
      reflectionHelper.saveObjectToFile( config.getConfigPath(), config );
   }
}
