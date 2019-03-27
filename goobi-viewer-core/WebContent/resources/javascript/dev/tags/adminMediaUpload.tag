<adminMediaUpload>
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

    <script>
        this.files = [];
        this.displayFiles = [];
        this.fileTypes = 'jpg, png, svg, tif, docx, doc, rtf, html, xhtml, xml';
        this.isDragover = false;
    
        this.on('mount', function () {
            var dropZone = (this.refs.dropZone);
    
            dropZone.addEventListener('dragover', function (e) {
                e.stopPropagation();
                e.preventDefault();
                e.dataTransfer.dropEffect = 'copy';
    
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
    
                this.uploadFiles();
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
            for (i = 0; i < this.files.length; i++) {
                uploads.push(Q(this.uploadFile(i)));
            }
            Q.allSettled(uploads)
                .then(function (results) {
                    results.forEach(function (result) {
                        if (result.state === "fulfilled") {
                            var value = result.value;
                            this.fileUploaded(value);
                        } else {
                            var reason = result.reason;
                            this.fileUploadError(result);
                        }
                    }.bind(this));
                    //         		if(this.opts.onUploadComplete) {
                    //         			this.opts.onUploadComplete();
                    //         		}
                }.bind(this))
        }
    
        fileUploaded(fileInfo) {
            $('.admin-cms-media__upload-messages, .admin-cms-media__upload-message.success').addClass('in-progress');
        }
    
        fileUploadError(error) {
            var responseText = error.reason.responseText;
            
            if (responseText) {
                $('.admin-cms-media__upload-messages, .admin-cms-media__upload-message.error').addClass('in-progress');
                $('.admin-cms-media__upload-message.error span').text(responseText);
            }
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
            var data = new FormData();
    
            data.append('file', this.files[i])
    
            return $.ajax({
                url: this.opts.postUrl,
                type: 'POST',
                data: data,
                dataType: 'json',
                cache: false,
                contentType: false,
                processData: false
            });
        }
    </script> 
</adminMediaUpload>