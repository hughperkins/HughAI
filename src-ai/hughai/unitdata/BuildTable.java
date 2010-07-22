// Copyright Submarine, Hugh Perkins 2006, 2009
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

package hughai.unitdata;

import java.util.*;
import java.io.*;

import com.springrts.ai.oo.*;

import hughai.*;
import hughai.utils.*;


// collects all the unitdefs together, so can be accessed by name
// based initially on BuildTable.cpp/BuildTable.h by Submarine, though it's changed
// somewhat since then...
public class BuildTable
{
   //int numOfUnitTypes = 0;
   UnitDef[] availableunittypes;
   private HashMap<String, UnitDef> UnitDefByName = new HashMap<String,UnitDef>();
   private HashMap<Integer, UnitDef> UnitDefById = new HashMap<Integer, UnitDef>(); // yes, we could use array, but this is easier to read

   private int largestUnitDefId = 0;
   
   //PlayerObjects playerObjects;
   CSAI CSAI;
   OOAICallback aicallback;
   LogFile logfile;
   String modname;

   public BuildTable(PlayerObjects playerObjects) // protected constructor to force Singleton instantiation
   {
      //this.playerObjects = playerObjects;
      CSAI = playerObjects.getCSAI();
      aicallback = CSAI.aicallback;
      logfile = playerObjects.getLogFile();

      modname = aicallback.getMod().getHumanName();

      logfile.WriteLine( "calling GetUnitDefList... " );
      List<UnitDef> unittypeslist = aicallback.getUnitDefs();
      //new ArrayList<UnitDef>();
      //int numunittypes = aicallback.GetNumUnitDefs();
      //for (int i = 1; i <= numunittypes; i++)
      //{
      //	unittypeslist.Add( aicallback.GetUnitDefByTypeId( i ) );
      //}
      availableunittypes = unittypeslist.toArray(new UnitDef[0]);
      logfile.WriteLine( "... done" );

      if( !LoadCache( modname ) )
      {
         CSAI.sendTextMessage( "Creating new cachefile for mod " + modname );
         GenerateBuildTable( modname );
         SaveCache( modname );
      }
   }
   
   public UnitDef getUnitDefByName( String unitdefname ) {
      UnitDef unitdef = UnitDefByName.get( unitdefname.toLowerCase() );
      if( unitdef == null ) {
         CSAI.sendTextMessage( "No unit named '" + unitdefname + "'.  Please check the build table in the AI's directory for allowed unit names in this mod." );
//         throw new RuntimeException( "No unit named '" + unitdefname + "'.  Please check the build table in the AI's directory for allowed unit names in this mod.");
         return null;
      }
      return unitdef;
   }
   
   public UnitDef getUnitDefByUnitDefId( int unitDefId ) {
      return UnitDefById.get( unitDefId );
   }

   void GenerateBuildTable( String modname )
   {
      try {
         logfile.WriteLine( "Generating indexes mod " + modname );

         PrintWriter printWriter = new PrintWriter( CSAI.getAIDirectoryPath() + "buildtable_" + CSAI.Team + ".txt" );
         for( UnitDef unitdef : availableunittypes )
         {
            String logline = unitdef.getUnitDefId() + " " + unitdef.getName() + " " + unitdef.getHumanName() + " size: " + unitdef.getXSize() + "," + unitdef.getZSize();
            MoveData movedata = unitdef.getMoveData();
            if( movedata != null )
            {
               logline += " maxSlope: " + movedata.getMaxSlope() + " depth " + movedata.getDepth() + " slopeMod " + movedata.getSlopeMod() +
               " depthMod: " + movedata.getDepthMod() + " movefamily: " + movedata.getMoveFamily();
            }
            printWriter.println(  logline );

            if (!UnitDefByName.containsKey(unitdef.getName().toLowerCase()))
            {
               int unitdefid = unitdef.getUnitDefId();
               UnitDefByName.put( unitdef.getName().toLowerCase(), unitdef );
               UnitDefById.put(unitdefid, unitdef);
               if( unitdefid > largestUnitDefId  ) {
                  largestUnitDefId = unitdefid;
               }
            }
            else
            {
               logfile.WriteLine( "Warning: duplicate name: " + unitdef.getName().toLowerCase() );
               printWriter.println( "Warning: duplicate name: " + unitdef.getName().toLowerCase() );
            }
         }
         printWriter.close();
      } catch( Exception e ) {
         logfile.WriteLine( Formatting.exceptionToStackTrace( e ) );
         throw new RuntimeException( e );
      }

   }

   void SaveCache( String modname )
   {
   }

   boolean LoadCache( String modname )
   {
      return false;
   }

   //int BiggestMexUnit = -1; // cache value to save time later
   public UnitDef GetBiggestMexUnit()
   {
      //if( BiggestMexUnit != -1 )
      //{
      //  return BiggestMexUnit;
      //}

      logfile.WriteLine( "Entering GetBiggestMexUnit()");

      int biggest_mex_id = 0, biggest_area = 0;

      logfile.WriteLine( "Scanning unitdef list...");
      UnitDef biggestmexunit = null;
      for( UnitDef unitdef : availableunittypes )
      {
         if( unitdef.getExtractsResource(aicallback.getResources().get(0)) > 0 )
         {
            int thisarea = unitdef.getXSize() * unitdef.getZSize();
            if( thisarea > biggest_area )
            {
               biggest_mex_id = unitdef.getUnitDefId();
               biggest_area = thisarea;
               biggestmexunit = unitdef;
            }
         }
      }

      logfile.WriteLine( "Leaving GetBiggestMexUnit(), it's unittypeid is " + biggest_mex_id  );
      return biggestmexunit;
   }

   public int getLargestUnitDefId() {
      return largestUnitDefId;
   }
}
