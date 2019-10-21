/**
 * Methods to detect current browser and handle compatibility issues. This file must be compatible with older javascript versions
 */
function setupBrowserSupport() {
    var browser = getCurrentBrowser();
    switch (browser) {
        case 'Chrome':
            break;
        case 'Firefox':
            break;
        case 'IE':
            /* SET IE CLASS TO HTML */
            $('html').addClass('is-IE');
            break;
        case 'Edge':
            break;
        case 'Safari':
            break;
    }
}

function getCurrentBrowser() {
            // Opera 8.0+
            var isOpera = ( !!window.opr && !!opr.addons ) || !!window.opera || navigator.userAgent.indexOf( ' OPR/' ) >= 0;
            // Firefox 1.0+
            var isFirefox = typeof InstallTrigger !== 'undefined';
            // Safari 3.0+ "[object HTMLElementConstructor]"
            var isSafari = /constructor/i.test( window.HTMLElement ) || ( function( p ) {
                return p.toString() === "[object SafariRemoteNotification]";
            } )( !window[ 'safari' ] || ( typeof safari !== 'undefined' && safari.pushNotification ) );
            // Internet Explorer 6-11
            var isIE = /* @cc_on!@ */false || !!document.documentMode;
            // Edge 20+
            var isEdge = !isIE && !!window.StyleMedia;
            // Chrome 1+
            var isChrome = !!window.chrome;
            // Blink engine detection
            // var isBlink = ( isChrome || isOpera ) && !!window.CSS;
            
            if ( isOpera ) {
                return 'Opera';
            }
            else if ( isFirefox ) {
                return 'Firefox';
            }
            else if ( isSafari ) {
                return 'Safari';
            }
            else if ( isIE ) {
                return 'IE';
            }
            else if ( isEdge ) {
                return 'Edge';
            }
            else if ( isChrome ) {
                return 'Chrome';
            }
        }

setupBrowserSupport();
