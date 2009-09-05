package hughai.loader.utils;

import java.util.*;

// can store things across reloads
// Note: any ai classes that you use here will NOT work across reloads
// since their reloaded classes will not match
// if you use interfaces to access the ai classes, that will work ok however.
public class TransLoadStorage {
   static TransLoadStorage instance = new TransLoadStorage();
   public static TransLoadStorage getInstance() { return instance; }
   
   public TransLoadStorage() {
      System.out.println("TransLoadStorage()");
   }
   
   @Override
   protected void finalize() {
      System.out.println( this.getClass().getSimpleName() + ".finalize()");
   }

   HashMap<String,Object> attributes = new HashMap<String, Object>();
   
   public void setAttribute( String key, Object value ) {
      synchronized( attributes ) {
         attributes.put( key, value );
      }
   }
   
   public Object getAttribute( String key ) {
      synchronized( attributes ) {
         return attributes.get( key );
      }
   }
}
