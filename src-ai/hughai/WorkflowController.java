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

import hughai.*;
import hughai.building.Workflows.Workflow;
import hughai.Ownership.IOrder;
import hughai.building.Workflows.Workflow.Order;
import hughai.building.*;
import hughai.basictypes.*;
import hughai.mapping.*;
import hughai.unitdata.*;
import hughai.utils.*;
import hughai.controllers.level1.*;
import hughai.controllers.level2.ScoutControllerRaider;
import hughai.controllers.level2.TankController;

// in theory this is a high level class that just matches
// other classes together
// for now, it's still a bit of a hack
// a lot of this needs to be factored out better
//
// The connection between this and Workflows is a bit hacky for now,
// not least the hashmap of unitsUnderContructionByOrder
// but it gets it working.  This bit is under transition at the moment, since
// just finished writing the Workflows class.
public class WorkflowController
{
   PlayerObjects playerObjects;
   CSAI csai;
   OOAICallback aicallback;
   ResourceManager resourceManager;
   LogFile logfile;
   UnitController unitController;
   //	MovementMaps movementMaps;
   //	BuildMap buildMap;
   BuildPlanner buildPlanner;
   Ownership ownership;
   BuildTree buildTree;
   BuildTable buildTable;
   MetalController metalController;
   EnergyController energyController;
   GiveOrderWrapper giveOrderWrapper;
   UnitDefHelp unitDefHelp;
   Metal metal;
   DrawingUtils drawingUtils;
   ReclaimHelper reclaimHelper;
   UnitLists unitLists;
   //LosMap losmap;
   Maps maps;
   EnemyTracker enemyTracker;
//   TankController tankcontroller;
//   TankController helicoptercontroller;
//   ScoutControllerRaider scoutcontroller;
   Config config;
   Offense offense;
   Reconnaissance reconnaissance;
   Workflows workflows;

   Random random = new Random();

   public double energymetalratio = 10.0;

   HashMap<Order,ArrayList<Ownership.IOrder>> unitsUnderContructionByOrder = new HashMap<Order, ArrayList<IOrder>>();

   List<Unit> ActiveConstructors = new ArrayList<Unit>();
   Map<Unit, Unit> AssistingConstructors = new HashMap<Unit, Unit>(); // id of assisted keyed by id of assistor

   Workflows.Workflow currentWorkflow;
   Collection<Workflows.Workflow.Order> currentOrders;
   
//   int lastcheckidleframe = -1000;

//   List<Order> orders = new ArrayList<Order>();

   public WorkflowController( PlayerObjects playerObjects )
   {
      this.playerObjects = playerObjects;
      csai = playerObjects.getCSAI();
      aicallback = csai.aicallback;
      resourceManager = playerObjects.getResourceManager();
      logfile = playerObjects.getLogFile();
      unitController = playerObjects.getUnitController();
      //		movementMaps = playerObjects.getMovementMaps();
      //		buildMap = playerObjects.getBuildMap();
      buildPlanner = playerObjects.getBuildPlanner();
      ownership = playerObjects.getOwnership();
      buildTree = playerObjects.getBuildTree();
      buildTable = playerObjects.getBuildTable();
      metalController = playerObjects.getMetalController();
      energyController = playerObjects.getEnergyController();
      giveOrderWrapper = playerObjects.getGiveOrderWrapper();
      unitDefHelp = playerObjects.getUnitDefHelp();
      metal = playerObjects.getMetal();
      drawingUtils = playerObjects.getDrawingUtils();
      reclaimHelper = playerObjects.getReclaimHelper();
      unitLists = playerObjects.getUnitLists();
      config = playerObjects.getConfig();
      //		losmap = playerObjects.getLosMap();
      maps = playerObjects.getMaps();
      enemyTracker = playerObjects.getEnemyTracker();
      workflows = playerObjects.getWorkflows();
      
      if (csai.DebugOn)
      {
         csai.RegisterVoiceCommand("dumpworkflow", new DumpWorkFlowHandler());
      }
   }

