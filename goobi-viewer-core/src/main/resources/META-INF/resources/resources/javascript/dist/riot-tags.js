riot.tag2('adminmediaupload', '<div class="admin-cms-media__upload-wrapper"><div class="admin-cms-media__upload {isDragover ? \'is-dragover\' : \'\'}" ref="dropZone"><div class="admin-cms-media__upload-input"><p> {opts.msg.uploadText} <br><small>({opts.msg.allowedFileTypes}: {fileTypes})</small></p><label for="file" class="btn btn--default">{opts.msg.buttonUpload}</label><input id="file" class="admin-cms-media__upload-file" type="file" multiple="multiple" onchange="{buttonFilesSelected}"></div><div class="admin-cms-media__upload-messages"><div class="admin-cms-media__upload-message uploading"><i class="fa fa-spinner fa-pulse fa-fw"></i> {opts.msg.mediaUploading} </div><div class="admin-cms-media__upload-message success"><i class="fa fa-check-square-o" aria-hidden="true"></i> {opts.msg.mediaFinished} </div><div class="admin-cms-media__upload-message error"><i class="fa fa-exclamation-circle" aria-hidden="true"></i><span></span></div></div></div><div if="{this.opts.showFiles}" class="admin-cms-media__list-files {this.uploadedFiles.length > 0 ? \'in\' : \'\'}" ref="filesZone"><div each="{file in this.uploadedFiles}" class="admin-cms-media__list-files__file"><img riot-src="{file}" alt="{getFilename(file)}" title="{getFilename(file)}"><div class="delete_overlay" onclick="{deleteFile}"><i class="fa fa-trash" aria-hidden="true"></i></div></div></div></div>', '', '', function(opts) {
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

            if(this.opts.showFiles) {
                this.initUploadedFiles();
            }

            this.initDrop();

        }.bind(this));

        this.initDrop = function() {
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
        }.bind(this)

        this.initUploadedFiles = function() {
			this.getUploadedFiles();

            var filesZone = (this.refs.filesZone);

        }.bind(this)

        this.buttonFilesSelected = function(e) {
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
        }.bind(this)

        this.uploadFiles = function() {
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
        }.bind(this)

        this.fileUploaded = function(fileInfo) {
            $('.admin-cms-media__upload-messages, .admin-cms-media__upload-message.uploading').removeClass('in-progress');
            $('.admin-cms-media__upload-messages, .admin-cms-media__upload-message.success').addClass('in-progress');

            setTimeout( function() {
                $('.admin-cms-media__upload-messages, .admin-cms-media__upload-message.uploading, .admin-cms-media__upload-message.success').removeClass('in-progress');
        	}, 5000 );
        }.bind(this)

        this.fileUploadError = function(responseText) {
            $('.admin-cms-media__upload-messages, .admin-cms-media__upload-message.uploading').removeClass('in-progress');
        	if (responseText) {
                $('.admin-cms-media__upload-messages, .admin-cms-media__upload-message.error').addClass('in-progress');
                $('.admin-cms-media__upload-message.error span').html(responseText);
            }
        }.bind(this)

        this.getUploadedFiles = function() {
            return fetch(this.opts.postUrl, {
                method: "GET",
       		})
       		.then(response => response.json())
       		.then(json => {
       		    console.log("uploaded files ", json);
       		    this.uploadedFiles = json;
       		    this.update();
       		})
        }.bind(this)

        this.deleteUploadedFiles = function() {
            return fetch(this.opts.postUrl, {
                method: "DELETE",
       		})
        }.bind(this)

        this.deleteUploadedFile = function(file) {
            return fetch(this.opts.postUrl + this.getFilename(file), {
                method: "DELETE",
       		})
        }.bind(this)

        this.deleteFile = function(data) {
            console.log("delete ", this.getFilename(data.item.file));
            this.deleteUploadedFile(data.item.file)
            .then( () => {
                this.getUploadedFiles();
            })
        }.bind(this)

        this.uploadFile = function(i) {
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
        }.bind(this)

        this.getFilename = function(url) {
            let result = url.match(/_tifU002F(.*)\/(?:full|square)/);
            if(result && result.length > 1) {
                return result[1];
            } else {
             	return url;
            }
        }.bind(this)
});


riot.tag2('bookmarklist', '<ul if="{opts.bookmarks.config.userLoggedIn}" class="{mainClass} list"><li each="{bookmarkList in getBookmarkLists()}"><button if="{pi}" class="btn btn--clean" type="button" onclick="{inList(bookmarkList, this.pi, this.page, this.logid) ? remove : add}"><i if="{inList(bookmarkList, this.pi, this.page, this.logid)}" class="fa fa-check" aria-hidden="true"></i> {bookmarkList.name} <span>{bookmarkList.numItems}</span></button><div if="{!pi}" class="row no-margin"><div class="col-9 no-padding"><a href="{opts.bookmarks.getBookmarkListUrl(bookmarkList.id)}">{bookmarkList.name}</a></div><div class="col-2 no-padding icon-list"><a if="{maySendList(bookmarkList)}" href="{sendListUrl(bookmarkList)}" title="{msg(\'bookmarkList_session_mail_sendList\')}"><i class="fa fa-paper-plane-o" aria-hidden="true"></i></a><a href="{searchListUrl(bookmarkList)}" data-toggle="tooltip" data-placement="top" data-original-title="" title="{msg(\'action__search_in_bookmarks\')}"><i class="fa fa-search" aria-hidden="true"></i></a><a href="{miradorUrl(bookmarkList)}" target="_blank" title="{msg(\'viewMiradorComparison\')}"><i class="fa fa-th" aria-hidden="true"></i></a></div><div class="col-1 no-padding"><span class="{mainClass}-counter">{bookmarkList.numItems}</span></div></div></li></ul><ul if="{!opts.bookmarks.config.userLoggedIn}" each="{bookmarkList in getBookmarkLists()}" class="{mainClass} list"><li each="{bookmark in bookmarkList.items}"><div class="row no-margin"><div class="col-4 no-padding"><div class="{mainClass}-image" riot-style="background-image: url({bookmark.representativeImageUrl});"></div></div><div class="col-7 no-padding"><h4><a href="{opts.bookmarks.config.root}{bookmark.url}">{bookmark.name}</a></h4></div><div class="col-1 no-padding {mainClass}-remove"><button class="btn btn--clean" type="button" data-bookshelf-type="delete" onclick="{remove}"><i class="fa fa-ban" aria-hidden="true"></i></button></div></div></li></ul><div if="{!opts.bookmarks.config.userLoggedIn}" each="{bookmarkList in getBookmarkLists()}" class="{mainClass}-actions"><div if="{mayEmptyList(bookmarkList)}" class="{mainClass}-reset"><button class="btn btn--clean" type="button" data-bookshelf-type="reset" onclick="{deleteList}"><span>{msg(\'bookmarkList_reset\')}</span><i class="fa fa-trash-o" aria-hidden="true"></i></button></div><div if="{maySendList(bookmarkList)}" class="{mainClass}-send"><a href="{sendListUrl(bookmarkList)}"><span>{msg(\'bookmarkList_session_mail_sendList\')}</span><i class="fa fa-paper-plane-o" aria-hidden="true"></i></a></div><div if="{maySearchList(bookmarkList)}" class="{mainClass}-search"><a href="{searchListUrl(bookmarkList)}" data-toggle="tooltip" data-placement="top" data-original-title="" title=""><span>{msg(\'action__search_in_bookmarks\')}</span><i class="fa fa-search" aria-hidden="true"></i></a></div><div if="{mayCompareList(bookmarkList)}" class="{mainClass}-mirador"><a href="{miradorUrl(bookmarkList)}" target="_blank"><span>{msg(\'viewMiradorComparison\')}</span><i class="fa fa-th" aria-hidden="true"></i></a></div></div>', '', '', function(opts) {


this.pi = this.opts.data.pi;
this.logid = this.opts.data.logid;
this.page = this.opts.data.page;
this.loader = this.opts.loader;
this.button = this.opts.button;
this.mainClass = (this.opts.style && this.opts.style.mainClass) ? this.opts.style.mainClass : "bookmark-popup__body-list";

this.on( 'mount', function() {
    this.updateLists();
    this.opts.bookmarks.listsUpdated.subscribe( () => this.onListUpdate());
});

this.getBookmarkLists = function() {
    let lists =  this.opts.bookmarks.getBookmarkLists();
    return lists;
}.bind(this)

this.updateLists = function() {
    this.opts.bookmarks.listsNeedUpdate.next();
}.bind(this)

this.onListUpdate = function() {
	this.update();
    this.hideLoader();
}.bind(this)

this.hideLoader = function() {
    $(this.loader).hide();
}.bind(this)

this.showLoader = function() {
    $(this.loader).show();
}.bind(this)

this.add = function(event) {
    let list = event.item.bookmarkList
    this.opts.bookmarks.addToBookmarkList(list.id, this.pi, this.page, this.logid, this.opts.bookmarks.isTypePage())
    .then( () => this.updateLists());
}.bind(this)

this.remove = function(event) {
    if(this.opts.bookmarks.config.userLoggedIn) {
	    let list = event.item.bookmarkList
	    this.opts.bookmarks.removeFromBookmarkList(list.id, this.pi, this.page, this.logid, this.opts.bookmarks.isTypePage())
	    .then( () => this.updateLists())
    } else {
        let bookmark = event.item.bookmark;
        this.opts.bookmarks.removeFromBookmarkList(undefined, bookmark.pi, undefined, undefined, false)
	    .then( () => this.updateLists())
    }
}.bind(this)

this.inList = function(list, pi, page, logid) {
    return this.opts.bookmarks.inList(list, pi, page, logid);
}.bind(this)

this.mayEmptyList = function(list) {
    return list.items.length > 0;
}.bind(this)

this.deleteList = function(event) {
    let list = event.item.bookmarkList
    this.opts.bookmarks.removeBookmarkList(list.id)
    .then( () => this.updateLists());
}.bind(this)

this.maySendList = function(list) {
    return !opts.bookmarks.config.userLoggedIn && list.items.length > 0;
}.bind(this)

this.sendListUrl = function(list) {
	return this.opts.bookmarks.config.root + "/bookmarks/send/";
}.bind(this)

this.maySearchList = function(list) {
    return list.items.length > 0;
}.bind(this)

this.searchListUrl = function(list) {
    let url;
    if(this.opts.bookmarks.config.userLoggedIn) {
	    url = this.opts.bookmarks.config.root + "/user/bookmarks/search/" + list.name + "/";
    } else {
	    url = this.opts.bookmarks.config.root + "/bookmarks/search/" + list.name + "/";
    }
    return url;
}.bind(this)

this.mayCompareList = function(list) {
    return list.items.length > 0;
}.bind(this)

this.miradorUrl = function(list) {
    if(list.id != null) {
    	return this.opts.bookmarks.config.root + "/mirador/id/" + list.id + "/";
    } else {
    	return this.opts.bookmarks.config.root + "/mirador/";
    }
}.bind(this)

this.msg = function(key) {
    return this.opts.bookmarks.translator.translate(key);
}.bind(this)

});


