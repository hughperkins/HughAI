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

package hughai.unitdata;

import hughai.PlayerObjects;
import hughai.utils.TdfParser;

// figures out the name of our side, from the startscript
// a difficult task ;-) , but actually, harder than it seems, so let's factor
// it out like this
public class SideManager {
   PlayerObjects playerObjects;
   
   private String side;
   
   public SideManager( PlayerObjects playerObjects ) {
      this.playerObjects = playerObjects;
      init();
   }
   
   void debug( Object message ) {
      playerObjects.getLogFile().WriteLine( "SideManager: " + message);
   }
   
   void init() {
      String startscriptcontents = playerObjects.getAicallback().getGame().getSetupScript();
//      debug( "start script: " );
//      debug( startscriptcontents );
      
      int ourteamnumber = playerObjects.getAicallback().getTeamId();
      debug("our team number: " + ourteamnumber );
      
      TdfParser tdfParser = new TdfParser( playerObjects, startscriptcontents );
//      this.side = tdfParser.RootSection.GetStringValue( "GAME/TEAM" + ourteamnumber + "/Side" ).toLowerCase();
      this.side = playerObjects.getAicallback().getGame().getTeamSide( ourteamnumber );
//      tdfParser.RootSection.SubSections.
      debug("Our side: " + this.side ); 
      if( this.side.equals("") ) {
         throw new RuntimeException("Failed to obtain the name of our faction." );
      }
   }

   public String getSide() {
      return side;
   }
}
