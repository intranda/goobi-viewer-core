/**
 * Verovio Toolbar Integration Module
 * Handles UI binding and toolbar functionality for Verovio editor
 */

/**
 * Initialize complete toolbar functionality
 * @param {Object} verovioApp - The main Verovio application instance
 */
export function initializeToolbar(verovioApp) {
    // First try to use existing toolbar controls
    const toolbar = document.querySelector('[data-editor="verovio-toolbar"]');

    if (toolbar) {
        enableToolbarControls(toolbar, verovioApp);
    } else {
        // Create floating toolbar if none exists
        createFloatingToolbar(verovioApp);
    }
}

/**
 * Enable and connect existing toolbar controls
 * @param {Element} toolbar - The toolbar DOM element
 * @param {Object} verovioApp - The main Verovio application instance
 */
function enableToolbarControls(toolbar, verovioApp) {
    // Enable download controls
    bindDownloadControls(toolbar, verovioApp);

    // Enable MIDI controls
    bindMidiControls(toolbar, verovioApp);

    // Bind navigation controller to toolbar for zoom and pagination controls
    if (verovioApp.navigationController) {
        verovioApp.navigationController.bindToToolbar(toolbar);
    }
}

/**
 * Bind zoom control buttons
 * @param {Element} toolbar - The toolbar DOM element
 * @param {Object} verovioApp - The main Verovio application instance
 */
function bindZoomControls(toolbar, verovioApp) {
    const zoomInBtn = toolbar.querySelector('#verovioZoomIn');
    const zoomOutBtn = toolbar.querySelector('#verovioZoomOut');
    const zoomResetBtn = toolbar.querySelector('#verovioZoomReset');

    if (zoomInBtn) {
        zoomInBtn.disabled = false;
        zoomInBtn.addEventListener('click', () => {
            verovioApp.zoomIn();
        });
    }

    if (zoomOutBtn) {
        zoomOutBtn.disabled = false;
        zoomOutBtn.addEventListener('click', () => {
            verovioApp.zoomOut();
        });
    }

    if (zoomResetBtn) {
        zoomResetBtn.disabled = false;
        zoomResetBtn.addEventListener('click', () => {
            verovioApp.zoomReset();
        });
    }
}

/**
 * Bind pagination control buttons
 * @param {Element} toolbar - The toolbar DOM element
 * @param {Object} verovioApp - The main Verovio application instance
 */
function bindPaginationControls(toolbar, verovioApp) {
    const prevBtn = toolbar.querySelector('#verovioPreviousPage');
    const nextBtn = toolbar.querySelector('#verovioNextPage');

    if (prevBtn) {
        prevBtn.disabled = false;
        prevBtn.addEventListener('click', () => {
            verovioApp.previousPage();
        });
    }

    if (nextBtn) {
        nextBtn.disabled = false;
        nextBtn.addEventListener('click', () => {
            verovioApp.nextPage();
        });
    }
}

/**
 * Bind download control buttons
 * @param {Element} toolbar - The toolbar DOM element
 * @param {Object} verovioApp - The main Verovio application instance
 */
function bindDownloadControls(toolbar, verovioApp) {
    const downloadMEIBtn = toolbar.querySelector('#verovioDownloadMEI');
    const downloadMIDIBtn = toolbar.querySelector('#verovioDownloadMIDI');

    if (downloadMEIBtn) {
        downloadMEIBtn.disabled = false;
        downloadMEIBtn.addEventListener('click', () => {
            verovioApp.exportMEI();
        });
    }

    if (downloadMIDIBtn) {
        downloadMIDIBtn.disabled = false;
        downloadMIDIBtn.addEventListener('click', () => {
            verovioApp.exportMIDI();
        });
    }
}

/**
 * Bind MIDI playback control buttons
 * @param {Element} toolbar - The toolbar DOM element
 * @param {Object} verovioApp - The main Verovio application instance
 */
export function bindMidiControls(toolbar, verovioApp) {
    const playBtn = toolbar.querySelector('#verovioPlayMIDI');
    const pauseBtn = toolbar.querySelector('#verovioPauseMIDI');
    const stopBtn = toolbar.querySelector('#verovioStopMIDI');

    if (playBtn) {
        playBtn.disabled = false;
        playBtn.addEventListener('click', async () => {
            // Dynamic import to avoid circular dependencies
            const playback = await import('./playback.js');

            // Check if we're currently paused - if so, resume instead of starting new playback
            if (playback.isPlayingMidi() && playback.isPausedMidi()) {
                await verovioApp.pauseMidi(); // This will resume playback since we're currently paused
            } else {
                await verovioApp.playMEI(); // Start new playback
            }
        });
    }

    if (pauseBtn) {
        pauseBtn.disabled = false;
        pauseBtn.addEventListener('click', async () => {
            await verovioApp.pauseMidi();
        });
    }

    if (stopBtn) {
        stopBtn.disabled = false;
        stopBtn.addEventListener('click', async () => {
            await verovioApp.stopMidi();
        });
    }

    // Initialize scrubber in the toolbar context
    verovioApp.initializeScrubber();
}

