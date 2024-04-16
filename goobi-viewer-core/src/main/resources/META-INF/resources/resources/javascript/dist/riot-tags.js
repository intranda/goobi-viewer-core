riot.tag2('adminmediaupload', '<div class="admin-cms-media__upload-wrapper"><div class="admin-cms-media__upload" ref="dropZone"><div class="admin-cms-media__upload-input"><p> {opts.msg.uploadText} <br><small>({opts.msg.allowedFileTypes}: {fileTypes})</small></p><label for="file" class="btn btn--default">{opts.msg.buttonUpload}</label><input id="file" class="admin-cms-media__upload-file" type="file" multiple="multiple" onchange="{buttonFilesSelected}"></div><div class="admin-cms-media__upload-messages"><div class="admin-cms-media__upload-message uploading"><i class="fa fa-spinner fa-pulse fa-fw"></i> {opts.msg.mediaUploading} </div><div class="admin-cms-media__upload-message success"><i class="fa fa-check-square-o" aria-hidden="true"></i> {opts.msg.mediaFinished} </div><div class="admin-cms-media__upload-message error"><i class="fa fa-exclamation-circle" aria-hidden="true"></i><span></span></div></div></div><div if="{this.opts.showFiles}" class="admin-cms-media__list-files {this.uploadedFiles.length > 0 ? \'in\' : \'\'}" ref="filesZone"><div each="{file in this.uploadedFiles}" class="admin-cms-media__list-files__file"><img riot-src="{file}" alt="{getFilename(file)}" title="{getFilename(file)}"><div class="delete_overlay" onclick="{deleteFile}"><i class="fa fa-trash" aria-hidden="true"></i></div></div></div></div>', '', '', function(opts) {
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

        this.initDrop = function() {
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
        }.bind(this)

        this.initUploadedFiles = function() {
			this.getUploadedFiles();

            var filesZone = (this.refs.filesZone);
        }.bind(this)

        this.buttonFilesSelected = function(e) {
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
        }.bind(this)

        this.uploadFiles = function() {
            var uploads = [];

            $('.admin-cms-media__upload-messages, .admin-cms-media__upload-message.success, .admin-cms-media__upload-message.error').removeClass('in-progress');
            $('.admin-cms-media__upload-messages, .admin-cms-media__upload-message.uploading').addClass('in-progress');

            for (i = 0; i < this.files.length; i++) {
            	if(this.opts.fileTypeValidator) {
            		let regex = this.opts.fileTypeValidator;
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
        }.bind(this)

        this.fileUploaded = function(fileInfo) {
            $('.admin-cms-media__upload-messages, .admin-cms-media__upload-message.uploading').removeClass('in-progress');
            $('.admin-cms-media__upload-messages, .admin-cms-media__upload-message.success').addClass('in-progress');

            setTimeout( function() {
                $('.admin-cms-media__upload-messages, .admin-cms-media__upload-message.uploading, .admin-cms-media__upload-message.success').removeClass('in-progress');
        	}, 5000 );
        }.bind(this)

        this.fileUploadError = function(responseText) {
        	console.log("fileUploadError", responseText);
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
            this.deleteUploadedFile(data.item.file)
            .then( () => {
                this.getUploadedFiles();
            })
        }.bind(this)

        this.uploadFile = function(i) {
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
        }.bind(this)

        this.getFilename = function(url) {
            let filename = url.replace(this.opts.postUrl, "");
            if(filename.startsWith("/")) {
                filename = filename.slice(1);
            }
            let filenameEnd = filename.indexOf("/");
            if(filenameEnd > 0) {
                filename = filename.slice(0,filenameEnd);
            }
            return filename;
        }.bind(this)

        this.setDragover = function(dragover) {
        	this.isDragover = dragover;
        	var dropZone = (this.refs.dropZone);
        	if(dropZone) {
        		if(dragover) {
        			dropZone.classList.add("isdragover");
        		} else {
        			dropZone.classList.remove("isdragover");
        		}
        	}

        }.bind(this)
});


riot.tag2('annotationbody', '<plaintextresource if="{isPlaintext()}" resource="{this.annotationBody}" annotationid="{this.opts.annotationid}"></plaintextResource><htmltextresource if="{isHtml()}" resource="{this.annotationBody}" annotationid="{this.opts.annotationid}"></htmltextResource><geomapresource if="{isGeoJson()}" resource="{this.annotationBody}" annotationid="{this.opts.annotationid}" mapboxtoken="{this.opts.mapboxtoken}" initialview="{this.opts.geomap.initialView}"></geoMapResource><authorityresource if="{isAuthorityResource()}" resource="{this.annotationBody}" annotationid="{this.opts.annotationid}" currentlang="{this.opts.currentlang}" resturl="{this.opts.resturl}"></authorityResource><datasetresource if="{isDatasetResource()}" resource="{this.annotationBody}" annotationid="{this.opts.annotationid}" currentlang="{this.opts.currentlang}" resturl="{this.opts.resturl}"></datasetResource>', '', '', function(opts) {

this.on("mount", () => {
    if(this.opts.contentid) {
        let content = document.getElementById(this.opts.contentid).innerText;
        try {
	        this.annotationBody = JSON.parse(content);
	        this.type = this.annotationBody.type;
	        if(!this.type) {
	            this.type = this.anotationBody["@type"];
	        }
	        this.format = this.annotationBody.format;
    	} catch(e) {
    	    this.annotationBody = {value: content};
    	    this.type = "TextualResource";
    	    this.format = "text/plain";
   		}
        this.update();
    }
})

this.isPlaintext = function() {
    if(this.type == "TextualBody" || this.type == "TextualResource") {
        return !this.format || this.format == "text/plain";
    }
    return false;
}.bind(this)

this.isHtml = function() {
    if(this.type == "TextualBody" || this.type == "TextualResource") {
        return this.format == "text/html";
    }
    return false;
}.bind(this)

this.isGeoJson = function() {
    return this.type == "Feature";
}.bind(this)

this.isAuthorityResource = function() {
    return this.type == "AuthorityResource";
}.bind(this)

this.isDatasetResource = function() {
    return this.type == "Dataset";
}.bind(this)

});


riot.tag2('authorityresource', '<div class="annotation__body__authority"><div if="{normdataList.length == 0}">{authorityId}</div><dl class="annotation__body__authority__normdata_list" each="{normdata in normdataList}"><dt class="normdata_list__label">{normdata.property}: </dt><dd class="normdata_list__value">{normdata.value}</dd></dl></div>', '', '', function(opts) {
    this.normdataList = [];

	this.on("mount", () => {
		this.authorityId = this.opts.resource.id;
	    this.url = this.opts.resturl + "authority/resolver?id=" + this.unicodeEscapeUri(this.authorityId) + "&template=ANNOTATION&lang=" + this.opts.currentlang
		this.update();
	    fetch(this.url)
	    .then(response => {
	        if(!response.ok) {
	            throw "Error: " + response.status;
	        } else {
	            return response;
	        }
	    })
	    .then(response => response.json())
	    .then(response => {
	        this.normdataList = this.parseResponse(response);
	    })
	    .catch(error => {
	        console.error("failed to load ", this.url, ": " + error);
	    })
	    .then(() => this.update());
	})

	this.unicodeEscapeUri = function(uri) {
    	return uri.replace(/\//g, 'U002F').replace('/\\/g','U005C').replace('/?/g','U003F').replace('/%/g','U0025');
	}.bind(this)

	this.parseResponse = function(jsonResponse) {
	    let normdataList = [];
	    $.each( jsonResponse, (i, object ) => {
            $.each( object, ( property, value ) => {
                let stringValue = value.map(v => v.text).join("; ");
                normdataList.push({property: property, value:stringValue});
            });
	    });
	    return normdataList;
	}.bind(this)

});
riot.tag2('datasetresource', '<div class="annotation__body__dataset"><dl class="annotation__body__dataset__data_list" each="{field in dataFields}"><dt class="data_list__label">{getName(field)}: </dt><dd class="data_list__value">{getValue(field)}</dd></dl></div>', '', '', function(opts) {
    this.dataSet = {};
    this.dataFields = [];

	this.on("mount", () => {
		this.dataSet = this.opts.resource.data;
		this.dataFields = Object.keys(this.dataSet);
		if(viewerJS.translator) {
		    viewerJS.translator.addTranslations(this.dataFields)
			.then(() => this.update());
		} else {
			viewerJS.initialized.subscribe(() => {
		        viewerJS.translator.addTranslations(this.dataFields)
				.then(() => this.update());
			});
		}
	})

	this.getValue = function(field) {
	    let value = this.dataSet[field];
	    if(!value) {
	        return "";
	    } else if(Array.isArray(value)) {
	        return value.join("; ")
	    } else {
	        return value;
	    }
	}.bind(this)

	this.getName = function(field) {
	    return viewerJS.translator.translate(field);
	}.bind(this)

});

riot.tag2('geomapresource', '<div id="geomap_{opts.annotationid}" class="annotation__body__geomap geomap"></div>', '', '', function(opts) {

this.on("mount", () => {
	this.feature = this.opts.resource;
	this.config = {
	        popover: undefined,
	        mapId: "geomap_" + this.opts.annotationid,
	        fixed: true,
	        clusterMarkers: false,
	        initialView : this.opts.initialview,
	    };
    this.geoMap = new viewerJS.GeoMap(this.config);
    let view = this.feature.view;
    let features = [this.feature];
    this.geoMap.init(view, features);

});

});
riot.tag2('htmltextresource', '<div ref="container" class="annotation__body__htmltext"></div>', '', '', function(opts) {

	this.on("mount", () => {
	    this.refs.container.innerHTML = this.opts.resource.value;
	})

});
riot.tag2('plaintextresource', '<div class="annotation__body__plaintext">{this.opts.resource.value}</div>', '', '', function(opts) {
});
riot.tag2('bookmarklist', '<ul class="{mainClass} list"><li each="{bookmarkList in getBookmarkLists()}"><button if="{pi}" class="btn btn--clean" type="button" onclick="{inList(bookmarkList, this.pi, this.page, this.logid) ? remove : add}"><i if="{inList(bookmarkList, this.pi, this.page, this.logid)}" class="fa fa-check" aria-hidden="true"></i> {bookmarkList.name} <span>{bookmarkList.numItems}</span></button><div if="{!pi}" class="row no-margin"><div class="col-9 no-padding"><a href="{opts.bookmarks.getBookmarkListUrl(bookmarkList.id)}">{bookmarkList.name}</a></div><div class="col-2 no-padding icon-list"><a if="{maySendList(bookmarkList)}" href="{sendListUrl(bookmarkList)}" title="{msg(\'bookmarkList_session_mail_sendList\')}"><i class="fa fa-paper-plane-o" aria-hidden="true"></i></a><a href="{searchListUrl(bookmarkList)}" data-toggle="tooltip" data-placement="top" data-original-title="" title="{msg(\'action__search_in_bookmarks\')}"><i class="fa fa-search" aria-hidden="true"></i></a><a href="{miradorUrl(bookmarkList)}" target="_blank" title="{msg(\'viewMiradorComparison\')}"><i class="fa fa-th" aria-hidden="true"></i></a></div><div class="col-1 no-padding"><span class="{mainClass}-counter">{bookmarkList.numItems}</span></div></div></li></ul>', '', '', function(opts) {


this.pi = this.opts.data.pi;
this.logid = this.opts.data.logid;
this.page = this.opts.data.page;
this.loader = this.opts.loader;
this.button = this.opts.button;
this.mainClass = (this.opts.style && this.opts.style.mainClass) ? this.opts.style.mainClass : "bookmark-popup__body-list";

this.on( 'mount', function() {
    this.opts.bookmarks.listsUpdated.pipe(rxjs.operators.merge(rxjs.of(""))).subscribe( () => this.onListUpdate());
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
    let list = event.item.bookmarkList;
    let pi = this.pi;
    let logid = undefined;
    let page = this.opts.bookmarks.isTypePage() ? this.page : undefined;
    this.opts.bookmarks.addToBookmarkList(list.id, pi, page, logid)
    .then( () => this.updateLists());
}.bind(this)

this.remove = function(event) {
    if(this.opts.bookmarks.config.userLoggedIn) {
	    let list = event.item.bookmarkList;
	    let pi = this.pi;
	    let logid = undefined;
	    let page = this.opts.bookmarks.isTypePage() ? this.page : undefined;
	    this.opts.bookmarks.removeFromBookmarkList(list.id, pi, page, logid)
	    .then( () => this.updateLists())
    } else {
        let bookmark = event.item.bookmark;
        this.opts.bookmarks.removeFromBookmarkList(0, bookmark.pi, undefined, undefined)
	    .then( () => this.updateLists())
    }
}.bind(this)

this.inList = function(list, pi, page, logid) {
    return this.opts.bookmarks.inList(list, pi, page, logid);
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


riot.tag2('bookmarklistloggedin', '<ul if="{opts.bookmarks.config.userLoggedIn}" class="{mainClass}-small-list list"><li class="{mainClass}-entry" each="{bookmarkList in getBookmarkLists()}"><div class="login-navigation__bookmarks-name"><a href="{opts.bookmarks.getBookmarkListUrl(bookmarkList.id)}">{bookmarkList.name}</a></div><div class="login-navigation__bookmarks-icon-list icon-list"><a href="{searchListUrl(bookmarkList)}" data-toggle="tooltip" data-placement="top" data-original-title="" title="{msg(\'action__search_in_bookmarks\')}"><i class="fa fa-search" aria-hidden="true"></i></a><a href="{miradorUrl(bookmarkList)}" target="_blank" title="{msg(\'viewMiradorComparison\')}"><i class="fa fa-th" aria-hidden="true"></i></a><span title="{msg(\'admin__crowdsourcing_campaign_statistics_numRecords\')}" class="{mainClass}-counter">{bookmarkList.numItems}</span></div></li><li class="{mainClass}-entry"><a class="login-navigation__bookmarks-overview-link" href="{allBookmarksUrl()}" data-toggle="tooltip" data-placement="top" data-original-title="" title="{msg(\'bookmarkList_overview_all\')}">{msg(\'bookmarkList_overview_all\')} </a></li></ul>', '', '', function(opts) {


this.pi = this.opts.data.pi;
this.logid = this.opts.data.logid;
this.page = this.opts.data.page;
this.loader = this.opts.loader;
this.button = this.opts.button;
this.mainClass = (this.opts.style && this.opts.style.mainClass) ? this.opts.style.mainClass : "bookmark-popup__body-list";

this.on( 'mount', function() {
    this.opts.bookmarks.listsUpdated.subscribe( () => this.onListUpdate());
});

this.getBookmarkLists = function() {
    let lists =  this.opts.bookmarks.getBookmarkLists().slice(0,5);
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
	    let list = event.item.bookmarkList
	    this.opts.bookmarks.removeFromBookmarkList(list.id, this.pi, this.page, this.logid, this.opts.bookmarks.isTypePage())
	    .then( () => this.updateLists())
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

this.allBookmarksUrl = function() {
    	return this.opts.bookmarks.config.root + "/user/bookmarks/";
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


riot.tag2('bookmarklistsession', '<ul each="{bookmarkList in getBookmarkLists()}" class="{mainClass} list"><li each="{bookmark in bookmarkList.items}"><div class="row no-margin {mainClass}-single-entry"><div class="col-11 no-padding {mainClass}-title"><a href="{opts.bookmarks.config.root}{bookmark.url}"><div class="row no-gutters"><div class="col-4 no-padding"><div class="{mainClass}-image" riot-style="background-image: url({bookmark.representativeImageUrl});"></div></div><div class="col-7 no-padding"><h3>{bookmark.name}</h3></div></div></a></div><div class="col-1 no-padding {mainClass}-remove"><button class="btn btn--clean" type="button" data-bookmark-list-type="delete" onclick="{remove}" aria-label="{msg(\'bookmarkList_removeFromBookmarkList\')}"><i class="fa fa-ban" aria-hidden="true"></i></button></div></div></li></ul><div each="{bookmarkList in getBookmarkLists()}" class="{mainClass}-actions"><div if="{mayEmptyList(bookmarkList)}" class="{mainClass}-reset"><button class="btn btn--clean" type="button" data-bookmark-list-type="reset" onclick="{deleteList}"><span>{msg(\'bookmarkList_reset\')}</span><i class="fa fa-trash-o" aria-hidden="true"></i></button></div><div if="{maySendList(bookmarkList)}" class="{mainClass}-send"><a href="{sendListUrl(bookmarkList)}"><span>{msg(\'bookmarkList_session_mail_sendList\')}</span><i class="fa fa-paper-plane-o" aria-hidden="true"></i></a></div><div if="{maySearchList(bookmarkList)}" class="{mainClass}-search"><a href="{searchListUrl(bookmarkList)}" data-toggle="tooltip" data-placement="top" data-original-title="" title=""><span>{msg(\'action__search_in_bookmarks\')}</span><i class="fa fa-search" aria-hidden="true"></i></a></div><div if="{mayCompareList(bookmarkList)}" class="{mainClass}-mirador"><a href="{miradorUrl(bookmarkList)}" target="_blank"><span>{msg(\'viewMiradorComparison\')}</span><i class="fa fa-th" aria-hidden="true"></i></a></div></div>', '', '', function(opts) {


this.pi = this.opts.data.pi;
this.logid = this.opts.data.logid;
this.page = this.opts.data.page;
this.loader = this.opts.loader;
this.button = this.opts.button;
this.mainClass = (this.opts.style && this.opts.style.mainClass) ? this.opts.style.mainClass : "bookmark-popup__body-list";

this.on( 'mount', function() {
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

this.mayEmptyList = function(list) {
    return list.items.length > 0;
}.bind(this)

this.remove = function(event) {
        let bookmark = event.item.bookmark;
        this.opts.bookmarks.removeFromBookmarkList(0, bookmark.pi, undefined, undefined, false)
	    .then( () => this.updateLists())
}.bind(this)

this.deleteList = function(event) {
    this.opts.bookmarks.removeBookmarkList(0)
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
    	return this.opts.bookmarks.config.root + "/mirador/id/0/";
}.bind(this)

this.msg = function(key) {
    return this.opts.bookmarks.translator.translate(key);
}.bind(this)

});


riot.tag2('bookmarkspopup', '<div class="bookmark-popup__body-loader"></div><div if="{opts.data.page !== undefined}" class="bookmark-popup__radio-buttons"><div><label><input type="radio" checked="{opts.bookmarks.isTypeRecord()}" name="bookmarkType" riot-value="{msg(\'bookmarkList_typeRecord\')}" onclick="{setBookmarkTypeRecord}">{msg(\'bookmarkList_typeRecord\')}</label></div><div><label><input type="radio" checked="{opts.bookmarks.isTypePage()}" name="bookmarkType" riot-value="{msg(\'bookmarkList_typePage\')}" onclick="{setBookmarkTypePage}">{msg(\'bookmarkList_typePage\')}</label></div></div><div class="bookmark-popup__header"> {msg(\'bookmarkList_selectBookmarkList\')} </div><div class="bookmark-popup__body"><bookmarklist data="{this.opts.data}" loader="{this.opts.loader}" button="{this.opts.button}" bookmarks="{this.opts.bookmarks}"></bookmarkList></div><div class="bookmark-popup__footer"><div class="row no-margin"><div class="col-11 no-padding"><input ref="inputValue" type="text" placeholder="{msg(\'bookmarkList_addNewBookmarkList\')}" aria-label="{msg(\'bookmarkList_addNewBookmarkList\')}"></div><div class="col-1 no-padding"><button class="btn btn-clean" type="button" aria-label="{msg(\'bookmarkList_addNewBookmarkList\')}" onclick="{add}"><svg xmlns="http://www.w3.org/2000/svg" viewbox="0 0 41.57 41.57"><g id="icon-bs-add" transform="translate(-27.5 -243.5)"><line id="Linie_12" data-name="Linie 12" class="cls-1" x2="41.57" transform="translate(27.5 264.285)"></line><line id="Linie_13" data-name="Linie 13" class="cls-1" x2="41.57" transform="translate(48.285 243.5) rotate(90)"></line></g></svg></button></div></div></div>', '', 'class="bookmark-popup bottom" role="region" aria-label="{msg(\'bookmarks\')}"', function(opts) {

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
riot.tag2('chronologygraph', '<div class="widget-chronology-slider__item chronology-slider" if="{this.yearList.length > 0}"><div class="chronology-slider__container" ref="container"><canvas class="chronology-slider__chart" ref="chart"></canvas><canvas class="chronology-slider__draw" ref="draw"></canvas></div><div class="chronology-slider__input-wrapper"><input onchange="{setStartYear}" data-input="number" aria-label="Start" class="form-control chronology-slider__input-start" ref="input_start" riot-value="{startYear}"></input><div class="chronology-slider__between-year-symbol">-</div><input onchange="{setEndYear}" data-input="number" aria-label="End" class="form-control chronology-slider__input-end" ref="input_end" riot-value="{endYear}"></input><button ref="button_search" class="btn btn--full chronology-slider__ok-button" data-trigger="triggerFacettingGraph" onclick="{setRange}">{msg.ok}</button></div></div><div hidden ref="line" class="chronology-slider__graph-line"></div><div hidden ref="area" class="chronology-slider__graph-area"></div><div hidden ref="range" class="chronology-slider__graph-range"></div>', '', '', function(opts) {


		this.yearList = [1];
		this.msg = {};
		this.on( 'mount', function() {

			this.lineColor = window.getComputedStyle(this.refs?.line)?.color;
			this.areaColor = window.getComputedStyle(this.refs?.area)?.color;
			this.rangeBorderColor = window.getComputedStyle(this.refs?.range)?.color;
			this.rangeFillColor = window.getComputedStyle(this.refs?.range)?.backgroundColor;
			this.rangeOpacity = window.getComputedStyle(this.refs?.range)?.opacity;

			let completeYearMap = this.generateCompleteYearMap(this.opts.datamap);

			let chartElement = this.refs.chart;
			this.yearList = Array.from(completeYearMap.keys()).map(y => parseInt(y));
			this.yearValues = Array.from(completeYearMap.values());
			this.startYear = parseInt(opts.startYear);
			this.endYear = parseInt(opts.endYear);
			this.minYear = this.yearList[0];
			this.maxYear = this.yearList[this.yearList.length - 1];
			this.valueInput = document.getElementById(opts.valueInput);
			this.updateFacet = document.getElementById(opts.updateFacet);
			this.loader = document.getElementById(opts.loader);
			this.msg = opts.msg;
			this.rtl = $( this.refs.slider ).closest('[dir="rtl"]').length > 0;
			this.reloadingPage = false;

			this.chartConfig = {
					type: "line",
					data: {
						labels: this.yearList,
						datasets: [
							{
								data: this.yearValues,
								borderWidth: 1,
								borderColor: this.lineColor,
								backgroundColor: this.areaColor,
								fill: "origin",
							}
						]
					},
					options: {

						elements: {
							point: {
								pointStyle: false,
							}
						},
						plugins: {
							legend: {
						        display: false
						    },
							tooltip: {
								  enabled: this.opts.showTooltip?.toLowerCase() == "true",
							      mode: 'index',
							      intersect: false,
							      displayColors: false,

							      callbacks: {
							    	  label: item => item.raw + " " + this.msg.hits
							      }
							},
						},
						scales: {
							y: {
								beginAtZero: true,
								display: false,
							},
							x: {
								type: "time",

								time: {
									unit: "year",
									tooltipFormat: "yyyy",
									displayFormats: {
										"year" : "yyyy"
									},
									parser: s => {
										let date = new Date();
										date.setYear(parseInt(s));
										return date.getTime();
									}
								},
							    ticks: {
							    	maxTicksLimit: 5,
							    	maxRotation: 0,
							    }
							}
						}
					}

			}
			if(this.refs.chart) {

				this.chart = new Chart(chartElement, this.chartConfig);
				this.initDraw();

				if(this.startYear > this.yearList[0] || this.endYear < this.yearList[this.yearList.length-1]) {
					this.drawInitialRange();
				}
				this.update();
			}
		})

		this.generateCompleteYearMap = function(datamap) {
			let keys = Array.from(datamap.keys());
			let startYear = parseInt(keys[0]);
			let endYear = parseInt(keys[keys.length-1]);
			let yearMap = new Map();
			for(let year = startYear; year <= endYear; year++) {
				let value = datamap.get(year.toString());
				yearMap.set(year, value !== undefined ? value : 0);
			}
			return yearMap;
		}.bind(this)

		this.drawInitialRange = function() {
			var points = this.chart.getDatasetMeta(0).data;
			if(points && points.length){
				let startYearIndex = this.yearList.indexOf(this.startYear);
				let endYearIndex = this.yearList.indexOf(this.endYear);
				let x1 = points[startYearIndex].x;
				let x2 = points[endYearIndex].x;
				this.drawRect(x1, x2, this.refs.draw);
			}
		}.bind(this)

		this.initDraw = function() {
			let width = this.refs.chart.offsetWidth;
			let height = this.refs.chart.offsetHeight;
			this.refs.draw.style.width = width + "px";
			this.refs.draw.style.height = height + "px";
			this.refs.draw.style.position = "absolute";
			this.refs.draw.style.top = 0;
			this.refs.container.style.position = "relative";

			let startPoint = undefined;
 			let initialYear = undefined;
			let drawing = false;

			this.refs.draw.addEventListener("mousedown", e => {
				if(!this.refs["button_search"].disabled) {
					initialYear = this.calculateYearFromEvent(e);
					this.startYear = initialYear;
					this.endYear = initialYear;
					startPoint = this.getPointFromEvent(e, this.refs.draw);
					drawing = true;
					this.refs.draw.getContext("2d").clearRect(0, 0, this.refs.draw.width, this.refs.draw.height);
					this.update();
				}
			})
			this.refs.draw.addEventListener("mouseout", e => {
				if(drawing) {

					drawing = false;
				}
				let event = new MouseEvent("mouseout", {
					bubbles: false,
					target: e.target,
					clientX: e.clientX,
					clientY: e.clientY
				});
				this.refs.chart.dispatchEvent(event);
			});
			this.refs.draw.addEventListener("mousemove", e => {
				if(drawing) {
					let year = this.calculateYearFromEvent(e);
					if(!isNaN(year)) {
						if(year < initialYear) {
							this.endYear = initialYear;
							this.startYear = year;
						} else {
							this.endYear = year;
							this.startYear = initialYear;
						}
						this.startYear = Math.min(year, this.startYear);
						this.endYear = Math.max(year, this.endYear);
						let currPoint = this.getPointFromEvent(e, this.refs.draw);
						this.drawRect(startPoint.x, currPoint.x, this.refs.draw);
						this.update();
					}
				} else {
					let event = new MouseEvent("mousemove", {
						bubbles: false,
						target: e.target,
						clientX: e.clientX,
						clientY: e.clientY
					});
					this.refs.chart.dispatchEvent(event);
				}
			})
			this.refs.draw.addEventListener("mouseup", e => {
				if(drawing) {
					drawing = false;
					if(this.startYear && this.endYear) {
						this.setRange();
					}
					this.update();
				}
			})
		}.bind(this)

		this.drawRect = function(x1, x2, canvas) {
			let scaleX = canvas.width/canvas.getBoundingClientRect().width;

		    let x1Scaled = x1*scaleX;
		    let x2Scaled = x2*scaleX;
			let drawContext = canvas.getContext("2d");
			drawContext.clearRect(0,0, canvas.width, canvas.height);
			drawContext.beginPath();
			drawContext.rect(x1Scaled, 1, x2Scaled-x1Scaled, canvas.height-1);
			drawContext.globalAlpha = 1;
			drawContext.strokeStyle = this.rangeBorderColor;
			drawContext.stroke();
 			drawContext.globalAlpha = this.rangeOpacity;
			drawContext.fillStyle = this.rangeFillColor;
			drawContext.fill();
		}.bind(this)

		this.setStartYear = function(e) {
			e.preventUpdate = true;
			let year = parseInt(e.target.value);
			this.startYear = Math.min.apply(Math, this.yearList.filter((x) => x >= year));
			this.fixDateOrder();
		}.bind(this)

		this.setEndYear = function(e) {
			e.preventUpdate = true;
			let year = parseInt(e.target.value);
			this.endYear = Math.max.apply(Math, this.yearList.filter((x) => x <= year));
			this.fixDateOrder();
		}.bind(this)

		this.fixDateOrder = function() {
			if(this.startYear > this.endYear) {
				let temp = this.startYear;
				this.startYear = this.endYear;
				this.endYear = temp;
			}
		}.bind(this)

		this.setRange = function() {

			    $( this.loader ).addClass( 'active' );

			    Array.from(document.getElementsByClassName("chronology-slider__input-start")).forEach(element => element.disabled = true);
			    Array.from(document.getElementsByClassName("chronology-slider__input-end")).forEach(element => element.disabled = true);
			    Array.from(document.getElementsByClassName("chronology-slider__ok-button")).forEach(element => element.disabled = true);

			    let value = '[' + this.startYear + ' TO ' + this.endYear + ']' ;
			    $( this.valueInput ).val(value);

			    $( this.updateFacet ).click();
		}.bind(this)

		this.calculateYearFromEvent = function(e) {
			var activePoints = this.chart.getElementsAtEventForMode(e, 'nearest', { axis: "x" }, true);
		    if(activePoints.length > 0) {
		    	let year = this.yearList[activePoints[0].index];
		    	return year;
		    }
		}.bind(this)

		this.getPointFromEvent = function(e, canvas) {
			let currX = e.clientX - canvas.getBoundingClientRect().left;
		    let currY = e.clientY - canvas.getBoundingClientRect().top;
		    return {x: currX, y: currY};
		}.bind(this)

	  this.on('update', function(){
		$(".chronology-slider__input-start, .chronology-slider__input-end").keyup(function(event) {
		    if (event.keyCode === 13) {
		        $('[data-trigger="triggerFacettingGraph"]').click();
		    }
		});
	  })

	  this.on('mount', function(){
		$(".chronology-slider__input-start, .chronology-slider__input-end").keyup(function(event) {
		    if (event.keyCode === 13) {
		        $('[data-trigger="triggerFacettingGraph"]').click();
		    }
		});
	  })

});
riot.tag2('chronologyslider', '<div class="widget-chronology-slider__item chronology-slider-start"><input ref="inputStart" data-input="number" class="widget-chronology-slider__item-input -no-outline -active-border" riot-value="{startYear}" title="{msg.enterYear}" data-toggle="tooltip" data-placement="top" aria-label="{msg.enterYear}"></input></div><div class="widget-chronology-slider__item chronology-slider-end"><input ref="inputEnd" data-input="number" class="widget-chronology-slider__item-input -no-outline -active-border" riot-value="{endYear}" title="{msg.enterYear}" data-toggle="tooltip" data-placement="top" aria-label="{msg.enterYear}"></input></div><div class="widget-chronology-slider__item chronology-slider"><div class="widget-chronology-slider__slider" ref="slider"></div></div>', '', '', function(opts) {

this.msg={}
this.on("mount", () => {
	this.yearList = JSON.parse(opts.yearList);
	this.startYear = parseInt(opts.startYear);
	this.endYear = parseInt(opts.endYear);
	this.minYear = this.yearList[0];
	this.maxYear = this.yearList[this.yearList.length - 1];
	this.valueInput = document.getElementById(opts.valueInput);
	this.updateFacet = document.getElementById(opts.updateFacet);
	this.loader = document.getElementById(opts.loader);
	this.msg = opts.msg;
	this.rtl = $( this.refs.slider ).closest('[dir="rtl"]').length > 0;
	this.update();
});

this.on("updated", () => {
	this.initSlider();
	this.initChangeEvents();
	this.setHandlePositions();
});

this.initSlider = function() {
	let options = {
			range: true,
			isRTL: this.rtl,
			min: 0,
			max: this.yearList.length - 1,
			values: [ this.yearList.indexOf( this.startYear ), this.yearList.indexOf( this.endYear ) ],
			slide: ( event, ui ) => {

				$( this.refs.inputStart ).val( this.yearList[ ui.values[ 0 ] ] );
				$( this.refs.inputEnd ).val( this.yearList[ ui.values[ 1 ] ] );

				if (this.rtl) {

					if ( ui.values[ 0 ] == ui.values[ 1 ] ) {
		        		$(this.refs.slider).find( ".ui-slider-handle" ).first().css('margin-right', '0px');
		        		$(this.refs.slider).find( ".ui-slider-handle" ).last().css('margin-left', '-10px');
		        	}	else {
		        		$(this.refs.slider).find( ".ui-slider-handle" ).last().css('margin-left', '0px');
					}

					$(this.refs.slider).find( ".ui-slider-handle" ).first().css('margin-left', '-10px');

				}
				else {

	    			this.$getLastHandle().css('margin-left', -1 * this.$getLastHandle().width() * ( ui.values[ 1 ] / this.$getSlider().slider('option', 'max')));

	    			this.$getFirstHandle().css('margin-left', -1 * this.$getFirstHandle().width() * ( ui.values[ 0 ] / this.$getSlider().slider('option', 'max')));

				}

			},
			change: ( event, ui ) => {
				var startDate = parseInt( $( this.refs.inputStart ).val() );
				var endDate = parseInt( $( this.refs.inputEnd ).val() );

				startDate =  this.yearList[ui.values[0]];
				endDate =  this.yearList[ui.values[1]];

				if(endDate >= startDate) {

				    $( this.loader ).addClass( 'active' );

				    let value = '[' + startDate + ' TO ' + endDate + ']' ;
				    $( this.valueInput ).val(value);

				   $( this.updateFacet ).click();
				}

			},
		}

    $( this.refs.slider ).slider(options);
}.bind(this)

this.setHandlePositions = function() {

	let firstHandlePos = parseInt( $(this.refs.slider).find(".ui-slider-handle:first" ).css('left') );
	let lastHandlePos = parseInt( $(this.refs.slider).find(".ui-slider-handle:last" ).css('left') );

	if (this.rtl) {

		$(this.refs.slider).find(".ui-slider-handle" ).first().css('margin-left', '-10px');
		$(this.refs.slider).find(".ui-slider-handle" ).last().css('margin-left', '0px');

    	if ( firstHandlePos == lastHandlePos ) {
    		$(this.refs.slider).find(".ui-slider-handle" ).last().css('margin-left', '-10px');
    	}

	} else {
		$(this.refs.slider).find(".ui-slider-handle" ).last().css('margin-left', -1 * $(this.refs.slider).find(".ui-slider-handle" ).last().width() * ($(this.refs.slider).slider( "values", 1 ) / $(this.refs.slider).slider('option', 'max')));
		$(this.refs.slider).find(".ui-slider-handle" ).first().css('margin-left', -1 * $(this.refs.slider).find(".ui-slider-handle" ).first().width() * ($(this.refs.slider).slider( "values", 0 ) / $(this.refs.slider).slider('option', 'max')));
	}
}.bind(this)

this.initChangeEvents = function() {
	$(this.refs.inputStart).on("change", (event) => {

      let value = parseInt(event.target.value);
      if(!isNaN(value)) {
          let yearIndex = this.getClosestYearIndexAbove(value, this.yearList);

          $(this.refs.slider).slider( "values", 0, yearIndex );
      }
  })
  $(this.refs.inputEnd).on("change", (event) => {
      let value = parseInt(event.target.value);
      if(!isNaN(value)) {
          let yearIndex = this.getClosestYearIndexBelow(value, this.yearList);

          $(this.refs.slider).slider( "values", 1, yearIndex );
      }
  })
}.bind(this)

this.getClosestYearIndexAbove = function(value, years) {
    for (var i = 0; i < years.length; i++) {
        let year = years[i];
        if(year >= value) {
            return i;
        }
    }
    return years.length-1;
}.bind(this)

this.getClosestYearIndexBelow = function(value, years) {
    for (var i = years.length; i > -1 ; i--) {
        let year = years[i];
        if(year <= value) {
            return i;
        }
    }
    return 0;
}.bind(this)

this.$getFirstHandle = function() {
	return $(this.refs.slider).find( ".ui-slider-handle" ).first();
}.bind(this)

this.$getLastHandle = function() {
	return $(this.refs.slider).find( ".ui-slider-handle" ).last();
}.bind(this)

this.$getSlider = function() {
	return $(this.refs.slider);
}.bind(this)

});
riot.tag2('collectionlist', '<div if="{collections}" each="{collection, index in collections}" class="card-group"><div class="card" role="tablist"><div class="card-header"><div class="card-thumbnail"><img if="{collection.thumbnail}" alt="{getValue(collection.label)}" class="img-fluid" riot-src="{collection.thumbnail[\'@id\']}"></div><h3 class="card-title"><a href="{getId(collection.rendering)}">{getValue(collection.label)} ({viewerJS.iiif.getContainedWorks(collection)})</a><a if="{hasChildren(collection)}" class="collapsed card-title-collapse" href="#collapse-{this.opts.setindex}-{index}" role="button" data-toggle="collapse" aria-expanded="false"><i class="fa fa-angle-flip" aria-hidden="true"></i></a></h3><div class="tpl-stacked-collection__actions"><div class="tpl-stacked-collection__info-toggle"><a if="{hasDescription(collection)}" href="#description-{this.opts.setindex}-{index}" role="button" data-toggle="collapse" aria-expanded="false"><i class="fa fa-info-circle" aria-hidden="true"></i></a></div><div class="card-rss"><a href="{viewerJS.iiif.getRelated(collection, \'Rss feed\')[\'@id\']}"><i class="fa fa-rss" aria-hidden="true"></i></a></div></div></div><div if="{hasDescription(collection)}" id="description-{this.opts.setindex}-{index}" class="card-collapse collapse" role="tabcard" aria-expanded="false"><p class="tpl-stacked-collection__long-info"><raw html="{getDescription(collection)}"></raw></p></div><div if="{hasChildren(collection)}" id="collapse-{this.opts.setindex}-{index}" class="card-collapse collapse" role="tabcard" aria-expanded="false"><div class="card-body"><subcollection if="{collection.members && collection.members.length > 0}" collection="{collection}" language="{this.opts.language}" defaultlanguage="{this.opts.defaultlanguage}"></subcollection></div></div></div></div>', '', 'class="tpl-stacked-collection__collection-list"', function(opts) {

riot.tag('raw', '', function(opts) {
    this.root.innerHTML = opts.html;
});

this.collections = this.opts.collections;

this.on("mount", () => {

    if(opts.depth == undefined) {
        opts.depth = 1;
    } else {
        opts.depth = parseInt(opts.depth);
    }
    this.loadSubCollections();
})

this.loadSubCollections = function() {
    let observable = rxjs.from(this.collections);

    for (let level = 0; level < opts.depth; level++) {
        observable = observable.pipe(
         	rxjs.operators.mergeMap( child => this.fetchMembers(child) ),
         	rxjs.operators.mergeMap( child => child ),
        );
    }
    observable.pipe(
    	rxjs.operators.debounceTime(100)
     )
    .subscribe( () => this.update());

}.bind(this)

this.fetchMembers = function(collection) {
    if(this.hasChildren(collection)) {
	    return fetch(collection['@id'])
	    .then( result => result.json())
	    .then(json => {collection.members = json.members; return collection.members;})
    } else {
        return Promise.resolve([collection]);
    }
}.bind(this)

this.getValue = function(element) {
    return viewerJS.iiif.getValue(element, this.opts.language, this.opts.defaultlanguage);
}.bind(this)

this.hasChildren = function(element) {
    let count = viewerJS.iiif.getChildCollections(element);
    return count > 0;
}.bind(this)

this.getChildren = function(collection) {
    if(collection.members) {
    	return collection.members.filter( child => viewerJS.iiif.isCollection(child));
    } else {
        return [collection];
    }
}.bind(this)

this.hasDescription = function(element) {
    return element.description != undefined;
}.bind(this)

this.getDescription = function(element) {
    return this.getValue(element.description);
}.bind(this)

this.getId = function(element) {
    if(!element) {
        return undefined;
    } else if (Array.isArray(element) && element.length > 0) {
        return viewerJS.iiif.getId(element[0]);
    } else {
        return viewerJS.iiif.getId(element);
    }
}.bind(this)

});


riot.tag2('collectionview', '<div each="{set, index in collectionSets}"><h2 if="{set[0] != \'\'}">{translator.translate(set[0])}</h2><collectionlist collections="{set[1]}" language="{opts.language}" defaultlanguage="{opts.defaultlanguage}" setindex="{index}" depth="{opts.depth}"></collectionlist></div>', '', '', function(opts) {

this.collectionSets = [];

this.on("mount", () => {
    this.fetchCollections()
    .then( () => {
        let keys = this.collectionSets.map(set => set[0]);
        this.translator = new viewerJS.Translator(this.opts.restapi.replace("/rest", "/api/v1"), this.opts.language);
        return this.translator.init(keys);
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
    let separator = "?";
    if(this.opts.grouping) {
        url += (separator + "grouping=" + this.opts.grouping);
        separator = "&";
    }
    if(this.opts.blacklist) {
        url += (separator + "ignore=" + this.opts.blacklist);
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
    .sort( (m1,m2) => this.compareMembers(m1, m2, this.opts.sorting) )
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

this.compareMembers = function(m1, m2, compareMode) {
    let l1 = viewerJS.iiif.getValue(m1.label, this.opts.language, this.opts.defaultlanguage);
    let l2 = viewerJS.iiif.getValue(m2.label, this.opts.language, this.opts.defaultlanguage);
    if(compareMode && compareMode.toLocaleLowerCase() == "numeric") {
        let res = viewerJS.helper.compareNumerical(l1, l2);
        if(res == 0) {
            return viewerJS.helper.compareAlphanumerical(l1, l2);
        } else {
            return res;
        }
    } else if(compareMode && compareMode.toLocaleLowerCase() == "alphanumeric"){
        return viewerJS.helper.compareAlphanumerical(l1, l2);
    }
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
riot.tag2('authorityresourcequestion', '<div if="{this.showInstructions()}" class="crowdsourcing-annotations__instruction"><label>{Crowdsourcing.translate(⁗crowdsourcing__help__create_rect_on_image⁗)}</label></div><div if="{this.showInactiveInstructions()}" class="crowdsourcing-annotations__single-instruction -inactive"><label>{Crowdsourcing.translate(⁗crowdsourcing__help__make_active⁗)}</label></div><div class="crowdsourcing-annotations__wrapper" id="question_{opts.index}_annotation_{index}" each="{anno, index in this.question.annotations}"><div class="crowdsourcing-annotations__annotation-area -small"><div if="{this.showAnnotationImages()}" class="crowdsourcing-annotations__annotation-area-image" riot-style="border-color: {anno.getColor()}"><img riot-src="{this.question.getImage(anno)}"></img></div><div if="{!this.opts.item.isReviewMode()}" class="crowdsourcing-annotations__question-text-input"><span class="crowdsourcing-annotations__gnd-text">https://d-nb.info/gnd/</span><input class="crowdsourcing-annotations__gnd-id form-control" onchange="{setIdFromEvent}" riot-value="{question.authorityData.baseUri && getIdAsNumber(anno)}"></input></div><div if="{this.opts.item.isReviewMode()}" class="crowdsourcing-annotations__question-text-input"><input class="form-control pl-1" disabled="{this.opts.item.isReviewMode() ? \'disabled\' : \'\'}" riot-value="{question.authorityData.baseUri}{getIdAsNumber(anno)}"></input><div if="{this.opts.item.isReviewMode()}" class="crowdsourcing-annotations__jump-to-gnd"><a target="_blank" href="{question.authorityData.baseUri}{getIdAsNumber(anno)}">{Crowdsourcing.translate(⁗cms_menu_create_item_new_tab⁗)}</a></div></div><div class="cms-module__actions crowdsourcing-annotations__annotation-action"><button if="{!this.opts.item.isReviewMode()}" onclick="{deleteAnnotationFromEvent}" class="crowdsourcing-annotations__delete-annotation btn btn--clean delete">{Crowdsourcing.translate(⁗action__delete_annotation⁗)} </button></div></div></div><button if="{showAddAnnotationButton()}" onclick="{addAnnotation}" class="options-wrapper__option btn btn--default" id="add-annotation">{Crowdsourcing.translate(⁗action__add_annotation⁗)}</button>', '', '', function(opts) {

	this.question = this.opts.question;

	this.on("mount", function() {
	    this.question.initializeView((anno) => new Crowdsourcing.Annotation.AuthorityResource(anno, this.question.authorityData.context), this.update, this.update, this.focusAnnotation);
	    this.opts.item.onImageOpen(function() {
	        switch(this.question.targetSelector) {
	            case Crowdsourcing.Question.Selector.WHOLE_PAGE:
	            case Crowdsourcing.Question.Selector.WHOLE_SOURCE:
	                if(this.question.annotations.length == 0 && !this.question.item.isReviewMode()) {
	                    this.question.addAnnotation();

	                    this.opts.item.dirty = false;
	                }
	        }
	        this.update()
	    }.bind(this));
	});

	this.focusAnnotation = function(index) {
	    let id = "question_" + this.opts.index + "_annotation_" + index;
	    let inputSelector = "#" + id + " input";
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

	this.showLinkToGND = function() {
	    return this.question.isReviewMode() && this.question.isRegionTarget();
	}.bind(this)

    this.setIdFromEvent = function(event) {
        event.preventUpdate = true;
        if(event.item.anno) {
            let uri = this.question.authorityData.baseUri;
            if(!uri.endsWith("/")) {
                uri += "/"
            }
            uri += event.target.value;
            event.item.anno.setId(uri);
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

    this.getIdAsNumber = function(anno) {
        if(anno.isEmpty()) {
            return "";
        } else {
            return anno.getId().replace(this.question.authorityData.baseUri, "").replace("/", "");
        }
    }.bind(this)

});


riot.tag2('campaignitem', '<div if="{!opts.pi}" class="crowdsourcing-annotations__content-wrapper"> {Crowdsourcing.translate(⁗crowdsourcing__error__no_item_available⁗)} </div><div if="{opts.pi}" class="crowdsourcing-annotations__content-wrapper"><span if="{this.loading}" class="crowdsourcing-annotations__loader-wrapper"><img riot-src="{this.opts.loaderimageurl}"></span></span><div class="crowdsourcing-annotations__content-left"><imageview if="{this.item}" id="mainImage" source="{this.item.getCurrentCanvas()}" item="{this.item}"></imageView></div><div if="{this.item}" class="crowdsourcing-annotations__content-right"><div class="crowdsourcing-annotations__questions-wrapper"><div each="{question, index in this.item.questions}" onclick="{setActive}" class="crowdsourcing-annotations__question-wrapper {question.isRegionTarget() ? \'area-selector-question\' : \'\'} {question.active ? \'active\' : \'\'}"><div class="crowdsourcing-annotations__question-wrapper-description">{Crowdsourcing.translate(question.text)}</div><plaintextquestion if="{question.questionType == \'PLAINTEXT\'}" question="{question}" item="{this.item}" index="{index}"></plaintextQuestion><richtextquestion if="{question.questionType == \'RICHTEXT\'}" question="{question}" item="{this.item}" index="{index}"></richtextQuestion><geolocationquestion if="{question.questionType == \'GEOLOCATION_POINT\'}" question="{question}" item="{this.item}" index="{index}" geomap="{this.opts.geomap}"></geoLocationQuestion><authorityresourcequestion if="{question.questionType == \'NORMDATA\'}" question="{question}" item="{this.item}" index="{index}"></authorityResourceQuestion><metadataquestion if="{question.questionType == \'METADATA\'}" question="{question}" item="{this.item}" index="{index}"></metadataQuestion></div></div><campaignitemlog if="{item.showLog}" item="{item}"></campaignItemLog><div if="{!item.pageStatisticMode && !item.isReviewMode()}" class="crowdsourcing-annotations__options-wrapper crowdsourcing-annotations__options-wrapper-annotate"><button onclick="{saveAnnotations}" class="crowdsourcing-annotations__options-wrapper__option btn btn--default" id="save">{Crowdsourcing.translate(⁗button__save⁗)}</button><div>{Crowdsourcing.translate(⁗label__or⁗)}</div><button if="{item.isReviewActive()}" onclick="{submitForReview}" class="options-wrapper__option btn btn--success" id="review">{Crowdsourcing.translate(⁗action__submit_for_review⁗)}</button><button if="{!item.isReviewActive()}" onclick="{saveAndAcceptReview}" class="options-wrapper__option btn btn--success" id="review">{Crowdsourcing.translate(⁗action__accept_review⁗)}</button><div>{Crowdsourcing.translate(⁗label__or⁗)}</div><button if="{this.opts.nextitemurl}" onclick="{skipItem}" class="options-wrapper__option btn btn--link" id="skip">{Crowdsourcing.translate(⁗action__skip_item⁗)}</button></div><div if="{!item.pageStatisticMode && item.isReviewActive() && item.isReviewMode()}" class="crowdsourcing-annotations__options-wrapper crowdsourcing-annotations__options-wrapper-review"><button onclick="{acceptReview}" class="options-wrapper__option btn btn--success" id="accept">{Crowdsourcing.translate(⁗action__accept_review⁗)}</button><div>{Crowdsourcing.translate(⁗label__or⁗)}</div><button onclick="{rejectReview}" class="options-wrapper__option btn btn--default" id="reject">{Crowdsourcing.translate(⁗action__reject_review⁗)}</button><div>{Crowdsourcing.translate(⁗label__or⁗)}</div><button if="{this.opts.nextitemurl}" onclick="{skipItem}" class="options-wrapper__option btn btn--link" id="skip">{Crowdsourcing.translate(⁗action__skip_item⁗)}</button></div><div if="{item.pageStatisticMode && !item.isReviewMode()}" class="crowdsourcing-annotations__options-wrapper crowdsourcing-annotations__options-wrapper-annotate"><button onclick="{savePageAnnotations}" class="crowdsourcing-annotations__options-wrapper__option btn btn--default" id="save">{Crowdsourcing.translate(⁗button__save_page⁗)}</button><div>{Crowdsourcing.translate(⁗label__or⁗)}</div><button if="{item.isReviewActive()}" onclick="{submitPageForReview}" class="options-wrapper__option btn btn--success" id="review">{Crowdsourcing.translate(⁗action__submit_page_for_review⁗)}</button><button if="{!item.isReviewActive()}" onclick="{saveAndAcceptReviewForPage}" class="options-wrapper__option btn btn--success" id="review">{Crowdsourcing.translate(⁗action__accept_page_review⁗)}</button><div>{Crowdsourcing.translate(⁗label__or⁗)}</div><button if="{this.opts.nextitemurl}" onclick="{skipItem}" class="options-wrapper__option btn btn--link" id="skip">{Crowdsourcing.translate(⁗action__skip_item⁗)}</button></div><div if="{item.pageStatisticMode && item.isReviewActive() && item.isReviewMode()}" class="crowdsourcing-annotations__options-wrapper crowdsourcing-annotations__options-wrapper-review"><button onclick="{acceptReviewForPage}" class="options-wrapper__option btn btn--success" id="accept">{Crowdsourcing.translate(⁗action__accept_page_review⁗)}</button><div>{Crowdsourcing.translate(⁗label__or⁗)}</div><button onclick="{rejectReviewForPage}" class="options-wrapper__option btn btn--default" id="reject">{Crowdsourcing.translate(⁗action__reject_page_review⁗)}</button><div>{Crowdsourcing.translate(⁗label__or⁗)}</div><button if="{this.opts.nextitemurl}" onclick="{skipItem}" class="options-wrapper__option btn btn--link" id="skip">{Crowdsourcing.translate(⁗action__skip_item⁗)}</button></div></div></div>', '', '', function(opts) {

	this.itemSource = this.opts.restapiurl + "crowdsourcing/campaigns/" + this.opts.campaign + "/" + this.opts.pi + "/";
	this.annotationSource = this.itemSource + "annotations/";
	this.loading = true;

	this.on("mount", function() {
	    fetch(this.itemSource)
	    .then(response => this.handleServerResponse(response))
	    .then( itemConfig => this.loadItem(itemConfig))
	    .then( () => this.fetch(this.annotationSource))
	    .then(response => this.handleServerResponse(response))
	    .then( annotations => this.initAnnotations(annotations))
	    .then( () => this.item.notifyItemInitialized())
		.catch( error => {
		   	this.handleError(error);
			this.item = undefined;
	    	this.loading = false;
	    	this.update();
		})
	});

	this.loadItem = function(itemConfig) {
	    this.item = new Crowdsourcing.Item(itemConfig, 0);
	    this.item.logEndpoint = this.item.id + "/" + this.opts.pi + "/log/";
	    if(this.opts.currentuserid) {
	        this.item.setCurrentUser(this.opts.currentuserid, this.opts.currentusername, this.opts.currentuseravatar);
	    }
	    this.item.nextItemUrl = this.opts.nextitemurl;
	    this.item.setReviewMode(this.opts.itemstatus && this.opts.itemstatus.toUpperCase() == "REVIEW");
		this.item.onImageRotated( () => this.update());
		return fetch(this.item.imageSource)
		.then(response => this.handleServerResponse(response))
		.then( imageSource => this.item.initViewer(imageSource))
		.then( () => this.loading = false)
		.then( () => this.update())
		.then( () => this.item.onImageOpen( () => {this.loading = false; this.update()}))
		.then( () => this.item.statusMapUpdates.subscribe( statusMap => this.update()))

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

	this.initAnnotations = function(annotations) {
	    let save = this.item.createAnnotationMap(annotations);
	    this.item.saveToLocalStorage(save);
	}.bind(this)

	this.initAnnotationsForPage = function(annotations, pageId) {
	    annotations = annotations.filter( (anno) => pageId == Crowdsourcing.getResourceId(anno.target) );
	    let save = this.item.getFromLocalStorage();
	    this.item.deleteAnnotations(save, pageId);
        this.item.addAnnotations(annotations, save);

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

	this.resetItemsForPage = function() {
	    fetch(this.annotationSource)
	    .then( response => response.json() )
	    .then( annotations => this.initAnnotationsForPage(annotations, this.item.getCurrentPageId()))
	    .then( () => this.resetQuestions())
	    .then( () => this.item.notifyAnnotationsReload())
	    .then( () => this.update())
		.catch( error => console.error("ERROR ", error));
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
	    .then( res => res.ok ? res : Promise.reject(res) )
	    .then(res => {this.item.dirty=false; return res});
	}.bind(this)

	this.savePageToServer = function() {
	    let pages = this.item.loadAnnotationPages(undefined, this.item.getCurrentPageId());
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
	    .then( res => res.ok ? res : Promise.reject(res) )
	    .then(res => {this.item.dirty=false; return res});

	}.bind(this)

	this.saveAnnotations = function() {
	    this.saveToServer()
	    .then(() => this.resetItems())
	    .then(() => this.setStatus("ANNOTATE"))
	    .then((res) => {
	        this.loading = false;
	        viewerJS.notifications.success(Crowdsourcing.translate("crowdsourcing__save_annotations__success"));
		    this.update();
	    })
	    .catch((error) => {
	        this.loading = false;
	        console.log("Error saving page annotations ", error);
	        viewerJS.notifications.error(Crowdsourcing.translate("crowdsourcing__save_annotations__error"));
		    this.update();
	    })
	}.bind(this)

	this.savePageAnnotations = function() {
	    this.savePageToServer()
	    .then(() => this.resetItemsForPage())
	    .then(() => this.setStatus("ANNOTATE"))
	    .then((res) => {
	        this.loading = false;
	        viewerJS.notifications.success(Crowdsourcing.translate("crowdsourcing__save_annotations__success"));
		    this.update();
	    })
	    .catch((error) => {
	        this.loading = false;
	        console.log("Error saving page annotations ", error);
	        viewerJS.notifications.error(Crowdsourcing.translate("crowdsourcing__save_annotations__error"));
		    this.update();
	    })
	}.bind(this)

	this.submitForReview = function() {
	    this.saveToServer()
	    .then(() => this.setStatus("REVIEW"))
	    .then(() => this.skipItem());
	}.bind(this)

	this.submitPageForReview = function() {
	    this.savePageToServer()
	    .then(() => this.setPageStatus("REVIEW"))
	    .then(() => this.skipPage());
	}.bind(this)

	this.saveAndAcceptReview = function() {
	    this.saveToServer()
	    .then(() => this.setStatus("FINISHED"))
	    .then(() => this.skipItem());
	}.bind(this)

	this.saveAndAcceptReviewForPage = function() {
	    this.savePageToServer()
	    .then(() => this.setPageStatus("FINISHED"))
	    .then(() => this.skipPage());
	}.bind(this)

	this.acceptReview = function() {
	    this.setStatus("FINISHED")
	    .then(() => this.skipItem());
	}.bind(this)

	this.acceptReviewForPage = function() {
	    this.setPageStatus("FINISHED")
	    .then(() => this.skipPage());
	}.bind(this)

	this.rejectReview = function() {
	    this.setStatus("ANNOTATE")
	    .then(() => this.skipItem());
	}.bind(this)

	this.rejectReviewForPage = function() {
	    this.setPageStatus("ANNOTATE")
	    .then(() => this.skipPage());
	}.bind(this)

	this.skipItem = function() {
		this.item.loadNextItem(true);
	}.bind(this)

	this.skipPage = function() {
	    let index = this.item.getNextAccessibleIndex(this.item.currentCanvasIndex);
	    if(index == undefined) {
	        this.skipItem();
	    } else {
	        this.item.loadImage(index);
	    }
	}.bind(this)

	this.setPageStatus = function(status) {
	    return this.setStatus(status);
	}.bind(this)

	this.setStatus = function(status) {
	    let body = {
	            recordStatus: status,
	            creator: this.item.getCreator().id,
	    }
	    return fetch(this.itemSource + (this.item.currentCanvasIndex + 1 ) + "/", {
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

	this.handleServerResponse = function(response) {
   		if(!response.ok){
   			try {
   				throw response.json()
   			} catch(error) {
   				response.message = error;
   				throw response;
   			}
   		} else {
   			try {
   				return response.json();
   			} catch(error) {
   				response.message = error;
   				throw response;
   			}
   		}
	}.bind(this)

	this.handleError = function(error) {
		 console.error("ERROR", error);
		    if(viewerJS.isString(error)) {
		    	viewerJS.notifications.error(error);
		    } else if(error.message && error.message.then) {
		    	error.message.then((err) => {
			    	console.log("error ", err)
			    	let errorMessage = "Error retrieving data from <br/>";
			    	errorMessage += error.url + "<br/><br/>";
			    	if(err.message) {
			    		errorMessage += "Message = " + err.message + "<br/><br/>";
			    	}
			    	errorMessage += "Status = " + error.status;
			    	viewerJS.notifications.error(errorMessage);
		    	})
		    } else {
		    	let errorMessage = "Error retrieving data from\n\n";
		    	errorMessage += error.url + "\n\n";
		    	if(error.message) {
		    		errorMessage += "Message = " + error.message + "\n\n";
		    	}
		    	errorMessage += "Status = " + error.status;
		    	viewerJS.notifications.error(errorMessage);
		    }
	}.bind(this)

});
riot.tag2('campaignitemlog', '<div class="crowdsourcing-annotations__log-wrapper"><div class="crowdsourcing-annotations__log-title"><span>{Crowdsourcing.translate(⁗log⁗)}</span><button ref="compress" onclick="{compressLog}" class="crowdsourcing-annotations__log-expand btn btn--clear"><i class="fa fa-angle-up" aria-hidden="true"></i></button><button ref="expand" onclick="{expandLog}" class="btn btn--clear crowdsourcing-annotations__log-expand"><i class="fa fa-angle-down" aria-hidden="true"></i></button></div><div ref="toggleBox" class="crowdsourcing-annotations__toggle-box"><div ref="innerWrapper" class="crowdsourcing-annotations__log-inner-wrapper"><div each="{message in messages}" class="crowdsourcing-annotations__log-message-entry {isCurrentUser(message.creator) ? \'-from-me\' : \'\'}"><img class="crowdsourcing-annotations__log-round-avatar" riot-src="{message.creator.avatar}"></img><div class="crowdsourcing-annotations__log-speech-bubble"><div class="crowdsourcing-annotations__log-message-info"><div class="crowdsourcing-annotations__log-message-user-name"> {message.creator.name} </div></div><div class="crowdsourcing-annotations__log-message-text"> {message.message} </div><div class="crowdsourcing-annotations__log-message-time-stamp"> {formatDate(message.dateCreated)} </div></div></div></div><div ref="messageBox" class="crowdsourcing-annotations__log-send-message-area"><input onkeypress="{addMessageOnEnter}" placeholder="{Crowdsourcing.translate(\'label__enter_message_here\')}" class="crowdsourcing-annotations__log-message-input" id="crowdsourcingAnnotationsLogMessageInput" name="crowdsourcingAnnotationsLogMessageInput" ref="messageText"></input><button class="btn btn--default crowdsourcing-annotations__log-message-send-button" onclick="{addMessage}">{Crowdsourcing.translate(\'action__send\')}</button></div></div></div>', '', '', function(opts) {

this.currentUser = this.opts.item.currentUser;
this.messages = this.opts.item.log;
this.expanded = false;

this.on("mount", function() {

    if (sessionStorage.getItem("logCompressed") === 'logIsCompressed') {
    	$(this.refs.toggleBox).hide();
        $(this.refs.compress).hide();
    }
    else {
        $(this.refs.expand).hide();
    }

});

this.on("updated", function() {

    this.scrollToBottom();

});

this.addMessageOnEnter = function(event) {
    var code = event.keyCode || event.which;
	if(code==13){
	    this.addMessage();
	} else {

	    event.preventUpdate = true;
	}
}.bind(this)

this.addMessage = function() {
    let text = this.refs.messageText.value;
    this.refs.messageText.value = "";
    if(text.trim().length > 0) {
        let message = {
                message : text,
                dateCreated : new Date().toJSON(),
                creator : this.currentUser,
        }
        this.opts.item.addLogMessage(message);

    }

}.bind(this)

this.isCurrentUser = function(user) {
    return user.userId == this.currentUser.userId;
}.bind(this)

this.scrollToBottom = function() {
	$(this.refs.innerWrapper).scrollTop(this.refs.innerWrapper.scrollHeight);
}.bind(this)

this.expandLog = function() {
    $(this.refs.expand).hide({
        complete: () => {
        	$(this.refs.compress).show();
    	},
        duration: 0
    });
	$(this.refs.toggleBox).slideToggle(400);
    $('.crowdsourcing-annotations__content-right').animate({scrollTop: '+=400px'}, 400);
    sessionStorage.setItem('logCompressed', 'logNotCompressed');
}.bind(this)

this.compressLog = function() {
    $(this.refs.compress).hide({
        complete: () => {
        	$(this.refs.expand).show();
    	},
        duration: 0
    });
	$(this.refs.toggleBox).slideToggle(400);
	sessionStorage.setItem('logCompressed', 'logIsCompressed');
}.bind(this)

this.formatDate = function(dateString) {
    let date = new Date(dateString);
    return date.toLocaleString(Crowdsourcing.translator.language, {
		dateStyle: "long",
		timeStyle: "short"
    });
}.bind(this)

});

riot.tag2('canvaspaginator', '<nav class="numeric-paginator" aria-label="{Crowdsourcing.translate(aria_label__nav_pagination)}"><ul><li if="{getCurrentIndex() > 0}" class="numeric-paginator__navigate navigate_prev"><span onclick="{this.loadPrevious}"><i class="fa fa-angle-left" aria-hidden="true"></i></span></li><li each="{canvas in this.firstCanvases()}" class="group_left {this.getIndex(canvas) == this.getCurrentIndex() ? \'numeric-paginator__active\' : \'\'}"><span index="{this.getIndex(canvas)}" onclick="{this.loadFromEvent}">{this.getOrder(canvas)}</span></li><li class="numeric-paginator__separator" if="{this.useMiddleButtons()}">...</li><li each="{canvas in this.middleCanvases()}" class="group_middle {this.getIndex(canvas) == this.getCurrentIndex() ? \'numeric-paginator__active\' : \'\'}"><span index="{this.getIndex(canvas)}" onclick="{this.loadFromEvent}">{this.getOrder(canvas)}</span></li><li class="numeric-paginator__separator" if="{this.useLastButtons()}">...</li><li each="{canvas in this.lastCanvases()}" class="group_right {this.getIndex(canvas) == this.getCurrentIndex() ? \'numeric-paginator__active\' : \'\'}"><span index="{this.getIndex(canvas)}" onclick="{this.loadFromEvent}">{this.getOrder(canvas)}</span></li><li if="{getCurrentIndex() < getTotalImageCount()-1}" class="numeric-paginator__navigate navigate_next"><span onclick="{this.loadNext}"><i class="fa fa-angle-right" aria-hidden="true"></i></span></li></ul></nav>', '', '', function(opts) {

this.on( "mount", function() {

    var paginatorConfig = {
	        previous: () => this.load(this.getCurrentIndex()-1),
	        next: () => this.load(this.getCurrentIndex()+1),
	        first: () => this.load(0),
	        last: () => this.load(this.getTotalImageCount()-1),
	}
	viewerJS.paginator.init(paginatorConfig);

});

this.loadFromEvent = function(e) {
    let index = parseInt(e.target.attributes["index"].value);
	this.load(index);
}.bind(this)

this.load = function(index) {
    if(index != this.getCurrentIndex() && index >= 0 && index < this.getTotalImageCount()) {
		if(this.opts.actionlistener) {
			this.opts.actionlistener.next({
				action: "setImageIndex",
				value: index
			})
		}
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
    return this.opts.index
}.bind(this)

this.getIndex = function(canvas) {
    return this.opts.items.indexOf(canvas);
}.bind(this)

this.getOrder = function(canvas) {
    return this.getIndex(canvas) + 1;
}.bind(this)

this.getTotalImageCount = function() {
    return this.opts.items.length;
}.bind(this)

this.useMiddleButtons = function() {
    return this.getTotalImageCount() > 9 && this.getCurrentIndex() > 4 && this.getCurrentIndex() < this.getTotalImageCount()-5;
}.bind(this)

this.useLastButtons = function() {
    return this.getTotalImageCount() > 9;
}.bind(this)

this.firstCanvases = function() {
    if(this.getTotalImageCount() < 10) {
        return this.opts.items;
    } else if(this.getCurrentIndex() < 5) {
        return this.opts.items.slice(0, this.getCurrentIndex()+3);
    } else {
        return this.opts.items.slice(0, 2);
    }
}.bind(this)

this.middleCanvases = function() {
    if(this.getTotalImageCount() < 10) {
        return [];
    } else if(this.getCurrentIndex() < this.getTotalImageCount()-5 && this.getCurrentIndex() > 4) {
        return this.opts.items.slice(this.getCurrentIndex()-2, this.getCurrentIndex()+3);
    } else {
        return [];
    }
}.bind(this)

this.lastCanvases = function() {
    if(this.getTotalImageCount() < 10) {
        return [];
    } else if(this.getCurrentIndex() < this.getTotalImageCount()-5) {
        return this.opts.items.slice(this.getTotalImageCount()-2);
    } else {
        return this.opts.items.slice(this.getCurrentIndex()-2);
    }
}.bind(this)

this.toPageNumber = function(e) {
    let page = parseInt(e.target.value);
    if(page > 0 && page <= this.getTotalImageCount()) {
    	this.load(page-1);
    } else{
        alert(page + " is not a valid page number")
    }
}.bind(this)

});
riot.tag2('geolocationquestion', '<div if="{this.showInstructions()}" class="crowdsourcing-annotations__instruction"><label>{Crowdsourcing.translate(⁗crowdsourcing__help__create_rect_on_image⁗)}</label></div><div if="{this.showAddMarkerInstructions()}" class="crowdsourcing-annotations__single-instruction"><label>{Crowdsourcing.translate(⁗crowdsourcing__help__add_marker_to_image⁗)}</label></div><div if="{this.showInactiveInstructions()}" class="crowdsourcing-annotations__single-instruction -inactive"><label>{Crowdsourcing.translate(⁗crowdsourcing__help__make_active⁗)}</label></div><div class="geo-map__wrapper"><div ref="geocoder" class="geocoder"></div><div id="geoMap_{opts.index}" class="geo-map"></div></div><div id="annotation_{index}" each="{anno, index in this.annotations}"></div>', '', '', function(opts) {


this.question = this.opts.question;
this.annotationToMark = null;
this.addMarkerActive = !this.question.isRegionTarget() && !this.opts.item.reviewMode;

const DEFAULT_VIEW = {
    zoom: 5,
    center: [11.073397, 49.451993]
};

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
    if(this.geoMap.layers[0].getMarkerCount() > 0) {
        let zoom = 12;
        if(this.geoMap.layers[0].getMarkerCount() == 1) {
            let marker = this.geoMap.layers[0].getMarker(this.question.annotations[0].markerId);
            if(marker) {
            	zoom = marker.feature.view.zoom;
            }
        }
        let featureView = this.geoMap.getViewAroundFeatures(this.geoMap.layers[0].getFeatures(), zoom);
	    this.geoMap.setView(featureView);
    }
}.bind(this)

this.setFeatures = function(annotations) {
    this.geoMap.layers.forEach(l => l.resetMarkers());
    annotations.filter(anno => !anno.isEmpty()).forEach((anno) => {
        if(anno.color) {
            let markerIcon = this.geoMap.layers[0].getMarkerIcon().options;
            markerIcon.markerColor = anno.color;
            this.geoMap.layers[0].config.markerIcon = markerIcon;
        }
        let marker = this.geoMap.layers[0].addMarker(anno.body);
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
        let marker = this.geoMap.layers[0].getMarker(anno.markerId);
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
        language: Crowdsourcing.translator.language,
        tilesource: this.opts.geomap.tilesource,
        layers: [{
	        allowMovingFeatures: !this.opts.item.isReviewMode(),
	        popover: undefined,
	        emptyMarkerMessage: undefined,
	        popoverOnHover: false,
	        markerIcon: {
	            shape: "circle",
	            prefix: "fa",
	            markerColor: "blue",
	            iconColor: "white",
	            icon: "fa-circle",
	            svg: true
	        }
        }]
    })

    let initialView = $.extend(true, {}, DEFAULT_VIEW, this.opts.geomap.initialView);
    this.geoMap.init(initialView);
    this.geoMap.initGeocoder(this.refs.geocoder, {placeholder: Crowdsourcing.translate("ADDRESS")});
    this.geoMap.layers.forEach(l => l.onFeatureMove.subscribe(feature => this.moveFeature(feature)));
    this.geoMap.layers.forEach(l => l.onFeatureClick.subscribe(feature => this.removeFeature(feature)));
    this.geoMap.onMapClick.subscribe(geoJson => {
        if(this.addMarkerActive && (this.question.targetFrequency == 0 || this.geoMap.layers[0].getMarkerCount() < this.question.targetFrequency)) {
            if(this.annotationToMark && this.annotationToMark.color) {
                let markerIcon = this.geoMap.layers[0].getMarkerIcon().options;
                markerIcon.markerColor = this.annotationToMark.color;
                this.geoMap.layers[0].config.markerIcon = markerIcon;
            }
            let marker = this.geoMap.layers[0].addMarker(geoJson);
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
    let marker = this.geoMap.layers[0].getMarker(annotation.markerId);
    annotation.setBody(marker.feature);
    annotation.setView(marker.feature.view);
    this.question.saveToLocalStorage();
}.bind(this)

this.addFeature = function(id) {
    let marker = this.geoMap.layers[0].getMarker(id);
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
    this.geoMap.layers[0].removeMarker(feature);
	let annotation = this.getAnnotation(feature.id);
    if(annotation) {
	    this.question.deleteAnnotation(annotation);
	    this.question.saveToLocalStorage();
    }
}.bind(this)

});


riot.tag2('imagecontrols', '<div class="image_controls"><div class="image-controls__actions"><div onclick="{toggleThumbs}" class="image-controls__action thumbs {this.opts.imagecount < 2 ? \'d-none\' : \'\'} {this.opts.showthumbs ? \'in\' : \'\'}"><a></a></div><div if="{this.opts.image}" class="image-controls__action -imageControlsFont back {this.opts.imageindex === 0 ? \'-inactive\' : \'\'}"><a onclick="{previousItem}"><i class="image-back"></i></a></div><div if="{this.opts.image}" class="image-controls__action -imageControlsFont forward {this.opts.imageindex === this.opts.imagecount -1 ? \'-inactive\' : \'\'}"><a onclick="{nextItem}"><i class="image-forward"></i></a></div><div if="{this.opts.image}" class="image-controls__action -imageControlsFont rotate-left"><a onclick="{rotateLeft}"><i class="image-rotate_left"></i></a></div><div if="{this.opts.image}" class="image-controls__action -imageControlsFont rotate-right"><a onclick="{rotateRight}"><i class="image-rotate_right"></i></a></div><div if="{this.opts.image}" class="image-controls__action zoom-slider-wrapper"><input type="range" min="0" max="1" value="0" step="0.01" class="slider zoom-slider" aria-label="zoom slider"></div></div></div>', '', '', function(opts) {

    this.rotateRight = function()
    {
        if ( this.opts.image ) {
            this.opts.image.controls.rotateRight();
        }
    	this.handleAction("rotate", 90)
    }.bind(this)

    this.rotateLeft = function()
    {
        if ( this.opts.image ) {
            this.opts.image.controls.rotateLeft();
        }
    	this.handleAction("rotate", -90)
    }.bind(this)

    this.previousItem = function()
    {
    	if (this.opts.imageindex > 0) {
    		this.handleAction("previousImage");
    	}
    }.bind(this)

    this.nextItem = function()
    {
    	if (this.opts.imageindex < this.opts.imagecount -1) {
    		this.handleAction("nextImage");
    	}
    }.bind(this)

    this.toggleThumbs = function() {
    	this.opts.showthumbs = !this.opts.showthumbs;
    	this.handleAction("toggleThumbs", this.opts.showthumbs)
    }.bind(this)

    this.handleAction = function(control, value) {
    	if(this.opts.actionlistener) {
    		this.opts.actionlistener.next({
    			action: control,
    			value: value
    		});
    	}
    }.bind(this)

	$( document ).ready(function() {
	    $('.image-controls__action.thumbs').tooltip({
	      placement: 'top',
	      title: Crowdsourcing.translate("crowdsourcing__campaign_tooltip_back_to_overview"),
	      trigger: 'hover'
	    });
	    $('.image-controls__action.back').tooltip({
	        placement: 'top',
	      title: Crowdsourcing.translate("prevImage"),
	      trigger: 'hover'
	    });
	    $('.image-controls__action.forward').tooltip({
	      placement: 'top',
	      title: Crowdsourcing.translate("nextImage"),
	      trigger: 'hover'
	    });
	    $('.image-controls__action.rotate-left').tooltip({
	      placement: 'top',
	      title: Crowdsourcing.translate("rotateLeft"),
	      trigger: 'hover'
	    });
	    $('.image-controls__action.rotate-right').tooltip({
	      placement: 'top',
	      title: Crowdsourcing.translate("rotateRight"),
	      trigger: 'hover'
	    });
	});

});
/**
 * Takes a IIIF canvas object in opts.source. 
 * If opts.item exists, it creates the method opts.item.setImageSource(canvas) 
 * and provides an observable in opts.item.imageChanged triggered every time a new image source is loaded (including the first time)
 * The imageView itself is stored in opts.item.image
 */

riot.tag2('imageview', '<div id="wrapper_{opts.id}" class="imageview_wrapper"><span if="{this.error}" class="loader_wrapper"><span class="error_message">{this.error.message}</span></span><imagecontrols if="{this.image}" image="{this.image}" imageindex="{this.opts.item.currentCanvasIndex}" imagecount="{this.opts.item.canvases.length}" actionlistener="{this.actionListener}" showthumbs="{this.showThumbs}" class="{this.showThumbs ? \'d-none\' : \'\'}"></imageControls><div class="image_container {this.showThumbs ? \'d-none\' : \'\'}"><div id="image_{opts.id}" class="image"></div></div><div class="image_thumbnails-wrapper {this.opts.item.reviewMode ? \'reviewmode\' : \'\'} {this.showThumbs ? \'\' : \'d-none\'}"><div class="thumbnails-filters"><button ref="filter_unfinished" class="thumbnails-filter-unfinished btn btn--clean">{Crowdsourcing.translate(⁗crowdsourcing__campaign_filter_show_unfinished⁗)}</button><button ref="filter_reset" class="thumbnails-filter-reset btn btn--clean">{Crowdsourcing.translate(⁗crowdsourcing__campaign_filter_show_all⁗)}</button></div><thumbnails class="image_thumbnails" source="{{items: this.opts.item.canvases}}" actionlistener="{this.actionListener}" imagesize=",200" index="{this.opts.item.currentCanvasIndex}" statusmap="{getPageStatusMap()}"></thumbnails></div></div>', '', '', function(opts) {


	this.on("updated", function() {
		this.initTooltips();
	});

	this.on("mount", function() {
		this.showThumbs = this.isShowThumbs();
		this.initFilters();

		$("#controls_" + opts.id + " .draw_overlay").on("click", () => this.drawing = true);
		try{
			imageViewConfig.image.tileSource = this.getImageInfo(opts.source);
			this.image = new ImageView.Image(imageViewConfig);
			this.image.load()
			.then( (image) => {
				if(this.opts.item) {
					this.opts.item.image = this.image;

				    var now = rxjs.of(image);
					this.opts.item.setImageSource = function(source) {
					    this.image.setTileSource(this.getImageInfo(source));
					}.bind(this);
				    this.opts.item.notifyImageOpened(image.observables.viewerOpen.pipe(rxjs.operators.map( () => image),rxjs.operators.merge(now)));
				}
				return image;
			})
		} catch(error) {
		    console.error("ERROR ", error);
	    	this.error = error;
	    	this.update();
		}

		this.actionListener = new rxjs.Subject();
		this.actionListener.subscribe((event) => this.handleImageControlAction(event));
		if(this.opts.item.setShowThumbs) {
		    this.opts.item.setShowThumbs.subscribe(show => {
		        this.showThumbs = show;
		        this.update();
		    });
		}
	})

	this.initTooltips = function() {
	    $('.thumbnails-image-wrapper.review').tooltip('dispose');
	    $('.thumbnails-image-wrapper.review').tooltip({
	        placement: 'top',
	      title: Crowdsourcing.translate("crowdsourcing__campaign_tooltip_in_review"),
	      trigger: 'hover'
	    });

	    $('.thumbnails-image-wrapper.finished').tooltip('dispose');
	    $('.thumbnails-image-wrapper.finished').tooltip({
	        placement: 'top',
	      title: Crowdsourcing.translate("crowdsourcing__campaign_tooltip_completed"),
	      trigger: 'hover'
	    });

	    function updateLockedTooltip() {
	    	$('.thumbnails-image-wrapper.locked').tooltip('dispose');
		    $('.thumbnails-image-wrapper.locked').tooltip({
		      placement: 'top',
		      title: Crowdsourcing.translate("crowdsourcing__campaign_tooltip_locked"),
		      trigger: 'hover'
		    });

		    $(".thumbnails-image-wrapper.locked").each(function() {
				if ($(this).is(":hover")) {
    				$(this).tooltip('show');
			  }
			})

		    setTimeout(updateLockedTooltip, 4000);
	    }
	    updateLockedTooltip();

	}.bind(this)

	this.initFilters = function() {
    	this.refs.filter_unfinished.onclick = event => {
    		$('.thumbnails-image-wrapper').show();
    		$('.thumbnails-image-wrapper.review').hide();
    		$('.thumbnails-image-wrapper.annotate').hide();
    		$('.thumbnails-image-wrapper.finished').hide();
    	};

    	this.refs.filter_reset.onclick = event => {
    		$('.thumbnails-image-wrapper').show();
    	};

	}.bind(this)

	$( document ).ready(function() {

	    $('.thumbnails-filter-reset').addClass('-activeFilter');
	    $('.thumbnails-filter-reset, .thumbnails-filter-finished, .thumbnails-filter-unfinished, .thumbnails-filter-annotated').click(function() {
	    	$('.thumbnails-filter-reset, .thumbnails-filter-finished, .thumbnails-filter-unfinished, .thumbnails-filter-annotated').removeClass('-activeFilter');
	    	$(this).addClass('-activeFilter');
	    });
	});

    $('.image_thumbnails-wrapper.reviewmode .thumbnails-image-wrapper:not(".image_thumbnails-wrapper.reviewmode .thumbnails-image-wrapper.finished")').tooltip('dispose');

	this.getPosition = function() {
		let pos_os = this.dataPoint.getPosition();
		let pos_image = ImageView.CoordinateConversion.scaleToImage(pos_os, this.image.viewer, this.image.getOriginalImageSize());
		let pos_image_rot = ImageView.CoordinateConversion.convertPointFromImageToRotatedImage(pos_image, this.image.controls.getRotation(), this.image.getOriginalImageSize());
		return pos_image_rot;
	}.bind(this)

	this.handleImageControlAction = function(event) {

		switch(event.action) {
			case "toggleThumbs":
				this.showThumbs = event.value;
				this.update();
				break;
			case "rotate":
		        if(this.opts.item) {
		            this.opts.item.notifyImageRotated(event.value);
		        }
		        break;
			case "clickImage":
				this.showThumbs = false;
			case "setImageIndex":
			    this.opts.item.loadImage(event.value, true);
			    break;
			case "previousImage":
			    this.opts.item.loadImage(this.opts.item.getPreviousAccessibleIndex(this.opts.item.currentCanvasIndex), true);
				break;
			case "nextImage":
			    this.opts.item.loadImage(this.opts.item.getNextAccessibleIndex(this.opts.item.currentCanvasIndex), true);
			    break;

		}
	}.bind(this)

	this.getImageInfo = function(canvas) {
	    return canvas.images[0].resource.service["@id"] + "/info.json"
	}.bind(this)

	this.getPageStatusMap = function() {
			return this.opts.item.pageStatusMap;
	}.bind(this)

	this.isShowThumbs = function() {
		if(this.opts.item.reviewMode && this.opts.item.pageStatisticMode) {

			let count = 0;
			for(let status of this.opts.item.pageStatusMap.values()) {
			    if(status.toUpperCase() == "REVIEW") {
					count++;
				}
			}
			return count !== 1;
		} else {
			return this.opts.item.canvases.length > 1
		}
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


riot.tag2('metadataquestion', '<div if="{this.showInstructions()}" class="crowdsourcing-annotations__instruction"><label>{Crowdsourcing.translate(⁗crowdsourcing__help__create_rect_on_image⁗)}</label></div><div if="{this.showInactiveInstructions()}" class="crowdsourcing-annotations__single-instruction -inactive"><label>{Crowdsourcing.translate(⁗crowdsourcing__help__make_active⁗)}</label></div><div class="crowdsourcing-annotations__wrapper" id="question_{opts.index}_annotation_{index}" each="{anno, index in this.question.annotations}"><div class="crowdsourcing-annotations__annotation-area"><div if="{this.showAnnotationImages()}" class="crowdsourcing-annotations__annotation-area-image" riot-style="border-color: {anno.getColor()}"><img riot-src="{this.question.getImage(anno)}"></img></div><div class="crowdsourcing-annotations__question-metadata-list"><div each="{field, fieldindex in this.metadataFields}" class="crowdsourcing-annotations__question-metadata-list-item mb-2"><label class="crowdsourcing-annotations__question-metadata-list-item-label">{Crowdsourcing.translate(field)}:</label><div class="crowdsourcing-annotations__question-metadata-list-item-field" if="{this.hasOriginalValue(field)}">{this.getOriginalValue(field)}</div><input class="crowdsourcing-annotations__question-metadata-list-item-input form-control" if="{!this.hasOriginalValue(field)}" disabled="{this.opts.item.isReviewMode() ? \'disabled\' : \'\'}" ref="input_{index}_{fieldindex}" type="text" data-annotationindex="{index}" riot-value="{anno.getValue(field)}" onchange="{setValueFromEvent}"></input></div></div></div></div><button if="{showAddAnnotationButton()}" onclick="{addAnnotation}" class="options-wrapper__option btn btn--default" id="add-annotation">{Crowdsourcing.translate(⁗action__add_annotation⁗)}</button>', '', '', function(opts) {

	this.question = this.opts.question;

	this.metadataFields = [];

	this.originalData = {};

	this.on("mount", function() {
	    this.initOriginalMetadata(this.question);
	    this.question.initializeView((anno) => new Crowdsourcing.Annotation.Metadata(anno, this.originalData), this.update, this.update, this.focusAnnotation);
		    this.opts.item.onImageOpen(function() {
		        switch(this.question.targetSelector) {
		            case Crowdsourcing.Question.Selector.WHOLE_PAGE:
		            case Crowdsourcing.Question.Selector.WHOLE_SOURCE:
		                if(this.question.annotations.length == 0 && !this.question.item.isReviewMode()) {
		                    this.question.addAnnotation();

		                    this.opts.item.dirty = false;
		                }
		        }
		        this.update();
		    }.bind(this));
	    Crowdsourcing.translator.addTranslations(this.question.metadataFields)
	    .then(() => this.update());
	});

	this.initOriginalMetadata = function(question) {
        this.metadataFields = question.metadataFields;
        let allMetadata = question.item.metadata;
        this.originalData = {};
        this.metadataFields.forEach(field => {
            let value = allMetadata[field];
            if(value) {
                this.originalData[field] = value;
            }
        })
	}.bind(this)

    this.hasOriginalValue = function(field) {
        return this.originalData[field] != undefined;
    }.bind(this)

    this.getOriginalValue = function(field) {
	    let value =this.originalData[field];
	    if(!value) {
	        return "";
	    } else if(Array.isArray(value)) {
	        return value.join("; ");
	    } else {
	        return value;
	    }
    }.bind(this)

	this.focusAnnotation = function(index) {
	    let id = "question_" + this.opts.index + "_annotation_" + index + "_0";
	    let inputSelector = "#" + id;
	    this.refs.input_0.focus();
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

	this.setValueFromEvent = function(event) {
        event.preventUpdate = true;
        let annoIndex = event.target.dataset.annotationindex;
        let anno = this.question.annotations[annoIndex];
        let field = event.item.field;
        let value = event.target.value;
        if(anno && field) {
            anno.setValue(field, value);
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

riot.tag2('plaintextquestion', '<div if="{this.showInstructions()}" class="crowdsourcing-annotations__instruction"><label>{Crowdsourcing.translate(⁗crowdsourcing__help__create_rect_on_image⁗)}</label></div><div if="{this.showInactiveInstructions()}" class="crowdsourcing-annotations__single-instruction -inactive"><label>{Crowdsourcing.translate(⁗crowdsourcing__help__make_active⁗)}</label></div><div class="crowdsourcing-annotations__wrapper" id="question_{opts.index}_annotation_{index}" each="{anno, index in this.question.annotations}"><div class="crowdsourcing-annotations__annotation-area"><div if="{this.showAnnotationImages()}" class="crowdsourcing-annotations__annotation-area-image" riot-style="border-color: {anno.getColor()}"><img riot-src="{this.question.getImage(anno)}"></img></div><div class="crowdsourcing-annotations__question-text-input"><textarea disabled="{this.opts.item.isReviewMode() ? \'disabled\' : \'\'}" onchange="{setTextFromEvent}" riot-value="{anno.getText()}"></textarea></div></div><div class="cms-module__actions crowdsourcing-annotations__annotation-action"><button if="{!this.opts.item.isReviewMode()}" onclick="{deleteAnnotationFromEvent}" class="crowdsourcing-annotations__delete-annotation btn btn--clean delete">{Crowdsourcing.translate(⁗action__delete_annotation⁗)} </button></div></div><button if="{showAddAnnotationButton()}" onclick="{addAnnotation}" class="options-wrapper__option btn btn--default" id="add-annotation">{Crowdsourcing.translate(⁗action__add_annotation⁗)}</button>', '', '', function(opts) {

	this.question = this.opts.question;

	this.on("mount", function() {
	    this.question.initializeView((anno) => new Crowdsourcing.Annotation.Plaintext(anno), this.update, this.update, this.focusAnnotation);
	    this.opts.item.onImageOpen(function() {
	        switch(this.question.targetSelector) {
	            case Crowdsourcing.Question.Selector.WHOLE_PAGE:
	            case Crowdsourcing.Question.Selector.WHOLE_SOURCE:
	                if(this.question.annotations.length == 0 && !this.question.item.isReviewMode()) {
	                    this.question.addAnnotation();

	                    this.opts.item.dirty = false;
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


riot.tag2('richtextquestion', '<div if="{this.showInstructions()}" class="annotation_instruction"><label>{Crowdsourcing.translate(⁗crowdsourcing__help__create_rect_on_image⁗)}</label></div><div if="{this.showInactiveInstructions()}" class="crowdsourcing-annotations__single-instruction -inactive"><label>{Crowdsourcing.translate(⁗crowdsourcing__help__make_active⁗)}</label></div><div class="crowdsourcing-annotations__wrapper" id="question_{opts.index}_annotation_{index}" each="{anno, index in this.question.annotations}"><div class="crowdsourcing-annotations__annotation-area"><div if="{this.showAnnotationImages()}" class="crowdsourcing-annotations__annotation-area-image" riot-style="border-color: {anno.getColor()}"><img riot-src="{this.question.getImage(anno)}"></img></div><div class="crowdsourcing-annotations__question-text-input"><textarea class="tinyMCE" disabled="{this.opts.item.isReviewMode() ? \'disabled\' : \'\'}" onchange="{setTextFromEvent}" riot-value="{anno.getText()}"></textarea></div></div><div class="cms-module__actions crowdsourcing-annotations__annotation-action"><button if="{!this.opts.item.isReviewMode()}" onclick="{deleteAnnotationFromEvent}" class="annotation_area__button btn btn--clean delete">{Crowdsourcing.translate(⁗action__delete_annotation⁗)} </button></div></div><button if="{showAddAnnotationButton()}" onclick="{addAnnotation}" class="options-wrapper__option btn btn--default" id="add-annotation">{Crowdsourcing.translate(⁗action__add_annotation⁗)}</button>', '', '', function(opts) {

	this.question = this.opts.question;

	this.on("mount", function() {
	    this.question.initializeView((anno) => new Crowdsourcing.Annotation.Richtext(anno), this.update, this.update, this.focusAnnotation);

	    this.opts.item.onImageOpen(function() {
	        switch(this.question.targetSelector) {
	            case Crowdsourcing.Question.Selector.WHOLE_PAGE:
	            case Crowdsourcing.Question.Selector.WHOLE_SOURCE:
	                if(this.question.annotations.length == 0 && !this.question.item.isReviewMode()) {
	                    this.question.addAnnotation();

	                    this.opts.item.dirty = false;
	                }
	        }
	        this.update();
	    }.bind(this));
	});

	this.on("updated", function(e) {
	    this.initTinyMce();
	});

	this.initTinyMce = function() {
	    if($(".tinyMCE").length) {
		    let config = viewerJS.tinyMce.getConfig({
		        language: Crowdsourcing.language,
		    	setup: (editor) => {
		    	    editor.on('change', (e) => {
		    	        editor.save();
		    	        editor.targetElm.dispatchEvent(new Event('change'));
		    	    });
		    	}
		    });
		    if(this.opts.item.isReviewMode()) {
		        config.readonly = 1;
		    }
	  	    tinymce.init( config );
	    }
	}.bind(this)

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



riot.tag2('external-resource-download', '<div class="download-external-resource__list"><div class="download-external-resource__item" each="{url in urls}"><div class="download-external-resource__error_wrapper {isError(url) ? \'-active\' : \'\'}"><i class="fa fa-exclamation-triangle"></i><label class="download-external-resource__error">{getErrorMessage(url)}</label></div><div class="download-external-resource__inner-wrapper {isFinished(url) ? \'\' : \'-active\'}"><span class="download-external-resource__label">{url}</span><div class="download-external-resource__button-wrapper"><button class="download-external-resource__order download-external-resource__button btn btn--full {isRequested(url)|isError(url)|isFinished(url) ? \'\' : \'-active\'}" onclick="{startDownloadTask}">{msg.downloadButton}</button></div><div class="download-external-resource__waiting_animation {isWaiting(url) ? \'-active\' : \'\'}"><img riot-src="{preloader}" class="img-responsive" alt="{msg.action__external_files__download_in_queue}" title="{msg.action__external_files__download_in_queue}"></div><div class="download-external-resource__loading_animation {isDownloading(url) ? \'-active\' : \'\'}"><progress riot-value="{getDownloadProgress(url)}" max="{getDownloadSize(url)}" title="{getDownloadProgressLabel(url)}">{getDownloadProgressLabel(url)}</progress></div></div><div class="download-external-resource__results {isFinished(url) ? \'-active\' : \'\'}"><virtual each="{object in getFiles(url)}"><div class="born-digital__items-wrapper"><div class="born-digital__head-mobile"><span>{msg.label__born_digital__filename}</span></div><div class="born-digital__item"><span>{object.path}</span></div><div class="born-digital__head-mobile"><span>{msg.label__born_digital__filedescription}</span></div><div class="born-digital__item"><span>{object.description}</span></div><div class="born-digital__head-mobile"><span>{msg.label__born_digital__filesize}</span></div><div class="born-digital__item"><span>{object.size}</span></div><div class="born-digital__head-mobile"><span>{msg.label__born_digital__fileformat}</span></div><div class="born-digital__item"><span>{msg[object.mimeType]}</span></div><div class="born-digital__item-download-last"><a class="born-digital__item__download btn btn--full" href="{object.url}" target="_blank">{msg.action__born_digital__download}</a></div></div></virtual></div></div></div>', '', '', function(opts) {
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

      this.initWebSocket = function() {

        const socket = new viewerJS.WebSocket(window.location.host, this.contextPath, viewerJS.WebSocket.PATH_DOWNLOAD_TASK);
        console.log("created web socket ", socket.socket.url);
        socket.onMessage.subscribe( (event) => {
          this.handleUpdateMessage(event);
          this.update();
        });
        return socket;
      }.bind(this)

      this.sendMessage = function(message) {

    	  if(typeof message != "string") {
    		message = JSON.stringify(message);
    	  }
    	  this.ws.sendMessage(message);
    	  return new Promise((resolve, reject) => {
    		 rxjs.merge(this.ws.onMessage, this.ws.onError).pipe(rxjs.operators.first()).subscribe(e => resolve(e));
    	  });
      }.bind(this)

      this.startDownloadTask = function(e) {

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
      }.bind(this)

      this.handleUpdateMessage = function(event) {
    	  let data = this.parseSocketMessage(event.data);
          if(data == null) {
        	  this.handleError("Not a valid message object: " + event.data);
          } else if(data.pi == this.pi && data.url && data.status) {

        	  switch(data.status) {
        	  case "waiting":
        	  case "processing":
	        	  this.downloads.set(data.url, data);
	        	  if(!this.updateListeners.has(data.url)) {

	        		const listener = viewerJS.helper.repeatPromise(() => this.sendMessage(this.createSocketMessage(this.pi, data.url, "update")), this.updateDelay);
			        this.updateListeners.set(data.url, listener);
			        listener.then(() => {});

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
        	  case "dormant":
        	  }

          } else {
        	  this.handleError("Wrong or insufficient data in message object: " + event.data);
        	  this.updateListeners.forEach(value => value.cancel());
        	  this.updateListeners = new Map();
          }
      }.bind(this)

      this.cancelListener = function(url) {
    	  if(this.updateListeners.has(url)) {
      	  	this.updateListeners.get(url).cancel();
	  		this.updateListeners.delete(url);
      	  }
      }.bind(this)

      this.handleError = function(message) {
    	  alert(message);
      }.bind(this)

      this.isRequested = function(url) {
    	  return this.downloads.has(url) && this.downloads.get(url).status !== 'dormant';
      }.bind(this)

      this.isDownloading = function(url) {
    	return this.downloads.get(url)?.status == 'processing';
      }.bind(this)

      this.isWaiting = function(url) {
    	  return this.downloads.get(url)?.status == 'waiting';
      }.bind(this)

      this.isFinished = function(url) {
    	  return this.downloads.get(url)?.status == 'complete';
      }.bind(this)

      this.getDownloadProgress = function(url) {
   	  	if(this.getDownloadSize(url) <= 0 || isNaN(this.getDownloadSize(url))) {
   	  		return undefined;
   	  	}
    	return this.downloads.get(url)?.progress;
      }.bind(this)

      this.getDownloadProgressLabel = function(url) {
    	  let fraction = this.getDownloadProgress(url)/this.getDownloadSize(url);
    	  if(isNaN(fraction) || fraction < 0) {
    		  console.log("title: ", this.msg.label__born_digital__downloading)
    		  return this.msg.label__born_digital__downloading;
    	  } else {
    		  return this.msg.label__born_digital__downloading + ": " + (fraction * 100) + "%";
    	  }
      }.bind(this)

      this.getDownloadSize = function(url) {
    	  return this.downloads.get(url)?.resourceSize;
      }.bind(this)

      this.getFiles = function(url) {
    	  console.log("get files ", url, this.downloads.get(url));
    	  return this.downloads.get(url)?.files;
      }.bind(this)

      this.isError = function(url) {
    	  return this.downloads.get(url)?.status == "error";
      }.bind(this)

      this.getErrorMessage = function(url) {
    	  return this.downloads.get(url)?.errorMessage;
      }.bind(this)

      this.cancelDownload = function(e) {
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
      }.bind(this)

      this.parseSocketMessage = function(jsonString) {
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
      }.bind(this)

      this.createSocketMessage = function(pi, url, action) {
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
riot.tag2('fsthumbnails', '<div class="fullscreen__view-image-thumbs" ref="thumbnailWrapper"><div each="{thumbnail in thumbnails}" class="fullscreen__view-image-thumb"><figure class="fullscreen__view-image-thumb-image"><a href="{getViewerPageUrl(thumbnail)[\'@id\']}"><fsthumbnailimage thumbnail="{thumbnail}" observable="{observable}" root=".fullscreen__view-image-thumbs-wrapper" imgsrc="{thumbnail.thumbnail[\'@id\']}"></fsThumbnailImage></a><figcaption><div class="fullscreen__view-image-thumb-image-order {thumbnail.loaded ? \'in\' : \'\'}">{thumbnail.label}</div></figcaption></figure></div></div>', '', '', function(opts) {
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

        		$( this.wrapper ).outerWidth( this.thumbsWidth ).fadeToggle( 'fast' );

            	if ( this.thumbnails.length == 0 ) {

            		$.ajax( {
                        url: opts.thumbnailUrl,
                        type: "GET",
                        datatype: "JSON"
                    } ).then( function( data ) {
                    	this.thumbnails = data.canvases;
                    	this.update();
                    }.bind( this ) );
    			}
        	}.bind(this));
    	}.bind( this ) );

    	this.observable.on( 'imageLoaded', function( thumbnail ) {
    		thumbnail.loaded = true;
    		this.update();
    	}.bind( this ) );

    	this.getViewerPageUrl = function(thumbnail) {
    	    if(thumbnail.rendering) {
    	        if(Array.isArray(thumbnail.rendering)) {
    	            return thumbnail.rendering.find(render => "text/html" == render.format)
    	        } else {
    	            return thumbnail.rendering;
    	        }
    	    }
    	}.bind(this)
});
riot.tag2('featuresetfilter', '<ul if="{filters.length > 0}"><li each="{filter in filters}" class="{filter.styleClass}"><label>{filter.label}</label><div><input type="radio" name="options_{filter.field}" id="options_{filter.field}_all" value="" checked onclick="{resetFilter}"><label for="options_{filter.field}_all">{opts.msg.alle}</label></div><div each="{option, index in filter.options}"><input type="radio" name="options_{filter.field}" id="options_{filter.field}_{index}" riot-value="{option.name}" onclick="{setFilter}"><label for="options_{filter.field}_{index}">{option.name}</label></div></li></ul>', '', '', function(opts) {

this.filters = [];

this.on("mount", () => {
	this.geomap = this.opts.geomap;
	this.featureGroups = this.opts.featureGroups;
	this.filters = this.createFilters(this.opts.filters, this.featureGroups);
	this.geomap.onActiveLayerChange.subscribe(groups => {
		this.featureGroups = groups;
		this.filters = this.createFilters(this.opts.filters, this.featureGroups);
 		this.update();
	})
	this.update();
})

this.createFilters = function(filterMap, featureGroups) {
	let filters = [];
	for (const entry of filterMap.entries()) {
		let layerName = entry[0];
		let filterConfigs = entry[1];
		let groups = featureGroups.filter(g => this.getLayerName(g) == layerName);
		if(layerName && filterConfigs && filterConfigs.length > 0 && groups.length > 0) {
			filterConfigs.forEach(filterConfig => {
				let filter = {
						field: filterConfig.value,
						label: filterConfig.label,
						styleClass: filterConfig.styleClass,
						layers: groups,
						options: this.findValues(groups, filterConfig.value).map(v => {
							return {
								name: v,
								field: filterConfig.value
							}
						}),
					};
				filters.push(filter);
			});
		}
	}
	return filters.filter(filter => filter.options.length > 1);
}.bind(this)

this.getLayerName = function(layer) {
	let name = viewerJS.iiif.getValue(layer.config.label, this.opts.defaultLocale);
	return name;
}.bind(this)

this.getFilterName = function(filter) {
	let name = viewerJS.iiif.getValue(filter.label, this.opts.defaultLocale);
	return name;
}.bind(this)

this.findValues = function(featureGroups, filterField) {
	return Array.from(new Set(this.findEntities(featureGroups, filterField)
	.map(e => e[filterField]).map(a => a[0])
	.map(value => viewerJS.iiif.getValue(value, this.opts.locale, this.opts.defaultLocale)).filter(e => e)));
}.bind(this)

this.findEntities = function(featureGroups, filterField) {
	let entities = featureGroups.flatMap(group => group.markers).flatMap(m => m.feature.properties.entities).filter(e => e[filterField]);
	return entities;
}.bind(this)

this.resetFilter = function(event) {
	let filter = event.item.filter;
	filter.layers.forEach(g => g.showMarkers(entity => this.isShowMarker(entity, filter, undefined)));
}.bind(this)

this.setFilter = function(event) {
	let filter = this.getFilterForField(event.item.option.field);
	let value = event.item.option.name;
	filter.layers.forEach(g => g.showMarkers(entity => this.isShowMarker(entity, filter, value)));
}.bind(this)

this.isShowMarker = function(entity, filter, value) {
	let filters = this.filters.filter(f => f.layers.filter(g => filter.layers.includes(g)).length > 0);

	filter.selectedValue = value;
	let match = filters.map(filter => {
		if(filter.selectedValue) {
			let show = entity[filter.field] != undefined && entity[filter.field].map(v => viewerJS.iiif.getValue(v, this.opts.locale, this.opts.defaultLocale)).includes(filter.selectedValue);
			return show;
		} else {
			return true;
		}
	})
	.every(match => match);
	return match;
}.bind(this)

this.getFilterForField = function(field) {
	return this.filters.find(f => f.field == field);
}.bind(this)

});
riot.tag2('featuresetselector', '<div class="tab" if="{featureGroups.length > 1}"><button each="{featureGroup, index in featureGroups}" class="tablinks {isActive(featureGroup) ? \'-active\':\'\'}" onclick="{setFeatureGroup}">{getLabel(featureGroup)}</button></div>', '', '', function(opts) {

this.featureGroups = [];

this.on("mount", () => {
	this.featureGroups = opts.featureGroups;
	this.geomap = opts.geomap;
	this.update();
})

this.setFeatureGroup = function(event) {
	let featureGroup = event.item.featureGroup;
	this.geomap.setActiveLayers([featureGroup]);
}.bind(this)

this.getLabel = function(featureGroup) {
	return viewerJS.iiif.getValue(featureGroup.config.label, this.opts.locale, this.opts.defaultLocale);
}.bind(this)

this.isActive = function(featureGroup) {
	return featureGroup.active;
}.bind(this)

});
riot.tag2('geojsonfeaturelist', '<div class="custom-map__sidebar-inner-wrapper"><div class="custom-map__sidebar-inner-top"><h4 class="custom-map__sidebar-inner-heading"><rawhtml content="{getListLabel()}"></rawhtml></h4><input if="{getVisibleEntities().length > 0}" class="custom-map__sidebar-inner-search-input" type="text" ref="search" oninput="{filterList}"></input></div><div class="custom-map__sidebar-inner-bottom"><ul if="{getVisibleEntities().length > 0}" class="custom-map__inner-wrapper-list"><li class="custom-map__inner-wrapper-list-entry" each="{entity in getVisibleEntities()}"><a href="{getLink(entity)}"><rawhtml content="{getEntityLabel(entity)}"></rawhtml></a></li></ul></div></div>', '', 'onclick="{preventBubble}"', function(opts) {

this.entities = [];
this.filteredEntities = undefined;

this.on("mount", () => {
	this.opts.featureGroups.forEach(group => {
		group.onFeatureClick.subscribe(f => {
			this.title = f.properties?.title;
			this.setEntities(f.properties?.entities?.filter(e => e.visible !== false).filter(e => this.getEntityLabel(e)?.length > 0));
		});
	})
	this.opts.geomap.onMapClick.subscribe(e => this.hide());
	this.hide();
})

this.setEntities = function(entities) {

	this.entities = [];
	this.filteredEntities = undefined;
	if(this.refs["search"]) {
		this.refs["search"].value = "";
	}
	if(entities?.length || this.opts.showAlways) {
		this.entities = entities;
		this.show();
		this.update();
	}
}.bind(this)

this.getVisibleEntities = function() {
	if(!this.entities) {
		return [];
	} else if(this.filteredEntities === undefined) {
		return this.entities;
	} else {
		return this.filteredEntities;
	}
}.bind(this)

this.preventBubble = function(e) {
	event.stopPropagation();
}.bind(this)

this.filterList = function(e) {
	let filter = e.target.value;
	if(filter) {
		this.filteredEntities = this.entities.filter(e => this.getLabel(e).toLowerCase().includes(filter.toLowerCase() ));
	} else {
		this.filteredEntities = undefined;
	}
}.bind(this)

this.getEntityLabel = function(entity) {
	if(entity) {
		return this.getLabel(entity);
	}
}.bind(this)

this.getListLabel = function() {
	if(this.title) {
		let label = viewerJS.iiif.getValue(this.title, this.opts.locale, this.opts.defaulLocale);
		return label;
	}
}.bind(this)

this.getLink = function(entity) {
	if(entity) {
		let labels = this.opts.entityLinkFormat;
		label = labels.map(format => {
			let groups = [...format.matchAll(/\${(.*?)}/g)];
			let l = "";
			groups.forEach(group => {
				if(group.length > 1) {
					let value = entity[group[1]]?.map(s => viewerJS.iiif.getValue(s, this.opts.locale, this.opts.defaultLocale)).join(", ");
					if(value) {
						l += format.replaceAll(group[0], value ? value : "");
					}
				}
			})
			return l;
		}).join("");
		return label;

	}
}.bind(this)

this.getLabel = function(entity) {

	if(entity.title) {
		let label = viewerJS.iiif.getValue(entity.title, this.opts.locale, this.opts.defaulLocale);
		return label;
	} else {
		return "";
	}

}.bind(this)

this.hide = function() {
	this.root.style.display = "none";
}.bind(this)

this.show = function() {
	this.root.style.display = "block";
}.bind(this)

});
riot.tag2('geomapsearch', '<yield><div class="geo-map__wrapper"><div ref="geocoder" class="geocoder"></div><div class="geo-map__buttons-wrapper"></div><div ref="map" class="geo-map"></div></div>', '', '', function(opts) {

this.on("mount", function() {

	this.geoMap = this.initMap();
	this.drawLayer = this.initDrawLayer(this.geoMap);
    if(this.opts.area) {
    	this.initArea(this.drawLayer, this.opts.area);
    }
	if(!this.opts.inactive) {
	    this.initGeocoder(this.geoMap);
	    this.drawnItems = this.initMapDraw(this.geoMap, this.drawLayer);
	}
 	this.hitsLayer = this.initHitsLayer(this.geoMap);
	if(this.opts.toggleFeatures) {
		this.initToggleLayer(this.geoMap, this.hitsLayer, this.opts.toggleFeatures);
	}
	if(this.opts.heatmap?.enabled) {
		this.heatmap = this.initHeatmap(this.hitsLayer)
	}
});

this.initMap = function() {

    let geoMap = new viewerJS.GeoMap({
        element : this.refs.map,
        language: viewerJS.translator.language,
        fixed: this.opts.inactive ? true : false,
        layer: this.opts.hitsLayer
    })
    let initialView = {
        zoom: 5,
        center: [11.073397, 49.451993]
    };
    geoMap.init(initialView, this.opts.features);
    return geoMap;
}.bind(this)

this.initDrawLayer = function(map) {
    let drawLayer = new viewerJS.GeoMap.featureGroup(map, {
   	    style : this.opts.areaLayer.style
    });
	return drawLayer;
}.bind(this)

this.initGeocoder = function(map) {
	let geocoderConfig = {};
	if(this.opts.search_placeholder) {
		geocoderConfig.placeholder = this.opts.search_placeholder
	}
	if(this.opts.search_enabled) {
   		map.initGeocoder(this.refs.geocoder, geocoderConfig);
	}
}.bind(this)

this.initToggleLayer = function(geoMap, layer, button) {
	let ToggleFeaturesControl = L.Control.extend({
	    options: {
	        position: "topleft"
	    },
	    onAdd: function(map) {
	        L.DomEvent.on(button, "dblclick" , (e) => {
	            L.DomEvent.stopPropagation(e);
	            e.stopPropagation();
	            return false;
	        });
	        L.DomEvent.on(button, "click" , (e) => {
	            layer.setVisible(!layer.isVisible());
	            L.DomEvent.stopPropagation(e);
	            e.stopPropagation();
	            return false;
	        });
	        return button;
	    }.bind(this),
	    onRemove: function(map) {

	    }
	})
	let control = new ToggleFeaturesControl();
	geoMap.map.addControl(control);
}.bind(this)

this.initArea = function(layer, shape) {
	if(viewerJS.isString(shape)) {
        try {
        	shape = JSON.parse(shape);
        } catch(e) {
            console.error("Unable to draw geomap area ", this.opts.area, ": cannot parse json");
        }
    }

    let feature = undefined;
    switch(shape.type) {
        case "polygon":
            feature = layer.drawPolygon(shape.vertices, true);
            break;
        case "circle":
            feature = layer.drawCircle(shape.center, shape.radius, true);
            break;
        case "rectangle":
            feature = layer.drawRectangle([shape.vertices[0], shape.vertices[2]], true);
            break;
    }
    this.onLayerDrawn({layer: feature});
}.bind(this)

this.initMapDraw = function(geomap, drawLayer) {

    let drawnItems = new L.FeatureGroup();

    geomap.map.addLayer(drawnItems);
    let drawControl = new L.Control.Draw({
        edit: {
            featureGroup: drawnItems,
            edit: false,
            remove: false
        },
        draw: {
            polyline: false,
            marker: false,
            circlemarker: false
        }
    });
    drawControl.setDrawingOptions({
        rectangle: {
        	shapeOptions: drawLayer.config.style
        },
        circle: {
        	shapeOptions: drawLayer.config.style
        },
        polygon: {
        	shapeOptions: drawLayer.config.style
        }
    });

    geomap.map.addControl(drawControl);

    let edited = new rxjs.Subject();
    edited.pipe(rxjs.operators.debounceTime(300)).subscribe(e => this.onLayerEdited(e));
    geomap.map.on(L.Draw.Event.EDITMOVE, e => edited.next(e));
    geomap.map.on(L.Draw.Event.EDITRESIZE, e => edited.next(e));
    geomap.map.on(L.Draw.Event.EDITVERTEX, e => edited.next(e));

    let deleted = new rxjs.Subject();
    deleted.subscribe(e => this.onLayerDeleted(e));
    geomap.map.on(L.Draw.Event.DELETED, e => deleted.next(e));
    geomap.map.on(L.Draw.Event.DRAWSTART, e => deleted.next(e));

    geomap.map.on(L.Draw.Event.CREATED, (e) => this.onLayerDrawn(e));

    if(this.opts.reset_button) {
        $(this.opts.reset_button).on("click",  e => deleted.next(e));
    }
    return drawnItems;
}.bind(this)

this.onLayerDeleted = function(e) {
    if(this.searchLayer) {
        if(this.drawnItems) {
        	this.drawnItems.removeLayer(this.searchLayer);
        }
        this.searchLayer = undefined;
    }
    this.notifyFeatureSet(undefined);
}.bind(this)

this.onLayerEdited = function(e) {

    if(e.layer) {
    	this.searchLayer = e.layer;
    } else if(e.poly) {
        this.searchLayer = e.poly;
    } else {
        logger.warn("Called layer edited event with no given layer ", e);
        return;
    }
	this.setSearchArea(this.searchLayer);
}.bind(this)

this.onLayerDrawn = function(e) {
    if(e.layer) {
	    this.searchLayer = e.layer;
		if(this.drawnItems) {
	    	this.drawnItems.addLayer(e.layer);
		}
		this.searchLayer.editing.enable();
		this.setSearchArea(this.searchLayer);
    }
}.bind(this)

this.setSearchArea = function(layer) {

    let type = this.getType(layer);
    switch(type) {
        case "polygon":
        case "rectangle":
            let origLayer = L.polygon(layer.getLatLngs());
            let wrappedCenter = this.geoMap.map.wrapLatLng(layer.getCenter());
            let distance = layer.getCenter().lng - wrappedCenter.lng;

	        let vertices = [...layer.getLatLngs()[0]].map(p => L.latLng(p.lat, p.lng-distance)).map(p => this.geoMap.normalizePoint(p));

	        if(vertices[0] != vertices[vertices.length-1]) {
	        	vertices.push(vertices[0]);
	        }

	        this.notifyFeatureSet({
	           type : type,
	           vertices: vertices.map(p => [p.lat, p.lng])
	        })
	        break;
        case "circle":
            let bounds = this.geoMap.map.wrapLatLngBounds(layer.getBounds());
            let center = this.geoMap.map.wrapLatLng(layer.getLatLng());
            let circumgon = this.createCircumgon(bounds.getCenter(), bounds.getSouthWest(), bounds.getSouthWest(), 16);
            let diameterM = bounds.getSouthWest().distanceTo(bounds.getNorthWest());
            this.notifyFeatureSet({
                type : "circle",
                vertices: circumgon.map(p => this.geoMap.normalizePoint(p)).map(p => [p.lat, p.lng]),
                center: center,
            	radius: layer.getRadius()
            })
            break;
    }
}.bind(this)

this.createCircumgon = function(center, sw, ne, numVertices) {

    let lSW = this.geoMap.map.latLngToLayerPoint(sw);
    let lNE = this.geoMap.map.latLngToLayerPoint(ne);
    let lCenter = this.geoMap.map.latLngToLayerPoint(center);

    let radius = Math.abs(lCenter.x - lSW.x);

    let points = [];
    for(let i = 0; i < numVertices; i++) {
        let x = lCenter.x + radius *  Math.cos(2*Math.PI*i/numVertices);
        let y = lCenter.y + radius *  Math.sin(2*Math.PI*i/numVertices);
        points.push([x,y]);
    }
    points.push(points[0]);
    let geoPoints = points.map(p => this.geoMap.map.layerPointToLatLng(p));
    return geoPoints;
}.bind(this)

this.notifyFeatureSet = function(feature) {

    if(this.opts.onFeatureSelect) {
        this.opts.onFeatureSelect(feature);
    }

}.bind(this)

this.buildSearchString = function(vertices) {
    let string = "WKT_COORDS:\"IsWithin(POLYGON((";
    string += vertices.map(v => v[1] + " " + v[0]).join(", ");
    string += "))) distErrPct=0\"";
    return string;
}.bind(this)

this.getType = function(layer) {
    if(layer.getRadius) {
        return "circle";
    } else if(layer.setBounds) {
        return "rectangle";
    } else if(layer.getLatLngs) {
        return "polygon"
    } else {
        throw "Unknown layer type: " + layer;
    }
}.bind(this)

this.initHitsLayer = function(map) {
    this.opts.hitsLayer.language = viewerJS.translator.language;
	let hitsLayer = new viewerJS.GeoMap.featureGroup(map, this.opts.hitsLayer);
	map.layers.push(hitsLayer);

	hitsLayer.init(this.opts.features, false);
	hitsLayer.onFeatureClick.subscribe(f => {
		if(f.properties && f.properties.link) {
			$(this.opts.search?.loader).show();
			window.location.assign(f.properties.link);
		}
	})

	return hitsLayer;
}.bind(this)

this.initHeatmap = function(hitsLayer) {
	let heatmapQuery = this.opts.heatmap.mainQuery;
	let heatmapFacetQuery = this.opts.heatmap.facetQuery;

	let heatmap = L.solrHeatmap(this.opts.heatmap.heatmapUrl, this.opts.heatmap.featureUrl, hitsLayer, {
		field: "WKT_COORDS",
		type: "clusters",
		filterQuery: heatmapQuery,
		facetQuery: heatmapFacetQuery,
		labelField: this.opts.heatmap.labelField,
		queryAdapter: "goobiViewer"
	});
	heatmap.addTo(this.geoMap.map);
	return heatmap;
}.bind(this)

});
riot.tag2('imagefilters', '<div class="imagefilters__filter-list"><div class="imagefilters__filter" each="{filter in filters}"><span class="imagefilters__label {filter.config.slider ? \'\' : \'imagefilters__label-long\'}">{filter.config.label}</span><input disabled="{filter.disabled ? \'disabled=\' : \'\'}" class="imagefilters__checkbox" if="{filter.config.checkbox}" type="checkbox" onchange="{apply}" checked="{filter.isActive() ? \'checked\' : \'\'}" aria-label="{filter.config.label}"><input disabled="{filter.disabled ? \'disabled=\' : \'\'}" class="imagefilters__slider" title="{filter.getValue()}" if="{filter.config.slider}" type="range" oninput="{apply}" riot-value="{filter.getValue()}" min="{filter.config.min}" max="{filter.config.max}" step="{filter.config.step}" orient="horizontal" aria-label="{filter.config.label}: {filter.getValue()}"></div></div><div class="imagefilters__options"><button type="button" class="btn btn--full" onclick="{resetAll}">{this.config.messages.clearAll}</button></div>', '', '', function(opts) {

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

riot.tag2('metadataeditor', '<div if="{this.metadataList}"><h2>Pin content</h2><div class="admin__language-tabs"><ul class="nav nav-tabs"><li each="{language, index in this.opts.languages}" class="admin__language-tab {language == this.currentLanguage ? \'active\' : \'\'}"><a onclick="{this.setCurrentLanguage}">{language}</a></li></ul></div><div class="cms__geomap__featureset_panel "><div class="active"><div class="input_form"><div each="{metadata, index in this.metadataList}" class="input_form__option_group"><div class="input_form__option_label"><label for="input-{metadata.property}">{metadata.label}:</label></div><div class="input_form__option_marker {metadata.required ? \'in\' : \'\'}"><label>*</label></div><div class="input_form__option_control"><input tabindex="{index+1}" disabled="{this.isEditable(metadata) ? \'\' : \'disabled\'}" ref="input" if="{metadata.type != \'longtext\'}" type="{metadata.type}" id="input-{metadata.property}" class="form-control" riot-value="{getValue(metadata)}" oninput="{this.updateMetadata}"><textarea tabindex="{index+1}" disabled="{this.isEditable(metadata) ? \'\' : \'disabled\'}" ref="input" if="{metadata.type == \'longtext\'}" id="input-{metadata.property}" class="form-control" riot-value="{getValue(metadata)}" oninput="{this.updateMetadata}"></textarea></div><div if="{metadata.helptext}" class="input_form__option_help"><button type="button" class="btn btn--clean" data-toggle="helptext" for="help_{metadata.property}"><i class="fa fa-question-circle" aria-hidden="true"></i></button></div><div if="{metadata.helptext}" id="help_{metadata.property}" class="input_form__option_control_helptext">{metadata.helptext}</div></div><div class="admin__geomap-edit-delete-wrapper"><a if="{this.opts.deleteListener}" disabled="{this.mayDelete() ? \'\' : \'disabled\'}" class="btn btn--clean -redlink" onclick="{this.notifyDelete}">{this.opts.deleteLabel}</a></div></div></div></div></div>', '', '', function(opts) {

 	this.on("mount", () => {
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



riot.tag2('modal', '<div class="modal fade {modalClass}" id="{modalId}" tabindex="-1" ref="modal" role="dialog" aria-labelledby="{modalTitle}" aria-hidden="true"><div class="modal-dialog modal-dialog-centered" role="document"><div class="modal-content"><div class="modal-header"><h1 class="modal-title">{modalTitle}</h1><button class="fancy-close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">x</span></button></div><div class="modal-body"><yield from="body"></yield></div><div class="modal-right"><yield from="right"></yield></div><div class="modal-footer"><yield from="footer"></yield></div></div></div><div class="alt-backdrop"></div></div>', '', '', function(opts) {

    this.modalClass = this.opts.styleclass ? this.opts.styleclass : "";
    this.modalId = this.opts.modalid;
    this.modalTitle = this.opts.title;

	this.on("mount", () => {

	    if(this.opts.onClose) {
	        $(this.refs.modal).on('hide.bs.modal', () => this.opts.onClose());
	    }
	});

});
riot.tag2('pdfdocument', '<div class="pdf-container"><pdfpage each="{page, index in pages}" page="{page}" pageno="{index+1}"></pdfPage></div>', '', '', function(opts) {

		this.pages = [];

		var loadingTask = pdfjsLib.getDocument( this.opts.data );
	    loadingTask.promise.then( ( pdf ) => {
	        var pageLoadingTasks = [];
	        for(var pageNo = 1; pageNo <= pdf.numPages; pageNo++) {
   		        var page = pdf.getPage(pageNo);
   		        pageLoadingTasks.push(page);
   		    }
   		    return Promise.allSettled(pageLoadingTasks);
	    })
	    .then( (results) => {
			results.forEach(result => {
			    if (result.status === "fulfilled") {
                	var page = result.value;
                	this.pages.push(page);
                } else {
                    console.log("Error loading page: ", result);
                }
			});
			this.update();
        })
	    .then( () => {
			$(".pdf-container").show();
			console.log("loader", this.opts.loaderSelector);
            $(this.opts.loaderSelector).hide();
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


riot.tag2('rawhtml', '', '', '', function(opts) {
  this.on("mount", () => {
	    this.root.innerHTML = opts.content;
	  })
  this.on("updated", () => {
    this.root.innerHTML = opts.content;
  })
});
riot.tag2('slide_default', '<a class="swiper-link slider-{this.opts.stylename}__link" href="{this.opts.link}" target="{this.opts.link_target}" rel="noopener"><div class="swiper-heading slider-{this.opts.stylename}__header">{this.opts.label}</div><div class="swiper-image slider-{this.opts.stylename}__image" riot-style="background-image: url({this.opts.image})"></div><div class="swiper-description slider-{this.opts.stylename}__description" ref="description"></div></a>', '', '', function(opts) {
		this.on("mount", () => {
			this.refs.description.innerHTML = this.opts.description
		});
});
riot.tag2('slide_indexslider', '<a class="slider-{this.opts.stylename}__link-wrapper" href="{this.opts.link}"><div class="swiper-heading slider-mnha__header">{this.opts.label}</div><img class="slider-{this.opts.stylename}__image" loading="lazy" riot-src="{this.opts.image}"><div class="swiper-lazy-preloader"></div></a>', '', '', function(opts) {
});
riot.tag2('slide_stories', '<div class="slider-{this.opts.stylename}__image" riot-style="background-image: url({this.opts.image})"></div><a class="slider-{this.opts.stylename}__info-link" href="{this.opts.link}"><div class="slider-{this.opts.stylename}__info-symbol"><svg width="6" height="13" viewbox="0 0 6 13" fill="none" xmlns="http://www.w3.org/2000/svg"><path fill-rule="evenodd" clip-rule="evenodd" d="M4.664 1.21C4.664 2.134 4.092 2.728 3.168 2.728C2.354 2.728 1.936 2.134 1.936 1.474C1.936 0.506 2.706 0 3.454 0C4.136 0 4.664 0.506 4.664 1.21ZM5.258 11.528C4.664 12.1 3.586 12.584 2.42 12.716C1.386 12.496 0.748 11.792 0.748 10.78C0.748 10.362 0.836 9.658 1.1 8.58C1.276 7.81 1.452 6.534 1.452 5.852C1.452 5.588 1.43 5.302 1.408 5.236C1.144 5.17 0.726 5.104 0.198 5.104L0 4.488C0.572 4.07 1.716 3.718 2.398 3.718C3.542 3.718 4.202 4.312 4.202 5.566C4.202 6.248 4.026 7.194 3.828 8.118C3.542 9.328 3.432 10.12 3.432 10.472C3.432 10.802 3.454 11.022 3.542 11.154C3.96 11.066 4.4 10.868 4.928 10.56L5.258 11.528Z" fill="white"></path></svg></div><div class="slider-single-story__info-phrase">{this.opts.label}</div></a>', '', '', function(opts) {
});


riot.tag2('slider', '<div ref="container" class="swiper-container slider-{this.styleName}__container"><div class="swiper-wrapper slider-{this.styleName}__wrapper"><div each="{slide, index in slides}" class="swiper-slide slider-{this.styleName}__slide" ref="slide_{index}"></div></div><div if="{this.showPaginator}" ref="paginator" class="swiper-pagination slider-{this.styleName}__dots"></div></div>', '', '', function(opts) {


	this.showPaginator = true;

    this.on( 'mount', function() {
		this.style = this.opts.styles.get(this.opts.style);

		this.amendStyle(this.style);
		this.styleName = this.opts.styles.getStyleNameOrDefault(this.opts.style);

		this.timeout = this.style.timeout ? this.style.timeout : 100000;
		this.maxSlides = this.style.maxSlides ? this.style.maxSlides : 1000;
		this.linkTarget = this.opts.linktarget ? this.opts.linktarget : "_self";

    	let pSource;
    	if(this.opts.sourceelement) {
    		let sourceElement = document.getElementById(this.opts.sourceelement);
    		if(sourceElement) {
    			pSource = Promise.resolve(JSON.parse(sourceElement.textContent));

    		} else {
    			logger.error("sourceElement was included but no matching dom element found");
    			return;
    		}
    	}  else {
    		pSource = fetch(this.opts.source)
        	.then(result => result.json());
    	}
    	rxjs.from(pSource)
    	.pipe(
    		rxjs.operators.flatMap(source => source),
    		rxjs.operators.flatMap(uri => fetch(uri), undefined, 5),
    		rxjs.operators.filter(result => result.status == 200),
    		rxjs.operators.takeUntil(rxjs.timer(this.timeout)),
    		rxjs.operators.flatMap(result => result.json()),
    		rxjs.operators.map(element => this.createSlide(element)),
    		rxjs.operators.filter(element => element != undefined),
    		rxjs.operators.take(this.maxSlides),
    		rxjs.operators.reduce((res, item) => res.concat(item), []),
    		rxjs.operators.map(array => array.sort( (s1,s2) => s1.order-s2.order ))
    	)
    	.subscribe(slides => this.setSlides(slides))
    });

    this.on( 'updated', function() {

    	if(this.slides && this.slides.length > 0) {
    		if(this.slider) {
    			this.slider.destroy();
    		}
			this.initSlideTags(this.slides);
    		this.swiper = new Swiper(this.refs.container, this.style.swiperConfig);
    	}

    	if (this.style.onUpdate) {
    		this.style.onUpdate();
    	}

    });

    this.setSlides = function(slides) {

    	this.slides = slides;
    	this.update();
    }.bind(this)

    this.initSlideTags = function(slides) {
    	slides.forEach( (slide, index) => {
    		let tagElement = this.refs["slide_" + index];

    		riot.mount(tagElement, "slide_" + this.getLayout(),  {
    			stylename: this.styleName,
   				link: this.getLink(slide),
   				link_target: this.linkTarget,
   				image: this.getImage(slide),
   				label: this.translate(slide.label),
   				description: this.translate(slide.description),
    		});
    	});
    }.bind(this)

	this.getElements = function(source) {
		if(viewerJS.iiif.isCollection(source)) {
			return source.members.filter(member => viewerJS.iiif.isCollection(member));
		} else {
			console.error("Cannot get slides from ", source);
		}
	}.bind(this)

    this.createSlide = function(element) {

    	if(viewerJS.iiif.isCollection(element) || viewerJS.iiif.isManifest(element)) {
    		let slide = {
    				label : element.label,
    				description : element.description,
    				image : element.thumbnail,
    				link : viewerJS.iiif.getId(viewerJS.iiif.getViewerPage(element)),
    				order : element.order
    		}
    		return slide;
    	} else {
    		return element;
    	}
    }.bind(this)

    this.translate = function(text) {
    	let translation =  viewerJS.iiif.getValue(text, this.opts.language, this.opts.defaultlanguage);
    	if(!translation) {
    			translation = viewerJS.getMetadataValue(text, this.opts.language, this.opts.defaultlanguage);
    	}
    	return translation;
    }.bind(this)

    this.getImage = function(slide) {
    	let image = slide.image;
    	if(image == undefined) {
    		return undefined;
    	} else if(viewerJS.isString(image)) {
    		return image;
    	} else if(image.service && (this.style.imageWidth || this.style.imageHeight)) {
    		let url = viewerJS.iiif.getId(image.service) + "/full/" + this.getIIIFSize(this.style.imageWidth, this.style.imageHeight) + "/0/default.jpg"
    		return url;
    	} else if(image["@id"]) {
    		return image["@id"]
    	} else {
    		return image.id;
    	}
    }.bind(this)

    this.getIIIFSize = function(width, height) {
    	if(width && height) {
    		return "!" + width + "," + height;
    	} else if(width) {
    		return width + ",";
    	} else if(height) {
    		return "," + height;
    	} else {
    		return "max";
    	}
    }.bind(this)

    this.getLink = function(slide) {
    	if(this.linkTarget == 'none') {
    		return "";
    	} else {
    		return slide.link;
    	}
    }.bind(this)

    this.amendStyle = function(styleConfig) {
    	let swiperConfig = styleConfig.swiperConfig;
    	if(swiperConfig.pagination && !swiperConfig.pagination.el)  {
    		swiperConfig.pagination.el = this.refs.paginator;
    		this.showPaginator = true;
    	} else {
    		this.showPaginator = false;
    	}
    }.bind(this)

    this.getLayout = function() {
    	let layout = this.style.layout ? this.style.layout : 'default';
    	return layout;
    }.bind(this)

});














riot.tag2('slideshow', '<a if="{manifest === undefined}" data-linkid="{opts.pis}"></a><figure class="slideshow" if="{manifest !== undefined}" onmouseenter="{mouseenter}" onmouseleave="{mouseleave}"><div class="slideshow__image"><a href="{getLink(manifest)}" class="remember-scroll-position" data-linkid="{opts.pis}" onclick="{storeScrollPosition}"><img riot-src="{getThumbnail(manifest)}" class="{\'active\' : active}" alt="{getLabel(manifest)}" onload="{setImageActive}"></a></div><figcaption><h3>{getTitleOrLabel(manifest)}</h3><p><span each="{md in metadataList}"> {getMetadataValue(manifest, md)} <br></span></p><div if="{pis.length > 1}" class="slideshow__dots"><ul><li each="{imagepi in pis}"><button class="btn btn--clean {\'active\' : pi === imagepi}" onclick="{setPi}"></button></li></ul></div></figcaption></figure>', '', '', function(opts) {

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
        			console.error("error loading ", url, ": ", error);
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
riot.tag2('subcollection', '<ul if="{collection.members && collection.members.length > 0}" class="list card-body__list"><li each="{child in getChildren(collection)}"><div class="card-body__links"><a class="card-body__collection" href="{getId(child.rendering)}">{getValue(child.label)} ({viewerJS.iiif.getContainedWorks(child)})</a><a class="card-body__rss" href="{viewerJS.iiif.getRelated(child, \'Rss feed\')[\'@id\']}" target="_blank"><i class="fa fa-rss" aria-hidden="true"></i></a></div><subcollection if="{child.members && child.members.length > 0}" collection="{child}" language="{this.opts.language}" defaultlanguage="{this.opts.defaultlanguage}"></subcollection></li></ul>', '', '', function(opts) {
		this.collection = this.opts.collection;

		this.getId = function(element) {
		    if(!element) {
		        return undefined;
		    } else if (Array.isArray(element) && element.length > 0) {
		        return viewerJS.iiif.getId(element[0]);
		    } else {
		        return viewerJS.iiif.getId(element);
		    }
		}.bind(this)

		this.getValue = function(element) {
		    return viewerJS.iiif.getValue(element, this.opts.language, this.opts.defaultlanguage);
		}.bind(this)

		this.getChildren = function(collection) {
		    if(collection.members) {
		    	return collection.members.filter( child => viewerJS.iiif.isCollection(child));
		    } else {
		        return [];
		    }
		}.bind(this)
});



riot.tag2('thumbnails', '<div ref="thumb" class="thumbnails-image-wrapper {this.opts.index == index ? \'selected\' : \'\'} {getPageStatus(index)}" each="{canvas, index in thumbnails}"><a class="thumbnails-image-link" href="{getLink(canvas)}" onclick="{handleClickOnImage}"><img class="thumbnails-image" alt="{getObjectTitle() + \': \' + getValue(canvas.label)}" riot-src="{getImage(canvas)}" loading="lazy"><div class="thumbnails-image-overlay"><div class="thumbnails-label">{getValue(canvas.label)}</div></div></a></div>', '', '', function(opts) {

this.thumbnails = [];
this._debug = false;

this.on("mount", () => {
	if(this._debug)console.log("mount ", this.opts);
	this.type = opts.type ? opts.type : "items";
	this.language = opts.language ? opts.language : "en";
	this.imageSize = opts.imagesize;
	if(this.opts.index === undefined) {
		this.opts.index = 0;
	}

	let source = opts.source;
	if(viewerJS.isString(source)) {
		fetch(source)
		.then(response => response.json())
		.then(json => this.loadThumbnails(json, this.type))
		.catch(e => {
			console.error("Error reading manifest from ", source);
		})
	} else {
		this.loadThumbnails(source, this.type);
	}
});

this.on("updated", () => {
	if(this._debug)console.log("updated", this.opts);
	let activeThumb = this.refs.thumb[this.opts.index];
	if(activeThumb) {
		activeThumb.scrollIntoView({block: "end", behavior: "smooth"});
	}
	if(this.opts.onload) {
	    this.opts.onload();
	}
});

this.loadThumbnails = function(source, type) {
    if(this._debug)console.log("Loading thumbnails from ", source);
	if(source) {
		switch(type) {
			case "structures":
				if(this._debug)console.log("structures", source.structures);
				rxjs.from(source.structures)
				.pipe(
						rxjs.operators.map(range => this.getFirstCanvas(range, true)),
						rxjs.operators.concatMap(canvas => this.loadCanvas(canvas))
						)
				.subscribe(item => this.addThumbnail(item));
				break;
			case "sequence":
				this.createThumbnails(source.sequences[0].canvases);
				break;
			case "items":
			case "default":
				this.createThumbnails(source.items)
		}
	} else {
		throw "source manifest not defined";
	}

}.bind(this)

this.addThumbnail = function(item) {
    if(this._debug)console.log("add thumbnail from ", item);

	this.thumbnails.push(item);
	this.update();
}.bind(this)

this.createThumbnails = function(items) {
    if(this._debug)console.log("creating thumbnails from ", items);

	this.thumbnails = items;
	this.update();
}.bind(this)

this.getFirstCanvas = function(range, overwriteLabel) {

	let canvas = undefined;
	if(range.start) {
		canvas = range.start;
	} else if(range.items) {
		canvas = range.items.find( item => item.type == "Canvas");
	}
	if(canvas && overwriteLabel) {
		if(this.opts.label) {
			let md = range.metadata.find(md => viewerJS.iiif.getValue(md.label, "none") == this.opts.label);
			if(md) {
				canvas.label = this.getValue(md.value);
			} else {
				canvas.label = range.label;
			}
		} else {
			canvas.label = range.label;
		}

	}
	return canvas;
}.bind(this)

this.loadCanvas = function(source) {
	return fetch(viewerJS.iiif.getId(source))
	.then(response => response.json())
	.then(canvas => {

		if(source.label) {
			canvas.label = source.label;
		}
		return canvas;
	})
}.bind(this)

this.getValue = function(value) {
	return viewerJS.iiif.getValue(value, this.language, this.language == "en" ? "de" : "en");
}.bind(this)

this.getObjectTitle = function() {
	try {
	return document.querySelector('.archives__object-title').innerHTML;
	}
	catch (e) {

		return '';
	}
}.bind(this)

this.getImage = function(canvas) {

	if(canvas.items) {
		return canvas.items
		.filter(page => page.items != undefined)
		.flatMap(page => page.items)
		.filter(anno => anno.body != undefined)
		.map(anno => anno.body)
		.map(res => this.getImageUrl(res, this.imageSize))
		.find(url => url != undefined)
	} else if(canvas.images && canvas.images.length > 0) {
		return this.getImageUrl(canvas.images[0].resource, this.imageSize);
	} else {
		return undefined;
	}
}.bind(this)

this.getImageUrl = function(resource, size) {

	if(size && resource.service && (!Array.isArray(resource.service) || resource.service.length > 0)) {
		let url = viewerJS.iiif.getId(viewerJS.iiif.getId(resource.id) ? resource.service[0] : resource.service);
		return url + "/full/" + size + "/0/default." + this.getExtension(resource.format);
	} else {
		return viewerJS.iiif.getId(resource);
	}
}.bind(this)

this.getExtension = function(format) {
	if(format && format == "image/png") {
		return "png";
	} else {
		return "jpg";
	}
}.bind(this)

this.getLink = function(canvas) {
	if(this.opts.link) {
		return this.opts.link(canvas);
	} else {
		return this.getHomepage(canvas);
	}
}.bind(this)

this.getHomepage = function(canvas) {
	if(canvas.homepage && canvas.homepage.length > 0) {
		return canvas.homepage[0].id;
	} else {
		return undefined;
	}
}.bind(this)

this.handleClickOnImage = function(event) {
	if(this.opts.actionlistener) {
		this.opts.actionlistener.next({
			action: "clickImage",
			value: event.item.index
		})
	}

	event.preventUpdate = true;
}.bind(this)

this.getPageStatus = function(index) {
	if(this.opts.statusmap) {
		return this.opts.statusmap.get(index);
	}
}.bind(this)

});
riot.tag2('timematrix', '<div class="timematrix__subarea"><span class="timematrix__loader" ref="loader"><img if="{loading}" riot-src="{opts.contextPath}resources/images/infinity_loader.svg" class="img-fluid" alt="Timematrix Loader"></span></div><div class="timematrix__selection"><div id="locateTimematrix"><div class="timematrix__bar"><div class="timematrix__period"><span>{translate(⁗timematrix__timePeriod⁗)}:</span>&#xA0; <input tabindex="0" aria-label="{translate(\'aria_label__timeline_period_start\')}" class="timematrix__selectionRangeInput" ref="inputStartYear" riot-value="{this.startYear}" maxlength="4"> &#xA0;<span>-</span>&#xA0; <input tabindex="0" aria-label="{translate(\'aria_label__timeline_period_end\')}" class="timematrix__selectionRangeInput" ref="inputEndYear" riot-value="{this.endYear}" maxlength="4"></div><div class="timematrix__hitsForm"><div class="timematrix__hitsInput"><span>{translate(⁗timematrix__maxResults⁗)}: &#xA0;</span><input onchange="{updateHitsPerPage}" type="text" id="hitsPerPage" class="hitsPerPage" name="hitsPerPage" riot-value="{this.maxHits}" placeholder="" maxlength="5" aria-label="{translate(\'aria_label__timeline_hits\')}"></div></div></div><div id="slider-range" ref="sliderRange"></div><button type="submit" ref="setTimematrix" class="btn btn--full setTimematrix">{translate(⁗timematrix__calculate⁗)}</button></div></div><div class="timematrix__objects"><label if="{!loading && manifests.length == 0}">{translate(⁗hitsZero⁗)}</label><div each="{manifest in manifests}" class="timematrix__content"><div class="timematrix__img"><a href="{getViewerUrl(manifest)}"><img ref="image" data-src="{getImageUrl(manifest)}" class="timematrix__image" data-viewer-thumbnail="thumbnail" alt="" aria-hidden="true" onload="$(this).parents(\'.timematrix__img\').css(\'background\', \'transparent\')"><div class="timematrix__text"><p if="{hasTitle(manifest)}" name="timetext" class="timetext">{getDisplayTitle(manifest)}</p></div></a></div></div></div>', '', '', function(opts) {
		this.manifests = [];
		this.loading = true;

		this.on( 'updated', function() {
		    if(this.refs.image) {
		        if(Array.isArray(this.refs.image)) {
				    this.refs.image.forEach(ele => {
				        if(!ele.src) {
				        	viewerJS.thumbnailLoader.load(ele);
				        }
				    })
		        } else {
		            viewerJS.thumbnailLoader.load(this.refs.image)
		        }
		    }
		});

	    this.on( 'mount', function() {

	        let restoredValues = this.restoreValues();
	        if(restoredValues) {
	            this.startYear = restoredValues.startYear;
		        this.endYear = restoredValues.endYear;
		        this.maxHits = restoredValues.maxHits;
	        } else {
		        this.startYear = this.opts.minYear;
		        this.endYear = this.opts.maxYear;
		        this.maxHits = this.opts.maxHits;
	        }

	        this.updateTimeMatrix = new rxjs.Subject();

	        this.updateTimeMatrix.pipe(
	                rxjs.operators.map( e => this.getIIIFApiUrl()),
	                rxjs.operators.switchMap( url => {

	                    this.loading = true;
	                    this.update();
	                    return fetch(url);
	                }),
	                rxjs.operators.switchMap( result => {

	                    return result.json();
	                }),
	                ).subscribe(json => {
	                    this.manifests = json.orderedItems ? json.orderedItems : [];

	                    this.loading = false;
	                    this.update();
	                })

	        this.initSlider( this.opts.slider, this.startYear, this.endYear, this.opts.minYear, this.opts.maxYear );
	        this.updateTimeMatrix.next();
	    } );

	    this.getViewerUrl = function(manifest) {
	        let viewer  = manifest.rendering;
	        if(Array.isArray(viewer)) {
	            viewer = viewer.find(r => r.format == "text/html");
	        }
	        if(viewer) {
	            return viewer["@id"];
	        } else {
	            return "";
	        }
	    }.bind(this)

	    this.getImageUrl = function(manifest) {
	        if(manifest.thumbnail) {
	            let url = manifest.thumbnail["@id"];
	            return url;
	        }
	    }.bind(this)

	    this.hasTitle = function(manifest) {
	        return manifest.label != undefined;
	    }.bind(this)

	    this.getDisplayTitle = function(manifest) {
	        return viewerJS.iiif.getValue(manifest.label, this.opts.language, "en");
	    }.bind(this)

	    this.getIIIFApiUrl = function() {
	        var apiTarget = this.opts.contextPath;
	        apiTarget += "api/v1/records/list";
	        apiTarget += "?start=" + this.startYear;
	        apiTarget += "&end=" + this.endYear;
	        apiTarget += "&rows=" + this.maxHits;
	        apiTarget += "&sort=RANDOM";
	        if ( this.opts.subtheme ) {
	            apiTarget += ( "&subtheme=" + this.opts.subtheme );
	        }
	        return apiTarget;
	    }.bind(this)

	    this.getApiUrl = function() {

	        var apiTarget = this.opts.contextPath;
	        apiTarget += 'rest/records/timematrix/range/';
	        apiTarget += $( this.opts.startInput ).val();
	        apiTarget += "/";
	        apiTarget += $( this.opts.endInput ).val();
	        apiTarget += '/';
	        apiTarget += $( this.maxHits ).val();
	        apiTarget += '/';

	        if ( this.opts.subtheme ) {
	            apiTarget += ( "?subtheme=" + this.opts.subtheme );
	        }

	        return apiTarget;
	    }.bind(this)

	    this.initSlider = function( sliderSelector, startYear, endYear, minYear, maxYear ) {
	        let $slider = $( this.refs.sliderRange );

	        let rtl = $slider.closest('[dir="rtl"]').length > 0;

	        $slider.slider( {
	            range: true,
	            isRTL: rtl,
	            min: minYear,
	            max: maxYear,
	            values: [ startYear, endYear ],
	            slide: function( event, ui ) {
	                $( this.refs.inputStartYear ).val( ui.values[ 0 ] ).change();
	                this.startYear = parseInt( ui.values[ 0 ] );
	                $( this.refs.inputEndYear ).val( ui.values[ 1 ] ).change();
	                this.endYear = parseInt( ui.values[ 1 ] );
	            }.bind( this ),
	            stop: (event, ui) => {
	                this.updateTimeMatrix.next();
                    this.storeValues();
	            }
	        } );

	        $slider.find( ".ui-slider-handle" ).on( 'mousedown', function() {
	            $( '.ui-slider-handle' ).removeClass( 'top' );
	            $( this ).addClass( 'top' );
	        } );
	    }.bind(this)

	    this.translate = function(key) {
	        return this.opts.msg[key];
	    }.bind(this)
	    this.updateHitsPerPage = function(event) {
	        this.maxHits = event.target.value;
	        this.storeValues();
	        this.updateTimeMatrix.next();
	    }.bind(this)

	    this.restoreValues = function() {
	        let string = sessionStorage.getItem("viewer_timematrix");
	        if(string) {
	            let json = JSON.parse(string);
	            return json;
	        } else {
	            return undefined;
	        }
	    }.bind(this)

	    this.storeValues = function() {
	        let json = {startYear: this.startYear, endYear: this.endYear, maxHits: this.maxHits}
	        let string = JSON.stringify(json);
	        sessionStorage.setItem("viewer_timematrix", string);
	    }.bind(this)

});

