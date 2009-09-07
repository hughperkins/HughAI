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

import com.springrts.ai.*;
import com.springrts.ai.oo.*;

import hughai.*;
import hughai.basictypes.*;
import hughai.utils.*;

// this class handles reading/writing objects to/from xml
// it can handle lists and maps
// maybe there is a function built into java for this, but there
// was for .net too, and it was both highly complicated and
// insufficiently flexible, so I wrote my own
//
//for now, this handles the following types:
//- String
//- boolean primitive
//- int primitive
//- float primitive
//- ArrayList<>
//- custom classes (NOT Unit, UnitDef, AIFloat3, or anything
//  that comes from the Java Interface ;-) )
//
//It's fairly easy to add other primitive types, so just ask if you need
//We could add a HashMap in the future fairlyeasily if you need
//
// I don't really want to add double, since I'm trying to avoid
// using doubles
//
//Important:
//
//- if you use a customclass, you must annotate with @CustomClass
//  => This just makes debugging easier, since any other class
//  is assumed to be an error
//
//- if you use a List, you must add a @ReflectionHelper.ListTypeInfo annotation
//just before the list declaration.
//See reconnaissanceunitnnames for an example
//That's because Java uses type erasure for generics, which means
//that the generic collection types are not available for reflection
//at runtime.  It's not that big a deal with annotations now available.
//
// You cna exclude a field by annotating with @Exclude
public class ReflectionHelper {
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
   
   @Target(ElementType.FIELD)
   @Retention(RetentionPolicy.RUNTIME)
   public @interface CustomClass {
   }
   
   // put this on any field you don't want to save or restore to/from xml
   @Target(ElementType.FIELD)
   @Retention(RetentionPolicy.RUNTIME)
   public @interface Exclude {
   }   
   
   PlayerObjects playerObjects;

   public ReflectionHelper( PlayerObjects playerObjects ) {
      this.playerObjects = playerObjects;
     // loadConfig();
   }

   public void loadObjectFromFile( String filepath, Object object ) {
      try {
         Document document = XmlHelper.OpenDom( filepath );
         loadObjectFromElement( document.getDocumentElement(), object );
         //         validate();
//         saveObjectToFile( filepath, object );
      } catch( Exception e ) {
         e.printStackTrace();
         throw new RuntimeException( e );
      }
   }
   
   public void loadObjectFromElement( Element element, Object object ) throws Exception {
      String objectname = object.getClass().getSimpleName();
      writeDebugLine( "loadObjectFromElement " + objectname + " {" );
      indent += 3;
      for( Field field : object.getClass().getDeclaredFields() ) {
         Exclude exclude = field.getAnnotation( Exclude.class );
         if( exclude == null ) { // if it's not null, we've excluded it
            Element property = XmlHelper.SelectSingleElement( element, 
                  "property[@name='" + field.getName() + "']" );  
            if( property != null ) {
               writeDebugLine( "reading " + objectname + " property " + field.getName() );
               //                     String stringValue = property.getAttribute( "value" );
               for( Annotation annotation : field.getAnnotations() ) {
                  writeDebugLine( "- annotation: " + annotation );
               }
               Object value = elementToFieldValue( field.getType(), field.getAnnotations(), property );
               //                     Object value = stringTofieldValue( field.getType(), stringValue );
               field.set( object, value );
            }
         }
      }      
      indent -= 3;
      writeDebugLine( "} // loadObjectFromElement " + objectname );
   }

   void validate() {
   }

   public void saveObjectToFile( String filepath, Object object ) {
      try {
         Document document = XmlHelper.CreateDom();
         Element objectElement = document.getDocumentElement();
         //Element objectElement = XmlHelper.AddChild( document.getDocumentElement(), "object" );
         writeDebugLine("Saving " + object.getClass().getSimpleName()
               + " to " + filepath );
         saveObjectToElement( objectElement, object );
         XmlHelper.SaveDom( document, filepath );
      } catch( Exception e ) {
         e.printStackTrace();
         throw new RuntimeException( e );
      }
   }
   
