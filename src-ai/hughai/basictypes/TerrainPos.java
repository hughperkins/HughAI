package hughai.basictypes;

import com.springrts.ai.AIFloat3;

// terrainpos represents a pos on the terrain itself,
// ie multiplied by squaresize, eg from (0,0,0) to (4000,0,4000) typically
public class TerrainPos extends Float3 {
   public TerrainPos() {}
   public TerrainPos( Float3 float3 ) {
      x = float3.x; y = float3.y; z = float3.z;
   }
   public TerrainPos( float x, float y, float z ) {
      super( x, y, z );
   }
   public static TerrainPos fromAIFloat3( AIFloat3 aifloat3 ) {
      return new TerrainPos( aifloat3.x, aifloat3.y, aifloat3.z );
   }
   
   public TerrainPos add( TerrainPos second ) {
      return new TerrainPos( super.add( second ) );
   }
   public TerrainPos subtract( TerrainPos second ) {
      return new TerrainPos( super.subtract( second ) );
   }
   public TerrainPos multiply( float scalar ) {
      return new TerrainPos( super.multiply( scalar ) );
   }
   public TerrainPos divide( float scalar ) {
      return new TerrainPos( super.divide( scalar ) );
   }
}
