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

import com.springrts.ai.*;
import com.springrts.ai.oo.*;

import hughai.CSAI;
import hughai.GameAdapter;
import hughai.PlayerObjects;
import hughai.basictypes.*;
import hughai.*;
import hughai.utils.*;


// stores the start position
// difficult job I know ;-)
// actually, harder than it sounds, because of .reloadai
// so we need to serialize it during reloads
// or.... ????
// add a point???
public class StartPosition extends GameAdapter
{
	CSAI csai;
	OOAICallback aicallback;
	UnitController unitController;
	FrameController frameController;
	
	StartPosition( PlayerObjects playerObjects )
	{
		csai = playerObjects.getCSAI();
		aicallback = playerObjects.getAicallback();
		this.unitController = playerObjects.getUnitController();
		this.frameController = playerObjects.getFrameController();
	
		csai.registerGameListener(this);
		//CSAI.GetInstance().UnitCreatedEvent += new CSAI.UnitCreatedHandler(UnitCreatedEvent);
	}

	public Float3 startposition = null;

	@Override
	public void UnitCreated(Unit unit, Unit builder)
	{
	   if( frameController.getFrame() <= 1 ) {
//		if (aicallback.getGame().getCurrentFrame() <= 1)
//		{
			if (unit.getDef().isCommander())
			{
				startposition = unitController.getPos( unit );
				//aicallback.get
			}
		}
	}
}

