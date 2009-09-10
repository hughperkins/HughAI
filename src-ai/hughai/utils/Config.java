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
import org.w3c.dom.*;

import hughai.PlayerObjects;
import hughai.basictypes.*;
import hughai.*;
import hughai.utils.*;
import hughai.utils.ReflectionHelper.Exclude;
import hughai.utils.ReflectionHelper.ListTypeInfo;

import java.lang.annotation.*;


// This class uses reflection, so all you have to do
// is add any fields you want, and appropriate getters/setters, and the 
// class handles the rest by itself!
// it reads from a mod-dependent config file, so the config is mod-specific
// and map-independent
// this might change in the future, but it's how it works for now
// maybe we can have mod-config and map-config in the future?
//
// For documentation on what types are supported and so on,
// please see ReflectionHelper.java
public class Config implements ConfigHelper.IConfig {
   public interface ConfigListener {
      public void configUpdated();
   }
   
   @Exclude
   List<ConfigListener> listeners = new ArrayList<ConfigListener>();
   public void registerListener( ConfigListener listener ) {
      listeners.add( listener );
   }
   public void configUpdated() { // this assumes anything that will
                                 // modify the config will tell us,
                                 // which is fairly reasnoable,
                                 // since it's just the gui that will do so
      for( ConfigListener listener : listeners ) {
         listener.configUpdated();
      }
   }
   
   @ReflectionHelper.Exclude // dont' try to save/restore playerObjects ;-)
   PlayerObjects playerObjects;
   
   @ReflectionHelper.Exclude
   final String ConfigVersion = "2";
   
   String consoleclasspath="$aidir/SkirmishAI.jar:$aidir/UnderlyingAI.jar"
        + ":$aidir/../../../Interfaces/Java/0.1/AIInterface.jar"
        + ":$aidir/../../../Interfaces/Java/0.1/jlib/jna.jar" 
        + ":$aidir/../../../Interfaces/Java/0.1/jlib/vecmath.jar"; 

   int maxTimeToConserveMobileEnemyOnThreatMapGameSeconds = 30;
   
   boolean GUIActivated = true; // do we show the ui?
   
   String defaultWorkflowName = "default";

   String metalspotmarkerunitname = "armmex";
   String usedmetalspotmarkerunitname = "armfort";
   String spreadsearchnextmovemarkerunitname = "armrad";

   String commanderunitname = "armcom";
   String basicmetalextractorunitname = "armmex";
   String basicenergyextractorunitname = "armsolar";
   String basicconstructionvehicleunitname = "armcv";
   
   int welcomeMessageSecondsInterval = 10;
   
   @ListTypeInfo(String.class)
   List<String> welcomeMessages = Arrays.asList( new String[] {
         "Welcome to HughAI, by Hugh Perkins 2006, 2009",
         "This AI works best with Balanced Annihilation.",
         "You can configure HughAI from the attached gui, and in workflow files in the xxx_workflows directory.",
         "You can say '.hughai help' for text commands, or use the attached GUI panel.",
         "For more information and questions, please don't hesitate to post in the HughAI thread in the forums, or email me at hughperkins@gmail.com."
       }
   );
   
   int maxLinesOnMap = 1000; // spring, or maybe java interface, crashes with too many lines...
   
   float maxvehicleslope = 0.08f;  // maximum slope a vehicle can use. arbitrary cutoff, for simplicity
   float maxinfantryslope = 0.33f; // maximum slope infantry can use.  arbitrary cutoff, for simplicity

   int mapDrawGranularity = 8; // in a jna-free world, we wouldn't need this ;-)
                               // but we need to do something to make up for the
                               // insane jna lag ;-)
                               // this is in map squares
         // Addendum: also ,with a line limit of 1000 lines max, this probably needs to be set
         // to at least 8 or so, otherwise map drawing will run out of lines...
   int tickFrameInterval = 30;
   int losMapInterpolationDistance = 100;
   int losrefreshallintervalframecount = 2000;
   int losmapdistancethresholdforunitupdate = 100;

   String typicallevel1tankunitdefname = "armstump";
   
   boolean debug = false;
   boolean mapHack = false;

