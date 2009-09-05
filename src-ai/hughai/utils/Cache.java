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

package hughai.utils;

import java.util.*;

// caches ... something
// this version just caches a list of something
// and can return if the item is already in the lsit or not
// can be configured with an "age" which will cause isInCache to return false
// if that age has been exceeded
// for now, just using an integer as the age, which we will use as frame in
// reality
public class Cache<T> {
   HashMap<T,Integer> timebyitem = new HashMap<T,Integer>();
   
   int maxAge;
   
   public Cache( int maxAge ) {
      this.maxAge = maxAge;
   }
   
   // checks if in cache, and not aged
   // doesn't modify cache
   public boolean isInCache( int currenttime, T item ) {
      boolean isincache = false;
      Integer olditemtiime = timebyitem.get( item );
      if( olditemtiime != null ) {
         if( currenttime - olditemtiime <= maxAge ) {
            isincache = true;
         } else {
            timebyitem.remove( item );
         }
      }
      return isincache;
   }
   
   // adds item to cache
   public void cache( int currenttime, T item ) {
      timebyitem.put( item, currenttime );
   }
   
   // purges cache of old items
   public void cleanUp( int currenttime ) {
      for( T item : timebyitem.keySet() ) {
         if( currenttime - timebyitem.get( item ) > maxAge ) {
            timebyitem.remove( item );
         }
      }
   }
}