   public class DumpWorkFlowHandler implements VoiceCommandHandler {
      public void commandReceived(String cmd, String[] split, int playe)
      {
         logfile.WriteLine("Workflow:");
         for (Order order : currentOrders )
         {
            logfile.WriteLine(order.toString());
         }
      }
   }

   public void Activate()
   {
      metal.Init();

      // just pick some workflow for now...
      currentWorkflow = workflows.getWorkflowsByName().get(
            config.getDefaultWorkflowName() );
      if( currentWorkflow == null ) {
         for( Workflow workflow : workflows.getWorkflowsByName().values() ) {
            currentWorkflow = workflow;
         }
      }
      currentOrders = currentWorkflow.orders;
      
//      tankcontroller = new TankController(
//            playerObjects,
//            unitLists.getTankList().getUnits(), 
//            buildTable.getUnitDefByName("armstump" ) );
//      tankcontroller.Activate();
      //	helicoptercontroller = new TankController(HelicopterList.GetInstance().defbyid, BuildTable.GetInstance().UnitDefByName["arm_brawler"]);
      //helicoptercontroller.Activate();
//      scoutcontroller = new ScoutControllerRaider( playerObjects );
//      scoutcontroller.Activate();

      // note to self: should these really be in the workflow?
      reconnaissance = new Reconnaissance( playerObjects );
      reconnaissance.Activate();
      
      offense = new Offense( playerObjects );
      offense.Activate();

      maps.getLosMap();
      
      playerObjects.getBuildTree();

      unitController.LoadExistingUnits();
      enemyTracker.LoadExistingUnits();

      CheckIdleUnits();

      csai.registerGameListener( new GameListener() );
   }

   class GameListener extends GameAdapter {
      @Override
      public void Tick( int frame ) {
//         if ( frame - lastcheckidleframe > 30)
//         {
            CheckIdleUnits();
//            lastcheckidleframe = frame;
//         }
      }

