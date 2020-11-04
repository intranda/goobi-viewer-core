<campaignItemLog>
	
	<div each="{message in messages}" class="{isCurrentUser(message.creator) ? 'from_me' : ''}">
		<div>{message.creator.name}</div>
		<img src="{message.creator.avatar}"></img>
		<div>{message.dateCreated}</div>
		<div>{message.message}</div>
	</div>
	<div>
		<div>{currentUser.name}</div>
		<img src="{currentUser.avatar}"></img>
		<input ref="messageText"></input>
		<button onclick="{addMessage}">Send</button>
	</div>

<script>

this.currentUser = this.opts.user;
this.messages = this.opts.messages

this.on("mount", function() {
    //actions to do when tag is created
});

addMessage(event) {
    let text = this.refs.messageText.value;
    this.refs.messageText.value = "";
    if(text.trim().length > 0) {
        let message = {
                message : text,
                dateCreated : new Date().toJSON(),
                creator : this.currentUser,
                
        }
        this.messages.push(message);
        console.log("added message", message);
    }
}

isCurrentUser(user) {
    return user.userId == this.currentUser.userId;
}


</script>


</campaignItemLog>

