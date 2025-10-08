import ZoomableImage from "./media/zoomableImage.mjs";

document.addEventListener('DOMContentLoaded', () => {
    window.image = new ZoomableImage();
    window.image.load();
}); 