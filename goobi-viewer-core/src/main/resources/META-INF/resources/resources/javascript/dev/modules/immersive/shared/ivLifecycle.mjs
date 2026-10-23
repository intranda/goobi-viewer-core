/** Teardown bookkeeping for the setup modules: one disposer per module, one call undoes everything. */

/** Destroyable that does nothing, returned by setup functions whose markup is not on the page. */
export const noop = Object.freeze({ destroy() {} });

/**
 * Collects listeners and teardown callbacks so a setup module can undo itself.
 *
 * @returns {{listen: Function, add: Function, dispose: Function}}
 */
export function createDisposer() {
    const fns = [];
    return {
        /** addEventListener whose removal is queued for dispose. */
        listen(target, type, handler, options) {
            target.addEventListener(type, handler, options);
            fns.push(() => target.removeEventListener(type, handler, options));
        },
        /** Queues a teardown callback (an unsubscribe or unregister function) and returns it. */
        add(fn) {
            if (typeof fn === 'function') fns.push(fn);
            return fn;
        },
        /** Runs every queued teardown once, newest first; a throwing one is logged and does not stop the rest. */
        dispose() {
            while (fns.length) {
                try {
                    fns.pop()();
                } catch (e) {
                    console.error('immersive: teardown failed', e);
                }
            }
        },
    };
}
