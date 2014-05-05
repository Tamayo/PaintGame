$(function() {
	//Variables Required for Client
	var state = "Guess";
	var currentGroup = 1;
    var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket
    var chatSocket; 
    
    //Makes modal appear on screen load
	$('#startModal').modal('toggle');
    
    //Handles a New Connection to a Group, starts a new websocket to the groupNum
    $('#checkUser').click(function(){
		var name = $('#userInput').val();
		chatSocket = new WS("ws://localhost:9000/connectWS?username="+name+"&groupNum=" + currentGroup);
		chatSocket.onmessage = receiveEvent
    });
    
    //Sends Message to Server
    var messageToServ = function(Message,Type) {
        chatSocket.send(JSON.stringify(
            {type: Type,message: Message}
        ))
        $("#talk").val('')
    }
    
    var handleJoin = function(data){
    	$('#insertChat').append('<dt class="text-success">'+data.type+'</dt><dd class="text-success">'+ data.user +' '+data.message+'</dd>');;
    	$('#insertChat').scrollTop($('#insertChat')[0].scrollHeight);
    }
    
    var handleVote = function(data){
    	$('#insertChat').append('<dt>'+data.type+'</dt><dd>'+ data.user +' '+data.message+'</dd>');
    	$('#insertChat').scrollTop($('#insertChat')[0].scrollHeight);
    }
    
    var handleGuess = function(data){
    	$('#insertChat').append('<dt>'+data.user+'</dt><dd>'+data.message+'</dd>');
    	$('#insertChat').scrollTop($('#insertChat')[0].scrollHeight);
    }
    
    var handleQuit = function(data){
    	$('#insertChat').append('<dt class="text-danger">'+data.type+'</dt><dd class="text-success">'+ data.user +' '+data.message+'</dd>');
    	$('#insertChat').scrollTop($('#insertChat')[0].scrollHeight);
    }
    
    var toggleState = function(data){
    	state = data.message;
    }

    var receiveEvent = function(event) {
        var data = JSON.parse(event.data)
        //If error is sent, end socket and display error message
        if(data.error) {
        	alert(data.error);
            chatSocket.close()
            return
        } 
        switch(data.type){
        //Case Success - Joined Chat Successfully, release Modal
        case "Success":
        	$('#startModal').modal('toggle');
        	break;
        //Case Join - Announce in chat new user has joined
        case "Join":
        	handleJoin(data);
        	break;
        //Case Guess - Add Message to Chat
        case "Guess":
        	handleGuess(data);
        	break;
        //Case Vote - Update Vote Totals and Add Message to Chat
        case "Vote":
        	handleVote(data);
        	break;
        //Case Quit - Announce User has left in chat
        case "Quit":
        	handleQuit(data);
        	break;
        case "StateChange":
        	toggleState(data);
        	break;
        }
    }

    var handleKeyPress = function(e) {
        if(e.charCode == 13 || e.keyCode == 13) {
            e.preventDefault()
            var message = $('#talk').val();
            messageToServ(message,state);
        }
    }

    $("#talk").keypress(handleKeyPress);
    
    $("#submitTalk").click(function(){
    	var message = $('#talk').val();
    	messageToServ(message,state);
    });

})
