// Copyright Hugh Perkins 2006, 2009
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

package hughai.mapping;

import java.util.*;
import org.w3c.dom.*;
import java.io.*;

import com.springrts.ai.*;
import com.springrts.ai.oo.*;
import com.springrts.ai.oo.Map;

import hughai.basictypes.*;
import hughai.*;
import hughai.mapping.BuildMap.BuildMapPos;
import hughai.ui.MainUI;
import hughai.unitdata.*;
import hughai.utils.*;


// handles figuring out where the best metal spots are,
// and handing them out on demand
// listens for new extractors being built, and ticks those areas off
// from the list
public class Metal
{
   public final String MetalClassVersion = "0.1"; // used for detecting cache validity; if cache was built with older version we rebuild it

   public int MinMetalForSpot = 30; // from 0-255, the minimum percentage of metal a spot needs to have
   //from the maximum to be saved. Prevents crappier spots in between taken spaces.
   //They are still perfectly valid and will generate metal mind you!
   public int MaxSpots = 5000; //If more spots than that are found the map is considered a metalmap, tweak this as needed        
   //int MaxSpots = 3;
   
   public static final int granularity = 2;
   
   public static class MetalPos extends Int2 {
      public MetalPos() {
         
      }
      public MetalPos( Int2 int2 ) {
         x = int2.x;
         y = int2.y;
      }
      public MetalPos( int x, int y ) {
         super( x, y );
      }
      public TerrainPos toTerrainPos() {
         return new TerrainPos( x * 8 * granularity, 0, y * 8 * granularity );
      }
      public static MetalPos fromTerrainPos( TerrainPos terrainPos ) {
         return new MetalPos( (int)terrainPos.x / 8 / granularity,
               (int)terrainPos.z / 8 / granularity );
      }
   }

   public MetalSpot[] MetalSpots;
   public ArrayList<MetalSpot> MetalSpotsUsed = new ArrayList<MetalSpot>(); // arraylist of MetalSpots
   public boolean isMetalMap = false;

   public HashMap<Unit,Float3> Extractors = new HashMap<Unit,Float3>();

   double ExtractorRadius;

   CSAI csai;
   OOAICallback aicallback;
   LogFile logfile;
   UnitController unitcontroller;
   UnitDefHelp unitdefhelp;
   ResourceManager resourceManager;
   //	BuildMap buildMap;
   TimeHelper timeHelper;
   //	MovementMaps movementMaps;
   Maps maps;
   DrawingUtils drawingUtils;
   PlayerObjects playerObjects;

   public Metal( PlayerObjects playerObjects ) // protected constructor to force Singleton instantiation
   {
      this.playerObjects = playerObjects;
      csai = playerObjects.getCSAI();
      aicallback = csai.aicallback;
      logfile = playerObjects.getLogFile();
      unitcontroller = playerObjects.getUnitController();
      unitdefhelp = playerObjects.getUnitDefHelp();
      resourceManager = playerObjects.getResourceManager();
      //		buildMap = playerObjects.getBuildMap();
      timeHelper = playerObjects.getTimeHelper();
      //		movementMaps = playerObjects.getMovementMaps();
      maps = playerObjects.getMaps();
      drawingUtils = playerObjects.getDrawingUtils();

      ExtractorRadius = aicallback.getMap().getExtractorRadius(resourceManager.getMetalResource());

      unitcontroller.registerListener( new UnitListener());

//      csai.RegisterVoiceCommand( "showmetalspots", new DrawMetalSpotsHandler() );
      csai.RegisterVoiceCommand( "searchmetalspots", new VoiceSearchMetal() );
      
      playerObjects.getMainUI().registerButton( "Show metal spots", new ShowMetalSpotsButton() );
   }
   
   class ShowMetalSpotsButton implements MainUI.ButtonHandler {
      @Override
      public void go() {
         DrawMetalSpots();
      }
   }

