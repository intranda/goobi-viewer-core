// playback.js - MIDI playback and highlighting for Verovio using Tone.js
// This module is designed to be imported and used by the main Verovio editor class.

import * as Tone from 'tone';
import * as ToneMidi from '@tonejs/midi';
import { createMIDIBasedSoundMapping } from './sounds.js';

const { Midi } = ToneMidi;

let synth = null;
let soundMappings = null; // Store intelligent sound mappings
let currentMidiParts = [];
let isPlaying = false;
let isPaused = false;
let currentMidiDuration = 0;
let playbackStartTime = 0;
let pausedTime = 0;
let totalPausedDuration = 0;
let pauseStartTime = 0;
let timeDisplayInterval = null;
let autoStopTimeout = null;
let scrubberUpdateInterval = null;
let scrubberElement = null;
let onStopCallback = null;
let verovioInstance = null;
let currentContainer = null;
let navigationController = null; // Reference to navigation controller for auto page navigation
let autoNavigationEnabled = true; // Enable/disable automatic page navigation during playback
let highlightingTimer = null;
let highlightedElements = [];

function getSynth() {
    if (!synth) {
        synth = new Tone.PolySynth(Tone.Synth, {
            oscillator: { type: 'triangle' },
            envelope: { attack: 0.02, decay: 0.1, sustain: 0.3, release: 0.8 }
        }).toDestination();
    }
    return synth;
}

export function base64ToArrayBuffer(base64midi) {
    const binaryString = atob(base64midi);
    const bytes = new Uint8Array(binaryString.length);
    for (let i = 0; i < binaryString.length; i++) {
        bytes[i] = binaryString.charCodeAt(i);
    }
    return bytes.buffer;
}

// Set Verovio instance and container for highlighting
export function setVerovioInstance(verovio, container) {
    verovioInstance = verovio;
    currentContainer = container;
}

// Set navigation controller for automatic page navigation during playback
export function setNavigationController(navController) {
    navigationController = navController;
}

// Enable or disable automatic page navigation during playback
export function setAutoNavigation(enabled) {
    autoNavigationEnabled = enabled;
}

// Get current auto navigation setting
export function isAutoNavigationEnabled() {
    return autoNavigationEnabled;
}

export async function playMidiWithTone(midiBuffer, onNote, onStop) {
    try {
        if (isPlaying) stopMidi();
        isPaused = false;
        pausedTime = 0;
        totalPausedDuration = 0;
        pauseStartTime = 0;
        isPlaying = true;

        // Store the onStop callback for later use
        onStopCallback = onStop;

        const midi = new Midi(midiBuffer);
        currentMidiDuration = midi.duration || 0;
        if (currentMidiDuration <= 0) currentMidiDuration = 5;
        currentMidiParts = [];

        // Create intelligent sound mapping based on MIDI data
        let trackSynths = [];
        if (midi.tracks.length > 0) {
            try {
                soundMappings = createMIDIBasedSoundMapping(midi);
                console.log('Created MIDI-based sound mapping:', soundMappings);

                // Create synthesizers for each track
                trackSynths = soundMappings.tracks.map(trackMapping => {
                    return trackMapping.synth || getSynth(); // Fallback to default synth
                });
            } catch (error) {
                console.warn('Failed to create MIDI-based sound mapping, falling back to default synth:', error);
                // Fallback to default synthesizer for all tracks
                trackSynths = midi.tracks.map(() => getSynth());
            }
        } else {
            // No tracks, use default synthesizer
            trackSynths = [getSynth()];
        }

        midi.tracks.forEach((track, trackIndex) => {
            if (track.notes.length > 0) {
                const synthInstance = trackSynths[trackIndex] || getSynth();
                const part = new Tone.Part((time, note) => {
                    const noteName = note.name || Tone.Frequency(note.midi, "midi").toNote();
                    const velocity = note.velocity || 0.7;
                    synthInstance.triggerAttackRelease(noteName, note.duration, time, velocity);
                    if (onNote) onNote(note, time);
                }, track.notes.map(note => ({
                    time: note.time,
                    name: note.name,
                    midi: note.midi,
                    duration: note.duration,
                    velocity: note.velocity
                })));
                part.loop = false;
                part.start(0);
                currentMidiParts.push(part);
            }
        });
        playbackStartTime = Tone.now();

        // Set up transport stop handler for end of playback
        Tone.Transport.on('stop', () => {
            if (isPlaying && !isPaused) {
                setTimeout(() => {
                    if (isPlaying) {
                        // Natural end of playback
                        handlePlaybackEnd();
                        if (onStop) onStop();
                    }
                }, 100);
            }
        });

        // Set up auto-stop timeout as fallback
        if (currentMidiDuration > 0) {
            autoStopTimeout = setTimeout(() => {
                if (isPlaying) {
                    handlePlaybackEnd();
                    if (onStop) onStop();
                }
            }, (currentMidiDuration + 0.5) * 1000); // Add 0.5s buffer
        }

        await Tone.start();
        Tone.Transport.start();

        // Start scrubber updates and show scrubber
        showScrubber();
        startScrubberUpdates();
        startHighlighting();
    } catch (error) {
        isPlaying = false;
        isPaused = false;
        stopMidi();
        if (onStop) onStop(error);
        throw error;
    }
}