   @ListTypeInfo(String.class)
   List<String> reconnaissanceunitnames = Arrays.asList( 
         new String[]{
               "armfav"  
         } );
   @ListTypeInfo(String.class)
   List<String> offensiveunitnames = Arrays.asList( 
         new String[]{
               "armsam", "armstump", "armrock", "armjeth", "armkam", "armanac", "armsfig", "armmh", "armah", 
                  "armbull", "armmart", "armmav", "armyork"  
         } );
   @ListTypeInfo(String.class)
   List<String> scoutraiderprioritytargets = Arrays.asList(new String[] { 
         "armmex", "cormex", "armrad", "corrad" });

   public Config( PlayerObjects playerObjects ) {
      this.playerObjects = playerObjects;
   }
   
   @Exclude
   ConfigHelper<Config> configHelper;
   
   public void init() {
      configHelper = new ConfigHelper<Config>( playerObjects );
      configHelper.loadConfig( this );      
   }
   
   public void reload(){
      configHelper.loadConfig( this );            
   }
   
   public void save() {
      configHelper.saveConfig( this );                  
   }

   @Override
   public String getConfigPath() {
      String configPath = playerObjects.getCSAI().getAIDirectoryPath() + playerObjects.getAicallback()
         .getMod().getShortName() + "_" + ConfigVersion + ".xml";
      playerObjects.getLogFile().WriteLine( "Config path: " + configPath );
      return configPath;
   }

   public String getMetalspotmarkerunitname() {
      return metalspotmarkerunitname;
   }

   public void setMetalspotmarkerunitname( String metalspotmarkerunitname ) {
      this.metalspotmarkerunitname = metalspotmarkerunitname;
   }

   public String getUsedmetalspotmarkerunitname() {
      return usedmetalspotmarkerunitname;
   }

   public void setUsedmetalspotmarkerunitname( String usedmetalspotmarkerunitname ) {
      this.usedmetalspotmarkerunitname = usedmetalspotmarkerunitname;
   }

   public String getCommanderunitname() {
      return commanderunitname;
   }

   public void setCommanderunitname( String commanderunitname ) {
      this.commanderunitname = commanderunitname;
   }

   public String getBasicmetalextractorunitname() {
      return basicmetalextractorunitname;
   }

   public void setBasicmetalextractorunitname( String basicmetalextractorunitname ) {
      this.basicmetalextractorunitname = basicmetalextractorunitname;
   }

   public String getBasicenergyextractorunitname() {
      return basicenergyextractorunitname;
   }

   public void setBasicenergyextractorunitname( String basicenergyextractorunitname ) {
      this.basicenergyextractorunitname = basicenergyextractorunitname;
   }

   public boolean isDebug() {
      return debug;
   }

   public void setDebug( boolean debug ) {
      this.debug = debug;
   }

   public List<String> getReconnaissanceunitnames() {
      return reconnaissanceunitnames;
   }

   public void setReconnaissanceunitnames( List<String> reconnaissanceunitnames ) {
      this.reconnaissanceunitnames = reconnaissanceunitnames;
   }

   public List<String> getOffensiveunitnames() {
      return offensiveunitnames;
   }

   public void setOffensiveunitnames( List<String> offensiveunitnames ) {
      this.offensiveunitnames = offensiveunitnames;
   }

   public String getTypicallevel1tankunitdefname() {
      return typicallevel1tankunitdefname;
   }

   public void setTypicallevel1tankunitdefname( String typicallevel1tankunitdefname ) {
      this.typicallevel1tankunitdefname = typicallevel1tankunitdefname;
   }

   public int getTickFrameInterval() {
      return tickFrameInterval;
   }

   public void setTickFrameInterval( int tickFrameInterval ) {
      this.tickFrameInterval = tickFrameInterval;
   }

   public int getLosMapInterpolationDistance() {
      return losMapInterpolationDistance;
   }

   public void setLosMapInterpolationDistance( int losMapInterpolationDistance ) {
      this.losMapInterpolationDistance = losMapInterpolationDistance;
   }

   public int getLosrefreshallintervalframecount() {
      return losrefreshallintervalframecount;
   }

