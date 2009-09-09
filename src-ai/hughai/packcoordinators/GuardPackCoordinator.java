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



// this sets units to guard a particular unit
// not used or tested yet
public class GuardPackCoordinator extends PackCoordinator
{
   Unit target;
   Unit lasttarget;
   
   HashMap<Unit,Integer> LastOrderTimeByUnit = new HashMap<Unit, Integer>();
   HashSet<Unit> idlingunits = new HashSet<Unit>();
   
   PlayerObjects playerObjects;

   // can pass in pointer to a hashtable in another class if we want
   // ie other class can directly modify our hashtable
   public GuardPackCoordinator(
         PlayerObjects playerObjects )
   {
      super( playerObjects );
      this.playerObjects = playerObjects;

      csai.registerGameListener( new GameListenerHandler() );
   }

   // does NOT imply Activate()
   public void SetTarget( Unit target )
   {
      this.target = target;
      //Activate();
   }

   @Override	
   public void SetTarget( TerrainPos target )
   {
      //	this.targetid = targetid;
      //	Activate();
   }


   // AIFloat3 lasttargetpos = null;

   // handle units that were reported as idling, that are probably still idling
   // we didn't just give them stuff to do straight away, in order to avoid order spam
   void RedoIdling( int frame ){
      for( Unit unit : idlingunits ) {
         SetGuard( unit );
         LastOrderTimeByUnit.put( unit, frame );
      }
      idlingunits.clear();
   }
   
   @Override
   void Recoordinate()
   {
      logfile.WriteLine( this.getClass().getSimpleName() + ".recoordinate()" );
      if( !activated )
      {
         return;
      }

      //if( restartedfrompause || AIFloat3Helper.GetSquaredDistance(targetpos, lasttargetpos ) > ( 20 * 20 ) )
      if( restartedfrompause 
            || target != lasttarget )
      {
//         int frame = aicallback.getGame().getCurrentFrame();
         int frame = playerObjects.getFrameController().getFrame();
         for( Unit unit : unitsControlled )
         {
            //int deployedid = (int)de.Key;
            //UnitDef unitdef = de.Value as UnitDef;
            SetGuard( unit );
            LastOrderTimeByUnit.put(  unit, frame );
         }
         idlingunits.clear();
         lasttarget = target;
      }
   }

   void SetGuard( Unit guardingunit )
   {
      logfile.WriteLine( this.getClass().getSimpleName() + ".SetGuard() " + guardingunit.getUnitId() + " " + guardingunit.getDef().getHumanName() );
      giveOrderWrapper.Guard(guardingunit, target);
      //aicallback.GiveOrder( unitid, new Command( Command.CMD_GUARD, new double[]{ targetid } ) );
   }

   class GameListenerHandler extends GameAdapter {
      @Override
      public void UnitIdle( Unit unit )
      {
         if( activated )
         {
            if( unitsControlled.contains( unit ) )
            {
//               int frame = aicallback.getGame().getCurrentFrame();
               int frame = playerObjects.getFrameController().getFrame();
               Integer lastordertime = LastOrderTimeByUnit.get( unit );
               if( ( lastordertime == null ) || ( lastordertime < ( frame - 30 ) ) ) {
                  SetGuard( unit );
                  LastOrderTimeByUnit.put( unit, frame );
               } else {
                  idlingunits.add( unit );
               }
            }
         }
      }

//      int ticks = 0;
      @Override
      public void Tick( int frame )
      {
//         ticks++;
//         if( ticks >= 30 )
//         {
            RedoIdling(frame);
            //Recoordinate();

//            ticks = 0;
//         }
      }
   }
}

