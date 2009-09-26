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
//
// IMPORTANT: MAKE SURE TO USE CLASS TYPES (INTEGER, FLOAT ETC,)RATHER
// THAN PRIMITIVE TYPES, SO WE CAN SET THESE VALUES TO NULL TO SHOW
// THEIR VALUES DONT EXIST (SEE CONFIGCONTROLLER, CONFIGSTARTSCRIPTREADER ...)
public class Config {
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

   Integer maxTimeToConserveMobileEnemyOnThreatMapGameSeconds = 30;
   
   Boolean GUIActivated = true; // do we show the ui?
   
   String defaultWorkflowName = "default";

   String metalspotmarkerunitname = "armmex";
   String usedmetalspotmarkerunitname = "armfort";
   String spreadsearchnextmovemarkerunitname = "armrad";

   @ListTypeInfo(String.class)
   List<String> commanderunitnames = Arrays.asList( new String[] {
         "armcom", "corcom" } );
   @ListTypeInfo(String.class)
   List<String> basicmetalextractorunitnames = Arrays.asList( new String[] {
         "armmex", "cormex" } );
   @ListTypeInfo(String.class)
   List<String> basicenergyextractorunitnames = Arrays.asList( new String[] {
         "armsolar", "corsolar" } );
   @ListTypeInfo(String.class)
   List<String> basicconstructionvehicleunitnames = Arrays.asList( new String[] {
         "armcv", "corcv" } );
   
   Integer welcomeMessageSecondsInterval = 10;
   
   @ListTypeInfo(String.class)
   List<String> welcomeMessages = Arrays.asList( new String[] {
         "Welcome to HughAI, by Hugh Perkins 2006, 2009",
         "This AI works best with Balanced Annihilation.",
         "You can configure HughAI from the attached gui, and in workflow files in the xxx_workflows directory.",
         "You can say '.hughai help' for text commands, or use the attached GUI panel.",
         "For more information and questions, please don't hesitate to post in the HughAI thread in the forums, or email me at hughperkins@gmail.com."
       }
   );
   
   Integer maxLinesOnMap = 1000; // spring, or maybe java interface, crashes with too many lines...
   
   Float maxvehicleslope = 0.08f;  // maximum slope a vehicle can use. arbitrary cutoff, for simplicity
   Float maxinfantryslope = 0.33f; // maximum slope infantry can use.  arbitrary cutoff, for simplicity

   Integer mapDrawGranularity = 8; // in a jna-free world, we wouldn't need this ;-)
                               // but we need to do something to make up for the
                               // insane jna lag ;-)
                               // this is in map squares
         // Addendum: also ,with a line limit of 1000 lines max, this probably needs to be set
         // to at least 8 or so, otherwise map drawing will run out of lines...
   Integer tickFrameInterval = 30;
   Integer losMapInterpolationDistance = 100;
   Integer losrefreshallintervalframecount = 2000;
   Integer losmapdistancethresholdforunitupdate = 100;

   String typicallevel1tankunitdefname = "armstump";
   
   Boolean debug = false;
   Boolean mapHack = false;

   @ListTypeInfo(String.class)
   List<String> reconnaissanceunitnames = Arrays.asList( 
         new String[]{
               "armfav", "corfav"
         } );
   @ListTypeInfo(String.class)
   List<String> offensiveunitnames = Arrays.asList( 
         new String[]{
               // arm units
               "armsam", "armstump", "armrock", "armjeth", "armkam", "armanac", "armsfig", "armmh", "armah", 
                  "armbull", "armmart", "armmav", "armyork",
               // core units
               "correap", "cormist", "corraid", "cormart", "corsent", "corpyro" 
         } );
   @ListTypeInfo(String.class)
   List<String> scoutraiderprioritytargets = Arrays.asList(new String[] { 
         "armmex", "cormex", "armrad" });

   public Config( PlayerObjects playerObjects ) {
      this.playerObjects = playerObjects;
   }
   
//   @Exclude
//   ConfigFileReadWriter<Config> configFileHelper;
   
   public void init() {
//      configFileHelper = new ConfigFileReadWriter<Config>( playerObjects );
//      configFileHelper.loadConfig( this );      
   }
   
//   public void reload(){
//      configFileHelper.loadConfig( this );            
//   }
//   
//   public void save() {
//      configFileHelper.saveConfig( this );                  
//   }

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

   public Boolean isDebug() {
      return debug;
   }

