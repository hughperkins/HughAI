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
      Class<?>value();
   }
   
   @Target(ElementType.FIELD)
   @Retention(RetentionPolicy.RUNTIME)
   public @interface MapTypeInfo {
      Class<?> keyType();
      Class<?> valueType();
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
         playerObjects.getLogFile().WriteLine( Formatting.exceptionToStackTrace( e ) );
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
//            Element property = XmlHelper.SelectSingleElement( element, 
//                  "property[@name='" + field.getName() + "']" );  
//            if( property != null ) {
               writeDebugLine( "reading " + objectname + " property " + field.getName() );
               //                     String stringValue = property.getAttribute( "value" );
               for( Annotation annotation : field.getAnnotations() ) {
                  writeDebugLine( "- annotation: " + annotation );
               }
               Object value = elementToFieldValue( true, field.getName(), field.getType(), field.getAnnotations(), element );
               //                     Object value = stringTofieldValue( field.getType(), stringValue );
               if( value != null ) { // prevents exceptions on primitives which don't exist
                                     // it's not great like this, but it kind of works
                  field.set( object, value );
               }
//            }
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
         playerObjects.getLogFile().WriteLine( Formatting.exceptionToStackTrace( e ) );
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
//      debug( message );
   }
   
   void debug( Object message ) {
      playerObjects.getLogFile().WriteLine( "" + this.getClass().getSimpleName() + ": " + message );
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
//            Element childelement = XmlHelper.AddChild( parentelement, "property" );
//            childelement.setAttribute("name", field.getName() );
            //property.setAttribute("value", valueasstring );
            writeDebugLine( "SaveObjectToelement " + field.getName() + " annotations: " + field.getAnnotations().length );
            addFieldValueToElement( true, field.getName(), parentelement, field.getType(),
                 field.getAnnotations(), value );
         }
      }
      indent -= 3;
      writeDebugLine( "} // SaveObjectToelement " + objectname );
   }
   
   int indent = 0;

   String primitiveToString( Class<?> fieldclass, Object value ) {
      //Class fieldclass = value.getClass();
      if( fieldclass == String.class ) {
         return (String)value;
      }
      if( fieldclass == boolean.class || fieldclass == Boolean.class ) {
         if( (Boolean)value ){
            return "yes";
         } else {
            return "no";
         }
      }
      if( fieldclass == int.class || fieldclass == Integer.class ) {
         return "" + value;
      }
      if( fieldclass == float.class || fieldclass == Float.class ) {
         return "" + value;
      }
      throw new RuntimeException("Config.primitiveToString: unknown field class: " + fieldclass.getName() );      
   }

   void addFieldValueToElement ( boolean addsubelementforobjects, String fieldname, Element element, Class<?> fieldclass, Annotation[] annotations, Object value ) throws Exception {
      writeDebugLine( "addFieldValueToElement " + value  + " " + fieldclass.getSimpleName() );
      if( fieldclass == String.class ) {
         String primitiveasstring = primitiveToString( fieldclass, value );
         writeDebugLine( "setting attribute " + fieldname + " to " + primitiveasstring );
         element.setAttribute(fieldname, primitiveasstring );
         return;
      }
      if( fieldclass == boolean.class || fieldclass == Boolean.class ) {
         element.setAttribute(fieldname, primitiveToString( fieldclass, value ) );
         return;
      }
      if( fieldclass == int.class || fieldclass == Integer.class ) {
         element.setAttribute(fieldname, primitiveToString( fieldclass, value ) );
         return;
      }
      if( fieldclass == float.class || fieldclass == Float.class ) {
         element.setAttribute(fieldname, primitiveToString( fieldclass, value ) );
         return;
      }
      if( List.class.isAssignableFrom( fieldclass ) ) {
         List<?> thislist = (List<?>)value;
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
            Class<?> valueType = listTypeInfo.value();
//         }
         Element listelement = XmlHelper.AddChild( element, "list" );
         listelement.setAttribute("fieldname", fieldname );
         if( value == null ) {
            listelement.setAttribute("null","null");
            return;
         }
         for( Object item : thislist ) {
//            ListTypeInfo listTypeInfo = field.getAnnotation( ListTypeInfo.class );
//            Class valueType = String.class; // if not annotated, assume it's a String
            //String thisvalue = primitiveToString( valueType, item );
            Element child = XmlHelper.AddChild( listelement, "listitem" );
            //child.setAttribute( "value", thisvalue );
            ArrayList<Annotation> childAnnotations = new ArrayList<Annotation>();
            if( customclassannotation != null ) {
               childAnnotations.add( customclassannotation );
            }
            addFieldValueToElement( false, "value", child, valueType, childAnnotations.toArray( new Annotation[0] ), item );
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
         if( addsubelementforobjects ) {
            element = XmlHelper.AddChild( element, "object" );
            element.setAttribute( "fieldname", fieldname );
         }         
         saveObjectToElement( element, value );
         return;
      }
      
      throw new RuntimeException("addFieldValueToElement: unknown field class: " + fieldclass.getName() );
   }

   Object elementToFieldValue( boolean addsubelementforobjects, String fieldname, Class<?> fieldclass, Annotation[] annotations, Element element ) {
      try {
         if( fieldclass == String.class ) {
            if( !element.hasAttribute( fieldname )) {
               return null;
            }
            return stringTofieldValue( fieldclass, element.getAttribute(fieldname) );
         }
         if( fieldclass == boolean.class || fieldclass == Boolean.class ) {
            if( !element.hasAttribute( fieldname )) {
               return null;
            }
            return stringTofieldValue( fieldclass, element.getAttribute(fieldname) );
         }
         if( fieldclass == int.class || fieldclass == Integer.class ) {
            if( !element.hasAttribute( fieldname )) {
               return null;
            }
            return stringTofieldValue( fieldclass, element.getAttribute(fieldname) );
         }
         if( fieldclass == float.class || fieldclass == Float.class ) {
            if( !element.hasAttribute( fieldname )) {
               return null;
            }
            return stringTofieldValue( fieldclass, element.getAttribute(fieldname) );
         }
         if( List.class.isAssignableFrom( fieldclass ) ) {
            Element listelement = XmlHelper.SelectSingleElement( element, "list[@fieldname='" + fieldname + "']" );
            if( listelement == null ) {
               return null;
            }
            if( listelement.getAttribute("null").equals("null") ) {
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
            Class<?> valueType = listTypeInfo.value();
//            if( listTypeInfo != null ) {
//               valueType = listTypeInfo.value();
//            }
            List<Object> thislist =  new ArrayList<Object>();
            for( Element valueelement : XmlHelper.SelectElements( listelement, "listitem" ) ) {
               ArrayList<Annotation> childAnnotations = new ArrayList<Annotation>();
               if( customclassannotation != null ) {
                  childAnnotations.add( customclassannotation );
               }
               Object listitem = elementToFieldValue( false, "value", valueType, childAnnotations.toArray( new Annotation[0] ), valueelement );
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
            if( addsubelementforobjects ) {
               element = XmlHelper.SelectSingleElement( element, "object[@fieldname='" + fieldname + "']" );
            }
            
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
         playerObjects.getLogFile().WriteLine( Formatting.exceptionToStackTrace( e ) );
         throw new RuntimeException( e );
      }
      throw new RuntimeException("elementToFieldValue: unknown field class: " + fieldclass.getName() );
   }

   public Object stringTofieldValue( Class<?> fieldclass, String stringValue ) {
      if( fieldclass == String.class ) {
         return stringValue;
      }
      if( fieldclass == boolean.class || fieldclass == Boolean.class ) {
         stringValue = stringValue.toLowerCase();
         if( stringValue.equals( "yes" ) || stringValue.equals("true") ) {
            return true;
         }
         return false;
      }
      if( fieldclass == int.class || fieldclass == Integer.class ) {
         return Integer.parseInt( stringValue );
      }
      if( fieldclass == float.class || fieldclass == Float.class ) {
         return Float.parseFloat( stringValue );
      }
      throw new RuntimeException("Config.stringTofieldValue: unknown field class: " + fieldclass.getName() );
   }
   
   // note: don't copy anything with Exclude annotation set
   // destination is NOT reallocated
   public void deepCopy( Object source, Object destination ) {
      deepCopy(source, destination, true );
   }
   
   // note: don't copy anything with Exclude annotation set
   // destination is NOT reallocated
   public void deepCopy( Object source, Object destination, boolean copynulls ) {
      try {
         for( Field field : source.getClass().getDeclaredFields() ) {
            Exclude exclude = field.getAnnotation( Exclude.class );
            if( exclude == null ) { // if it's not null, we've excluded it
               Object sourcevalue = field.get( source );
               if( sourcevalue == null ) {
                  if( copynulls ) {
                     field.set( destination, null );
                  } // else dont copy the null
               } else {
//                  if( !copynulls ) {
                     debug("copying field " + field.getName() );
//                  }
                  field.set( destination, deepCopyGeneric( sourcevalue ) );
               }
            }
         }
      } catch( Exception e) {
         playerObjects.getLogFile().writeStackTrace( e );
         throw new RuntimeException( e );
      }
   }
   
   // generic deepcopy routine, from http://javatechniques.com/blog/faster-deep-copies-of-java-objects/
   // this will allocate a brand new object for the destination
   Object deepCopyGeneric( Object source ) {
      try {
         ByteArrayOutputStream bos = new ByteArrayOutputStream();
         ObjectOutputStream out = new ObjectOutputStream(bos);
         out.writeObject(source);
         out.flush();
         out.close();
   
         ObjectInputStream in = new ObjectInputStream(
             new ByteArrayInputStream(bos.toByteArray()));
         return in.readObject();         
      } catch( Exception e ) {
         playerObjects.getLogFile().writeStackTrace( e );
         throw new RuntimeException( e );
      }
   }
   
   // don't copy anything across that is null
   // note: don't copy anything with Exclude annotation set
   public void deepCopyNonNullOnly( Object source, Object destination ) {
      deepCopy(source, destination, false );
   }
}
