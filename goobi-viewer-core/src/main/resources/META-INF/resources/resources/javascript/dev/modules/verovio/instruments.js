// instruments.js - Comprehensive MIDI instrument mapping
// Based on General MIDI standard with enhanced categorization and name patterns

/**
 * MIDI Program Number to Instrument Type mapping
 * Maps General MIDI program numbers (1-128) to our internal instrument types
 */
export const midiPrograms = {
    1: "piano",
    2: "piano",
    3: "piano",
    4: "piano",
    5: "piano",
    6: "piano",
    7: "harpsichord",
    8: "harpsichord",
    9: "bell",
    10: "bell",
    11: "bell",
    12: "bell",
    13: "bell",
    14: "bell",
    15: "bell",
    16: "bell",
    17: "organ",
    18: "organ",
    19: "organ",
    20: "organ",
    21: "organ",
    22: "organ",
    23: "organ",
    24: "organ",
    25: "guitar",
    26: "guitar",
    27: "guitar",
    28: "guitar",
    29: "guitar",
    30: "guitar",
    31: "guitar",
    32: "guitar",
    33: "bass",
    34: "bass",
    35: "bass",
    36: "bass",
    37: "bass",
    38: "bass",
    39: "bass",
    40: "bass",
    41: "violin",
    42: "viola",
    43: "cello",
    44: "double-bass",
    45: "violin",
    46: "violin",
    47: "harp",
    48: "drums",
    49: "strings",
    50: "strings",
    51: "strings",
    52: "strings",
    53: "voice",
    54: "voice",
    55: "voice",
    56: "strings",
    57: "trumpet",
    58: "trombone",
    59: "tuba",
    60: "trumpet",
    61: "horn",
    62: "brass",
    63: "brass",
    64: "brass",
    65: "saxophone",
    66: "saxophone",
    67: "saxophone",
    68: "saxophone",
    69: "oboe",
    70: "oboe",
    71: "bassoon",
    72: "clarinet",
    73: "flute",
    74: "flute",
    75: "flute",
    76: "flute",
    77: "flute",
    78: "flute",
    79: "flute",
    80: "flute",
    81: "synth",
    82: "synth",
    83: "synth",
    84: "synth",
    85: "synth",
    86: "synth",
    87: "synth",
    88: "synth",
    89: "synth",
    90: "synth",
    91: "synth",
    92: "synth",
    93: "synth",
    94: "synth",
    95: "synth",
    96: "synth",
    97: "synth",
    98: "synth",
    99: "synth",
    100: "synth",
    101: "synth",
    102: "synth",
    103: "synth",
    104: "synth",
    105: "guitar",
    106: "guitar",
    107: "guitar",
    108: "guitar",
    109: "bell",
    110: "organ",
    111: "violin",
    112: "oboe",
    113: "bell",
    114: "drums",
    115: "drums",
    116: "drums",
    117: "drums",
    118: "drums",
    119: "drums",
    120: "drums",
    121: "guitar",
    122: "voice",
    123: "synth",
    124: "synth",
    125: "synth",
    126: "synth",
    127: "synth",
    128: "synth"
};

/**
 * Track name patterns for instrument detection
 * Maps common track name patterns to instrument types
 * Includes multiple languages and abbreviations
 */
export const namePatterns = {
    "violin": ["violin", "vln", "vl.", "violine", "violino"],
    "viola": ["viola", "vla", "va.", "bratsche"],
    "cello": ["cello", "vc", "vc.", "violoncello", "violoncel", "vcl"],
    "double-bass": ["double bass", "contrabass", "bass", "cb", "db", "kontrabass", "upright bass"],
    "voice": ["soprano", "alto", "tenor", "bass", "voice", "vocal", "singstimme", "gesang", "choir", "chorus"],
    "piano": ["piano", "pianoforte", "klavier", "keyboard", "pf", "pno", "grand piano"],
    "harpsichord": ["harpsichord", "cembalo", "hpschd", "harpsich"],
    "organ": ["organ", "orgel", "org", "church organ", "pipe organ"],
    "flute": ["flute", "fl", "fl.", "flöte", "flauto", "piccolo"],
    "oboe": ["oboe", "ob", "ob.", "hautbois", "english horn"],
    "clarinet": ["clarinet", "cl", "cl.", "klarinette", "clarinetto", "bass clarinet"],
    "bassoon": ["bassoon", "bsn", "bsn.", "fagott", "fagotto", "contrabassoon"],
    "saxophone": ["saxophone", "sax", "sax.", "saxophon", "soprano sax", "alto sax", "tenor sax", "baritone sax"],
    "trumpet": ["trumpet", "tr", "tr.", "trompete", "tromba", "cornet"],
    "horn": ["horn", "hr", "hr.", "french horn", "corno", "waldhorn", "f horn"],
    "trombone": ["trombone", "tb", "tb.", "posaune", "tbn", "bass trombone"],
    "tuba": ["tuba", "tb", "tuba.", "basstuba", "euphonium"],
    "drums": ["drums", "percussion", "perc", "schlagzeug", "batterie", "timpani", "drum set"],
    "guitar": ["guitar", "gtr", "gt", "gitarre", "acoustic guitar", "electric guitar"],
    "bass": ["bass guitar", "electric bass", "acoustic bass", "upright bass", "string bass"],
    "harp": ["harp", "harfe", "arpa", "pedal harp"],
    "bell": ["bells", "glockenspiel", "celesta", "chimes", "tubular bells"],
    "strings": ["strings", "string ensemble", "string section"],
    "brass": ["brass", "brass ensemble", "brass section"],
    "synth": ["synth", "synthesizer", "electronic", "pad", "lead"]
};

