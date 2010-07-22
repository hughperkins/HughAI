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
import java.util.Map;

import com.springrts.ai.*;
import com.springrts.ai.oo.*;

import hughai.*;
import hughai.utils.*;
import hughai.basictypes.*;
import hughai.basictypes.Float3.*;
import hughai.ui.*;

// The role of UnitController is to keep track of what units we have
// Really it's just a wrapper for unitfinished and unitdestroyed events from csai.
// Note to self: do we really need this class???
public class UnitController
{
	public static interface UnitListener {
		public void ExistingUnit(Unit unit);
		public void UnitAdded(Unit unit );
		public void UnitRemoved( Unit unit );		
		public void AllUnitsLoaded();
	}
	public static class UnitAdapter implements UnitListener {
		@Override public void ExistingUnit(Unit unit){}
		@Override public void UnitAdded(Unit unit ){}
		@Override public void UnitRemoved( Unit unit ){}
		@Override public void AllUnitsLoaded(){}
	}

	HashSet<UnitListener> unitListeners = new HashSet<UnitListener>();

	public void registerListener( UnitListener listener ){
		unitListeners.add(listener);
	}

	CSAI csai;
	OOAICallback aicallback;
	LogFile logfile;
	UnitDefHelp unitdefhelp;
	GiveOrderWrapper giveOrderWrapper;
	DrawingUtils drawingUtils;

	//public Hashtable UnitDefByDeployedId = new Hashtable();
	//public HashMap<Integer,UnitDef> UnitDefByDeployedId = new HashMap<Integer,UnitDef>();
	//public HashMap<Integer,Unit> UnitByUnitId = new HashMap<Integer,Unit>();
	public HashMap<String, List<Unit>> UnitsByName = new HashMap<String, List<Unit>>();
	public List<Unit> units = new ArrayList<Unit>();
	HashMap<Unit,UnitDef> unitdefbyunit = new HashMap<Unit, UnitDef>(); // because unit.getDef() is f**king slow.

	// wipe these each frame:
	HashMap<Unit,TerrainPos> posbyunit = new HashMap<Unit,TerrainPos>();
    List<Map<?,?>> mapstowipeeachframe = Arrays.asList( new Map<?,?>[]{
       posbyunit } ); 

	public UnitDef getUnitDef( Unit unit ) {
	   return unitdefbyunit.get( unit );
	}
	
	// retrieves cached pos if exists, or gets it, and caches it.
	public TerrainPos getPos( Unit unit ) {
	   // we assume that mostly the pos will already exist, so check this first
	   if( posbyunit.containsKey( unit ) ) {
	      return posbyunit.get( unit );
	   }
	   // store it in variable, so we don't have to do a fetch from the collection after the put
	   TerrainPos thispos = TerrainPos.fromAIFloat3( unit.getPos() );
	   if( thispos.equals( new TerrainPos() ) ) { // if the pos is zero, just make it null, it makes life simpler...
	      thispos = null;
	   }
       posbyunit.put( unit, thispos );
       return thispos;
	}
	
	public UnitController( PlayerObjects playerObjects )
	{
		csai = playerObjects.getCSAI();
		aicallback = csai.aicallback;
		logfile = playerObjects.getLogFile();
		unitdefhelp = new UnitDefHelp( playerObjects );
		giveOrderWrapper = playerObjects.getGiveOrderWrapper();
		drawingUtils = playerObjects.getDrawingUtils();

		csai.registerGameListener(new GameListenerHandler());

//		csai.RegisterVoiceCommand( "killallfriendly", new VoiceCommandKillAllFriendlyHandler() );
		csai.RegisterVoiceCommand( "countunits", new VoiceCommandCountUnits() );
//        csai.RegisterVoiceCommand( "labelunits", new VoiceLabelUnits() );
        
        playerObjects.getMainUI().registerButton( "Label units", new ButtonLabelUnits() );
        playerObjects.getMainUI().registerButton( "Kill all friendly", new ButtonKillAllFriendly() );

		logfile.WriteLine ("*UnitController initialized*");
	}
	
	class ButtonLabelUnits implements MainUI.ButtonHandler {
	   public void go() {
	      labelUnits();
	   }
	}
	
    class ButtonKillAllFriendly implements MainUI.ButtonHandler {
       public void go() {
          killAllFriendly();
       }
    }
    
	public class VoiceCommandCountUnits implements VoiceCommandHandler {
		@Override public void commandReceived( String cmd, String[]splitcmd, int player )
		{
			csai.SendTextMsg( "friendly unit count: " + units.size() );
		}
	}
	
