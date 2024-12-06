<imagepaginator>

    <virtual if="{opts.enablePageNavigation}">
        <!-- FIRST PAGE -->
        <li if="{opts.numPages > 2}" class="image-controls__action {opts.rtl ? 'end' : 'start'} {isFirstPage() ? 'inactive' : ''}">
           
            <a if="{!isFirstPage() && !isSequenceMode()}" href="{getPageUrl(opts.firstPageNumber)}" title="{msg.firstImage}" data-toggle="tooltip" data-placement="{opts.tooltipPlacement}" aria-labelledby="firstImageLabel">
                <virtual if="{!opts.rtl}">
                	<yield from="first-page"/>
                </virtual>
                <virtual if="{opts.rtl}">
                	<yield from="last-page"/>
                </virtual>
<!--                 <img if="{!opts.rtl}" src="{opts.icons.first}"/> -->
<!--                 <img if="{opts.rtl}" src="{opts.icons.last}"/> --> 
                <span id="firstImageLabel" class="labeltext">{msg.firstImage}</span>
            </a>
            <button if="{!isFirstPage() && isSequenceMode()}" onclick="{gotoFirstPage}" type="button" title="{msg.firstImage}" data-toggle="tooltip" data-placement="{opts.tooltipPlacement}" aria-labelledby="firstImageLabel">
				<virtual if="{!opts.rtl}">
                	<yield from="first-page"/>
                </virtual>
                <virtual if="{opts.rtl}">
                	<yield from="last-page"/>
                </virtual>
