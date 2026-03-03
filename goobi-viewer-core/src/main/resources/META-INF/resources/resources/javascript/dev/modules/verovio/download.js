// download.js - Export/Download functionality for Verovio
// Handles MEI and MIDI file exports with proper file naming

/**
 * Generate MIDI blob from Verovio toolkit and MEI data
 * @param {VerovioToolkit} verovio - Verovio toolkit instance
 * @param {string} mei - MEI data
 * @returns {Blob|null} MIDI blob or null if generation fails
 */
export function generateMidiBlob(verovio, mei) {
    if (!verovio || !mei) {
        console.warn('Cannot generate MIDI: missing Verovio toolkit or MEI data');
        return null;
    }

    try {
        const midiBase64 = verovio.renderToMIDI();
        if (!midiBase64) {
            console.warn('No MIDI data returned from Verovio');
            return null;
        }

        // Convert base64 to binary data
        const midiData = atob(midiBase64);
        const arrayBuffer = new ArrayBuffer(midiData.length);
        const uint8Array = new Uint8Array(arrayBuffer);

        for (let i = 0; i < midiData.length; i++) {
            uint8Array[i] = midiData.charCodeAt(i);
        }

        return new Blob([arrayBuffer], { type: 'audio/midi' });
    } catch (error) {
        console.error('Error generating MIDI blob:', error);
        return null;
    }
}

/**
 * Export MEI data as XML file
 * @param {string} mei - MEI XML data
 * @param {string} filename - Base filename (without extension)
 * @returns {string|null} MEI data or null if export fails
 */
export function exportMEI(mei, filename = 'untitled.xml') {
    if (!mei) {
        console.warn('No MEI data to export');
        return null;
    }

    try {
        const blob = new Blob([mei], { type: 'application/xml' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = filename;
        link.click();
        URL.revokeObjectURL(url);

        console.log(`MEI exported as ${filename}`);
        return mei;
    } catch (error) {
        console.error('Error exporting MEI:', error);
        return null;
    }
}

/**
 * Export MIDI blob as .mid file
 * @param {Blob} midiBlob - MIDI blob data
 * @param {string} filename - Base filename (will be converted to .mid)
 * @returns {Blob|null} MIDI blob or null if export fails
 */
export function exportMIDI(midiBlob, filename = 'untitled.xml') {
    if (!midiBlob) {
        console.warn('No MIDI data to export');
        return null;
    }

    try {
        const url = URL.createObjectURL(midiBlob);
        const link = document.createElement('a');
        link.href = url;
        link.download = filename.replace(/\.(xml|mei)$/i, '.mid');
        link.click();
        URL.revokeObjectURL(url);

        console.log(`MIDI exported as ${link.download}`);
        return midiBlob;
    } catch (error) {
        console.error('Error exporting MIDI:', error);
        return null;
    }
}

/**
 * Initialize download controls in a toolbar
 * @param {HTMLElement} toolbar - Toolbar element containing download buttons
 * @param {Object} callbacks - Callback functions for downloads
 * @param {Function} callbacks.onMEIExport - Function that returns {mei, filename}
 * @param {Function} callbacks.onMIDIExport - Function that returns {midiBlob, filename}
 */
export function initializeDownloadControls(toolbar, callbacks) {
    if (!toolbar) {
        console.warn('No toolbar provided for download controls');
        return;
    }

    const downloadMEIBtn = toolbar.querySelector('#verovioDownloadMEI');
    const downloadMIDIBtn = toolbar.querySelector('#verovioDownloadMIDI');

    if (downloadMEIBtn) {
        downloadMEIBtn.disabled = false;
        downloadMEIBtn.addEventListener('click', () => {
            try {
                const { mei, filename } = callbacks.onMEIExport();
                exportMEI(mei, filename);
            } catch (error) {
                console.error('Error in MEI export callback:', error);
            }
        });
    }

    if (downloadMIDIBtn) {
        downloadMIDIBtn.disabled = false;
        downloadMIDIBtn.addEventListener('click', () => {
            try {
                const { midiBlob, filename } = callbacks.onMIDIExport();
                exportMIDI(midiBlob, filename);
            } catch (error) {
                console.error('Error in MIDI export callback:', error);
            }
        });
    }

    console.log('Download controls initialized');
}
