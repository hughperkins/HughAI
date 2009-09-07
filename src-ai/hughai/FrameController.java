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

package hughai;

import java.util.*;

import com.springrts.ai.*;
import com.springrts.ai.oo.*;

import hughai.basictypes.*;
import hughai.unitdata.*;
import hughai.utils.*;

// stores the current frame.  A very tough job!
// I'm assuming the current frame is not cached anywhere
// plus, the method to get it - independent from the tick event - is deprecated

// this class should be instantiated early on, so it gets called early on in the tick stack
public class FrameController {
   PlayerObjects playerObjects;
   CSAI csai;
//   OOAICallback aicallback;
  // LogFile logfile;
   
   int frame;

   public FrameController( PlayerObjects playerObjects ) {
      this.playerObjects = playerObjects;
      this.csai = playerObjects.getCSAI();
//      this.aicallback = playerObjects.getAicallback();
     // this.logfile = playerObjects.getLogFile();
      
   }
   
   public void Init() {
      csai.registerGameListener( new GameListener() );      
   }
   
   class GameListener extends GameAdapter {
      @Override
      public void Tick( int frame ) {
//         logfile.WriteLine( "framecontroller.tick frame " + frame );
         setFrame( frame );
      }
   }

   public int getFrame() {
      return frame;
   }

   void setFrame( int frame ) {
      this.frame = frame;
   }
}
