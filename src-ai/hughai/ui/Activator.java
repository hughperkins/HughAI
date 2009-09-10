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
// ======================================================================================
//

package hughai.ui;

import hughai.basictypes.*;
import hughai.mapping.*;
import hughai.mapping.HeightMap.*;

// This class is used to expose variuos constructors to ecma script
// until such time as we figure out how to expose the appropriate classloader
// to the ecma script engine.
// feel free to add any additional constructors that you need, then send me
// a patchfile, or just tell me which constructors you need, and I'll look at 
// adding those in.
public class Activator {
   public TerrainPos newTerrainPos(float x, float y, float z ) {
      return new TerrainPos( x, y, z );
   }
   public HeightMapPos newHeightMapPos(int x, int y ) {
      return new HeightMapPos( x, y );
   }
}
