import { VerovioToolkit } from 'verovio/esm';
import createVerovioModule from 'verovio/wasm';
import * as playback from './verovio/playback.js';
import { NavigationController } from './verovio/navigation.js';
import { initializeToolbar, updateMidiButtonStates } from './verovio/toolbar.js';
import { generateMidiBlob, exportMEI, exportMIDI } from './verovio/download.js';
import * as demo from './verovio/demo.js';

// Main Verovio application class
class VerovioApp {
    constructor() {
        this.verovio = null;
        this.mei = "";
        this.filename = "untitled.xml";
        this.fallbackMEI = null;
        this.currentContainer = null;

        // Initialize navigation controller (will be set up after Verovio init)
        this.navigationController = null;
    }

    /**
     * Fetch fallback MEI data from Verovio website
     * @returns {Promise<string>} MEI data or null if fetch fails
     */
    async fetchFallbackMEI() {
        if (this.fallbackMEI) {
            return this.fallbackMEI;
        }

        this.fallbackMEI = await demo.fetchFallbackMEI();
        return this.fallbackMEI;
    }

    async init() {
        try {
            // Initialize the WASM module first
            const verovioModule = await createVerovioModule();

            // Create toolkit with the module
            this.verovio = new VerovioToolkit(verovioModule);

            // Set default options
            this.verovio.setOptions({
                scale: 100, // Default zoom level
                adjustPageWidth: true,
                pageWidth: 2100,
                pageHeight: 2970
            });

        } catch (error) {
            console.error('Failed to initialize Verovio:', error);
            return this;
        }

        // Initialize navigation controller
        this.navigationController = new NavigationController(this.verovio, null, {
            onZoomChange: (newScale, oldScale) => {
                this.renderCurrentPage();
            },
            onPageChange: (newPage, oldPage) => {
                this.renderCurrentPage();
            }
        });

        // Check for verovio input and load MEI data
        await this.loadFromInput();

        // Initialize MIDI scrubber
        this.initializeScrubber();

        // Initialize toolbar
        this.initializeToolbar();

        return this;
    }

    /**
     * Load MEI data from input element or fallback
     */
    async loadFromInput() {
        // Check for verovio input element
        const inputElement = document.querySelector('[id$="verovioInput"]');

        if (inputElement && inputElement.value.trim()) {
            // Load MEI from input
            const mei = inputElement.value.trim();
            this.loadData(mei, 'input.mei');
            console.log('MEI loaded from input element');
        } else {
            // Check if demo mode is enabled
            const editorElement = document.querySelector('[data-editor="verovio"]');
            const isDemoMode = editorElement && editorElement.getAttribute('data-demo') === 'true';

            if (isDemoMode) {
                await this.loadFallbackMEI();
                console.log('Demo mode enabled - loaded fallback MEI');
            } else {
                console.log('No input MEI found and demo mode not enabled');
            }
        }

        // Find and set the container for rendering
        this.findAndSetContainer();
    }

    /**
     * Find the appropriate container for rendering
     */
    findAndSetContainer() {
        // Look for the editor container
        let container = document.querySelector('[data-editor="verovio"] #editor');

        if (!container) {
            // Fallback: look for any element with class 'verovio'
            container = document.querySelector('.verovio');
        }

        if (!container) {
            // Last resort: look for any container with id ending in 'editor'
            container = document.querySelector('[id$="editor"]');
        }

        if (container) {
            this.currentContainer = container;

            // Update navigation controller with the container
            if (this.navigationController) {
                this.navigationController.setContainer(container);
            }

            console.log('Container found and set:', container.id || container.className);

            // If we have MEI data, render it immediately
            if (this.mei) {
                this.renderCurrentPage();
            }
        } else {
            console.warn('No suitable container found for Verovio rendering');
        }
    }

