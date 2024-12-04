<imageControls>

<nav id="imageControls" aria-label="{msg.aria_label__nav_image_controls}" class="{opts.rtl ? 'image-controls -rtl' : 'image-controls -ltr'}">
                <div class="image-controls__actions">
                    <ul>
                        <virtual if="{opts.enablePageNavigation}">

                            <!-- FIRST PAGE -->
                            <li if="{opts.numPages > 2}" class="image-controls__action {opts.rtl ? 'end' : 'start'} {isFirstPage() ? 'inactive' : ''}">
                                <a if="{!isFirstPage() && !isSequenceMode()}" href="{getPageUrl(opts.firstPageNumber)}" title="{msg.firstImage}" data-toggle="tooltip" data-placement="{opts.tooltipPlacement}" aria-labelledby="firstImageLabel">
                                    <img if="{!opts.rtl}" src="{opts.icons.first}"/>
                                    <img if="{opts.rtl}" src="{opts.icons.last}"/>
                                    <span id="firstImageLabel" class="labeltext">{msg.firstImage}</span>
                                </a>
                                <button if="{!isFirstPage() && isSequenceMode()}" click="{gotoPage(opts.firstPageNumber)}" title="{msg.firstImage}" data-toggle="tooltip" data-placement="{opts.tooltipPlacement}" aria-labelledby="firstImageLabel">
                                    <img if="{!opts.rtl}" src="{opts.icons.first}"/>
                                    <img if="{opts.rtl}" src="{opts.icons.last}"/>
                                    <span id="firstImageLabel" class="labeltext">{msg.firstImage}</span>
                                </button>
                                <span if="{isFirstPage()}">
                                    <img if="{!opts.rtl}" src="{opts.icons.first}"/>
                                    <img if="{opts.rtl}" src="{opts.icons.last}"/>
                                </span>
                            </li>

                            <li each="#{step in opts.navigationSteps.reverse()}" class="image-controls__action {currentPageNumber - step > opts.firstPageNumber ? '' : 'inactive'}">
                                <virtual if="{opts.numPages > step}">
                                    <a if="{currentPageNumber - step > opts.firstPageNumber && !isSequenceMode()}" href="{getPageUrl(opts.currentPageNumber - step)}" title="{step + msg.stepBack}" data-toggle="tooltip" data-placement="{opts.tooltipPlacement}" aria-labelledby="imageLabel-back-{step}">
                                        <img if="{!opts.rtl && step == 1}" src="{opts.icons.prev}"/>
                                        <img if="{opts.rtl && step == 1}" src="{opts.icons.next}"/>
                                        <span if="{!opts.rtl && step > 1}">-{step}</span>
                                        <span if="{opts.rtl && step > 1}">+{step}</span>
                                        <span id="imageLabel-back-{step}" class="labeltext">{step + msg.stepBack}</span>
                                    </a>
                                    <button if="{currentPageNumber - step > opts.firstPageNumber && isSequenceMode()}" click="{gotoPage(opts.currentPageNumber - step)}" title="{step + msg.stepBack}" data-toggle="tooltip" data-placement="{opts.tooltipPlacement}" aria-labelledby="imageLabel-back-{step}">
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
                                    <select ref="dropdown" id="pageDropdown" aria-label="{msg.aria_label__select_page}" change="{changeDropdownValue}">
                                        <option each="{item in opts.pageList}" value="{item.value}" title="{item.description ? item.description : item.label}">{item.label}</option>
                                    </select>
                                </div>
                            </li>

                            <li each="#{step in opts.navigationSteps}" class="image-controls__action {currentPageNumber + step < opts.lastPageNumber ? '' : 'inactive'}">
                                <virtual if="{opts.numPages > step}">
                                    <a if="{currentPageNumber + step < opts.lastPageNumber && !isSequenceMode()}" href="{getPageUrl(opts.currentPageNumber + step)}" title="{step + msg.stepForward}" data-toggle="tooltip" data-placement="{opts.tooltipPlacement}" aria-labelledby="imageLabel-forward-{step}">
                                        <img if="{!opts.rtl && step == 1}" src="{opts.icons.next}"/>
                                        <img if="{opts.rtl && step == 1}" src="{opts.icons.prev}"/>
                                        <span if="{!opts.rtl && step > 1}">+{step}</span>
                                        <span if="{opts.rtl && step > 1}">-{step}</span>
                                        <span id="imageLabel-forward-{step}" class="labeltext">{step + msg.stepForward}</span>
                                    </a>
                                    <button if="{currentPageNumber + step < opts.lastPageNumber && isSequenceMode()}" click="{gotoPage(opts.currentPageNumber + step)}" title="{step + msg.stepForward}" data-toggle="tooltip" data-placement="{opts.tooltipPlacement}" aria-labelledby="imageLabel-forward-{step}">
                                        <img if="{!opts.rtl && step == 1}" src="{opts.icons.next}"/>
                                        <img if="{opts.rtl && step == 1}" src="{opts.icons.prev}"/>
                                        <span if="{!opts.rtl && step > 1}">+{step}</span>
                                        <span if="{opts.rtl && step > 1}">-{step}</span>
                                        <span id="imageLabel-forward-{step}" class="labeltext">{step + msg.stepForward}</span>
                                    </button>
                                    <span if="{currentPageNumber + step < opts.lastPageNumber}">
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
                                <button if="{!isLastPage() && isSequenceMode()}" click="{gotoPage(opts.lastPageNumber)}" title="{msg.lastImage}" data-toggle="tooltip" data-placement="{opts.tooltipPlacement}" aria-labelledby="lastImageLabel">
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


                            <!-- DOUBLE PAGE VIEW -->
                            <ui:fragment
                                rendered="#{activeDocumentBean.viewManager.doublePageNavigationEnabled and pageCount gt 1 and activeDocumentBean.viewManager.currentPage.baseMimeType.name == 'image' and !activeDocumentBean.viewManager.doublePageMode and navigationHelper.currentPage != 'fulltext' and !activeDocumentBean.viewManager.currentPage.doubleImage}">
                                <li class="image-controls__action double-page-view">
                                    <h:commandLink action="#{activeDocumentBean.setDoublePageModeAction(true)}" title="#{msg.doublePageView}">
                                        <ui:include src="#{navigationHelper.getResource('images/icons/icon_ic-double-page.svg')}"/>
                                        <span class="labeltext">#{msg.doublePageView}</span>
                                        <f:passThroughAttribute name="data-toggle" value="tooltip" />
                                        <f:passThroughAttribute name="data-placement" value="#{cc.attrs.tooltipPlacement}" />
                                        <f:passThroughAttribute name="aria-label" value="#{msg.doublePageView}" />
                                    </h:commandLink>
                                </li>
                            </ui:fragment>
                            <ui:fragment rendered="#{activeDocumentBean.viewManager.doublePageMode and navigationHelper.currentPage != 'fulltext'}">
                                <!-- SINGLE PAGE VIEW -->
                                <li class="image-controls__action single-page-view">
                                    <h:commandLink action="#{activeDocumentBean.setDoublePageModeAction(false)}" title="#{msg.singlePageView}">
                                        <ui:include src="#{navigationHelper.getResource('images/icons/icon_ic-single-page.svg')}" />
                                        <span class="labeltext">#{msg.singlePageView}</span>
                                        <f:passThroughAttribute name="data-toggle" value="tooltip" />
                                        <f:passThroughAttribute name="data-placement" value="#{cc.attrs.tooltipPlacement}" />
                                        <f:passThroughAttribute name="aria-label" value="#{msg.singlePageView}" />
                                    </h:commandLink>
                                </li>
                                <!-- TOGGLE RECTO/VERSO -->
                                <li class="image-controls__action switch-pages">
                                    <h:commandLink action="#{activeDocumentBean.viewManager.togglePageOrientation()}" title="#{msg.switchPages}">
                                        <ui:include src="#{navigationHelper.getResource('images/icons/icon_ic-switch-pages.svg')}" />
                                        <span class="labeltext">#{msg.switchPages}</span>
                                        <f:passThroughAttribute name="data-toggle" value="tooltip" />
                                        <f:passThroughAttribute name="data-placement" value="#{cc.attrs.tooltipPlacement}" />
                                    </h:commandLink>
                                </li>
                            </ui:fragment>
                        <ui:fragment rendered="#{navigationHelper.currentPage != 'fulltext'}">
                            <!-- ROTATE -->
                            <li class="image-controls__action rotate-left">
                                <button
									type="button"
									class="btn btn--clear"
									data-toggle="tooltip"
									data-placement="#{cc.attrs.tooltipPlacement}"
									title="#{msg.rotateLeft}"
									tabindex="0"
									aria-label="#{msg.rotateLeft}">
                                    <ui:include src="#{navigationHelper.getResource('images/icons/icon_ic-rotate-left.svg')}" />
                                    <span class="labeltext">#{msg.rotateLeft}</span>
                                </button>
                            </li>
                            <li class="image-controls__action rotate-right">
                                <button
									type="button"
									class="btn btn--clear"
									data-toggle="tooltip"
									data-placement="#{cc.attrs.tooltipPlacement}"
									title="#{msg.rotateRight}"
									tabindex="0"
									aria-label="#{msg.rotateRight}">
                                    <ui:include src="#{navigationHelper.getResource('images/icons/icon_ic-rotate-right.svg')}" />
                                    <span class="labeltext">#{msg.rotateRight}</span>
                                </button>
                            </li>
                            <!-- RESET -->
                            <li class="image-controls__action reset">
                                <button
									type="button"
									class="btn btn--clear"
									data-toggle="tooltip"
									data-placement="#{cc.attrs.tooltipPlacement}"
									title="#{msg.resetImage}"
									tabindex="0"
									aria-label="#{msg.resetImage}">
                                    <ui:include src="#{navigationHelper.getResource('images/icons/icon_ic-reset.svg')}" />
                                    <span class="labeltext">#{msg.resetImage}</span>
                                </button>
                            </li>
                            <!-- FULLSCREEN -->
                            <ui:fragment rendered="#{navigationHelper.currentPage != 'fullscreen'}">
                                <li class="image-controls__action enter-fullscreen">
                                    <h:outputLink value="#{activeDocumentBean.fullscreenImageUrl}" title="#{msg.fullscreen_enter}">
                                        <ui:include src="#{navigationHelper.getResource('images/icons/icon_ic-fullscreen.svg')}" />
                                        <span id="fullScreenLabel" class="labeltext">#{msg.enterFullscreen}</span>
                                        <f:passThroughAttribute name="data-toggle" value="tooltip" />
                                        <f:passThroughAttribute name="data-placement" value="#{cc.attrs.tooltipPlacement}" />
                                        <f:passThroughAttribute name="aria-labelledby" value="fullScreenLabel" />
                                    </h:outputLink>
                                </li>
                            </ui:fragment>
                            <!-- ZOOMSLIDER -->
                            <!-- TODO: rendered Anweisung, wenn pageBrowse in der config aktiv -->
                            <ui:fragment rendered="#{activeDocumentBean.viewManager.currentPage.baseMimeType.name == 'image' and activeDocumentBean.viewManager.currentPage.accessPermissionImageZoom}">
                                <li class="image-controls__action zoom-slider-wrapper">
                                	<input type="range" min="0" max="1" value="0" step="0.01" class="slider zoom-slider" aria-label="#{msg.aria_label__zoom_slider}"/>
                                </li>