   @Override
   protected void finalize() {
      System.out.println( this.getClass().getSimpleName() + ".finalize()");
   }
//   static Object lock = new Object();

   public void Init()
   {
//      synchronized( lock ) {
         if( !LoadCache() )
         {
            csai.SendTextMsg( "Metal analyzer rebuilding cachefile" );
            SearchMetalSpots();
         }
         if( !isMetalMap )
         {
            ReserveMetalExtractorSpaces();
         }
//      }
   }
   
   class VoiceSearchMetal implements VoiceCommandHandler {
      @Override
      public void commandReceived(  String chatString, String[] splitchatString, int player ) {
         csai.sendTextMessage( "searching metal spots..." );
         SearchMetalSpots();
      }
   }

   void ReserveMetalExtractorSpaces()
   {
      for( MetalSpot metalspot : MetalSpots )
      {
         //			logfile.WriteLine("reserving space for " + metalspot.Pos.toString() );
         BuildMapPos buildMapPos = BuildMapPos.fromTerrainPos( metalspot.Pos );
         maps.getBuildMap().ReserveSpace( this, buildMapPos, 6, 6 );
      }
   }

   class UnitListener extends UnitController.UnitAdapter {
      @Override
      public void UnitAdded( Unit newunit )
      {
        // logfile.WriteLine( "Metal.UnitAdded " + newunit.getDef().getHumanName() + " " + newunit.getUnitId() );
         if( !isMetalMap )
         {
            boolean ismex = unitdefhelp.IsMex( newunit.getDef() ); 
            //logfile.WriteLine( "Is in extractors? " + Extractors.containsKey( newunit ) 
             //  + " ismex? " + ismex );
            if( !Extractors.containsKey( newunit ) 
                  && ismex )
            {
               TerrainPos mexpos = unitcontroller.getPos( newunit );
               logfile.WriteLine( "Metal.UnitAdded new extractor finished, pos " + mexpos.toString() );
               Extractors.put( newunit, mexpos );
               double squareextractorradius = ExtractorRadius * ExtractorRadius;
               for( MetalSpot metalspot : MetalSpots )
               {
                  float thisdistancesquared = metalspot.Pos.GetSquaredDistance( mexpos );
                  //   logfile.WriteLine( "squareextractorradius: " + squareextractorradius + " thisdistancesquared: " + thisdistancesquared );
                  if( thisdistancesquared <= squareextractorradius )
                  {
                     MetalSpotsUsed.add( metalspot );
                     logfile.WriteLine( "Marking metal spot used: " + metalspot.Pos.toString() );
                  }
               }
            }
         }
      }

      @Override
      public void UnitRemoved( Unit removedunit )
      {
         if( !isMetalMap )
         {
            if( Extractors.containsKey( removedunit ) )
            {
               TerrainPos mexpos = unitcontroller.getPos( removedunit );
               logfile.WriteLine( "Metal.UnitRemoved, pos " + mexpos.toString() );
               double squareextractorradius = ExtractorRadius * ExtractorRadius;
               for( MetalSpot metalspot : MetalSpots )
               {
                  if( metalspot.Pos.GetSquaredDistance( mexpos ) < squareextractorradius )
                  {
                     if( MetalSpotsUsed.contains( metalspot ) )
                     {
                        logfile.WriteLine( "Marking metal spot free: " + metalspot.Pos.toString() );
                        MetalSpotsUsed.remove( metalspot );
                     }
                  }
               }
               Extractors.remove( removedunit );
            }
         }
      }
   }

