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
package hughai.basictypes;

import com.springrts.ai.*;
import com.springrts.ai.oo.*;
import com.springrts.ai.oo.clb.*;

import hughai.unitdata.UnitController;

// wraps the Spring Command with an OOP version

// allows adding toString etc

public class OOPCommands {
	// something that can be attacked, moved to etc
	public static class Target
	{
		public Target() { }
		public double[] ToDoubleArray()
		{
			return null;
		}
	}

	public static class PositionTarget extends Target
	{
		public Float3 targetpos;
		public PositionTarget(Float3 targetpos)
		{
			this.targetpos = targetpos;
		}
		//@Override
		//public double[] ToDoubleArray()
		//{
			//return targetpos.ToDoubleArray();
		//}
		@Override
		public String toString()
		{
			return "PositionTarget: " + targetpos.toString();
		}
	}

	// targeting a specific unit
	public static class UnitTarget extends Target
	{
		public Unit target;
		public UnitTarget(Unit target)
		{
			this.target = target;
		}
//		@Override
	//	public double[] ToDoubleArray()
		//{
			//return new double[] { target };
		//}
	}

	public static class OOPCommand
	{
		public Unit UnitToReceiveOrder;
		public OOPCommand()
		{
		}
		public OOPCommand(Unit unittoreceiveorder)
		{
			this.UnitToReceiveOrder = unittoreceiveorder;
		}
		public void execute() throws CallbackAIException
		{
		}
		@Override
		public String toString()
		{
			return this.getClass().getSimpleName() + " unit " + UnitToReceiveOrder.getUnitId() + " " + UnitToReceiveOrder.getDef().getHumanName();
		}
	}

	public static class BuildCommand extends OOPCommand
	{
		public UnitDef unitdeftobuild;
		public Float3 pos = null;
		public String unitdefhumanname = "";

		public BuildCommand(Unit builder, UnitDef unitdeftobuild, String unitdefhumanname)
		{
			this.UnitToReceiveOrder = builder;
			this.unitdeftobuild = unitdeftobuild;
			this.unitdefhumanname = unitdefhumanname;
		}

		public BuildCommand(Unit builder, UnitDef unitdeftobuild, Float3 pos, String unitdefhumanname )
		{
			this.UnitToReceiveOrder = builder;
			this.unitdeftobuild = unitdeftobuild;
			this.pos = pos;
			this.unitdefhumanname = unitdefhumanname;
		}
		@Override
		public void execute() throws CallbackAIException
		{
			if (pos == null)
			{
				UnitToReceiveOrder.build(unitdeftobuild, new AIFloat3(), 0, (short)0, Integer.MAX_VALUE );
//						- idtobuild, new double[] { });
			}
			else
			{
				UnitToReceiveOrder.build(unitdeftobuild, pos.toAIFloat3(), 0, (short)0, Integer.MAX_VALUE );
//				return new Command( -idtobuild, pos.ToDoubleArray());
			}
		}
		@Override
		public String toString()
		{
			if (pos != null)
			{
				return "BuildCommand " + UnitToReceiveOrder.getDef().getHumanName() + " " 
				   + UnitToReceiveOrder.getUnitId() + " building " + unitdefhumanname + " at " + pos.toString();
			}
			else
			{
				return "BuildCommand " + UnitToReceiveOrder.getDef().getHumanName() + " " 
                + UnitToReceiveOrder.getUnitId() + " building " + unitdefhumanname;
			}
		}
	}

	public static class AttackCommand extends OOPCommand
	{
		public Target target;
		public AttackCommand(Unit attacker, Target target)
		{
			this.UnitToReceiveOrder = attacker;
			this.target = target;
		}
		@Override
		public void execute() throws CallbackAIException
		{
			if( target.getClass() == UnitTarget.class){
				UnitToReceiveOrder.attack(((UnitTarget)target).target, (short)0, Integer.MAX_VALUE );
			} else if ( target.getClass() == PositionTarget .class){
				UnitToReceiveOrder.attackArea(((PositionTarget)target).targetpos.toAIFloat3(), 100, (short)0, Integer.MAX_VALUE );
			}
			throw new RuntimeException("Invalid target");
		}
		@Override
		public String toString()
		{
			return "AttackCommand " + UnitToReceiveOrder + " attacking " + target.toString();
		}
	}

	public static class MoveToCommand extends OOPCommand
	{
		public Float3 targetpos;
		public MoveToCommand(Unit unit, Float3 pos)
		{
			this.UnitToReceiveOrder = unit;
			this.targetpos = pos;
		}
		@Override
		public void execute() throws CallbackAIException
		{
			UnitToReceiveOrder.moveTo(targetpos.toAIFloat3(), (short)0, Integer.MAX_VALUE );
		}
		@Override
		public String toString()
		{
		   UnitDef unitdef = UnitToReceiveOrder.getDef();
		   if( unitdef != null ) {
			return "MoveToCommand " + UnitToReceiveOrder.getUnitId() + " " + UnitToReceiveOrder.getDef().getHumanName() + " moving to " + targetpos.toString();
		   }
		   else {
		      return "MoveToCommand.  Unittoreceiveorder doesn't exist. id " + UnitToReceiveOrder.getUnitId();
		   }
		}
	}

	public static class GuardCommand extends OOPCommand
	{
		public Unit unittobeguarded;
		public GuardCommand(Unit unit, Unit unittobeguarded)
		{
			this.UnitToReceiveOrder = unit;
			this.unittobeguarded = unittobeguarded;
		}
		@Override
		public void execute() throws CallbackAIException
		{
			UnitToReceiveOrder.guard(unittobeguarded, (short)0, Integer.MAX_VALUE );
			//return new Command(Command.CMD_GUARD, new double[] { targetid } );
		}
	}

	public static class ReclaimCommand extends OOPCommand
	{
		public Float3 pos;
		public double radius;
		public ReclaimCommand(Unit unit, Float3 pos, double radius)
		{
			this.UnitToReceiveOrder = unit;
			this.pos = pos;
			this.radius = radius;
		}
		@Override
		public void execute() throws CallbackAIException
		{
			UnitToReceiveOrder.reclaimInArea(pos.toAIFloat3(), (float)radius, (short)0, Integer.MAX_VALUE );
			//Command(Command.CMD_RECLAIM, new double[] { pos.x, pos.y, pos.z, radius });
		}
	}

	public static class SelfDestructCommand extends OOPCommand
	{
		public SelfDestructCommand(Unit unit )
		{
			this.UnitToReceiveOrder = unit;
		}
		@Override
		public void execute() throws CallbackAIException
		{
			UnitToReceiveOrder.selfDestruct((short)0, Integer.MAX_VALUE );
			// return new Command(Command.CMD_SELFD, new double[] {});
		}
	}

	public static class StopCommand extends OOPCommand
	{
		public StopCommand(Unit unit)
		{
			this.UnitToReceiveOrder = unit;
		}
		@Override
		public void execute() throws CallbackAIException
		{
			UnitToReceiveOrder.stop((short)0, Integer.MAX_VALUE );
			// return new Command(Command.CMD_STOP, new double[] { });
		}
	}
}
