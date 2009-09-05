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

import java.lang.annotation.*;


// This class uses reflection, so all you have to do
// is add any fields you want, and appropriate getters/setters, and the 
// class handles the rest by itself!
// it reads from a mod-dependent config file, so the config is mod-specific
// and map-independent
// this might change in the future, but it's how it works for now
// maybe we can have mod-config and map-config in the future?
//
// for now, this handles the following types:
// - String
// - boolean primitive
// - int primitive
// - ArrayList<String>
// It's fairly easy to add other primitive types, so just ask if you need
//
// we could also add hashmaps, but for strings only
// since Java uses type erasure, so we can't reflect on the collection's
// generic type, and just have to guess basically.
// we could probably handle this using annotations in the future if we want
//
// we're not going to handle objects, especially sicne hte object that
// one might most be tempted to use would be Unit or UnitDef, which come 
// from the JavaInterface, and would need a bit of thought before using
//
// we should probably split this into two parts: data in one part, and the
// methods in a more generic class
public class Config implements ConfigHelper.IConfig {
   PlayerObjects playerObjects;

   String metalspotmarkerunitname = "armmex";
   String usedmetalspotmarkerunitname = "armfort";
   String spreadsearchnextmovemarkerunitname = "armrad";

   String commanderunitname = "armcom";
   String basicmetalextractorunitname = "armmex";
   String basicenergyextractorunitname = "armsolar";
   String basicconstructionvehicleunitname = "armcv";
   
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

   List<String> reconnaissanceunitnames = Arrays.asList( 
         new String[]{
               "armfav"  
         } );
   List<String> offensiveunitnames = Arrays.asList( 
         new String[]{
               "armsam", "armstump", "armrock", "armjeth", "armkam", "armanac", "armsfig", "armmh", "armah", 
                  "armbull", "armmart", "armmav", "armyork"  
         } );
   List<String> scoutraiderprioritytargets = Arrays.asList(new String[] { 
         "armmex", "cormex", "armrad", "corrad" });

   public Config( PlayerObjects playerObjects ) {
      this.playerObjects = playerObjects;
      ConfigHelper configHelper = new ConfigHelper( playerObjects );
      configHelper.loadConfig( this );
   }

   @Override
   public String getConfigPath() {
      return playerObjects.getCSAI().getAIDirectoryPath() + playerObjects.getAicallback()
      .getMod().getShortName() + ".xml";
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
}