   public TerrainPos GetNearestMetalSpot( TerrainPos mypos )
   {
      if( !isMetalMap )
      {
         double closestdistancesquared = 1000000000000d;
         TerrainPos bestpos = null;
         MetalSpot bestspot = null;
         for( MetalSpot metalspot : MetalSpots )
         {
            if( !MetalSpotsUsed.contains( metalspot ) )
            {
               if( bestpos == null )
               {
                  bestpos = metalspot.Pos;
               }
               float thisdistancesquared = mypos.GetSquaredDistance( metalspot.Pos );
               //logfile.WriteLine( "thisdistancesquared = " + thisdistancesquared + " closestdistancesquared= " + closestdistancesquared );
               if( thisdistancesquared < closestdistancesquared )
               {
                  closestdistancesquared = thisdistancesquared;
                  bestpos = metalspot.Pos;
                  bestspot = metalspot;
               }
            }
         }
         return bestspot.Pos;
      }
      else
      {
         return mypos; // if metal map just return passed-in pos
      }
   }

   public class DrawMetalSpotsHandler implements VoiceCommandHandler {
      @Override
      public void commandReceived( String chatString, String[] splitchatString, int player )
      {
         DrawMetalSpots();
      }
   }

   public class MetalSpot
   {
      public int Amount;
      public TerrainPos Pos = new TerrainPos();
      public boolean IsOccupied = false;

      public MetalSpot(){}
      public MetalSpot( int amount, TerrainPos pos )
      {
         Amount = amount;
         Pos = pos;
      }
      public MetalSpot( int amount, TerrainPos pos, boolean isoccupied )
      {
         Amount = amount;
         IsOccupied = isoccupied;
         Pos = pos;
      }
      @Override
      public String toString()
      {
         return "MetalSpot( Pos=" + Pos.toString() + ", Amount=" + Amount + ", IsOccupied=" + IsOccupied;
      }
   }

   // for debugging / convincing oneself spots are in right place
   void DrawMetalSpots()
   {
      if( !isMetalMap )
      {
         String  metalspotunitname = playerObjects.getConfig().getMetalspotmarkerunitname();
         String metalspotusedunit = playerObjects.getConfig().getUsedmetalspotmarkerunitname();
         for( MetalSpot metalspot : MetalSpots )
         {
            logfile.WriteLine("drawing spot at " + metalspot.Pos );
            drawingUtils.DrawUnit(metalspotunitname, metalspot.Pos, 0.0f, 1, aicallback.getTeamId(), true, true);
            drawingUtils.drawText(metalspot.Pos, "" + metalspot.Pos );
         }
         for( MetalSpot metalspot : MetalSpotsUsed )
         {
            logfile.WriteLine("drawing usedspot at " + metalspot.Pos );
            drawingUtils.DrawUnit(metalspotusedunit, metalspot.Pos, 0.0f, 1, aicallback.getTeamId(), true, true);
            drawingUtils.drawText(metalspot.Pos, "" + metalspot.Pos );
         }
      }
      else
      {
         csai.SendTextMsg( "Metal analyzer reports this is a metal map" );
      }
   }

   // loads cache file
   // returns true if cache loaded ok, otherwise false if not found, out-of-date, etc
   // we check the version and return false if out-of-date
   boolean LoadCache()
   {
      String MapName = aicallback.getMap().getName();
      String cachefilepath = csai.getCacheDirectoryPath() + File.separator + MapName + "_metal.xml";

      if( !new File( cachefilepath ).exists() )
      {
         logfile.WriteLine( "cache file doesnt exist -> building" );
         return false;
      }

      Document cachedom = XmlHelper.OpenDom( cachefilepath );
      Element metadata = XmlHelper.SelectSingleElement( cachedom.getDocumentElement(), "/root/metadata" );
      String cachemetalclassversion = metadata.getAttribute( "version" );
      if( !cachemetalclassversion.equals( MetalClassVersion ) )
      {
         logfile.WriteLine( "cache file out of date ( " + cachemetalclassversion + " vs " + MetalClassVersion + " ) -> rebuilding" );
         return false;
      }

      logfile.WriteLine( cachedom.getTextContent() );

      isMetalMap = Boolean.parseBoolean( metadata.getAttribute( "ismetalmap" ) );

      if( isMetalMap )
      {
         logfile.WriteLine( "metal map" );
         return true;
      }


      Element metalspots = XmlHelper.SelectSingleElement( cachedom.getDocumentElement(), "/root/metalspots" );
      ArrayList<MetalSpot> metalspotsal = new ArrayList<MetalSpot>();
      for( Element metalspot : XmlHelper.SelectElements( metalspots, "metalspot" ) ) {
         int amount = Integer.parseInt( metalspot.getAttribute("amount") );
         TerrainPos pos = new TerrainPos();
         pos.readFromXmlElement( metalspot );
         //pos.LoadCsv( metalspot.GetAttribute("pos") );
         MetalSpot newmetalspot = new MetalSpot( amount, pos );
         metalspotsal.add( newmetalspot );
         // logfile.WriteLine( "metalspot xml: " + metalspot.InnerXml );
         //         logfile.WriteLine( "metalspot: " + newmetalspot.toString() );
      }
      MetalSpots = (MetalSpot[])metalspotsal.toArray( new MetalSpot[0] );

      logfile.WriteLine( "metal spot cache file loaded" );
      return true;
   }

