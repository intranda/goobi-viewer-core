/** Unit tests for the setup-module teardown helper. */
import { jest } from '@jest/globals';
import { createDisposer, noop } from '../shared/ivLifecycle.mjs';

test('listen adds a listener that dispose removes, honouring the options', () => {
    const bag = createDisposer();
    const el = document.createElement('div');
    const seen = jest.fn();
    bag.listen(el, 'click', seen, { capture: true });
    el.dispatchEvent(new Event('click'));
    bag.dispose();
    el.dispatchEvent(new Event('click'));
    expect(seen).toHaveBeenCalledTimes(1);
});

test('add registers teardown callbacks; dispose runs them newest first and only once', () => {
    const bag = createDisposer();
    const order = [];
    bag.add(() => order.push('first'));
    expect(bag.add(() => order.push('second'))).toEqual(expect.any(Function));
    bag.add(undefined);
    bag.dispose();
    bag.dispose();
    expect(order).toEqual(['second', 'first']);
});

test('noop is a destroyable that does nothing', () => {
    expect(() => noop.destroy()).not.toThrow();
});
