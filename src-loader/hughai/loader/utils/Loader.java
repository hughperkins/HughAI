// This was adapted from http://blog.snippetparty.com/2009/05/simple-dynamic-loadingunloading-of-code-in-java/ by Hugh Perkins 2009
// You can use and distribute it under public domain or GPLv2, or GPLv3, at your choice.

package hughai.loader.utils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import com.springrts.ai.oo.OOAI;

public class Loader {
   // from http://blog.snippetparty.com/2009/05/simple-dynamic-loadingunloading-of-code-in-java/, by Sune
   // Note that if the class to be loaded is already available
   // on the classpath, THEN IT WILL BE LOADED BY THE SYSTEMLOADER
   // instead of this, and dynamic loading will fail
   public static IHughAI loadOOAI( URL[] locations, String name) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
      TransLoadStorage.getInstance(); // load transstorage instance / class.
      ClassLoader baseClassLoader = Loader.class.getClassLoader();
      if( baseClassLoader == null ) {
         System.out.println("using system classloader as base");
         baseClassLoader = ClassLoader.getSystemClassLoader();
      } else {
         System.out.println("using our classloader as base");         
      }
      URLClassLoader newclassloader = new URLClassLoader(
            locations, baseClassLoader );
      Class<?> cls = newclassloader.loadClass(name);
      System.out.println("loaded class.");
      System.out.println("loaded class: " + cls.getName());
      if (!IHughAI.class.isAssignableFrom(cls)) {
         throw new RuntimeException("Invalid class");
      }
      Object newInstance = cls.newInstance(); 
      System.out.println("Got object instance.");
      System.out.println("Got object instance: " + newInstance.getClass().getName());
      IHughAI ooai = (IHughAI)newInstance;
      System.out.println("Cast instance ok.");
      return ooai;
   }
}

