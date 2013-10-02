package hughai.building;

import java.util.*;
import java.util.Map;
import java.io.*;

import com.springrts.ai.*;
import com.springrts.ai.oo.clb.*;

import hughai.*;
import hughai.basictypes.*;
import hughai.mapping.*;
import hughai.unitdata.*;
import hughai.utils.*;
import hughai.utils.ReflectionHelper.*;
import hughai.controllers.level1.*;
import hughai.controllers.level2.*;

public class Workflows {
   public static class Workflow {
      public String workflowName;
      
      @ListTypeInfo(Order.class)
      @CustomClass
      public ArrayList<Order> orders = new ArrayList<Order>();

      public static class Order {
         public Order(){}
         public Order( float priority, String unitname, int quantity ) {
            this.priority = priority;
            this.unitname = unitname;
            this.quantity = quantity;
            int orderref = nextorderref;
            nextorderref++;
         }
         @ReflectionHelper.Exclude
         static int nextorderref = 0;
         @ReflectionHelper.Exclude
         int orderref;
         @Override
         public int hashCode(){
            return orderref;
         }
         
         public float priority;
         public String unitname;
         public int quantity;
      }

      public ArrayList<Order> getOrders() {
         return orders;
      }

      public void setOrders( ArrayList<Order> orders ) {
         this.orders = orders;
      }

      public void setWorkflowName( String workflowName ) {
         this.workflowName = workflowName;
      }
      
      public String getWorkflowName() {
         return workflowName; 
      }
   }

   HashMap<String,Workflow> workflowsByName = new HashMap<String, Workflow>();

   PlayerObjects playerObjects;
   LogFile logfile;
   CSAI csai;
   OOAICallback aicallback;
   Config config;

   //ReflectionHelper<Workflow> reflectionHelper;

   String modname;
   String workflowdirectory;
   
   public HashMap<String,Workflow> getWorkflowsByName() {
      return workflowsByName;
   }

   public Workflows( PlayerObjects playerObjects ) {
      this.playerObjects = playerObjects;
      this.logfile = playerObjects.getLogFile();
      this.csai = playerObjects.getCSAI();
      this.aicallback = playerObjects.getAicallback();
      this.config = playerObjects.getConfig();

      this.modname = aicallback.getMod().getShortName().toLowerCase();
      this.workflowdirectory = csai.getAIDirectoryPath() + "workflows" +
			  File.separator + aicallback.getMod().getShortName() +
			  File.separator;
      new File( workflowdirectory ).mkdirs();
      Init();
   }
   
   String getWorkflowFilePath( String workflowname ) {
      return workflowdirectory + workflowname + ".xml";
   }

   void Init () {
      String defaultWorkflowName = config.getDefaultWorkflowName();
      logfile.WriteLine( "workflows.init()" );
      ReflectionHelper reflectionHelper = new ReflectionHelper( playerObjects );
      for( File file : new File(this.workflowdirectory).listFiles() ) {
         String filename = file.getName().toLowerCase();
         if( filename.startsWith( "." ) ) {  // various hidden files, vim swap, ....
            continue;
         }
         String workflowname = filename.split("\\.")[0]; // remove extension
         logfile.WriteLine( "Workflow file found: " + filename );
         Workflow workflow = new Workflow();
         reflectionHelper.loadObjectFromFile( workflowdirectory + filename, workflow );
         workflow.setWorkflowName( workflowname );
         validate( workflow );
         workflowsByName.put( workflow.getWorkflowName(), workflow );
      }
//      if( workflowsByName.size() == 0 ) {
//         csai.sendTextMessage( "No workflow config files found for mod " + 
//               aicallback.getMod().getHumanName() + ".  Creating one: " + this.workflowdirectory
//                  + defaultWorkflowName + ".xml");
      if( workflowsByName.get( defaultWorkflowName ) == null ) {
         csai.sendTextMessage( "No default workflow config files found for mod " + 
               aicallback.getMod().getHumanName() + ".  Creating one: " + this.workflowdirectory
               + defaultWorkflowName + ".xml");
         Workflow workflow = new Workflow();
         workflow.setWorkflowName( defaultWorkflowName );
         populateSampleWorkflow( workflow );
//         workflow.getOrders().add( new Workflow.Order( 1.5f, "armstump", 10 ) );
//         workflow.getOrders().add( new Workflow.Order( 1.4f, "armsam", 8 ) );
         workflowsByName.put( workflow.getWorkflowName(), workflow );
      }
      for( Workflow workflow : workflowsByName.values() ) {
         reflectionHelper.saveObjectToFile( workflowdirectory + workflow.getWorkflowName() + ".xml", workflow );
      }
   }
   
