package hughai.building;

import java.util.*;
import java.util.Map;
import java.io.*;

import com.springrts.ai.*;
import com.springrts.ai.oo.*;

import hughai.*;
import hughai.basictypes.*;
import hughai.mapping.*;
import hughai.unitdata.*;
import hughai.utils.*;
import hughai.controllers.level1.*;
import hughai.controllers.level2.*;

public class Workflows {
   public static class Workflow {
      String name;
      @ReflectionHelper.ListTypeInfo(Order.class)
      public ArrayList<Order> orders = new ArrayList<Order>();

      public String getName() { return name; }

      public static class Order {
         public Order(){}
         public Order( double priority, String unitname, int quantity ) {
            this.priority = priority;
            this.unitname = unitname;
            this.quantity = quantity;
         }
         public double priority;
         public String unitname;
         public int quantity;
      }

      public ArrayList<Order> getOrders() {
         return orders;
      }

      public void setOrders( ArrayList<Order> orders ) {
         this.orders = orders;
      }

      public void setName( String name ) {
         this.name = name;
      }
   }

   HashMap<String,Workflow> workflowsByName = new HashMap<String, Workflow>();

   PlayerObjects playerObjects;
   LogFile logfile;
   CSAI csai;
   OOAICallback aicallback;

   ReflectionHelper<Workflow> reflectionHelper;

   String modname;
   String workflowdirectory;

   public Workflows( PlayerObjects playerObjects ) {
      this.playerObjects = playerObjects;
      this.logfile = playerObjects.getLogFile();
      this.csai = playerObjects.getCSAI();
      this.aicallback = playerObjects.getAicallback();

      this.modname = aicallback.getMod().getShortName().toLowerCase();
      this.workflowdirectory = csai.getAIDirectoryPath() + aicallback.getMod().getShortName() + "_workflows" 
         + File.separator;
      new File( workflowdirectory ).mkdirs();
   }

   public void Init () {
      logfile.WriteLine( "workflows.init()" );
      reflectionHelper = new ReflectionHelper<Workflow>( playerObjects );
      for( File file : new File(this.workflowdirectory).listFiles() ) {
         String filename = file.getName().toLowerCase();
         String workflowname = filename.split(".")[0]; // remove extension
         logfile.WriteLine( "Workflow file found: " + filename );
         Workflow workflow = new Workflow();
         reflectionHelper.loadObjectFromFile( filename, workflow );
         workflow.setName( workflowname );
         workflowsByName.put( workflow.getName(), workflow );
      }
      if( workflowsByName.size() == 0 ) {
         csai.sendTextMessage( "No workflow config files found for mod " + 
               aicallback.getMod().getHumanName() + ".  Creating one: " + this.workflowdirectory
                  + "dummy.xml");
         Workflow workflow = new Workflow();
         workflow.setName( "dummy" );
         workflow.getOrders().add( new Workflow.Order( 1.5, "armstump", 10 ) );
         workflow.getOrders().add( new Workflow.Order( 1.4, "armsam", 8 ) );
      }
      for( Workflow workflow : workflowsByName.values() ) {
         reflectionHelper.saveObjectToFile( workflow.getName() + ".xml", workflow );
      }
   }
}
