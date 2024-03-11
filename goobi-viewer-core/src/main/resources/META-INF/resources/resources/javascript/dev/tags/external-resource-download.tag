<!-- progress-bar.tag -->
<external-resource-download>
<div class="download-external-resource__resource_list">
	<div class="download-external-resource__resource" each="{url in urls}">
		<div class="download-external-resource__error_wrapper {isError(url) ? '-active' : ''}">
			<i class="fa fa-exclamation-triangle"/>
			<label class="download-external-resource__error">{getErrorMessage(url)}</label>
		</div>
		<div class="download-external-resource__progress-wrapper {isFinished(url) ? '' : '-active'}">
			<label>{url}</label>
			<button onclick="{startDownloadTask}" class="download-external-resource__order btn btn--full {isRequested(url)|isError(url)|isFinished(url) ? '' : '-active'}">{msg.downloadButton}</button>
			<div class="download-external-resource__waiting_animation {isWaiting(url) ? '-active' : ''}">
				<img src="{preloader}" class="img-responsive"
					alt="{msg.action__external_files__download_in_queue}"
					title="{msg.action__external_files__download_in_queue}" />
			</div>
			<div class="download-external-resource__loading_animation {isDownloading(url) ? '-active' : ''}">
				<progress value="{getDownloadProgress(url)}"
					max="{getDownloadSize(url)}" title="{getDownloadProgressLabel(url)}">{getDownloadProgressLabel(url)}</progress>
			</div>
		</div>
		<div class="download-external-resource__results_wrapper {isFinished(url) ? '-active' : ''}">
          		<virtual each="{object in getFiles(url)}"> 
          			<div class="born-digital__items-wrapper">
	         		<div class="born-digital__head-mobile">
	         			<span>{msg.label__born_digital__filename}</span>
	         		</div>
	                <div class="born-digital__item">
	                	<span>{object.path}</span>
	                </div>
	         		<div class="born-digital__head-mobile">
	         			<span>{msg.label__born_digital__filedescription}</span>
	         		</div>
	                <div class="born-digital__item">
						<span>{object.description}</span>
					</div>
	         		<div class="born-digital__head-mobile">
	         			<span>{msg.label__born_digital__filesize}</span>
	         		</div>
	                <div class="born-digital__item">
	                 	<span>{object.size}</span>
	                </div>
	         		<div class="born-digital__head-mobile">
	         			<span>{msg.label__born_digital__fileformat}</span>
	         		</div>
	                <div class="born-digital__item">
	                 	<span>{msg[object.mimeType]}</span>
	                </div>
	                <div class="born-digital__item-download-last">
	               		<a class="born-digital__item__download btn btn--full" href="{object.url}" target="_blank">{msg.action__born_digital__download}</a>
	              	</div>
	        		</div>
	          	</virtual>
		</div>
	</div>
</div>

<script>
      this.urls = [];
      this.downloads = new Map();
      this.updateListeners = new Map();
      this.updateDelay = 1000;
      this.ws = null;
      this.contextPath = "";
      this.preloader = "/resources/images/ajax_preloader.gif";
      this.msg = {
    		  action__external_files__order_download: "action__external_files__order_download",
    		  action__external_files__cancel_download: "action__external_files__cancel_download",
    		  action__external_files__download_in_queue: "action__external_files__download_in_queue",
    		  label__born_digital__filename: "label__born_digital__filename",
    		  label__born_digital__filedescription: "label__born_digital__filedescription",
    		  label__born_digital__filesize: "label__born_digital__filesize",
    		  label__born_digital__fileformat: "label__born_digital__fileformat",
    		  action__born_digital__download: "label__born_digital__fileformat",
    		  label__born_digital__downloading: "label__born_digital__downloading",
      }

      this.on("mount", () => {
    	  console.log("mounting external resource download", this, this.opts);
      	this.urls = this.opts.urls;
      	this.pi = this.opts.pi;
      	this.msg = this.opts.msg;
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
//     	  console.log("sending message ", message);
    	  if(typeof message != "string") {
    		message = JSON.stringify(message);
    	  }
    	  this.ws.sendMessage(message);
    	  return new Promise((resolve, reject) => {
    		 rxjs.merge(this.ws.onMessage, this.ws.onError).pipe(rxjs.operators.first()).subscribe(e => resolve(e));
    	  });
      }
      
      startDownloadTask(e) {
//     	  console.log("Start download ", e);
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
//         	  console.log("received message ", data);
        	  switch(data.status) {
        	  case "waiting":
        	  case "processing":
	        	  this.downloads.set(data.url, data);
	        	  if(!this.updateListeners.has(data.url)) {
	        		  //download in progress. Set up listener to wait for update
	        		const listener = viewerJS.helper.repeatPromise(() => this.sendMessage(this.createSocketMessage(this.pi, data.url, "update")), this.updateDelay);
			        this.updateListeners.set(data.url, listener);
			        listener.then(() => {});
// 	        		  this.startDownloadTask({item:data})
	        	  }
        		  break;
        	  case "complete":
        		  if(data.files && data.files.length > 0) {
        			  data = $.extend(true, {}, this.downloads.get(data.url), data);
    	        	  console.log("download completed", data);
    	        	  this.downloads.set(data.url, data);
    	        	  this.cancelListener(data.url);
        		  } else {
    	        	  this.downloads.set(data.url, data);
    		          this.sendMessage(this.createSocketMessage(this.pi, data.url, 'listfiles'));
        		  }
        		  break;
        	  case "error":
        		  console.log("error in ", data);
        		  this.downloads.set(data.url, data);
        		  this.cancelListener(data.url);
          		  break;
        	  case "canceled":
        		  if(this.downloads.has(data.url)) {
	        		  this.downloads.delete(data.url);        			  
        		  }
        		  this.cancelListener(data.url);
        		  break;
        	  case "dormant": //do nothing
        	  }
        	  
          } else {
        	  this.handleError("Wrong or insufficient data in message object: " + event.data);
        	  this.updateListeners.forEach(value => value.cancel());
        	  this.updateListeners = new Map();
          }
      }
      
      cancelListener(url) {
    	  if(this.updateListeners.has(url)) {	        		  
      	  	this.updateListeners.get(url).cancel();
	  		this.updateListeners.delete(url);
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
    		  console.log("title: ", this.msg.label__born_digital__downloading)
    		  return this.msg.label__born_digital__downloading;
    	  } else {
    		  return this.msg.label__born_digital__downloading + ": " + (fraction * 100) + "%";
    	  }
      }
      
      getDownloadSize(url) {
    	  return this.downloads.get(url)?.resourceSize;
      }
      
      getFiles(url) {
    	  console.log("get files ", url, this.downloads.get(url));
    	  return this.downloads.get(url)?.files;
      }
      
      isError(url) {
    	  return this.downloads.get(url)?.status == "error";
      }
      
      getErrorMessage(url) {
    	  return this.downloads.get(url)?.errorMessage;
      }
      
      cancelDownload(e) {
    	  const url = e.item.url;
    	  if(url && this.downloads.has(url)) {
	    	  this.sendMessage(this.createSocketMessage(this.pi, url, 'canceldownload'));
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
//     	  	  console.log("sending return message", newMessage, "to", oldMessage, "with", pi, url, action)
    		  return newMessage;
    	  } else {
//     		  console.log("sending new message with",pi, url, action);	
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

</style>
</external-resource-download>
