/*
  Tonal ramp generator for misc/palette.less.
  A step is a CIE L* tone; hue and chroma stay constant along the ramp, so a step
  distance is a lightness distance for every ramp. The hue sits beside the seed hue,
  the way GitHub and Material derive their neutrals from the brand.

  The ramps in the two palette files were generated with (offset -19: towards blue, away from purple):

    node gulp/tonalRamp.js --seed '#2563eb' --offset -19 --chroma 3 --name neutral \
      --steps 0:100,50:98,100:96,150:94,200:91,300:84,350:77,400:70,450:62,500:50,550:44,600:34,700:28,800:22,850:17,900:12,950:6,1000:0
    node gulp/tonalRamp.js --seed '#015999' --offset -19 --chroma 2 --name gray \
      --steps 0:100,50:98,100:96,150:94,200:91,300:84,400:66,500:50,550:44,600:34,700:28,800:22,900:12,1000:0
    node gulp/tonalRamp.js --seed '#015999' --offset -19 --chroma 8 --name slate \
      --steps 100:92,200:86,300:78,400:64,500:46,600:30,700:22,750:17,800:12,900:6,950:3.5

  Prints one `--color-<name>-<step>: #hex;` line per step. Chroma is CIELAB chroma
  (3 reads as a barely tinted grey, 8 as a clear blue-grey); a tone that cannot hold it
  loses chroma until it fits sRGB. --offset rotates the hue in degrees; --hue sets it directly.
*/

const D65 = [0.95047, 1.0, 1.08883];
const toLinear = (c) => (c <= 0.04045 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4);
const fromLinear = (c) => (c <= 0.0031308 ? 12.92 * c : 1.055 * c ** (1 / 2.4) - 0.055);

function hexToRgb(hex) {
    const h = hex.replace('#', '');
    const full = h.length === 3 ? [...h].map((c) => c + c).join('') : h;
    return [0, 2, 4].map((i) => parseInt(full.slice(i, i + 2), 16) / 255);
}

function rgbToLab(rgb) {
    const [r, g, b] = rgb.map(toLinear);
    const xyz = [0.4124564 * r + 0.3575761 * g + 0.1804375 * b, 0.2126729 * r + 0.7151522 * g + 0.072175 * b, 0.0193339 * r + 0.119192 * g + 0.9503041 * b];
    const f = (t) => (t > 216 / 24389 ? Math.cbrt(t) : (841 / 108) * t + 4 / 29);
    const [fx, fy, fz] = xyz.map((v, i) => f(v / D65[i]));
    return [116 * fy - 16, 500 * (fx - fy), 200 * (fy - fz)];
}

function labToRgb([L, a, b]) {
    const fy = (L + 16) / 116;
    const fx = fy + a / 500;
    const fz = fy - b / 200;
    const finv = (t) => (t ** 3 > 216 / 24389 ? t ** 3 : (108 / 841) * (t - 4 / 29));
    const [x, y, z] = [fx, fy, fz].map((v, i) => finv(v) * D65[i]);
    const lin = [3.2404542 * x - 1.5371385 * y - 0.4985314 * z, -0.969266 * x + 1.8760108 * y + 0.041556 * z, 0.0556434 * x - 0.2040259 * y + 1.0572252 * z];
    return lin.map(fromLinear);
}

// A very dark or very light tone cannot hold the full chroma; reduce it until the colour fits sRGB.
function labToRgbInGamut(L, chroma, radians) {
    const rgb = (c) => labToRgb([L, c * Math.cos(radians), c * Math.sin(radians)]);
    const fits = (v) => v.every((channel) => channel >= -1e-4 && channel <= 1 + 1e-4);
    if (fits(rgb(chroma))) return rgb(chroma);
    let low = 0;
    let high = chroma;
    for (let i = 0; i < 24; i++) {
        const middle = (low + high) / 2;
        if (fits(rgb(middle))) low = middle;
        else high = middle;
    }
    return rgb(low);
}

const toHex = (rgb) =>
    '#' +
    rgb
        .map((c) => Math.round(Math.min(1, Math.max(0, c)) * 255).toString(16).padStart(2, '0'))
        .join('');

/**
 * @param {{seed?: string, hue?: number, offset?: number, chroma?: number, steps: Array<[string|number, number]>}} spec
 * @returns {Array<{step: string|number, tone: number, hex: string}>}
 */
function tonalRamp({ seed, hue, offset = 0, chroma = 4, steps }) {
    let h = hue;
    if (h === undefined) {
        if (!seed) throw new Error('tonalRamp: pass --seed or --hue');
        const [, a, b] = rgbToLab(hexToRgb(seed));
        h = (Math.atan2(b, a) * 180) / Math.PI;
    }
    const radians = ((h + offset) * Math.PI) / 180;
    return steps.map(([step, tone]) => {
        // Pure black and white stay pure: chroma at the ends of the ramp only produces clipping.
        const c = tone <= 0 || tone >= 100 ? 0 : chroma;
        return { step, tone, hex: toHex(labToRgbInGamut(tone, c, radians)) };
    });
}

function parseArgs(argv) {
    const args = {};
    for (let i = 0; i < argv.length; i++) {
        if (!argv[i].startsWith('--')) continue;
        const key = argv[i].slice(2);
        const value = argv[i + 1] !== undefined && !argv[i + 1].startsWith('--') ? argv[++i] : true;
        args[key] = value;
    }
    return args;
}

if (require.main === module) {
    const args = parseArgs(process.argv.slice(2));
    if (!args.steps) {
        console.error('usage: node gulp/tonalRamp.js --seed <#hex> [--offset <deg>] [--hue <deg>] [--chroma <n>] --name <ramp> --steps <step:tone,...>');
        process.exit(1);
    }
    const steps = String(args.steps)
        .split(',')
        .map((pair) => pair.split(':'))
        .map(([step, tone]) => [step, Number(tone)]);
    const ramp = tonalRamp({
        seed: args.seed,
        hue: args.hue === undefined ? undefined : Number(args.hue),
        offset: args.offset === undefined ? 0 : Number(args.offset),
        chroma: args.chroma === undefined ? 4 : Number(args.chroma),
        steps,
    });
    const name = args.name || 'neutral';
    for (const { step, tone, hex } of ramp) console.log(`    --color-${name}-${step}: ${hex}; /* L* ${tone} */`);
}

module.exports = { tonalRamp, rgbToLab, labToRgb, hexToRgb, toHex };
