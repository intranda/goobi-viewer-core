/** Browser libraries the immersive view relies on, read lazily from window so tests can stub them. */

const win = () => (typeof window !== 'undefined' ? window : {});

export const env = {
    /** OpenSeadragon wrapper from imageView.min.js. */
    get ImageView() {
        return win().ImageView;
    },
    get riot() {
        return win().riot;
    },
    get rxjs() {
        return win().rxjs;
    },
    /** jQuery, needed for the Bootstrap popover API. */
    get $() {
        return win().$ || win().jQuery;
    },
    /** Legacy core namespace (TOC widget). */
    get viewerJS() {
        return win().viewerJS;
    },
    /** Image-filter labels the page script provides (iv_image.xhtml). */
    get immersiveFilterConfig() {
        return win().immersiveFilterConfig;
    },
};