riot.tag2('bookmarkspopup', '<div class="bookmark-popup__body-loader"></div><div if="{opts.data.page !== undefined}" class="bookmark-popup__radio-buttons"><div><label><input type="radio" checked="{opts.bookmarks.isTypeRecord()}" name="bookmarkType" riot-value="{msg(\'bookmarkList_typeRecord\')}" onclick="{setBookmarkTypeRecord}">{msg(\'bookmarkList_typeRecord\')}</label></div><div><label><input type="radio" checked="{opts.bookmarks.isTypePage()}" name="bookmarkType" riot-value="{msg(\'bookmarkList_typePage\')}" onclick="{setBookmarkTypePage}">{msg(\'bookmarkList_typePage\')}</label></div></div><div class="bookmark-popup__header"> {msg(\'bookmarkList_selectBookmarkList\')} </div><div class="bookmark-popup__body"><bookmarklist data="{this.opts.data}" loader="{this.opts.loader}" button="{this.opts.button}" bookmarks="{this.opts.bookmarks}"></bookmarkList></div><div class="bookmark-popup__footer"><div class="row no-margin"><div class="col-11 no-padding"><input ref="inputValue" type="text" placeholder="{msg(\'bookmarkList_addNewBookmarkList\')}"></div><div class="col-1 no-padding"><button class="btn btn-clean" type="button" onclick="{add}"></button></div></div></div>', '', 'class="bookmark-popup bottom"', function(opts) {

const popupOffset = 6;

this.opts.loader = ".bookmark-popup__body-loader";

this.on( 'mount', function() {

	this.setPosition();
	this.addCloseHandler();

});

this.setPosition = function() {
    var $button = $(this.opts.button);
    var anchor = {
            x : $button.offset().left + $button.outerWidth()/2,
            y : $button.offset().top + $button.outerHeight(),
    }
    var position = {
            left: anchor.x - this.root.getBoundingClientRect().width/2,
            top: anchor.y + popupOffset
    }
    $(this.root).offset(position);
}.bind(this)

this.addCloseHandler = function() {

    $(this.root).on("click", function(event){
        event.stopPropagation();
    });

    $('body').one("click", function(event) {
        this.unmount(true);
        $(this.root).off();
        this.root.remove();
    }.bind(this));
}.bind(this)

this.add = function() {
    let name = this.refs.inputValue.value;
    this.refs.inputValue.value = "";
    this.opts.bookmarks.addBookmarkList(name)
    .then( () => {
        this.opts.bookmarks.listsNeedUpdate.next();
        this.update();
    })
}.bind(this)

this.setBookmarkTypeRecord = function() {
    this.opts.bookmarks.setTypeRecord();
    this.opts.bookmarks.listsNeedUpdate.next();
}.bind(this)

this.setBookmarkTypePage = function() {
    this.opts.bookmarks.setTypePage();
    this.opts.bookmarks.listsNeedUpdate.next();
}.bind(this)

this.hideLoader = function() {
    $(this.opts.data.loader).hide();
}.bind(this)

this.showLoader = function() {
    $(this.opts.data.loader).show();
}.bind(this)

this.msg = function(key) {
    return this.opts.bookmarks.translator.translate(key);
}.bind(this)

});
riot.tag2('campaignitem', '<div if="{!opts.pi}" class="content"> {Crowdsourcing.translate(⁗crowdsourcing__error__no_item_available⁗)} </div><div if="{opts.pi}" class="content"><span if="{this.loading}" class="loader_wrapper"><img riot-src="{this.opts.loaderimageurl}"></span><span if="{this.error}" class="loader_wrapper"><span class="error_message">{this.error.message}</span></span></span><div class="content_left"><imageview if="{this.item}" id="mainImage" source="{this.item.getCurrentCanvas()}" item="{this.item}"></imageView><canvaspaginator if="{this.item}" item="{this.item}"></canvasPaginator></div><div if="{this.item}" class="content_right"><h1 class="content_right__title">{Crowdsourcing.translate(this.item.translations.title)}</h1><div class="questions_wrapper"><div each="{question, index in this.item.questions}" onclick="{setActive}" class="question_wrapper {question.isRegionTarget() ? \'area-selector-question\' : \'\'} {question.active ? \'active\' : \'\'}"><div class="question_wrapper__description">{Crowdsourcing.translate(question.translations.text)}</div><plaintextquestion if="{question.questionType == \'PLAINTEXT\'}" question="{question}" item="{this.item}" index="{index}"></plaintextQuestion><geolocationquestion if="{question.questionType == \'GEOLOCATION_POINT\'}" question="{question}" item="{this.item}" index="{index}"></geoLocationQuestion></div></div><div if="{!item.isReviewMode()}" class="options-wrapper options-wrapper-annotate"><button onclick="{saveAnnotations}" class="options-wrapper__option btn btn--default" id="save">{Crowdsourcing.translate(⁗button__save⁗)}</button><div>{Crowdsourcing.translate(⁗label__or⁗)}</div><button onclick="{submitForReview}" class="options-wrapper__option btn btn--success" id="review">{Crowdsourcing.translate(⁗action__submit_for_review⁗)}</button><div>{Crowdsourcing.translate(⁗label__or⁗)}</div><button if="{this.opts.nextitemurl}" onclick="{skipItem}" class="options-wrapper__option btn btn--link" id="skip">{Crowdsourcing.translate(⁗action__skip_item⁗)}</button></div><div if="{item.isReviewMode()}" class="options-wrapper options-wrapper-review"><button onclick="{acceptReview}" class="options-wrapper__option btn btn--success" id="accept">{Crowdsourcing.translate(⁗action__accept_review⁗)}</button><div>{Crowdsourcing.translate(⁗label__or⁗)}</div><button onclick="{rejectReview}" class="options-wrapper__option btn btn--danger" id="reject">{Crowdsourcing.translate(⁗action__reject_review⁗)}</button><div>{Crowdsourcing.translate(⁗label__or⁗)}</div><button if="{this.opts.nextitemurl}" onclick="{skipItem}" class="options-wrapper__option btn btn--link" id="skip">{Crowdsourcing.translate(⁗action__skip_item⁗)}</button></div></div></div>', '', '', function(opts) {

	this.itemSource = this.opts.restapiurl + "crowdsourcing/campaigns/" + this.opts.campaign + "/" + this.opts.pi + "/";
	this.annotationSource = this.itemSource + "annotations/";
	this.loading = true;

	this.on("mount", function() {
	    fetch(this.itemSource)
	    .then( response => response.json() )
	    .then( itemConfig => this.loadItem(itemConfig))
	    .then( () => this.fetch(this.annotationSource))
	    .then( response => response.json() )
	    .then( annotations => this.initAnnotations(annotations))
	    .then( () => this.item.notifyItemInitialized())
		.catch( error => {
		    console.error("ERROR ", error);
	    	this.error = error;
	    	this.update();
		})
	});

	this.loadItem = function(itemConfig) {
	    console.log("load item ", itemConfig);
	    this.item = new Crowdsourcing.Item(itemConfig, 0);
	    this.item.setReviewMode(this.opts.itemstatus && this.opts.itemstatus.toUpperCase() == "REVIEW");
		fetch(this.item.imageSource + "?mode=simple")
		.then( response => response.json() )
		.then( imageSource => this.initImageView(imageSource))
		.then( () => {this.loading = false; this.update()})
		.catch( error => {
		    this.loading = false;
		    console.error("ERROR ", error);
		})

		this.item.onImageRotated( () => this.update());
	}.bind(this)

	this.initImageView = function(imageSource) {
	    this.item.initViewer(imageSource)
	    this.update();
	}.bind(this)

	this.resolveCanvas = function(source) {
	    if(Crowdsourcing.isString(source)) {
	        return fetch(source)
	        .then( response => response.json() );
	    } else {
	        return Q.fcall(() => source);
	    }
	}.bind(this)

	this.initAnnotations = function(annotations) {
	    let save = this.item.createAnnotationMap(annotations);
	    this.item.saveToLocalStorage(save);
	}.bind(this)

	this.resetItems = function() {
	    fetch(this.annotationSource)
	    .then( response => response.json() )
	    .then( annotations => this.initAnnotations(annotations))
	    .then( () => this.resetQuestions())
	    .then( () => this.item.notifyAnnotationsReload())
	    .then( () => this.update())
		.catch( error => console.error("ERROR ", error));
	}.bind(this)

	this.resetQuestions = function() {
	    this.item.questions.forEach(question => {
		    question.loadAnnotationsFromLocalStorage();
		    question.initAnnotations();
	    })
	}.bind(this)

	this.setActive = function(event) {
	    if(event.item.question.isRegionTarget()) {
		    this.item.questions.forEach(q => q.active = false);
		    event.item.question.active = true;
	    }
	}.bind(this)

	this.saveToServer = function() {
	    let pages = this.item.loadAnnotationPages();
	    this.loading = true;
	    this.update();
	    return fetch(this.annotationSource, {
            method: "PUT",
            headers: {
                'Content-Type': 'application/json'
            },
            cache: "no-cache",
            mode: 'cors',
            body: JSON.stringify(pages)
	    })
	}.bind(this)

	this.saveAnnotations = function() {
	    this.saveToServer()
	    .then(() => this.resetItems())
	    .then(() => this.setStatus("ANNOTATE"))
	    .catch((error) => {
	        console.error(error);
	    })
	    .then(() => {
	        this.loading = false;
		    this.update();
	    });
	}.bind(this)

	this.submitForReview = function() {
	    this.saveToServer()
	    .then(() => this.setStatus("REVIEW"))
	    .then(() => this.skipItem());

	}.bind(this)

	this.acceptReview = function() {
	    this.setStatus("FINISHED")
	    .then(() => this.skipItem());
	}.bind(this)

	this.rejectReview = function() {
	    this.setStatus("ANNOTATE")
	    .then(() => this.skipItem());
	}.bind(this)

	this.skipItem = function() {
	    window.location.href = this.opts.nextitemurl;
	}.bind(this)

	this.setStatus = function(status) {
	    let body = {
	            recordStatus: status,
	            creator: this.item.getCreator().id
	    }
	    return fetch(this.itemSource, {
            method: "PUT",
            headers: {
                'Content-Type': 'application/json',
            },
            cache: "no-cache",
            mode: 'cors',
            body: JSON.stringify(body)
	    })
	}.bind(this)

	this.fetch = function(url) {
	    return fetch(url, {
            method: "GET",
            cache: "no-cache",
            mode: 'cors',
	    })
	}.bind(this)

});
riot.tag2('canvaspaginator', '<nav class="numeric-paginator"><ul><li if="{getCurrentIndex() > 0}" class="numeric-paginator__navigate navigate_prev"><span onclick="{this.loadPrevious}"><i class="fa fa-angle-left" aria-hidden="true"></i></span></li><li each="{canvas in this.firstCanvases()}" class="group_left {this.getIndex(canvas) == this.getCurrentIndex() ? \'numeric-paginator__active\' : \'\'}"><span index="{this.getIndex(canvas)}" onclick="{this.loadFromEvent}">{this.getOrder(canvas)}</span></li><li class="numeric-paginator__separator" if="{this.useMiddleButtons()}">...</li><li each="{canvas in this.middleCanvases()}" class="group_middle {this.getIndex(canvas) == this.getCurrentIndex() ? \'numeric-paginator__active\' : \'\'}"><span index="{this.getIndex(canvas)}" onclick="{this.loadFromEvent}">{this.getOrder(canvas)}</span></li><li class="numeric-paginator__separator" if="{this.useLastButtons()}">...</li><li each="{canvas in this.lastCanvases()}" class="group_right {this.getIndex(canvas) == this.getCurrentIndex() ? \'numeric-paginator__active\' : \'\'}"><span index="{this.getIndex(canvas)}" onclick="{this.loadFromEvent}">{this.getOrder(canvas)}</span></li><li if="{getCurrentIndex() < getTotalImageCount()-1}" class="numeric-paginator__navigate navigate_next"><span onclick="{this.loadNext}"><i class="fa fa-angle-right" aria-hidden="true"></i></span></li></ul></nav>', '', '', function(opts) {

this.on( "mount", function() {

    var paginatorConfig = {
	        previous: () => this.load(this.getCurrentIndex()-1),
	        next: () => this.load(this.getCurrentIndex()+1),
	        first: () => this.load(0),
	        last: () => this.load(this.getTotalImageCount()-1),
	}
	viewerJS.paginator.init(paginatorConfig);

})

this.loadFromEvent = function(e) {
    let index = parseInt(e.target.attributes["index"].value);
	this.load(index);
}.bind(this)

this.load = function(index) {
    if(index != this.getCurrentIndex() && index >= 0 && index < this.getTotalImageCount()) {
		this.opts.item.loadImage(index);
		this.update();
    }
}.bind(this)

this.loadPrevious = function() {
    let index = this.getCurrentIndex()-1;
	this.load(index);
}.bind(this)

this.loadNext = function() {
    let index = this.getCurrentIndex()+1;
	this.load(index);
}.bind(this)

this.getCurrentIndex = function() {
    return this.opts.item.currentCanvasIndex;
}.bind(this)

this.getIndex = function(canvas) {
    return this.opts.item.canvases.indexOf(canvas);
}.bind(this)

this.getOrder = function(canvas) {
    return this.getIndex(canvas) + 1;
}.bind(this)

this.getTotalImageCount = function() {
    return this.opts.item.canvases.length;
}.bind(this)

this.useMiddleButtons = function() {
    return this.getTotalImageCount() > 9 && this.getCurrentIndex() > 4 && this.getCurrentIndex() < this.getTotalImageCount()-5;
}.bind(this)

this.useLastButtons = function() {
    return this.getTotalImageCount() > 9;
}.bind(this)

this.firstCanvases = function() {
    if(this.getTotalImageCount() < 10) {
        return this.opts.item.canvases;
    } else if(this.getCurrentIndex() < 5) {
        return this.opts.item.canvases.slice(0, this.getCurrentIndex()+3);
    } else {
        return this.opts.item.canvases.slice(0, 2);
    }
}.bind(this)

this.middleCanvases = function() {
    if(this.getTotalImageCount() < 10) {
        return [];
    } else if(this.getCurrentIndex() < this.getTotalImageCount()-5 && this.getCurrentIndex() > 4) {
        return this.opts.item.canvases.slice(this.getCurrentIndex()-2, this.getCurrentIndex()+3);
    } else {
        return [];
    }
}.bind(this)

this.lastCanvases = function() {
    if(this.getTotalImageCount() < 10) {
        return [];
    } else if(this.getCurrentIndex() < this.getTotalImageCount()-5) {
        return this.opts.item.canvases.slice(this.getTotalImageCount()-2);
    } else {
        return this.opts.item.canvases.slice(this.getCurrentIndex()-2);
    }
}.bind(this)

});