   // save cache to speed up load times
   // we store the MetalClassVersion to enable cache rebuild if algo changed
   void SaveCache()
   {
      Map map = aicallback.getMap();
      String MapName = map.getName();
      String cachefilepath = csai.getCacheDirectoryPath() + File.separator +  MapName + "_metal.xml";

      Document cachedom = XmlHelper.CreateDom();
      Element metadata = XmlHelper.AddChild( cachedom.getDocumentElement(), "metadata" );
      metadata.setAttribute( "type", "MetalCache" );
      metadata.setAttribute( "map", MapName );
      metadata.setAttribute( "version", MetalClassVersion );
      metadata.setAttribute( "datecreated", timeHelper.GetCurrentRealTimeString() );
      metadata.setAttribute( "ismetalmap", "" + isMetalMap );
      metadata.setAttribute( "extractorradius", "" + map.getExtractorRadius(resourceManager.getMetalResource()) );
      metadata.setAttribute( "mapheight", "" + map.getHeight() );
      metadata.setAttribute( "mapwidth", "" + map.getWidth() );

      Element metalspots = XmlHelper.AddChild( cachedom.getDocumentElement(), "metalspots" );
      for( MetalSpot metalspot : MetalSpots )
      {
         Element metalspotnode = XmlHelper.AddChild( metalspots, "metalspot" );
         //metalspotnode.setAttribute( "pos", metalspot.Pos.ToCsv() );
         metalspot.Pos.writeToXmlElement( metalspotnode );
         metalspotnode.setAttribute( "amount", "" + metalspot.Amount );
      }

      XmlHelper.SaveDom( cachedom, cachefilepath );
   }

   public static class AvailableMetalResult {
      public int [][] availableMetal;
      public int maxMetalAmount;
      public AvailableMetalResult( int[][] availableMetal, int maxMetalAmount ) {
         this.availableMetal = availableMetal;
         this.maxMetalAmount = maxMetalAmount;
      }
   }

