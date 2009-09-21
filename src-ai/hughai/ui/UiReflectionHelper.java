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
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.annotation.*;
import java.lang.reflect.*;

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
import hughai.utils.ReflectionHelper.CustomClass;
import hughai.utils.ReflectionHelper.Exclude;
import hughai.utils.ReflectionHelper.ListTypeInfo;

public class UiReflectionHelper {
   PlayerObjects playerObjects;

   public UiReflectionHelper( PlayerObjects playerObjects ) {
      this.playerObjects = playerObjects;
   }
   
   public void dispose() {
      
   }

   JComponent objectToEditComponent( Object object, Annotation[] annotations ) {
      Class<?> objectClass = object.getClass();
      if( objectClass == String.class ) {
         JTextField textField = new JTextField();
         textField.setText( "" + object );
         return textField;
      }
      if( objectClass == boolean.class ) {
         JCheckBox checkBox = new JCheckBox();
         checkBox.setSelected( (Boolean)object );
         return checkBox;
      }
      if( objectClass == float.class ) {
         JTextField textField = new JTextField();
         textField.setText( "" + object );
         return textField;
      }
      if( objectClass == int.class ) {
         JTextField textField = new JTextField();
         textField.setText( "" + object );
         return textField;
      }
      if( List.class.isAssignableFrom( objectClass ) ) {

      }
      //      boolean foundCustomClassAttribute = false;
      //      for( Annotation annotation : annotations ) {
      //         if( CustomClass.class.isAssignableFrom( annotation.getClass() ) ) {
      //            foundCustomClassAttribute = true;
      //         }
      //      }
      //      if( foundCustomClassAttribute ) {
      // then, we put each field into its own column in a row?
      // and if one of them is a  list?
      for( Field field : objectClass.getDeclaredFields() ) {

      }
      //      }

      throw new RuntimeException("UiReflectionhelper: unhandled type: " + objectClass.getSimpleName() );
   }

   public enum DisplayContext {
      detailed,
      listrow,
      brief
   }

