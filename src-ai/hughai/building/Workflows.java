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
      @ReflectionHelper.ListTypeInfo(Order.class)
      public ArrayList<Order> orders = new ArrayList<Order>();

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
      this.workflowdirectory = csai.getAIDirectoryPath();
   }

   public void Init () {
      logfile.WriteLine( "workflows.init()" );
      reflectionHelper = new ReflectionHelper<Workflow>( playerObjects );
      for( File file : new File(this.workflowdirectory).listFiles() ) {
         String filename = file.getName().toLowerCase();
         if( filename.startsWith( modname )
               && filename.contains("workflow") ) {
            logfile.WriteLine( "Workflow file found: " + filename );
            Workflow workflow = new Workflow();
            reflectionHelper.loadObjectFromFile( filename, workflow );
         }
      }
      
   }
}