   public void setLosrefreshallintervalframecount(
         int losrefreshallintervalframecount ) {
      this.losrefreshallintervalframecount = losrefreshallintervalframecount;
   }

   public int getLosmapdistancethresholdforunitupdate() {
      return losmapdistancethresholdforunitupdate;
   }

   public void setLosmapdistancethresholdforunitupdate(
         int losmapdistancethresholdforunitupdate ) {
      this.losmapdistancethresholdforunitupdate = losmapdistancethresholdforunitupdate;
   }

   public List<String> getScoutraiderprioritytargets() {
      return scoutraiderprioritytargets;
   }

   public void setScoutraiderprioritytargets(
         List<String> scoutraiderprioritytargets ) {
      this.scoutraiderprioritytargets = scoutraiderprioritytargets;
   }

   public String getSpreadsearchnextmovemarkerunitname() {
      return spreadsearchnextmovemarkerunitname;
   }

   public void setSpreadsearchnextmovemarkerunitname(
         String spreadsearchnextmovemarkerunitname ) {
      this.spreadsearchnextmovemarkerunitname = spreadsearchnextmovemarkerunitname;
   }

   public int getMapDrawGranularity() {
      return mapDrawGranularity;
   }

   public void setMapDrawGranularity( int mapDrawGranularity ) {
      this.mapDrawGranularity = mapDrawGranularity;
   }

   public float getMaxvehicleslope() {
      return maxvehicleslope;
   }

   public void setMaxvehicleslope( float maxvehicleslope ) {
      this.maxvehicleslope = maxvehicleslope;
   }

   public float getMaxinfantryslope() {
      return maxinfantryslope;
   }

   public void setMaxinfantryslope( float maxinfantryslope ) {
      this.maxinfantryslope = maxinfantryslope;
   }

   public int getMaxLinesOnMap() {
      return maxLinesOnMap;
   }

   public void setMaxLinesOnMap( int maxLinesOnMap ) {
      this.maxLinesOnMap = maxLinesOnMap;
   }

   public String getBasicconstructionvehicleunitname() {
      return basicconstructionvehicleunitname;
   }

   public void setBasicconstructionvehicleunitname(
         String basicconstructionvehicleunitname ) {
      this.basicconstructionvehicleunitname = basicconstructionvehicleunitname;
   }

   public List<String> getWelcomeMessages() {
      return welcomeMessages;
   }

   public void setWelcomeMessages( List<String> welcomeMessages ) {
      this.welcomeMessages = welcomeMessages;
   }

   public int getWelcomeMessageSecondsInterval() {
      return welcomeMessageSecondsInterval;
   }

   public void setWelcomeMessageSecondsInterval( int welcomeMessageSecondsInterval ) {
      this.welcomeMessageSecondsInterval = welcomeMessageSecondsInterval;
   }

   public String getConfigVersion() {
      return ConfigVersion;
   }

   public String getDefaultWorkflowName() {
      return defaultWorkflowName;
   }

   public void setDefaultWorkflowName( String defaultWorkflowName ) {
      this.defaultWorkflowName = defaultWorkflowName;
   }

   public boolean isMapHack() {
      return mapHack;
   }

   public void setMapHack( boolean mapHack ) {
      this.mapHack = mapHack;
   }
   public int getMaxTimeToConserveMobileEnemyOnThreatMapGameSeconds() {
      return maxTimeToConserveMobileEnemyOnThreatMapGameSeconds;
   }
   public void setMaxTimeToConserveMobileEnemyOnThreatMapGameSeconds(
         int maxTimeToConserveMobileEnemyOnThreatMapGameSeconds ) {
      this.maxTimeToConserveMobileEnemyOnThreatMapGameSeconds = maxTimeToConserveMobileEnemyOnThreatMapGameSeconds;
   }
   public String getConsoleclasspath() {
      return consoleclasspath;
   }
   public void setConsoleclasspath( String consoleclasspath ) {
      this.consoleclasspath = consoleclasspath;
   }
   public boolean isGUIActivated() {
      return GUIActivated;
   }
   public void setGUIActivated( boolean gUIActivated ) {
      GUIActivated = gUIActivated;
   }
}
