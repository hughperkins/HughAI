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

package hughai;

import com.springrts.ai.AIFloat3;
import com.springrts.ai.oo.Unit;
import com.springrts.ai.oo.UnitDef;
import com.springrts.ai.oo.WeaponDef;

// use this to register a listener with csai
public class GameAdapter implements GameListener {
   public boolean overriden;
   
    @Override public void UnitCreated( Unit unit, Unit builder ){overriden = false;}
    @Override public void UnitFinished( Unit unit  ){overriden = false;}
    @Override public void UnitDamaged(Unit damaged,Unit attacker,float damage, AIFloat3 dir, WeaponDef weaponDef, boolean paralyzed ){overriden = false;}
    @Override public void UnitDestroyed( Unit deployed, Unit enemy ){overriden = false;}
    @Override public void UnitIdle( Unit deployedunit ){overriden = false;}
    
    @Override public void UnitMoveFailed(Unit unit){overriden = false;}
        
    @Override public void EnemyEnterLOS(Unit enemy){overriden = false;}
    @Override public void EnemyEnterRadar(Unit enemy){overriden = false;}
    @Override public void EnemyLeaveLOS(Unit enemy){overriden = false;}
    @Override public void EnemyLeaveRadar(Unit enemy){overriden = false;}
    @Override public void EnemyDestroyed( Unit enemy, Unit attacker ){overriden = false;}
    
    @Override public void Tick(int frame){overriden = false;}
}
