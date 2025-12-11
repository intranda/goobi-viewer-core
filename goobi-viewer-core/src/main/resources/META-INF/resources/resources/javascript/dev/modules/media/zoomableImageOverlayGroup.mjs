import ZoomableImage from "./zoomableImage.mjs";

const _debug = false;

const _config = {
    styleclass: "imageview-overlay",
    showTooltip: false,
    highlightClassName: "highlight",
    highlightOnHover: false
}

export default class ZoomableImageOverlayGroup {

    /**
     * @param {ZoomableImage} image 
     * @param {Array} overlaySources 
     * @param {Object} config
     */
    constructor(image, overlaySources, config) {

        this.config = jQuery.extend(true, {}, _config, config);
        if(_debug)console.log("initialize overlays", overlaySources, this.config);

        this.overlayGroup = new ImageView.OverlayGroup(image.viewer, {
            className: this.config.styleclass,
            tooltipClassName:  this.config.styleclass + " tooltip",
            highlightClassName:  this.config.highlightClassName,
            highlightOnHover: this.config.highlightOnHover
        }); 

        this.overlays = createOverlays(overlaySources, image.getCurrentTileSourceId());
    }

    show() {
        if(_debug)console.log("show in viewer ", this.overlays);
        this.overlayGroup.addToViewer(this.overlays);
    }

    hide() {
        this.overlayGroup.removeFromViewer(this.overlays);
    }
}



function createOverlays(coords, target) {
    const overlays = [];
    if(Array.isArray(coords)) {
        coords.forEach(coord => {
            if(Array.isArray(coord)) {
                overlays.push({
                    target: target,
                    coordinates: [coord[0], coord[1], coord[2] - coord[0], coord[3] - coord[1]],
                    tooltip: coord.length > 4 ? coord[4]: undefined,
                    id: coord.length > 5 ? coord[5]: undefined,
                });
            }
        });
    }
    return overlays;
}