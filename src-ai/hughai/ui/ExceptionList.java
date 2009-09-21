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

import java.util.*;
import java.util.Map;

import javax.swing.*;

import com.springrts.ai.*;
import com.springrts.ai.oo.*;

import hughai.*;
import hughai.EnemyTracker.EnemyAdapter;
import hughai.basictypes.*;
import hughai.mapping.*;
import hughai.packcoordinators.*;
import hughai.unitdata.*;
import hughai.unitdata.UnitController.UnitAdapter;
import hughai.utils.*;

// collects any exceptions, and shows them in a tab in the gui
public class ExceptionList {
   PlayerObjects playerObjects;
   
   JPanel panel;
   JScrollPane scrollpane;
   JTextArea exceptionsText;
   
   public ExceptionList( PlayerObjects playerObjects ) {
      this.playerObjects = playerObjects;
      
      panel = new JPanel();
      scrollpane = new JScrollPane();
      panel.add( scrollpane );
      
      exceptionsText = new JTextArea();
      scrollpane.add( exceptionsText );
      
      playerObjects.getMainUI().addPanelToTabbedPanel( "Exceptions", panel );
   }
   
   boolean exceptionReportedAlready = false;
   public void reportException( Exception e ) {
      if( !exceptionReportedAlready ) {
         playerObjects.getMainUI().showInfo( "Something went wrong :-/  Please send the team log in the AI directory to hughperkins@gmail.com.  Techie info follows: " + Formatting.exceptionToStackTrace( e ) );
         exceptionReportedAlready = true;
      }
      exceptionsText.setText(
           exceptionsText.getText() + 
           Formatting.exceptionToStackTrace( e ) + "\n" );
      panel.validate();
   }
}
