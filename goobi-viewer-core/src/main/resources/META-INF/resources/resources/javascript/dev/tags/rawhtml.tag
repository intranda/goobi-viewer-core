<rawhtml>
  <script>
  this.on("mount", () => {	  
	    this.root.innerHTML = opts.content;
	  })
  this.on("updated", () => {	  
    this.root.innerHTML = opts.content;
  })
  </script>
</rawhtml>