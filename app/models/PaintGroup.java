package models;

import play.mvc.*;


import play.libs.*;
import play.libs.F.*;

import scala.concurrent.Await;
import scala.concurrent.duration.Duration;
import akka.actor.*;
import static akka.pattern.Patterns.ask;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;



import java.util.*;

import static java.util.concurrent.TimeUnit.*;

//Actor that that will control each group
public class PaintGroup extends UntypedActor {
    
    // Groups
    static ActorRef defaultRoom = Akka.system().actorOf(Props.create(PaintGroup.class));
    static ActorRef secondRoom = Akka.system().actorOf(Props.create(PaintGroup.class));
    static ActorRef thirdRoom = Akka.system().actorOf(Props.create(PaintGroup.class));
    
    /**
     * Join the default room.
     */
    public static void join(final String username,final int roomNumber, WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out) throws Exception{
        
    	ActorRef roomToJoin = null;
    	switch (roomNumber){
    	case 1:
    		roomToJoin = defaultRoom;
    		break;
    	case 2:
    		roomToJoin = secondRoom;
    		break;
    	case 3:
    		roomToJoin = thirdRoom;
    		break;
    	}
        String result = (String)Await.result(ask(roomToJoin,new Join(username, out), 1000), Duration.create(1, SECONDS));
        final ActorRef joinedRoom = roomToJoin;
        if("OK".equals(result)) {
            in.onMessage(new MessageParser<JsonNode>(roomToJoin,username));
            in.onClose(new Callback0() {
               public void invoke() {
                   
                   // Send a Quit message to the room.
                   joinedRoom.tell(new Quit(username), null);
                   
               }
            });
            ObjectNode notifySuccess = Json.newObject();
            notifySuccess.put("type","Success");
            out.write(notifySuccess);
            
        } else {
            
            // Cannot connect, create a Json error.
            ObjectNode error = Json.newObject();
            error.put("error", result);
            
            // Send the error to the socket.
            out.write(error);
            
        }
        
    }
    
    // Members of this room.
    Map<String, WebSocket.Out<JsonNode>> members = new HashMap<String, WebSocket.Out<JsonNode>>();
    
    public void onReceive(Object message) throws Exception {
        
        if(message instanceof Join) 
        {
            Join join = (Join)message;
            
            //Checks to make sure someone is not playing using this current name
            if(members.containsKey(join.username)) {
                getSender().tell("Name is already used, try another name", getSelf());
            } else {
                members.put(join.username, join.channel);
                notifyAll("Join", join.username, "has entered the room");
                getSender().tell("OK", getSelf());
            } 
        } 
        else if(message instanceof Guess)  
        {
            
            Guess currentGuess = (Guess)message;
            //Check is current Guess is correct, then send out too all others in chat Room
            notifyAll("Guess", currentGuess.username, currentGuess.text);
            
        } 
        else if(message instanceof Quit)  
        {
            
            // Received a Quit message
            Quit quit = (Quit)message;
            
            members.remove(quit.username);
            
            notifyAll("Quit", quit.username, "has left the room");
        
        } 
        else if(message instanceof Vote)
        {
        	Vote vote = (Vote)message;
        	notifyAll("Vote",vote.username,"voted for " + vote.vote);
        }
        else {
            unhandled(message);
        }
        
    }
    
    //Send Message too all members of current group
    public void notifyAll(String type, String user, String text) {
        for(WebSocket.Out<JsonNode> channel: members.values()) {
            
            ObjectNode event = Json.newObject();
            event.put("type", type);
            event.put("user", user);
            event.put("message", text);
            
            ArrayNode m = event.putArray("members");
            for(String u: members.keySet()) {
                m.add(u);
            }
            
            channel.write(event);
        }
    }
    
    public void giveWord(String topic,String word, String user)
    {
    	WebSocket.Out<JsonNode> userOut = members.get(user);
    	ObjectNode wordObject = Json.newObject();
    	wordObject.put("topic",topic);
    	wordObject.put("user",user);
    	wordObject.put("word",word);
    	userOut.write(wordObject);
    }
    
    //Recieve Message Types
    public static class Join {
        
        final String username;
        final WebSocket.Out<JsonNode> channel;
        
        public Join(String username, WebSocket.Out<JsonNode> channel) {
            this.username = username;
            this.channel = channel;
        }
        
    }
    
    public static class Guess {
        
        final String username;
        final String text;
        
        public Guess(String username, String text) {
            this.username = username;
            this.text = text;
        }
        
    }
    public static class Vote {
    	final String username;
    	final String vote;
    	public Vote(String name, String newVote)
    	{
    		username = name;
    		vote = newVote;
    	}
    }
    public static class Quit {
        
        final String username;
        
        public Quit(String username) {
            this.username = username;
        }
        
    }
    
}