/** The stage placeholder for pages the user may not see, and for a stage that could not be loaded. */

/**
 * Builds the placeholder box: the server-rendered template's children when given, else a plain text.
 *
 * @param {object} opts
 * @param {HTMLElement|null} [opts.template]  element whose children are cloned (admin-configured image and text)
 * @param {string} [opts.text]  fallback text
 * @param {string} [opts.role='status']
 * @returns {HTMLElement}
 */
export function buildDeniedPlaceholder({ template, text, role = 'status' }) {
    const box = document.createElement('div');
    box.className = 'immersive__page-denied';
    box.setAttribute('role', role);
    const inner = document.createElement('div');
    inner.className = 'immersive__page-denied-inner';
    if (template && template.children.length) {
        for (const child of template.children) inner.appendChild(child.cloneNode(true));
    } else {
        const span = document.createElement('span');
        span.className = 'immersive__page-denied-text';
        span.textContent = text || '';
        inner.appendChild(span);
    }
    box.appendChild(inner);
    return box;
}

/**
 * The access-denied overlay of the stage: built once on first need, then toggled.
 *
 * @param {object} opts
 * @param {HTMLElement|null} opts.host  the OSD mount element
 * @param {HTMLElement|null} [opts.template]
 * @param {string} [opts.text]
 * @returns {{sync: (denied: boolean) => void, remove: Function}}
 */
export function createAccessOverlay({ host, template, text }) {
    let overlay = null;
    return {
        sync(denied) {
            if (!host) return;
            if (!denied) {
                if (overlay) overlay.hidden = true;
                return;
            }
            if (!overlay) {
                overlay = buildDeniedPlaceholder({ template, text });
                if (getComputedStyle(host).position === 'static') host.style.position = 'relative';
                host.appendChild(overlay);
            }
            overlay.hidden = false;
        },
        remove() {
            if (overlay) overlay.remove();
            overlay = null;
        },
    };
}
