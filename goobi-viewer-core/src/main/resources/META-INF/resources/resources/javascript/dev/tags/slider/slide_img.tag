<slide_default>
	<a class="swiper-link slider-{this.opts.stylename}__link" href="{this.opts.link}" target="{this.opts.link_target}" rel="noopener">
		<div class="swiper-heading slider-{this.opts.stylename}__header">{this.opts.label}</div>
		<img class="swiper-image slider-{this.opts.stylename}__image" src="{this.opts.image}" alt="{this.opts.alttext}"/>
	</a>

	<!-- THIS IS NEEDED BECAUSE OF HTML ESCAPING FOR THE COLLECTION DESCRIPTIONS -->
	<script>
		this.on("mount", () => {
		});
	</script>

</slide_default>