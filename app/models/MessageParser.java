package models;
import models.PaintGroup.*;
import play.libs.F.*;
import akka.actor.*;

import com.fasterxml.jackson.databind.JsonNode;

public class MessageParser<A> implements Callback<A>  {
	ActorRef myRoom;
	String username;
	public MessageParser(ActorRef newRoom, String newName)
	{
		myRoom = newRoom;
		username = newName;
	}
	@Override
	public void invoke(A event) throws Throwable {
		// TODO Auto-generated method stub
		JsonNode node = (JsonNode)event;
		 String messageType = node.get("type").asText();
        //Checks Message type and sends appropriate message to actor
		 switch(messageType){
		 case "Guess":
			 myRoom.tell(new Guess(username, node.get("message").asText()), null);
		 	break;
		 case "Vote":
			 myRoom.tell(new Vote(username, node.get("message").asText()), null);
			 break;
		 }
 	   
	}

}
