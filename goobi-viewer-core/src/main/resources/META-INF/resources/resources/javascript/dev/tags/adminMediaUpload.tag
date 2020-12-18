<adminMediaUpload>
	<div class="admin-cms-media__upload-wrapper">
	    <div class="admin-cms-media__upload {isDragover ? 'is-dragover' : ''}" ref="dropZone">
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
        	this.fileTypes = 'jpg, png, tif, jp2, gif, pdf';
        }
        this.isDragover = false;
    
        this.on('mount', function () {
            
            if(this.opts.showFiles) {
                this.initUploadedFiles();
            }

            this.initDrop();
            
        }.bind(this));

        initDrop() {
			var dropZone = (this.refs.dropZone);
    
            
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
        }
     
        initUploadedFiles() {
			this.getUploadedFiles();
            
            var filesZone = (this.refs.filesZone);
//             var deleteOverlay = (this.refs.deleteOverlay);
            
//             filesZone.addEventListener('mouseenter', function (e) {
//                 deleteOverlay.classList.add("in");
//             });

//             filesZone.addEventListener('mouseleave', function (e) {
//                 deleteOverlay.classList.remove("in");
//             });
            
//             deleteOverlay.addEventListener('click', function (e) {
//                 if(confirm(this.opts.msg.bulkDeleteConfirm)) {
//                     this.deleteUploadedFiles()
//                     .then( () => this.getUploadedFiles())
//                 }
//             }.bind(this));
        }
        
        buttonFilesSelected(e) {
            this.files = [];
            for (var f of e.target.files) {
                console.log("selected file "+ f.name);
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
                console.log("upload file ", i, this.files[i])
                uploads.push(Q(this.uploadFile(i)));
            }
            
            return Q.allSettled(uploads).then(function(results) {
             	var errorMsg = "";
                 results.forEach(function (result) {
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
       		    console.log("uploaded files ", json);
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
            console.log("delete file ", file, this.getFilename(file));
            return fetch(this.opts.postUrl + this.getFilename(file), {
                method: "DELETE",
       		})
        }
        
        deleteFile(data) {
            console.log("delete ", this.getFilename(data.item.file));
            this.deleteUploadedFile(data.item.file)
            .then( () => {
                this.getUploadedFiles();
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
                method: "HEAD",
                redirect: 'follow'
            })
            .then( response => { 
                console.log("HEAD respnse ", response);
                return response.status == 200;
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
        
        getFilename(url) {
            console.log("url" + url);
            console.log("base url " + this.opts.postUrl);
            let filename = url.replace(this.opts.postUrl, "");
            console.log("filename " + filename);
            if(filename.startsWith("/")) {
                filename = filename.slice(1);
            }
            console.log("filename " + filename);
            let filenameEnd = filename.indexOf("/");
            if(filenameEnd > 0) {
                filename = filename.slice(0,filenameEnd);
            }
            console.log("filename " + filename);
            return filename;
        }
    </script> 
</adminMediaUpload>