export function pauseMidi() {
    if (!isPlaying) return;
    if (isPaused) {
        // Resume playback
        isPaused = false;
        const pauseDuration = Tone.now() - pauseStartTime;
        totalPausedDuration += pauseDuration;

        // Make sure transport is at the right position before starting
        Tone.Transport.seconds = pausedTime;
        playbackStartTime = Tone.now() - pausedTime;

        Tone.Transport.start();
        startScrubberUpdates();
        startHighlighting(); // Resume highlighting when resuming playback

        // Restart auto-stop timeout for remaining time
        if (autoStopTimeout) {
            clearTimeout(autoStopTimeout);
            autoStopTimeout = null;
        }
        const remainingTime = currentMidiDuration - pausedTime;
        if (remainingTime > 0) {
            autoStopTimeout = setTimeout(() => {
                if (isPlaying) {
                    handlePlaybackEnd();
                    if (onStopCallback) onStopCallback();
                }
            }, (remainingTime + 0.5) * 1000); // Add 0.5s buffer
        }

        console.log(`Resumed playback from ${pausedTime.toFixed(2)}s`);
    } else {
        // Pause playback
        isPaused = true;
        pauseStartTime = Tone.now();
        pausedTime = Tone.Transport.seconds;
        Tone.Transport.pause();
        stopScrubberUpdates();
        stopHighlighting(); // Stop highlighting when pausing

        // Clear auto-stop timeout during pause
        if (autoStopTimeout) {
            clearTimeout(autoStopTimeout);
            autoStopTimeout = null;
        }

        console.log(`Paused playback at ${pausedTime.toFixed(2)}s`);
    }
    // Keep scrubber visible during pause
}

export function stopMidi() {
    if (isPlaying) {
        // Use the same cleanup logic as natural playback end
        handlePlaybackEnd();
    }
}

function handlePlaybackEnd() {
    console.log('Playback ended naturally');
    isPlaying = false;
    isPaused = false;
    pausedTime = 0;
    totalPausedDuration = 0;

    // Clean up transport
    Tone.Transport.stop();
    Tone.Transport.cancel();
    Tone.Transport.off('stop');

    // Clean up parts
    currentMidiParts.forEach(part => { if (part) { part.stop(); part.dispose(); } });
    currentMidiParts = [];

    // Release synth notes
    if (synth) synth.releaseAll();

    // Clear auto-stop timeout
    if (autoStopTimeout) {
        clearTimeout(autoStopTimeout);
        autoStopTimeout = null;
    }

    // Handle scrubber
    stopScrubberUpdates();
    resetScrubberToZero();
    hideScrubber();

    // Stop highlighting
    stopHighlighting();

    // Call the onStop callback to update UI
    if (onStopCallback) {
        onStopCallback();
        onStopCallback = null; // Clear the callback after calling
    }
}

