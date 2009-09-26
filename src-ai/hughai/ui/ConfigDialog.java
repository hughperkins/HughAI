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
import hughai.utils.ConfigController.ConfigSource;

// builds the configuration tab in the gui
// using reflection
// supports most primitive types
// todo: lists, custom classes, lists of custom classes..
public class ConfigDialog {
   PlayerObjects playerObjects;

   JPanel configPanel;
   GridLayout configGridLayout;

   public ConfigDialog( PlayerObjects playerObjects ) {
      this.playerObjects = playerObjects;
      Init();
   }
   
   public void Init() {
      configGridLayout = new GridLayout(0,2);
      configPanel = new JPanel( configGridLayout );
      
      playerObjects.getMainUI().addPanelToTabbedPanel( "Config", configPanel );

      buildConfigPanel();      
   }
   Method getGetMethod( Class<?> targetClass, Class<?> fieldType, String fieldName ) {
      fieldName = fieldName.substring( 0, 1 ).toUpperCase()
      + fieldName.substring( 1 );
      String methodname = "get" + fieldName;
      if( fieldType == boolean.class || fieldType == Boolean.class ) {
         methodname = "is" + fieldName;
      }
      Method method = null;
      try {
         method = targetClass.getMethod( methodname, new Class<?>[0] );
      } catch( Exception e ) {
         playerObjects.getLogFile().WriteLine( Formatting.exceptionToStackTrace( e ) );
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
         playerObjects.getLogFile().WriteLine( Formatting.exceptionToStackTrace( e ) );
      }
      return method;
   }

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
                  if( fieldType == boolean.class || fieldType == Boolean.class ) {
                     addBooleanComponent( field.getName(), (Boolean)value );
                  }
                  if( fieldType == float.class || fieldType == Float.class ) {
                     addTextBox( field.getName(), "" + value );
                  }
                  if( fieldType == int.class || fieldType == Integer.class ) {
                     addTextBox( field.getName(), "" + value );
                  }
               } else {
                  playerObjects.getLogFile().WriteLine( "No get accessor method for config field " + field.getName() );
               }
            }
         }
      } catch( Exception e ) {
         playerObjects.getLogFile().WriteLine( Formatting.exceptionToStackTrace( e ) );
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
                     if( fieldType == boolean.class || fieldType == Boolean.class ) {
                        ((JCheckBox)component).setSelected( (Boolean )value );
                     }
                     if( fieldType == float.class || fieldType == Float.class ) {
                        ((JTextField)component).setText( "" + value );
                     }
                     if( fieldType == int.class || fieldType == Integer.class ) {
                        ((JTextField)component).setText( "" + value );
                     }
                  }
               } else {
                  playerObjects.getLogFile().WriteLine( "No get accessor method for config field " + field.getName() );
               }
            }
         }
      } catch( Exception e ) {
         playerObjects.getLogFile().WriteLine( Formatting.exceptionToStackTrace( e ) );
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
                  if( fieldType == boolean.class || fieldType == Boolean.class ) {
                     value = ((JCheckBox)component).isSelected();
                  }
                  if( fieldType == float.class || fieldType == Float.class ) {
                     String stringvalue = (String)((JTextField)component).getText();
                     try {
                        value = Float.parseFloat( stringvalue );
                     } catch( Exception e ) {
                     }
                  }
                  if( fieldType == int.class || fieldType == Integer.class ) {
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
                        playerObjects.getLogFile().WriteLine( Formatting.exceptionToStackTrace( e ) );
                     }
                  }
               }
               if( fieldType == boolean.class || fieldType == Boolean.class ) {
                  //addBooleanComponent( field.getName(), (Boolean)value );
               }
            } else {
               playerObjects.getLogFile().WriteLine( "No get accessor method for config field " + field.getName() );
            }
         }
      }
      revertConfig(); // in case some parses and stuff didn't work, so 
                      // user can see what is actually being read.
      playerObjects.getMainUI().showInfo( "Config updated.  Note that most changes require an AI restart.  You can click on 'reloadAI' in 'Actions' tab to do so." );
      playerObjects.getConfig().configUpdated();
   }

   class ConfigSave implements ActionListener {
      @Override
      public void actionPerformed( ActionEvent e ){
         applyConfig();
         playerObjects.getConfigController().writeConfigBackToSource( ConfigSource.XmlFile );
//         playerObjects.getConfig().save();
      }
   }
   
   class ConfigReload implements ActionListener {
      @Override
      public void actionPerformed( ActionEvent e ){
         //playerObjects.getConfig().reload();
         playerObjects.getConfigController().restoreFromSource( ConfigSource.WorkingCopy );
         playerObjects.getConfig().configUpdated();
         revertConfig();
      }
   }
}
