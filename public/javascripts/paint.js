$(function() {
	//Variables Required for Client
	var state = "Guess";
	var canDraw = 0;
	var currentGroup = 1;
    var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket
    var chatSocket; 
    var name;
    
    //Makes modal appear on screen load
	$('#startModal').modal('toggle');
    
    //Handles a New Connection to a Group, starts a new websocket to the groupNum
    $('#checkUser').click(function(){
		name = $('#userInput').val();
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
    
    var paintUpdateToServ = function(X,Y,Color){
    	chatSocket.send(JSON.stringify(
    			{type: "Update", x: X, y: Y, color: Color}
    	))
    }
    
    ////Canvas Handler
    //
    
    
    function createCanvas(parent, width, height) {
        var canvas = {};
        canvas.node = document.createElement('canvas');
        canvas.context = canvas.node.getContext('2d');
        canvas.node.width = width || 100;
        canvas.node.height = height || 100;
        parent.appendChild(canvas.node);
        return canvas;
    }
    
    	var container = document.getElementById('Game1');
    	var fillColor = '#ff0000'
        var canvas = createCanvas(container, 400, 400);
        var ctx = canvas.context;
        // define a custom fillCircle method
        ctx.fillCircle = function(x, y, radius, fillColor) {
            this.fillStyle = fillColor;
            this.beginPath();
            this.moveTo(x, y);
            this.arc(x, y, radius, 0, Math.PI * 2, false);
            this.fill();
        };
        ctx.clearTo = function(fillColor) {
            ctx.fillStyle = fillColor;
            ctx.fillRect(0, 0, 400, 400);
        };
        ctx.clearTo("#ddd");
        
        // bind mouse events
        canvas.node.onmousemove = function(e) {
        	if(canDraw == 1){
	            if (!canvas.isDrawing) {
	               return;
	            }
	            var x = e.pageX - this.offsetLeft;
	            var y = e.pageY - this.offsetTop - 50;
	            var radius = 10; // or whatever
	            ctx.fillCircle(x, y, radius, fillColor);
	            paintUpdateToServ(x,y,fillColor);
        	}
        };
        canvas.node.onmousedown = function(e) {
            canvas.isDrawing = true;
        };
        canvas.node.onmouseup = function(e) {
            canvas.isDrawing = false;
        };
        
        $(".colorChanger").click(function(){
        	fillColor = $(this).attr('id');
        });
     ///End Canvas Handeler
    
    
        
    //Data will contain initial game state Upon joining a game
    var handleConnect = function(data){
    	$('#UserModal').text("Connected, Waiting for Init");
    	canDraw = data.canDraw;
    	state= data.mode;
    	messageToServ("","InitRequest");
    }
    
    var handleInit = function(data){
    	console.info("Init Recieved");
    	var imgdata =ctx.createImageData(400,400);
    	imgdata.data.set(new Uint8ClampedArray(data.init));
    	ctx.putImageData(imgdata,0,0);
    	$('#startModal').modal('toggle');
    }
    
    var handleInitRequest = function(data){
    	console.info("Init Request Recieved");
    	if(data.user == name)
    	{
    		console.info("Self, No Init Needed");
    		$('#startModal').modal('toggle');
    	}
    	else
    	{
    		chatSocket.send(JSON.stringify(
                    {type: "Init",user: data.user,message: ctx.getImageData(0,0,400,400).data}
                ));
    	}
    	
    	//messageToServ(ctx.getImageData(0,0,400,400).data,"Init");
    }
    
    //Displays Join Message to group, and Updates list of Members of Group
    var handleJoin = function(data){
    	$('#insertChat').append('<dt class="text-success">'+data.type+'</dt><dd class="text-success">'+ data.user +' '+data.message+'</dd>');;
    	$('#insertChat').scrollTop($('#insertChat')[0].scrollHeight);
    	$('#insertMembers').html("");
    	 $(data.members).each(function() {
             var li = document.createElement('li');
             li.textContent = this;
             $("#insertMembers").append(li);
         })
    }
    
    //Displays Users Vote to Group
    var handleVote = function(data){
    	$('#insertChat').append('<dt>'+data.type+'</dt><dd>'+ data.user +' '+data.message+'</dd>');
    	$('#insertChat').scrollTop($('#insertChat')[0].scrollHeight);
    }
    
    //Displays Users Guess/Chat to Group
    var handleGuess = function(data){
    	$('#insertChat').append('<dt>'+data.user+'</dt><dd>'+data.message+'</dd>');
    	$('#insertChat').scrollTop($('#insertChat')[0].scrollHeight);
    }
    
    //Updates list of Members of group, and posts Quit Message
    var handleQuit = function(data){
    	$('#insertChat').append('<dt class="text-danger">'+data.type+'</dt><dd class="text-success">'+ data.user +' '+data.message+'</dd>');
    	$('#insertChat').scrollTop($('#insertChat')[0].scrollHeight);
    	$('#insertMembers').html("");
    	$(data.members).each(function() {
            var li = document.createElement('li');
            li.textContent = this;
            $("#insertMembers").append(li);
        })
    }
    
    /*
     * Will be called every time the canvas is updated by drawer,
     * and will contain all data needed to update a Client Canvas 
    */
    var handleUpdate = function(data){
    	if(data.user == name)
    	{
    		return;
    	}
    	else
    	{
    		 ctx.fillCircle(data.x, data.y, 10, data.color);
    	}
    }
    
    var toggleState = function(data){
    	state = data.message;
    	if(data.message == "Vote")
    	{
	    	$('#insertChat').append('<dt>'+"Vote Initiated"+'</dt><dd>'+"Enter Vote for new Artist"+'</dd>');
	    	$('#insertChat').scrollTop($('#insertChat')[0].scrollHeight);
    	}
    	if(data.message == "Guess")
    	{
    		$('#insertChat').append('<dt>'+"Vote Finished"+'</dt><dd>'+"Waiting for Vote Result"+'</dd>');
	    	$('#insertChat').scrollTop($('#insertChat')[0].scrollHeight);
    	}
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
        	handleConnect(data);
        	break;
        	
        case "YouDrawNow":
        	canDraw = 1;
        	break;
        //Case Init - Initializes Canvas
        case "Init":
        	handleInit(data);
        	break;
        //Case InitRequest - Sends Current canvas state to Server
        case "InitRequest":
        	handleInitRequest(data);
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
        //Case StateChange - Toggles Between Guess mode and Vote mode for Client
        case "StateChange":
        	toggleState(data);
        	break;
        //Case Update - Updates Client canvas 
        case "Update":
        	handleUpdate(data);
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
