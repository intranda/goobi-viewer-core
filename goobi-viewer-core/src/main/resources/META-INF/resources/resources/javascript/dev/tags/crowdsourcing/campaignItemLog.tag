<campaignItemLog>

	<div class="crowdsourcing-annotations__log-wrapper">
		<div class="crowdsourcing-annotations__log-title">
			<span>{Crowdsourcing.translate("log")}</span>
			<button ref="compress" onclick="{compressLog}" class="crowdsourcing-annotations__log-expand btn btn--clear">
				<svg class="crowdsourcing-annotations__log-expand-icon" viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
					<use riot-href="{getIconHref('chevron-up')}"></use>
				</svg>
			</button>
			<button ref="expand" onclick="{expandLog}" class="btn btn--clear crowdsourcing-annotations__log-expand">
				<svg class="crowdsourcing-annotations__log-expand-icon" viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
					<use riot-href="{getIconHref('chevron-down')}"></use>
				</svg>
			</button>
		</div>
		<div ref="toggleBox" class="crowdsourcing-annotations__toggle-box">
			<div ref="innerWrapper" class="crowdsourcing-annotations__log-inner-wrapper">
				<div each="{message in messages}" class="crowdsourcing-annotations__log-message-entry {isCurrentUser(message.creator) ? '-from-me' : ''}">
					<img class="crowdsourcing-annotations__log-round-avatar" src="{message.creator.avatar}"></img>

					<div class="crowdsourcing-annotations__log-speech-bubble">
						<div class="crowdsourcing-annotations__log-message-info">
							<div class="crowdsourcing-annotations__log-message-user-name">
								{message.creator.name}
							</div>
						</div>
						<div class="crowdsourcing-annotations__log-message-text">
							{message.message}
						</div>
						<div class="crowdsourcing-annotations__log-message-time-stamp">
							{formatDate(message.dateCreated)}
						</div>
					</div>
				</div>
			</div>

			<div ref="messageBox" class="crowdsourcing-annotations__log-send-message-area">
				<input onkeypress="{addMessageOnEnter}" placeholder="{Crowdsourcing.translate('label__enter_message_here')}" class="crowdsourcing-annotations__log-message-input" id="crowdsourcingAnnotationsLogMessageInput" name="crowdsourcingAnnotationsLogMessageInput" ref="messageText"></input>
				<button class="btn btn--default crowdsourcing-annotations__log-message-send-button" onclick="{addMessage}">{Crowdsourcing.translate('action__send')}</button>
			</div>
		</div>
	</div>


<script>
const ensureTrailingSlash = value => value.endsWith('/') ? value : `${value}/`;
const viewerConfig = window.viewerConfig || {};
this.iconBasePath = ensureTrailingSlash(viewerConfig.iconBasePath || viewerConfig.contextPath || '/');
this.getIconHref = iconName => `${this.iconBasePath}resources/icons/outline/${iconName}.svg#icon`;

this.currentUser = this.opts.item.currentUser;
this.messages = this.opts.item.log;
this.expanded = false;

this.on("mount", function() {
//add any initialization here
	// hide log on page load if user compressed it before
    if (sessionStorage.getItem("logCompressed") === 'logIsCompressed') {
    	$(this.refs.toggleBox).hide();
        $(this.refs.compress).hide();
    }
    else {
        $(this.refs.expand).hide();
    }

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
    $(this.refs.expand).hide({
        complete: () => {
        	$(this.refs.compress).show();
    	},
        duration: 0
    });
	$(this.refs.toggleBox).slideToggle(400);
    $('.crowdsourcing-annotations__content-right').animate({scrollTop: '+=400px'}, 400);
    sessionStorage.setItem('logCompressed', 'logNotCompressed');
}

compressLog() {
    $(this.refs.compress).hide({
        complete: () => {
        	$(this.refs.expand).show();
    	},
        duration: 0
    });
	$(this.refs.toggleBox).slideToggle(400);
	sessionStorage.setItem('logCompressed', 'logIsCompressed');
}

formatDate(dateString) {
    let date = new Date(dateString);
    return date.toLocaleString(Crowdsourcing.translator.language, {
		dateStyle: "long",
		timeStyle: "short"
    });
}

</script>


</campaignItemLog>