    public class VoiceLabelUnits implements VoiceCommandHandler {
       @Override public void commandReceived( String cmd, String[]splitcmd, int player )
       {
          labelUnits();
       }
   }
    
    public void labelUnits() {
       for( Unit unit : units ) {
          drawingUtils.drawText( getPos( unit ), "" + unit.getUnitId() );
       }       
    }
   
	class GameListenerHandler extends GameAdapter {
		@Override
		public void Tick( int frame ) {
//			csai.sendTextMessage("unitcontroller.tick");
			//units = aicallback.getTeamUnits();
           for( Map<?,?> map : mapstowipeeachframe ) {
              map.clear();
           }
		}

		@Override
		public void UnitFinished( Unit newunit )
		{
			logfile.WriteLine( "UnitController.NewUnitFinished " + newunit.getDef().getHumanName() + " " + newunit.getUnitId() );
			AddUnit( newunit );
		}

		@Override
		public void UnitDestroyed( Unit destroyedunit, Unit enemy )
		{
			logfile.WriteLine( "UnitController.UnitDestroyed " + destroyedunit.getUnitId() );
			RemoveUnit( destroyedunit );
		}		
	}

	public void LoadExistingUnits()
	{
		List<Unit> friendlyunits = aicallback.getFriendlyUnits();	
		for( Unit friendlyunit : friendlyunits )
		{
			logfile.WriteLine("friendly unit existing: " + friendlyunit.getUnitId() + " " + friendlyunit.getDef().getHumanName() + " " + friendlyunit.getDef().getName() + " " + friendlyunit.getDef().getHumanName() );
			AddUnit( friendlyunit );
			for( UnitListener unitListener : unitListeners ) {
				unitListener.ExistingUnit( friendlyunit );
			}
		}
		for( UnitListener unitListener : unitListeners ) { 
			unitListener.AllUnitsLoaded();
		}
	}

	public void RefreshMyMemory( UnitListener listener )
	{
		units = aicallback.getTeamUnits();
		//for( int deployedid : UnitByUnitId.keySet() )
		for( Unit unit : units )
		{
		//	Unit unit = UnitByUnitId.get(deployedid);
			listener.UnitAdded( unit );
		}
	}

	public void AddUnit( Unit friendlyunit )
	{
		UnitDef unitdef = friendlyunit.getDef();
		if( !units.contains( friendlyunit ) )
		{
			logfile.WriteLine("UnitController.AddUnit: unit id " + friendlyunit.getDef().getName() + " " + unitdef.getHumanName() );
			String name = unitdef.getName().toLowerCase();
			//UnitByUnitId.put( friendlyunit.getUnitId(), friendlyunit );
			if( !UnitsByName.containsKey( name ) )
			{
				UnitsByName.put( name, new ArrayList<Unit>() );
			}
			UnitsByName.get(name).add(friendlyunit);
			units.add(friendlyunit);
			unitdefbyunit.put( friendlyunit, friendlyunit.getDef() );

			for( UnitListener unitListener : unitListeners ) {
				unitListener.UnitAdded( friendlyunit );
			}
			logfile.WriteLine("UnitController.AddUnit finished");
		}
		else
		{
			logfile.WriteLine( "UnitController.AddUnit: unit id " + friendlyunit.getUnitId() + " " + unitdef.getHumanName() + " already exists" );
		}
	}

	void RemoveUnit( Unit friendlyunit )
	{
		if( units.contains( friendlyunit ) )
		{
			UnitDef unitdef = friendlyunit.getDef();
			String name = unitdef.getName().toLowerCase();
			//UnitByUnitId.remove(friendlyunit);
			UnitsByName.get(name).remove(friendlyunit);
			units.remove(friendlyunit);
			unitdefbyunit.remove(  friendlyunit );
		}
		for( UnitListener unitListener : unitListeners ) {
			unitListener.UnitRemoved( friendlyunit );
		}
	}
	
	public void killAllFriendly(){
       for( Unit unit : units )
       {
           if( !unit.getDef().isCommander() )
           {
               giveOrderWrapper.SelfDestruct(unit);
           }
       }	   
	}
	
	// kills all except commander, allows testing expansion from nothing, without having to relaod spring.exe
	public class VoiceCommandKillAllFriendlyHandler implements VoiceCommandHandler {
		@Override
		public void commandReceived( String cmd, String[]splitcmd, int player )
		{
		   killAllFriendly();
		}
	}
}
