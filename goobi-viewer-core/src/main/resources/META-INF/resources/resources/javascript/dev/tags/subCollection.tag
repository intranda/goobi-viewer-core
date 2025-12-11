<subCollection>
	<ul if="{collection.members && collection.members.length > 0}" class="list card-body__list">
		<li each="{child in getChildren(collection)}">
			<div class="card-body__links">
				<a class="card-body__collection" href="{getId(child.rendering)}">{getValue(child.label)} ({viewerJS.iiif.getContainedWorks(child)})</a>
				<a class="card-body__rss" href="{viewerJS.iiif.getRelated(child, 'Rss feed')['@id']}" target="_blank">
					<svg class="card-body__rss-icon" viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
						<use riot-href="{getIconHref('rss')}"></use>
					</svg>
				</a> 
			</div>
			<subCollection if="{child.members && child.members.length > 0}" collection="{child}" language="{this.opts.language}" defaultlanguage="{this.opts.defaultlanguage}"/>
		</li>
	</ul>

	<script>
		const ensureTrailingSlash = value => value.endsWith('/') ? value : `${value}/`;
		const viewerConfig = window.viewerConfig || {};
		this.iconBasePath = ensureTrailingSlash(viewerConfig.iconBasePath || viewerConfig.contextPath || '/');
		this.getIconHref = iconName => `${this.iconBasePath}resources/icons/outline/${iconName}.svg#icon`;

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