      // note to self: this function is a f**king mess
      @Override
      public void UnitIdle(Unit constructor)
      {
         UnitDef constructorunitdef = unitController.getUnitDef( constructor );
//         logfile.WriteLine( "workflow.unitidle " + idleunit.getUnitId() 
//               + " " + idleunit.getDef().getHumanName() + " " +
//               " isbuilder: " + unitdef.isBuilder() + " "
//               + " unitdefhelp.isconstructor " + unitDefHelp.IsConstructor( unitdef ));
         if (!constructorunitdef.isBuilder() )
         {
            //logfile.WriteLine("cantbuild");
            return;
         }
         logfile.WriteLine("unitidleevent " + constructor.getUnitId()
               + " " + constructorunitdef.getName() 
               + " " + constructorunitdef.getHumanName());
         if (ActiveConstructors.contains(constructor))
         {
            ActiveConstructors.remove(constructor);
         }
         ownership.SignalConstructorIsIdle(constructor);
         double highestpriority = 0;
         List<Order> bestorders = new ArrayList<Order>();
         for (Order order : currentOrders)
         {
            double thispriority = order.priority;
            if (thispriority >= highestpriority)
            {
               if( unitsUnderContructionByOrder.get( order) == null ) {
                  unitsUnderContructionByOrder.put(  order, new ArrayList<IOrder>() );
               }
               List<IOrder> unitsunderconstruction = unitsUnderContructionByOrder.get( order);
               int currentunits = unitsunderconstruction.size();
               if (unitController.UnitsByName.containsKey(order.unitname))
               {
                  currentunits += unitController.UnitsByName.get(order.unitname).size();
               }
               if( currentunits < order.quantity)
               {
                  if (buildTree.CanBuild(constructorunitdef.getName().toLowerCase(), order.unitname))
                  {
                     //if( CanBuild(deployedunitid, 
                     if (thispriority > highestpriority)
                     {
                        highestpriority = thispriority;
                        bestorders = new ArrayList<Order>();
                        bestorders.add(order);
                       // csai.DebugSay("Possible order: " + order.toString());
                     }
                     else if (thispriority == highestpriority)
                     {
                        bestorders.add(order);
                      //  csai.DebugSay("Possible order: " + order.toString());
                     }
                  }
               }
            }
         }
         //if (bestorders.Count == 0)
         //  {
         //      csai.DebugSay("No orders found");
         //      return;
         //  }
         ArrayList<Order>possibleorders = new ArrayList<Order>(); // get orders this unit can build
         boolean metalneeded = false;
         boolean energyneeded = false;
         UnitDef deftobuild = null;
         for (Order order : bestorders)
         {
            csai.DebugSay("bestorder " + order.unitname);
            //if( BuildTree.GetInstance().CanBuild( unitdef.getName().ToLower(), order.unitname ) )
            //{
            deftobuild = buildTable.getUnitDefByName(order.unitname);
            if (metalController.CanBuild( constructorunitdef, deftobuild))
            {
               if (energyController.CanBuild( constructorunitdef, deftobuild))
               {
                  possibleorders.add(order);
                  csai.DebugSay("possible: " + order.unitname);
               }
               else
               {
                  csai.DebugSay("needs energy");
                  energyneeded = true;
               }
            }
            else
            {
               csai.DebugSay("needs metal");
               metalneeded = true;
            }
            //}
         }
         if( possibleorders.size() == 0 )
         {
            String commanderUnitName = buildTree.listToOurTeamsUnitName( config.getCommanderunitnames() );
            if ( unitLists.getLevel1ConstructorList().getUnits().size() < 1 &&
                  !unitController.UnitsByName.containsKey( commanderUnitName ) )
            {
               if (BuildConstructionVehicle(constructor, constructorunitdef))
               {
                  return;
               }
            }

            if (energyneeded || resourceManager.getCurrentEnergy() < resourceManager.getEnergyStorage() / 5 )
            {
               if (BuildSolarCell(constructor))
               {
                  logfile.WriteLine("building solarcell");
                  if (AssistingConstructors.containsKey(constructor))
                  {
                     AssistingConstructors.remove(constructor);
                  }
                  return;
               }
            }
            if (metalneeded || resourceManager.getCurrentMetal() < resourceManager.getMetalStorage() / 5 )
            {
               TerrainPos reclaimpos = reclaimHelper.GetNearestReclaim( constructor );
               if( reclaimpos != null )
               {
                  giveOrderWrapper.Reclaim( constructor, reclaimpos, 100 );
                  return;
               }
               if (BuildMex(constructor))
               {
                  logfile.WriteLine("building mex");
                  if (AssistingConstructors.containsKey(constructor))
                  {
                     AssistingConstructors.remove(constructor);
                  }
                  return;
               }
            }


            logfile.WriteLine("offering assistance");
            OfferAssistance(constructor);
            return;
         }
         Order ordertodo = possibleorders.get(random.nextInt(possibleorders.size()));
         String metalextractorname = buildTree.listToOurTeamsUnitName( config.getBasicmetalextractorunitnames() );
         if (ordertodo.unitname.equals(metalextractorname ) )
         {
            BuildMex(constructor);
            if (AssistingConstructors.containsKey(constructor))
            {
               AssistingConstructors.remove(constructor);
            }
         }
         else
         {
            //ordertodo.unitsunderconstruction += 1;
            deftobuild = buildTable.getUnitDefByName(ordertodo.unitname);
            TerrainPos pos = BuildUnit(constructor, ordertodo.unitname);
            Ownership.IOrder ownershiporder = ownership.RegisterBuildingOrder(
                  new BuildListener(),
                  constructor,
                  deftobuild, pos);
            logfile.WriteLine("building: " + ordertodo.unitname);
            if( unitsUnderContructionByOrder.get( ordertodo ) == null ) {
               unitsUnderContructionByOrder.put(  ordertodo, new ArrayList<IOrder>() );
            }
            unitsUnderContructionByOrder.get( ordertodo ).add(ownershiporder);
            if (AssistingConstructors.containsKey(constructor))
            {
               AssistingConstructors.remove(constructor);
            }
         }
      }
   }

