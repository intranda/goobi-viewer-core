<!-- progress-bar.tag -->
<external-resource-download>
<ul>
	<label>DOWNLOAD EXTERNAL RESOURCES</label>
	<li each="{url in urls}"><label>{url}</label>
		<button onclick="{startDownloadTask}" show="{!isRequested(url)}">{msg.action__external_files__order_download}</button>
		<div if="{isWaiting(url)}">
			<img src="{preloader}" class="img-responsive"
				alt="{msg.action__external_files__download_in_queue}"
				title="{msg.action__external_files__download_in_queue}" />
		</div>
		<div if="{isDownloading(url)}">
			<progress value="{getDownloadProgress(url)}"
				max="{getDownloadSize(url)}" title="{getDownloadProgressLabel(url)}">{getDownloadProgressLabel(url)}</progress>
		</div>
		<div if="{isFinished(url)}">
			<ul>
				<li each="{object in getFiles(url)}">
					<a href="{object.url}">{object.path}</a>
				</li>
			</ul>
		</div>
		<div if="{isRequested(url) && !isFinished(url)}">
			<button onclick="{cancelDownload}">{msg.action__external_files__cancel_download}</button>
		</div>
	</li>
</ul>

<script>
      this.urls = [];
      this.downloads = new Map();
      this.updateListeners = new Map();
      this.updateDelay = 1000;
      this.ws = null;
      this.contextPath = "";
      this.preloader = "/resources/images/ajax_preloader.gif";
      this.msg = {
    		  action__external_files__order_download: "Download resource",
    		  action__external_files__cancel_download: "Cancel",
    		  action__external_files__download_in_queue: "Download queued"
      }

      this.on("mount", () => {
      	this.urls = this.opts.urls;
      	this.pi = this.opts.pi;
      	this.contextPath = this.opts.contextPath ? this.opts.contextPath : this.contextPath;
      	this.preloader = this.contextPath + this.preloader;
      	this.msg = $.extend(true, {}, this.msg, this.opts.msg ? this.opts.msg : {});
      	this.ws = this.initWebSocket();
      	this.ws.onOpen.subscribe(() => {
      		rxjs.from(this.urls).pipe(
      			rxjs.operators.flatMap(url => this.sendMessage(this.createSocketMessage(this.pi, url, "update")))		
      		).subscribe(() => {});
      	})
      	console.log("mount download external resources for urls ", this.urls);
      	this.update();
      });
      
      this.on("unmount", () => {
    	  if (this.ws && this.ws.isOpen()) {
              this.ws.close();
          }
    	  this.updateListeners.forEach(value => value.cancel());
      });


      initWebSocket() {
        // Ändern Sie die WebSocket-URL entsprechend Ihrer Konfiguration
        const socket = new viewerJS.WebSocket(window.location.host, this.contextPath, viewerJS.WebSocket.PATH_DOWNLOAD_TASK);
        console.log("created web socket ", socket.socket.url);
        socket.onMessage.subscribe( (event) => {
          this.handleUpdateMessage(event);
          this.update();
        });
        return socket;
      }
      
      sendMessage(message) {
    	  console.log("sending message ", message);
    	  if(typeof message != "string") {
    		message = JSON.stringify(message);
    	  }
    	  this.ws.sendMessage(message);
    	  return new Promise((resolve, reject) => {
    		 rxjs.merge(this.ws.onMessage, this.ws.onError).pipe(rxjs.operators.first()).subscribe(e => resolve(e));
    	  });
      }
      
      startDownloadTask(e) {
    	  console.log("Start download ", e);
    	const urlToDownload = e.item.url;
    	if(urlToDownload) { 
    		if(this.updateListeners.has(urlToDownload)) {
    			this.updateListeners.get(urlToDownload).cancel();
    		}
	      	this.sendMessage({pi: this.pi, url: urlToDownload, action: 'startdownload'});
	        const listener = viewerJS.helper.repeatPromise(() => this.sendMessage(this.createSocketMessage(this.pi, urlToDownload, "update")), this.updateDelay);
	        this.updateListeners.set(urlToDownload, listener);
	        listener.then(() => {});
    	} else {
    		console.error("No url found to download");
    	}
      }
      
      handleUpdateMessage(event) {
    	  let data = this.parseSocketMessage(event.data);
          if(data == null) {
        	  this.handleError("Not a valid message object: " + event.data);
          } else if(data.pi == this.pi && data.url && data.status) {
        	  console.log("received message ", data);
        	  switch(data.status) {
        	  case "waiting":
        	  case "processing":
	        	  this.downloads.set(data.url, data);
	        	  if(!this.updateListeners.has(data.url)) {
	        		  this.startDownloadTask({item:data})
	        	  }
        		  break;
        	  case "complete":
        		  if(data.files && data.files.length > 0) {
        			  data = $.extend(true, {}, this.downloads.get(data.url), data);
    	        	  console.log("download completed", data);
    	        	  this.downloads.set(data.url, data);
    	        	  if(this.updateListeners.has(data.url)) {	        		  
    	        	  	this.updateListeners.get(data.url).cancel();
    		  		  	this.updateListeners.delete(data.url);
    	        	  }
        		  } else {
    	        	  this.downloads.set(data.url, data);
    		          this.sendMessage(this.createSocketMessage(this.pi, data.url, 'listfiles'));
        		  }
        		  break;
        	  case "error":
        		  this.handleError(data.errorMessage);
        		  //fall through
        	  case "canceled":
        		  if(this.downloads.has(data.url)) {
	        		  this.downloads.delete(data.url);        			  
        		  }
        		  if(this.updateListeners.has(data.url)) {	        		  
  	        	  	this.updateListeners.get(data.url).cancel();
  		  		  	this.updateListeners.delete(data.url);
  	        	  }
        		  break;
        	  case "dormant": //do nothing
        	  }
        	  
          } else {
        	  this.handleError("Wrong or insufficient data in message object: " + event.data);
        	  this.updateListeners.forEach(value => value.cancel());
        	  this.updateListeners = new Map();
          }
      }
      
      handleError(message) {
    	  alert(message);
      }
       
      isRequested(url) {
    	  return this.downloads.has(url) && this.downloads.get(url).status !== 'dormant';
      }
      
      isDownloading(url) {
    	return this.downloads.get(url)?.status == 'processing';
      }
      
      isWaiting(url) {
    	  return this.downloads.get(url)?.status == 'waiting';
      }
      
      isFinished(url) {
    	  return this.downloads.get(url)?.status == 'complete';
      }
      

      getDownloadProgress(url) {
   	  	if(this.getDownloadSize(url) <= 0 || isNaN(this.getDownloadSize(url))) {
   	  		return undefined;
   	  	}
    	return this.downloads.get(url)?.progress;
      }
      
      getDownloadProgressLabel(url) {
    	  let fraction = this.getDownloadProgress(url)/this.getDownloadSize(url);
    	  if(isNaN(fraction) || fraction < 0) {
    		  return "unknown";
    	  } else {
    		  return (fraction * 100) + "%";
    	  }
      }
      
      getDownloadSize(url) {
    	  return this.downloads.get(url)?.resourceSize;
      }
      
      getFiles(url) {
    	  return this.downloads.get(url)?.files;
      }
      
      cancelDownload(e) {
    	  const url = e.item.url;
    	  if(url && this.downloads.has(url)) {
	    	  this.sendMessage({pi: this.pi, url: url, messageQueueId: this.downloads.get(url).messageQueueId, action: 'canceldownload'});
	    	  this.downloads.delete(url);
	    	  if(this.updateListeners.has(url)) {
	  			this.updateListeners.get(url).cancel();
	  			this.updateListeners.delete(url);
	  		  }

    	  }
    	  this.update();
      }
      
      parseSocketMessage(jsonString) {
    	  try {
    	        const json = JSON.parse(jsonString);
    	        if(!viewerJS.jsonValidator.validate(json)) {
    	        	throw new Error("The json object does not conform to the json signature " + json.jsonSignature);
    	        } else {
    	        	return json;
    	        }
    	    } catch (error) {
    	        console.error('Error parsing socket message string as JSON:', jsonString, error.message);
    	        return null;
    	    }
      }
      
      createSocketMessage(pi, url, action) {
    	  if(this.downloads.has(url)) {
    		  let oldMessage = this.downloads.get(url);
    		  let newMessage = $.extend(true, {}, oldMessage, {pi: pi, url: url, action: action});
    	  	  return newMessage;
    	  } else {
	    	  return {
	    		  pi: pi,
	    		  url: url,
	    		  action: action,
	    		  messageQueueId: undefined,
	    		  progress: 0,
	    		  resourceSize: 1,
	    		  status: undefined,
	    		  files: []
	    	  }    		  
    	  }
      }

  </script>

<style>
/* Hier können Sie das Styling nach Bedarf anpassen */
li {
	margin-bottom: 10px;
}
</style>
</external-resource-download>
