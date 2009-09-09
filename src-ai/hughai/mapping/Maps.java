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

package hughai.mapping;

import hughai.PlayerObjects;
import hughai.*;
import hughai.utils.LogFile;


// holds the other map classes. whether this is a good idea is another question.
public class Maps {
   LosMap losMap;
   BuildMap buildMap;
   EnemyMap enemyMap;
   HeightMap heightMap;
   MovementMaps movementMaps;
   SlopeMap slopeMap;
   ThreatMap threatMap;
   LogFile logfile;

   public Maps( PlayerObjects playerObjects ) {
      logfile = playerObjects.getLogFile();
      System.out.println("maps making losmap.");
      losMap = new LosMap(playerObjects);
      logfile.WriteLine( "playerobjects instantiating new BuildMap" );
      buildMap = new BuildMap(playerObjects);
      logfile.WriteLine( "playerobjects instantiating new EnemyMap" );
      enemyMap = new EnemyMap(playerObjects);
      logfile.WriteLine( "playerobjects instantiating new HeightMap" );
      heightMap = new HeightMap(playerObjects);
      logfile.WriteLine( "playerobjects instantiating new HeightMap" );
      movementMaps = new MovementMaps(playerObjects);
      logfile.WriteLine( "playerobjects instantiating new SlopeMap" );
      slopeMap = new SlopeMap(playerObjects);
      logfile.WriteLine( "playerobjects instantiating new ThreatMap" );
      threatMap = new ThreatMap(playerObjects);
      logfile.WriteLine( " ... maps done." );
   }
   
   public void Init() {
      movementMaps.Init();
   }

   public LosMap getLosMap() {
      return losMap;
   }

   public BuildMap getBuildMap() {
      return buildMap;
   }

   public EnemyMap getEnemyMap() {
      return enemyMap;
   }

   public HeightMap getHeightMap() {
      return heightMap;
   }

   public MovementMaps getMovementMaps() {
      return movementMaps;
   }

   public SlopeMap getSlopeMap() {
      return slopeMap;
   }

   public ThreatMap getThreatMap() {
      return threatMap;
   }
}
