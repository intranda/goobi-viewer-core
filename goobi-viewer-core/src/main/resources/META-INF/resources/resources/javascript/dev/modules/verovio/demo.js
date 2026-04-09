// demo.js - Development and demo utilities for Verovio
// Provides fallback content and demo functionality

/**
 * Fetch fallback MEI data from Verovio website
 * @returns {Promise<string|null>} MEI data or null if fetch fails
 */
export async function fetchFallbackMEI() {
    try {
        const response = await fetch('https://www.verovio.org/examples/downloads/Schubert_Lindenbaum.mei');
        if (response.ok) {
            const mei = await response.text();
            console.log('Fallback MEI loaded from verovio.org');
            return mei;
        } else {
            console.warn('Failed to fetch fallback MEI:', response.status);
            return null;
        }
    } catch (error) {
        console.warn('Error fetching fallback MEI:', error);
        return null;
    }
}

/**
 * Load fallback MEI into Verovio instance
 * @param {VerovioToolkit} verovio - Verovio toolkit instance
 * @returns {Promise<boolean>} Success status
 */
export async function loadFallbackMEI(verovio) {
    if (!verovio) {
        console.error('Verovio not initialized');
        return false;
    }

    try {
        const fallbackMEI = await fetchFallbackMEI();
        if (fallbackMEI) {
            verovio.loadData(fallbackMEI);
            console.log('Fallback MEI loaded successfully');
            return true;
        } else {
            console.warn('Could not load fallback MEI');
            return false;
        }
    } catch (error) {
        console.error('Error loading fallback MEI:', error);
        return false;
    }
}

/**
 * Create a demo container for Verovio rendering
 * @param {string} title - Container title
 * @param {Object} options - Container options
 * @returns {HTMLElement} Created container element
 */
export function createDemoContainer(title = 'Verovio Demo', options = {}) {
    const container = document.createElement('div');
    container.id = options.id || 'verovio-demo-container';
    container.style.cssText = `
        border: 2px solid #007bff;
        margin: 20px;
        padding: 20px;
        background: #f8f9fa;
        border-radius: 5px;
        ${options.style || ''}
    `;

    const header = document.createElement('h3');
    header.textContent = title;
    header.style.cssText = 'margin-top: 0; color: #007bff;';

    const content = document.createElement('div');
    content.className = 'verovio-content';

    container.appendChild(header);
    container.appendChild(content);

    if (options.appendTo) {
        options.appendTo.appendChild(container);
    } else {
        document.body.appendChild(container);
    }

    return container;
}

/**
 * Render Verovio demo with fallback MEI
 * @param {VerovioToolkit} verovio - Verovio toolkit instance
 * @param {HTMLElement} container - Container element (optional)
 * @param {Object} options - Demo options
 * @returns {Promise<boolean>} Success status
 */
export async function renderDemo(verovio, container = null, options = {}) {
    if (!verovio) {
        console.error('Verovio not initialized');
        return false;
    }

    try {
        // Load fallback MEI if no data is available
        const hasData = await loadFallbackMEI(verovio);
        if (!hasData) {
            console.error('Demo rendering failed: no MEI data available');
            return false;
        }

        // Create container if not provided
        if (!container) {
            container = createDemoContainer('Verovio Demo', options);
        }

        // Render SVG
        const svg = verovio.renderToSVG(1);
        if (svg) {
            const contentDiv = container.querySelector('.verovio-content') || container;
            const pageCount = verovio.getPageCount();

            contentDiv.innerHTML = `
                <div class="demo-info" style="margin-bottom: 15px; padding: 10px; background: #e9ecef; border-radius: 3px;">
                    <small>
                        <strong>Demo Content:</strong> Schubert's "Der Lindenbaum" (from verovio.org)<br>
                        <strong>Pages:</strong> ${pageCount} | <strong>Scale:</strong> ${verovio.getOptions().scale || 100}%
                    </small>
                </div>
                ${svg}
            `;

            console.log('Demo rendered successfully');
            return true;
        } else {
            console.error('Demo rendering failed: no SVG generated');
            return false;
        }
    } catch (error) {
        console.error('Error rendering demo:', error);
        return false;
    }
}