   // algorithm more or less by krogothe
   // ported from Submarine's original C++ version
   AvailableMetalResult CalculateAvailableMetalForEachSpot( int[][] metalremaining, int ExtractorRadius )
   {
      int mapwidth = metalremaining.length;
      int mapheight = metalremaining[0].length;
      int SquareExtractorRadius = (int)( ExtractorRadius * ExtractorRadius );
      int [][]SpotAvailableMetal = new int[ mapwidth][ mapheight ];

      logfile.WriteLine( "mapwidth: " + mapwidth + " mapheight: " + mapheight + " ExtractorRadius: " + ExtractorRadius + " SquareExtractorRadius: " + SquareExtractorRadius );

      // Now work out how much metal each spot can make by adding up the metal from nearby spots
      // we scan each point on map, and for each point, we scan from + to - ExtractorRadius, eliminating anything where
      // the actual straight line distance is more than the ExtractorRadius
      int maxmetalspotamount = 0;
      for (int spoty = 0; spoty < mapheight; spoty++)
      {
         for (int spotx = 0; spotx < mapwidth; spotx++)
         {
            int metalthisspot = 0;
            //get the metal from all pixels around the extractor radius 
            for (int deltax = - ExtractorRadius; deltax <= ExtractorRadius; deltax++)
            {
               int thisx = spotx + deltax;
               if ( thisx >= 0 && thisx < mapwidth )
               {
                  for (int deltay = - ExtractorRadius; deltay <= ExtractorRadius; deltay++)
                  {
                     int thisy = spoty + deltay;
                     if ( thisy >= 0 && thisy < mapheight )
                     {
                        if( ( deltax * deltax + deltay * deltay ) <= SquareExtractorRadius )
                        {
                           metalthisspot += metalremaining[ thisx][ thisy ]; 
                        }
                     }
                  }
               }
            }
            SpotAvailableMetal[ spotx][ spoty ] = metalthisspot; //set that spots metal making ability (divide by cells to values are small)
            maxmetalspotamount = Math.max( metalthisspot, maxmetalspotamount ); //find the spot with the highest metal to set as the map's max
         }
      }

      //  logfile.WriteLine ("*******************************************");
      return new AvailableMetalResult( SpotAvailableMetal, maxmetalspotamount );
   }