   String padLeft( String instring, String padString, int paddedlength ) {
      while( instring.length() < paddedlength ) {
         instring = padString + instring; 
      }
      return instring;
   }
   
   void writeDebugLine( String message ) {
      // uncomment for debugging, you'll need it most likely ;-) :-D
      playerObjects.getLogFile().WriteLine( padLeft( "", " ", indent ) + message );
   }
   
   public void saveObjectToElement ( Element parentelement, Object object ) throws Exception {

      if( object == null ) {
         parentelement.setAttribute("null", "null");
         return;
      }
      String objectname = object.getClass().getSimpleName();
      writeDebugLine( "SaveObjectToelement " + objectname + " {" );
      indent += 3;
      for( Field field : object.getClass().getDeclaredFields() ) {
         Exclude exclude = field.getAnnotation( Exclude.class );
         if( exclude == null ) { // if it's not null, we've excluded it
            writeDebugLine( "writing " + objectname + " property " + field.getName() );
            Object value = field.get( object );
            //String valueasstring = fieldValueToString( field.getType(), value  );
            Element childelement = XmlHelper.AddChild( parentelement, "property" );
            childelement.setAttribute("name", field.getName() );
            //property.setAttribute("value", valueasstring );
            writeDebugLine( "SaveObjectToelement " + field.getName() + " annotations: " + field.getAnnotations().length );
            addFieldValueToElement( childelement, field.getType(),
                 field.getAnnotations(), value );
         }
      }
      indent -= 3;
      writeDebugLine( "} // SaveObjectToelement " + objectname );
   }
   
   int indent = 0;

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

   void addFieldValueToElement ( Element element, Class fieldclass, Annotation[] annotations, Object value ) throws Exception {
      writeDebugLine( "addFieldValueToElement " + value  + " " + fieldclass.getSimpleName() );
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
         if( value == null ) {
            element.setAttribute("null","null");
            return;
         }
         List thislist = (List)value;
         ListTypeInfo listTypeInfo = null;
         CustomClass customclassannotation = null;
         for( Annotation annotation : annotations ) {
            if( ListTypeInfo.class.isAssignableFrom( annotation.getClass() ) ) {
               listTypeInfo = (ListTypeInfo)annotation;
            }
            if( CustomClass.class.isAssignableFrom( annotation.getClass() ) ) {
               customclassannotation = (CustomClass)annotation;
            }
         }
         if( listTypeInfo == null ) {
            throw new RuntimeException("Reflectionhelper: encountered ArrayList without a ListTypeInfo annotation." );
         }
         writeDebugLine( "List type: " + listTypeInfo.value().getSimpleName() );
//         if( listTypeInfo != null ) {
            Class valueType = listTypeInfo.value();
//         }
         for( Object item : thislist ) {
//            ListTypeInfo listTypeInfo = field.getAnnotation( ListTypeInfo.class );
//            Class valueType = String.class; // if not annotated, assume it's a String
            //String thisvalue = primitiveToString( valueType, item );
            Element child = XmlHelper.AddChild( element, "listvalue" );
            //child.setAttribute( "value", thisvalue );
            ArrayList<Annotation> childAnnotations = new ArrayList<Annotation>();
            if( customclassannotation != null ) {
               childAnnotations.add( customclassannotation );
            }
            addFieldValueToElement( child, valueType, childAnnotations.toArray( new Annotation[0] ), item );
         }
         return;
      }
      if( java.util.Map.class.isAssignableFrom( fieldclass ) ) {
         throw new RuntimeException( "ReflectionHelper: maps not handled yet." );
      }
      if( Unit.class.isAssignableFrom( fieldclass ) ) {
         throw new RuntimeException( "ReflectionHelper: Units deliberately not handled ;-) ." );
      }
      if( UnitDef.class.isAssignableFrom( fieldclass ) ) {
         throw new RuntimeException( "ReflectionHelper: UnitDefs deliberately not handled ;-) ." );
      }
      if( AIFloat3.class.isAssignableFrom( fieldclass ) ) {
         throw new RuntimeException( "ReflectionHelper: AIFloat3s deliberately not handled ;-) ." );
      }
      // otherwise, assume we have some custom class we want to handle
      // and attempt...
//      Element child = XmlHelper.AddChild( element, "object" );
      // check for CustomClass attribute
      boolean foundCustomClassAttribute = false;
      for( Annotation annotation : annotations ) {
         if( CustomClass.class.isAssignableFrom( annotation.getClass() ) ) {
            foundCustomClassAttribute = true;
         }
      }
      if( foundCustomClassAttribute ) {
         saveObjectToElement( element, value );
         return;
      }
      