   // ok, so if we call with a primitive, it's easy.
   // if we call with a class, then we want either:
   //   fieldone: valueone
   //   fieldtwo: valuetwo
   //   fieldthree: valuethree
   // or:
   //   fieldone    fieldtwo    fieldthree
   //   valueone    valuetwo    valuethree
   // maybe the first is better?
   // next, if we call with a list, then we want something like:
   //   valueone
   //   valuetwo
   //   valuethree
   // what if it is a list of classes?  then something like:
   //   fieldone    fieldtwo   fieldthree
   //   itemonev1   itemonev2  itemonev3
   //   itemtwov1   itemtwov2  itemtwov3
   //
   // if we call with a class with a list in, then something like:
   //    fieldone: valueone
   //    fieldtwo: valuetwo
   //    fieldthree: valuethree
   //    listfield: [button here to see details]
   // or maybe:
   //    fieldone: valueone
   //    fieldtwo: valuetwo
   //    fieldthree: valuethree
   //    listfield:
   //    valueone
   //    valuetwo
   //    valuethree
   // or:
   //    fieldone: valueone
   //    fieldtwo: valuetwo
   //    fieldthree: valuethree
   //    listfield:  valueone
   //                valuetwo
   //                valuethree
   // first option: button to see details, is easiest
   //
   // what if list of classes, and the class contains a list?
   // maybe as before for a list of classes, with the button added?
   //
   // ok, so detailed can be true or false:
   // if true, then class fields will be listed vertically, and list in full
   // if false, then lists and classes will just become a button?
   // if true, and classasrowtable is true, then a class will be horizontal,
   // as a table row, otherwise vertically
   //
   // so much for layout... what about behavior when canModify is true?
   // - probably need a button 'apply', 'revert' for each dialog and sub-dialog, ie for classes, maybe also for lists?
   //   - apply and revert applies only to that dialog, not to modifications to children dialogs
   // - for lists, need button for each row, to delete it, and a button to add a new row
   // - for primitive types, nothing is applied until parent class or list applies it?
   // - maybe 'apply' and 'revert' apply at the level of each custom class?
   public JComponent objectToDisplayComponent( final Object object, final Annotation[] annotations, DisplayContext displayContext, final boolean canModify ) {
      try {
      if( object == null ) {
         return new JLabel("<null>");
      }
      
      final Class<?> objectClass = object.getClass();
      if( objectClass == String.class ) {
         if( canModify ) {
            JTextField textField = new JTextField();
            textField.setText( "" + object );
            return textField;
         } else {
            JLabel textField = new JLabel();
            textField.setText( "" + object );
            return textField;            
         }
      }
      if( objectClass.equals( Float.class ) ) {
         if( canModify ) {
            JTextField textField = new JTextField();
            textField.setText( "" + object );
            return textField;
         } else {
            JLabel textField = new JLabel();
            textField.setText( "" + object );
            return textField;
         }
      }
      if( objectClass.equals( Integer.class ) ) {
         if( canModify ) {
            JTextField textField = new JTextField();
            textField.setText( "" + object );
            return textField;
         } else {
            JLabel textField = new JLabel();
            textField.setText( "" + object );
            return textField;
         }
      }
      if( objectClass.equals( Boolean.class ) ) {
         JCheckBox checkbox = new JCheckBox();
         checkbox.setEnabled( canModify );
         checkbox.setSelected( (Boolean )object );
         return checkbox;
      }
      if( List.class.isAssignableFrom( objectClass ) ) {
         Class<?> listItemClass = getListItemClass( annotations );
         if( listItemClass == null ) {
            throw new RuntimeException("Missing ListTypeInfo annotation on list" );
         }
         
         if( displayContext.equals( DisplayContext.brief )
               || displayContext.equals( DisplayContext.listrow ) ) { // just return a button
            final JButton button = new JButton( "..." );
            button.addActionListener( new ActionListener() {
               @Override
               public void actionPerformed( ActionEvent e ) {
                  System.out.println("creating child dialog...");
                  JDialog childDialog = createDialog( button, "List", true );
                  JComponent childcomponents = objectToDisplayComponent(object, annotations, DisplayContext.detailed, canModify );
                  childDialog.add( childcomponents );
                  childDialog.setSize( 300, 300 );
                  childDialog.setVisible( true );
               }
            } );
            return button;
         }

         // otherwise show as a table
         List objectAsList = (List)object;
         GridLayout gridLayout = new GridLayout(0,1);
         JPanel panel = new JPanel( gridLayout );

         JComponent headers = listToHeaders( listItemClass );
         if( headers != null ) {
            addGridLayoutRow( gridLayout );
            panel.add( headers );
         }

         for( Object item : objectAsList ) {
            JComponent childcomponent = objectToDisplayComponent( item,
                  annotations, DisplayContext.listrow, canModify );
            addGridLayoutRow( gridLayout );
            panel.add( childcomponent );
         }         
         return panel;
      }
      //      boolean foundCustomClassAttribute = false;
      //      for( Annotation annotation : annotations ) {
      //         if( CustomClass.class.isAssignableFrom( annotation.getClass() ) ) {
      //            foundCustomClassAttribute = true;
      //         }
      //      }
      //      if( foundCustomClassAttribute ) {
      if( displayContext.equals( DisplayContext.brief ) ) { // just return a button
//         JPanel panel = new JPanel( new GridLayout( 1, 1 ));
         
//         final JLabel label = new JLabel( objectClass.getSimpleName() );
         
         final JButton button = new JButton( "..." );
         button.setSize( 50, 50 );
         
         button.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed( ActionEvent e ) {
               System.out.println("creating child dialog...");
               JDialog childDialog = createDialog( button, objectClass.getSimpleName(), true );
               JComponent childcomponents = objectToDisplayComponent(object, new Annotation[0], DisplayContext.detailed, canModify );
               childDialog.add( childcomponents );
               childDialog.setSize( 300, 300 );
               childDialog.setVisible( true );
            }
         } );
//         panel.add( label );
//         panel.add( button );
         return button;
//         return panel;
      }

