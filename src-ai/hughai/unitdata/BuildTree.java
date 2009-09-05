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

import com.springrts.ai.oo.*;

import hughai.CSAI;
import hughai.PlayerObjects;
import hughai.*;
import hughai.utils.*;


// gives what can be constructed, given the name of the thing which will do the 
// constructing
public class BuildTree
{
	HashMap<String, ArrayList<String>> buildablenamesbyname = new HashMap<String, ArrayList<String>>();

	//PlayerObjects playerObjects;
	CSAI csai;
	OOAICallback aicallback;
	LogFile logfile;
	BuildTable buildTable;

	public BuildTree(PlayerObjects playerObjects)
	{
		//this.playerObjects = playerObjects;
		csai = playerObjects.getCSAI();
		aicallback = csai.aicallback;
		buildTable = playerObjects.getBuildTable();
		logfile = playerObjects.getLogFile();
	}

	public boolean CanBuild(String buildername, String targetname)
	{
		if (!buildablenamesbyname.containsKey(buildername))
		{
			ArrayList<String> thisbuilderoptions = new ArrayList<String>();
			buildablenamesbyname.put(buildername,thisbuilderoptions );
			UnitDef unitdef = buildTable.UnitDefByName.get(buildername);
			for( UnitDef buildingOption : unitdef.getBuildOptions() ) {				
				thisbuilderoptions.add( buildingOption.getName() );
			}
		}
		return buildablenamesbyname.get(buildername).contains(targetname);
	}
}