<!--                                 <li> -->
<!--                                 	<input type="number" class="zoom-slider-label"/>% -->
<!--                                 </li> -->
                            </ui:fragment>
                        </ui:fragment>
                        <ui:fragment rendered="#{navigationHelper.currentPage == 'fulltext'}">
                        	<li class="image-controls__action enter-fullscreen">
                                <h:outputLink value="#{activeDocumentBean.fullscreenImageUrl}?activetab=fulltext" title="#{msg.fullscreen_enter}">
                                    <ui:include src="#{navigationHelper.getResource('images/icons/icon_ic-fullscreen.svg')}" />
                                    <span id="fullScreenLabel" class="labeltext">#{msg.enterFullscreen}</span>
                                    <f:passThroughAttribute name="data-toggle" value="tooltip" />
                                    <f:passThroughAttribute name="data-placement" value="#{cc.attrs.tooltipPlacement}" />
                                    <f:passThroughAttribute name="aria-labelledby" value="fullScreenLabel" />
                                </h:outputLink>
                            </li>
                        </ui:fragment>

                    </ul>
                </div>

                <!-- DOCUMENT OPTIONS -->
                <div class="image-controls__options">
                    <ul>
                        <!-- CROWDSOURCING TRANSCRIPTION MODULE -->
                        <ui:fragment rendered="#{configurationBean.displayCrowdsourcingModuleLinks}">
                            <ui:fragment rendered="#{navigationHelper.currentPage == 'object'}">
                                <li class="image-controls__option -ltr">
                                    <a href="#{navigationHelper.applicationUrl}crowd/#{activeDocumentBean.viewManager.pi}/#{activeDocumentBean.viewManager.currentImageOrder}">#{msg.action__crowdsourcing_participate}!</a>
                                </li>
                            </ui:fragment>
                            <ui:fragment rendered="#{navigationHelper.currentPage == 'fulltext'}">
                                <li class="image-controls__option">
                                    <a href="#{navigationHelper.applicationUrl}crowd/editOcr/#{activeDocumentBean.viewManager.pi}/#{activeDocumentBean.viewManager.currentImageOrder}">#{msg.action__edit_text}</a>
                                </li>
                            </ui:fragment>
                        </ui:fragment>

                        <!-- CROWDSOURCING CAMPAIGN -->
                        <ui:fragment>
                            <ui:repeat var="campaign" value="#{crowdsourcingBean.getActiveCampaignsForRecord(activeDocumentBean.viewManager.pi)}">
