package hughai.loader.utils;

import com.springrts.ai.oo.*;

import hughai.loader.*;

public interface IHughAI extends OOAI {
   public void Shutdown();
   public void setHughAILoader( HughAILoader hughAILoader );
}