riot.tag2('slideshow', '<a if="{manifest === undefined}" data-linkid="{opts.pis}"></a><figure class="slideshow" if="{manifest !== undefined}" onmouseenter="{mouseenter}" onmouseleave="{mouseleave}"><div class="slideshow__image"><a href="{getLink(manifest)}" class="remember-scroll-position" data-linkid="{opts.pis}" onclick="{storeScrollPosition}"><img riot-src="{getThumbnail(manifest)}" class="{\'active\' : active}" alt="{getLabel(manifest)}" onload="{setImageActive}"></a></div><figcaption><h4>{getTitleOrLabel(manifest)}</h4><p><span each="{md in metadataList}"> {getMetadataValue(manifest, md)} <br></span></p><div if="{pis.length > 1}" class="slideshow__dots"><ul><li each="{imagepi in pis}"><button class="btn btn--clean {\'active\' : pi === imagepi}" onclick="{setPi}"></button></li></ul></div></figcaption></figure>', '', '', function(opts) {

    	$.fn.isInViewport = function() {
        	var elementTop = $( this ).offset().top;
        	var elementBottom = elementTop + $( this ).outerHeight();
        	var elementHeight = $( this ).outerHeight();
        	var viewportTop = $( window ).scrollTop();
        	var viewportBottom = viewportTop + $( window ).height();

        	return elementBottom > (viewportTop + elementHeight) && elementTop < (viewportBottom - elementHeight);
    	};

    	this.pis = this.opts.pis.split(/[\s,;]+/);
    	this.pis = this.pis.filter( function( pi ) {
    		return pi != undefined && pi.length > 0;
    	} );
        this.metadataList = this.opts.metadata.split(/[,;]+/);
        this.manifest = undefined;
        this.manifests = new Map();
        this.active = false;
        this.visible = false;
        this.mouseover = false;

        this.on( 'mount', function() {
        	this.loadManifest( this.pis[0] );
        }.bind( this ));

        this.mouseenter = function() {
        	this.mouseover = true;
        }.bind(this)

        this.mouseleave = function() {
        	this.mouseover = false;
        }.bind(this)

        this.checkPosition = function() {
        	var slideshow = $( '#' + this.opts.id + ' figure' );

        	if ( !this.visible && this.pis.length > 1 && slideshow.isInViewport() ) {
        		this.visible = true;
            	this.moveSlides( this.pis, true );
        	}
        	else if ( this.visible && !slideshow.isInViewport() ) {
        		this.visible = false;
        		this.moveSlides( this.pis, false );
        	}
        }.bind(this)

        this.moveSlides = function( pis, move ) {
        	var index = 1;

        	if ( move ) {
        		clearInterval( this.interval );

        		this.interval = setInterval( function() {
                	if ( index === pis.length ) {
                		index = 0;
                	}
                	if ( !this.mouseover ) {
            			this.loadManifest( pis[ index ] );
                    	index++;
                	}
                }.bind( this ), 3000 );
        	}
        	else {
        		clearInterval( this.interval );
        	}
        }.bind(this)

        this.setPi = function( event ) {
        	let pi = event.item.imagepi;

        	if ( pi != this.pi ) {
        		this.pi = pi;

        		return this.loadManifest( pi );
        	}
        }.bind(this)

        this.setImageActive = function() {
        	this.active = true;
        	this.update();
        }.bind(this)

        this.loadManifest = function( pi ) {
        	let url = this.opts.manifest_base_url.replace( "{pi}", pi );
        	let json = this.manifests.get( url );
        	this.pi = pi;
        	this.active = false;
        	this.update();

        	if ( !json ) {
        		$.getJSON( url, function( manifest ) {
        			if ( manifest ) {

        				this.manifest = manifest;
        				this.manifests.set( url, manifest );
        				this.update();
            			this.checkPosition();

        				$( window ).on( 'resize scroll', function() {
            				this.checkPosition();
        				}.bind( this ) );
        			}
        		}.bind( this ))
        		.then(function(data) {
        		})
        		.catch(function(error) {
        			console.error("error laoding ", url, ": ", error);
        		});
        	}
        	else {

            	setTimeout( function() {
            		this.manifest = json;
            		this.update();
            	}.bind( this ), 300 );
        	}
        }.bind(this)
        this.getThumbnail = function( manifest, width, height ) {
        	if( !manifest.thumbnail.service || ( !width && !height ) ) {
        		return manifest.thumbnail['@id'];
        	}
        	else {
        		let sizePrefix = width && height ? "!" : "";

        		return manifest.thumbnail.service['@id'] + "/full/" + sizePrefix + width + "," + height + "/0/default.jpg";
        	}
        }.bind(this)

        this.getLink = function( manifest ) {
        	rendering = manifest.rendering;

        	if ( Array.isArray( rendering ) ) {
        		rendering = rendering.find( ( rend ) => rend.format == "text/html" );
        	}
        	if ( rendering ) {
        		return rendering['@id'];
        	}
        	else {
        		return '';
        	}
        }.bind(this)

        this.getTitleOrLabel = function( manifest ) {
        	var title = this.getMetadataValue( manifest, 'Title' );

        	if(title) {
        		return title;
        	} else {
        		return getLabel( manifest );
        	}
        }.bind(this)

        this.getLabel = function( manifest ) {
        	return this.getValue(manifest.label, this.opts.locale);
        }.bind(this)

        this.getMetadataValue = function( manifest, metadataLabel ) {
        	if ( manifest && metadataLabel ) {
        		let metadata = manifest.metadata.find( ( md ) => {
        			let label = md.label;
        			if ( Array.isArray( label ) ) {
        				label = label.find( (l) => l['@value'].trim() == metadataLabel.trim());
        				if ( label ) {
        					label = label['@value']
        				}
        			}
        			return label && label.trim() == metadataLabel.trim();
        		});

        		if ( metadata ) {
        			let value = this.getValue( metadata.value, this.opts.locale );

        			return value;
        		}
        	}
        }.bind(this)

        this.getValue = function ( element, locale ) {
            if ( element ) {
            	if ( typeof element === 'string' ) {
            		return element;
            	}
        		else if ( Array.isArray( element ) ) {
            		var fallback;

            		for ( var index in element  ) {
            			var item = element[index];

            			if ( typeof item === 'string' ) {
            				return item;
            			}
            			else {
            				var value = item['@value'];
            				var language = item['@language'];

            				if ( locale == language ) {
            					return value;
            				}
            				else if ( !fallback || language == 'en' ) {
            					fallback = value;
            				}
            			}
            		}

            		return fallback;
            	}
            	else {
            		return element['@value'];
            	}
            }
        }.bind(this)

        this.storeScrollPosition = function(event) {
            $target = $(event.target).closest("a");
            viewerJS.handleScrollPositionClick($target);
        }.bind(this)
});

