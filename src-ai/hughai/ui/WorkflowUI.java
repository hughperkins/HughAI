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
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.springrts.ai.*;
import com.springrts.ai.oo.*;

import hughai.*;
import hughai.EnemyTracker.EnemyAdapter;
import hughai.basictypes.*;
import hughai.building.Workflows;
import hughai.mapping.*;
import hughai.packcoordinators.*;
import hughai.unitdata.*;
import hughai.unitdata.UnitController.UnitAdapter;
import hughai.utils.*;

// provides a GUI for monitoring workflows. Will do.  In theory.
//
// Let's think about what our ideal gui will look like:
// - displays a drop-down for selecting from available workflow names
// - button to add a new workflow
// - button for deleting a workflow
// - button to save all workflows, deleting any no longer in existence
// - button to restore all workflows from file
// - button to save current workflow
// - button to restore current workflow form file
//
// then, for each workflow:
// - display a line for each request
//   - the contents of the workflow file
//   - how many of these units exist in the game right now, as friendly units
//      - a button to press to show them on the map
//   - how many under construction at the moment
//      - a button to show them on the map
//      - maybe have their percent construction
//      - maybe put each of them on separate lines? with a progress bar for each
// - be able to add new lines, specifying priority, selecting unit from
//   current buildtable, by human name, shortname
// - be able to edit current lines, editing quantity, priority, and unit name (?)
//
// conundrum: how much to use reflection, and how much just to statically code?
public class WorkflowUI {
   PlayerObjects playerObjects;
   
   JPanel panel;

   private JList workflowNames;
   
   public WorkflowUI( PlayerObjects playerObjects ) {
      this.playerObjects = playerObjects;
         
      setup();
      
      playerObjects.getCSAI().registerGameListener( new GameListener() );
   }
   
   // creaate any initial panels and stuff
   void setup() {
      panel = new JPanel();
      
      workflowNames = new JList();
      Workflows workflows = playerObjects.getWorkflows();
      DefaultListModel listModel = new DefaultListModel();
      for( String name : workflows.getWorkflowsByName().keySet() ) {
         listModel.addElement( name );
      }
      workflowNames.setModel( listModel );
      workflowNames.addListSelectionListener( new WorkflowNamesSelectionListener() );
      
      panel.add( workflowNames );
      
      JTable table = new JTable( 0, 3 );
      
      refresh();
      
      playerObjects.getMainUI().addPanelToTabbedPanel( "Workflows", panel );      
   }
   
   class WorkflowNamesSelectionListener implements ListSelectionListener {
      @Override
      public void valueChanged( ListSelectionEvent e ) {
         displaySelectedWorkflow();
      }
   }
   
   void displaySelectedWorkflow() {
      String selectedWorkflowname = (String)workflowNames.getSelectedValue();
      Workflows workflows = playerObjects.getWorkflows();
      Workflows.Workflow workflow = workflows.getWorkflowsByName().get( selectedWorkflowname );
      for( Workflows.Workflow.Order order : workflow.orders ) {
         
      }
   }
   
   // will run once a second or so, and update the workflows panel
   void refresh() {
      Workflows workflows = playerObjects.getWorkflows();
      DefaultListModel listModel = new DefaultListModel();
      for( String name : workflows.getWorkflowsByName().keySet() ) {
         listModel.addElement( name );
      }
      workflowNames.setModel( listModel );
//      workflows.
      panel.validate();
   }
   
   class GameListener extends GameAdapter {
      @Override
      public void Tick( int frame ) {
         refresh();
      }
   }
}
