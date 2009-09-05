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

package hughai.unitdata;

import java.util.*;

import com.springrts.ai.*;
import com.springrts.ai.oo.*;

import hughai.CSAI;
import hughai.PlayerObjects;
import hughai.basictypes.*;
import hughai.*;
import hughai.utils.*;


// caches last instruction given to a unit
public class UnitCommandCache
{
	CSAI csai;
	OOAICallback aicallback;
	LogFile logfile;

	public UnitCommandCache( PlayerObjects playerObjects )
	{
		csai = playerObjects.getCSAI();
		aicallback = csai.aicallback;
		logfile = playerObjects.getLogFile();
	}

	public HashMap< Unit,OOPCommands.OOPCommand> LastCommandByUnit = new HashMap< Unit, OOPCommands.OOPCommand>();

	public void RegisterCommand(Unit unit, OOPCommands.OOPCommand command)
	{
		LastCommandByUnit.put(unit, command);
	}
}

