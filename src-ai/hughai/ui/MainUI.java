package hughai.ui;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import java.util.*;
import java.util.Map;

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
   JPanel panel;
   GridLayout gridLayout;

   public MainUI( PlayerObjects playerObjects ) {
      this.playerObjects = playerObjects;
      Init();
   }

   public void Init() {
      frame = new JFrame( "HughAI " + playerObjects.getAicallback().getTeamId() );
      frame.setSize( 200, 500 );
      frame.setVisible( true );
      //      if( false ) {
      gridLayout = new GridLayout(0,1);
      panel = new JPanel( gridLayout );
      frame.add( panel );
      frame.setVisible( true );
      //      }
      this.playerObjects.getCSAI().registerShutdown( new ShutdownHandler() );
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
         panel = null;
         gridLayout = null;
         buttonActionHandler = null;
         buttonhandlerbybutton = null;
         frame.dispose();
         frame = null;
         playerObjects = null;
         //      }
      }
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
      gridLayout.setRows( gridLayout.getRows() + 1 );
      panel.add( button );
      //frame.setSize( 200, gridLayout.getRows() * 50 );
      frame.setSize( 200, 450 );

      panel.validate();
      frame.validate();
      //      }
   }
}
