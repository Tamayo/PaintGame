package models;
import models.PaintGroup.*;
import play.libs.F.*;
import akka.actor.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

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
        //Checks Message type and sends appropriate message to actorOf
		 //System.out.println(messageType);
		 
		 switch(messageType){
			 case "Guess":
				 myRoom.tell(new Guess(username, node.get("message").asText()), null);
			 	break;
			 case "Vote":
				 myRoom.tell(new Vote(username, node.get("message").asText()), null);
				 break;
			 case "Update":
				 myRoom.tell(new Update(node.get("x").asInt(),node.get("y").asInt(),node.get("color").asText(),username),null);
				 break;
			 case "Init":
				 myRoom.tell(new InitRequest(node.get("user").asText(),node.get("message")),null);
				 break;
			 case "InitRequest":
				 myRoom.tell(new Init(username), null);
				 break;
		 }
 	   
	}

}