   public void setDebug( Boolean debug ) {
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

   public Integer getTickFrameInterval() {
      return tickFrameInterval;
   }

   public void setTickFrameInterval( Integer tickFrameInterval ) {
      this.tickFrameInterval = tickFrameInterval;
   }

   public Integer getLosMapInterpolationDistance() {
      return losMapInterpolationDistance;
   }

   public void setLosMapInterpolationDistance( Integer losMapInterpolationDistance ) {
      this.losMapInterpolationDistance = losMapInterpolationDistance;
   }

   public Integer getLosrefreshallintervalframecount() {
      return losrefreshallintervalframecount;
   }

   public void setLosrefreshallintervalframecount(
         Integer losrefreshallintervalframecount ) {
      this.losrefreshallintervalframecount = losrefreshallintervalframecount;
   }

   public Integer getLosmapdistancethresholdforunitupdate() {
      return losmapdistancethresholdforunitupdate;
   }

   public void setLosmapdistancethresholdforunitupdate(
         Integer losmapdistancethresholdforunitupdate ) {
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

   public Integer getMapDrawGranularity() {
      return mapDrawGranularity;
   }

   public void setMapDrawGranularity( Integer mapDrawGranularity ) {
      this.mapDrawGranularity = mapDrawGranularity;
   }

   public Float getMaxvehicleslope() {
      return maxvehicleslope;
   }

   public void setMaxvehicleslope( Float maxvehicleslope ) {
      this.maxvehicleslope = maxvehicleslope;
   }

   public Float getMaxinfantryslope() {
      return maxinfantryslope;
   }

   public void setMaxinfantryslope( Float maxinfantryslope ) {
      this.maxinfantryslope = maxinfantryslope;
   }

   public Integer getMaxLinesOnMap() {
      return maxLinesOnMap;
   }

   public void setMaxLinesOnMap( Integer maxLinesOnMap ) {
      this.maxLinesOnMap = maxLinesOnMap;
   }

   public List<String> getWelcomeMessages() {
      return welcomeMessages;
   }

   public void setWelcomeMessages( List<String> welcomeMessages ) {
      this.welcomeMessages = welcomeMessages;
   }

   public Integer getWelcomeMessageSecondsInterval() {
      return welcomeMessageSecondsInterval;
   }

   public void setWelcomeMessageSecondsInterval( Integer welcomeMessageSecondsInterval ) {
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

   public Boolean isMapHack() {
      return mapHack;
   }

   public void setMapHack( Boolean mapHack ) {
      this.mapHack = mapHack;
   }
   public Integer getMaxTimeToConserveMobileEnemyOnThreatMapGameSeconds() {
      return maxTimeToConserveMobileEnemyOnThreatMapGameSeconds;
   }
   public void setMaxTimeToConserveMobileEnemyOnThreatMapGameSeconds(
         Integer maxTimeToConserveMobileEnemyOnThreatMapGameSeconds ) {
      this.maxTimeToConserveMobileEnemyOnThreatMapGameSeconds = maxTimeToConserveMobileEnemyOnThreatMapGameSeconds;
   }
   public String getConsoleclasspath() {
      return consoleclasspath;
   }
   public void setConsoleclasspath( String consoleclasspath ) {
      this.consoleclasspath = consoleclasspath;
   }
   public Boolean isGUIActivated() {
      return GUIActivated;
   }
   public void setGUIActivated( Boolean gUIActivated ) {
      GUIActivated = gUIActivated;
   }
   public List<String> getCommanderunitnames() {
      return commanderunitnames;
   }
   public void setCommanderunitnames( List<String> commanderunitnames ) {
      this.commanderunitnames = commanderunitnames;
   }
   public List<String> getBasicmetalextractorunitnames() {
      return basicmetalextractorunitnames;
   }
   public void setBasicmetalextractorunitnames(
         List<String> basicmetalextractorunitnames ) {
      this.basicmetalextractorunitnames = basicmetalextractorunitnames;
   }
   public List<String> getBasicenergyextractorunitnames() {
      return basicenergyextractorunitnames;
   }
   public void setBasicenergyextractorunitnames(
         List<String> basicenergyextractorunitnames ) {
      this.basicenergyextractorunitnames = basicenergyextractorunitnames;
   }
   public List<String> getBasicconstructionvehicleunitnames() {
      return basicconstructionvehicleunitnames;
   }
   public void setBasicconstructionvehicleunitnames(
         List<String> basicconstructionvehicleunitnames ) {
      this.basicconstructionvehicleunitnames = basicconstructionvehicleunitnames;
   }
}