   void CheckIdleUnits()
   {
     // logfile.WriteLine("Workflow.CheckIdleUnits()");
      for( Unit unit : unitController.units ) {
         // foreach (KeyValuePair<int, UnitDef> kvp in unitController.UnitDefByDeployedId)
         // logfile.WriteLine("Checking " + kvp.Key);
         UnitDef unitdef = unit.getDef();
         //logfile.WriteLine( " considering " + unit.getUnitId() + " " + unit.getDef().getHumanName() + " ..." );
         if (unitdef.isBuilder() )
         {
            //logfile.WriteLine( "... is builder ..." );
            if( unit.getCurrentCommands().size() == 0 )
            {
               //logfile.WriteLine( "... command queue is empty ..." );
               logfile.WriteLine("Unit " + unitdef.getHumanName() + " not busy, calling idle");
               if( AssistingConstructors.containsKey( unit ) )
               {
                  logfile.WriteLine("removing from assistingconstructors");
                  AssistingConstructors.remove(unit);
               }
               logfile.WriteLine( "... calling unitidle ..." );
               new GameListener().UnitIdle(unit);
            }
            if (!ActiveConstructors.contains(unit))
            {
               logfile.WriteLine("Unit " + unitdef.getHumanName() + " not in active commanders, calling idle");
               new GameListener().UnitIdle(unit);
            }
         }
      }
   }

//   public class Order
//   {
//      public double priority;
//      public String unitname;
//      public int quantity;
//      public ArrayList<Ownership.IOrder> unitsunderconstruction = new ArrayList<Ownership.IOrder>();
//      public Order(double priority, String unitname, int quantity)
//      {
//         this.priority = priority;
//         this.unitname = unitname;
//         this.quantity = quantity;
//      }
//      @Override
//      public String toString()
//      {
//         int underconstruction = unitsunderconstruction.size();
//         int currentunits = 0;
//         if (unitController.UnitsByName.containsKey(unitname))
//         {
//            currentunits += unitController.UnitsByName.get(unitname).size();
//         }
//         return "Order priority: " + priority + " unitname: " + unitname + " quantity: " + currentunits + "(" + 
//         underconstruction + ") / " + quantity;
//      }
//   }

   // note to self: need to add weighting probably
   // need to add filters/position requireemtns for radars, metal extractors
//   public void BuildUnit(double priority, String name, int quantity)
//   {
//      orders.add( new Order(priority, name, quantity));
//   }

   List<String> energyunits = new ArrayList<String>();

   public void AddEnergyUnit(String energyunitname)
   {
      energyunits.add(energyunitname);
   }

   List<String> metalunits = new ArrayList<String>();

   public void AddMetalUnit(String metalunitname)
   {
      metalunits.add(metalunitname);
   }

