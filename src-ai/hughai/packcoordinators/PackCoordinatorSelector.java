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

package hughai.packcoordinators;

import java.util.*;

import com.springrts.ai.*;
import com.springrts.ai.oo.*;
import com.springrts.ai.oo.Map;

import hughai.PlayerObjects;
import hughai.basictypes.*;
import hughai.*;
import hughai.utils.*;


// use this if your class uses many packcoordinators, and you want to switch easily between them
public class PackCoordinatorSelector
{
	ArrayList<IPackCoordinator> packcoordinators = new ArrayList<IPackCoordinator>();

	LogFile logfile;

	public PackCoordinatorSelector( PlayerObjects playerObjects )
	{
		logfile = playerObjects.getLogFile();
	}

	// calling class calls this for each packcoordinator it wants to use
	public void LoadCoordinator( IPackCoordinator packcoordinator )
	{
		if( !packcoordinators.contains( packcoordinator ) )
		{
			packcoordinators.add( packcoordinator );
		}
	}

	IPackCoordinator activecoordinator = null;

	// calling class runs this to activate packcoordinator
	public void ActivatePackCoordinator( IPackCoordinator packcoordinator )
	{
		if( !packcoordinators.contains( packcoordinator ) )
		{
			throw new RuntimeException(
					"PackCoordinatorSelector: pack coordinator " + packcoordinator.toString() + " not found" );
		}

		if( activecoordinator != packcoordinator )
		{
			logfile.WriteLine( "PackCoordinator selector: changing to coordinator " + packcoordinator.toString() );
			// first disactivate others, then activate new one
			// the order of operations is important since they will probalby be using GiveORder, and GiveOrder overwrites old orders with new ones
			for( IPackCoordinator thispackcoordinator : packcoordinators )
			{
				if( thispackcoordinator != packcoordinator )
				{
					thispackcoordinator.Disactivate();
				}
			}
			for( IPackCoordinator thispackcoordinator : packcoordinators )
			{
				if( thispackcoordinator == packcoordinator )
				{
					thispackcoordinator.Activate();
				}
			}
			activecoordinator = packcoordinator;
		}
	}

	public void DisactivateAll()
	{
		if( activecoordinator != null )
		{
			logfile.WriteLine( "PackCoordinator selector: disactivating all" );
			for( IPackCoordinator thispackcoordinator : packcoordinators )
			{
				thispackcoordinator.Disactivate();
			}
			activecoordinator = null;
		}
	}
}

