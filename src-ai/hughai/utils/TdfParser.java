// Copyright Hugh Perkins 2006, 2009
//hughperkins@gmail.com http://manageddreams.com
//
//This program is free software; you can redistribute it and/or modify it
//under the terms of the GNU General Public License as published by the
//Free Software Foundation; either version 2 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful, but
//WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
//or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
//more details.
//
//You should have received a copy of the GNU General Public License along
//with this program in the file licence.txt; if not, write to the
//Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-
//1307 USA
//You can find the licence also on the web at:
//http://www.opensource.org/licenses/gpl-license.php
//

package hughai.utils;

import java.util.*;
import java.io.*;

import com.springrts.ai.oo.*;

import hughai.*;
import hughai.utils.*;

// note: treats file as UTF8.
// note: section and key names are key insensitive.
//
// example usage:
//   String startscriptcontents = playerObjects.getAicallback().getGame().getSetupScript();
//   TdfParser tdfParser = new TdfParser( playerObjects, startscriptcontents );
//   this.side = tdfParser.RootSection.GetStringValue( "GAME/TEAM" + ourteamnumber + "/Side" );
public class TdfParser
{
   PlayerObjects playerObjects;

   public String rawtdf = "";
   //   public TdfParser()
   //   {
   //   }
   public TdfParser( PlayerObjects playerObjects, String tdfcontents)
   {
      this.playerObjects = playerObjects;

      this.rawtdf = tdfcontents;
      Parse();
   }
   
   void debug( Object message ) {
//      playerObjects.getLogFile().WriteLine( "TdfParser: " + message );
   }

   public static TdfParser FromFile( PlayerObjects playerObjects, String filename)
   {
      String rawtdf = new FileHelper( playerObjects ).readFile( filename );
      return new TdfParser(playerObjects, rawtdf);
   }
   //   public static TdfParser FromBuffer(byte[] data, int size)
   //   {
   //      return new TdfParser( Encoding.UTF8.GetString(data, 0, size) );
   //   }

   String[] splitrawtdf;

   void GenerateSplitArray()
   {
      splitrawtdf = rawtdf.split("\n");
      for (int i = 0; i < splitrawtdf.length; i++)
      {
         splitrawtdf[i] = splitrawtdf[i].trim();
      }
   }

   enum State
   {
      InSectionHeader,
      NormalParse
   }

   State currentstate;
   Section currentsection;
   public Section RootSection;
   int level;

   public class Section
   {
      public String Name;
      public Section Parent = null;
      public HashMap<String, Section> SubSections = new HashMap<String, Section>(); // section by sectionname
      public HashMap<String, String> Values = new HashMap<String, String>(); // we dont bother parsing the values, that's done by the relevant parse
      // the reason is that only the client knows what type the variable actually is
      // ( unless we supply a DTD or equivalent )

      public Section SubSection(String name)
      {
         try
         {
            return GetSectionByPath(name);
         }
         catch( Exception e )
         {
//            e.printStackTrace();
            return null;
         }
      }

