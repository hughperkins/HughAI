package hughai.test;

import java.util.*;
import java.util.Map;

import com.springrts.ai.*;
import com.springrts.ai.oo.*;

import hughai.*;
import hughai.EnemyTracker.EnemyAdapter;
import hughai.basictypes.*;
import hughai.mapping.*;
import hughai.packcoordinators.*;
import hughai.unitdata.*;
import hughai.unitdata.UnitController.UnitAdapter;
import hughai.utils.*;
import hughai.ui.*;

public class Debugging {
   PlayerObjects playerObjects;
   
   public Debugging( PlayerObjects playerObjects ) {
      this.playerObjects = playerObjects;
      playerObjects.getMainUI().registerButton( "Say memory usage",
            new ButtonSayMemoryUsage() );
   }
   
   class ButtonSayMemoryUsage implements MainUI.ButtonHandler {
      @Override
      public void go() {
         Runtime runtime = Runtime.getRuntime();
         playerObjects.getCSAI().sendTextMessage( 
               "Memory usage: total=" + Formatting.longToMeg(runtime.totalMemory() ) +
               " free=" + Formatting.longToMeg( runtime.freeMemory() )
               + " used=" + Formatting.longToMeg( runtime.totalMemory() - runtime.freeMemory() ) );
      }
   }
}
