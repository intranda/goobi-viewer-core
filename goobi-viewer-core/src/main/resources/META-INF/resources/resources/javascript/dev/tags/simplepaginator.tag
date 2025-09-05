<simplepaginator>

    <div if="{opts.itemCount > 1}" class="{opts.rtl ? 'numeric-paginator -rtl' : 'numeric-paginator -ltr'} {opts.classSuffix}">

        <nav aria-label="{opts.positionBottom ? msg.aria_label__pagination_bottom : msg.aria_label__pagination_pages}">
            <ul>
                <li if="{this.currentItem > this.opts.firstItem}" class="numeric-paginator__navigate navigate_prev">
                    <a if="{isRenderLinks()}" href="{getItemUrl(currentItem-1)}" data-target="paginatorPrevPage" aria-label="{msg.aria_label__pagination_previous}">
                        <i if="{!opts.rtl}" class="fa {msg.numericPaginator_prev}" aria-hidden="true"></i>
                        <i if="{opts.rtl}" class="fa {msg.numericPaginator_next}" aria-hidden="true"></i>
                    </a>
                    <button if="{isRenderButtons()}" onclick="{navigateToPrevItem}" data-target="paginatorPrevPage" aria-label="{msg.aria_label__pagination_previous}">
                        <i if="{!opts.rtl}" class="fa {msg.numericPaginator_prev}" aria-hidden="true"></i>
                        <i if="{opts.rtl}" class="fa {msg.numericPaginator_next}" aria-hidden="true"></i>
                    </button>
                </li>
                <li each="{item in getFirstItems()}" class="numeric-paginator__navigate">
                    <a if="{isRenderLinks()}" href="{getItemUrl(item)}" aria-label="{msg.aria_label__pagination_goto}">
                        <span>{item}</span>
                    </a>
                    <button if="{isRenderButtons()}" onclick="{navigateToItem}" aria-label="{msg.aria_label__pagination_goto}">
                        <span>{item}</span>
                    </button>
                </li>
                <li class="numeric-paginator__dots" if="{isShowDotsAfterFirstItems()}"><span>...</span></li>
                <li each="{item in getCenterItems()}" class="numeric-paginator__navigate {item == currentItem ? '-active' : ''}">
                    <a if="{isRenderLinks() && item != currentItem}" href="{getItemUrl(item)}" aria-label="{msg.aria_label__pagination_goto}">
                        <span>{item}</span>
                    </a>
                    <button if="{isRenderButtons() && item != currentItem}" onclick="{navigateToItem}" aria-label="{msg.aria_label__pagination_goto}">
                        <span>{item}</span>
                    </button>
                    <span if="{item == currentItem}">{item}</span>
                </li>
                <li class="numeric-paginator__dots" if="{isShowDotsBeforeLastItems()}"><span>...</span></li>
                <li each="{item in getLastItems()}" class="numeric-paginator__navigate">
                    <a if="{isRenderLinks()}" href="{getItemUrl(item)}" aria-label="{msg.aria_label__pagination_goto}">
                        <span>{item}</span>
                    </a>
                    <button if="{isRenderButtons()}" onclick="{navigateToItem}" aria-label="{msg.aria_label__pagination_goto}">
                        <span>{item}</span>
                    </button>
                </li>
                <li if="{this.currentItem < this.opts.lastItem}" class="numeric-paginator__navigate navigate_next">
                    <a if="{isRenderLinks()}" href="{getItemUrl(currentItem+1)}" data-target="paginatorNextPage" aria-label="{msg.aria_label__pagination_next}">
                        <i if="{!opts.rtl}" class="fa {msg.numericPaginator_next}" aria-hidden="true"></i>
                        <i if="{opts.rtl}" class="fa {msg.numericPaginator_prev}" aria-hidden="true"></i>
                    </a>
                    <button if="{isRenderButtons()}" onclick="{navigateToNextItem}" data-target="paginatorNextPage" aria-label="{msg.aria_label__pagination_next}">
                        <i if="{!opts.rtl}" class="fa {msg.numericPaginator_next}" aria-hidden="true"></i>
                        <i if="{opts.rtl}" class="fa {msg.numericPaginator_prev}" aria-hidden="true"></i>
                    </button>
                </li>
            </ul>
        </nav>
    </div>

    <script>

        this.currentItem = 0;
        this.msg = {};
        this.range = 2;

        this.on("mount", () => {
            this.msg = opts.msg;
            this.currentItem = opts.itemActive;
            if(this.opts.range) {
                this.range = this.opts.range;
            }
            if(this.opts.update) {
                this.opts.update.subscribe(itemNumber => {
                    this.currentItem = itemNumber;
                    this.update();
                });
            }
            this.update();
        });

        this.on("update", () => {
            //hide all tooltips. Otherwise if elements are replaced after the update, old tooltips may be shown indefinitely
            $("[data-toggle='tooltip']").tooltip('hide');
            if(this.refs.dropdown) {
                this.refs.dropdown.value = this.currentItem;
            }
        });

        getItemUrl(itemNumber) {
            return this.opts.urlTemplate(itemNumber);
        }

        gotoFirstItem() {
            this.gotoItem(this.opts.firstItem);
        }

        gotoLastItem() {
            this.gotoItem(this.opts.lastItem);
        }

        navigateToItem(e) {
            const item = e.item.item;
            this.gotoItem(item);
        }

        navigateToPrevItem() {
            this.gotoItem(this.currentItem-1);
        }

        navigateToNextItem() {
            this.gotoItem(this.currentItem+1);
        }

        gotoItem(itemNumber) {
            this.currentItem = itemNumber;
            if(this.opts.onUpdate) {
                this.opts.onUpdate(itemNumber);
            }
        }

        isShowDotsAfterFirstItems() {
            return this.currentItem - this.range > this.opts.firstItem + this.range + 1
        }

        isShowDotsBeforeLastItems() {
            return this.currentItem + this.range < this.opts.lastItem - this.range - 1
        }

        getFirstItems() {
            let result = [];
            let firstCenterItem = this.getCenterItems()[0];
            let lastItem = Math.min(this.opts.firstItem + this.range + 1, firstCenterItem);
            for (let i = this.opts.firstItem; i < lastItem; i++) {
                result.push(i);
            }
            // console.log("get first items", result);
            return result;
        }

        getLastItems() {
            let result = [];
            let centerItems = this.getCenterItems();
            let lastCenterItem = centerItems[centerItems.length-1];
            let firstItem = Math.max(this.opts.lastItem - this.range - 1, lastCenterItem);
                for (let i = firstItem + 1; i <= this.opts.lastItem; i++) {
                    result.push(i);
                }
            return result;
        }

        getCenterItems() {
            let result = [];
            for (let i = this.currentItem - this.range; i <= this.currentItem + this.range; i++) {
                if(i >= this.opts.firstItem && i <= this.opts.lastItem) {
                    result.push(i);
                }
            }
            return result;
        }

        isRenderLinks() {
            return this.opts.navigationMode != "buttons";
        }

        isRenderButtons() {
            return this.opts.navigationMode == "buttons";
        }


        isFirstItem() {
            return this.currentItem == this.opts.firstItem;
        }

        isLastItem() {
            return this.currentItem == this.opts.lastItem;
        }
    </script>
        

</simplepaginator>