export function isPlayingMidi() { return isPlaying; }
export function isPausedMidi() { return isPaused; }
export function getPlaybackTime() {
    let currentTime = isPaused ? pausedTime : Tone.Transport.seconds || 0;

    // Clamp current time to not exceed the MIDI duration
    currentTime = Math.min(currentTime, currentMidiDuration);

    // Only check for end of playback if we're actively playing (not paused)
    // and we've actually reached or exceeded the duration
    if (currentTime >= currentMidiDuration && isPlaying && !isPaused) {
        // Add a small delay and recheck to avoid false triggers during resume
        setTimeout(() => {
            if (isPlaying && !isPaused && Tone.Transport.seconds >= currentMidiDuration) {
                console.log('Natural end of playback detected');
                handlePlaybackEnd();
            }
        }, 100);
    }

    return {
        current: currentTime,
        total: currentMidiDuration,
        progress: currentMidiDuration > 0 ? (currentTime / currentMidiDuration) * 100 : 0
    };
}

export function cleanup() {
    stopMidi();
    stopScrubberUpdates();
    hideScrubber();
    stopHighlighting();

    // Clear any remaining timeouts
    if (autoStopTimeout) {
        clearTimeout(autoStopTimeout);
        autoStopTimeout = null;
    }

    // Clear callback
    onStopCallback = null;

    if (synth) { synth.dispose(); synth = null; }
    isPlaying = false;
    isPaused = false;
    pausedTime = 0;
    totalPausedDuration = 0;
    currentMidiDuration = 0;
}

// Scrubber functionality
export function initializeScrubber(elementId = 'editor-midi-scrubber') {
    scrubberElement = document.getElementById(elementId);
    if (!scrubberElement) {
        console.warn(`Scrubber element with ID '${elementId}' not found`);
        return false;
    }

    const track = scrubberElement.querySelector('.scrubber-track');

    if (!track) {
        console.error('Scrubber track element not found after initialization');
        return false;
    }

    // Add event listener for click-to-seek
    track.addEventListener('click', handleScrubberClick);
    return true;
}

function handleScrubberClick(event) {
    seekToPosition(event);
}

function seekToPosition(event) {
    if (!scrubberElement || currentMidiDuration <= 0) return;

    const track = scrubberElement.querySelector('.scrubber-track');
    if (!track) return;

    const rect = track.getBoundingClientRect();
    const x = Math.max(0, Math.min(event.clientX - rect.left, rect.width));
    const percentage = x / rect.width;
    const targetTime = percentage * currentMidiDuration;

    seekToTime(targetTime);
}

export function seekToTime(targetTime) {
    if (!isPlaying && !isPaused) {
        console.warn('Cannot seek when not playing');
        return;
    }

    if (targetTime < 0 || targetTime > currentMidiDuration) {
        console.warn('Seek time out of bounds');
        return;
    }

    try {
        // Remember if we were playing before seeking
        const wasPlaying = isPlaying && !isPaused;

        // Pause transport without stopping completely
        if (wasPlaying) {
            Tone.Transport.pause();
        }

        // Set new transport position
        Tone.Transport.seconds = targetTime;

        // Update our internal timing tracking
        pausedTime = targetTime;
        playbackStartTime = Tone.now() - targetTime;

        // Reset any pause duration tracking since we're seeking
        totalPausedDuration = 0;

        // Clear the old auto-stop timeout and set a new one based on remaining time
        if (autoStopTimeout) {
            clearTimeout(autoStopTimeout);
            autoStopTimeout = null;
        }

        // Set new auto-stop timeout for the remaining duration
        const remainingTime = currentMidiDuration - targetTime;
        if (remainingTime > 0) {
            autoStopTimeout = setTimeout(() => {
                if (isPlaying) {
                    handlePlaybackEnd();
                    if (onStopCallback) onStopCallback();
                }
            }, (remainingTime + 0.5) * 1000); // Add 0.5s buffer
        }

        // Resume playback if it was playing before seeking
        if (wasPlaying) {
            Tone.Transport.start();
        } else {
            // If we were paused, stay paused but update the position
            isPaused = true;
        }

        updateScrubberDisplay();
        console.log(`Seeked to ${targetTime.toFixed(2)}s`);
    } catch (error) {
        console.error('Error seeking to time:', error);
    }
}