/**
 * Create a complete demo setup with toolbar
 * @param {VerovioApp} verovioApp - Main Verovio application instance
 * @param {Object} options - Demo options
 * @returns {Promise<HTMLElement>} Demo container element
 */
export async function createCompleteDemo(verovioApp, options = {}) {
    try {
        // Create demo container
        const container = createDemoContainer('Verovio Complete Demo', {
            style: 'max-width: 1200px; margin: 20px auto;',
            ...options,
        });

        // Load demo content
        const success = await renderDemo(verovioApp.verovio, container);
        if (!success) {
            throw new Error('Failed to render demo content');
        }

        // Create floating toolbar if requested
        if (options.includeToolbar !== false) {
            const { createFloatingToolbar } = await import('./toolbar.js');
            const toolbar = createFloatingToolbar({
                verovio: verovioApp,
                navigation: verovioApp.navigation,
                playback: verovioApp.playback,
            });

            // Position toolbar relative to demo container
            toolbar.style.position = 'absolute';
            toolbar.style.top = '10px';
            toolbar.style.right = '10px';
            container.style.position = 'relative';
        }

        console.log('Complete demo created successfully');
        return container;
    } catch (error) {
        console.error('Error creating complete demo:', error);
        return null;
    }
}

/**
 * Initialize demo mode for development
 * @param {VerovioApp} verovioApp - Main Verovio application instance
 * @param {Object} options - Demo options
 */
export async function initializeDemoMode(verovioApp, options = {}) {
    try {
        // Set up demo environment
        console.log('🎭 Initializing Verovio Demo Mode');

        // Load fallback MEI
        if (!verovioApp.mei) {
            await loadFallbackMEI(verovioApp.verovio);
            verovioApp.mei = await fetchFallbackMEI(); // Store in app instance
            verovioApp.filename = 'Schubert_Lindenbaum.mei';
        }

        // Create demo container if none exists
        let container = document.querySelector('[data-editor="verovio"] [id$="editor"]');
        if (!container) {
            container = await createCompleteDemo(verovioApp, options);
        }

        // Set current container reference
        verovioApp.currentContainer = container;

        // Initialize navigation if available
        if (verovioApp.navigation) {
            verovioApp.navigation.updatePageCount();
        }

        console.log('✅ Demo mode initialized successfully');
        return true;
    } catch (error) {
        console.error('❌ Error initializing demo mode:', error);
        return false;
    }
}

/**
 * Clean up demo containers and resources
 */
export function cleanupDemo() {
    // Remove demo containers
    const demoContainers = document.querySelectorAll('#verovio-demo-container');
    demoContainers.forEach((container) => container.remove());

    // Remove floating toolbar
    const floatingToolbar = document.getElementById('verovio-floating-toolbar');
    if (floatingToolbar) {
        floatingToolbar.remove();
    }

    console.log('Demo cleanup completed');
}

/**
 * Demo utility functions for testing
 */
export const demoUtils = {
    /**
     * Test all zoom levels
     */
    async testZoomLevels(verovioApp) {
        const levels = [25, 50, 75, 100, 125, 150, 200];
        for (const level of levels) {
            console.log(`Testing zoom level: ${level}%`);
            if (verovioApp.navigation) {
                verovioApp.navigation.setZoom(level);
            }
            await new Promise((resolve) => setTimeout(resolve, 1000));
        }
    },

    /**
     * Test page navigation
     */
    async testPageNavigation(verovioApp) {
        if (!verovioApp.navigation) return;

        const pageInfo = verovioApp.navigation.getPageInfo();
        console.log(`Testing page navigation (${pageInfo.total} pages)`);

        for (let i = 1; i <= pageInfo.total; i++) {
            console.log(`Navigating to page ${i}`);
            verovioApp.navigation.setPage(i);
            await new Promise((resolve) => setTimeout(resolve, 1500));
        }
    },

    /**
     * Test MIDI playback
     */
    async testMidiPlayback(verovioApp) {
        console.log('Testing MIDI playback');
        try {
            await verovioApp.playMEI();
            await new Promise((resolve) => setTimeout(resolve, 3000));
            verovioApp.pauseMidi();
            console.log('MIDI test completed');
        } catch (error) {
            console.error('MIDI test failed:', error);
        }
    },
};
