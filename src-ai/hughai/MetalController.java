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

package hughai;

import java.util.*;

import com.springrts.ai.*;
import com.springrts.ai.oo.*;

import hughai.basictypes.*;
import hughai.utils.*;


// manages metal
// decides whether we need more extractors etc
public class MetalController
{
	CSAI csai;
	OOAICallback aicallback;
	ResourceManager resourceManager;
	LogFile logfile;

	public MetalController( PlayerObjects playerObjects )
	{
		csai = playerObjects.getCSAI();
		aicallback = csai.aicallback;
		logfile = playerObjects.getLogFile();
		resourceManager = playerObjects.getResourceManager();
	}

	// looks at current power usage, and requirements for unit, and decides if we can build it without stalling
	// assumes nothing else suddenly starting at same time...
	// do something more elegant later
	public boolean CanBuild( UnitDef constructor, UnitDef targetdef )
	{
		Resource metal = resourceManager.metalResource;
		float metalcost = targetdef.getCost(metal);
		float currentmetal = resourceManager.getCurrentMetal();
		float metalincome = aicallback.getEconomy().getIncome(metal);
		float metalusage = aicallback.getEconomy().getUsage(metal);
		
		float constructorbuildspeed = constructor.getBuildSpeed();
		
		// if (csai.DebugOn)
		//  {
		//      aicallback.SendTextMsg("metalcontroller canbuild " + def.humanName + " current metal: " + aicallback.GetMetal() + " cost: " + def.metalCost, 0);
		//  }
		if ( currentmetal > metalcost )
		{
			return true;
		}
		//return aicallback.GetMetal() > def.metalCost;
		//double excessmetalrequired = ( metalcost - currentmetal * 8 / 10 ) / targetdef.getBuildTime();
		float actualbuildtime = targetdef.getBuildTime() / constructorbuildspeed;
        logfile.WriteLine( "constructor build speed: " + constructorbuildspeed +
              " targetbuildtime: " + targetdef.getBuildTime()
                + " actual buildtime: " + actualbuildtime
                + " metalcost " + metalcost
                + " currentmetal " + currentmetal );
        float excessmetalrequired = metalcost - currentmetal;
        float OurIncome = metalincome - metalusage;
		// now, in actualbuildtime seconds, will we have earned excessmetalrequired metal?
		
		float metalearnedwhilstbuilding = actualbuildtime * OurIncome;
		
		// let's have a margin of 20% to be safe
		boolean result = ( metalearnedwhilstbuilding * 0.8 ) > excessmetalrequired;
        logfile.WriteLine( "Metal income: " + metalincome
              + " metal usage: " + metalusage
              + " metalearnedwhilstbuilding: " + metalearnedwhilstbuilding 
              + " result: " + result );
		
//		boolean result = ( excessmetalrequired * 4 ) < OurIncome;
//		logfile.WriteLine( "Current metal: " + currentmetal + " itemmetalcost: " + metalcost + " buildtime: " + targetdef.getBuildTime() + " excessmetalrequired: " + excessmetalrequired + " our income: " + OurIncome + " overall: " + result );
		return result;

	}
}