      public Section()
      {
      }
      public Section(String name)
      {
         this.Name = name;
      }
      public double GetDoubleValue(String name)
      {
         return GetDoubleValue(0, name);
      }
      public int GetIntValue(String name)
      {
         return GetIntValue(0, name);
      }
      public String GetStringValue(String name)
      {
         return GetStringValue("", name);
      }
      public double[] GetDoubleArray(String name)
      {
         return GetDoubleArray(new double[] { }, name);
      }
      public double GetDoubleValue(double defaultvalue, String name)
      {
         try
         {
            String stringvalue = GetValueByPath(name);
            return Double.parseDouble( stringvalue);
         }
         catch( Exception e )
         {
            return defaultvalue;
         }
      }
      public int GetIntValue(int defaultvalue, String name)
      {
         try
         {
            String stringvalue = GetValueByPath(name);
            return Integer.parseInt( stringvalue);
         }
         catch( Exception e )
         {
            return defaultvalue;
         }
      }
      public String GetStringValue(String defaultvalue, String name)
      {
         try
         {
            return GetValueByPath(name);
         }
         catch( Exception e )
         {
            e.printStackTrace();
            return defaultvalue;
         }
      }
      public double[] GetDoubleArray(double[] defaultvalue, String name)
      {
         try
         {
            String stringvalue = GetValueByPath(name);
            String[] splitvalue = stringvalue.trim().split(" " );
            int length = splitvalue.length;
            double[] values = new double[length];
            for (int i = 0; i < length; i++)
            {
               values[i] = Double.parseDouble( splitvalue[i]);
            }
            return values;
         }
         catch( Exception e )
         {
            return defaultvalue;
         }
      }
      List<String> GetPathParts(String path)
      {
         String[] splitpath = path.split("/");
         List<String> pathparts = new ArrayList<String>();
         debug("GetPathParts" );
         for( String subpath : splitpath )
         {
            debug("  subpath " + subpath );
            String[] splitsubpath = subpath.trim().split("\\\\");
            for( String subsubpath : splitsubpath )
            {
               debug("  subsubpath " + subsubpath );
               pathparts.add(subsubpath.trim().toLowerCase());
            }
         }
         return pathparts;
      }
      Section GetSectionByPath(String path)
      {
         debug("GetSectionByPath " + path );
         List<String> pathparts = GetPathParts(path);
         Section thissection = this;
         // we're just going to walk, letting exception fly if they want
         // this is not a public function, and we'll catch the exception in public function
         for (int i = 0; i < pathparts.size(); i++)
         {
            debug( "   pathpart: " + pathparts.get(i));
            thissection = thissection.SubSections.get( pathparts.get( i ) );
         }
         return thissection;
      }
      String GetValueByPath(String path)
      {
         List<String> pathparts = GetPathParts(path);
         Section thissection = this;
         // we're just going to walk, letting exception fly if they want
         // this is not a public function, and we'll catch the exception in public function
         for (int i = 0; i < pathparts.size() - 1; i++)
         {
            thissection = thissection.SubSections.get( pathparts.get( i ) );
         }
         return thissection.Values.get( pathparts.get( pathparts.size() - 1 ) );
      }
   }
   int ParseLine( int linenum, String line)
   {
      debug("ParseLine " + linenum + " " + line);
      switch (currentstate)
      {
      case NormalParse:
      {
         if (line.indexOf("[") == 0)
         {
            currentstate = State.InSectionHeader;
            String sectionname = ( (line.substring(1) + "]").split("]")[0] ).toLowerCase();
            Section subsection = new Section( sectionname );
            subsection.Parent = currentsection;
            if (!currentsection.SubSections.containsKey(sectionname))
            {
               currentsection.SubSections.put(sectionname, subsection);
            }
            else
            {
               // silently ignore
            }
            debug("section header found: " + sectionname );
            currentsection = subsection;
         }
         else if( line.indexOf("}") == 0 )
         {
            level--;
            debug("section } found, new level:" + level);
            if (currentsection.Parent != null)
            {
               currentsection = currentsection.Parent;
            }
            else
            {
               // silently ignore
            }
         }
         else if( line != "" )
         {
            if (line.indexOf("//") != 0 && line.indexOf("/*") != 0)
            {
               int equalspos = line.indexOf("=");
               if (equalspos >= 0)
               {
                  String valuename = line.substring(0, equalspos).toLowerCase();
                  String value = line.substring(equalspos + 1);
//                  debug("   line: " + line );
                  value = (value + ";").split(";", -1)[0]; // remove trailing ";"
                  debug("   value found [" + valuename + "] = [" + value + "]");
                  if (!currentsection.Values.containsKey(valuename))
                  {
                     currentsection.Values.put(valuename, value);
                  }
               }
            }
         }
         break;
      }

      case InSectionHeader:
      {
         if (line.indexOf("{") == 0)
         {
            currentstate = State.NormalParse;
            level++;
            debug("section { found, new level:" + level);
         }
         break;
      }
      }
      return linenum;
   }

   void Parse()
   {
      GenerateSplitArray();
      RootSection = new Section(); ;
      level = 0;
      currentsection = RootSection;
      currentstate = State.NormalParse;
      for (int linenum = 0; linenum < splitrawtdf.length; linenum++)
      {
         linenum = ParseLine( linenum, splitrawtdf[linenum]);
      }
   }
}