   // check that all unit names are ok, to save weird crashes later
   void validate( Workflow workflow ) {
      List<String> invalidunitnames = new ArrayList<String>();
      for( Workflow.Order order : workflow.orders ) {
         String thisunitname = order.unitname;
         if( playerObjects.getBuildTable().getUnitDefByName( thisunitname ) == null ) {
            invalidunitnames.add( thisunitname );
         }
      }
      if( invalidunitnames.size() > 0 ) {
         playerObjects.getMainUI().showInfo( "ERROR: the workflow file\n" + getWorkflowFilePath( workflow.workflowName ) + "\ncontains the following invalid unitnames:\n" + invalidunitnames + " .  If you are not sure what this means, you may be using an unsupported mod.  Please restart the game using the BA mod." );
         playerObjects.getLogFile().WriteLine( "ERROR: the workflow file " + getWorkflowFilePath( workflow.workflowName ) + " contains the following invalid unitnames: " + invalidunitnames + " .  If you are not sure what this means, you may be using an unsupported mod.  Please restart the game using the BA mod."  );
      } else {
         playerObjects.getLogFile().WriteLine( "workflow " + workflow.workflowName + " validated."  );
      }
   }
   
   void populateSampleWorkflow( Workflow workflow ) {
      workflow.getOrders().add( new Workflow.Order(2.0f, "armvp", 1) );

      workflow.getOrders().add( new Workflow.Order(2.1f, "armfav", 2) );
      workflow.getOrders().add( new Workflow.Order(2.0f, "armstump", 10) );
      workflow.getOrders().add( new Workflow.Order(2.0f, "armsam", 10) );
      workflow.getOrders().add( new Workflow.Order(1.95f, "armmex", 4) );
      workflow.getOrders().add( new Workflow.Order(1.95f, "armsolar", 4) );
      workflow.getOrders().add( new Workflow.Order(1.9f, "armcv", 3) );
      workflow.getOrders().add( new Workflow.Order(1.8f, "armmstor", 1) );
      workflow.getOrders().add( new Workflow.Order(1.8f, "armavp", 1) );
      workflow.getOrders().add( new Workflow.Order(2.0f, "armbull", 3) );
      workflow.getOrders().add( new Workflow.Order(2.0f, "armmart", 2) );
      workflow.getOrders().add( new Workflow.Order(1.9f, "armseer", 1) ); // experimental
      workflow.getOrders().add( new Workflow.Order(1.7f, "armyork", 3) );
      workflow.getOrders().add( new Workflow.Order(1.7f, "armbull", 3) );
      workflow.getOrders().add( new Workflow.Order(1.7f, "armmart", 2) );
//      workflow.getOrders().add( new Workflow.Order(1.0f, "armmfus", 1) );
      workflow.getOrders().add( new Workflow.Order(0.9f, "armacv", 2) );
      workflow.getOrders().add( new Workflow.Order(0.8f, "armmmkr", 4) );
      workflow.getOrders().add( new Workflow.Order(0.8f, "armarad", 1) );
      workflow.getOrders().add( new Workflow.Order(0.8f, "armestor", 1) );
//      workflow.getOrders().add( new Workflow.Order(0.8f, "armmfus", 8) );
      workflow.getOrders().add( new Workflow.Order(0.7f, "armalab", 1) );
      workflow.getOrders().add( new Workflow.Order(0.7f, "armfark", 2) );

      workflow.getOrders().add( new Workflow.Order(0.6f, "armbull", 20) );
      workflow.getOrders().add( new Workflow.Order(0.6f, "armyork", 20) );
      workflow.getOrders().add( new Workflow.Order(0.6f, "armmart", 20) );
      workflow.getOrders().add( new Workflow.Order(0.5f, "armseer", 1) );
      workflow.getOrders().add( new Workflow.Order(0.5f, "armsjam", 1) );

      workflow.getOrders().add( new Workflow.Order(0.4f, "armmav", 50) ); // experimental
      workflow.getOrders().add( new Workflow.Order(0.3f, "armfark", 4) ); // experimental
      //   workflow.BuildUnit(0.3f, "armpeep", 3); // experimental
      //  workflow.BuildUnit(0.3f, "armap", 1); // experimental
      //workflow.BuildUnit(0.2f, "armbrawl", 50); // experimental
      //workflow.BuildUnit(0.2f, "armaap", 1); // experimental      
   }
}
