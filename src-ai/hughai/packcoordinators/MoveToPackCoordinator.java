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



// this moves the units to a particular point
// the point is assumed to be friendly; no particular attack pattern is assumed
public class MoveToPackCoordinator extends PackCoordinator
{
	// can pass in pointer to a hashtable in another class if we want
	// ie other class can directly modify our hashtable
	public MoveToPackCoordinator(
			PlayerObjects playerObjects )
	{
		super(playerObjects );

		csai.registerGameListener( new GameListenerHandler() );
	}

	TerrainPos targetpos;

	// does NOT imply Activate()
	public void SetTarget( TerrainPos newtarget )
	{
		this.targetpos = newtarget;
		//Activate();
	}

	TerrainPos lasttargetpos = null;

	@Override
	void Recoordinate()
	{
		if( !activated )
		{
			return;
		}

		if( restartedfrompause 
				|| targetpos.GetSquaredDistance( lasttargetpos ) 
				> ( 20 * 20 ) )
		{
			for( Unit unit : unitsControlled )
			{
				//int deployedid = (int)de.Key;
				//UnitDef unitdef = de.Value as UnitDef;
				Move( unit );
			}
			lasttargetpos = targetpos;
		}
	}

	void Move( Unit unit )
	{
		giveOrderWrapper.MoveTo(unit, targetpos );
		//aicallback.GiveOrder( unitid, new Command( Command.CMD_MOVE, targetpos.ToDoubleArray() ) );
	}

	class GameListenerHandler extends GameAdapter {
		@Override
		public void UnitIdle( Unit unit )
		{
			if( activated )
			{
				if( unitsControlled.contains( unit ) )
				{
					Move( unit );
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

