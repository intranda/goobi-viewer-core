// sounds.js - Intelligent MIDI sound assignment for Verovio
// Uses comprehensive MIDI program mapping with three-tier detection

import * as Tone from 'tone';
import {
    midiPrograms,
    namePatterns,
    instrumentCategories,
    getInstrumentFromMidiProgram,
    getInstrumentFromTrackName
} from './instruments.js';

// Legacy MIDI instrument mapping for backwards compatibility
const MIDI_INSTRUMENTS = {
    'piano': 1, 'voice': 54, 'violin': 41, 'viola': 42, 'cello': 43,
    'double-bass': 44, 'strings': 49, 'harp': 47, 'flute': 74,
    'oboe': 69, 'clarinet': 72, 'bassoon': 71, 'saxophone': 67,
    'trumpet': 57, 'horn': 61, 'trombone': 58, 'tuba': 59,
    'drums': 1, 'organ': 20, 'harpsichord': 7, 'default': 1
};

// Track characteristics analysis
const TRACK_CHARACTERISTICS = {
    // Range-based instrument detection
    ranges: {
        // Very high notes (C6 and above) - likely piccolo, flute, or coloratura soprano
        very_high: { min: 84, instruments: ['flute', 'voice'] },
        // High notes (C5-B5) - violin, soprano, trumpet
        high: { min: 72, max: 83, instruments: ['violin', 'voice', 'trumpet'] },
        // Mid-high (C4-B4) - viola, alto, horn
        mid_high: { min: 60, max: 71, instruments: ['violin', 'voice', 'clarinet'] },
        // Mid (C3-B3) - tenor voice, cello
        mid: { min: 48, max: 59, instruments: ['voice', 'cello', 'horn'] },
        // Low (C2-B2) - bass voice, bassoon
        low: { min: 36, max: 47, instruments: ['voice', 'cello', 'bassoon'] },
        // Very low (below C2) - double bass, tuba, bass voice
        very_low: { max: 35, instruments: ['double-bass', 'tuba', 'voice'] }
    }
};

// Sound synthesis configurations for different instrument types
const SYNTH_CONFIGS = {
    piano: {
        oscillator: { type: 'triangle' },
        envelope: { attack: 0.02, decay: 0.3, sustain: 0.1, release: 0.8 }
    },
    voice: {
        oscillator: { type: 'sine' },
        envelope: { attack: 0.1, decay: 0.2, sustain: 0.7, release: 1.2 }
    },
    strings: {
        oscillator: { type: 'sawtooth' },
        envelope: { attack: 0.1, decay: 0.1, sustain: 0.8, release: 1.0 }
    },
    brass: {
        oscillator: { type: 'square' },
        envelope: { attack: 0.05, decay: 0.1, sustain: 0.6, release: 0.8 }
    },
    woodwinds: {
        oscillator: { type: 'triangle' },
        envelope: { attack: 0.05, decay: 0.2, sustain: 0.5, release: 1.0 }
    },
    default: {
        oscillator: { type: 'triangle' },
        envelope: { attack: 0.02, decay: 0.1, sustain: 0.3, release: 0.8 }
    }
};

/**
 * Analyze MIDI track characteristics to suggest instruments
 * @param {Object} midiTrack - MIDI track object from @tonejs/midi
 * @returns {Object} Analysis results with suggested instrument
 */
export function analyzeMIDITrack(midiTrack) {
    if (!midiTrack.notes || midiTrack.notes.length === 0) {
        return { suggestedInstrument: 'default', confidence: 0 };
    }

    const notes = midiTrack.notes;
    const midiNumbers = notes.map(note => note.midi);

    const analysis = {
        noteCount: notes.length,
        minNote: Math.min(...midiNumbers),
        maxNote: Math.max(...midiNumbers),
        avgNote: midiNumbers.reduce((sum, note) => sum + note, 0) / midiNumbers.length,
        range: Math.max(...midiNumbers) - Math.min(...midiNumbers),
        density: notes.length / (midiTrack.duration || 1), // notes per second
        hasChords: hasSimultaneousNotes(notes),
        velocity: {
            min: Math.min(...notes.map(n => n.velocity || 0.7)),
            max: Math.max(...notes.map(n => n.velocity || 0.7)),
            avg: notes.reduce((sum, n) => sum + (n.velocity || 0.7), 0) / notes.length
        }
    };

    // Determine suggested instrument based on characteristics
    let suggestedInstrument = 'default';
    let confidence = 0.5;

    // Range-based detection
    for (const [rangeName, rangeData] of Object.entries(TRACK_CHARACTERISTICS.ranges)) {
        if (isNoteInRange(analysis.avgNote, rangeData)) {
            suggestedInstrument = rangeData.instruments[0]; // Take first suggestion
            confidence = 0.7;
            break;
        }
    }

    // Characteristic-based refinement
    if (analysis.hasChords && analysis.range > 24) {
        // Wide range with chords suggests piano
        suggestedInstrument = 'piano';
        confidence = 0.8;
    } else if (analysis.range < 12 && analysis.density < 2) {
        // Narrow range, low density suggests voice
        suggestedInstrument = 'voice';
        confidence = 0.7;
    }

    return {
        ...analysis,
        suggestedInstrument,
        confidence
    };
}

/**
 * Check if a note falls within a specified range
 */
function isNoteInRange(note, range) {
    if (range.min !== undefined && range.max !== undefined) {
        return note >= range.min && note <= range.max;
    } else if (range.min !== undefined) {
        return note >= range.min;
    } else if (range.max !== undefined) {
        return note <= range.max;
    }
    return false;
}

/**
 * Check if MIDI track has simultaneous notes (chords)
 */
