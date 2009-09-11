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
import java.util.List;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import javax.activation.FileTypeMap;
import javax.naming.Context;
import javax.swing.*;
import javax.script.*;
import javax.xml.transform.stream.StreamResult;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;

import hughai.*;
import hughai.loader.utils.Loader;
import hughai.utils.Formatting;
import hughai.utils.LogFile;

// provides a console tab to the gui that lets us execute code
// on the fly!
// is that wicked or wot! :-D
public class ConsoleEcma {
   final String consoletemplatefilename = "EcmaConsoleTemplate.txt";
   
//   final String classpath="$aidir/SkirmishAI.jar:$aidir/UnderlyingAI.jar:$aidir/../../Interfaces/Java/0.1/AIInterface.jar"; 

//   JFrame frame;
   JPanel textpanel;
   GridLayout gridLayout;
   JTextArea textarea;
   JButton gobutton;

   JTextArea outputTextarea;
   
   PlayerObjects playerObjects;
   
   public ConsoleEcma( PlayerObjects playerObjects ) {
      this.playerObjects = playerObjects;
      
      init();
   }
   
   void init () {
      try {
//      frame = new JFrame("Console");
//      frame.setSize( 400, 400 );
         
         JPanel outerpanel = new JPanel(new BorderLayout());

      gridLayout = new GridLayout( 2, 1 );
      textpanel = new JPanel( gridLayout );
//      frame.add( panel );
      
      JPanel buttonpanel = new JPanel(new GridLayout(1,2));
      
      outerpanel.add( "Center", textpanel );
      outerpanel.add( "South", buttonpanel );

      textarea = new JTextArea();
      JScrollPane scrollPane = new JScrollPane( textarea );

      outputTextarea = new JTextArea();
      JScrollPane outputscrollpane = new JScrollPane( outputTextarea );

      gobutton = new JButton( "Go" );

      String templatepath = playerObjects.getCSAI().getAIDirectoryPath() + consoletemplatefilename;
      String initialFile = readFile( templatepath );
      if( initialFile != null ) {
         textarea.setText( initialFile );
      } else {
         textarea.setText("<Missing file " + templatepath + ">" );
      }

      gobutton.addActionListener( new GoButton() );

      textpanel.add( scrollPane );
      textpanel.add( outputscrollpane );
      buttonpanel.add( gobutton );

      playerObjects.getMainUI().addPanelToTabbedPanel( "ECMA Console", outerpanel );
//      frame.validate();
//      frame.setVisible( true );
      } catch( Exception e ) {
         playerObjects.getLogFile().WriteLine( Formatting.exceptionToStackTrace( e ) );
         throw new RuntimeException( e );
      }
   }

   // should be moved to some generic utility class...
   // returns the contents of file filename
   // or null if not found
   String readFile( String filename ) {
      try {
         FileReader fileReader = new FileReader( filename );
         BufferedReader bufferedReader = new BufferedReader( fileReader );
         StringBuilder stringBuilder = new StringBuilder();
         String line = bufferedReader.readLine();
         while( line != null ) {
            stringBuilder.append( line );
            stringBuilder.append( "\n" );
            line = bufferedReader.readLine();
         }
         bufferedReader.close();
         fileReader.close();
         return stringBuilder.toString();
      } catch( Exception e ) {
         playerObjects.getLogFile().WriteLine( Formatting.exceptionToStackTrace( e ) );
         return null;
      }
   }

   void debug( Object message ) {
      playerObjects.getLogFile().WriteLine( "" + message );
   }

   class GoButton implements ActionListener {
      @Override
      public void actionPerformed( ActionEvent event ) {
         try {
            
            ClassLoader baseClassLoader = ConsoleEcma.class.getClassLoader();
            if( baseClassLoader == null ) {
               System.out.println("using system classloader as base");
               baseClassLoader = ClassLoader.getSystemClassLoader();
            } else {
               System.out.println("using our classloader as base");         
            }
            Class<?> cls = baseClassLoader.loadClass("javax.script.ScriptEngineManager");
            if (!ScriptEngineManager.class.isAssignableFrom(cls)) {
               throw new RuntimeException("Invalid class");
            }
            Object newInstance = cls.newInstance(); 
            ScriptEngineManager scriptEngineManager = (ScriptEngineManager)newInstance;
            for( String key : scriptEngineManager.getBindings().keySet() ) {
               debug( key );
            }
            
//            ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
            List<ScriptEngineFactory> factories = scriptEngineManager.getEngineFactories();
            ScriptEngineFactory ecmaScriptEngineFactory = null;
            for( ScriptEngineFactory scriptEngineFactory : factories ) {
//               System.out.println( scriptEngineFactory.getLanguageName() );
               if( scriptEngineFactory.getLanguageName().equals("ECMAScript") 
                     && scriptEngineFactory.getLanguageVersion().equals( "1.6" ) ) {
                  ecmaScriptEngineFactory = scriptEngineFactory;
               }
            }
            ScriptEngine scriptEngine = ecmaScriptEngineFactory.getScriptEngine();
            //scriptEngine.getContext().
            //javax.script.
            debug( ConsoleEcma.class.getClassLoader() );
            debug( ScriptEngineManager.class.getClassLoader() );
            debug( ScriptEngineFactory.class.getClassLoader() );
            debug( ScriptEngine.class.getClassLoader() );
            
            scriptEngine.eval( textarea.getText() );
            Invocable invocable = (Invocable)scriptEngine;
            String result = (String)invocable.invokeFunction( "go", playerObjects, new Activator() );
            outputTextarea.setText( result );
                       
         } catch( Exception e ) {
            outputTextarea.setText( exceptionToStackTrace( e ) );
         }
      }      
   }
   
   // should go in generic tools class...
   String exceptionToStackTrace( Exception e ) {
      StringWriter stringWriter = new StringWriter();
      PrintWriter printWriter = new PrintWriter(stringWriter);
      e.printStackTrace( printWriter );
      return stringWriter.toString();
   }
}
