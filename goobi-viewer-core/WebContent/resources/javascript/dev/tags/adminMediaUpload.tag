<adminMediaUpload>
    <div class="admin-cms-media__upload" ref="drop_area">
        <div class="admin-cms-media__upload-input">
            <p>
                {opts.msg.uploadText}
                <br />
                <small>({opts.msg.allowedFileTypes}: {fileTypes})</small>
            </p>
            <label for="file" class="btn btn--default">{opts.msg.buttonUpload}</label>
            <input id="file" class="admin-cms-media__upload-file" type="file" multiple="multiple">
            <button class="admin-cms-media__upload-button" type="submit">{opts.msg.buttonUpload}</button>
        </div>
        <div class="admin-cms-media__upload-uploading">{opts.msg.mediaUploading}</div>
        <div class="admin-cms-media__upload-success">{opts.msg.mediaFinished}</div>
        <div class="admin-cms-media__upload-error">{opts.msg.mediaError}:<span></span>.</div>
    </div>

    <script>
        this.files = [];
        this.displayFiles = [];
        this.fileTypes = 'jpg, png, svg, tif, docx, doc, rtf, html, xhtml, xml';
    
        this.on('mount', function () {
            var dropZone = (this.refs.drop_area);
            
            dropZone.addEventListener('dragover', function (e) {
                e.stopPropagation();
                e.preventDefault();
                e.dataTransfer.dropEffect = 'copy';
            });
            
            dropZone.addEventListener("drop", (e) => {
                e.stopPropagation();
                e.preventDefault();
                
                for (var f of e.dataTransfer.files) {
                    this.files.push(f);
                    var sizeUnit = "KB";
                    var size = f.size / 1000;
                    if (size > 1024) {
                        size = size / 1024;
                        sizeUnit = "MB";
                    }
                    if (size > 1024) {
                        size = size / 1024;
                        sizeUnit = "GB";
                    }
                    this.displayFiles.push({ name: f.name, size: Math.floor(size) + " " + sizeUnit, completed: 0 });
                }
    
                this.uploadFiles();
            });
        }.bind(this));
    
        buttonFilesSelected(e) {
            for (var f of e.target.files) {
                this.files.push(f);
                var sizeUnit = "KB";
                var size = f.size / 1000;
                
                if (size > 1024) {
                    size = size / 1024;
                    sizeUnit = "MB";
                }
                if (size > 1024) {
                    size = size / 1024;
                    sizeUnit = "GB";
                }
                
                this.displayFiles.push({ name: f.name, size: Math.floor(size) + " " + sizeUnit, completed: 0 });
            }
    
            this.uploadFiles();
        }
    
        uploadFiles() {
            this.uploadFile(0);
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
            
            $.ajax({
                url: this.opts.postUrl,
                type: 'POST',
                data: data,
                dataType: 'json',
                cache: false,
                contentType: false,
                processData: false,
                complete: function () {}.bind(this),
                success: function (data) {}.bind(this),
                error: function () {}.bind(this)
            });
        }
    </script> 
</adminMediaUpload>