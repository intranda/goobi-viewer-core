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
		  if ((navigator.userAgent.indexOf("Opera") || navigator.userAgent.indexOf('OPR')) != -1) {
		    return 'Opera';
		  } else if (navigator.userAgent.indexOf("Edg") != -1) {
		    return 'Edge';
		  } else if (navigator.userAgent.indexOf("Chrome") != -1) {
		    return 'Chrome';
		  } else if (navigator.userAgent.indexOf("Safari") != -1) {
		    return 'Safari';
		  } else if (navigator.userAgent.indexOf("Firefox") != -1) {
		    return 'Firefox';
			// IF IE > 10
		  } else if ((navigator.userAgent.indexOf("MSIE") != -1) || (!!document.documentMode == true)) {
		     return 'IE';
		  } else {
		    return 'unknown';
		  }
}

setupBrowserSupport();
