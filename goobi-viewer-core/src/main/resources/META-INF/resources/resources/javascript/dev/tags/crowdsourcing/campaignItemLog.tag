<campaignItemLog>
	
	<div each="{message in messages}" class="{isCurrentUser(message.creator) ? 'from_me' : ''}">
		<div>{message.creator.name}</div>
		<img src="{message.creator.avatar}"></img>
		<div>{message.dateCreated}</div>
		<div>{message.message}</div>
	</div>
	<div>
		<div>{currentUser.name}</div>
		<div>{currentUser.avatar}</div>		
		<input onchange="{addMessage}" value=""></input>
	</div>

<script>

this.currentUser = this.opts.user;
this.messages = this.opts.messages


this.on("mount", function() {
    //actions to do when tag is created
});

addMessage(event) {
    event.preventUpdate = true;
    let text = event.target.value;
    event.target.value = "";
    if(text) {
        let message = {
                message : text,
                dateCreated : new Date().toJSON(),
                creator : this.currentUser,
                
        }
        this.messages.push(message);
        this.update();
    }
}

isCurrentUser(user) {
    return user.userId == this.currentUser.userId;
}


</script>


</campaignItemLog>

