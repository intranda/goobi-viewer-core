/** Unit tests for the stage transitions: tween timing, crossfade opacities and the height-fit band. */
import { jest } from '@jest/globals';
import { tween, crossfade, heightBand } from '../engine/ivTransitions.mjs';

test('heightBand keeps y and height and triples the width around the same centre', () => {
    const rect = {
        x: 10,
        y: 5,
        width: 4,
        height: 8,
        clone() {
            return { ...this };
        },
    };
    const band = heightBand(rect);
    expect(band).toEqual(expect.objectContaining({ x: 6, width: 12, y: 5, height: 8 }));
});

test('tween reports 0→1 over the duration and crossfade mirrors it onto two images', async () => {
    let now = 0;
    const frames = [];
    const raf = jest.spyOn(globalThis, 'requestAnimationFrame').mockImplementation((cb) => {
        frames.push(cb);
        return frames.length;
    });
    try {
        const incoming = { setOpacity: jest.fn() };
        const outgoing = { setOpacity: jest.fn() };
        const done = crossfade(incoming, outgoing, 100);
        while (frames.length) frames.shift()((now += 50));
        await done;
        expect(incoming.setOpacity.mock.calls.map((c) => c[0])).toEqual([0, 0.5, 1]);
        expect(outgoing.setOpacity.mock.calls.map((c) => c[0])).toEqual([1, 0.5, 0]);
        const seen = [];
        const instant = tween(0, (t) => seen.push(t));
        while (frames.length) frames.shift()(now);
        await instant;
        expect(seen).toEqual([1]);
    } finally {
        raf.mockRestore();
    }
});
