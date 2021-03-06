package controllers;

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
	
	volatile String drawMan;
	volatile boolean voteMode = false;
	Map<String, Integer> votes = new HashMap<String,Integer>();
	HashSet<String> hasVoted = new HashSet<String>();
    // Groups
    static ActorRef defaultRoom = Akka.system().actorOf(Props.create(PaintGroup.class));
    static ActorRef secondRoom = Akka.system().actorOf(Props.create(PaintGroup.class));
    static ActorRef thirdRoom = Akka.system().actorOf(Props.create(PaintGroup.class));
    
    ActorRef myVoter = Akka.system().actorOf(Props.create(VoteHandel.class));
   // static Consensus helpMe;
    
    /**
     * Join the default room.
     */
    public static void join(final String username,final int roomNumber, WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out) throws Exception{
        //if(helpMe == null)
        	//helpMe = new Consensus(defaultRoom);
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
            System.out.println("Dafuq");
            joinedRoom.tell(new Success(username), null);
            
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
            	if(!isDrawing() && !voteMode)
            	{
            		this.drawMan = join.username;
            	}
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
            System.out.println("Current Leader: " + drawMan + " Me: " + quit.username);
            if(drawMan != null && drawMan.equals(quit.username))
            {
            	drawMan = null;
            	getSelf().tell(new initiateVote(), null);
            }        
        } 
        else if(message instanceof Vote)
        {
        	Vote vote = (Vote)message;
        	String myVote = vote.vote;
        	if(voteMode)
        	{
        		if(members.containsKey(myVote) && !hasVoted.contains(vote.username))
        		{
        			hasVoted.add(vote.username);
        			if(votes.containsKey(myVote))
        			{
        				int num = votes.remove(myVote);
        				votes.put(myVote, num+1);
        			}
        			else
        			{
        				votes.put(myVote, 1);
        			}
        			notifyAll("Vote",vote.username,"voted for " + vote.vote);
        		}
        		else
        		{
        			notifyAll("Vote",vote.username,"had an Invalid Vote");
        		}
        	}
        	
        }
        else if(message instanceof Update)
        {
        	Update update = (Update)message;
        	updateAll(update.user,"Update",update.x,update.y,update.color);
        }
        else if(message instanceof Init)
        {
        	Init init = (Init)message;
        	askForCanvas(init.user);
        }
        else if(message instanceof InitRequest)
        {
        	InitRequest initreq = (InitRequest)message;
        	sendCanvas(initreq.user,initreq.imageData);
        }
        else if(message instanceof Success)
        {
        	System.out.println("Success");
        	Success succ = (Success)message;
        	ObjectNode notifySuccess = Json.newObject();
            notifySuccess.put("type","Success");
            if(this.drawMan == succ.user && !this.voteMode)
            {
            	notifySuccess.put("canDraw", 1);
            }
            else
            {
            	notifySuccess.put("canDraw", 0);
            }
            if(this.voteMode)
            {
            	notifySuccess.put("mode", "Vote");
            }
            else
            {
            	notifySuccess.put("mode", "Guess");
            }
            String randomWord = WordController.findRandom();
        	System.out.println(randomWord);
        	notifySuccess.put("word",randomWord);
            WebSocket.Out<JsonNode> out = members.get(succ.user);
            out.write(notifySuccess);
        }
        else if(message instanceof endVote)
        {
        	this.voteMode = false;
        	notifyAll("StateChange","Vote Has Ended","Guess");
        	this.selectNewDraw(-1);
        }
        else if(message instanceof initiateVote)
        {
        	if(members.size()==1)
        	{
        		this.selectNewDraw(1);
        	}
        	else if(members.size() > 1)
        	{
	        	voteMode = true;
	        	myVoter.tell(message, this.getSelf());
	        	notifyAll("StateChange","New Vote Started","Vote");
        	}
        }
        else if(message instanceof Clear)
        {
        	notifyAll("Clear","","");
        }
        else if(message instanceof Save)
        {
        	System.out.println("Persisting up in this bitch");
        	Save save = (Save)message;
        	//SavedImageController.persist(save.pic, save.word);
        }
        else if(message instanceof CheckLeader)
        {
        	if(members.get(drawMan) == null && members.size()>0)
        	{
        		selectNewDraw(1);
        	}
        }
        else {
            unhandled(message);
        }
        
    }
    
    public void selectNewDraw(int size)
    {
    	if(size==1)
    	{
    		this.drawMan = members.keySet().iterator().next();
    		WebSocket.Out<JsonNode> newDraw = members.get(this.drawMan);
    		ObjectNode event = Json.newObject();
    		event.put("type","YouDrawNow");
    		event.put("word",WordController.findRandom());
    		if(newDraw != null)
    		{
	    		newDraw.write(event);
	    		this.notifyAll("Guess","New Artist",drawMan+" is the new Artist");
	    		
    		}
    		else if(members.size()>0)
    		{
    			selectNewDraw(1);
    		}
    	}
    	else
    	{
    		String mostName = "";
    		int mostNum = 0;
    		for(String name: votes.keySet())
    		{
    			int val = votes.get(name);
    			System.out.println("Name: " + name + " Votes: " + val);
    			if(val>mostNum)
    			{
    				mostName = name;
    				mostNum = val;
    			}
    		}
    		if(mostName.equals(""))
    		{
    			hasVoted.clear();
    			votes.clear();
    			selectNewDraw(1);
    		}
    		else
    		{
    			hasVoted.clear();
    			votes.clear();
    			drawMan = mostName;
    			WebSocket.Out<JsonNode> newDraw = members.get(drawMan);
        		ObjectNode event = Json.newObject();
        		event.put("type","YouDrawNow");
        		event.put("word",WordController.findRandom());
        		if(newDraw!= null)
        		{
	        		newDraw.write(event);
	        		this.notifyAll("Guess","New Artist",drawMan+" is the new Artist");
        		}
        		else
        		{
        			selectNewDraw(1);
        		}
    		}
    	}
    	
    }
    
    public boolean isDrawing()
    {
    	if(this.drawMan == null)
    		return false;
    	return true;
    }
    
    public void sendCanvas(String user, JsonNode imageData)
    {
    	WebSocket.Out<JsonNode> channel = members.get(user);
    	ObjectNode event = Json.newObject();
    	event.put("type","Init");
    	ArrayNode imageArray =event.putArray("init");
    	Iterator<JsonNode> iter = imageData.elements();
    	while(iter.hasNext())
    	{
    		imageArray.add(iter.next().asInt());
    	}
    	if(channel != null)
    	{
    		channel.write(event);
    	}
    }
    
    public void askForCanvas(String user)
    {
    	WebSocket.Out<JsonNode> channel = members.get(drawMan);
    	ObjectNode event = Json.newObject();
    	event.put("type","InitRequest");
    	event.put("user",user);
    	if(channel != null)
    	{
    		channel.write(event);
    	}
    	else
    	{
    		ObjectNode justGO = Json.newObject();
    		justGO.put("fail","fail");
    		sendCanvas(user,justGO);
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
            if(channel != null)
            {
            	channel.write(event);
            }
        }
    }
    
    public void updateAll(String user,String type, int x, int y, String color)
    {
        for(WebSocket.Out<JsonNode> channel: members.values()) {
            
            ObjectNode event = Json.newObject();
            event.put("type", type);
            event.put("user", user);
            event.put("x", x);
            event.put("y",y);
            event.put("color", color);
            if(channel != null)
            {
            	channel.write(event);
            }
        }
    }
    
    public void giveWord(String type,String word, String user)
    {
    	WebSocket.Out<JsonNode> userOut = members.get(user);
    	ObjectNode wordObject = Json.newObject();
    	wordObject.put("type",type);
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
    public static class Update{
    	final int x;
    	final int y;
    	final String color;
    	final String user;
    	public Update(int xcord, int ycord, String fillColor, String name)
    	{
    		x=xcord;
    		y=ycord;
    		color=fillColor;
    		user=name;
    	}
    	
    }
    public static class Quit {
        
        final String username;
        
        public Quit(String username) {
            this.username = username;
        }
        
    }
    public static class initiateVote{
    	
    }
    public static class endVote{
    	
    }
    public static class InitRequest{
    	final String user;
    	final JsonNode imageData;
    	public InitRequest(String name,JsonNode node)
    	{
    		user = name;
    		imageData = node;
    	}
    }
    
    public static class Success{
    	final String user;
    	public Success(String name)
    	{
    		user = name;
    	}
    }
    
    public static class Init {
    	final String user;
    	public Init(String name)
    	{
    		user=name;
    	}
    }
    
    public static class Clear{
    	
    }
    
    public static class Save{
    	final String word;
    	final JsonNode pic;
    	public Save(String word, JsonNode node)
    	{
    		this.word = word;
    		pic = node;
    	}
    }
    public static class CheckLeader
    {
    	
    }
    
}