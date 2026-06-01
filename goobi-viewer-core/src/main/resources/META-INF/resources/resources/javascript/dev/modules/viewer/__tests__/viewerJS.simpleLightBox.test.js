/**
 * Unit tests for viewerJS.simpleLightBox.
 *
 * The init() method binds a click listener on every `.lightbox-image`
 * present at call time. We set up the fixture, call init(), and then
 * trigger clicks. The internal helper functions (_setupLightBox,
 * _centerModalBox, etc.) are closure-private; they are exercised
 * through the public click flow.
 *
 * jQuery animations are forced to commit synchronously so that the
 * fadeIn() inside the click handler does not leave the overlay
 * mid-animation between assertions.
 */
$.fx.off = true;

const viewerJS = require('../viewerJS.simpleLightBox.js');

beforeEach(() => {
    document.body.innerHTML = `
        <a href="#"
           class="lightbox-image"
           data-imgpath="/img/path/"
           data-imgname="big.jpg">Open</a>`;
    viewerJS.simpleLightBox.init();
});

describe('viewerJS.simpleLightBox click flow', () => {
    test('clicking a .lightbox-image appends a .lightbox-overlay to the body', () => {
        expect(document.querySelector('.lightbox-overlay')).toBeNull();
        $('.lightbox-image').trigger('click');
        expect(document.querySelector('.lightbox-overlay')).not.toBeNull();
    });

    test('the overlay contains an <img> with src = imgpath + imgname', () => {
        $('.lightbox-image').trigger('click');
        const img = document.querySelector('.lightbox-overlay img');
        expect(img).not.toBeNull();
        expect(img.getAttribute('src')).toBe('/img/path/big.jpg');
        expect(img.getAttribute('alt')).toBe('big.jpg');
    });

    test('clicking the .lightbox-close-btn removes the overlay', () => {
        $('.lightbox-image').trigger('click');
        $('.lightbox-close-btn').trigger('click');
        expect(document.querySelector('.lightbox-overlay')).toBeNull();
    });

    test('the modal box is centered via inline margin-top / margin-left offsets', () => {
        $('.lightbox-image').trigger('click');
        const box = document.querySelector('.lightbox-modal-box');
        // Under jsdom outerWidth/outerHeight are 0 (no layout), so the
        // computed offset is 0; but the inline-style properties must be set
        // as proof that _centerModalBox ran. Match both "0px" and "-0px".
        expect(box.style.marginTop).toMatch(/^-?0px$/);
        expect(box.style.marginLeft).toMatch(/^-?0px$/);
    });
});