<!--                                 <ui:fragment rendered="#{campaign.mayAnnotate(userBean.user, activeDocumentBean.viewManager.pi) and campaign.isUserAllowedAction(userBean.user, 'ANNOTATE')}"> -->
                                	<li class="image-controls__option">
	                                    <h:commandLink
	                                		target="_blank" action="#{crowdsourcingBean.forwardToCrowdsourcingAnnotation(campaign, activeDocumentBean.viewManager.pi)}"
	                                        styleClass="btn btn--icon">
	                                        <f:passThroughAttribute name="data-toggle" value="tooltip" />
	                                    	<f:passThroughAttribute name="data-placement" value="#{cc.attrs.tooltipPlacement}" />
	                                        <f:passThroughAttribute name="data-original-title" value="#{msg.label__crowdsourcing_campaign_record_discriminator}: #{campaign.getTitle(navigationHelper.locale, true)}" />
	                                        <f:passThroughAttribute name="aria-label" value="#{msg.label__crowdsourcing_campaign_record_discriminator}: #{campaign.getTitle(navigationHelper.locale, true)}" />

	                                        <i class="fa fa-lightbulb-o" aria-hidden="true"></i>
	                                    </h:commandLink>
                                    </li>
<!--                                 </ui:fragment> -->
                            </ui:repeat>
                        </ui:fragment>

                        <ui:fragment rendered="#{navigationHelper.currentPage != 'fulltext'}">
                            <!-- FULLTEXT -->
                            <ui:fragment rendered="#{activeDocumentBean.viewManager.belowFulltextThreshold and activeDocumentBean.viewManager.currentPage.fulltextAvailable}">
                                <li class="image-controls__option fulltext">
                                    <h:outputLink styleClass="btn btn--icon" value="#{navigationHelper.fulltextActiveUrl}/#{activeDocumentBean.persistentIdentifier}/#{activeDocumentBean.imageToShow}/"
                                        title="#{msg.fulltext}" target="_blank">
                                            <i class="fa fa-file-text-o" aria-hidden="true"></i>
                                        <f:passThroughAttribute name="data-toggle" value="tooltip" />
                                        <f:passThroughAttribute name="data-placement" value="#{cc.attrs.tooltipPlacement}" />
                                        <f:passThroughAttribute name="aria-label" value="#{msg.fulltext}" />
                                    </h:outputLink>
                                </li>
                            </ui:fragment>

                            <!-- SHARE IMAGE REGION -->
                            <ui:fragment rendered="#{activeDocumentBean.viewManager.currentPage.baseMimeType.name == 'image' and !activeDocumentBean.viewManager.doublePageMode}">
                                <li class="image-controls__option share-image-region">
                                    <a role="button" title="#{msg.action__share_image_region}" data-toggle="tooltip" data-placement="top"
                                    data-popover-element="#share-image-area-popup" aria-label="#{msg.action__share_image_region}">
	                                    <i class="fa fa-crop" aria-hidden="true"></i>
                                    </a>
                                </li>
                            </ui:fragment>

                            <!-- IMAGE FILTER -->
                            <ui:fragment rendered="#{activeDocumentBean.viewManager.currentPage.baseMimeType.name == 'image'}">
                                <li class="image-controls__option image-filter">
                                    <button type="button" class="btn btn--icon" disabled="disabled"
                                	   data-popover-element="#imageFilterPopover"
                                       data-popover-dismiss="click-outside"
                                       aria-label="#{msg.label__image_filter_toolkit}"
		                               data-toggle="tooltip"
	                                	title="#{msg.label__image_filter_toolkit__error}">
	                                   <i class="fa fa-wrench" aria-hidden="true"></i>
                                    </button>
                                </li>
                            </ui:fragment>
                        </ui:fragment>

                        <!-- ADD TO BOOKMARK -->
                        <ui:fragment rendered="#{configurationBean.bookmarksEnabled}">
                            <li class="image-controls__option add-to-bookmark">
                                <button type="button" class="btn btn--icon"
                                    data-bookmark-list-type="add"
                                    data-pi="#{activeDocumentBean.viewManager.pi}"
                                    data-page="#{activeDocumentBean.viewManager.currentImageOrder}"
                                    role="switch"
                                    aria-label="#{msg.bookmarkList_addToBookmarkList}">
                                    <span data-bookmark-list-title-add="#{msg.bookmarkList_addToBookmarkList}" data-bookmark-list-title-added="#{msg.bookmarkList_removeFromBookmarkList}" data-toggle="tooltip" data-placement="#{cc.attrs.tooltipPlacement}">
                                        <i class="fa #{msg.bookmarkList_icon}" aria-hidden="true" ></i>
                                        <i class="fa #{msg.bookmarkList_iconAdded}" aria-hidden="true"></i>
                                    </span>
                                </button>
                            </li>
                        </ui:fragment>

                    </ul>
                </div>
        </nav>

</script>

    this.currentPageNumber = 0;

    this.on("mount", () => {
        this.currentPageNumber = this.opts.currentPageNumber;
        this.msg = this.opts.msg;
        if(this.opts.update) {
            this.opts.update.subscribe(pageNumber => {
                this.currentPageNumber = pageNumber;
                this.update();
            });
        }
    }

    this.on("update", () => {
        if(this.refs.dropdown) {
            this.refs.dropdown.value=this.currentPageNumber;
        }
    });

    getPageUrl(pageNo) {
        return this.opts.pageUrlTemplate(pageNo);
    }

    gotoPage(pageNumber) {
        this.currentPageNumber = pageNumber;
        if(this.opts.onUpdate) {
            this.opts.onUpdate(pageNumber);
        }
    }

    changeDropdownValue(e) {
        let pageNo = e.item.value;
        if(this.isSequenceMode()) {
            gotoPage(pageNo);
        } else {
            window.location.assign(getPageUrl(pageNo));
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

    istFirstPage() {
        return this.currentPageNumber == this.opts.firstPageNumber;
    }

    istLastPage() {
        return this.currentPageNumber == this.opts.lastPageNumber;
    }



</script>

</imageControls>