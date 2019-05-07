describe("Cms.tagList ", function() {
	
	var config;
	
	beforeEach( function(){
		$('<p id="hello" /><form id="tagListForm"><input type="text" id="inputField"></input><input type="text" id="inputFieldWithTags" value=\'["tag1","tag2","tag3"]\'></input><ul id="tagList"></ul><div id="targetDiv"></div></form>').appendTo('body');

		 config = {
					inputFieldId: "inputField",
					tagListId: "tagList",
					autoSuggestUrl: "/http:viewer/rest/contentAssist/mediaTags/",
			}
	})

	afterEach(function(){
		$('#tagListForm').remove()
	});

	it("is defined ", function() {
		expect(cmsJS.tagList).toBeDefined();
	})
	
    describe("init ", function() {
    	it("initializes the correct html elements", function() {
    		cmsJS.tagList.init(config)
    		expect(cmsJS.tagList.$inputField.attr("id")).toEqual(config.inputFieldId);
    		expect(cmsJS.tagList.$tagListElement.attr("id")).toEqual(config.tagListId);
    		expect(cmsJS.tagList.autoSuggestUrl).toEqual(config.autoSuggestUrl);
    	})
    	
    	it("writes content of input field into tag list", function() {
    		config.inputFieldId = "inputFieldWithTags";
    		cmsJS.tagList.init(config)
    		var $tagLis = cmsJS.tagList.$tagListElement.children("[id$=_item]");
    		$tagLis.each(function(index, element) {
    			console.log("Found element " + $(element).attr("id"));
    		})
    		expect($tagLis.length).toEqual(3);
    		var $inputLi = cmsJS.tagList.$tagListElement.parent().find("[id$=_inputField]");
    		$inputLi.each(function(index, element) {
    			console.log("Found input element " + $(element).attr("id"));
    		})
    		expect($inputLi.length).toEqual(1);
    	})
    })
    
    describe ("list items ", function() {
    	
    	beforeEach( function(){
    		config.inputFieldId = "inputFieldWithTags";
    		cmsJS.tagList.init(config)
    	});
    	
    	it("lists all tag jquery items ", function() {
    		cmsJS.tagList.getTags().each(function(index, tag) {
    			console.log("Tag ", tag);
    		})
    		expect(cmsJS.tagList.getTags().length).toBe(3);
    	});
    	
    	it("lists all tag strings ", function() {
    		cmsJS.tagList.getTagValues().each(function(value) {
    			console.log("Tag value ", value);
    		})
    		expect(cmsJS.tagList.getTagValues().length).toBe(3);
    		expect(cmsJS.tagList.getTagValues()[1]).toBe("tag2");
    	});
    	
    	it("gets a tag item by its value ", function() {
    		expect(cmsJS.tagList.getTag("tag2")).toBeDefined();
    		expect(cmsJS.tagList.getTag("tag4")).not.toBeDefined();
    	});
    	
    	it("gets the value of a tag ", function() {
    		expect(cmsJS.tagList.getValue(cmsJS.tagList.getTags()[1])).toEqual("tag2");
    	});
    })
    
    
    describe("add item ", function() {
    	it("adds an item with the content of inputfield on value change ", function() {
    		cmsJS.tagList.init(config)
    		var $input = cmsJS.tagList.$tagListElement.parent().find("[id$=_inputField]");
    		expect($input.length).toEqual(1);

    		var $tagLis = cmsJS.tagList.$tagListElement.children("[id$=_item]");
    		expect($tagLis.length).toBe(0);
    		$input.val("tag42");
    		$input.trigger("change");
    		$tagLis = cmsJS.tagList.$tagListElement.children("[id$=_item]");
    		expect($tagLis.length).toBe(1);
    		expect($tagLis.text()).toBe("tag42");
    		expect(cmsJS.tagList.$inputField.val()).toBe('["tag42"]')
    	})
    	
    	it("does not add an item that is already part of the list ", function() {
    		config.inputFieldId = "inputFieldWithTags";
    		cmsJS.tagList.init(config);
    		var $tagLis = cmsJS.tagList.$tagListElement.children("[id$=_item]");
    		expect($tagLis.length).toBe(3);
    		cmsJS.tagList.addTag("tag2");
    		$tagLis = cmsJS.tagList.$tagListElement.children("[id$=_item]");
    		expect($tagLis.length).toBe(3);
    	})
    })
    
    describe("delete item ", function() {
    	it("removes an item from the list", function() {
    		config.inputFieldId = "inputFieldWithTags";
    		cmsJS.tagList.init(config);
    		var $tagLis = cmsJS.tagList.$tagListElement.children("[id$=_item]");
    		expect ($tagLis.length).toBe(3);
    		var $secondTagTerminator = $tagLis.eq(1).find(".tag-terminator");
    		expect($secondTagTerminator.length).toBe(1);
    		$secondTagTerminator.trigger("click");
    		$tagLis = cmsJS.tagList.$tagListElement.children("[id$=_item]");
    		expect($tagLis.length).toBe(2);
    		expect($tagLis.eq(0).text()).toBe("tag1");
    		expect($tagLis.eq(1).text()).toBe("tag3");
    		expect(cmsJS.tagList.$inputField.val()).toBe('["tag1","tag3"]')
    	})
    })
})
