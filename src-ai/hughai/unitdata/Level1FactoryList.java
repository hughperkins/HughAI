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
import java.util.Map;

import com.springrts.ai.*;
import com.springrts.ai.oo.*;

import hughai.PlayerObjects;
import hughai.basictypes.*;
import hughai.*;
import hughai.mapping.*;
import hughai.unitdata.*;
import hughai.utils.*;


public class Level1FactoryList extends UnitList
{
	public Level1FactoryList( PlayerObjects playerObjects ) {
		super( playerObjects );
	}

	@Override
	List<String> getUnitNames(){
		return Arrays.asList( new String[] { 
				"armvp", "armlab", "corvp", "corlab", "armap", "corap" } );
	}
}

