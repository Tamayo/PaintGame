package controllers;


import play.libs.*;

import scala.concurrent.duration.*;
import akka.actor.*;
import static java.util.concurrent.TimeUnit.*;

public class VoteHandel extends UntypedActor {
   
    

	@Override
	public void onReceive(Object arg0) throws Exception {
		// TODO Auto-generated method stub
		Akka.system().scheduler().scheduleOnce(Duration.create(10,SECONDS),getSender(),new PaintGroup.endVote(),Akka.system().dispatcher(),null);
	}
    
}