export function startScrubberUpdates() {
    if (scrubberUpdateInterval) return;

    scrubberUpdateInterval = setInterval(() => {
        updateScrubberDisplay();
    }, 100); // Update every 100ms
}

export function stopScrubberUpdates() {
    if (scrubberUpdateInterval) {
        clearInterval(scrubberUpdateInterval);
        scrubberUpdateInterval = null;
    }
}

function updateScrubberDisplay() {
    if (!scrubberElement) return;

    const timeData = getPlaybackTime();
    const progress = scrubberElement.querySelector('.scrubber-progress');
    const currentTimeSpan = scrubberElement.querySelector('.current-time');
    const totalTimeSpan = scrubberElement.querySelector('.total-time');

    if (progress) {
        progress.style.width = `${timeData.progress}%`;
    }

    if (currentTimeSpan) {
        currentTimeSpan.textContent = formatTime(timeData.current);
    }

    if (totalTimeSpan) {
        totalTimeSpan.textContent = formatTime(timeData.total);
    }
}

function formatTime(seconds) {
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins}:${secs.toString().padStart(2, '0')}`;
}

export function setScrubberDuration(duration) {
    currentMidiDuration = duration;
    updateScrubberDisplay();
}

function resetScrubberToZero() {
    if (!scrubberElement) return;

    const progress = scrubberElement.querySelector('.scrubber-progress');
    const currentTimeSpan = scrubberElement.querySelector('.current-time');

    if (progress) {
        progress.style.width = '0%';
    }

    if (currentTimeSpan) {
        currentTimeSpan.textContent = '0:00';
    }
}

function showScrubber() {
    if (!scrubberElement) return;
    scrubberElement.style.display = 'flex';
}

function hideScrubber() {
    if (!scrubberElement) return;
    scrubberElement.style.display = 'none';
}

// Highlighting functionality
function startHighlighting() {
    if (!verovioInstance || !currentContainer) {
        console.warn('Verovio instance or container not set for highlighting');
        return;
    }

    // Update highlighting every 50ms for smooth animation
    highlightingTimer = setInterval(() => {
        if (isPlaying && !isPaused) {
            updateHighlighting();
        }
    }, 50);
}

function stopHighlighting() {
    if (highlightingTimer) {
        clearInterval(highlightingTimer);
        highlightingTimer = null;
    }
    clearHighlighting();
}

function updateHighlighting() {
    if (!verovioInstance || !currentContainer) return;

    try {
        // Get current playback time in milliseconds
        const currentTimeMs = (Tone.Transport.seconds || 0) * 1000;

        // Check if getElementsAtTime method exists
        if (typeof verovioInstance.getElementsAtTime !== 'function') {
            console.warn('verovioInstance.getElementsAtTime not available, using time-based navigation');
            // Use alternative time-based navigation
            performTimeBasedNavigation(currentTimeMs);
            return;
        }

        // Get elements at current time from Verovio
        const elementsAtTime = verovioInstance.getElementsAtTime(currentTimeMs);

        if (elementsAtTime && elementsAtTime.notes && elementsAtTime.notes.length > 0) {
            // Check if automatic page navigation is needed
            checkAndNavigateToElement(elementsAtTime.notes[0]);

            // Clear previous highlighting
            clearHighlighting();

            // Apply highlighting to current notes
            highlightedElements = elementsAtTime.notes;
            applyHighlighting(highlightedElements);
        }
    } catch (error) {
        console.error('Error updating highlighting:', error);
        // Fallback to time-based navigation
        try {
            const currentTimeMs = (Tone.Transport.seconds || 0) * 1000;
            performTimeBasedNavigation(currentTimeMs);
        } catch (fallbackError) {
            console.error('Fallback navigation also failed:', fallbackError);
        }
    }
}

/**
 * Simple time-based page navigation fallback
 * Estimates which page should be displayed based on playback time
 */
function performTimeBasedNavigation(currentTimeMs) {
    if (!autoNavigationEnabled || !navigationController || !verovioInstance) return;

    try {
        const totalPages = navigationController.getPageCount();
        const totalDuration = currentMidiDuration * 1000; // Convert to ms

        if (totalPages <= 1 || totalDuration <= 0) return;

        // Estimate which page we should be on based on time progression
        const timeProgress = currentTimeMs / totalDuration;
        const estimatedPage = Math.min(Math.max(1, Math.ceil(timeProgress * totalPages)), totalPages);
        const currentPage = navigationController.getCurrentPage();

        // Only navigate if we're off by more than 1 page to avoid excessive jumping
        if (Math.abs(estimatedPage - currentPage) > 1) {
            navigationController.setPage(estimatedPage);
        }
    } catch (error) {
        console.warn('Error in time-based navigation:', error);
    }
}

/**
 * Check if the given element is visible on the current page, and navigate if needed
 * @param {string} elementId - The ID of the element to check
 */
function checkAndNavigateToElement(elementId) {
    if (!autoNavigationEnabled || !navigationController || !verovioInstance || !elementId) return;

    try {
        // First check if element exists in current DOM (most efficient)
        const element = currentContainer.querySelector(`#${elementId}`);
        if (element) {
            // Element is visible on current page, no navigation needed
            return;
        }

        console.log(`Element ${elementId} not visible on current page, searching for correct page...`);

        // Try Verovio's getPageWithElement method if it exists
        if (typeof verovioInstance.getPageWithElement === 'function') {
            const pageInfo = verovioInstance.getPageWithElement(elementId);
            if (pageInfo && typeof pageInfo.page === 'number') {
                const targetPage = pageInfo.page;
                const currentPage = navigationController.getCurrentPage();

                if (targetPage !== currentPage && targetPage > 0) {
                    navigationController.setPage(targetPage);
                    console.log(`Auto-navigated to page ${targetPage} for element ${elementId}`);
                    return;
                }
            }
        }

        // Fallback: search through pages
        findAndNavigateToElementPage(elementId);

    } catch (error) {
        console.warn('Error in automatic page navigation:', error);
        // Try fallback method
        try {
            findAndNavigateToElementPage(elementId);
        } catch (altError) {
            console.warn('Could not determine page for element:', elementId, altError);
        }
    }
}

