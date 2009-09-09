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

package hughai.mapping;

import java.util.*;

import com.springrts.ai.*;
import com.springrts.ai.oo.*;

import hughai.CSAI;
import hughai.PlayerObjects;
import hughai.ResourceManager;
import hughai.basictypes.*;
import hughai.*;
import hughai.unitdata.UnitController;
import hughai.unitdata.UnitDefHelp;
import hughai.utils.*;


public class ReclaimHelper
{
   final int maxreclaimradius = 500;
   final int reclaimradiusperonehundredmetal = 150;

   PlayerObjects playerObjects;
   LogFile logfile;
   OOAICallback aicallback;
   UnitController unitController;
   UnitDefHelp unitDefHelp;
   ResourceManager resourceManager;
   DrawingUtils drawingUtils;
   //	MovementMaps movementMaps;
   CSAI csai;
   Maps maps;

   public ReclaimHelper( PlayerObjects playerObjects )
   {
      this.playerObjects = playerObjects;
      logfile = playerObjects.getLogFile();
      aicallback = playerObjects.getAicallback();
      unitController = playerObjects.getUnitController();
      unitDefHelp = playerObjects.getUnitDefHelp();
      resourceManager = playerObjects.getResourceManager();
      drawingUtils = playerObjects.getDrawingUtils();
      //		movementMaps = playerObjects.getMovementMaps();
      csai = playerObjects.getCSAI();
      maps = playerObjects.getMaps();
   }

   public TerrainPos GetNearestReclaim(Unit constructor)
   {
      TerrainPos mypos = unitController.getPos( constructor );
      if( playerObjects.getFrameController().getFrame() == 0 )// check ticks first, beacuse metal shows as zero at start
      {
         return null;
      }
      UnitDef unitdef = constructor.getDef();
      if (!unitDefHelp.IsMobile(unitdef))
      {
         return null;
      }
      //Float3 mypos = aicallback.GetUnitPos( constructorid );
      int currentarea = maps.getMovementMaps().GetArea( unitdef, mypos );
      //double nearestreclaimdistancesquared = 1000000;
      //Float3 nearestreclaimpos = null;
      float bestmetaldistanceratio = 0;
      Feature bestreclaim = null;
      int metalspace = (int)( resourceManager.getMetalStorage() 
            - resourceManager.getCurrentMetal() );
      logfile.WriteLine( "available space in metal storage: " + metalspace );
      List<Feature> nearbyfeatures = aicallback.getFeaturesIn(
            mypos.toAIFloat3(), maxreclaimradius );
      boolean reclaimfound = false;
      for( Feature feature : nearbyfeatures )
      {
         FeatureDef featuredef = feature.getDef();
         float containedMetal = featuredef.getContainedResource(resourceManager.getMetalResource()); 
         if( containedMetal > 0
               && containedMetal <= metalspace  )
         {
            TerrainPos thisfeaturepos = TerrainPos.fromAIFloat3( feature.getPosition() );
            float thisdistance = (float) Math.sqrt( 
                  thisfeaturepos.GetSquaredDistance( mypos ) );
            float thismetaldistanceratio = containedMetal / thisdistance;
            if( thismetaldistanceratio > bestmetaldistanceratio 
                  && maps.getMovementMaps().GetArea( unitdef, thisfeaturepos ) == currentarea )
            {
               logfile.WriteLine( "Potential reclaim, distance = " + thisdistance + " metal = " + containedMetal + " ratio = " + thismetaldistanceratio );
               bestmetaldistanceratio = thismetaldistanceratio;
               bestreclaim = feature;
               //         nearestreclaimpo
               reclaimfound = true;
            }
         }
      }
      if( reclaimfound 
            && ( bestmetaldistanceratio > ( 1.0 / ( 100 * reclaimradiusperonehundredmetal ) ) ) )
      {
         TerrainPos reclaimpos = TerrainPos.fromAIFloat3( bestreclaim.getPosition() );
         logfile.WriteLine( "Reclaim found, pos " + reclaimpos.toString() );
         if( csai.DebugOn )
         {
            drawingUtils.DrawUnit( "ARMMEX", reclaimpos, 0.0f, 200, aicallback.getTeamId(), true, true);
         }
         return reclaimpos;
         //aicallback.GiveOrder( constructorid, new Command( Command.CMD_RECLAIM, 
         //    new double[]{ reclaimpos.x, reclaimpos.y, reclaimpos.z, 10 } ) );
      }
      else
      {
         //logfile.WriteLine( "No reclaim within parameters" );
         return null;
      }
   }
}


