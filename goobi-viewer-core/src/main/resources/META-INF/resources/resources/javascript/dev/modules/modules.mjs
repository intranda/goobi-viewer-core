import ZoomableImage from "./media/zoomableImage.mjs";
import ShareImageFragment from "./media/shareImageFragment.mjs";

window.ShareImageFragment = ShareImageFragment;

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