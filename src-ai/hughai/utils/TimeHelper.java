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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import com.springrts.ai.oo.*;

import hughai.*;


public class TimeHelper
{
	// abstracts out our specific implementation of how we are storing time
	public static class TimePoint {
	}
	public static class GameTimePoint extends TimePoint{
		public int frame;
		public GameTimePoint(){}
		public GameTimePoint( int frame ){
			this.frame = frame;
		}
		public TimeSpan asTimeSpan(){
			return new TimeSpan((frame * 1000) / 30 );
		}
		@Override
		public String toString(){
			return "" + asTimeSpan(); 
		}
	}
	public static class RealTimePoint extends TimePoint {
		public long milliseconds;
		public RealTimePoint(){}
		public RealTimePoint(double seconds){
			this.milliseconds = (long)(seconds * 1000);
		}
		public RealTimePoint(long milliseconds){
			this.milliseconds = milliseconds;
		}
	}
	public static class TimeSpan{
		public int hours;
		public int minutes;
		public int seconds;
		public int milliseconds;
		public TimeSpan(){}
		public TimeSpan( int hours, int minutes, int seconds, int milliseconds){
			this.hours = hours;
			this.minutes = minutes;
			this.seconds = seconds;
			this.milliseconds = milliseconds;
		}
		public void add( TimeSpan second ) {
		   fromMilliseconds( getTotalMilliseconds() 
		         + second.getTotalMilliseconds() );
		}
		public TimeSpan( double seconds ){
			fromMilliseconds( (long)( seconds * 1000 ));
		}
		public TimeSpan( long milliseconds ){
			fromMilliseconds( milliseconds );
		}
		public long getTotalMilliseconds(){
			return (long)hours * 1000 * 60 * 60
				+ (long)minutes * 1000 * 60
				+ (long)seconds * 1000
				+ (long)milliseconds;
		}
		void fromMilliseconds( long milliseconds ){
			hours = (int)( milliseconds / 60 / 60 / 1000 );
			milliseconds -= hours * 60 * 60 * 1000;
			minutes = (int)( milliseconds / 60 / 1000 );
			milliseconds -= minutes * 60 * 1000;
			this.seconds = (int)( milliseconds / 1000);
			milliseconds -= this.seconds * 1000;
			this.milliseconds = (int)( milliseconds );			
		}
		@Override
		public String toString(){
		   if( getTotalMilliseconds() < 1000 ) {
		      return milliseconds + " ms";
		   }
		   return hours + ":" + minutes + ":" + seconds + "." + milliseconds;			
		}
	}
	
//	OOAICallback aicallback;
//	FrameController frameController;
	PlayerObjects playerObjects;
	
	public TimeHelper( PlayerObjects playerObjects ){
//		this.aicallback = aicallback;
	   this.playerObjects = playerObjects;
	}
	
	public RealTimePoint GetRealTimePoint()
	{
		return new RealTimePoint(System.currentTimeMillis());
	}
	public GameTimePoint GetGameTimePoint()
	{
		return new GameTimePoint(getCurrentFrame());
	}
	public int getCurrentFrame(){
		return playerObjects.getFrameController().getFrame();
	}
	public double getGameTimeSeconds(){
		int frames = playerObjects.getFrameController().getFrame();
		return (double)frames / 30;		
	}
	
	public String GetCurrentGameTimeString()
	{
		double gameTimeSeconds = getGameTimeSeconds();
		TimeSpan timeSpan = new TimeSpan( gameTimeSeconds );
		return "" + timeSpan;
	}
	public String GetCurrentRealTimeString()
	{
		Date now = new Date( System.currentTimeMillis() );
        DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        return dateFormat.format(now);
	}
	
	public String FormatTimeSpan(TimeSpan timespan)
	{
		return "" + timespan;
	}
	
	public TimeSpan CompareRealTimePoint(RealTimePoint later, RealTimePoint earlier) {
		return new TimeSpan( later.milliseconds - earlier.milliseconds );
	}
}