   // this causes all units to offer assistance, even static ones
   // which means units coming out of the factories will automatically assist,
   // which isn't always what we want, but anyway...
   void OfferAssistance(Unit constructor)
   {
     // if( constructor.)
      Unit bestconstructor = null;
      double bestsquareddistance = 1000000000;
      TerrainPos constructorpos = unitController.getPos( constructor );
      for (Unit activeconstructor : ActiveConstructors)
      {
         TerrainPos activeconstructorpos = unitController.getPos( activeconstructor );
         if (activeconstructorpos != null)
         {
            float thissquareddistance = constructorpos.GetSquaredDistance( activeconstructorpos);
            if (thissquareddistance < bestsquareddistance)
            {
               bestconstructor = activeconstructor;
               bestsquareddistance = thissquareddistance;
            }
         }
      }
      if (bestconstructor != null)
      {
         logfile.WriteLine("unit to assist should be " + bestconstructor);
         if( !AssistingConstructors.containsKey( constructor ) )
         {
            logfile.WriteLine("assisting " + bestconstructor);
            giveOrderWrapper.Guard(constructor, bestconstructor);
            AssistingConstructors.put(constructor, bestconstructor);
         }
         else if( AssistingConstructors.get( constructor ) != bestconstructor )
         {
            logfile.WriteLine("assisting " + bestconstructor);
            giveOrderWrapper.Guard(constructor, bestconstructor);
            AssistingConstructors.put( constructor, bestconstructor );
         }
      }
   }

   boolean BuildConstructionVehicle( Unit constructor, UnitDef constructordef)
   {
      UnitDef deftobuild = null;
      String basicConstructionVehicleName = buildTree.listToOurTeamsUnitName( config.getBasicconstructionvehicleunitnames() );
      if( basicConstructionVehicleName == null ) {
         return false;
      }
      if( buildTree.CanBuild( constructordef.getName().toLowerCase(), basicConstructionVehicleName ) )
      {
         deftobuild = buildTable.getUnitDefByName(basicConstructionVehicleName);
      }
      else if (buildTree.CanBuild(constructordef.getName().toLowerCase(), basicConstructionVehicleName))
      {
         deftobuild = buildTable.getUnitDefByName(basicConstructionVehicleName);
      }
      else
      {
         return false;
      }
      TerrainPos pos = BuildUnit(constructor, deftobuild.getName().toLowerCase());
      Ownership.IOrder ownershiporder = ownership.RegisterBuildingOrder(
            new BuildListener(),
            constructor,
            deftobuild, pos);
      logfile.WriteLine("building: " + deftobuild.getName().toLowerCase());
      //ordertodo.unitsunderconstruction.Add(ownershiporder);
      if (AssistingConstructors.containsKey(constructor))
      {
         AssistingConstructors.remove(constructor);
      }
      return true;
   }

   TerrainPos BuildUnit(Unit constructor, String targetunitname)
   {
      csai.DebugSay("workflow, building " + targetunitname);
      UnitDef targetunitdef = buildTable.getUnitDefByName(targetunitname);
      UnitDef constructordef = constructor.getDef();
      if (unitDefHelp.IsMobile(constructordef))
      {
         logfile.WriteLine("constructor is mobile");
         TerrainPos constructorpos = unitController.getPos( constructor );
         TerrainPos buildsite = buildPlanner.ClosestBuildSite(targetunitdef,
               constructorpos,
               3000, 2);
         buildsite = TerrainPos.fromAIFloat3( aicallback.getMap().findClosestBuildSite(
               targetunitdef, buildsite.toAIFloat3(), 1400, 0, 0) );
         logfile.WriteLine("constructor location: " + constructorpos + " Buildsite: " + buildsite + " target item: " + targetunitdef.getHumanName());
         if (!ActiveConstructors.contains(constructor))
         {
            ActiveConstructors.add(constructor);
         }
         drawingUtils.DrawUnit(targetunitname.toUpperCase(), buildsite, 0.0f, 1, aicallback.getTeamId(), true, true);
         giveOrderWrapper.BuildUnit(constructor, targetunitname, buildsite);
         return buildsite;
      }
      else
      {
         TerrainPos factorypos = unitController.getPos( constructor );
         logfile.WriteLine("factory location: " + factorypos + " target item: " + targetunitdef.getHumanName());
         if (!ActiveConstructors.contains(constructor))
         {
            ActiveConstructors.add(constructor);
         }
         drawingUtils.DrawUnit(targetunitdef.getName().toUpperCase(), factorypos, 0.0f, 200, aicallback.getTeamId(), true, true);
         giveOrderWrapper.BuildUnit(constructor, targetunitname );
         return factorypos;
      }
   }

