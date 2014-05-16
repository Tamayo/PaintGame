package controllers;

import play.*;
import play.mvc.*;
import play.libs.*;

import scala.concurrent.duration.*;
import akka.actor.*;

import com.fasterxml.jackson.databind.JsonNode;

import static java.util.concurrent.TimeUnit.*;

public class Consensus {
	
	public Consensus(ActorRef myRoom)
	{
		Akka.system().scheduler().schedule(
	            Duration.create(1, SECONDS),
	            Duration.create(1, SECONDS),
	            myRoom,
	            new PaintGroup.CheckLeader(),
	            Akka.system().dispatcher(),
	            /** sender **/ null
	        );
	}
}
