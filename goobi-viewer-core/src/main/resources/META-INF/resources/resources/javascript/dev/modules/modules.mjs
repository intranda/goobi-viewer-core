import ZoomableImage from './media/zoomableImage.mjs';
import ShareImageFragment from './media/shareImageFragment.mjs';
import Voyager3dView from './media/voyager3DViewer.mjs';

window.ShareImageFragment = ShareImageFragment;

window.zoomableImageLoaded = new rxjs.Subject();

document.addEventListener('DOMContentLoaded', () => {
    window.image = new ZoomableImage();
    window.image
        .load()
        .then((image) => {
            window.zoomableImageLoaded.next(image);
        })
        .catch((e) => {
            window.zoomableImageLoaded.error(e);
        });

    window.voyager3dView = new Voyager3dView();
});