riot.tag2('collectionlist', '<div if="{collections}" each="{collection, index in collections}" class="card-group"><div class="card" role="tablist"><div class="card-header"><div class="card-thumbnail"><img if="{collection.thumbnail}" class="img-fluid" riot-src="{collection.thumbnail[\'@id\']}"></div><h4 class="card-title"><a if="{!hasChildren(collection)}" href="{collection.rendering[0][\'@id\']}">{getValue(collection.label)} ({viewerJS.iiif.getContainedWorks(collection)})</a><a if="{hasChildren(collection)}" class="collapsed" href="#collapse-{this.opts.setindex}-{index}" role="button" data-toggle="collapse" aria-expanded="false"><span>{getValue(collection.label)} ({viewerJS.iiif.getContainedWorks(collection)})</span><i class="fa fa-angle-flip" aria-hidden="true"></i></a></h4><div class="tpl-stacked-collection__actions"><div class="tpl-stacked-collection__info-toggle"><a if="{hasDescription(collection)}" href="#description-{this.opts.setindex}-{index}" role="button" data-toggle="collapse" aria-expanded="false"><i class="fa fa-info-circle" aria-hidden="true"></i></a></div><div class="card-rss"><a href="{viewerJS.iiif.getRelated(collection, \'Rss feed\')[\'@id\']}"><i class="fa fa-rss" aria-hidden="true"></i></a></div></div></div><div if="{hasDescription(collection)}" id="description-{this.opts.setindex}-{index}" class="card-collapse collapse" role="tabcard" aria-expanded="false"><p class="tpl-stacked-collection__long-info"> {getDescription(collection)} </p></div><div if="{hasChildren(collection)}" id="collapse-{this.opts.setindex}-{index}" class="card-collapse collapse" role="tabcard" aria-expanded="false"><div class="card-body"><ul if="{collection.members && collection.members.length > 0}" class="list"><li each="{child in getChildren(collection)}"><a class="card-body__collection" href="{child.rendering[0][\'@id\']}">{getValue(child.label)} ({viewerJS.iiif.getContainedWorks(child)})</a><a class="card-body__rss" href="{viewerJS.iiif.getRelated(child, \'Rss feed\')[\'@id\']}" target="_blank"><i class="fa fa-rss" aria-hidden="true"></i></a></li></ul></div></div></div></div>', '', 'class="tpl-stacked-collection__collection-list"', function(opts) {

this.collections = this.opts.collections;

this.on("mount", () => {

    this.loadSubCollections();
})

this.loadSubCollections = function() {
    let promises = [];

    let subject = new Rx.Subject();
    this.collections.forEach( child => {
        fetch(child['@id'])
        .then( result => result.json())
        .then(json => {
            child.members = json.members;
            subject.next(child);
        })
        .catch( error => {
           subject.error(error);
        });
    });

    subject
    .pipe(RxOp.debounceTime(100))
    .subscribe( () => this.update())
}.bind(this)

this.getValue = function(element) {
    return viewerJS.iiif.getValue(element, this.opts.language);
}.bind(this)

this.hasChildren = function(element) {
    let count = viewerJS.iiif.getChildCollections(element);
    return count > 0;
}.bind(this)

this.getChildren = function(collection) {
    return collection.members.filter( child => viewerJS.iiif.isCollection(child));
}.bind(this)

this.hasDescription = function(element) {
    return element.description != undefined;
}.bind(this)

this.getDescription = function(element) {
    return this.getValue(element.description);
}.bind(this)

});


