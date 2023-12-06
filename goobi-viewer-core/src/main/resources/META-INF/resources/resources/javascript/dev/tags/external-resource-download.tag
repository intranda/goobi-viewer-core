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
			<progress value="{getProgress(url).absolute}"
				max="{getProgress(url).total}">{getProgress(url).fraction*100}%</progress>
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
          console.log("received message from socket: ", event);
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
	      	this.sendMessage({pi: this.pi, url: urlToDownload, action: 'start-download'});
	        const listener = viewerJS.helper.repeatPromise(() => this.sendMessage({"action": "update", "pi": this.pi, "url": urlToDownload}), this.updateDelay);
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
        	  
        	  switch(data.status) {
        	  case "WAITING":
        		  data.progressInfo = {absolute: 0, total: "unknown", fraction: 0};
	        	  this.downloads.set(data.url, data);
	        	  break;
        	  case "PROCESSING":
        		  data.progressInfo = {absolute: parseInt(data.progress), total: parseInt(data.size), fraction: parseInt(data.progress)/parseInt(data.size)}
	        	  this.downloads.set(data.url, data);
        		  break;
        	  case "COMPLETE":
        		  if(data.files && data.files.length > 0) {
        			  data = $.extend(true, {}, data, this.downloads.get(data.url));
    	        	  data.progressInfo = {absolute: 1, total: 1, fraction: 1};
    	        	  console.log("download completed", data);
    	        	  this.downloads.set(data.url, data);
    	        	  if(this.updateListeners.has(data.url)) {	        		  
    	        	  	this.updateListeners.get(data.url).cancel();
    		  		  	this.updateListeners.delete(data.url);
    	        	  }
        		  } else {
        			  data.progressInfo = {absolute: parseInt(data.progress), total: parseInt(data.size), fraction: 1}
    	        	  this.downloads.set(data.url, data);
    		          this.sendMessage({pi: this.pi, url: data.url, action: 'list-files'})
        		  }
        	  case "ERROR":
        		  this.handleError(data.errorMessage);
        		  //fall through
        	  case "CANCELED":
        		  this.downloads.delete(data.url);
        		  if(this.updateListeners.has(data.url)) {	        		  
  	        	  	this.updateListeners.get(data.url).cancel();
  		  		  	this.updateListeners.delete(data.url);
  	        	  }
        		  break;
        	  }
        	  
          } else {
        	  this.handleError("Wrong or insufficient data in message object: " + event.data);
          }
      }
      
      isRequested(url) {
    	  return this.downloads.has(url);
      }
      
      isDownloading(url) {
    	return this.downloads.get(url)?.progressInfo?.fraction > 0 && this.downloads.get(url)?.progressInfo?.fraction < 1;
      }
      
      isWaiting(url) {
    	  return this.downloads.get(url)?.progressInfo?.fraction == 0;
      }
      
      isFinished(url) {
    	  return this.isRequested(url) && this.downloads.get(url)?.files;
      }
      

      getProgress(url) {
    	  return this.downloads.get(url)?.progressInfo;
      }
      
      getFiles(url) {
    	  return this.downloads.get(url)?.files;
      }
      
      cancelDownload(e) {
    	  const url = e.item.url;
    	  if(url && this.downloads.has(url)) {
	    	  this.sendMessage({pi: this.pi, url: url, messageQueueId: this.downloads.get(url).messageQueueId, action: 'cancel-download'});
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

  </script>

<style>
/* Hier können Sie das Styling nach Bedarf anpassen */
li {
	margin-bottom: 10px;
}
</style>
</external-resource-download>
