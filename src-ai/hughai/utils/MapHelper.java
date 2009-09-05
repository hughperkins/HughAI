package hughai.utils;

import java.util.*;

import hughai.*;

public class MapHelper {
   PlayerObjects playerObjects;
   public MapHelper( PlayerObjects playerObjects ) {
      this.playerObjects = playerObjects;
   }
   
   public void DumpMapDistribution( float[][] map ) {
      LogFile logfile = playerObjects.getLogFile();
      float[] allvalues = new float[ map.length * map[0].length ];
      int index = 0;
      for( int x = 0; x < map.length; x++ ) {
         for( int z = 0; z < map[0].length; z++ ) {
            allvalues[index] = map[x][z];
            index++;
         }            
      }
      Arrays.sort(allvalues);
      int numSegments = 9;
      int lastindex = allvalues.length - 1;
      //logfile.WriteLine( "Slopemap distribution:" );
      for( int i = 0; i <= numSegments; i++ ) {
         int thisindex = ( lastindex * i ) / numSegments;
         logfile.WriteLine( "   " + thisindex + ": " + allvalues[thisindex] );
      }

   }
}
