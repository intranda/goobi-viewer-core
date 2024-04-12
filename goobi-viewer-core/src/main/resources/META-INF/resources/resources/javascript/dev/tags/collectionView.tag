<collectionView>
	<div each="{set, index in collectionSets}">
		<h2 if="{set[0] != ''}">{translator.translate(set[0])}</h2>
		<collectionList collections="{set[1]}" language="{opts.language}" defaultlanguage="{opts.defaultlanguage}" setindex="{index}" depth="{opts.depth}"/>
	</div>

<script>

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

fetchCollections() {
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
}

buildSets(collection) {
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
}

compareMembers(m1, m2, compareMode) {
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
}

addToMap(map, key, value) {
    let list = map.get(key);
    if(list === undefined) {
        list = [];
        map.set(key, list);
    }
    list.push(value);
}

</script>

</collectionView>