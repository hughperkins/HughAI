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
// ======================================================================================
//

package hughai.basictypes;

import org.w3c.dom.Element;

import com.springrts.ai.*;

// like AIFloat3 class, with vector operations etc added
public class Float3
{
   public float x;
   public float y;
   public float z;

   public Float3(){}

   public Float3( float x, float y, float z )
   {
      this.x = x;
      this.y = y;
      this.z = z;
   }

   public Float3(AIFloat3 float3)
   {
      this.x = float3.x;
      this.y = float3.y;
      this.z = float3.z;
   }

   public static Float3 fromAIFloat3( AIFloat3 float3 ){
      return new Float3( float3 );
   }

   public float[] ToDoubleArray(){
      return new float[] { x, y, z };
   }

   public AIFloat3 toAIFloat3() {
      return new AIFloat3((float)x,(float)y,(float)z);
   }

   public Float3 add( Float3 second ) {
      return new Float3(x + second.x, y + second.y, z + second.z);
   }

   //	public Float3 add( AIFloat3 second ) {
   //		return new Float3(x + second.x, y + second.y, z + second.z);
   //	}

   public Float3 subtract( Float3 second ) {
      return new Float3(x - second.x, y - second.y, z - second.z);
   }

   //	public Float3 subtract( AIFloat3 second ) {
   //		return new Float3(x - second.x, y - second.y, z - second.z);
   //	}

   public Float3 multiply( float scalar ) {
      return new Float3(x * scalar, y * scalar, z * scalar );
   }

   public Float3 divide( float scalar ) {
      return new Float3(x / scalar, y / scalar, z / scalar );
   }

   public void Normalize()
   {
      float magnitude = (float)Math.sqrt( x * x + y * y + z * z );
      if( magnitude > 0.00001 )
      {
         x /= magnitude;
         y /= magnitude;
         z /= magnitude;
      }
   }

   // ported from Spring's float3.h
   public Float3 Cross( Float3 f )
   {
      return new Float3( y*f.z - z*f.y,
            z*f.x - x*f.z,
            x*f.y - y*f.x  );
   }

   public float GetSquaredDistance( Float3 two )
   {
      return ( two.x - x ) * ( two.x - x ) 
      + ( two.y - y ) * ( two.y - y ) 
      + ( two.z - z ) * ( two.z - z );
   }	
   public void writeToXmlElement( Element element )
   {
      element.setAttribute( "x", "" + x );
      element.setAttribute( "y", "" + y );
      element.setAttribute( "z", "" + z );
   }
   public void readFromXmlElement( Element element )
   {
      x = Float.parseFloat( element.getAttribute( "x" ) );
      y = Float.parseFloat( element.getAttribute( "y" ) );
      z = Float.parseFloat( element.getAttribute( "z" ) );
   }

   @Override
   public String toString(){
      return "(" + x + "," + y + "," + z + ")";
   }

   // hashcode method from AIFloat3
   @Override
   public int hashCode() {

      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + Float.floatToIntBits(x);
      result = prime * result + Float.floatToIntBits(y);
      result = prime * result + Float.floatToIntBits(z);
      return result;
   }
   // equals method from AIFloat3
   @Override
   public boolean equals(Object obj) {
      //       System.out.println("Float3.equals()");

      if (this == obj) {
         return true;
         //        } else if (!super.equals(obj)) {
         //            return false;
      } else if( obj == null ) {
         return false;
      } else if (getClass() != obj.getClass()) {
         return false;
      }

      //        System.out.println("Float3: comparing values");
      Float3 other = (Float3) obj;
      if( Math.abs( x - other.x ) > 0.001 ) { return false; }
      //        System.out.println("Float3: x matches");
      if( Math.abs( y - other.y ) > 0.001 ) { return false; }
      //        System.out.println("Float3: y matches");
      if( Math.abs( z - other.z ) > 0.001 ) { return false; }
      //        System.out.println("Float3: all ok");
      return true;
      //        if (Float.floatToIntBits(x) != Float.floatToIntBits(other.x)) {
      //            return false;
      //        } else if (Float.floatToIntBits(y) != Float.floatToIntBits(other.y)) {
      //            return false;
      //        } else if (Float.floatToIntBits(z) != Float.floatToIntBits(other.z)) {
      //            return false;
      //        } else {
      //            return true;
      //        }
   }
}

