package hughai;

import java.util.*;

import com.springrts.ai.oo.*;

public class ResourceManager {
	OOAICallback aicallback;
	List<Resource> resources;
	
	Resource metalResource;
	Resource energyResource;
	
	public ResourceManager( OOAICallback aicallback ){
		this.aicallback = aicallback;
		this.resources = aicallback.getResources();
		for( Resource resource : resources ) {
			if( resource.getName().toLowerCase().contains("metal")){
				metalResource = resource;
			}
			if( resource.getName().toLowerCase().contains("energy")){
				energyResource = resource;
			}
		}
	}
	
	public float getCurrentMetal(){
		return aicallback.getEconomy().getCurrent(metalResource);
	}
	
	public float getMetalStorage(){
		return aicallback.getEconomy().getStorage(metalResource);
	}
		
	public float getCurrentEnergy(){
		return aicallback.getEconomy().getCurrent(energyResource);
	}

	public float getEnergyStorage(){
		return aicallback.getEconomy().getStorage(energyResource);
	}

	public Resource getMetalResource() {
		return metalResource;
	}

	public Resource getEnergyResource() {
		return energyResource;
	}
}