    loadData(mei, filename = "untitled.xml") {
        if (!this.verovio) {
            console.error('Verovio not initialized');
            return false;
        }

        try {
            this.mei = mei;
            this.filename = filename;

            // Try to find any method that might load MEI data
            const possibleMethods = [
                'loadData',
                'load',
                'loadMEI',
                'loadMusicXMLString',
                'loadMusicXML',
                'loadDocument',
                'setMEI',
                'from'
            ];

            let loadResult = false;
            let methodUsed = null;

            for (const method of possibleMethods) {
                if (typeof this.verovio[method] === 'function') {
                    try {
                        loadResult = this.verovio[method](mei);
                        methodUsed = method;
                        break;
                    } catch (error) {
                        console.warn(`Method ${method} failed:`, error.message);
                    }
                }
            }

            if (!methodUsed) {
                console.error('No working load method found');
                return false;
            }

            // Update navigation controller state
            if (this.navigationController) {
                this.navigationController.updatePageCount();
                // Render the first page if we have a container
                if (this.currentContainer) {
                    this.navigationController.setPage(1);
                }
            }

            console.log('MEI loaded successfully, pages:', this.verovio.getPageCount());
            return true;
        } catch (error) {
            console.error('Error loading MEI:', error);
            return false;
        }
    }

    /**
     * Load fallback MEI from Verovio website
     */
    async loadFallbackMEI() {
        if (!this.verovio) {
            console.error('Verovio not initialized');
            return false;
        }

        try {
            // Fetch the MEI data from demo module
            const fallbackMEI = await demo.fetchFallbackMEI();

            if (fallbackMEI) {
                // Use our own loadData method which has better error handling
                const success = this.loadData(fallbackMEI, 'Schubert_Lindenbaum.mei');

                if (success) {
                    // Try to render immediately for testing
                    this.renderDemo();
                    console.log('Fallback MEI loaded and rendered');
                    return true;
                } else {
                    console.error('Failed to load fallback MEI into Verovio');
                    return false;
                }
            } else {
                console.error('Failed to fetch fallback MEI');
                return false;
            }
        } catch (error) {
            console.error('Error loading fallback MEI:', error);
            return false;
        }
    }

    renderToSVG(pageNo = 1) {
        if (!this.verovio) {
            console.error('Verovio not initialized');
            return '';
        }

        try {
            const svg = this.verovio.renderToSVG(pageNo);
            return svg;
        } catch (error) {
            console.error('Error rendering SVG:', error);
            return '';
        }
    }

    setOptions(options) {
        if (!this.verovio) return;
        this.verovio.setOptions(options);
    }

    /**
     * Generate MIDI data for playback - delegate to download module
     * @returns {Blob|null} MIDI data as blob or null if generation fails
     */
    generateMidiBlob() {
        return generateMidiBlob(this.verovio, this.mei);
    }

    // Export methods - delegate to download module
    exportMEI() {
        return exportMEI(this.mei, this.filename);
    }

    exportMIDI() {
        if (!this.verovio || !this.mei) {
            console.warn('No Verovio instance or MEI data to export MIDI');
            return null;
        }

        // Generate MIDI blob first
        const midiBlob = generateMidiBlob(this.verovio, this.mei);
        if (!midiBlob) {
            console.warn('Failed to generate MIDI blob');
            return null;
        }

        // Then export/download the blob
        return exportMIDI(midiBlob, this.filename);
    }

    // MIDI playback methods using playback.js
    async playMEI() {
        // Generate fresh MIDI data
        if (!this.verovio || !this.mei) {
            console.warn('No Verovio instance or MEI data to generate MIDI');
            return;
        }
        try {
            const midiBase64 = this.verovio.renderToMIDI();
            if (!midiBase64) {
                console.error('Failed to generate MIDI from MEI');
                return;
            }
            const midiBuffer = playback.base64ToArrayBuffer(midiBase64);

            // Get MIDI duration for scrubber
            const { Midi } = await import('@tonejs/midi');
            const midi = new Midi(midiBuffer);
            const duration = midi.duration || 0;
            playback.setScrubberDuration(duration);

            // Set Verovio instance and container for highlighting
            playback.setVerovioInstance(this.verovio, this.currentContainer);

            // Set navigation controller for automatic page navigation
            if (this.navigationController) {
                playback.setNavigationController(this.navigationController);
            }

            await playback.playMidiWithTone(midiBuffer, null, async () => {
                await this.updateMidiButtonStates();
            });
            await this.updateMidiButtonStates();
        } catch (error) {
            console.error('Error playing MEI as MIDI:', error);
        }
    }

