package hughai;

import java.util.*;
import java.io.*;

import com.springrts.ai.*;
import com.springrts.ai.command.*;
import com.springrts.ai.oo.*;

import hughai.ui.MainUI;
import hughai.utils.*;
import hughai.utils.TimeHelper.TimeSpan;
import hughai.loader.*;
import hughai.loader.utils.*;
import hughai.test.*;

// Original intentions: Stores options from start script, pumps some into config
//
// Right, we could just have things get properties by strings, but that
// sounds... error-prone... so we could have a list of valid strings,
// or get them by reading one of the lua options files, or
// have a class and pump available options into that class/object
// but what if those values weren't specified in the incoming options?
// give them appropriate defaults? read from config? set them to null?
// let's make them classes for now, and have them default to null
// and: we'll need some higher level options abstraction to choose
// between the config values and the startscript values
// hmmm: we could make anything that is valid in the xml config file
// be a valid startscript option :-O
// then it is fairly easy to make a higher-level abstraction
// in the gui, we could just grey those out or something, signal
// somehow that they are being overridden
public class OptionsFromStartScript {
//   public class Options {
//      Integer difficultyLevel = null;
//      Boolean debugOn = null;
//   }
   
   PlayerObjects playerObjects;
   
   final HashMap<String,String> optionValues = new HashMap<String,String>();
   final HashMap<String,String> info = new HashMap<String,String>();
   
   public Collection<String> getOptionKeys() {
      return optionValues.keySet(); 
   }
   
   // returns null if option doesn't exist
   public String getOption( String optionName ) {
      return optionValues.get( optionName.toLowerCase() ); 
   }
   
   // returns defaultValue if option doesn't exist
   public String getOption( String optionName, String defaultValue ) {
      if( optionValues.containsKey( optionName.toLowerCase() ) ) {
         return optionValues.get( optionName.toLowerCase() ); 
      }
      return defaultValue;
   }
   
   public OptionsFromStartScript( PlayerObjects playerObjects ) {
      this.playerObjects = playerObjects;
      
      init();
   }
   
   void debug( Object message ) {
      playerObjects.getLogFile().WriteLine( "" + this.getClass().getSimpleName() + ": " + message );
   }
   
   void init() {
      Info inf = playerObjects.getAicallback().getSkirmishAI().getInfo();
      int numInfo = inf.getSize();
      for (int i=0; i < numInfo; i++) {
              String key = inf.getKey(i);
              String value = inf.getValue(i);
              debug( "infovalue: " + key + " = " + value );
              info.put(key, value);
      }

      OptionValues opVals = playerObjects.getAicallback().getSkirmishAI().getOptionValues();
      int numOpVals = opVals.getSize();
      for (int i=0; i < numOpVals; i++) {
              String key = opVals.getKey(i);
              String value = opVals.getValue(i);
              debug( "optionvalue: " + key + " = " + value );
              optionValues.put(key, value);
      }      
   }
}   