riot.tag2('collectionview', '<div each="{set, index in collectionSets}"><h3 if="{set[0] != \'\'}">{translator.translate(set[0])}</h3><collectionlist collections="{set[1]}" language="{opts.language}" setindex="{index}"></collectionlist></div>', '', '', function(opts) {

this.collectionSets = [];

this.on("mount", () => {

    this.fetchCollections()
    .then( () => {
        let keys = this.collectionSets.map(set => set[0]);
        this.translator = new viewerJS.Translator(keys, this.opts.restapi, this.opts.language);
    	return this.translator.init();
    })
    .then( () => {
        this.update();
    })
})

this.fetchCollections = function() {
    let url = this.opts.url;
    if(this.opts.baseCollection) {
        url += this.opts.baseCollection + "/";
    }
    if(this.opts.grouping) {
        url += "?grouping=" + this.opts.grouping;
    }
    return fetch(url)
    .then( result => result.json())
    .then( json => this.buildSets(json))
    .then( sets => this.collectionSets = sets);
}.bind(this)

this.buildSets = function(collection) {
    let map = new Map();
    collection.members
    .filter( member => viewerJS.iiif.isCollection(member))
    .forEach( member => {
        let tagList = viewerJS.iiif.getTags(member, "grouping");
        if(tagList == undefined || tagList.length == 0) {
            this.addToMap(map, "", member);
        } else {
            tagList.forEach(tag => {
               this.addToMap(map, tag, member);
            });
        }
    })
    let entries = Array.from(map.entries());
	entries.sort( (e1,e2) => {
	   	 let key1 = e1[0];
	   	 let key2 = e2[0];
	   	 if(key1 == "" && key2 != "") {
	   	     return 1;
	   	 } else if(key2 == "" && key1 != "") {
	   	     return -1;
	   	 } else {
	   	     return key1.localeCompare(key2);
	   	 }
	});
    return entries;
}.bind(this)

this.addToMap = function(map, key, value) {
    let list = map.get(key);
    if(list === undefined) {
        list = [];
        map.set(key, list);
    }
    list.push(value);
}.bind(this)

});
riot.tag2('fsthumbnailimage', '<div class="fullscreen__view-image-thumb-preloader" if="{preloader}"></div><img ref="image" alt="Thumbnail Image">', '', '', function(opts) {
    	this.preloader = false;

    	this.on('mount', function() {
    		this.createObserver();

    		this.refs.image.onload = function() {
        		this.refs.image.classList.add( 'in' );
				this.opts.observable.trigger( 'imageLoaded', this.opts.thumbnail );
        		this.preloader = false;
        		this.update();
    		}.bind(this);
    	}.bind(this));

    	this.createObserver = function() {
    		var observer;
    		var options = {
    			root: document.querySelector(this.opts.root),
    		    rootMargin: "1000px 0px 1000px 0px",
    		    threshold: 0.8
    		};

    		observer = new IntersectionObserver(this.loadImages, options);
    		observer.observe(this.refs.image);
    	}.bind(this)

    	this.loadImages = function(entries, observer) {
    		entries.forEach( entry => {
    			if (entry.isIntersecting) {
    				this.preloader = true;
    				this.refs.image.src = this.opts.imgsrc;
    				this.update();
    			}
    		} );
    	}.bind(this)
});
riot.tag2('fsthumbnails', '<div class="fullscreen__view-image-thumbs" ref="thumbnailWrapper"><div each="{thumbnail in thumbnails}" class="fullscreen__view-image-thumb"><figure class="fullscreen__view-image-thumb-image"><a href="{thumbnail.rendering[\'@id\']}"><fsthumbnailimage thumbnail="{thumbnail}" observable="{observable}" root=".fullscreen__view-image-thumbs-wrapper" imgsrc="{thumbnail.thumbnail[\'@id\']}"></fsThumbnailImage></a><figcaption><div class="fullscreen__view-image-thumb-image-order {thumbnail.loaded ? \'in\' : \'\'}">{thumbnail.label}</div></figcaption></figure></div></div>', '', '', function(opts) {
        function rmObservable() {
    		riot.observable( this );
    	}

    	this.observable = new rmObservable();
        this.thumbnails = [];
    	this.wrapper = document.getElementsByClassName( 'fullscreen__view-image-thumbs-wrapper' );
    	this.controls = document.getElementsByClassName( 'image-controls' );
    	this.image = document.getElementById( 'imageContainer' );
    	this.viewportWidth;
    	this.sidebarWidth;
    	this.thumbsWidth;

    	this.on( 'mount', function() {
        	$( '[data-show="thumbs"]' ).on( 'click', function(e) {
        		e.currentTarget.classList.toggle('in');

        		if ( e.currentTarget.classList.contains( 'in' ) ) {
            		$( '[data-show="thumbs"]' ).attr( 'title', opts.msg.hideThumbs ).tooltip( '_fixTitle' ).tooltip( 'show' );
        		}
        		else {
            		$( '[data-show="thumbs"]' ).attr( 'title', opts.msg.showThumbs ).tooltip( '_fixTitle' ).tooltip( 'show' );
        		}

        		console.log("controls", this.controls);
        		for (let control of this.controls) {
        		    control.classList.toggle( 'faded' );
        		};

            	this.viewportWidth = document.getElementById( 'fullscreen' ).offsetWidth;
            	this.sidebarWidth = document.getElementById( 'fullscreenViewSidebar' ).offsetWidth;
            	if ( sessionStorage.getItem( 'fsSidebarStatus' ) === 'false' ) {
                	this.thumbsWidth = this.viewportWidth;
            	}
            	else {
                	this.thumbsWidth = this.viewportWidth - this.sidebarWidth;
            	}

            	let visibility = $( this.image ).css('visibility');
            	if(visibility == 'hidden') {
            		$( this.image ).css('visibility','visible');
            	} else {
            		$( this.image ).css('visibility','hidden');
            	}

        		$( this.wrapper ).width( this.thumbsWidth ).fadeToggle( 'fast' );

            	if ( this.thumbnails.length == 0 ) {

            		$.ajax( {
                        url: opts.thumbnailUrl,
                        type: "GET",
                        datatype: "JSON"
                    } ).then( function( data ) {
                    	this.thumbnails = data;
                    	this.update();
                    }.bind( this ) );
    			}
        	}.bind(this));
    	}.bind( this ) );

    	this.observable.on( 'imageLoaded', function( thumbnail ) {
    		thumbnail.loaded = true;
    		this.update();
    	}.bind( this ) );
});
riot.tag2('geolocationquestion', '<div if="{this.showInstructions()}" class="annotation_instruction"><label>{Crowdsourcing.translate(⁗crowdsourcing__help__create_rect_on_image⁗)}</label></div><div if="{this.showAddMarkerInstructions()}" class="annotation_instruction"><label>{Crowdsourcing.translate(⁗crowdsourcing__help__add_marker_to_image⁗)}</label></div><div if="{this.showInactiveInstructions()}" class="annotation_instruction annotation_instruction_inactive"><label>{Crowdsourcing.translate(⁗crowdsourcing__help__make_active⁗)}</label></div><div id="geoMap_{opts.index}" class="geo-map"></div><div id="annotation_{index}" each="{anno, index in this.annotations}"></div>', '', '', function(opts) {


this.question = this.opts.question;
this.annotationToMark = null;
this.addMarkerActive = !this.question.isRegionTarget() && !this.opts.item.isReviewMode();

this.on("mount", function() {
	this.opts.item.onItemInitialized( () => {
	    this.question.initializeView((anno) => new Crowdsourcing.Annotation.GeoJson(anno), this.addAnnotation, this.updateAnnotation, this.focusAnnotation);
	    this.initMap();
	    this.opts.item.onImageOpen(() => this.resetFeatures());
	    this.opts.item.onAnnotationsReload(() => this.resetFeatures());
	})
});

this.setView = function(view) {
    this.map.setView(view.center, view.zoom);
}.bind(this)

this.resetFeatures = function() {
    this.setFeatures(this.question.annotations);
    if(this.geoMap.getMarkerCount() > 0) {
        let zoom = 12;
        if(this.geoMap.getMarkerCount() == 1) {
            let marker = this.geoMap.getMarker(this.question.annotations[0].markerId);
            if(marker) {
            	zoom = marker.feature.view.zoom;
            }
        }
        let featureView = this.geoMap.getViewAroundFeatures(zoom);
	    this.geoMap.setView(featureView);
    }
}.bind(this)

this.setFeatures = function(annotations) {
    this.geoMap.resetMarkers();
    annotations.filter(anno => !anno.isEmpty()).forEach((anno) => {
        let marker = this.geoMap.addMarker(anno.body);
        anno.markerId = marker.getId();
    });
}.bind(this)

this.addAnnotation = function(anno) {
   this.addMarkerActive = true;
   this.annotationToMark = anno;
   if(this.question.areaSelector) {
       this.question.areaSelector.disableDrawer();
   }
   this.update();
}.bind(this)

this.updateAnnotation = function(anno) {
    this.focusAnnotation(this.question.getIndex(anno));
}.bind(this)

this.focusAnnotation = function(index) {
    let anno = this.question.getByIndex(index);
    if(anno) {
        let marker = this.geoMap.getMarker(anno.markerId);
        if(marker) {
	        console.log("focus ", anno, marker);

        }
    }
}.bind(this)

this.showInstructions = function() {
    return !this.addMarkerActive && !this.opts.item.isReviewMode() &&  this.question.active && this.question.isRegionTarget();
}.bind(this)

this.showInactiveInstructions = function() {
    return !this.opts.item.isReviewMode() &&  !this.question.active && this.question.isRegionTarget() && this.opts.item.questions.filter(q => q.isRegionTarget()).length > 1;

}.bind(this)

this.showAddMarkerInstructions = function() {
    return this.addMarkerActive && !this.opts.item.isReviewMode() &&  this.question.active && this.question.isRegionTarget() ;

}.bind(this)

this.showAddAnnotationButton = function() {
    return !this.question.isReviewMode() && !this.question.isRegionTarget() && this.question.mayAddAnnotation();
}.bind(this)

this.setNameFromEvent = function(event) {
    event.preventUpdate = true;
    if(event.item.anno) {
        anno.setName(event.target.value);
        this.question.saveToLocalStorage();
    } else {
        throw "No annotation to set"
    }
}.bind(this)

this.initMap = function() {
    this.geoMap = new viewerJS.GeoMap({
        mapId : "geoMap_" + this.opts.index,
        initialView : {
            zoom: 5,
            center: [11.073397, 49.451993]
        },
        allowMovingFeatures: !this.opts.item.isReviewMode(),
        language: Crowdsourcing.translator.language,
        popover: undefined,
        emptyMarkerMessage: undefined,
        popoverOnHover: false,
    })
    this.geoMap.init();

    this.geoMap.onFeatureMove.subscribe(feature => this.moveFeature(feature));
    this.geoMap.onFeatureClick.subscribe(feature => this.removeFeature(feature));
    this.geoMap.onMapClick.subscribe(geoJson => {
        if(this.addMarkerActive && (this.question.targetFrequency == 0 || this.geoMap.getMarkerCount() < this.question.targetFrequency)) {
            let marker = this.geoMap.addMarker(geoJson);
            if(this.annotationToMark) {
                this.annotationToMark.markerId = marker.getId();
                this.updateFeature(marker.getId());
            } else {
            	this.addFeature(marker.getId());
            }
	        this.addMarkerActive = !this.question.isRegionTarget();
	        if(this.question.areaSelector) {
	            this.question.areaSelector.enableDrawer();
	        }
        }
    })
}.bind(this)

this.getAnnotation = function(id) {
    return this.question.annotations.find(anno => anno.markerId == id);
}.bind(this)

this.updateFeature = function(id) {
    let annotation = this.getAnnotation(id);
    let marker = this.geoMap.getMarker(annotation.markerId);
    annotation.setBody(marker.feature);
    annotation.setView(marker.feature.view);
    this.question.saveToLocalStorage();
}.bind(this)

this.addFeature = function(id) {
    let marker = this.geoMap.getMarker(id);
    let annotation = this.question.addAnnotation();
    annotation.markerId = id;
    annotation.setBody(marker.feature);
    annotation.setView(marker.feature.view);
    this.question.saveToLocalStorage();
}.bind(this)

this.moveFeature = function(feature) {
    let annotation = this.getAnnotation(feature.id);
    if(annotation) {
        annotation.setGeometry(feature.geometry);
        annotation.setView(feature.view);
    }
    this.question.saveToLocalStorage();
}.bind(this)

this.removeFeature = function(feature) {
    this.geoMap.removeMarker(feature);
	let annotation = this.getAnnotation(feature.id);
    if(annotation) {
	    this.question.deleteAnnotation(annotation);
	    this.question.saveToLocalStorage();
    }
}.bind(this)

});


