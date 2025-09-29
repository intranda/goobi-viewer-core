<adminMediaUpload>
	<div class="admin-cms-media__upload-wrapper">
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
	    <div if="{this.opts.showFiles}" class="admin-cms-media__list-files {this.uploadedFiles.length > 0 ? 'in' : ''}" ref="filesZone">
	       	<div each="{file in this.uploadedFiles}" class="admin-cms-media__list-files__file">
		       	<img src="{file}" alt="{getFilename(file)}" title="{getFilename(file)}"/>
		       	<div class="delete_overlay" onclick="{deleteFile}">
		       		<i class="fa fa-trash" aria-hidden="true"></i>
		       	</div>
	       	</div>
	    </div>
	</div>
    <script>
        this.files = [];
        this.displayFiles = [];
        this.uploadedFiles = []
        if(this.opts.fileTypes) {
            this.fileTypes = this.opts.fileTypes;
        } else {            
        	this.fileTypes = 'jpg, png, tif, jp2, gif, pdf, svg, ico, mp4';
        }
    
        this.on('mount', function () {
            if(this.opts.showFiles) {
                this.initUploadedFiles();
            }

            this.initDrop();
            
        }.bind(this));

        initDrop() {
			var dropZone = (this.refs.dropZone); 
    
            
            dropZone.addEventListener('dragover', e => {
                e.stopPropagation();
                e.preventDefault();
                e.dataTransfer.dropEffect = 'copy';

                $('.admin-cms-media__upload-messages, .admin-cms-media__upload-message.uploading, .admin-cms-media__upload-message.success, .admin-cms-media__upload-message.error').removeClass('in-progress');

                this.setDragover(true);
                this.update();
            });
    
            dropZone.addEventListener('dragleave', e => {
                this.setDragover(false);
                this.update();
            });
    
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
    			    this.setDragover(false);
    			    this.update();
    			})
                
            });
        }
     
        initUploadedFiles() {
			this.getUploadedFiles();
            
            var filesZone = (this.refs.filesZone);
        }
        
        buttonFilesSelected(e) {
            this.files = [];
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
            	if(this.opts.fileTypeValidator) {
            		let regex = this.opts.fileTypeValidator;// new RegExp(this.opts.fileTypeValidator);
            		if(!this.files[i]?.name?.match(regex)) {
	            		let errormessage = "File " + this.files[i].name + " is not allowed for upload";
	            		console.log(errormessage)
	            		uploads.push(Promise.reject(errormessage));
	            		continue;
            		}
            	}
                uploads.push(this.uploadFile(i));
            }
            
            return Promise.allSettled(uploads).then(function(results) {
             	var errorMsg = "";
                 results.forEach(function (result) {
                     if (result.status === "fulfilled") {
                     	var value = result.value;
                     	this.fileUploaded(value);
                     } else {
                         var responseText = result.reason.message ? result.reason.message : result.reason;
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
            .then( () => {
                if(this.opts.showFiles) {                    
                	return this.getUploadedFiles();
                }
            });
        }
    
        fileUploaded(fileInfo) {
            $('.admin-cms-media__upload-messages, .admin-cms-media__upload-message.uploading').removeClass('in-progress');
            $('.admin-cms-media__upload-messages, .admin-cms-media__upload-message.success').addClass('in-progress');
        	
            setTimeout( function() {
                $('.admin-cms-media__upload-messages, .admin-cms-media__upload-message.uploading, .admin-cms-media__upload-message.success').removeClass('in-progress');        		
        	}, 5000 );
        }
    
        fileUploadError(responseText) {
        	console.log("fileUploadError", responseText);
            $('.admin-cms-media__upload-messages, .admin-cms-media__upload-message.uploading').removeClass('in-progress');
        	if (responseText) {
                $('.admin-cms-media__upload-messages, .admin-cms-media__upload-message.error').addClass('in-progress');
                $('.admin-cms-media__upload-message.error span').html(responseText);
            }
        }
        
        getUploadedFiles() {
            return fetch(this.opts.postUrl, {
                method: "GET",
       		})
       		.then(response => response.json())
       		.then(json => {
       		    this.uploadedFiles = json;
       		    this.update();
       		})
        }
        
        deleteUploadedFiles() {
            return fetch(this.opts.postUrl, {
                method: "DELETE",
       		})
        }
        
        deleteUploadedFile(file) {
            return fetch(this.opts.postUrl + this.getFilename(file), {
                method: "DELETE",
       		})
        }
        
        deleteFile(data) {
            this.deleteUploadedFile(data.item.file)
            .then( () => {
                this.getUploadedFiles();
            })
        }
    
        uploadFile(i) {
            if (this.files.length <= i) {
                new Modal(this.refs.doneModal).show();
                return new Promise.resolve();
            }
    
            var displayFile = this.displayFiles[i];
            var config = {
                onUploadProgress: (progressEvent) => {
                    displayFile.completed = (progressEvent.loaded * 100) / progressEvent.total;
                    this.update();
                }
            };
            
            return fetch(this.opts.postUrl + this.files[i].name, {
                method: "HEAD",
                redirect: 'follow'
            })
            .then( response => { 
                return response.status == 200;
            })
            .then(exists => {
                if (exists) {
                    const fileName = this.files[i].name;
                    const message = this.opts.msg.overwriteFileConfirm.replace("{0}", fileName);
                    return viewerJS.notifications.confirm(
                        '',
                        this.opts.msg.button__overwrite,
                        undefined,
                        message,
                        {
                            icon: 'warning',
                            confirmButtonClass: 'btn btn--full',
                        }
                    )
                        .catch(() => {
                            throw this.opts.msg.overwriteFileRefused.replace("{0}", fileName);
                        });
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
       		    
       		    return new Promise((resolve, reject) => {
	       		    if(result.ok) {
	       		    	resolve(result);    		        
	       		    } else if(result.body && !result.responseText){
	                   result.body.getReader().read()
						.then(({ done, value }) => {
							reject({
							  responseText:   new TextDecoder("utf-8").decode(value)
							})
						});
	       		    } else {
	       		        reject(result);
	       		    }
       		        
       		    });
       		    
       		}));
        }
        
        getFilename(url) {
            let filename = url.replace(this.opts.postUrl, "");
            if(filename.startsWith("/")) {
                filename = filename.slice(1);
            }
            let filenameEnd = filename.indexOf("/");
            if(filenameEnd > 0) {
                filename = filename.slice(0,filenameEnd);
            }
            return filename;
        }
        
        setDragover(dragover) {
        	this.isDragover = dragover;
        	var dropZone = (this.refs.dropZone); 
        	if(dropZone) {
        		if(dragover) {
        			dropZone.classList.add("isdragover");
        		} else {
        			dropZone.classList.remove("isdragover");
        		}
        	}
        	
        }
    </script> 
</adminMediaUpload>
