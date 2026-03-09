// navigation.js - Zoom and Pagination functionality for Verovio
// Handles zoom levels, page navigation, and UI state management

export class NavigationController {
    constructor(verovio, container, options = {}) {
        this.verovio = verovio;
        this.container = container;
        this.toolbar = null; // Will be set when bindToToolbar is called

        // Zoom settings
        this.currentScale = options.initialScale || 100;
        this.minScale = options.minScale || 25;
        this.maxScale = options.maxScale || 200;
        this.scaleStep = options.scaleStep || 25;

        // Pagination settings
        this.currentPage = 1;
        this.pageCount = 0;

        // Callbacks
        this.onZoomChange = options.onZoomChange || null;
        this.onPageChange = options.onPageChange || null;
    }

    /**
     * Update page count from Verovio
     */
    updatePageCount() {
        if (this.verovio) {
            this.pageCount = this.verovio.getPageCount();
            this.currentPage = Math.min(this.currentPage, this.pageCount);
            this.currentPage = Math.max(this.currentPage, 1);
        }
    }

    /**
     * Set the container for rendering
     * @param {Element} container - The DOM element to render into
     */
    setContainer(container) {
        this.container = container;
    }

    // ====================
    // ZOOM FUNCTIONALITY
    // ====================

    /**
     * Zoom in (decrease scale number to make content appear larger)
     */
    zoomIn() {
        const newScale = Math.max(this.currentScale - this.scaleStep, this.minScale);
        this.setZoom(newScale);
    }

    /**
     * Zoom out (increase scale number to make content appear smaller)
     */
    zoomOut() {
        const newScale = Math.min(this.currentScale + this.scaleStep, this.maxScale);
        this.setZoom(newScale);
    }

    /**
     * Reset zoom to 100%
     */
    zoomReset() {
        this.setZoom(100);
    }

    /**
     * Set specific zoom level
     * @param {number} scale - Zoom scale percentage (25-200)
     */
    setZoom(scale) {
        if (scale < this.minScale || scale > this.maxScale) {
            console.warn(`Zoom level ${scale}% is outside valid range (${this.minScale}%-${this.maxScale}%)`);
            return;
        }

        const oldScale = this.currentScale;
        this.currentScale = scale;

        if (!this.verovio) {
            console.error('Verovio not initialized');
            return;
        }

        // Update Verovio options with proper scaling
        this.verovio.setOptions({
            scale: scale,
            adjustPageHeight: true,
            pageHeight: 2970 * (scale / 100),
            pageWidth: 2100 * (scale / 100),
            spacingStaff: 12 * (scale / 100),
            spacingSystem: 12 * (scale / 100),
        });

        // Force layout recalculation after scale change
        try {
            this.verovio.redoLayout();
        } catch (error) {
            console.warn('Could not redo layout:', error);
        }

        // Update page count as it may change with scaling
        this.updatePageCount();

        // Re-render current page with new scaling
        this.renderCurrentPage();

        // Update pagination UI if toolbar is bound
        if (this.toolbar) {
            this.updatePaginationUI(this.toolbar);
        }

        // Trigger callback if provided
        if (this.onZoomChange) {
            this.onZoomChange(scale, oldScale);
        }

        console.log(`Zoom set to ${scale}% (was ${oldScale}%)`);
    }

    /**
     * Get current zoom level
     * @returns {number} Current zoom percentage
     */
    getCurrentZoom() {
        return this.currentScale;
    }

    /**
     * Get zoom range information
     * @returns {Object} Zoom range details
     */
    getZoomRange() {
        return {
            min: this.minScale,
            max: this.maxScale,
            current: this.currentScale,
            step: this.scaleStep,
        };
    }

    // ====================
    // PAGINATION FUNCTIONALITY
    // ====================

    /**
     * Navigate to previous page
     * @returns {boolean} True if navigation occurred
     */
    previousPage() {
        if (this.currentPage > 1) {
            const oldPage = this.currentPage;
            this.currentPage--;

            // Re-render the page
            this.renderCurrentPage();

            // Update pagination UI if toolbar is bound
            if (this.toolbar) {
                this.updatePaginationUI(this.toolbar);
            }

            if (this.onPageChange) {
                this.onPageChange(this.currentPage, oldPage);
            }

            console.log(`Navigated to page ${this.currentPage}/${this.pageCount}`);
            return true;
        }
        return false;
    }

    /**
     * Navigate to next page
     * @returns {boolean} True if navigation occurred
     */
    nextPage() {
        if (this.currentPage < this.pageCount) {
            const oldPage = this.currentPage;
            this.currentPage++;

            // Re-render the page
            this.renderCurrentPage();

            // Update pagination UI if toolbar is bound
            if (this.toolbar) {
                this.updatePaginationUI(this.toolbar);
            }

            if (this.onPageChange) {
                this.onPageChange(this.currentPage, oldPage);
            }

            console.log(`Navigated to page ${this.currentPage}/${this.pageCount}`);
            return true;
        }
        return false;
    }

