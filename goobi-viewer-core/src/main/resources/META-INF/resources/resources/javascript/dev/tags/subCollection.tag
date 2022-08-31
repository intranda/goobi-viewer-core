<subCollection>
	<ul if="{collection.members && collection.members.length > 0}" class="list card-body__list">
		<li each="{child in getChildren(collection)}">
			<div class="card-body__links">
				<a class="card-body__collection" href="{getId(child.rendering)}">{getValue(child.label)} ({viewerJS.iiif.getContainedWorks(child)})</a>
				<a class="card-body__rss" href="{viewerJS.iiif.getRelated(child, 'Rss feed')['@id']}" target="_blank">
					<i class="fa fa-rss" aria-hidden="true"/>
				</a> 
			</div>
			<subCollection if="{child.members && child.members.length > 0}" collection="{child}"/>
		</li>
	</ul>

	<script>
		this.collection = this.opts.collection;
		
		getId(element) {
		    if(!element) {
		        return undefined;
		    } else if (Array.isArray(element) && element.length > 0) {
		        return viewerJS.iiif.getId(element[0]);
		    } else {
		        return viewerJS.iiif.getId(element);
		    }
		}
		
		getValue(element) {
		    return viewerJS.iiif.getValue(element, this.opts.language, this.opts.defaultlanguage);
		}
		
		getChildren(collection) {
		    if(collection.members) {        
		    	return collection.members.filter( child => viewerJS.iiif.isCollection(child));
		    } else {
		        return [];
		    }
		}
	</script>

</subCollection>
