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

import java.awt.*;
import java.awt.event.*;

import java.lang.reflect.*;
import javax.swing.*;

import java.util.*;
import java.util.Map;
import java.lang.annotation.*;

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
import hughai.utils.Config.ConfigListener;

// This creates a form with buttons in
// other components register their methods with this form
// for now, this just handles buttons, and simple no-parameter methods
// might be extended in the future!
public class MainUI {
   public interface ButtonHandler {
      public void go();
   }

   ButtonActionHandler buttonActionHandler = new ButtonActionHandler();

   PlayerObjects playerObjects;

   JFrame frame;

   JTabbedPane tabbedPane;

   JPanel actionsPanel;
   GridLayout actionsGridLayout;

   public MainUI( PlayerObjects playerObjects ) {
      this.playerObjects = playerObjects;
      Init();
   }
   
   public void Init() {
      frame = new JFrame( "HughAI " + playerObjects.getAicallback().getTeamId() );
      frame.setSize( 200, 500 );
      frame.setVisible( true );

      tabbedPane = new JTabbedPane();
      frame.add( tabbedPane );

      //      if( false ) {
      actionsGridLayout = new GridLayout(0,1);
      actionsPanel = new JPanel( actionsGridLayout );
      tabbedPane.addTab( "Actions", actionsPanel );
      
      frame.setVisible( playerObjects.getConfig().isGUIActivated() );
      //      }
      this.playerObjects.getCSAI().registerShutdown( new ShutdownHandler() );
      this.playerObjects.getConfig().registerListener( new ConfigHandler() );
   }
   
   class ConfigHandler implements ConfigListener {
      @Override
      public void configUpdated() {
         playerObjects.getLogFile().WriteLine( "MainUI.ConfigHandler()" );
         frame.setVisible( playerObjects.getConfig().isGUIActivated() );         
      }
   }

   class ShutdownHandler implements CSAI.ShutdownHandler {
      @Override
      public void shutdown() {
         //      if( false ) {
         playerObjects.getLogFile().WriteLine( "MainUI.shutdown()" );
         synchronized ( buttonhandlerbybutton ) {
            for( JButton button : buttonhandlerbybutton.keySet() ) {
               button.removeActionListener( buttonActionHandler );
            }
            buttonhandlerbybutton.clear();
         }
         //frame.setVisible( false );
         actionsPanel = null;
         actionsGridLayout = null;
         buttonActionHandler = null;
         buttonhandlerbybutton = null;
         frame.dispose();
         frame = null;
         playerObjects = null;
         //      }
      }
   }
   
   public void addPanelToTabbedPanel( String title, JPanel panel ) {
      playerObjects.getLogFile().WriteLine( "MainUI: addpaneltotabbedpanel " + title );
      tabbedPane.addTab( title, panel );
      frame.validate();
   }

   @Override
   protected void finalize() {
      System.out.println( this.getClass().getSimpleName() + ".finalize()");
   }

   //  HashSet<JButton> buttons = new HashSet<JButton>();

   // we could just add the handlers directly to the button
   // but I fear this may be part of the cause of the major swing
   // memory leak I had when I decided to move them into this hashmap
   HashMap<JButton,ButtonHandler> buttonhandlerbybutton = new HashMap<JButton, ButtonHandler>();

   class ButtonActionHandler implements ActionListener {
      @Override
      public void actionPerformed( ActionEvent e ){
         if( JButton.class.isAssignableFrom( e.getSource().getClass() ) ) {
            JButton button = (JButton)e.getSource();
            ButtonHandler handler = null;
            synchronized( buttonhandlerbybutton ) {
               handler = buttonhandlerbybutton.get( button );
            }
            // make sure this runs outside of the synchronized section above
            if( handler != null ) {
               handler.go();
            }
         }
      }
   }

   public void registerButton( String buttonText, ButtonHandler buttonHandler  ) {
      //      if( false ) {
      playerObjects.getLogFile().WriteLine( "MainUI.registerbutton " + buttonText );
      JButton button = new JButton( buttonText );
      button.addActionListener( buttonActionHandler );
      synchronized (buttonhandlerbybutton) {
         buttonhandlerbybutton.put( button, buttonHandler );
      }
      actionsGridLayout.setRows( actionsGridLayout.getRows() + 1 );
      actionsPanel.add( button );
      //frame.setSize( 200, gridLayout.getRows() * 50 );
      frame.setSize( 400, 450 );

      actionsPanel.validate();
      frame.validate();
      //      }
   }
   
   public void showInfo( String message ) {
      JOptionPane.showMessageDialog( frame, message );
   }
}