riot.tag2('imagecontrols', '<div class="image_controls"><div class="image-controls__actions"><div class="image-controls__action rotate-left"><a onclick="{rotateLeft}"><i class="image-rotate_left"></i></a></div><div class="image-controls__action rotate-right"><a onclick="{rotateRight}"><i class="image-rotate_right"></i></a></div><div class="image-controls__action zoom-slider-wrapper"><div class="zoom-slider"><div class="zoom-slider-handle"></div></div></div></div></div>', '', '', function(opts) {
    this.on( "mount", function() {

    } );

    this.rotateRight = function()
    {
        if ( this.opts.image ) {
            this.opts.image.controls.rotateRight();
        }
        if(this.opts.item) {
            this.opts.item.notifyImageRotated(90);
        }
    }.bind(this)

    this.rotateLeft = function()
    {
        if ( this.opts.image ) {
            this.opts.image.controls.rotateLeft();
        }
        if(this.opts.item) {
            this.opts.item.notifyImageRotated(-90);
        }
    }.bind(this)
});
riot.tag2('imagefilters', '<div class="imagefilters__filter-list"><div class="imagefilters__filter" each="{filter in filters}"><span class="imagefilters__label {filter.config.slider ? \'\' : \'imagefilters__label-long\'}">{filter.config.label}</span><input disabled="{filter.disabled ? \'disabled=\' : \'\'}" class="imagefilters__checkbox" if="{filter.config.checkbox}" type="checkbox" onchange="{apply}" checked="{filter.isActive() ? \'checked\' : \'\'}"><input disabled="{filter.disabled ? \'disabled=\' : \'\'}" class="imagefilters__slider" title="{filter.getValue()}" if="{filter.config.slider}" type="range" oninput="{apply}" riot-value="{filter.getValue()}" min="{filter.config.min}" max="{filter.config.max}" step="{filter.config.step}" orient="horizontal"></div></div><div class="imagefilters__options"><button type="button" class="btn btn--full" onclick="{resetAll}">{this.config.messages.clearAll}</button></div>', '', '', function(opts) {

		if(!this.opts.image) {
		    throw "ImageView object must be defined for imageFilters";
		}

		var defaultConfig = {
			filters: {
		        brightness : {
				    label: "Brightness",
				    type: ImageView.Tools.Filter.Brightness,
				    min: -255,
				    max: 255,
				    step: 1,
				    base: 0,
				    slider: true,
				    checkbox: false,
				    visible: true,
				},
		        contrast : {
				    label: "Contrast",
				    type: ImageView.Tools.Filter.Contrast,
				    min: 0,
				    max: 2,
				    step: 0.05,
				    base: 1,
				    slider: true,
				    checkbox: false,
				    visible: true
				},
		        saturate : {
				    label: "Color Saturation",
				    type: ImageView.Tools.Filter.ColorSaturation,
				    min: 0,
				    max: 5,
				    step: 0.1,
				    base: 1,
				    slider: true,
				    checkbox: false,
				    visible: true
				},
				hue : {
				    label: "Color rotation",
				    type: ImageView.Tools.Filter.ColorRotate,
				    min: -180,
				    max: 180,
				    step: 1,
				    base: 0,
				    slider: true,
				    checkbox: false,
				    visible: true
				},
				threshold : {
				    label: "Bitonal",
				    type: ImageView.Tools.Filter.Threshold,
				    min: 0,
				    max: 255,
				    step: 1,
				    base: 128,
				    slider: true,
				    checkbox: true,
				    visible: true,
				    preclude: ["grayscale", "sharpen"]
				},
		        grayscale : {
				    label: "Grayscale",
				    type: ImageView.Tools.Filter.Grayscale,
				    slider: false,
				    checkbox: true,
				    visible: true,
				    preclude: ["threshold"]
				},
				invert : {
				    label: "Invert",
				    type: ImageView.Tools.Filter.Invert,
				    slider: false,
				    checkbox: true,
				    visible: true
				},
		        blur : {
				    label: "Blur",
				    type: ImageView.Tools.Filter.Blur,
				    min: 1,
				    max: 10,
				    step: 1,
				    base: 1,
				    slider: true,
				    checkbox: false,
				    visible: true
				},
		        sharpen : {
				    label: "Sharpen",
				    type: ImageView.Tools.Filter.Sharpen,
				    base: 1,
				    slider: false,
				    checkbox: true,
				    visible: true
				},
			},
			messages : {
			    clearAll: "Clear all",
			    apply: "Apply"
			}
		}
		this.config = $.extend(true, {}, defaultConfig, this.opts.config);

		this.on("mount", function() {
		    this.filters = this.initFilters(this.config, this.opts.image);
			this.update();
		});

		this.initFilters = function(filterConfig, image) {
		    let filters = [];
		    for(var key in filterConfig.filters) {
		        let conf = filterConfig.filters[key];
		        if(conf.visible) {
		            let filter = new conf.type(image, conf.base);
		            filter.config = conf;
		            filter.name = key;
		            filters.push(filter);
		        }
		    }
		    return filters;
		}.bind(this)

		this.apply = function(event) {
		    let filter = event.item.filter;
		    let value = event.target.value;
		    if(filter) {
			    if(!filter.isActive()) {
			        filter.start();
			        this.disable(filter.config.preclude);
			    } else if(isNaN(value) ) {
			        filter.close();
			        this.enable(filter.config.preclude);
			    }
			    if(!isNaN(value) ) {
			    	filter.setValue(parseFloat(value));
			    	event.target.title = value;
			    }
		    }

		}.bind(this)

		this.disable = function(filterNames) {
		    if(filterNames) {
			    this.filters
			    .filter( filter => filterNames.includes(filter.name) )
			    .forEach( filter => {
			        filter.disabled = true;
			    })
			    this.update();
		    }
		}.bind(this)

		this.enable = function(filterNames) {
		    if(filterNames) {
			    this.filters
			    .filter( filter => filterNames.includes(filter.name) )
			    .forEach( filter => {
			   		filter.disabled = false;
			    })
			    this.update();
		    }
		}.bind(this)

		this.resetAll = function() {
		   this.filters.forEach( filter => {
		       filter.close();
		       filter.disabled = false;
		       if(filter.config.slider) {
		       	filter.setValue(filter.config.base);
		       }
		   })
		   this.update();
		}.bind(this)

});
/**
 * Takes a IIIF canvas object in opts.source. 
 * If opts.item exists, it creates the method opts.item.setImageSource(canvas) 
 * and provides an observable in opts.item.imageChanged triggered every time a new image source is loaded (including the first time)
 * The imageView itself is stored in opts.item.image
 */

riot.tag2('imageview', '<div id="wrapper_{opts.id}" class="imageview_wrapper"><span if="{this.error}" class="loader_wrapper"><span class="error_message">{this.error.message}</span></span><imagecontrols if="{this.image}" image="{this.image}" item="{this.opts.item}"></imageControls><div class="image_container"><div id="image_{opts.id}" class="image"></div></div></div>', '', '', function(opts) {

	this.getPosition = function() {
		let pos_os = this.dataPoint.getPosition();
		let pos_image = ImageView.CoordinateConversion.scaleToImage(pos_os, this.image.viewer, this.image.getOriginalImageSize());
		let pos_image_rot = ImageView.CoordinateConversion.convertPointFromImageToRotatedImage(pos_image, this.image.controls.getRotation(), this.image.getOriginalImageSize());
		return pos_image_rot;
	}.bind(this)

	this.on("mount", function() {
		$("#controls_" + opts.id + " .draw_overlay").on("click", function() {
			this.drawing=true;
		}.bind(this));
		try{
			imageViewConfig.image.tileSource = this.getImageInfo(opts.source);
			this.image = new ImageView.Image(imageViewConfig);
			this.image.load()
			.then( (image) => {
				if(this.opts.item) {
					this.opts.item.image = this.image;

				    var now = Rx.of(image);
					this.opts.item.setImageSource = function(source) {
					    this.image.setTileSource(this.getImageInfo(source));
					}.bind(this);
				    this.opts.item.notifyImageOpened(image.observables.viewerOpen.pipe(RxOp.map( () => image),RxOp.merge(now)));
				}
				return image;
			})
			.then(function() {
			  	this.update();
			}.bind(this));
		} catch(error) {
		    console.error("ERROR ", error);
	    	this.error = error;
	    	this.update();
		}
	})

	this.getImageInfo = function(canvas) {
	    return canvas.images[0].resource.service["@id"] + "/info.json"
	}.bind(this)

	const imageViewConfig = {
			global : {
				divId : "image_" + opts.id,
				fitToContainer: true,
				adaptContainerWidth: false,
				adaptContainerHeight: false,
				footerHeight: 00,
				zoomSpeed: 1.3,
				allowPanning : true,
			},
			image : {}
	};

	const drawStyle = {
			borderWidth: 2,
			borderColor: "#2FEAD5"
	}

	const lineStyle = {
			lineWidth : 1,
			lineColor : "#EEC83B"
	}

	const pointStyle = ImageView.DataPoint.getPointStyle(20, "#EEC83B");

});



