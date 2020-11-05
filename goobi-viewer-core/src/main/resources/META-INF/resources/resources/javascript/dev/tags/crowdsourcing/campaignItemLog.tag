<campaignItemLog>
	
	<div class="crowdsourcing-annotations__log-wrapper">
		<div class="crowdsourcing-annotations__log-title">
			Log
		</div>
		<div class="crowdsourcing-annotations__log-inner-wrapper">
			<div class="crowdsourcing-annotations__log-message-entry">
				<img src="http://www.gravatar.com/avatar/1a979da9c2caef517b355a4b5e3e5b58.jpg?s=96&d=identicon" class="crowdsourcing-annotations__log-round-avatar"></img>
				<div class="crowdsourcing-annotations__log-speech-bubble">
					<div class="crowdsourcing-annotations__log-message-info">
						<div class="crowdsourcing-annotations__log-message-user-name">
							JHG
						</div>
						<div class="crowdsourcing-annotations__log-message-time-stamp">
							2020-11-05T11:22:31
						</div>
					</div>
					<div class="crowdsourcing-annotations__log-message-text">
						Voll dufte alles hier... Dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book. It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged. It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages, and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum.
					</div>
				</div>
			</div>
			
			<div class="crowdsourcing-annotations__log-message-entry  -from-me">
				<img src="http://www.gravatar.com/avatar/9fcf7e472d36b222ceb6b747a940669c.jpg?d=identicon" class="crowdsourcing-annotations__log-round-avatar"></img>
				<div class="crowdsourcing-annotations__log-speech-bubble">
					<div class="crowdsourcing-annotations__log-message-info">
						<div class="crowdsourcing-annotations__log-message-user-name">
							Jan
						</div>
						<div class="crowdsourcing-annotations__log-message-time-stamp">
							2020-11-05T11:22:31
						</div>
					</div>
					<div class="crowdsourcing-annotations__log-message-text">
						Ney ney... Muss alles anners :-) Lorem Ipsum has been the industry's standard dummy text ever since the 1500s. and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum.
					</div>
				</div>
			</div>
			
			<div class="crowdsourcing-annotations__log-message-entry">
				<img src="http://www.gravatar.com/avatar/1a979da9c2caef517b355a4b5e3e5b58.jpg?s=96&d=identicon" class="crowdsourcing-annotations__log-round-avatar"></img>
				<div class="crowdsourcing-annotations__log-speech-bubble">
					<div class="crowdsourcing-annotations__log-message-info">
						<div class="crowdsourcing-annotations__log-message-user-name">
							JHG
						</div>
						<div class="crowdsourcing-annotations__log-message-time-stamp">
							2020-11-05T11:22:31
						</div>
					</div>
					<div class="crowdsourcing-annotations__log-message-text">
						Ok! :'( ... Dummy text of the printing and typesetting industry. 
					</div>
				</div>
			</div>

			<div each="{message in messages}" class="crowdsourcing-annotations__log-message-entry {isCurrentUser(message.creator) ? '-from-me' : ''}">
				<img class="crowdsourcing-annotations__log-round-avatar" src="{message.creator.avatar}"></img>
				
				<div class="crowdsourcing-annotations__log-speech-bubble">
					<div class="crowdsourcing-annotations__log-message-info">
						<div class="crowdsourcing-annotations__log-message-user-name">
							{message.creator.name}
						</div>
						<div class="crowdsourcing-annotations__log-message-time-stamp">
							{message.dateCreated}
						</div>
					</div>
					<div class="crowdsourcing-annotations__log-message-text">
						{message.message}
					</div>
				</div>
			</div>
			
			<div class="crowdsourcing-annotations__log-send-message-area">
	<!-- 			<div>{currentUser.name}</div> -->
	<!-- 			<img src="{currentUser.avatar}"></img> -->
				<textarea placeholder="Type here..." class="crowdsourcing-annotations__log-message-input" id="crowdsourcingAnnotationsLogMessageInput" name="crowdsourcingAnnotationsLogMessageInput" ref="messageText"></textarea>
				<button class="btn btn--default crowdsourcing-annotations__log-message-send-button" onclick="{addMessage}">Send</button>
			</div>
		</div>
	</div>
	

<script>

this.currentUser = this.opts.item.currentUser;
this.messages = this.opts.item.log;

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
        this.opts.item.addLogMessage(message);
        console.log("added message", message);
    }
}

isCurrentUser(user) {
    return user.userId == this.currentUser.userId;
}

// $( document ).ready(function() {
// 	$('.crowdsourcing-annotations__log-wrapper').hide();
// });
</script>


</campaignItemLog>

