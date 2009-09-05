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

// file logging functions
// ====================
//
// just call WriteLog( "my message" ) to write to the log
// log appears in same directory as dll (same directory as tasclient.exe), named "csharpai_teamX.log"
// where X is team number, in case we are running multiple AIs (one per team)

package hughai.utils;

import java.io.*;

import hughai.PlayerObjects;
import hughai.*;


public class LogFile
{
	public boolean AutoFlushOn = true;
	
	PrintWriter printWriter;
	
	PlayerObjects playerObjects;
	//TimeHelper timeHelper;

	public LogFile( PlayerObjects playerObjects ) {
	   this.playerObjects = playerObjects;
	}
	
	public LogFile Init( String logfilepath )
	{
		try {
		   System.out.println("logfile init: [" + logfilepath + "]");
		   //sw = new StreamWriter(logfilepath, false);
		   printWriter = new PrintWriter( logfilepath );
		   //CSAI.GetInstance().RegisterVoiceCommand( "flushlog", new CSAI.VoiceCommandHandler( this.VCFlushLog ) );
		} catch( Exception e ) {
			throw new RuntimeException( e );
		}
		return this;
	}

	public void Flush()
	{
		printWriter.flush();
	}

	// arguably we shouldnt auto-flush. because it slows writes, up to you
	public void WriteLine( String message )
	{
		//sw.WriteLine(DateTime.Now.toString("hh:mm:ss.ff") + ": " + message);
		printWriter.println(playerObjects.getTimeHelper().GetCurrentGameTimeString() + ": " + message);
		System.out.println(playerObjects.getTimeHelper().GetCurrentGameTimeString() + ": " + message);
		if( AutoFlushOn )
		{
			printWriter.flush();
		}
		//sw.Flush();
	}

	public void Shutdown()
	{
		printWriter.flush();
		printWriter.close();
	}
}