riot.tag2('metadataeditor', '<div if="{this.metadataList}"><ul class="nav nav-tabs"><li each="{language, index in this.opts.languages}" class="{language == this.currentLanguage ? \'active\' : \'\'}"><a onclick="{this.setCurrentLanguage}">{language}</a></li></ul><div class="tab-content"><div class="tab-pane active"><div class="input_form"><div each="{metadata, index in this.metadataList}" class="input_form__option_group"><div class="input_form__option_label"><label for="input-{metadata.property}">{metadata.label}:</label></div><div class="input_form__option_marker {metadata.required ? \'in\' : \'\'}"><label>*</label></div><div class="input_form__option_control"><input tabindex="{index+1}" disabled="{this.isEditable(metadata) ? \'\' : \'disabled\'}" ref="input" if="{metadata.type != \'longtext\'}" type="{metadata.type}" id="input-{metadata.property}" class="form-control" riot-value="{getValue(metadata)}" oninput="{this.updateMetadata}"><textarea tabindex="{index+1}" disabled="{this.isEditable(metadata) ? \'\' : \'disabled\'}" ref="input" if="{metadata.type == \'longtext\'}" id="input-{metadata.property}" class="form-control" riot-value="{getValue(metadata)}" oninput="{this.updateMetadata}"></textarea></div><div if="{metadata.helptext}" class="input_form__option_help"><button type="button" class="btn btn--clean" data-toggle="helptext" for="help_{metadata.property}"><i class="fa fa-question-circle" aria-hidden="true"></i></button></div><div if="{metadata.helptext}" id="help_{metadata.property}" class="input_form__option_control_helptext">{metadata.helptext}</div></div><div class="input_form__actions"><a if="{this.opts.deleteListener}" disabled="{this.mayDelete() ? \'\' : \'disabled\'}" class="btn btn--clean delete" onclick="{this.notifyDelete}">{this.opts.deleteLabel}</a></div></div></div></div></div>', '', '', function(opts) {

 	this.on("mount", () => {
 	    console.log("mount metadataEditor ", this.opts);
 	    this.currentLanguage = this.opts.currentLanguage;
 	    this.updateMetadataList(this.opts.metadata);
 	    this.focusInput();
 	    if(this.opts.provider) {
 	        this.opts.provider.subscribe( (metadata) => {
 	            this.updateMetadataList(metadata)
 	            this.update();
 	            this.focusInput();
 	        });
 	    }
 	})

 	this.focusInput = function() {
 	    if(Array.isArray(this.refs.input)) {
 	        this.refs.input[0].focus();
 	    } else if(this.refs.input) {
 	        this.refs.input.focus();
 	    }
 	}.bind(this)

 	this.updateMetadataList = function(metadataList) {
 	   this.metadataList = metadataList;
 	}.bind(this)

 	this.updateMetadata = function(event) {
 	    let metadata = event.item.metadata;
 	    if(!metadata.value) {
 	        metadata.value = {};
 	    }
 	    let value = event.target.value;
 	    if(value) {
	 	    metadata.value[this.currentLanguage] = [event.target.value];
 	    } else {
 	       metadata.value[this.currentLanguage] = undefined;
 	    }
 	    if(this.opts.updateListener) {
 	       this.opts.updateListener.next(metadata);
 	    }
 	}.bind(this)

 	this.getValue = function(metadata) {
 	    if(metadata.value && metadata.value[this.currentLanguage]) {
	 	    let value = metadata.value[this.currentLanguage][0];
	 	    return value;
 	    } else {
 	        return "";
 	    }
 	}.bind(this)

 	this.setCurrentLanguage = function(event) {
 	    this.currentLanguage = event.item.language;
 	    this.update();
 	}.bind(this)

 	this.notifyDelete = function() {
 	    this.opts.deleteListener.next();
 	}.bind(this)

 	this.isEditable = function(metadata) {
 	    return metadata.editable === undefined || metadata.editable === true;
 	}.bind(this)

 	this.mayDelete = function() {
 	    editable = this.metadataList.find( md => this.isEditable(md));
 	    return editable !== undefined;
 	}.bind(this)

});



riot.tag2('pdfdocument', '<div class="pdf-container"><pdfpage each="{page, index in pages}" page="{page}" pageno="{index+1}"></pdfPage></div>', '', '', function(opts) {

		this.pages = [];

		var loadingTask = pdfjsLib.getDocument( this.opts.data );
	    loadingTask.promise.then( function( pdf ) {
	        var pageLoadingTasks = [];
	        for(var pageNo = 1; pageNo <= pdf.numPages; pageNo++) {
   		        var page = pdf.getPage(pageNo);
   		        pageLoadingTasks.push(Q(page));
   		    }
   		    return Q.allSettled(pageLoadingTasks);
	    }.bind(this))
	    .then(function(results) {
			results.forEach(function (result) {
			    if (result.state === "fulfilled") {
                	var page = result.value;
                	this.pages.push(page);
                } else {
                    logger.error("Error loading page: ", result.reason);
                }
			}.bind(this));
			this.update();
        }.bind(this))
	    .then( function() {
			$(".pdf-container").show();
            $( '#literatureLoader' ).hide();
		} );

});
riot.tag2('pdfpage', '<div class="page" id="page_{opts.pageno}"><canvas class="pdf-canvas" id="pdf-canvas_{opts.pageno}"></canvas><div class="text-layer" id="pdf-text_{opts.pageno}"></div><div class="annotation-layer" id="pdf-annotations_{opts.pageno}"></div></div>', '', '', function(opts) {
	this.on('mount', function () {

           this.container = document.getElementById( "page_" + this.opts.pageno );
           this.canvas = document.getElementById( "pdf-canvas_" + this.opts.pageno );
           this.textLayer = document.getElementById( "pdf-text_" + this.opts.pageno );
           this.annotationLayer = document.getElementById( "pdf-annotations_" + this.opts.pageno );

		var containerWidth = $(this.container).width();
		var pageWidth = this.opts.page._pageInfo.view[2];
           var scale = containerWidth/pageWidth;
		this.viewport = this.opts.page.getViewport( scale );

           if(this.container) {
               this.loadPage();
           }
	});

    this.loadPage = function() {
        var canvasOffset = $( this.canvas ).offset();
        var context = this.canvas.getContext( "2d" );
        this.canvas.height = this.viewport.height;
        this.canvas.width = this.viewport.width;

        this.opts.page.render( {
            canvasContext: context,
            viewport: this.viewport
        } ).then( function() {
            return this.opts.page.getTextContent();
        }.bind( this ) ).then( function( textContent ) {
            console.log( "viewport ", this.viewport );
            $( this.textLayer ).css( {
                height: this.viewport.height + 'px',
                width: this.viewport.width + 'px',
            } );

            pdfjsLib.renderTextLayer( {
                textContent: textContent,
                container: this.textLayer,
                viewport: this.viewport,
                textDivs: []
            } );

            return this.opts.page.getAnnotations();
        }.bind( this ) ).then( function( annotationData ) {

            $( this.annotationLayer ).css( {
                width: this.viewport.width + 'px',
            } );

            pdfjsLib.AnnotationLayer.render( {
                viewport: this.viewport.clone( {
                    dontFlip: true
                } ),
                div: this.annotationLayer,
                annotations: annotationData,
                page: this.opts.page,
                linkService: {
                    getDestinationHash: function( dest ) {
                        return '#';
                    },
                    getAnchorUrl: function( hash ) {
                        return '#';
                    },
                    isPageVisible: function() {
                        return true;
                    },
                    externalLinkTarget: pdfjsLib.LinkTarget.BLANK,
                }
            } );

        }.bind( this ) )
    }.bind(this)

});
	
	
riot.tag2('plaintextquestion', '<div if="{this.showInstructions()}" class="annotation_instruction"><label>{Crowdsourcing.translate(⁗crowdsourcing__help__create_rect_on_image⁗)}</label></div><div if="{this.showInactiveInstructions()}" class="annotation_instruction annotation_instruction_inactive"><label>{Crowdsourcing.translate(⁗crowdsourcing__help__make_active⁗)}</label></div><div class="annotation_wrapper" id="question_{opts.index}_annotation_{index}" each="{anno, index in this.question.annotations}"><div class="annotation_area"><div if="{this.showAnnotationImages()}" class="annotation_area__image" riot-style="border-color: {anno.getColor()}"><img riot-src="{this.question.getImage(anno)}"></img></div><div class="annotation_area__text_input"><textarea disabled="{this.opts.item.isReviewMode() ? \'disabled\' : \'\'}" onchange="{setTextFromEvent}" riot-value="{anno.getText()}"></textarea></div></div><div class="cms-module__actions"><button if="{!this.opts.item.isReviewMode()}" onclick="{deleteAnnotationFromEvent}" class="annotation_area__button btn btn--clean delete">{Crowdsourcing.translate(⁗action__delete_annotation⁗)} </button></div></div><button if="{showAddAnnotationButton()}" onclick="{addAnnotation}" class="options-wrapper__option btn btn--default" id="add-annotation">{Crowdsourcing.translate(⁗action__add_annotation⁗)}</button>', '', '', function(opts) {

	this.question = this.opts.question;

	this.on("mount", function() {
	    this.question.initializeView((anno) => new Crowdsourcing.Annotation.Plaintext(anno), this.update, this.update, this.focusAnnotation);
	    this.opts.item.onImageOpen(function() {
	        switch(this.question.targetSelector) {
	            case Crowdsourcing.Question.Selector.WHOLE_PAGE:
	            case Crowdsourcing.Question.Selector.WHOLE_SOURCE:
	                if(this.question.annotations.length == 0 && !this.question.item.isReviewMode()) {
	                    this.question.addAnnotation();
	                }
	        }
	        this.update()
	    }.bind(this));
	});

	this.focusAnnotation = function(index) {
	    let id = "question_" + this.opts.index + "_annotation_" + index;
	    let inputSelector = "#" + id + " textarea";
	    this.root.querySelector(inputSelector).focus();
	}.bind(this)

	this.showAnnotationImages = function() {
	    return this.question.isRegionTarget();
	}.bind(this)

	this.showInstructions = function() {
	    return !this.opts.item.isReviewMode() &&  this.question.active && this.question.targetSelector == Crowdsourcing.Question.Selector.RECTANGLE && this.question.annotations.length == 0;
	}.bind(this)

	this.showInactiveInstructions = function() {
	    return !this.opts.item.isReviewMode() &&  !this.question.active && this.question.targetSelector == Crowdsourcing.Question.Selector.RECTANGLE && this.opts.item.questions.filter(q => q.isRegionTarget()).length > 1;

	}.bind(this)

	this.showAddAnnotationButton = function() {
	    return !this.question.isReviewMode() && !this.question.isRegionTarget() && this.question.mayAddAnnotation();
	}.bind(this)

    this.setTextFromEvent = function(event) {
        event.preventUpdate = true;
        if(event.item.anno) {
            event.item.anno.setText(event.target.value);
            this.question.saveToLocalStorage();
        } else {
            throw "No annotation to set"
        }
    }.bind(this)

    this.deleteAnnotationFromEvent = function(event) {
        if(event.item.anno) {
            this.question.deleteAnnotation(event.item.anno);
            this.update();
        }
    }.bind(this)

    this.addAnnotation = function() {
        this.question.addAnnotation();
        this.question.focusCurrentAnnotation();
    }.bind(this)

});
riot.tag2('popup', '<yield></yield>', '', '', function(opts) {

this.on( 'mount', function() {
	this.addCloseHandler();
	$(this.root).offset(this.opts.offset);
    $("body").append($(this.root));
    $(this.root).css("position", "absolute");
    $(this.root).show();
});

this.addCloseHandler = function() {
    $(this.root).on("click", function(event){
        event.stopPropagation();
    });

    $('body').one("click", function(event) {
        this.unmount(true);
        $(this.root).off();
        if(this.opts.myparent) {
             $(this.root).hide();
            $(this.opts.myparent).append($(this.root));
            $(this.root).offset({left:0, top:0});
        } else {
            this.root.remove();
        }
    }.bind(this));

}.bind(this)

});


