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

import java.util.*;
import java.util.Map;

import com.springrts.ai.*;
import com.springrts.ai.oo.*;

import hughai.basictypes.*;
import hughai.utils.*;


// handles assigning ownership of newly created units
// a unit taht builds something tells Ownership what it built and roughly where
// this class assigns the first unit that matches to that unit
// this class follows units until they are finished or destroyed
public class Ownership
{
   int maxdistanceconsideredsame = 500;

   public interface IBuilder
   {
      public void UnitCreated(IOrder order, Unit newunit );
      public void UnitFinished(IOrder order, Unit newunit );
      public void UnitDestroyed(IOrder order, Unit newunit );
   }

   public interface IOrder
   {
   }

   static class OwnershipOrder implements IOrder
   {
      public IBuilder builder;
      public Unit constructor;
      public UnitDef orderedunit;
      public Float3 pos;
      public Unit newunit = null;
      public OwnershipOrder(){}
      public OwnershipOrder( IBuilder builder, Unit constructor, UnitDef orderedunit, Float3 pos )
      {
         this.builder = builder;
         this.constructor = constructor;
         this.orderedunit = orderedunit;
         this.pos = pos;
      }
      @Override
      public String toString()
      {
         return "OwnershipOrder orderedunit: " + orderedunit.getHumanName() + " pos " + pos.toString() + " deployedid " + newunit;
      }
   }

   List<OwnershipOrder> orders = new ArrayList<OwnershipOrder>();
   Map<Unit, OwnershipOrder> ordersbyconstructor = new HashMap<Unit, OwnershipOrder>(); // index of orders by constructorid, to allow removal later

   //PlayerObjects playerObjects;
   CSAI csai;
   OOAICallback aicallback;
   //	FriendlyUnitPositionObserver friendlyUnitPositionObserver;
   LogFile logfile;

   Ownership(PlayerObjects playerObjects)
   {
      csai = playerObjects.getCSAI();
      aicallback = csai.aicallback;
      logfile = playerObjects.getLogFile();
      //		friendlyUnitPositionObserver = playerObjects.getFriendlyUnitPositionObserver();

      csai.registerGameListener( new GameListener() );

      if (csai.DebugOn)
      {
         csai.RegisterVoiceCommand("dumpownership", new DumpOwnershipHandler());
      }
   }

   // if constructor is idle, this constructor is no longer building
   public void SignalConstructorIsIdle(Unit constructor)
   {
      RemovePreviousOrdersFromThisConstructor(constructor);
   }

   void RemovePreviousOrdersFromThisConstructor(Unit constructor)
   {
      synchronized (orders)
      {
         if (ordersbyconstructor.containsKey(constructor))
         {
            OwnershipOrder order = ordersbyconstructor.get(constructor);
            logfile.WriteLine("Removing order by " + constructor.getUnitId() + " for " + order.orderedunit.getHumanName());
            order.builder.UnitDestroyed(order, order.newunit );
            orders.remove(order);
            ordersbyconstructor.remove(constructor);
         }
      }
   }

   public class DumpOwnershipHandler implements VoiceCommandHandler {
      @Override
      public void commandReceived( String cmd, String[] split, int player)
      {
         synchronized (orders)
         {
            logfile.WriteLine("number orders: " + orders.size() );
            for (OwnershipOrder order : orders)
            {
               logfile.WriteLine(order.toString());
            }
         }
      }
   }

   class GameListener extends GameAdapter {
      @Override
      public void UnitDestroyed(Unit unit, Unit enemy)
      {
         synchronized (orders)
         {
            List<OwnershipOrder> orderstoremove = new ArrayList<OwnershipOrder>();
            for (OwnershipOrder order : orders)
            {
               if (order.newunit == unit)
               {
                  order.builder.UnitDestroyed(order, unit );
                  orderstoremove.add(order);
               }
            }
            for (OwnershipOrder order : orderstoremove)
            {
               orders.remove(order);
            }
            ordersbyconstructor.remove(unit);
         }
      }

      @Override
      public void UnitCreated(Unit newunit, Unit builder )
      {
         if( builder != null ) {
         logfile.WriteLine( "Ownership.unitcreated " + newunit.getUnitId() 
               + " " + newunit.getDef().getHumanName() + 
               " by " + builder.getUnitId() + 
               " " + builder.getDef().getHumanName() );
         } else {
            logfile.WriteLine( "Ownership.unitcreated " + newunit.getUnitId() 
                  + " " + newunit.getDef().getHumanName() );            
         }
         synchronized (orders)
         {
            for (OwnershipOrder order : orders)
            {
               if (order.constructor == builder )
               {
                  logfile.WriteLine( " ... matches order" );
                  //AIFloat3 createdunitpos = friendlyUnitPositionObserver.PosById[ deployedunitid ];
                  //if (Float3Helper.GetSquaredDistance(createdunitpos, order.pos) < maxdistanceconsideredsame * maxdistanceconsideredsame)
                  //{
                  order.newunit = newunit;
                  order.builder.UnitCreated(order, newunit );
                  //}
               }
            }
         }
      }

      @Override
      public void UnitFinished(Unit newunit)
      {
         synchronized (orders)
         {
            Random random = new Random();
            //int refnum = random.nextInt(10000);
            List<OwnershipOrder> orderstoremove = new ArrayList<OwnershipOrder>();
            logfile.WriteLine("orders count is : " + orders.size() );
            try
            {
               for (OwnershipOrder order : orders)
               {
                  if (order.newunit == newunit)
                  {
                     order.builder.UnitFinished(order, newunit);
                     orderstoremove.add(order);
                  }
               }
               for (OwnershipOrder order : orderstoremove)
               {
                  logfile.WriteLine( "removing order for " + order.orderedunit.getHumanName()
                        + " , built by " + order.constructor.getUnitId() + " " + order.constructor.getDef().getHumanName() );
                  orders.remove(order);
               }
               ordersbyconstructor.remove(newunit);
            }
            catch (Exception e)
            {
               logfile.WriteLine( Formatting.exceptionToStackTrace( e ) );
               logfile.WriteLine( e.toString());
               //throw e;
               throw new RuntimeException(e);
            }
         }
      }
   }

   public IOrder RegisterBuildingOrder(IBuilder builder, Unit constructor, UnitDef orderedunittype, Float3 orderedpos)
   {
      RemovePreviousOrdersFromThisConstructor(constructor);

      OwnershipOrder order = new OwnershipOrder(builder, constructor, orderedunittype, orderedpos);
      synchronized (orders)
      {
         orders.add(order);
         ordersbyconstructor.put(constructor, order);
      }
      return order;
   }
}

