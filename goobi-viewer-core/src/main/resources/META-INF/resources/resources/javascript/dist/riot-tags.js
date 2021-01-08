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


riot.tag2('annotationbody', '<plaintextresource if="{isPlaintext()}" resource="{this.annotationBody}" annotationid="{this.opts.annotationid}"></plaintextResource><htmltextresource if="{isHtml()}" resource="{this.annotationBody}" annotationid="{this.opts.annotationid}"></htmltextResource><geomapresource if="{isGeoJson()}" resource="{this.annotationBody}" annotationid="{this.opts.annotationid}" mapboxtoken="{this.opts.mapboxtoken}"></geoMapResource><authorityresource if="{isAuthorityResource()}" resource="{this.annotationBody}" annotationid="{this.opts.annotationid}" currentlang="{this.opts.currentlang}" resturl="{this.opts.resturl}"></authorityResource><datasetresource if="{isDatasetResource()}" resource="{this.annotationBody}" annotationid="{this.opts.annotationid}" currentlang="{this.opts.currentlang}" resturl="{this.opts.resturl}"></datasetResource>', '', '', function(opts) {

this.on("mount", () => {
    if(this.opts.contentid) {
        this.annotationBody = JSON.parse(document.getElementById(this.opts.contentid).innerText);
        this.type = this.annotationBody.type;
        if(!this.type) {
            this.type = this.anotationBody["@type"];
        }
        this.format = this.annotationBody.format;
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
	    this.url = this.opts.resturl + "normdata/get/" + this.unicodeEscapeUri(this.authorityId) + "/ANNOTATION/" + this.opts.currentlang + "/"
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
                console.log()
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
    let lists =  this.opts.bookmarks.getBookmarkLists().slice(0,3);
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


riot.tag2('bookmarklistsession', '<ul each="{bookmarkList in getBookmarkLists()}" class="{mainClass} list"><li each="{bookmark in bookmarkList.items}"><div class="row no-margin"><div class="col-11 no-padding"><a class="row no-gutters" href="{opts.bookmarks.config.root}{bookmark.url}"><div class="col-4 no-padding"><div class="{mainClass}-image" riot-style="background-image: url({bookmark.representativeImageUrl});"></div></div><div class="col-7 no-padding"><h4>{bookmark.name}</h4></div></a></div><div class="col-1 no-padding {mainClass}-remove"><button class="btn btn--clean" type="button" data-bookmark-list-type="delete" onclick="{remove}" aria-label="{msg(\'bookmarkList_removeFromBookmarkList\')}"><i class="fa fa-ban" aria-hidden="true"></i></button></div></div></li></ul><div each="{bookmarkList in getBookmarkLists()}" class="{mainClass}-actions"><div if="{mayEmptyList(bookmarkList)}" class="{mainClass}-reset"><button class="btn btn--clean" type="button" data-bookshelf-type="reset" onclick="{deleteList}"><span>{msg(\'bookmarkList_reset\')}</span><i class="fa fa-trash-o" aria-hidden="true"></i></button></div><div if="{maySendList(bookmarkList)}" class="{mainClass}-send"><a href="{sendListUrl(bookmarkList)}"><span>{msg(\'bookmarkList_session_mail_sendList\')}</span><i class="fa fa-paper-plane-o" aria-hidden="true"></i></a></div><div if="{maySearchList(bookmarkList)}" class="{mainClass}-search"><a href="{searchListUrl(bookmarkList)}" data-toggle="tooltip" data-placement="top" data-original-title="" title=""><span>{msg(\'action__search_in_bookmarks\')}</span><i class="fa fa-search" aria-hidden="true"></i></a></div><div if="{mayCompareList(bookmarkList)}" class="{mainClass}-mirador"><a href="{miradorUrl(bookmarkList)}" target="_blank"><span>{msg(\'viewMiradorComparison\')}</span><i class="fa fa-th" aria-hidden="true"></i></a></div></div>', '', '', function(opts) {


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


riot.tag2('bookmarkspopup', '<div class="bookmark-popup__body-loader"></div><div if="{opts.data.page !== undefined}" class="bookmark-popup__radio-buttons"><div><label><input type="radio" checked="{opts.bookmarks.isTypeRecord()}" name="bookmarkType" riot-value="{msg(\'bookmarkList_typeRecord\')}" onclick="{setBookmarkTypeRecord}">{msg(\'bookmarkList_typeRecord\')}</label></div><div><label><input type="radio" checked="{opts.bookmarks.isTypePage()}" name="bookmarkType" riot-value="{msg(\'bookmarkList_typePage\')}" onclick="{setBookmarkTypePage}">{msg(\'bookmarkList_typePage\')}</label></div></div><div class="bookmark-popup__header"> {msg(\'bookmarkList_selectBookmarkList\')} </div><div class="bookmark-popup__body"><bookmarklist data="{this.opts.data}" loader="{this.opts.loader}" button="{this.opts.button}" bookmarks="{this.opts.bookmarks}"></bookmarkList></div><div class="bookmark-popup__footer"><div class="row no-margin"><div class="col-11 no-padding"><input ref="inputValue" type="text" placeholder="{msg(\'bookmarkList_addNewBookmarkList\')}" aria-label="{msg(\'bookmarkList_addNewBookmarkList\')}"></div><div class="col-1 no-padding"><button class="btn btn-clean" type="button" aria-label="{msg(\'bookmarkList_addNewBookmarkList\')}" onclick="{add}"></button></div></div></div>', '', 'class="bookmark-popup bottom" role="region" aria-label="{msg(\'bookmarks\')}"', function(opts) {

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
    rxjs.from(this.collections)
    .pipe(
    	rxjs.operators.filter( child => this.hasChildren(child) ),
    	rxjs.operators.mergeMap( child => this.fetchMembers(child) ),
    	rxjs.operators.debounceTime(100)

    )
    .subscribe( () => {this.update();});

}.bind(this)

this.fetchMembers = function(collection) {
    return fetch(collection['@id'])
    .then( result => result.json())
    .then(json => {collection.members = json.members;})
}.bind(this)

this.getValue = function(element) {
    return viewerJS.iiif.getValue(element, this.opts.language, this.opts.defaultlanguage);
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


riot.tag2('collectionview', '<div each="{set, index in collectionSets}"><h3 if="{set[0] != \'\'}">{translator.translate(set[0])}</h3><collectionlist collections="{set[1]}" language="{opts.language}" defaultlanguage="{opts.defaultlanguage}" setindex="{index}"></collectionlist></div>', '', '', function(opts) {

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


riot.tag2('campaignitem', '<div if="{!opts.pi}" class="crowdsourcing-annotations__content-wrapper"> {Crowdsourcing.translate(⁗crowdsourcing__error__no_item_available⁗)} </div><div if="{opts.pi}" class="crowdsourcing-annotations__content-wrapper"><span if="{this.loading}" class="crowdsourcing-annotations__loader-wrapper"><img riot-src="{this.opts.loaderimageurl}"></span><span if="{this.error}" class="crowdsourcing-annotations__loader-wrapper"><span class="error_message">{this.error.message}</span></span></span><div class="crowdsourcing-annotations__content-left"><imageview if="{this.item}" id="mainImage" source="{this.item.getCurrentCanvas()}" item="{this.item}"></imageView><canvaspaginator if="{this.item}" item="{this.item}"></canvasPaginator></div><div if="{this.item}" class="crowdsourcing-annotations__content-right"><div class="crowdsourcing-annotations__questions-wrapper"><div each="{question, index in this.item.questions}" onclick="{setActive}" class="crowdsourcing-annotations__question-wrapper {question.isRegionTarget() ? \'area-selector-question\' : \'\'} {question.active ? \'active\' : \'\'}"><div class="crowdsourcing-annotations__question-wrapper-description">{Crowdsourcing.translate(question.text)}</div><plaintextquestion if="{question.questionType == \'PLAINTEXT\'}" question="{question}" item="{this.item}" index="{index}"></plaintextQuestion><richtextquestion if="{question.questionType == \'RICHTEXT\'}" question="{question}" item="{this.item}" index="{index}"></richtextQuestion><geolocationquestion if="{question.questionType == \'GEOLOCATION_POINT\'}" question="{question}" item="{this.item}" index="{index}"></geoLocationQuestion><authorityresourcequestion if="{question.questionType == \'NORMDATA\'}" question="{question}" item="{this.item}" index="{index}"></authorityResourceQuestion><metadataquestion if="{question.questionType == \'METADATA\'}" question="{question}" item="{this.item}" index="{index}"></metadataQuestion></div></div><campaignitemlog if="{item.showLog}" item="{item}"></campaignItemLog><div if="{!item.isReviewMode()}" class="crowdsourcing-annotations__options-wrapper crowdsourcing-annotations__options-wrapper-annotate"><button onclick="{saveAnnotations}" class="crowdsourcing-annotations__options-wrapper__option btn btn--default" id="save">{Crowdsourcing.translate(⁗button__save⁗)}</button><div>{Crowdsourcing.translate(⁗label__or⁗)}</div><button if="{item.isReviewActive()}" onclick="{submitForReview}" class="options-wrapper__option btn btn--success" id="review">{Crowdsourcing.translate(⁗action__submit_for_review⁗)}</button><button if="{!item.isReviewActive()}" onclick="{acceptReview}" class="options-wrapper__option btn btn--success" id="review">{Crowdsourcing.translate(⁗action__accept_review⁗)}</button><div>{Crowdsourcing.translate(⁗label__or⁗)}</div><button if="{this.opts.nextitemurl}" onclick="{skipItem}" class="options-wrapper__option btn btn--link" id="skip">{Crowdsourcing.translate(⁗action__skip_item⁗)}</button></div><div if="{item.isReviewActive() && item.isReviewMode()}" class="crowdsourcing-annotations__options-wrapper crowdsourcing-annotations__options-wrapper-review"><button onclick="{acceptReview}" class="options-wrapper__option btn btn--success" id="accept">{Crowdsourcing.translate(⁗action__accept_review⁗)}</button><div>{Crowdsourcing.translate(⁗label__or⁗)}</div><button onclick="{rejectReview}" class="options-wrapper__option btn btn--danger" id="reject">{Crowdsourcing.translate(⁗action__reject_review⁗)}</button><div>{Crowdsourcing.translate(⁗label__or⁗)}</div><button if="{this.opts.nextitemurl}" onclick="{skipItem}" class="options-wrapper__option btn btn--link" id="skip">{Crowdsourcing.translate(⁗action__skip_item⁗)}</button></div></div></div>', '', '', function(opts) {

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
	    this.item = new Crowdsourcing.Item(itemConfig, 0);
	    this.item.logEndpoint = this.item.id + "/" + this.opts.pi + "/log/";
	    if(this.opts.currentuserid) {
	        this.item.setCurrentUser(this.opts.currentuserid, this.opts.currentusername, this.opts.currentuseravatar);
	    }
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
	            creator: this.item.getCreator().id,
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
riot.tag2('campaignitemlog', '<div class="crowdsourcing-annotations__log-wrapper"><div class="crowdsourcing-annotations__log-title"><span>{Crowdsourcing.translate(⁗log⁗)}</span><span ref="compress" onclick="{compressLog}" class="crowdsourcing-annotations__log-expand"><i class="fa fa-angle-up" aria-hidden="true"></i></span><span ref="expand" onclick="{expandLog}" class="crowdsourcing-annotations__log-expand"><i class="fa fa-angle-down" aria-hidden="true"></i></span></div><div ref="toggleBox" class="crowdsourcing-annotations__toggle-box"><div ref="innerWrapper" class="crowdsourcing-annotations__log-inner-wrapper"><div each="{message in messages}" class="crowdsourcing-annotations__log-message-entry {isCurrentUser(message.creator) ? \'-from-me\' : \'\'}"><img class="crowdsourcing-annotations__log-round-avatar" riot-src="{message.creator.avatar}"></img><div class="crowdsourcing-annotations__log-speech-bubble"><div class="crowdsourcing-annotations__log-message-info"><div class="crowdsourcing-annotations__log-message-user-name"> {message.creator.name} </div></div><div class="crowdsourcing-annotations__log-message-text"> {message.message} </div><div class="crowdsourcing-annotations__log-message-time-stamp"> {formatDate(message.dateCreated)} </div></div></div></div><div ref="messageBox" class="crowdsourcing-annotations__log-send-message-area"><input onkeypress="{addMessageOnEnter}" placeholder="{Crowdsourcing.translate(\'label__enter_message_here\')}" class="crowdsourcing-annotations__log-message-input" id="crowdsourcingAnnotationsLogMessageInput" name="crowdsourcingAnnotationsLogMessageInput" ref="messageText"></input><button class="btn btn--default crowdsourcing-annotations__log-message-send-button" onclick="{addMessage}">{Crowdsourcing.translate(\'action__send\')}</button></div></div></div>', '', '', function(opts) {

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

this.toPageNumber = function(e) {
    console.log("Change in ", e.target, e.target.value);
    let page = parseInt(e.target.value);
    if(page > 0 && page <= this.getTotalImageCount()) {
    	this.load(page-1);
    } else{
        alert(page + " is not a valid page number")
    }
}.bind(this)

});
riot.tag2('geolocationquestion', '<div if="{this.showInstructions()}" class="crowdsourcing-annotations__instruction"><label>{Crowdsourcing.translate(⁗crowdsourcing__help__create_rect_on_image⁗)}</label></div><div if="{this.showAddMarkerInstructions()}" class="crowdsourcing-annotations__single-instruction"><label>{Crowdsourcing.translate(⁗crowdsourcing__help__add_marker_to_image⁗)}</label></div><div if="{this.showInactiveInstructions()}" class="crowdsourcing-annotations__single-instruction -inactive"><label>{Crowdsourcing.translate(⁗crowdsourcing__help__make_active⁗)}</label></div><div id="geoMap_{opts.index}" class="geo-map"></div><div id="annotation_{index}" each="{anno, index in this.annotations}"></div>', '', '', function(opts) {


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
        if(anno.color) {
            let markerIcon = this.geoMap.getMarkerIcon().options;
            markerIcon.markerColor = anno.color;
            this.geoMap.setMarkerIcon(markerIcon);
        }
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
        allowMovingFeatures: !this.opts.item.isReviewMode(),
        language: Crowdsourcing.translator.language,
        popover: undefined,
        emptyMarkerMessage: undefined,
        popoverOnHover: false,
    })
    let initialView = {
        zoom: 5,
        center: [11.073397, 49.451993]
    };
    this.geoMap.setMarkerIcon({
        shape: "circle",
        prefix: "fa",
        markerColor: "blue",
        iconColor: "white",
        icon: "fa-circle",
        svg: true
    })
    this.geoMap.init(initialView);
    this.geoMap.onFeatureMove.subscribe(feature => this.moveFeature(feature));
    this.geoMap.onFeatureClick.subscribe(feature => this.removeFeature(feature));
    this.geoMap.onMapClick.subscribe(geoJson => {
        if(this.addMarkerActive && (this.question.targetFrequency == 0 || this.geoMap.getMarkerCount() < this.question.targetFrequency)) {
            if(this.annotationToMark && this.annotationToMark.color) {
                let markerIcon = this.geoMap.getMarkerIcon().options;
                markerIcon.markerColor = this.annotationToMark.color;
                this.geoMap.setMarkerIcon(markerIcon);
            }
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

				    var now = rxjs.of(image);
					this.opts.item.setImageSource = function(source) {
					    this.image.setTileSource(this.getImageInfo(source));
					}.bind(this);
				    this.opts.item.notifyImageOpened(image.observables.viewerOpen.pipe(rxjs.operators.map( () => image),rxjs.operators.merge(now)));
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


riot.tag2('metadataquestion', '<div if="{this.showInstructions()}" class="crowdsourcing-annotations__instruction"><label>{Crowdsourcing.translate(⁗crowdsourcing__help__create_rect_on_image⁗)}</label></div><div if="{this.showInactiveInstructions()}" class="crowdsourcing-annotations__single-instruction -inactive"><label>{Crowdsourcing.translate(⁗crowdsourcing__help__make_active⁗)}</label></div><div class="crowdsourcing-annotations__wrapper" id="question_{opts.index}_annotation_{index}" each="{anno, index in this.question.annotations}"><div class="crowdsourcing-annotations__annotation-area"><div if="{this.showAnnotationImages()}" class="crowdsourcing-annotations__annotation-area-image" riot-style="border-color: {anno.getColor()}"><img riot-src="{this.question.getImage(anno)}"></img></div><div class="crowdsourcing-annotations__question-metadata-list"><div each="{field, fieldindex in this.metadataFields}" class="crowdsourcing-annotations__question-metadata-list-item mb-2"><label class="crowdsourcing-annotations__question-metadata-list-item-label">{Crowdsourcing.translate(field)}:</label><div class="crowdsourcing-annotations__question-metadata-list-item-field" if="{this.hasOriginalValue(field)}">{this.getOriginalValue(field)}</div><input class="crowdsourcing-annotations__question-metadata-list-item-input form-control" if="{!this.hasOriginalValue(field)}" disabled="{this.opts.item.isReviewMode() ? \'disabled\' : \'\'}" ref="input_{index}_{fieldindex}" type="text" data-annotationindex="{index}" riot-value="{anno.getValue(field)}" onchange="{setValueFromEvent}"></input></div></div></div><div class="cms-module__actions crowdsourcing-annotations__annotation-action"><button if="{!this.opts.item.isReviewMode()}" onclick="{deleteAnnotationFromEvent}" class="crowdsourcing-annotations__delete-annotation btn btn--clean delete">{Crowdsourcing.translate(⁗action__delete_annotation⁗)} </button></div></div><button if="{showAddAnnotationButton()}" onclick="{addAnnotation}" class="options-wrapper__option btn btn--default" id="add-annotation">{Crowdsourcing.translate(⁗action__add_annotation⁗)}</button>', '', '', function(opts) {

	this.question = this.opts.question;

	this.metadataFields = [];

	this.originalData = {};

	this.on("mount", function() {
	    this.initOriginalMetadata(this.question);
	    this.question.initializeView((anno) => new Crowdsourcing.Annotation.Metadata(anno, this.originalData), this.update, this.update, this.focusAnnotation);
	    console.log("mounted ", this);
		    this.opts.item.onImageOpen(function() {
		        switch(this.question.targetSelector) {
		            case Crowdsourcing.Question.Selector.WHOLE_PAGE:
		            case Crowdsourcing.Question.Selector.WHOLE_SOURCE:
		                if(this.question.annotations.length == 0 && !this.question.item.isReviewMode()) {
		                    this.question.addAnnotation();
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


riot.tag2('richtextquestion', '<div if="{this.showInstructions()}" class="annotation_instruction"><label>{Crowdsourcing.translate(⁗crowdsourcing__help__create_rect_on_image⁗)}</label></div><div if="{this.showInactiveInstructions()}" class="crowdsourcing-annotations__single-instruction -inactive"><label>{Crowdsourcing.translate(⁗crowdsourcing__help__make_active⁗)}</label></div><div class="crowdsourcing-annotations__wrapper" id="question_{opts.index}_annotation_{index}" each="{anno, index in this.question.annotations}"><div class="crowdsourcing-annotations__annotation-area"><div if="{this.showAnnotationImages()}" class="crowdsourcing-annotations__annotation-area-image" riot-style="border-color: {anno.getColor()}"><img riot-src="{this.question.getImage(anno)}"></img></div><div class="crowdsourcing-annotations__question-text-input"><textarea class="tinyMCE" disabled="{this.opts.item.isReviewMode() ? \'disabled\' : \'\'}" onchange="{setTextFromEvent}" riot-value="{anno.getText()}"></textarea></div></div><div class="cms-module__actions crowdsourcing-annotations__annotation-action"><button if="{!this.opts.item.isReviewMode()}" onclick="{deleteAnnotationFromEvent}" class="annotation_area__button btn btn--clean delete">{Crowdsourcing.translate(⁗action__delete_annotation⁗)} </button></div></div><button if="{showAddAnnotationButton()}" onclick="{addAnnotation}" class="options-wrapper__option btn btn--default" id="add-annotation">{Crowdsourcing.translate(⁗action__add_annotation⁗)}</button>', '', '', function(opts) {

	this.question = this.opts.question;

	this.on("mount", function() {
	  console.log("item ", this.question.item);
	    this.question.initializeView((anno) => new Crowdsourcing.Annotation.Richtext(anno), this.update, this.update, this.focusAnnotation);

	    this.opts.item.onImageOpen(function() {
	        switch(this.question.targetSelector) {
	            case Crowdsourcing.Question.Selector.WHOLE_PAGE:
	            case Crowdsourcing.Question.Selector.WHOLE_SOURCE:
	                if(this.question.annotations.length == 0 && !this.question.item.isReviewMode()) {
	                    this.question.addAnnotation();
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


riot.tag2('timematrix', '<div class="timematrix__objects"><div each="{manifest in manifests}" class="timematrix__content"><div class="timematrix__img"><a href="{getViewerUrl(manifest)}"><img riot-src="{getImageUrl(manifest)}" class="timematrix__image" data-viewer-thumbnail="thumbnail" alt="" aria-hidden="true" onerror="this.onerror=null;this.src=\'/viewer/resources/images/access_denied.png\'"><div class="timematrix__text"><p if="{hasTitle(manifest)}" name="timetext" class="timetext">{getDisplayTitle(manifest)}</p></div></a></div></div></div>', '', '', function(opts) {
	    this.on( 'mount', function() {

	        rxjs.fromEvent($( this.opts.button ), "click").pipe(
	                rxjs.operators.map( e => this.getIIIFApiUrl()),
	                rxjs.operators.switchMap( url => {

	                    this.opts.loading.show();
	                    return fetch(url);
	                }),
	                rxjs.operators.switchMap( result => {

	                    return result.json();
	                }),
	                ).subscribe(json => {
	                    this.manifests = json.orderedItems;
	                    this.update();
	                    this.opts.loading.hide();
	                })

	        this.manifests = [];
	        this.startDate = parseInt( $( this.opts.startInput ).val() );
	        this.endDate = parseInt( $( this.opts.endInput ).val() );
	        this.initSlider( this.opts.slider, this.startDate, this.endDate );
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
	        apiTarget += "?start=" + $( this.opts.startInput ).val();
	        apiTarget += "&end=" + $( this.opts.endInput ).val();
	        apiTarget += "&rows=" + $( this.opts.count ).val();
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
	        apiTarget += $( this.opts.count ).val();
	        apiTarget += '/';

	        if ( this.opts.subtheme ) {
	            apiTarget += ( "?subtheme=" + this.opts.subtheme );
	        }

	        return apiTarget;
	    }.bind(this)

	    this.initSlider = function( sliderSelector, startDate, endDate ) {
	        let $slider = $( sliderSelector );

	        $slider.slider( {
	            range: true,
	            min: parseInt( startDate ),
	            max: parseInt( endDate ),
	            values: [ startDate, endDate ],
	            slide: function( event, ui ) {
	                $( this.opts.startInput ).val( ui.values[ 0 ] ).change();
	                this.startDate = parseInt( ui.values[ 0 ] );
	                $( this.opts.endInput ).val( ui.values[ 1 ] ).change();
	                this.endDate = parseInt( ui.values[ 1 ] );
	            }.bind( this )
	        } );

	        $slider.find( ".ui-slider-handle" ).on( 'mousedown', function() {
	            $( '.ui-slider-handle' ).removeClass( 'top' );
	            $( this ).addClass( 'top' );
	        } );
	    }.bind(this)

});