   boolean BuildMex(Unit constructor)
   {
//      String metalextractorname = config.getBasicmetalextractorunitname();
      String metalextractorname = buildTree.listToOurTeamsUnitName( config.getBasicmetalextractorunitnames() );
      if( metalextractorname == null ) {
         return false;
      }
      if( !buildTree.CanBuild( constructor.getDef().getName().toLowerCase(),
            metalextractorname ) )
      {
         return false;
      }
      UnitDef unitdef = buildTable.getUnitDefByName(metalextractorname);
      TerrainPos buildsitefrommetal = metal.GetNearestMetalSpot(
            unitController.getPos( constructor ) );
      TerrainPos buildsite = TerrainPos.fromAIFloat3( aicallback.getMap().findClosestBuildSite(
            unitdef, buildsitefrommetal.toAIFloat3(), 100, 0, 0) );
      logfile.WriteLine( "Workflow.buildmex, metal says build at " + buildsitefrommetal + " map changes this to: " + buildsite );
      if (!ActiveConstructors.contains(constructor))
      {
         ActiveConstructors.add(constructor);
      }
      //aicallback.GiveOrder(constructorid, new Command(-unitdef.id, buildsite.ToDoubleArray()));
      giveOrderWrapper.BuildUnit(constructor, unitdef.getName(), buildsite);
      return true;
   }

   boolean BuildSolarCell(Unit constructor)
   {
      String energyextractorunitname = buildTree.listToOurTeamsUnitName( config.getBasicenergyextractorunitnames() );
      if( energyextractorunitname == null ) {
         return false;
      }
      if (!buildTree.CanBuild(constructor.getDef().getName().toLowerCase(),
            energyextractorunitname))
      {
         return false;
      }
      UnitDef unitdef = buildTable.getUnitDefByName(energyextractorunitname);
      TerrainPos buildsite = buildPlanner.ClosestBuildSite(unitdef,
            unitController.getPos( constructor ),
            3000, 2);
      buildsite = TerrainPos.fromAIFloat3( aicallback.getMap().findClosestBuildSite(
            unitdef, buildsite.toAIFloat3(), 100, 0, 0) );
      if (!ActiveConstructors.contains(constructor))
      {
         ActiveConstructors.add(constructor);
      }
      //aicallback.GiveOrder(constructorid, new Command(-unitdef.id, buildsite.ToDoubleArray()));
      giveOrderWrapper.BuildUnit(constructor, unitdef.getName(), buildsite );
      return true;
   }

   class BuildListener implements Ownership.IBuilder {
      @Override
      public void UnitFinished(Ownership.IOrder ownershiporder, Unit unit )
      {
         for (Order order : currentOrders)
         {
            if( unitsUnderContructionByOrder.get( order ) == null ) {
               unitsUnderContructionByOrder.put(  order, new ArrayList<IOrder>() );
            }
            if (unitsUnderContructionByOrder.get( order ).contains(ownershiporder))
            {
               unitsUnderContructionByOrder.get( order ).remove(ownershiporder);
            }
         }
         new GameListener().UnitIdle(unit);
      }
      
      @Override
      public void UnitCreated(Ownership.IOrder order,Unit unit )
      {
      }

      @Override
      public void UnitDestroyed(Ownership.IOrder ownershiporder, Unit unit )
      {
         for (Order order : currentOrders )
         {
            if( unitsUnderContructionByOrder.get( order ) == null ) {
               unitsUnderContructionByOrder.put(  order, new ArrayList<IOrder>() );
            }
            if (unitsUnderContructionByOrder.get( order ).contains(ownershiporder))
            {
               unitsUnderContructionByOrder.get( order ).remove(ownershiporder);
            }
         }
      }
   }
}