    /**
     * Navigate to specific page
     * @param {number} pageNum - Page number (1-based)
     * @returns {boolean} True if navigation occurred
     */
    setPage(pageNum) {
        if (pageNum < 1 || pageNum > this.pageCount) {
            console.warn(`Page ${pageNum} is outside valid range (1-${this.pageCount})`);
            return false;
        }

        if (pageNum !== this.currentPage) {
            const oldPage = this.currentPage;
            this.currentPage = pageNum;

            // Render the new page
            this.renderCurrentPage();

            // Update pagination UI if toolbar is bound
            if (this.toolbar) {
                this.updatePaginationUI(this.toolbar);
            }

            if (this.onPageChange) {
                this.onPageChange(this.currentPage, oldPage);
            }

            console.log(`Navigated to page ${this.currentPage}/${this.pageCount}`);
            return true;
        }
        return false;
    }

    /**
     * Render the current page
     */
    renderCurrentPage() {
        if (!this.container || !this.verovio) {
            console.warn('Cannot render page: missing container or Verovio instance');
            return;
        }

        try {
            const svg = this.verovio.renderToSVG(this.currentPage);
            if (svg) {
                this.container.innerHTML = svg;
                // console.log(`Rendered page ${this.currentPage}/${this.pageCount}`);
            }
        } catch (error) {
            console.error('Error rendering current page:', error);
        }
    }

    /**
     * Get current page information
     * @returns {Object} Page information
     */
    getPageInfo() {
        return {
            current: this.currentPage,
            total: this.pageCount,
            hasPrevious: this.currentPage > 1,
            hasNext: this.currentPage < this.pageCount,
        };
    }

    /**
     * Get current page number
     * @returns {number} Current page number
     */
    getCurrentPage() {
        return this.currentPage;
    }

    /**
     * Get total page count
     * @returns {number} Total number of pages
     */
    getPageCount() {
        return this.pageCount;
    }

    // ====================
    // UI BINDING
    // ====================

    /**
     * Bind navigation controls to toolbar elements
     * @param {HTMLElement} toolbar - Toolbar element
     */
    bindToToolbar(toolbar) {
        if (!toolbar) {
            console.warn('No toolbar provided for navigation controls');
            return;
        }

        // Store toolbar reference for pagination UI updates
        this.toolbar = toolbar;

        // Bind zoom controls
        this.bindZoomControls(toolbar);

        // Bind pagination controls
        this.bindPaginationControls(toolbar);

        // console.log('Navigation controls bound to toolbar');
    }

    /**
     * Bind zoom controls to toolbar
     * @param {HTMLElement} toolbar - Toolbar element
     */
    bindZoomControls(toolbar) {
        const zoomInBtn = toolbar.querySelector('#verovioZoomIn');
        const zoomOutBtn = toolbar.querySelector('#verovioZoomOut');
        const zoomResetBtn = toolbar.querySelector('#verovioZoomReset');

        if (zoomInBtn) {
            zoomInBtn.disabled = false;
            zoomInBtn.addEventListener('click', () => this.zoomIn());
        }

        if (zoomOutBtn) {
            zoomOutBtn.disabled = false;
            zoomOutBtn.addEventListener('click', () => this.zoomOut());
        }

        if (zoomResetBtn) {
            zoomResetBtn.disabled = false;
            zoomResetBtn.addEventListener('click', () => this.zoomReset());
        }
    }

    /**
     * Bind pagination controls to toolbar
     * @param {HTMLElement} toolbar - Toolbar element
     */
    bindPaginationControls(toolbar) {
        const prevBtn = toolbar.querySelector('#verovioPreviousPage');
        const nextBtn = toolbar.querySelector('#verovioNextPage');

        if (prevBtn) {
            prevBtn.disabled = false;
            prevBtn.addEventListener('click', () => {
                this.previousPage();
                this.updatePaginationUI(toolbar);
            });
        }

        if (nextBtn) {
            nextBtn.disabled = false;
            nextBtn.addEventListener('click', () => {
                this.nextPage();
                this.updatePaginationUI(toolbar);
            });
        }

        // Initial UI update
        this.updatePaginationUI(toolbar);
    }

    /**
     * Update pagination UI elements
     * @param {HTMLElement} toolbar - Toolbar element
     */
    updatePaginationUI(toolbar) {
        if (!toolbar) return;

        const prevBtn = toolbar.querySelector('#verovioPreviousPage');
        const nextBtn = toolbar.querySelector('#verovioNextPage');
        const currentPageSpan = toolbar.querySelector('.editor-page-current');
        const pageCountSpan = toolbar.querySelector('.editor-page-count');

        // Update page indicators
        if (currentPageSpan) {
            currentPageSpan.textContent = this.currentPage;
        }
        if (pageCountSpan) {
            pageCountSpan.textContent = this.pageCount;
        }

        // Update button states
        if (prevBtn) {
            prevBtn.disabled = this.currentPage <= 1;
        }
        if (nextBtn) {
            nextBtn.disabled = this.currentPage >= this.pageCount;
        }
    }

    /**
     * Reset navigation state
     */
    reset() {
        this.currentPage = 1;
        this.pageCount = 0;
        this.currentScale = 100;
    }
}
