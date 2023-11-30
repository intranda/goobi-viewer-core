<!-- progress-bar.tag -->
<external-resource-download>
<ul>
	<li each={url in urls}>
		<label>{url}</label>
		<button onclick={order} show={!isDownloading(url)}>Bestellen</button>
		<div if={isDownloading(url)}>
			<progress value={getProgress(url)} max="100" if={progress < 100}></progress>
			<ul if={getProgress(url) === 100}>
				<li each={object in getDownloadObjects(url)}>
					<a href={object.url}>{object.label}</button>
				</li>
			</ul>
		</div>
	</li>
</ul>

<script>
      this.urls = [];
      this.downloading = new Map();
      this.progress = new Map();
      this.downloadObjects = new Map();
      this.ws = null;

      this.on("mount", () => {
      	this.urls = this.opts.urls;
      	this.pi = this.opts.pi;
      	this.ws = this.initWebSocket(this.opts.webSocketUrl);
      });
      
      this.on("unmount", () => {
    	  if (this.ws && this.ws.readyState === WebSocket.OPEN) {
              this.ws.close();
          }
      });
      
      order(e) {
    	console.log("order ", e);
    	urlToDownload = undefined; //get from e
        this.downloading = true;
        this.progress = 0;
        this.sendMessage({pi: this.pi, url: urlToDownload, action: 'start-download'})
      }

      initWebSocket(socketUrl) {
        // Ändern Sie die WebSocket-URL entsprechend Ihrer Konfiguration
        this.ws = new WebSocket(socketUrl);
        this.ws.addEventListener('message', (event) => {
          const data = JSON.parse(event.data);
          if(data.progress !== undefined) {
	          this.progress = parseInt(data.progress);
	          if (this.progress === 100) {
	            this.ws.close();
	            hanldeOrderFinished(data.downloadUrl);
	          }        	  
          }
        });
      }
      
      sendMessage(message) {
    	  if(typeof message != "string") {
    		message = JSON.stringify(message);
    	  }
    	  this.ws.send(message);
      }

      download() {
        // Fügen Sie hier den tatsächlichen Download-Code hinzu
        console.log('Download completed!');
        // Setzen Sie den Fortschrittsbalken zurück und ändern Sie den Zustand auf "nicht herunterladen"
        this.progress = 0;
        this.downloading = false;
      }
      
      isDownloading(url) {
    	  if(this.progress.has(url)) {
    	  	return this.downloading.get(url);
    	  } else{
    		return false;  
    	  }
      }
      
      getProgress(url) {
    	  if(this.progress.has(url)) {
	    	  return this.progress.get(url);    		  
    	  } else {
    		  return 0;
    	  }
      }
      
      getDownloadObjects(url) {
    	  if(this.downloadObjects.has(url)) {
	    	  return this.progress.get(url);    		  
    	  } else {
    		  return [];
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
