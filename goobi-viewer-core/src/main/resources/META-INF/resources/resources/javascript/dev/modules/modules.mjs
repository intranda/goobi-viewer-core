import ZoomableImage from './media/zoomableImage.mjs';
import ShareImageFragment from './media/shareImageFragment.mjs';
import Voyager3dView from './media/voyager3DViewer.mjs';
import { initImmersiveViewer } from './immersive/ivInit.mjs';

window.ShareImageFragment = ShareImageFragment;

window.zoomableImageLoaded = new rxjs.Subject();

document.addEventListener('DOMContentLoaded', () => {
    if (document.querySelector('[data-image="zoomable"]')) {
        window.image = new ZoomableImage();
        window.image
            .load()
            .then((image) => {
                window.zoomableImageLoaded.next(image);
            })
            .catch((e) => {
                window.zoomableImageLoaded.error(e);
            });
    }

    window.voyager3dView = new Voyager3dView();

    // Wire the immersive chrome whenever the view is present; the image viewer
    // itself is only built when the user has VIEW_IMAGES (see ivInit).
    const immersiveRoot = document.querySelector('.immersive');
    if (immersiveRoot) {
        initImmersiveViewer(immersiveRoot);
    }
});