riot.tag2('progressbar', '<div class="goobi-progress-bar-wrapper"><div class="goobi-progress-bar"><div each="{value, index in this.values}" class="goobi-progress-bar__bar {styleClasses[index]}" riot-style="width: {getRelativeWidth(value)};"></div></div></div>', '', '', function(opts) {
	this.values = JSON.parse(this.opts.values);
	this.styleClasses = JSON.parse(this.opts.styleclasses);

	this.on("mount", function() {
	    let bar = this.root.querySelector(".goobi-progress-bar");
	    this.totalBarWidth = bar.getBoundingClientRect().width;
		this.update();
	})

	this.getRelativeWidth = function(value) {
		    let barWidth = value/this.opts.total*this.totalBarWidth;
		    return barWidth + "px";
	}.bind(this)

	this.loaded = function() {
	    console.log("on load");
	}.bind(this)

});
riot.tag2('questiontemplate', '<div if="{showInstructions()}" class="annotation_instruction"><label>{Crowdsourcing.translate(⁗crowdsourcing__help__create_rect_on_image⁗)}</label></div><div if="{showInactiveInstructions()}" class="annotation_instruction annotation_instruction_inactive"><label>{Crowdsourcing.translate(⁗crowdsourcing__help__make_active⁗)}</label></div><div class="annotation_wrapper" id="question_{opts.index}_annotation_{index}" each="{anno, index in this.question.annotations}"><div class="annotation_area"></div></div><button if="{showAddAnnotationButton()}" onclick="{addAnnotation}" class="options-wrapper__option btn btn--default" id="add-annotation">{Crowdsourcing.translate(⁗action__add_annotation⁗)}</button>', '', '', function(opts) {

this.question = this.opts.question;

this.on("mount", function() {
    this.question.initializeView((anno) => new Crowdsourcing.Annotation.Implementation(anno), this.update, this.update, this.focusAnnotation);
    this.opts.item.onImageOpen(function() {
        this.update()
    }.bind(this));
});

this.focusAnnotation = function(index) {
    let id = "question_" + this.opts.index + "_annotation_" + index;
    let inputSelector = "#" + id + " textarea";
    this.root.querySelector(inputSelector).focus();
}.bind(this)

this.showInstructions = function() {
    return !this.opts.item.isReviewMode() &&  this.question.active && this.question.targetSelector == Crowdsourcing.Question.Selector.RECTANGLE && this.question.annotations.length == 0;
}.bind(this)

this.showInactiveInstructions = function() {
    return !this.opts.item.isReviewMode() &&  !this.question.active && this.question.targetSelector == Crowdsourcing.Question.Selector.RECTANGLE && this.opts.item.questions.filter(q => q.isRegionTarget()).length > 1;

}.bind(this)

this.showAddAnnotationButton = function() {
    return !this.question.isReviewMode() && !this.question.isRegionTarget() && this.question.mayAddAnnotation();
}.bind(this)

this.setBodyFromEvent = function(event) {
    event.preventUpdate = true;
    if(event.item.anno) {

        this.question.saveToLocalStorage();
    } else {
        throw "No annotation to set"
    }
}.bind(this)

this.deleteAnnotationFromEvent = function(event) {
    if(event.item.anno) {
        this.question.deleteAnnotation(event.item.anno);
        this.update();
    }
}.bind(this)

this.addAnnotation = function() {
    this.question.addAnnotation();
    this.question.focusCurrentAnnotation();
}.bind(this)

});


riot.tag2('timematrix', '<div class="timematrix__objects"><div each="{image in imageList}" class="timematrix__content"><div id="imageMap" class="timematrix__img"><a href="{image.url}"><img riot-src="{image.mediumimage}" class="timematrix__image" data-viewer-thumbnail="thumbnail" onerror="this.onerror=null;this.src=\'/viewer/resources/images/access_denied.png\'"><div class="timematrix__text"><p if="{image.title}" name="timetext" class="timetext">{image.title[0]}</p></div></a></div></div></div>', '', '', function(opts) {

		 this.on( 'mount', function() {
		 	$(this.opts.button).on("click", this.updateRange);
		 	this.imageList=[];
		 	this.startDate = parseInt($(this.opts.startInput).val());
		 	this.endDate = parseInt($(this.opts.endInput).val());
		 	this.initSlider(this.opts.slider, this.startDate, this.endDate);
		 });

		 this.updateRange = function(event){
			this.getTimematrix()
		}.bind(this)
		 this.getTimematrix = function(){

		     var apiTarget = this.opts.contextPath;
		     apiTarget += 'rest/records/timematrix/range/';
		     apiTarget += $(this.opts.startInput).val();
		     apiTarget += "/";
		     apiTarget += $(this.opts.endInput).val();
		     apiTarget += '/';
		     apiTarget += $(this.opts.count).val();
		     apiTarget += '/';

		    opts.loading.show()
			let fetchPromise = fetch(apiTarget);
		    fetchPromise.then( function(result) {
			    return result.json();
			})
			.then( function(json) {
			    this.imageList=json;
			    this.update()
			    opts.loading.hide()
			}.bind(this));
		 }.bind(this)

		 this.initSlider = function(sliderSelector, startDate, endDate) {
		     let $slider = $(sliderSelector);

	            $slider.slider( {
	                range: true,
	                min: parseInt( startDate ),
	                max: parseInt( endDate ),
	                values: [ startDate, endDate ],
	                slide: function( event, ui ) {
	                    $(this.opts.startInput).val( ui.values[ 0 ] ).change();
	                    this.startDate = parseInt(ui.values[ 0 ]);
	                    $(this.opts.endInput).val( ui.values[ 1 ] ).change();
	                    this.endDate = parseInt(ui.values[ 1 ]);
	                }.bind(this)
	            } );

	            $slider.find(".ui-slider-handle").on( 'mousedown', function() {
	                $( '.ui-slider-handle' ).removeClass( 'top' );
	                $( this ).addClass( 'top' );
	            } );
		 }.bind(this)

});
