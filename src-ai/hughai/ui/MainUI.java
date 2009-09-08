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

   JPanel configPanel;
   GridLayout configGridLayout;

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

      configGridLayout = new GridLayout(0,2);
      configPanel = new JPanel( configGridLayout );
      tabbedPane.addTab( "Config", configPanel );

      buildConfigPanel();

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

   Method getGetMethod( Class<?> targetClass, Class<?> fieldType, String fieldName ) {
      fieldName = fieldName.substring( 0, 1 ).toUpperCase()
      + fieldName.substring( 1 );
      String methodname = "get" + fieldName;
      if( fieldType == boolean.class ) {
         methodname = "is" + fieldName;
      }
      Method method = null;
      try {
         method = targetClass.getMethod( methodname, new Class<?>[0] );
      } catch( Exception e ) {
         e.printStackTrace();
      }
      return method;
   }

   Method getSetMethod( Class<?> targetClass, Class<?> fieldType, String fieldName ) {
      fieldName = fieldName.substring( 0, 1 ).toUpperCase()
      + fieldName.substring( 1 );
      String methodname = "set" + fieldName;
      Method method = null;
      try {
         method = targetClass.getMethod( methodname, new Class<?>[]{ fieldType } );
      } catch( Exception e ) {
         e.printStackTrace();
      }
      return method;
   }

   // kind of ugly to do this in this class, but it doesn't really fit
   // the config class either.
   // either confighelper, or a differnet, new class perhaps?
   void buildConfigPanel() {
      try {
         Config config = playerObjects.getConfig();
         for( Field field : config.getClass().getDeclaredFields() ) {
            Annotation excludeAnnotation = field.getAnnotation( ReflectionHelper.Exclude.class );
            if( excludeAnnotation == null ) { // so, this field is not excluded
               Class<?> fieldType = field.getType();
               Method getMethod = getGetMethod( config.getClass(), field.getType(), field.getName() );
               if( getMethod != null ) {
                  Object value = getMethod.invoke( config ); 
                  if( fieldType == String.class ) {
                     addTextBox( field.getName(), (String)value );
                  }
                  if( fieldType == boolean.class ) {
                     addBooleanComponent( field.getName(), (Boolean)value );
                  }
                  if( fieldType == float.class ) {
                     addTextBox( field.getName(), "" + value );
                  }
                  if( fieldType == int.class ) {
                     addTextBox( field.getName(), "" + value );
                  }
               } else {
                  playerObjects.getLogFile().WriteLine( "No get accessor method for config field " + field.getName() );
               }
            }
         }
      } catch( Exception e ) {
         e.printStackTrace();
      }

      configGridLayout.setRows( configGridLayout.getRows() + 2 );

      configRevertButton = new JButton( "Revert" );
      configReloadButton = new JButton( "Reload" );
      configApplyButton = new JButton( "Apply" );
      configSaveButton = new JButton( "Save" );
      
      configRevertButton.addActionListener( new ConfigRevert() );
      configReloadButton.addActionListener( new ConfigReload() );
      configApplyButton.addActionListener( new ConfigApply() );
      configSaveButton.addActionListener( new ConfigSave() );

      configPanel.add( configRevertButton );
      configPanel.add( configReloadButton );
      configPanel.add( configApplyButton );
      configPanel.add( configSaveButton );
   }

   JButton configRevertButton;
   JButton configReloadButton;
   JButton configApplyButton;
   JButton configSaveButton;
   HashMap< String, JComponent > componentByName = new HashMap<String, JComponent>();

   void addTextBox( String name, String currentValue ) {
      configGridLayout.setRows( configGridLayout.getRows() + 1 );
      addConfigLabel( name );

      JTextField textField = new JTextField();
      textField.setText( currentValue );

      componentByName.put(  name, textField );
      configPanel.add( textField );
   }

   void addConfigLabel( String labelName ) {
      JLabel label = new JLabel( labelName );
      configPanel.add( label );      
   }

   void addBooleanComponent( String name, boolean currentValue ) {
      configGridLayout.setRows( configGridLayout.getRows() + 1 );
      addConfigLabel( name );

      JCheckBox checkBox = new JCheckBox();
      checkBox.setSelected( currentValue );

      componentByName.put(  name, checkBox );
      configPanel.add( checkBox );
   }

   void debug( Object message ) {
      playerObjects.getLogFile().WriteLine( "" + message );
   }
   
   class ConfigRevert implements ActionListener {
      @Override
      public void actionPerformed( ActionEvent event ){
         revertConfig();
      }
   }
      
   void revertConfig() {
      try {
         debug("reverting config panel");
         Config config = playerObjects.getConfig();
         for( Field field : config.getClass().getDeclaredFields() ) {
            Annotation excludeAnnotation = field.getAnnotation( ReflectionHelper.Exclude.class );
            if( excludeAnnotation == null ) { // so, this field is not excluded
               debug("field " + field.getName() );
               Class<?> fieldType = field.getType();
               Method getMethod = getGetMethod( config.getClass(), field.getType(), field.getName() );
               if( getMethod != null ) {
                  debug(" ... found accessor method" );
                  Object value = getMethod.invoke( config );
                  String fieldname = field.getName();
                  Component component = componentByName.get( fieldname );
                  if( component != null ) {
                     debug(" ... found component" );
                     if( fieldType == String.class ) {
                        ((JTextField)component).setText( (String )value );
                     }
                     if( fieldType == boolean.class ) {
                        ((JCheckBox)component).setSelected( (Boolean )value );
                     }
                     if( fieldType == float.class ) {
                        ((JTextField)component).setText( "" + value );
                     }
                     if( fieldType == int.class ) {
                        ((JTextField)component).setText( "" + value );
                     }
                  }
               } else {
                  playerObjects.getLogFile().WriteLine( "No get accessor method for config field " + field.getName() );
               }
            }
         }
      } catch( Exception e ) {
         e.printStackTrace();
      }
   }

   class ConfigApply implements ActionListener {
      @Override
      public void actionPerformed( ActionEvent event ){
         applyConfig();
      }
   }
   
   void applyConfig() {
      debug("applying config from panel");
      Config config = playerObjects.getConfig();
      for( Field field : config.getClass().getDeclaredFields() ) {
         Annotation excludeAnnotation = field.getAnnotation( ReflectionHelper.Exclude.class );
         if( excludeAnnotation == null ) { // so, this field is not excluded
            debug("field " + field.getName() );
            Class<?> fieldType = field.getType();
            Method setMethod = getSetMethod( config.getClass(), field.getType(), field.getName() );
            if( setMethod != null ) {
               debug(" ... found accessor method" );
               String fieldname = field.getName();
               Component component = componentByName.get( fieldname );
               if( component != null ) {
                  debug(" ... found component" );
                  Object value = null;
                  if( fieldType == String.class ) {
                     value = ((JTextField)component).getText();
                  }
                  if( fieldType == boolean.class ) {
                     value = ((JCheckBox)component).isSelected();
                  }
                  if( fieldType == float.class ) {
                     String stringvalue = (String)((JTextField)component).getText();
                     try {
                        value = Float.parseFloat( stringvalue );
                     } catch( Exception e ) {
                     }
                  }
                  if( fieldType == int.class ) {
                     String stringvalue = (String)((JTextField)component).getText();
                     try {
                        value = Integer.parseInt( stringvalue );
                     } catch( Exception e ) {
                     }
                  }
                  if( value != null ) {
                     try {
                        setMethod.invoke( config, value );
                     } catch( Exception e ) {
                        e.printStackTrace();
                     }
                  }
               }
               if( fieldType == boolean.class ) {
                  //addBooleanComponent( field.getName(), (Boolean)value );
               }
            } else {
               playerObjects.getLogFile().WriteLine( "No get accessor method for config field " + field.getName() );
            }
         }
      }
      revertConfig(); // in case some parses and stuff didn't work, so 
                      // user can see what is actually being read.
      JOptionPane.showMessageDialog( frame, "Config updated.  Note that most changes require an AI restart.  You can click on 'reloadAI' in 'Actions' tab to do so." );
   }

   class ConfigSave implements ActionListener {
      @Override
      public void actionPerformed( ActionEvent e ){
         applyConfig();
         playerObjects.getConfig().save();
      }
   }
   
   class ConfigReload implements ActionListener {
      @Override
      public void actionPerformed( ActionEvent e ){
         playerObjects.getConfig().reload();
         revertConfig();
      }
   }
}
