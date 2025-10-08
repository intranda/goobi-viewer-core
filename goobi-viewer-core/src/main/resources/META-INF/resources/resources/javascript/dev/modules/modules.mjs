import ZoomableImage from "./media/zoomableImage.mjs";

window.zoomableImageLoaded = new rxjs.Subject();

document.addEventListener('DOMContentLoaded', () => {
    window.image = new ZoomableImage();
    window.image.load()
    .then(image => {
        window.zoomableImageLoaded.next(image);
    })
    .catch(e => {
        window.zoomableImageLoaded.error(e);
    });
}); 