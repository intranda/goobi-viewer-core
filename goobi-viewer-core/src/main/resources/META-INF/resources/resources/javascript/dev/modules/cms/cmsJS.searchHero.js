/**
 * Loads the three query-driven CMS hero images and changes one work at a time.
 * The surrounding search, links and collection facts are rendered by JSF.
 *
 * @module cmsJS.searchHero
 */
var cmsJS = (function (cms) {
    'use strict';

    var _module = {};
    var _heroes = [];
    var _imageSelector = '.cms-search-hero__tile-img';
    var _interval = 10000;
    var _rows = 18;

    _module.request = function (url) {
        return fetch(url, { headers: { Accept: 'application/json' } }).then(function (response) {
            if (!response.ok) {
                throw new Error('HTTP ' + response.status);
            }
            return response.json();
        });
    };

    _module.shuffle = function (records) {
        for (var i = records.length - 1; i > 0; i--) {
            var j = Math.floor(Math.random() * (i + 1));
            var record = records[i];
            records[i] = records[j];
            records[j] = record;
        }
        return records;
    };

    /** Only complete records and unique identifiers enter a pool; queries define its content. */
    _module.normalize = function (items, appUrl) {
        var seen = new Set();
        return (items || []).reduce(function (records, item) {
            var match = /\/records\/([^/]+)\//.exec(item['@id'] || '');
            var label = Array.isArray(item.label) ? item.label[0] : item.label;
            if (label && typeof label === 'object') {
                label = label['@value'] || label.value;
            }
            label = typeof label === 'string' ? label.trim() : '';
            var thumbnail = Array.isArray(item.thumbnail) ? item.thumbnail[0] : item.thumbnail;
            var thumb = thumbnail && thumbnail['@id'];
            if (!match || !label || !thumb || thumb.indexOf('/full/') < 0 || seen.has(match[1])) {
                return records;
            }
            seen.add(match[1]);
            var rendering = [].concat(item.rendering || []).find(function (entry) {
                return entry && entry.format === 'text/html' && entry['@id'];
            });
            records.push({
                pi: match[1],
                label: label,
                thumb: thumb,
                url: rendering ? rendering['@id'] : appUrl + 'image/' + encodeURIComponent(match[1]) + '/1/',
            });
            return records;
        }, []);
    };

    function poolFor(context, slot, requests) {
        var filter = (slot.element.getAttribute('data-search-hero-query') || '').trim();
        var query = 'ISWORK:true' + (filter ? ' AND (' + filter + ')' : '');
        var url = context.api + 'records/list?rows=' + _rows + '&query=' + encodeURIComponent(query);
        if (!requests.has(url)) {
            requests.set(
                url,
                _module
                    .request(url + '&sort=random_' + Math.floor(Math.random() * 1000000))
                    .catch(function () {
                        // Indexes without random sort support still provide a usable pool.
                        return _module.request(url);
                    })
                    .then(function (data) {
                        return _module.shuffle(_module.normalize(data.orderedItems, context.api.replace(/api\/v\d+\/?$/, '')));
                    })
            );
        }
        return requests.get(url);
    }

    function available(context, slot, record) {
        return !context.failed.has(record.pi) && !context.reserved.has(record.pi) && (!slot.current || record.pi !== slot.current.pi);
    }

    function hasAlternatives(context) {
        return context.slots.some(function (slot) {
            return (
                slot.current &&
                slot.pool.some(function (record) {
                    return available(context, slot, record);
                })
            );
        });
    }

    function updateGalleryVisibility(context) {
        if (!context.active) {
            return;
        }
        context.element.classList.toggle(
            '-gallery-empty',
            context.slots.every(function (slot) {
                return slot.settled && !slot.current;
            })
        );
    }

    function unavailable(slot) {
        if (slot.current) {
            return;
        }
        slot.element.classList.add('-unavailable');
        slot.element.hidden = true;
        slot.element.removeAttribute('href');
        slot.element.setAttribute('tabindex', '-1');
        slot.element.setAttribute('aria-hidden', 'true');
    }

    function canRotate(context) {
        if (!context.active || document.hidden) {
            return false;
        }
        var bounds = context.gallery.getBoundingClientRect();
        return bounds.bottom > 0 && bounds.right > 0 && bounds.top < window.innerHeight && bounds.left < window.innerWidth;
    }

    function release(context, slot, record) {
        if (record && context.reserved.get(record.pi) === slot) {
            context.reserved.delete(record.pi);
        }
    }

    function nextRecord(context, slot) {
        for (var i = 0; i < slot.pool.length; i++) {
            var record = slot.pool[i];
            if (available(context, slot, record)) {
                slot.pool.splice(i, 1);
                slot.pool.push(record);
                context.reserved.set(record.pi, slot);
                return record;
            }
        }
        return null;
    }

    function loadImage(record, size) {
        return new Promise(function (resolve, reject) {
            var image = new Image();
            image.decoding = 'async';
            image.onload = function () {
                resolve(image.src);
            };
            image.onerror = reject;
            image.src = record.thumb.replace(/\/full\/[^/]+\//, '/full/!' + size + ',' + size + '/');
        });
    }

    function show(context, slot, record, src) {
        var element = slot.element;
        var layers = element.querySelectorAll(_imageSelector);
        var shown = element.querySelector(_imageSelector + '.-shown');
        var next = layers[0] === shown ? layers[1] : layers[0];
        release(context, slot, slot.current);
        slot.current = record;
        next.src = src;
        next.alt = record.label;
        if (shown) {
            shown.classList.remove('-shown');
        }
        next.classList.add('-shown');
        element.setAttribute('data-search-hero-pi', record.pi);
        element.setAttribute('href', record.url);
        element.setAttribute('aria-label', context.openLabel ? record.label + ' – ' + context.openLabel : record.label);
        var caption = element.querySelector('[data-search-hero-caption]');
        if (caption) {
            caption.textContent = record.label;
        }
        element.classList.add('-loaded');
        element.classList.remove('-unavailable');
        element.hidden = false;
        element.removeAttribute('tabindex');
        element.removeAttribute('aria-hidden');
    }

    async function fillSlot(context, slot, initial) {
        try {
            var record;
            while (context.active && (record = nextRecord(context, slot))) {
                var src;
                try {
                    src = await loadImage(record, slot.size);
                } catch (error) {
                    context.failed.add(record.pi);
                    release(context, slot, record);
                    continue;
                }
                if (!context.active || (!initial && !canRotate(context))) {
                    release(context, slot, record);
                    return;
                }
                show(context, slot, record, src);
                return;
            }
            if (context.active) {
                unavailable(slot);
            }
        } finally {
            slot.settled = true;
            updateGalleryVisibility(context);
        }
    }

    // Each timeout begins after the previous image settled, never overlapping another change.
    function schedule(context) {
        if (!context.active || context.timer || context.changing || !hasAlternatives(context)) {
            return;
        }
        context.timer = setTimeout(async function () {
            context.timer = null;
            if (canRotate(context)) {
                for (var checked = 0; checked < context.slots.length; checked++) {
                    var slot = context.slots[context.index++ % context.slots.length];
                    if (
                        slot.current &&
                        slot.pool.some(function (record) {
                            return available(context, slot, record);
                        })
                    ) {
                        context.changing = true;
                        await fillSlot(context, slot, false);
                        context.changing = false;
                        break;
                    }
                }
            }
            schedule(context);
        }, _interval);
    }

    function initHero(element, requests) {
        var context = {
            element: element,
            gallery: element.querySelector('.cms-search-hero__gallery') || element,
            api: element.getAttribute('data-search-hero-api') || '',
            openLabel: element.getAttribute('data-search-hero-open-label') || '',
            slots: [],
            reserved: new Map(),
            failed: new Set(),
            active: true,
            index: 0,
            timer: null,
            changing: false,
        };
        context.slots = Array.from(element.querySelectorAll('[data-search-hero-slot]')).map(function (tile) {
            tile.classList.remove('-loaded', '-unavailable');
            tile.hidden = false;
            tile.removeAttribute('href');
            tile.removeAttribute('data-search-hero-pi');
            tile.setAttribute('tabindex', '-1');
            tile.setAttribute('aria-hidden', 'true');
            tile.querySelectorAll(_imageSelector).forEach(function (image) {
                image.classList.remove('-shown');
            });
            return { element: tile, size: tile.getAttribute('data-search-hero-size') || '900', pool: [], current: null, settled: false };
        });
        _heroes.push(context);
        updateGalleryVisibility(context);
        Promise.all(
            context.slots.map(async function (slot) {
                try {
                    var pool = await poolFor(context, slot, requests);
                    if (context.active) {
                        slot.pool = pool.slice();
                    }
                } catch (error) {
                    if (context.active) {
                        unavailable(slot);
                        slot.settled = true;
                        updateGalleryVisibility(context);
                    }
                }
            })
        ).then(async function () {
            if (!context.active) {
                return;
            }
            // Let narrowly curated slots reserve their works before broader queries do.
            await Promise.all(
                context.slots
                    .slice()
                    .sort(function (a, b) {
                        return a.pool.length - b.pool.length;
                    })
                    .map(function (slot) {
                        return fillSlot(context, slot, true);
                    })
            );
            if (context.active) {
                schedule(context);
            }
        });
    }

    _module.init = function () {
        var elements = Array.from(document.querySelectorAll('[data-search-hero]'));
        if (
            elements.length === _heroes.length &&
            elements.every(function (element, index) {
                return _heroes[index].element === element;
            })
        ) {
            return;
        }
        _module.destroy();
        var requests = new Map();
        elements.forEach(function (element) {
            initHero(element, requests);
        });
    };

    _module.destroy = function () {
        _heroes.forEach(function (context) {
            context.active = false;
            clearTimeout(context.timer);
        });
        _heroes = [];
    };

    cms.searchHero = _module;
    return cms;
})(cmsJS || {});

if (typeof module !== 'undefined' && module.exports) {
    module.exports = cmsJS.searchHero;
}
