/** Central keydown dispatcher for the immersive view. */

/**
 * Creates the immersive key dispatcher: one document-level keydown listener,
 * handlers consulted in ascending priority order, the first handler that
 * returns true consumes the event. This keeps the overlay precedence (modal →
 * grid → dropdown → panels) declared in one place instead of guard selectors
 * scattered over independent listeners. Pure + tested.
 *
 * @returns {{register:Function, handleEvent:Function, attach:Function}}
 */
export function createKeyDispatcher() {
    const handlers = [];
    return {
        /**
         * @param {number} priority  lower runs first
         * @param {(event: KeyboardEvent) => boolean} handler  true = consumed
         */
        register(priority, handler) {
            handlers.push({ priority, handler });
            handlers.sort((a, b) => a.priority - b.priority);
        },
        /** @returns {boolean} whether any handler consumed the event */
        handleEvent(event) {
            for (const { handler } of handlers) {
                if (handler(event)) return true;
            }
            return false;
        },
        /** Binds the single keydown listener. */
        attach(target) {
            target.addEventListener('keydown', (e) => {
                if (this.handleEvent(e)) e.preventDefault();
            });
        },
    };
}