   // algorithm more or less by krogothe
   // ported from Submarine's original C++ version
   public void SearchMetalSpots()
   {	
      logfile.WriteLine( "SearchMetalSpots() >>>");

      isMetalMap = false;

      ArrayList<MetalSpot> metalspotsal = new ArrayList<MetalSpot>();

      Map map = aicallback.getMap();
      int mapheight = map.getHeight() / granularity; //metal map has 1/2 resolution of normal map
      int mapwidth = map.getWidth() / granularity;
      double mapmaxmetal = map.getMaxResource(resourceManager.getMetalResource());
      int totalcells = mapheight * mapwidth;

      logfile.WriteLine( "mapwidth: " + mapwidth + " mapheight " + mapheight + " maxmetal:" + mapmaxmetal );

      List<Byte> metalmap = map.getResourceMapRaw(resourceManager.getMetalResource()); // original metal map
      int[][] metalremaining = new int[ mapwidth][ mapheight ];  // actual metal available at that point. we remove metal from this as we add spots to MetalSpots
      int[][] SpotAvailableMetal = new int [ mapwidth][ mapheight ]; // amount of metal an extractor on this spot could make
      int[][] NormalizedSpotAvailableMetal = new int [ mapwidth][ mapheight ]; // SpotAvailableMetal, normalized to 0-255 range

      int totalmetal = 0;
      ArrayIndexer arrayindexer = new ArrayIndexer( mapwidth, mapheight );
      //Load up the metal Values in each pixel
      logfile.WriteLine( "width: " + mapwidth + " height: " + mapheight );
      for (int y = 0; y < mapheight; y++)
      {
         //String logline = "";
         for( int x = 0; x < mapwidth; x++ )
         {
            metalremaining[ x][ y ] = (int)metalmap.get( arrayindexer.GetIndex( x, y ) );
            totalmetal += metalremaining[ x][ y ];		// Count the total metal so you can work out an average of the whole map
            //logline += metalremaining[ x, y ].toString() + " ";
            //  logline += metalremaining[ x, y ] + " ";
         }
         // logfile.WriteLine( logline );
      }
      logfile.WriteLine ("*******************************************");

      double averagemetal = ((double)totalmetal) / ((double)totalcells);  //do the average
      // int maxmetal = 0;

      int ExtractorRadius = (int)( map.getExtractorRadius(resourceManager.getMetalResource())
            / 16.0 );
      int DoubleExtractorRadius = ExtractorRadius * 2;
      int SquareExtractorRadius = ExtractorRadius * ExtractorRadius; //used to speed up loops so no recalculation needed
      int FourSquareExtractorRadius = 4 * SquareExtractorRadius; // same as above 
      double CellsInRadius = Math.PI * ExtractorRadius * ExtractorRadius;

      int maxmetalspotamount = 0;
      logfile.WriteLine( "Calculating available metal for each spot..." );
      AvailableMetalResult availableMetalResult = CalculateAvailableMetalForEachSpot( metalremaining, ExtractorRadius );
      SpotAvailableMetal = availableMetalResult.availableMetal;
      maxmetalspotamount = availableMetalResult.maxMetalAmount;

      //		logfile.WriteLine( "Normalizing..." );
      // normalize the metal so any map will have values 0-255, no matter how much metal it has
      int[][] NormalizedMetalRemaining = new int[ mapwidth][ mapheight ];
      for (int y = 0; y < mapheight; y++)
      {
         for (int x = 0; x < mapwidth; x++)
         {
            NormalizedSpotAvailableMetal[ x][ y ] = ( SpotAvailableMetal[ x][ y ] * 255 ) / maxmetalspotamount;
         }
      }

      //		logfile.WriteLine( "maxmetalspotamount: " + maxmetalspotamount );

      boolean Stopme = false;
      int SpotsFound = 0;
      //logfile.WriteLine( BuildTable.GetInstance().GetBiggestMexUnit().toString() );
      // UnitDef biggestmex = BuildTable.GetInstance().GetBiggestMexUnit();
      // logfile.WriteLine( "biggestmex is " + biggestmex.name + " " + biggestmex.humanName );
      for (int spotindex = 0; spotindex < MaxSpots && !Stopme; spotindex++)
      {	                
         //			logfile.WriteLine( "spotindex: " + spotindex );
         int bestspotx = 0, bestspoty = 0;
         int actualmetalatbestspot = 0; // use to try to put extractors over spot itself
         //finds the best spot on the map and gets its coords
         int BestNormalizedAvailableSpotAmount = 0;
         for (int y = 0; y < mapheight; y++)
         {
            for (int x = 0; x < mapwidth; x++)
            {
               if( NormalizedSpotAvailableMetal[ x ][ y ] > BestNormalizedAvailableSpotAmount ||
                     ( NormalizedSpotAvailableMetal[ x ][ y ] == BestNormalizedAvailableSpotAmount && 
                           metalremaining[ x ][ y ] > actualmetalatbestspot ) )
               {
                  BestNormalizedAvailableSpotAmount = NormalizedSpotAvailableMetal[ x ][ y ];
                  bestspotx = x;
                  bestspoty = y;
                  actualmetalatbestspot = metalremaining[ x ][ y ];
               }
            }
         }		
         //			logfile.WriteLine( "BestNormalizesdAvailableSpotAmount: " + BestNormalizedAvailableSpotAmount );                
         if( BestNormalizedAvailableSpotAmount < MinMetalForSpot )
         {
            Stopme = true; // if the spots get too crappy it will stop running the loops to speed it all up
            //				logfile.WriteLine( "Remaining spots too small; stopping search" );
         }

         if( !Stopme )
         {
            MetalPos metalPos = new MetalPos( bestspotx, bestspoty );
            TerrainPos terrainpos = metalPos.toTerrainPos();
            terrainpos.y = maps.getHeightMap().getElevationAt( terrainpos );

            //pos = Map.PosToFinalBuildPos( pos, biggestmex );

            //				logfile.WriteLine( "Metal spot: " + pos + " " + BestNormalizedAvailableSpotAmount );
            MetalSpot thismetalspot = new MetalSpot( (int)( ( BestNormalizedAvailableSpotAmount  * mapmaxmetal * maxmetalspotamount ) / 255 ), terrainpos );

            //    if (aicallback.CanBuildAt(biggestmex, pos) )
            //  {
            // pos = Map.PosToBuildMapPos( pos, biggestmex );
            // logfile.WriteLine( "Metal spot: " + pos + " " + BestNormalizedAvailableSpotAmount );

            //     if(pos.z >= 2 && pos.x >= 2 && pos.x < mapwidth -2 && pos.z < mapheight -2)
            //      {
            //  if(CanBuildAt(pos.x, pos.z, biggestmex.xsize, biggestmex.ysize))
            // {
            metalspotsal.add( thismetalspot );			
            SpotsFound++;

            //if(pos.y >= 0)
            //{
            // SetBuildMap(pos.x-2, pos.z-2, biggestmex.xsize+4, biggestmex.ysize+4, 1);
            //}
            //else
            //{
            //SetBuildMap(pos.x-2, pos.z-2, biggestmex.xsize+4, biggestmex.ysize+4, 5);
            //}
            //  }
            //   }
            //   }

            for (int myx = bestspotx - (int)ExtractorRadius; myx < bestspotx + (int)ExtractorRadius; myx++)
            {
               if (myx >= 0 && myx < mapwidth )
               {
                  for (int myy = bestspoty - (int)ExtractorRadius; myy < bestspoty + (int)ExtractorRadius; myy++)
                  {
                     if ( myy >= 0 && myy < mapheight &&
                           ( ( bestspotx - myx ) * ( bestspotx - myx ) + ( bestspoty - myy ) * ( bestspoty - myy ) ) <= (int)SquareExtractorRadius )
                     {
                        metalremaining[ myx ][ myy ] = 0; //wipes the metal around the spot so its not counted twice
                        NormalizedSpotAvailableMetal[ myx ][ myy ] = 0;
                     }
                  }
               }
            }

            // Redo the whole averaging process around the picked spot so other spots can be found around it
            for (int y = bestspoty - (int)DoubleExtractorRadius; y < bestspoty + (int)DoubleExtractorRadius; y++)
            {
               if(y >=0 && y < mapheight)
               {
                  for (int x = bestspotx - (int)DoubleExtractorRadius; x < bestspotx + (int)DoubleExtractorRadius; x++)
                  {
                     //funcion below is optimized so it will only update spots between r and 2r, greatly speeding it up
                     if((bestspotx - x)*(bestspotx - x) + (bestspoty - y)*(bestspoty - y) <= (int)FourSquareExtractorRadius && 
                           x >=0 && x < mapwidth && 
                           NormalizedSpotAvailableMetal[ x ][ y ] > 0 )
                     {
                        totalmetal = 0;
                        for (int myx = x - (int)ExtractorRadius; myx < x + (int)ExtractorRadius; myx++)
                        {
                           if (myx >= 0 && myx < mapwidth )
                           {
                              for (int myy = y - (int)ExtractorRadius; myy < y + (int)ExtractorRadius; myy++)
                              { 
                                 if (myy >= 0 && myy < mapheight && 
                                       ((x - myx)*(x - myx) + (y - myy)*(y - myy)) <= (int)SquareExtractorRadius )
                                 {
                                    totalmetal += metalremaining[ myx ][ myy ]; //recalculate nearby spots to account for deleted metal from chosen spot
                                 }
                              }
                           }
                        }
                        NormalizedSpotAvailableMetal[ x ][ y ] = totalmetal * 255 / maxmetalspotamount; //set that spots metal amount 
                     }
                  }
               }
            }
         }
      }

      if(SpotsFound > 500)
      {
         isMetalMap = true;
         metalspotsal.clear();
         logfile.WriteLine( "Map is considered to be a metal map" );
      }
      else
      {
         isMetalMap = false;

         // debug
         //for(list<AAIMetalSpot>::iterator spot = metal_spots.begin(); spot != metal_spots.end(); spot++)
      }

      MetalSpots = ( MetalSpot[] )metalspotsal.toArray( new MetalSpot[0] );

      SaveCache();
      logfile.WriteLine( "SearchMetalSpots() <<<");
   }
}
