package hughai.ui;

import java.util.*;

import hughai.*;
import hughai.utils.*;

public class WelcomeMessages {
   PlayerObjects playerObjects;
   
   Config config;
   
   List<String> welcomeMessages;
   int welcomeMessageIntervalFrames;
   
   int initialFrameNumber = -1;  // so we can test without reloading Spring...
   
   public WelcomeMessages( PlayerObjects playerObjects ) {
      this.playerObjects = playerObjects;
      config = playerObjects.getConfig();
      
      welcomeMessages = config.getWelcomeMessages();
      welcomeMessageIntervalFrames = config.getWelcomeMessageSecondsInterval() * 30;
      
      //initialFrameNumber = playerObjects.getFrameController().getFrame();
      
      playerObjects.getCSAI().registerGameListener( new GameListener() );
   }
   
   class GameListener extends GameAdapter {
      @Override
      public void Tick( int frame ) {
         if( initialFrameNumber == -1 ) {
            initialFrameNumber = frame;
         }
         frame = frame - initialFrameNumber;
         if( ( frame % welcomeMessageIntervalFrames ) == 0 ) {
            int messageindex = ( frame / welcomeMessageIntervalFrames );
            if( ( messageindex >= 0 ) && ( messageindex < welcomeMessages.size() ) ) {
               playerObjects.getCSAI().sendTextMessage( welcomeMessages.get( messageindex ) );
            }
         }
      }
   }
}