/**
 * Alternative method to find which page contains an element
 * @param {string} elementId - The ID of the element to find
 */
function findAndNavigateToElementPage(elementId) {
    if (!autoNavigationEnabled || !navigationController || !verovioInstance) return;

    try {
        const totalPages = navigationController.getPageCount();
        const currentPage = navigationController.getCurrentPage();

        console.log(`Searching for element ${elementId} across ${totalPages} pages...`);

        // Search through pages to find the element
        for (let pageNum = 1; pageNum <= totalPages; pageNum++) {
            if (pageNum === currentPage) continue; // Skip current page, we know it's not there

            try {
                // Render the page temporarily to check for element
                const svgContent = verovioInstance.renderToSVG(pageNum);
                if (svgContent && svgContent.includes(`id="${elementId}"`)) {
                    console.log(`Found element ${elementId} on page ${pageNum}, navigating...`);
                    navigationController.setPage(pageNum);
                    return;
                }
            } catch (renderError) {
                console.warn(`Could not render page ${pageNum}:`, renderError);
                continue;
            }
        }

        console.warn(`Element ${elementId} not found on any page`);
    } catch (error) {
        console.warn('Could not find page for element:', elementId, error);
    }
}

function applyHighlighting(elementIds) {
    if (!currentContainer || !elementIds) return;

    elementIds.forEach(id => {
        const element = currentContainer.querySelector(`#${id}`);
        if (element) {
            element.classList.add('highlighted-note');
        }
    });
}

function clearHighlighting() {
    if (!currentContainer || !highlightedElements) return;

    highlightedElements.forEach(id => {
        const element = currentContainer.querySelector(`#${id}`);
        if (element) {
            // Remove all highlighting styles
            element.classList.remove('highlighted-note');
        }
    });

    highlightedElements = [];
}
