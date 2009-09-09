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

package hughai.packcoordinators;

import java.util.*;
import java.util.Map;

import com.springrts.ai.*;
import com.springrts.ai.oo.*;

import hughai.GameAdapter;
import hughai.PlayerObjects;
import hughai.basictypes.*;
import hughai.*;
import hughai.mapping.*;
import hughai.packcoordinators.*;
import hughai.unitdata.*;
import hughai.utils.*;



// this carries out a spread search: sends all units around randomly
// can be used by scoutcontroller, or by tankcontroller, for example
public class SpreadSearchPackCoordinator extends PackCoordinator
{
	Float3 targetpos;

	// can pass in pointer to a hashtable in another class if we want
	// ie other class can directly modify our hashtable
	public SpreadSearchPackCoordinator(
			PlayerObjects playerObjects )
	{
		super( playerObjects );

		csai.registerGameListener( new GameListenerHandler() );
	}

	// does NOT imply Activate()
	public void SetTarget( TerrainPos newtarget )
	{
		this.targetpos = newtarget;
		//Activate();
	}

	@Override
	public void Activate()
	{
		if( !activated )
		{
			logfile.WriteLine( "SpreadSearchPackCoordinator initiating spreadsearch" );
			activated = true;
			restartedfrompause = true;
			Recoordinate();
		}
	}

	@Override
	public void Disactivate()
	{
		if( activated )
		{
			activated = false;
			logfile.WriteLine( "SpreadSearchPackCoordinator shutting down" );
		}
	}

	TerrainPos lasttargetpos = null;

	@Override
	void Recoordinate()
	{
		if( !activated )
		{
			return;
		}

		// just send each unit to random destination
		// in unit onidle, we send each unit to a new place
		for( Unit unit : unitsControlled )
		{
			//int deployedid = (int)de.Key;
			//UnitDef unitdef = de.Value as UnitDef;
			ExploreWith( unit );
		}
	}

	void ExploreWith( Unit unit )
	{
	   TerrainPos destination = GetRandomDestination();
		giveOrderWrapper.MoveTo(unit, destination);
	}

	TerrainPos GetRandomDestination()
	{
	   TerrainPos destination = new TerrainPos();
		destination.x = random.nextFloat() * aicallback.getMap().getWidth() * maps.getMovementMaps().SQUARE_SIZE;
		destination.z = random.nextFloat() * aicallback.getMap().getHeight() * maps.getMovementMaps().SQUARE_SIZE;
		destination.y = aicallback.getMap().getElevationAt( destination.x, destination.y );
		return destination;
	}

	class GameListenerHandler extends GameAdapter {
		@Override
		public void UnitIdle( Unit unit )
		{
			if( activated )
			{
				if( unitsControlled.contains( unit ) )
				{
					ExploreWith( unit );
				}
			}
		}

//		int ticks = 0;
		@Override
		public void Tick( int frame )
		{
//			ticks++;
//			if( ticks >= 30 )
//			{
				//Recoordinate();

//				ticks = 0;
//			}
		}
	}
}