      throw new RuntimeException("addFieldValueToElement: unknown field class: " + fieldclass.getName() );
   }

   Object elementToFieldValue( Class<?> fieldclass, Annotation[] annotations, Element element ) {
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
            if( element.getAttribute("null").equals("null") ) {
               writeDebugLine( "ReflectionHelper.elementToFieldValue object field is null." );
               return null;
            }

            ListTypeInfo listTypeInfo = null;
            CustomClass customclassannotation = null;
            for( Annotation annotation : annotations ) {
               if( ListTypeInfo.class.isAssignableFrom( annotation.getClass() ) ) {
                  listTypeInfo = (ListTypeInfo)annotation;
               }
               if( CustomClass.class.isAssignableFrom( annotation.getClass() ) ) {
                  customclassannotation = (CustomClass)annotation;
               }
            }
//            Class valueType = String.class; // if not annotated, assume it's a String
            if( listTypeInfo == null ) {
               throw new RuntimeException("ReflectionHelper: error: arraylist used without ListTypeInfo annotation type." );
            }
            Class valueType = listTypeInfo.value();
//            if( listTypeInfo != null ) {
//               valueType = listTypeInfo.value();
//            }
            List thislist =  new ArrayList();
            for( Element valueelement : XmlHelper.SelectElements( element, "listvalue" ) ) {
               ArrayList<Annotation> childAnnotations = new ArrayList<Annotation>();
               if( customclassannotation != null ) {
                  childAnnotations.add( customclassannotation );
               }
               Object listitem = elementToFieldValue( valueType, childAnnotations.toArray( new Annotation[0] ), valueelement );
               thislist.add( listitem );
               writeDebugLine( " list element " + valueelement.getAttribute("value") );
            }
            return thislist;
         }
         if( java.util.Map.class.isAssignableFrom( fieldclass ) ) {
            throw new RuntimeException( "ReflectionHelper: maps not handled yet." );
         }
         if( Unit.class.isAssignableFrom( fieldclass ) ) {
            throw new RuntimeException( "ReflectionHelper: Units deliberately not handled ;-) ." );
         }
         if( UnitDef.class.isAssignableFrom( fieldclass ) ) {
            throw new RuntimeException( "ReflectionHelper: UnitDefs deliberately not handled ;-) ." );
         }
         if( AIFloat3.class.isAssignableFrom( fieldclass ) ) {
            throw new RuntimeException( "ReflectionHelper: AIFloat3s deliberately not handled ;-) ." );
         }


         // check for CustomClass attribute
         boolean foundCustomClassAttribute = false;
         for( Annotation annotation : annotations ) {
            if( CustomClass.class.isAssignableFrom( annotation.getClass() ) ) {
               foundCustomClassAttribute = true;
            }
         }
         if( foundCustomClassAttribute ) {
            if( element.getAttribute("null").equals("null") ) {
               writeDebugLine( "ReflectionHelper.elementToFieldValue object field is null." );
               return null;
            }
            writeDebugLine( "ReflectionHelper.elementToFieldValue making new child object..." );
            Object childobject = fieldclass.newInstance();
            writeDebugLine( "New child object: " + childobject + " " + childobject.getClass() );
            loadObjectFromElement( element, childobject );
            return childobject;
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
