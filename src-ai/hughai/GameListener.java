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

import com.springrts.ai.*;
import com.springrts.ai.oo.*;

// Use this to register a game listener with csai
public interface GameListener {
    public void UnitCreated( Unit unit, Unit builder );
    public void UnitFinished( Unit unit );
    public void UnitDamaged(Unit damaged,Unit attacker,float damage, AIFloat3 dir, WeaponDef weaponDef, boolean paralyzed);
    public void UnitDestroyed( Unit damaged, Unit enemy );
    public void UnitIdle( Unit unit );
    
    public void UnitMoveFailed(Unit unit);
        
    public void EnemyEnterLOS(Unit enemy);
    public void EnemyEnterRadar(Unit enemy);
    public void EnemyLeaveLOS(Unit enemy);
    public void EnemyLeaveRadar(Unit enemy);
    public void EnemyDestroyed( Unit enemy, Unit attacker );
    
    public void Tick(int frame);
}
