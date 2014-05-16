package controllers;

import play.mvc.*;


import com.fasterxml.jackson.databind.JsonNode; 
import views.html.*;
import models.*;

public class Application extends Controller {

    public static Result index()
    {
        return ok(index.render("Your new application is ready."));
    }
    
    public static Result paintChat()
    {
    	return ok(paintChat.render(""));
    }
    public static WebSocket<JsonNode> connectWS(final String username, final int groupNum) {
    	System.out.println("Ayyy");
        return new WebSocket<JsonNode>() {
            
            // Called when the Websocket Handshake is done.
            public void onReady(WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out){
                
                // Join the chat room.
                try { 
                    PaintGroup.join(username, groupNum, in, out);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
    }

}