    async pauseMidi() {
        playback.pauseMidi();
        await this.updateMidiButtonStates();
    }

    async stopMidi() {
        playback.stopMidi();
        await this.updateMidiButtonStates();
    }

    // Auto navigation control methods
    setAutoNavigation(enabled) {
        playback.setAutoNavigation(enabled);
    }

    isAutoNavigationEnabled() {
        return playback.isAutoNavigationEnabled();
    }

    // Initialize MIDI scrubber
    initializeScrubber() {
        try {
            // Try to find scrubber by class first, then by ID
            let scrubberElement = document.querySelector('.editor-midi-scrubber');
            if (!scrubberElement) {
                scrubberElement = document.getElementById('editor-midi-scrubber');
            }

            if (scrubberElement && !scrubberElement.id) {
                scrubberElement.id = 'editor-midi-scrubber';
            }

            const scrubberInitialized = playback.initializeScrubber('editor-midi-scrubber');
            if (scrubberInitialized) {
                console.log('MIDI scrubber initialized successfully');
            } else {
                console.warn('MIDI scrubber initialization failed - element may not exist yet');
                // Try again after a short delay in case the DOM isn't ready
                setTimeout(() => {
                    let element = document.querySelector('.editor-midi-scrubber');
                    if (element && !element.id) {
                        element.id = 'editor-midi-scrubber';
                    }
                    playback.initializeScrubber('editor-midi-scrubber');
                }, 1000);
            }
        } catch (error) {
            console.error('Error initializing MIDI scrubber:', error);
        }
    }

    // Utility method to get current MEI data
    getCurrentMEI() {
        return this.mei;
    }



    // Demo methods - delegate to demo module
    renderDemo() {
        if (!this.verovio) {
            console.error('Verovio not initialized');
            return;
        }

        if (!this.mei) {
            console.error('Demo rendering failed: no MEI data available');
            return;
        }

        try {
            const svg = this.renderToSVG(1);
            if (svg) {
                // Create demo container and render directly since we already have MEI data loaded
                this.currentContainer = demo.createDemoContainer();

                // Render the SVG directly into the container instead of calling demo.renderDemo
                // which would try to load MEI again
                const contentDiv = this.currentContainer.querySelector('.verovio-content') || this.currentContainer;
                const pageCount = this.verovio.getPageCount();
                contentDiv.innerHTML = `<h3>Verovio Demo - Page 1/${pageCount}</h3>${svg}`;

                console.log('Demo rendered successfully');
                return svg;
            } else {
                console.error('Demo rendering failed: could not generate SVG');
            }
        } catch (error) {
            console.error('Demo rendering failed:', error);
        }

        return null;
    }

    // Toolbar methods - delegate to toolbar module
    initializeToolbar() {
        initializeToolbar(this);
    }

    async updateMidiButtonStates() {
        await updateMidiButtonStates();
    }

    // Navigation methods - delegate to navigation controller
    previousPage() {
        if (this.navigationController) {
            this.navigationController.previousPage();
        }
    }

    nextPage() {
        if (this.navigationController) {
            this.navigationController.nextPage();
        }
    }

    getCurrentPage() {
        return this.navigationController ? this.navigationController.getCurrentPage() : 1;
    }

    getPageCount() {
        return this.navigationController ? this.navigationController.getPageCount() : 0;
    }

    // Navigation methods - delegate to navigation controller
    zoomIn() {
        if (this.navigationController) {
            this.navigationController.zoomIn();
        }
    }

    zoomOut() {
        if (this.navigationController) {
            this.navigationController.zoomOut();
        }
    }

    zoomReset() {
        if (this.navigationController) {
            this.navigationController.zoomReset();
        }
    }

    setZoom(scale) {
        if (this.navigationController) {
            this.navigationController.setZoom(scale);
        }
    }

    getZoom() {
        return this.navigationController ? this.navigationController.getZoom() : 100;
    }

    // Render the current page - delegate to navigation controller
    renderCurrentPage() {
        if (this.navigationController) {
            this.navigationController.renderCurrentPage();
        }
    }
}
window.app = new VerovioApp();
window.toolbar = window.app; // Backward compatibility

// Initialize when DOM is ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        window.app.init();
    });
} else {
    window.app.init();
}