      if( displayContext.equals( DisplayContext.listrow ) ) { // put each field into a column
         GridLayout gridLayout = new GridLayout(1,0);
         JPanel panel = new JPanel( gridLayout );
         for( Field field : objectClass.getDeclaredFields() ) {
            JComponent childcomponent = objectToDisplayComponent( field.get( object ), field.getAnnotations(), DisplayContext.brief, canModify );
            gridLayout.setColumns( gridLayout.getColumns() + 1 );
            panel.add( childcomponent );
         }
         return panel;            
      } else {
         // then, we put each field into its own row
         GridLayout gridLayout = new GridLayout(0,2);
         JPanel panel = new JPanel( gridLayout );
         for( Field field : objectClass.getDeclaredFields() ) {
            JLabel label = new JLabel( field.getName() );
            JComponent childcomponent = objectToDisplayComponent( field.get( object ), field.getAnnotations(), DisplayContext.brief, canModify );

            gridLayout.setRows( gridLayout.getRows() + 1 );

            panel.add(  label );
            panel.add( childcomponent );
         }
         return panel;
      }
      } catch( Exception e ) {
         e.printStackTrace();
         throw new RuntimeException( e );
      }

      //      }

//      throw new RuntimeException("UiReflectionhelper: unhandled type: " + objectClass.getSimpleName() );
   }
   
   JComponent listToHeaders( Class<?> listClass ) {
      if( listClass.isPrimitive() ) {
         return null;
      }
      
      if( listClass.equals( String.class ) ) {
         return null;
      }
      
      GridLayout gridLayout = new GridLayout( 1, 0 );
      JPanel panel = new JPanel( gridLayout );
      
      // assume custom class, for now
      for( Field field : listClass.getDeclaredFields() ) {
         if( isExcluded( field ) ) {
            continue;
         }
         
         JLabel label = new JLabel( field.getName() );
         addGridLayoutColumn( gridLayout );
         panel.add( label );
      }
      return panel;
   }
   
   void addGridLayoutRow( GridLayout gridLayout ) {
      gridLayout.setRows( gridLayout.getRows() + 1 );
   }
   
   void addGridLayoutColumn( GridLayout gridLayout ) {
      gridLayout.setColumns( gridLayout.getColumns() + 1 );
   }
   
   boolean isExcluded(Annotation[] annotations ) {
      for( Annotation annotation : annotations ) {
         if( Exclude.class.isAssignableFrom( annotation.getClass() ) ) {
            return true;
         }
      }
      return false;
   }
   
   boolean isExcluded( Field field ) {
      return isExcluded( field.getAnnotations() ); // not that efficient, but do we care?
   }
   
   Class<?> getListItemClass( Annotation[] annotations ) {
      for( Annotation annotation : annotations ) {
         if( ListTypeInfo.class.isAssignableFrom( annotation.getClass() ) ) {
            ListTypeInfo listTypeInfo = (ListTypeInfo)annotation;
            return listTypeInfo.value();
         }
      }
      return null;
   }
   
   JDialog createDialog( Component lockedtoparentofthiscomponent, String title, boolean modal ) {
      if( JFrame.class.isAssignableFrom( lockedtoparentofthiscomponent.getClass() ) ) {
         JDialog dialog = new JDialog( (JFrame)lockedtoparentofthiscomponent,
               title, modal );
         return dialog;
      }
      if( JDialog.class.isAssignableFrom( lockedtoparentofthiscomponent.getClass() ) ) {
         JDialog dialog = new JDialog( (JDialog)lockedtoparentofthiscomponent,
               title, modal );
         return dialog;
      }
      if( lockedtoparentofthiscomponent == null ) {
         throw new RuntimeException("UiReflectionHelper: couldn't find parent frame or dialog" );
      }
      return createDialog( lockedtoparentofthiscomponent.getParent(), title, modal );
   }

//   JComponent objectToTableHeaders( Object object, Annotation[] annotations ) {
//      return null; // placeholder
//   }
}