function hasSimultaneousNotes(notes) {
    for (let i = 0; i < notes.length - 1; i++) {
        for (let j = i + 1; j < notes.length; j++) {
            const note1 = notes[i];
            const note2 = notes[j];

            // Check if notes overlap in time
            const overlap = Math.max(0, Math.min(note1.time + note1.duration, note2.time + note2.duration) -
                                     Math.max(note1.time, note2.time));
            if (overlap > 0.01) { // Small threshold for floating point precision
                return true;
            }
        }
    }
    return false;
}

/**
 * Create appropriate synthesizer for an instrument
 * @param {string} instrumentName - Name of the instrument
 * @returns {Object} Tone.js synthesizer instance
 */
export function createSynthForInstrument(instrumentName) {
    // Determine synth category
    let synthCategory = 'default';

    if (['piano', 'pianoforte', 'keyboard'].includes(instrumentName)) {
        synthCategory = 'piano';
    } else if (['voice', 'vocal', 'soprano', 'alto', 'tenor', 'bass', 'singstimme', 'choir'].includes(instrumentName)) {
        synthCategory = 'voice';
    } else if (['violin', 'viola', 'cello', 'double-bass', 'strings', 'harp'].includes(instrumentName)) {
        synthCategory = 'strings';
    } else if (['trumpet', 'horn', 'trombone', 'tuba'].includes(instrumentName)) {
        synthCategory = 'brass';
    } else if (['flute', 'oboe', 'clarinet', 'bassoon', 'saxophone'].includes(instrumentName)) {
        synthCategory = 'woodwinds';
    }

    const config = SYNTH_CONFIGS[synthCategory] || SYNTH_CONFIGS.default;

    // Create polyphonic synthesizer with appropriate configuration
    const synth = new Tone.PolySynth(Tone.Synth, config).toDestination();

    // Add some instrument-specific effects
    addInstrumentEffects(synth, synthCategory);

    return synth;
}

/**
 * Add effects appropriate for instrument category
 */
function addInstrumentEffects(synth, category) {
    try {
        switch (category) {
            case 'voice':
                // Add subtle reverb for voice
                const voiceReverb = new Tone.Reverb(1.2).toDestination();
                synth.connect(voiceReverb);
                break;

            case 'strings':
                // Add longer reverb for strings
                const stringReverb = new Tone.Reverb(2.0).toDestination();
                synth.connect(stringReverb);
                break;

            case 'piano':
                // Add subtle compression for piano
                const compressor = new Tone.Compressor(-30, 3).toDestination();
                synth.connect(compressor);
                break;

            default:
                // No additional effects for default
                break;
        }
    } catch (error) {
        console.warn('Could not add effects for', category, ':', error);
    }
}

/**
 * Create intelligent sound mapping based on track names and MIDI data
 * @param {Object} midiData - Parsed MIDI data from @tonejs/midi
 * @returns {Object} Sound mapping with synthesizers for each track
 */
export function createMIDIBasedSoundMapping(midiData) {
    console.log('Creating MIDI-based sound mapping...');

    const trackMappings = midiData.tracks.map((track, index) => {
        let instrumentType = 'piano'; // Default to piano instead of complex heuristics
        let confidence = 0.3; // Low confidence for default
        let source = 'default';

        // Priority 1: Use track name if available
        if (track.name && track.name.trim()) {
            const nameBasedInstrument = getInstrumentFromTrackName(track.name.trim());
            if (nameBasedInstrument) {
                instrumentType = nameBasedInstrument;
                confidence = 0.9;
                source = `track name "${track.name}"`;
                console.log(`Track ${index}: Track name "${track.name}" -> ${instrumentType}`);
            }
        }

        // Priority 2: Use MIDI instrument program if no track name match
        if (confidence < 0.8 && track.instrument && track.instrument.number !== undefined) {
            const programBasedInstrument = getInstrumentFromMidiProgram(track.instrument.number);
            if (programBasedInstrument !== 'piano') {
                instrumentType = programBasedInstrument;
                confidence = 0.8;
                source = `MIDI program ${track.instrument.number}`;
                console.log(`Track ${index}: MIDI program ${track.instrument.number} -> ${instrumentType}`);
            }
        }

        // Priority 3: Check for program changes as fallback
        if (confidence < 0.7) {
            const programChanges = track.controlChanges?.programNumber || [];
            if (programChanges.length > 0) {
                const midiProgram = programChanges[0].value;
                const programBasedInstrument = getInstrumentFromMidiProgram(midiProgram);
                if (programBasedInstrument !== 'piano') {
                    instrumentType = programBasedInstrument;
                    confidence = 0.7;
                    source = `program change ${midiProgram}`;
                    console.log(`Track ${index}: Program change ${midiProgram} -> ${instrumentType}`);
                }
            }
        }

        // If still no good match, use piano as default
        if (confidence < 0.7) {
            console.log(`Track ${index}: No instrument detected, defaulting to piano`);
            source = 'default (piano)';
        }

        return {
            trackIndex: index,
            instrumentType: instrumentType,
            confidence: confidence,
            source: source,
            trackName: track.name || `Track ${index}`,
            synth: createSynthForInstrument(instrumentType)
        };
    });

    return {
        tracks: trackMappings
    };
}

/**
 * Get MIDI program number for instrument (legacy compatibility)
 * @param {string} instrumentName - Name of the instrument
 * @returns {number} MIDI program number
 */
export function getMidiProgramNumber(instrumentName) {
    return MIDI_INSTRUMENTS[instrumentName] || MIDI_INSTRUMENTS.default;
}

// Re-export functions from instruments.js for convenience
export { getAvailableInstruments, getInstrumentCategory } from './instruments.js';
