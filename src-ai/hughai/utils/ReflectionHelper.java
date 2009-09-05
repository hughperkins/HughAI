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

package hughai.utils;

import java.io.*;
import java.io.ObjectInputStream.GetField;
import java.util.*;
import java.lang.reflect.*;
import java.lang.annotation.*;

import org.w3c.dom.*;

import hughai.*;
import hughai.basictypes.*;
import hughai.utils.*;

public class ReflectionHelper<T> {
   // Put this on an ArrayList that you want to save/load from an xml file
   // and then you can specify the generic type of the ArrayList
   // which would otherwise be unavailable at runtime, because of type erasure
   @Target(ElementType.FIELD)
   @Retention(RetentionPolicy.RUNTIME)
   public @interface ListTypeInfo {
      Class value();
   }
   
   @Target(ElementType.FIELD)
   @Retention(RetentionPolicy.RUNTIME)
   public @interface MapTypeInfo {
      Class keyType();
      Class valueType();
   }
   
   PlayerObjects playerObjects;

   public ReflectionHelper( PlayerObjects playerObjects ) {
      this.playerObjects = playerObjects;
     // loadConfig();
   }

   public void loadObjectFromFile( String filepath, T object ) {
      try {
         if( new File(filepath ).exists() ) {
            Document document = XmlHelper.OpenDom( filepath );
            Element properties = XmlHelper.SelectSingleElement( document.getDocumentElement(), 
            "properties" );
            for( Field field : object.getClass().getDeclaredFields() ) {
               if( !field.getName().contains("playerObjects" ) ) {
                  Element property = XmlHelper.SelectSingleElement( document.getDocumentElement(), 
                        "properties/property[@name='" + field.getName() + "']" );  
                  if( property != null ) {
                     playerObjects.getLogFile().WriteLine( "reading hughai.config property " + field.getName() );
                     //                     String stringValue = property.getAttribute( "value" );
                     Object value = elementToFieldValue( field.getType(), property );
                     //                     Object value = stringTofieldValue( field.getType(), stringValue );
                     field.set( object, value );
                  }
               }
            }
         } else {
            playerObjects.getLogFile().WriteLine( "No file " + filepath +  " found." );
            playerObjects.getLogFile().WriteLine( "Creating empty base one... " );
         }
         validate();
         saveObjectToFile( filepath, object );
      } catch( Exception e ) {
         e.printStackTrace();
         throw new RuntimeException( e );
      }
   }

   void validate() {
   }

   public void saveObjectToFile( String filepath, T object ) {
      try {
         Document document = XmlHelper.CreateDom();
         Element properties = XmlHelper.AddChild( document.getDocumentElement(), "properties" );
         for( Field field : object.getClass().getDeclaredFields() ) {
            if( !field.getName().contains("playerObjects" ) ) {
               playerObjects.getLogFile().WriteLine( "writing hughai.config property " + field.getName() );
               Object value = field.get( object );
               //String valueasstring = fieldValueToString( field.getType(), value  );
               Element property = XmlHelper.AddChild( properties, "property" );
               property.setAttribute("name", field.getName() );
               //property.setAttribute("value", valueasstring );
               addFieldValueToElement( property, field.getType(), value );
            }
         }

         XmlHelper.SaveDom( document, filepath );
      } catch( Exception e ) {
         e.printStackTrace();
         throw new RuntimeException( e );
      }
   }

   String primitiveToString( Class fieldclass, Object value ) {
      //Class fieldclass = value.getClass();
      if( fieldclass == String.class ) {
         return (String)value;
      }
      if( fieldclass == boolean.class ) {
         if( (Boolean)value ){
            return "yes";
         } else {
            return "no";
         }
      }
      if( fieldclass == int.class ) {
         return "" + value;
      }
      if( fieldclass == float.class ) {
         return "" + value;
      }
      throw new RuntimeException("Config.primitiveToString: unknown field class: " + fieldclass.getName() );      
   }

   void addFieldValueToElement( Element element, Class fieldclass, Object value ) {
      if( fieldclass == String.class ) {
         element.setAttribute("value", primitiveToString( fieldclass, value ) );
         return;
      }
      if( fieldclass == boolean.class ) {
         element.setAttribute("value", primitiveToString( fieldclass, value ) );
         return;
      }
      if( fieldclass == int.class ) {
         element.setAttribute("value", primitiveToString( fieldclass, value ) );
         return;
      }
      if( fieldclass == float.class ) {
         element.setAttribute("value", primitiveToString( fieldclass, value ) );
         return;
      }
      if( List.class.isAssignableFrom( fieldclass ) ) {
         List thislist = (List)value;
         for( Object item : thislist ) {
            String thisvalue = primitiveToString( String.class, item );
            Element child = XmlHelper.AddChild( element, "value" );
            child.setAttribute( "value", thisvalue );
         }
         return;
      }
      throw new RuntimeException("addFieldValueToElement: unknown field class: " + fieldclass.getName() );
   }

   Object elementToFieldValue( Class<?> fieldclass, Element element ) {
      try {
         if( fieldclass == String.class ) {
            return stringTofieldValue( fieldclass, element.getAttribute("value") );
         }
         if( fieldclass == boolean.class ) {
            return stringTofieldValue( fieldclass, element.getAttribute("value") );
         }
         if( fieldclass == int.class ) {
            return stringTofieldValue( fieldclass, element.getAttribute("value") );
         }
         if( fieldclass == float.class ) {
            return stringTofieldValue( fieldclass, element.getAttribute("value") );
         }
         if( List.class.isAssignableFrom( fieldclass ) ) {
            //List<String> thislist = (List<String>)fieldclass.newInstance();
            List<String> thislist =  new ArrayList<String>();
            for( Element valueelement : XmlHelper.SelectElements( element, "value" ) ) {
               thislist.add( valueelement.getAttribute("value") ); // string only
               // for now, because of type erasure
               playerObjects.getLogFile().WriteLine( " list element " + valueelement.getAttribute("value") );
            }
            return thislist;
         }
      } catch( Exception e ) {
         e.printStackTrace();
         throw new RuntimeException( e );
      }
      throw new RuntimeException("elementToFieldValue: unknown field class: " + fieldclass.getName() );
   }

   Object stringTofieldValue( Class fieldclass, String stringValue ) {
      if( fieldclass == String.class ) {
         return stringValue;
      }
      if( fieldclass == boolean.class ) {
         stringValue = stringValue.toLowerCase();
         if( stringValue.equals( "yes" ) || stringValue.equals("true") ) {
            return true;
         }
         return false;
      }
      if( fieldclass == int.class ) {
         return Integer.parseInt( stringValue );
      }
      if( fieldclass == float.class ) {
         return Float.parseFloat( stringValue );
      }
      throw new RuntimeException("Config.stringTofieldValue: unknown field class: " + fieldclass.getName() );
   }
}