/**
 * Update MIDI button visibility based on playback state
 */
export async function updateMidiButtonStates() {
    // Import playback module dynamically
    const playback = await import('./playback.js');

    const toolbar = document.querySelector('[data-editor="verovio-toolbar"]');
    if (!toolbar) return;

    const playBtn = toolbar.querySelector('#verovioPlayMIDI');
    const pauseBtn = toolbar.querySelector('#verovioPauseMIDI');
    const stopBtn = toolbar.querySelector('#verovioStopMIDI');

    const isPlaying = playback.isPlayingMidi();
    const isPaused = playback.isPausedMidi();

    if (isPlaying && !isPaused) {
        // Currently playing - show pause button
        playBtn?.classList.add('d-none');
        pauseBtn?.classList.remove('d-none');
        stopBtn?.classList.remove('disabled');
    } else if (isPlaying && isPaused) {
        // Currently paused - show play button
        playBtn?.classList.remove('d-none');
        pauseBtn?.classList.add('d-none');
        stopBtn?.classList.remove('disabled');
    } else {
        // Not playing - show play button, disable stop
        playBtn?.classList.remove('d-none');
        pauseBtn?.classList.add('d-none');
        stopBtn?.classList.add('disabled');
    }
}

/**
 * Update pagination control states and text
 * @param {Object} verovioApp - The main Verovio application instance
 */
export function updatePaginationControls(verovioApp) {
    const toolbar = document.querySelector('[data-editor="verovio-toolbar"]');
    if (!toolbar) return;

    const prevBtn = toolbar.querySelector('#verovioPreviousPage');
    const nextBtn = toolbar.querySelector('#verovioNextPage');
    const currentPageSpan = toolbar.querySelector('.editor-page-current');
    const pageCountSpan = toolbar.querySelector('.editor-page-count');

    const currentPage = verovioApp.getCurrentPage();
    const pageCount = verovioApp.getPageCount();

    // Update page indicators
    if (currentPageSpan) {
        currentPageSpan.textContent = currentPage;
    }
    if (pageCountSpan) {
        pageCountSpan.textContent = pageCount;
    }

    // Update button states
    if (prevBtn) {
        prevBtn.disabled = currentPage <= 1;
    }
    if (nextBtn) {
        nextBtn.disabled = currentPage >= pageCount;
    }
}

/**
 * Create floating toolbar if no existing toolbar is found
 * @param {Object} verovioApp - The main Verovio application instance
 */
export function createFloatingToolbar(verovioApp) {
    // Check if floating toolbar already exists
    let floatingToolbar = document.getElementById('verovio-floating-toolbar');

    if (!floatingToolbar) {
        floatingToolbar = document.createElement('div');
        floatingToolbar.id = 'verovio-floating-toolbar';
        floatingToolbar.setAttribute('data-editor', 'verovio-toolbar');
        floatingToolbar.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            background: white;
            border: 1px solid #ccc;
            border-radius: 5px;
            padding: 10px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
            z-index: 1000;
            display: flex;
            gap: 5px;
            flex-wrap: wrap;
        `;

        // Create toolbar buttons
        const buttons = [
            { id: 'verovioZoomIn', text: 'Zoom In', title: 'Zoom In' },
            { id: 'verovioZoomOut', text: 'Zoom Out', title: 'Zoom Out' },
            { id: 'verovioZoomReset', text: 'Reset', title: 'Reset Zoom' },
            { id: 'verovioPreviousPage', text: '◀', title: 'Previous Page' },
            { id: 'verovioNextPage', text: '▶', title: 'Next Page' },
            { id: 'verovioPlayMIDI', text: '▶', title: 'Play MIDI' },
            { id: 'verovioPauseMIDI', text: '⏸', title: 'Pause MIDI', className: 'd-none' },
            { id: 'verovioStopMIDI', text: '⏹', title: 'Stop MIDI' },
            { id: 'verovioDownloadMEI', text: 'MEI', title: 'Download MEI' },
            { id: 'verovioDownloadMIDI', text: 'MIDI', title: 'Download MIDI' }
        ];

        buttons.forEach(btn => {
            const button = document.createElement('button');
            button.id = btn.id;
            button.textContent = btn.text;
            button.title = btn.title;
            button.style.cssText = `
                padding: 5px 10px;
                margin: 2px;
                border: 1px solid #007bff;
                background: #007bff;
                color: white;
                border-radius: 3px;
                cursor: pointer;
                font-size: 12px;
            `;
            if (btn.className) {
                button.className = btn.className;
            }
            floatingToolbar.appendChild(button);
        });

        // Add page indicator
        const pageIndicator = document.createElement('span');
        pageIndicator.style.cssText = 'margin: 5px; font-size: 12px; align-self: center;';
        pageIndicator.innerHTML = 'Page <span class="editor-page-current">1</span>/<span class="editor-page-count">1</span>';
        floatingToolbar.appendChild(pageIndicator);

        document.body.appendChild(floatingToolbar);
    }

    // Bind controls to the floating toolbar
    enableToolbarControls(floatingToolbar, verovioApp);

    return floatingToolbar;
}
