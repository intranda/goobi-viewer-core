/** Serialises page navigations: one at a time, always released, with a watchdog against a lost library event. */

/**
 * @param {number} watchdogMs  last-resort release when a navigation never settles
 * @returns {{busy: boolean, run: Function, onRelease: Function, destroy: Function}}
 */
export function createNavLock(watchdogMs) {
    let busy = false;
    let watchdog = null;
    const releaseHooks = [];
    return {
        get busy() {
            return busy;
        },
        /**
         * Runs `task` (returns the settle promise) under the lock; `onStuck` cleans up if the watchdog fires first.
         *
         * @param {() => Promise|any} task
         * @param {Function} [onStuck]
         */
        run(task, onStuck) {
            busy = true;
            let released = false;
            const release = () => {
                if (released) return;
                released = true;
                clearTimeout(watchdog);
                busy = false;
                releaseHooks.forEach((hook) => hook());
            };
            watchdog = setTimeout(() => {
                try {
                    if (onStuck) onStuck();
                } catch {}
                release();
            }, watchdogMs);
            try {
                Promise.resolve(task())
                    .catch(() => {})
                    .finally(release);
            } catch {
                release();
            }
        },
        /** Registers a callback that runs after every release (queued navigations pick up here). */
        onRelease(fn) {
            releaseHooks.push(fn);
        },
        destroy() {
            clearTimeout(watchdog);
        },
    };
}