<!--                 <img if="{!opts.rtl}" src="{opts.icons.first}"/> -->
<!--                 <img if="{opts.rtl}" src="{opts.icons.last}"/> -->
                <span id="firstImageLabel" class="labeltext">{msg.firstImage}</span>
            </button>
            <span if="{isFirstPage()}">
                <img if="{!opts.rtl}" src="{opts.icons.first}"/>
                <img if="{opts.rtl}" src="{opts.icons.last}"/>
            </span>
        </li>

        <li each="{step in opts.navigationSteps.slice().reverse()}" class="image-controls__action {currentPageNumber - step > opts.firstPageNumber ? '' : 'inactive'}">
            <virtual if="{opts.numPages > step}">
                <a if="{currentPageNumber - step > opts.firstPageNumber && !isSequenceMode()}" href="{getPageUrl(opts.currentPageNumber - step)}" title="{step + " " + msg.stepBack}" data-toggle="tooltip" data-placement="{opts.tooltipPlacement}" aria-labelledby="imageLabel-back-{step}">
                    <img if="{!opts.rtl && step == 1}" src="{opts.icons.prev}"/>
                    <img if="{opts.rtl && step == 1}" src="{opts.icons.next}"/>
                    <span if="{!opts.rtl && step > 1}">-{step}</span>
                    <span if="{opts.rtl && step > 1}">+{step}</span>
                    <span id="imageLabel-back-{step}" class="labeltext">{step + msg.stepBack}</span>
                </a>
                <button if="{currentPageNumber - step > opts.firstPageNumber && isSequenceMode()}" onclick="{navigateBack}"  type="button" title="{step + " " + msg.stepBack}" data-toggle="tooltip" data-placement="{opts.tooltipPlacement}" aria-labelledby="imageLabel-back-{step}">
                    <img if="{!opts.rtl && step == 1}" src="{opts.icons.prev}"/>
                    <img if="{opts.rtl && step == 1}" src="{opts.icons.next}"/>
                    <span if="{!opts.rtl && step > 1}">-{step}</span>
                    <span if="{opts.rtl && step > 1}">+{step}</span>
                    <span id="imageLabel-back-{step}" class="labeltext">{step + msg.stepBack}</span>
                </button>
                <span if="{currentPageNumber - step <= opts.firstPageNumber}">
                    <img if="{!opts.rtl && step == 1}" src="{opts.icons.prev}"/>
                    <img if="{opts.rtl && step == 1}" src="{opts.icons.next}"/>
                    <span if="{!opts.rtl && step > 1}">-{step}</span>
                    <span if="{opts.rtl && step > 1}">+{step}</span>
                </span>
            </virtual>
        </li>

        <li if="{opts.showDropdown}" class="image-controls__action select">
            <div class="custom-control custom-control--select">
                <select ref="dropdown" id="pageDropdown" aria-label="{msg.aria_label__select_page}" onchange="{changeDropdownValue}">
                    <option each="{item in opts.pageList}" value="{item.value}" title="{item.description ? item.description : item.label}">{item.label}</option>
                </select>
            </div>
        </li>

        <li each="{step in opts.navigationSteps}" class="image-controls__action {currentPageNumber + step < opts.lastPageNumber ? '' : 'inactive'}">
            <virtual if="{opts.numPages > step}">
                <a if="{currentPageNumber + step < opts.lastPageNumber && !isSequenceMode()}" href="{getPageUrl(opts.currentPageNumber + step)}" title="{step + " " + msg.stepForward}" data-toggle="tooltip" data-placement="{opts.tooltipPlacement}" aria-labelledby="imageLabel-forward-{step}">
                    <img if="{!opts.rtl && step == 1}" src="{opts.icons.next}"/>
                    <img if="{opts.rtl && step == 1}" src="{opts.icons.prev}"/>
                    <span if="{!opts.rtl && step > 1}">+{step}</span>
                    <span if="{opts.rtl && step > 1}">-{step}</span>
                    <span id="imageLabel-forward-{step}" class="labeltext">{step + msg.stepForward}</span>
                </a>
                <button if="{currentPageNumber + step < opts.lastPageNumber && isSequenceMode()}" onclick="{navigateForward}" type="button" title="{step + " " + msg.stepForward}" data-toggle="tooltip" data-placement="{opts.tooltipPlacement}" aria-labelledby="imageLabel-forward-{step}">
                    <img if="{!opts.rtl && step == 1}" src="{opts.icons.next}"/>
                    <img if="{opts.rtl && step == 1}" src="{opts.icons.prev}"/>
                    <span if="{!opts.rtl && step > 1}">+{step}</span>
                    <span if="{opts.rtl && step > 1}">-{step}</span>
                    <span id="imageLabel-forward-{step}" class="labeltext">{step + msg.stepForward}</span>
                </button>
                <span if="{currentPageNumber + step >= opts.lastPageNumber}">
                    <img if="{!opts.rtl && step == 1}" src="{opts.icons.next}"/>
                    <img if="{opts.rtl && step == 1}" src="{opts.icons.prev}"/>
                    <span if="{!opts.rtl && step > 1}">+{step}</span>
                    <span if="{opts.rtl && step > 1}">-{step}</span>
                </span>
            </virtual>
        </li>

        <!-- LAST PAGE -->
        <li if="{opts.numPages > 2}" class="image-controls__action {opts.rtl ? 'start' : 'end'} {isLastPage() ? 'inactive' : ''}">
            <a if="{!isLastPage() && !isSequenceMode()}" href="{getPageUrl(opts.lastPageNumber)}" title="{msg.lastImage}" data-toggle="tooltip" data-placement="{opts.tooltipPlacement}" aria-labelledby="lastImageLabel">
                <img if="{!opts.rtl}" src="{opts.icons.last}"/>
                <img if="{opts.rtl}" src="{opts.icons.first}"/>
                <span id="lastImageLabel" class="labeltext">{msg.lastImage}</span>
            </a>
            <button if="{!isLastPage() && isSequenceMode()}" onclick="{gotoLastPage}" type="button" title="{msg.lastImage}" data-toggle="tooltip" data-placement="{opts.tooltipPlacement}" aria-labelledby="lastImageLabel">
                <img if="{!opts.rtl}" src="{opts.icons.last}"/>
                <img if="{opts.rtl}" src="{opts.icons.first}"/>
                <span id="lastImageLabel" class="labeltext">{msg.lastImage}</span>
            </button>
            <span if="{isLastPage()}">
                <img if="{!opts.rtl}" src="{opts.icons.last}"/>
                <img if="{opts.rtl}" src="{opts.icons.first}"/>
            </span>
        </li>

    </virtual>

    <script>

        this.currentPageNumber = 0;
        this.msg = {};

        this.on("mount", () => {
        	console.log("this", this);
            this.currentPageNumber = this.opts.currentPageNumber;
            this.msg = this.opts.msg;
            if(this.opts.update) {
                this.opts.update.subscribe(pageNumber => {
                    this.currentPageNumber = pageNumber;
                    this.update();
                });
            }
            this.update();
        });

        this.on("update", () => {
            //hide all tooltips. Otherwise if elements are replaced after the update, old tooltips may be shown indefinitely
            $("[data-toggle='tooltip']").tooltip('hide');
            if(this.refs.dropdown) {
                this.refs.dropdown.value = this.currentPageNumber;
            }
        });

        getPageUrl(pageNo) {
            return this.opts.pageUrlTemplate(pageNo);
        }

        gotoFirstPage() {
            this.gotoPage(this.opts.firstPageNumber);
        }

        gotoLastPage() {
            this.gotoPage(this.opts.lastPageNumber);
        }

        navigateBack(e) {
            const step = e.item.step;
            this.gotoPage(this.currentPageNumber - step);
        }

        navigateForward(e) {
            const step = e.item.step;
            this.gotoPage(this.currentPageNumber + step);
        }

        gotoPage(pageNumber) {
            this.currentPageNumber = pageNumber;
            if(this.opts.onUpdate) {
                this.opts.onUpdate(pageNumber);
            }
        }

        changeDropdownValue(e) {
            let pageNo = e.target.value;
            if(this.isSequenceMode()) {
                this.gotoPage(pageNo);
            } else {
                window.location.assign(this.getPageUrl(pageNo));
            }
        }

        isSequenceMode() {
            return this.opts.navigationMode.toLowerCase() == 'sequence'
        }

        isDoublePageMode() {
            return this.opts.navigationMode.toLowerCase() == 'double'
        }

        isSinglePageMode() {
            return this.opts.navigationMode.toLowerCase() == 'single'
        }

        isFirstPage() {
            return this.currentPageNumber == this.opts.firstPageNumber;
        }

        isLastPage() {
            return this.currentPageNumber == this.opts.lastPageNumber;
        }

    </script>

</imagepaginator>