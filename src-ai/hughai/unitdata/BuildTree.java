// Copyright Hugh Perkins 2006, 2009
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

import com.springrts.ai.oo.*;

import hughai.*;
import hughai.utils.*;


// gives what can be constructed, given the name of the thing which will do the 
// constructing
public class BuildTree
{
//	HashMap<String, ArrayList<UnitDef>> buildableunitdefsbyname = new HashMap<String, ArrayList<UnitDef>>();

    private HashSet<UnitDef> thisTeamsUnitDefs = new HashSet<UnitDef>(); // bit of a hack for now, but ok for 99% of purposes for now...
    private HashMap<String, UnitDef> thisTeamsUnitDefsByName = new HashMap<String,UnitDef>();
    
    boolean directedGraphUnitDefIdToUnitDefId [][]; // basically, if
       // [buildunitdefid][childunitdefid] is true, then the unitdef with unitdefid
       // buildunitdefid can build units with unit def id childunitdefid
       // this is built when the class is instantiated, and contains all unitdefs
       // in the mod, not just friendly ones
       // such an array must be int based, hash based would be insanely large ;-)
       // so we'll use the unitdefid
    boolean seenthisunitdefidbefore[]; // if true, we've added this unitdef id into our list of
       // available unitdefs before
	  
	PlayerObjects playerObjects;
	CSAI csai;
	OOAICallback aicallback;
	LogFile logfile;
	BuildTable buildTable;

	public BuildTree(PlayerObjects playerObjects)
	{
		this.playerObjects = playerObjects;
		
		csai = playerObjects.getCSAI();
		aicallback = csai.aicallback;
		buildTable = playerObjects.getBuildTable();
		logfile = playerObjects.getLogFile();
		
		init();
		this.playerObjects.getUnitController().registerListener( new UnitListener() );
	}
	
	// set up directed graph
	void init() {
	   playerObjects.getLogFile().WriteLine( "building buildtree directed graph..." );
	   UnitDef[] availableunittypes = playerObjects.getBuildTable().availableunittypes;
	   directedGraphUnitDefIdToUnitDefId = new boolean[buildTable.getLargestUnitDefId() + 1]
	                                                   [buildTable.getLargestUnitDefId() + 1];
	   seenthisunitdefidbefore = new boolean[buildTable.getLargestUnitDefId() + 1];
	   for( UnitDef unitDef : availableunittypes ) {
	      int thisunitdefid = unitDef.getUnitDefId();
	      for( UnitDef childUnitDef : unitDef.getBuildOptions() ) {
	         int childUnitDefId = childUnitDef.getUnitDefId();
	         directedGraphUnitDefIdToUnitDefId[thisunitdefid][childUnitDefId] = true;
	      }
	   }
       playerObjects.getLogFile().WriteLine( " ... done" );
	}
	
	void debug( Object message ) {
	   playerObjects.getLogFile().WriteLine( "BuildTree: " + message );
	}
	
	class UnitListener extends UnitController.UnitAdapter {
	   @Override
	   public void UnitAdded( Unit unit ) {
	      debug("unitadded" );
	      UnitDef unitDef = playerObjects.getUnitController().getUnitDef( unit );
	      if( unitDef == null ) {
	          debug("... unitdef null" );
	         return;
	      }
          debug(" ... " + unit.getDef().getUnitDefId() + " " + unit.getDef().getHumanName() );
	      if( thisTeamsUnitDefs.contains( unitDef ) ) {
	          debug(" ...  seen before ... " );
	         return;
	      }
          debug(" ... not seen before ... " );
	      
	      // if we got this far, we got to a unitdef that is not in our tree
	      // do some sort of dfs or bfs walk across the directed build tree graph
	      // and add all unitdefs found into thisTeamsUnitDefs
	      int thisunitdefid = unitDef.getUnitDefId();
	      // now we'll do a dfs/bfs along the directed graph
	      Stack<Integer> unitdefidstoprocess = new Stack<Integer>();
	      unitdefidstoprocess.push(  thisunitdefid );
	      while( !unitdefidstoprocess.isEmpty() ) {
	         int thisWalkUnitDefId = unitdefidstoprocess.pop();
	         if( seenthisunitdefidbefore[thisWalkUnitDefId] ) {
	            continue;
	         }

             seenthisunitdefidbefore[thisWalkUnitDefId] = true;

	         // add self to our lists of buildables
	         UnitDef thisUnitDef = buildTable.getUnitDefByUnitDefId( thisWalkUnitDefId );
	         thisTeamsUnitDefs.add( thisUnitDef );
	         thisTeamsUnitDefsByName.put( thisUnitDef.getName(), thisUnitDef );
//             playerObjects.getLogFile().WriteLine( "BuildTree.  walking graph, adding " + thisUnitDef.getHumanName() );
	         
	         // add children to stack
	         for( int childunitdefid = 0; childunitdefid <= buildTable.getLargestUnitDefId(); childunitdefid++ ) {
	            if( directedGraphUnitDefIdToUnitDefId[thisWalkUnitDefId][childunitdefid]) {
	               // if we got here, then thiswalkunitdefid can build childunitdefid
	               // so add to stack
	               unitdefidstoprocess.push( childunitdefid );
	            }
	         }
	      }
	   }
	}
	
	public boolean isOurTeam( UnitDef unitDef ) {
	   return thisTeamsUnitDefs.contains( unitDef );
	}

    public boolean isOurTeam( String unitDefName ) {
       return thisTeamsUnitDefsByName.containsKey( unitDefName );
    }
    
    //returns first string in unitdefnames that is on our side, ie in
    // our build tree
    // null if none found
    public String listToOurTeamsUnitName( List<String> unitdefnames ) {
       for( String name : unitdefnames ) {
          if( thisTeamsUnitDefsByName.containsKey( name ) ) {
             return name;
          }
       }
       //throw new RuntimeException("Failed to find unitdef for any of the following unitdefnames: " +
         // unitdefnames );
       return null;
    }

	public boolean CanBuild(String buildername, String targetname)
	{
	   UnitDef builderUnitDef = buildTable.getUnitDefByName( buildername );
	   UnitDef targetunitdef = buildTable.getUnitDefByName( targetname );
	   if( builderUnitDef == null ) {
	      return false;
	   }
	   if( targetunitdef == null ) {
	      playerObjects.getLogFile().WriteLine( "WARNING: no such unit name as " + targetname + " please check your configuration." );
	      return false;
	   }
	   int builderunitdefid = builderUnitDef.getUnitDefId();
	   int targetunitdefid = targetunitdef.getUnitDefId();
	   return directedGraphUnitDefIdToUnitDefId[builderunitdefid][targetunitdefid];
//		if (!buildablenamesbyname.containsKey(buildername))
//		{
//			ArrayList<String> thisbuilderoptions = new ArrayList<String>();
//			buildablenamesbyname.put(buildername,thisbuilderoptions );
//			UnitDef unitdef = buildTable.getUnitDefByName(buildername);
//			for( UnitDef buildingOption : unitdef.getBuildOptions() ) {				
//				thisbuilderoptions.add( buildingOption.getName() );
//			}
//		}
//		return buildablenamesbyname.get(buildername).contains(targetname);
	}
}
