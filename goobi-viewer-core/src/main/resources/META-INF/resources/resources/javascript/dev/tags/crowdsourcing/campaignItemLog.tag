<campaignItemLog>
	
	<div class="crowdsourcing-annotations__log-wrapper">
		<div class="crowdsourcing-annotations__log-title">
			<span>Log</span>
			<span ref="expand" onclick="{expandLog}" class="crowdsourcing-annotations__log-expand"><i class="fa fa-angle-down" aria-hidden="true"></i></span>
			<span ref="compress" onclick="{compressLog}" class="crowdsourcing-annotations__log-expand"><i class="fa fa-angle-up" aria-hidden="true"></i></span>
		</div>
		<div ref="innerWrapper" class="crowdsourcing-annotations__log-inner-wrapper">

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
		</div>
			
		<div class="crowdsourcing-annotations__log-send-message-area">
<!-- 			<div>{currentUser.name}</div> -->
<!-- 			<img src="{currentUser.avatar}"></img> -->
			<input onkeypress="{addMessageOnEnter}" placeholder="Type here..." class="crowdsourcing-annotations__log-message-input" id="crowdsourcingAnnotationsLogMessageInput" name="crowdsourcingAnnotationsLogMessageInput" ref="messageText"></input>
			<button class="btn btn--default crowdsourcing-annotations__log-message-send-button" onclick="{addMessage}">Send</button>
		</div>
	</div>
	

<script>

this.currentUser = this.opts.item.currentUser;
this.messages = this.opts.item.log;
this.expanded = false;

this.on("mount", function() {
	//add any initialization here
    $(this.refs.compress).hide();
});

this.on("updated", function() {
    //update occurs after mount and then every time a message is added
    this.scrollToBottom();    

});

addMessageOnEnter(event) {
    var code = event.keyCode || event.which;
	if(code==13){
	    this.addMessage();
	} else {
	    //don't update tag if any other key was pressed
	    event.preventUpdate = true;
	}
}

addMessage() {
    let text = this.refs.messageText.value;
    this.refs.messageText.value = "";
    if(text.trim().length > 0) {
        let message = {
                message : text,
                dateCreated : new Date().toJSON(),
                creator : this.currentUser,
        }
        this.opts.item.addLogMessage(message);
//         console.log("added message", message);
    }
}

isCurrentUser(user) {
    return user.userId == this.currentUser.userId;
}

scrollToBottom() {
	$(this.refs.innerWrapper).scrollTop(this.refs.innerWrapper.scrollHeight);
}

expandLog() {
    $(this.refs.expand).fadeOut({
        complete: () => {
        	$(this.refs.compress).fadeIn();
    	}
    });
    $('.crowdsourcing-annotations__content-right').animate({scrollTop: '+=150px'}, 600);
	$(this.refs.innerWrapper).css({"overflow-y": "auto", "max-height": "500px"});
}

compressLog() {
    $(this.refs.compress).fadeOut({
        complete: () => {
        	$(this.refs.expand).fadeIn();
    	}
    });
    $('.crowdsourcing-annotations__content-right').animate({scrollTop: '+=150px'}, 600);
	$(this.refs.innerWrapper).css({"overflow-y": "hidden", "max-height": "350px"});
}


</script>


</campaignItemLog>

