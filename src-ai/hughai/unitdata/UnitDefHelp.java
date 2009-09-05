// copyright Hugh Perkins 2006, 2009

// Adapted from AF's C++ version 2006, ported to C# 2006, then to Java in 2009

package hughai.unitdata;

import com.springrts.ai.oo.*;

import hughai.PlayerObjects;
import hughai.ResourceManager;
import hughai.*;
import hughai.utils.LogFile;


public class UnitDefHelp
{
   OOAICallback aicallback;
   ResourceManager resourceManager;
   LogFile logfile;

   public UnitDefHelp( PlayerObjects playerObjects )
   {
      this.aicallback = playerObjects.getAicallback();
      this.resourceManager = playerObjects.getResourceManager();
      this.logfile = playerObjects.getLogFile();
   }

   public boolean IsEnergy(UnitDef ud){
      if(ud.isNeedGeo()){
         return false;
      }
      if(ud.getUpkeep(resourceManager.getEnergyResource()) < -1) return true;
      if((ud.getWindResourceGenerator(resourceManager.getEnergyResource()) > 0)
            &&(aicallback.getMap().getMaxWind() > 9 )) return true;
      if(ud.getTidalResourceGenerator(resourceManager.getEnergyResource()) > 0 ) return true;
      if(ud.getMakesResource(resourceManager.getEnergyResource()) > 5) return true;
      return false;
   }

   public boolean IsMobile(UnitDef ud){
      if(ud.getSpeed() < 1) return false;
      if(ud.getMoveData() != null) return true;
      if(ud.isAbleToFly() ) return true;
      return false;
   }

   public boolean IsBoat( UnitDef unitdef )
   {
      if( IsMobile( unitdef ) && unitdef.getMinWaterDepth() > 0 )
      {
         return true;
      }
      return false;
   }

   public boolean IsConstructor( UnitDef unitdef )
   {
      if(unitdef.getBuildOptions().size() == 0) return false;
      return( unitdef.isBuilder() && IsMobile( unitdef ) );
   }

   public boolean IsFactory(UnitDef ud){
      if (ud.getBuildOptions().size() == 0) return false;
      if(ud.getType().toLowerCase() == "factory" ) return true;
      return ud.isBuilder() && !IsMobile(ud);
   }

   public boolean IsGroundMelee( UnitDef ud )
   {
      return IsMobile( ud ) && ud.isAbleToAttack();
   }

   public boolean IsMex(UnitDef ud){
     // logfile.WriteLine( "unitdefhelp.ismex, ud type: " + ud.getType() );
      //ud.
      //if(ud.getType().toLowerCase() == "metalextractor"){
       //  return true;
      //}
      return ( ud.getExtractsResource( resourceManager.getMetalResource() ) > 0 );
      //return false;
   }

   public boolean IsAirCraft(UnitDef ud){
      if(ud.getType().toLowerCase() == "fighter")	return true;
      if(ud.getType().toLowerCase() == "bomber")	return true;
      if(ud.isAbleToFly()&&(ud.getMoveData() == null)) return true;
      return false;
   }

   public boolean IsGunship(UnitDef ud){
      if(IsAirCraft(ud)&&ud.isHoverAttack()) return true;
      return false;
   }

   public boolean IsFighter(UnitDef ud){
      if(IsAirCraft(ud)&& !ud.isHoverAttack()) return true;
      return false;
   }

   public boolean IsBomber(UnitDef ud){
      if(IsAirCraft(ud)&&(ud.getType().toLowerCase() == "bomber" )) return true;
      return false;
   }        
}