/**
 * Instrument categories with metadata
 * Provides grouping and additional information about each instrument type
 */
export const instrumentCategories = {
    "piano": {
        "type": "keyboard",
        "midiPrograms": [1, 2, 3, 4, 5, 6]
    },
    "harpsichord": {
        "type": "keyboard",
        "midiPrograms": [7, 8]
    },
    "organ": {
        "type": "keyboard",
        "midiPrograms": [17, 18, 19, 20, 21, 22, 23, 24, 110]
    },
    "violin": {
        "type": "strings",
        "midiPrograms": [41, 45, 46, 111]
    },
    "viola": {
        "type": "strings",
        "midiPrograms": [42]
    },
    "cello": {
        "type": "strings",
        "midiPrograms": [43]
    },
    "double-bass": {
        "type": "strings",
        "midiPrograms": [44]
    },
    "harp": {
        "type": "strings",
        "midiPrograms": [47]
    },
    "guitar": {
        "type": "strings",
        "midiPrograms": [25, 26, 27, 28, 29, 30, 31, 32, 105, 106, 107, 108, 121]
    },
    "bass": {
        "type": "strings",
        "midiPrograms": [33, 34, 35, 36, 37, 38, 39, 40]
    },
    "trumpet": {
        "type": "brass",
        "midiPrograms": [57, 60]
    },
    "trombone": {
        "type": "brass",
        "midiPrograms": [58]
    },
    "tuba": {
        "type": "brass",
        "midiPrograms": [59]
    },
    "horn": {
        "type": "brass",
        "midiPrograms": [61]
    },
    "brass": {
        "type": "brass",
        "midiPrograms": [62, 63, 64]
    },
    "flute": {
        "type": "woodwinds",
        "midiPrograms": [73, 74, 75, 76, 77, 78, 79, 80]
    },
    "oboe": {
        "type": "woodwinds",
        "midiPrograms": [69, 70, 112]
    },
    "clarinet": {
        "type": "woodwinds",
        "midiPrograms": [72]
    },
    "bassoon": {
        "type": "woodwinds",
        "midiPrograms": [71]
    },
    "saxophone": {
        "type": "woodwinds",
        "midiPrograms": [65, 66, 67, 68]
    },
    "voice": {
        "type": "voice",
        "midiPrograms": [53, 54, 55, 122]
    },
    "strings": {
        "type": "ensemble",
        "midiPrograms": [49, 50, 51, 52, 56]
    },
    "bell": {
        "type": "percussion",
        "midiPrograms": [9, 10, 11, 12, 13, 14, 15, 16, 109, 113]
    },
    "drums": {
        "type": "percussion",
        "midiPrograms": [48, 114, 115, 116, 117, 118, 119, 120]
    },
    "synth": {
        "type": "electronic",
        "midiPrograms": [81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 123, 124, 125, 126, 127, 128]
    }
};

/**
 * Get instrument type from MIDI program number
 * @param {number} programNumber - MIDI program number (1-128)
 * @returns {string} Instrument type or 'piano' as default
 */
export function getInstrumentFromMidiProgram(programNumber) {
    return midiPrograms[programNumber] || 'piano';
}

/**
 * Get instrument type from track name
 * @param {string} trackName - Name of the track
 * @returns {string|null} Instrument type or null if no match
 */
export function getInstrumentFromTrackName(trackName) {
    if (!trackName) return null;

    const name = trackName.toLowerCase().trim();

    // Check against name patterns
    for (const [instrument, patterns] of Object.entries(namePatterns)) {
        for (const pattern of patterns) {
            if (name.includes(pattern)) {
                return instrument;
            }
        }
    }

    return null;
}

/**
 * Get all available instrument types
 * @returns {string[]} Array of instrument type names
 */
export function getAvailableInstruments() {
    return Object.keys(instrumentCategories);
}

/**
 * Get instrument category information
 * @param {string} instrumentType - Instrument type
 * @returns {Object|null} Category information or null if not found
 */
export function getInstrumentCategory(instrumentType) {
    return instrumentCategories[instrumentType] || null;
}
