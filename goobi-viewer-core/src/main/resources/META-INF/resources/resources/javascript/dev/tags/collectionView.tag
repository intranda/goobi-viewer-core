<collectionView>

	<div each="{set, index in collectionSets}">
		<h3 if="{set[0] != ''}">{translator.translate(set[0])}</h3>
		<collectionList collections="{set[1]}" language="{opts.language}" setindex="{index}"/>
	</div>

<script>

this.collectionSets = [];


this.on("mount", () => {
    console.log("mounting collectionView", this.opts);
    
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

fetchCollections() {
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
}

buildSets(collection) {
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