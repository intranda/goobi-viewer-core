<adminMediaUpload>
	<div class="admin-cms-media__upload-wrapper {isDragover ? 'is-dragover' : ''}">
	    <div class="admin-cms-media__upload" ref="dropZone">
	        <div class="admin-cms-media__upload-input">
	            <p>
	                {opts.msg.uploadText}
	                <br />
	                <small>({opts.msg.allowedFileTypes}: {fileTypes})</small>
	            </p>
	            <label for="file" class="btn btn--default">{opts.msg.buttonUpload}</label>
	            <input id="file" class="admin-cms-media__upload-file" type="file" multiple="multiple" onchange="{buttonFilesSelected}">
	        </div>
	        <div class="admin-cms-media__upload-messages">
	            <div class="admin-cms-media__upload-message uploading">
	                <i class="fa fa-spinner fa-pulse fa-fw"></i> {opts.msg.mediaUploading}
	            </div>
	            <div class="admin-cms-media__upload-message success">
	                <i class="fa fa-check-square-o" aria-hidden="true"></i> {opts.msg.mediaFinished}
	            </div>
	            <div class="admin-cms-media__upload-message error">
	                <i class="fa fa-exclamation-circle" aria-hidden="true"></i> <span></span>
	            </div>        
	        </div>
	    </div>
	    <div class="admin-cms-media__list-files" if="{this.opts.showFiles && this.uploadedFiles.length > 0}">
	       	<ul>
	       		<li each="{file in this.uploadedFiles}">
	       			{file}
	       		</li>
	       		
	       	</ul>
	    </div>
	</div>
    <script>
        this.files = [];
        this.displayFiles = [];
        this.uploadedFiles = []
        if(this.opts.fileTypes) {
            this.fileTypes = this.opts.fileTypes;
        } else {            
        	this.fileTypes = 'jpg, png, docx, doc, pdf, rtf, html, xhtml, xml';
        }
        this.isDragover = false;
    
        this.on('mount', function () {
            var dropZone = (this.refs.dropZone);
    
            this.getUploadedFiles();
            
            dropZone.addEventListener('dragover', function (e) {
                e.stopPropagation();
                e.preventDefault();
                e.dataTransfer.dropEffect = 'copy';

                $('.admin-cms-media__upload-messages, .admin-cms-media__upload-message.uploading, .admin-cms-media__upload-message.success, .admin-cms-media__upload-message.error').removeClass('in-progress');

                this.isDragover = true;
                this.update();
            }.bind(this));
    
            dropZone.addEventListener('dragleave', function (e) {
                this.isDragover = false;
                this.update();
            }.bind(this));
    
            dropZone.addEventListener('drop', (e) => {
                e.stopPropagation();
                e.preventDefault();
                this.files = [];
                
                for (var f of e.dataTransfer.files) {
                    this.files.push(f);
                    var sizeUnit = 'KB';
                    var size = f.size / 1000;
                    
                    if (size > 1024) {
                        size = size / 1024;
                        sizeUnit = 'MB';
                    }
                    
                    if (size > 1024) {
                        size = size / 1024;
                        sizeUnit = 'GB';
                    }
                    
                    this.displayFiles.push({ name: f.name, size: Math.floor(size) + ' ' + sizeUnit, completed: 0 });
                }
    			this.uploadFiles()
    			.then( () => {
    			    this.isDragover = false;
    			    this.update();
    			})
                
            });
        }.bind(this));
     
        buttonFilesSelected(e) {
            for (var f of e.target.files) {
            	            	
                this.files.push(f);
                var sizeUnit = 'KB';
                var size = f.size / 1000;
    
                if (size > 1024) {
                    size = size / 1024;
                    sizeUnit = 'MB';
                }
                if (size > 1024) {
                    size = size / 1024;
                    sizeUnit = 'GB';
                }
    
                this.displayFiles.push({ name: f.name, size: Math.floor(size) + ' ' + sizeUnit, completed: 0 });
            }
    
            this.uploadFiles();
        }
    
        uploadFiles() {
            var uploads = [];

            $('.admin-cms-media__upload-messages, .admin-cms-media__upload-message.success, .admin-cms-media__upload-message.error').removeClass('in-progress');
            $('.admin-cms-media__upload-messages, .admin-cms-media__upload-message.uploading').addClass('in-progress');
            
            for (i = 0; i < this.files.length; i++) {
                uploads.push(Q(this.uploadFile(i)));
            }
            
            return Q.allSettled(uploads).then(function(results) {
             	var errorMsg = "";
                 results.forEach(function (result) {
                     console.log("settled ", result);
                     if (result.state === "fulfilled") {
                     	var value = result.value;
                     	this.fileUploaded(value);
                     } else {
                         var responseText = result.reason.responseText ? result.reason.responseText : result.reason;
                         errorMsg += (responseText + "</br>");
                     }
                 }.bind(this));
                 
                 if (errorMsg) {         
                 	this.fileUploadError(errorMsg);
                 } else if(this.opts.onUploadSuccess) {
                     this.opts.onUploadSuccess();
                 }
                 
           		if (this.opts.onUploadComplete) {
           			this.opts.onUploadComplete();
           		}
            
            }.bind(this))
            .then( () => this.getUploadedFiles());
        }
    
        fileUploaded(fileInfo) {
            $('.admin-cms-media__upload-messages, .admin-cms-media__upload-message.uploading').removeClass('in-progress');
            $('.admin-cms-media__upload-messages, .admin-cms-media__upload-message.success').addClass('in-progress');
        	
            setTimeout( function() {
                $('.admin-cms-media__upload-messages, .admin-cms-media__upload-message.uploading, .admin-cms-media__upload-message.success').removeClass('in-progress');        		
        	}, 5000 );
        }
    
        fileUploadError(responseText) {
            $('.admin-cms-media__upload-messages, .admin-cms-media__upload-message.uploading').removeClass('in-progress');
        	if (responseText) {
                $('.admin-cms-media__upload-messages, .admin-cms-media__upload-message.error').addClass('in-progress');
                $('.admin-cms-media__upload-message.error span').html(responseText);
            }
        }
        
        getUploadedFiles() {
            fetch(this.opts.postUrl, {
                method: "GET",
       		})
       		.then(response => response.json())
       		.then(json => {
       		    this.uploadedFiles = json;
       		    this.update();
       		})
        }
    
        uploadFile(i) {
            if (this.files.length <= i) {
                new Modal(this.refs.doneModal).show();
                return;
            }
    
            var displayFile = this.displayFiles[i];
            var config = {
                onUploadProgress: (progressEvent) => {
                    displayFile.completed = (progressEvent.loaded * 100) / progressEvent.total;
                    this.update();
                }
            };
            
            return fetch(this.opts.postUrl + this.files[i].name, {
                method: "GET",
            })
            .then(r => r.json())
            .then( json => { 
                return json.image != undefined
            })
            .then(exists => {
                if(exists) {                    
	                let overwrite = confirm(this.opts.msg.overwriteFileConfirm.replace("{0}",  this.files[i].name));
	                if(!overwrite) {
	                    throw this.opts.msg.overwriteFileRefused.replace("{0}",  this.files[i].name);
	                }
                }
            })
            .then(overwrite => {
	            var data = new FormData();   
	            data.append("filename", this.files[i].name);
	            data.append('file', this.files[i]);
				return data;
            })
            .then( data => fetch(this.opts.postUrl, {
                method: "POST",
                body: data,
                
       		})
       		.then( result => {
       		    console.log("post result ", result);
       		    var defer = Q.defer();
       		    if(result.ok) {
       		    	defer.resolve(result);    		        
       		    } else if(result.body && !result.responseText){
                   result.body.getReader().read()
					.then(({ done, value }) => {
						defer.reject({
						  responseText:   new TextDecoder("utf-8").decode(value)
						})
					});
       		    } else {
       		        defer.reject(result);
       		    }
       		    return defer.promise;
       		}));
    	
            
        }
    </script> 
</adminMediaUpload>

