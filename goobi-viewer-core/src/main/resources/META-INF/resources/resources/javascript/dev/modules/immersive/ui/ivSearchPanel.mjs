/** Search panel wiring: in-work IIIF content search with hit list and highlights. */

import { search, nextIndex, prevIndex } from '../data/ivFulltextSearch.mjs';
import { createDisposer } from '../shared/ivLifecycle.mjs';

/**
 * Wires the in-place IIIF content search: result list in the left panel,
 * prev/next hit buttons, and hit highlights on the image.
 *
 * @param {IvViewer} viewer
 * @param {string} pi
 * @param {string} apiBase
 * @returns {{destroy: function():void}}
 */
export function setupSearchPanel(viewer, pi, apiBase) {
    const bag = createDisposer();
    const searchState = { hits: [], activeIndex: -1, term: '', failed: false };
    let searchReq = 0;
    const resultsBox = document.getElementById('immersiveSearchResults');
    const resultsList = document.getElementById('immersiveResultsList');
    const hitCounter = document.getElementById('immersiveHitCounter');
    // Only rendered when annotation layout tags are configured
    const annotationsOnlyBox = document.getElementById('immersiveSearchAnnotationsOnly');
    const hitsOnOrder = (order) => searchState.hits.filter((h) => h.order === order);

    const renderHighlights = () => {
        const activeHit = searchState.hits[searchState.activeIndex];
        const rects = viewer.getCurrentPages().flatMap((o) =>
            hitsOnOrder(o)
                .filter((h) => h.rect)
                .map((h) => ({ ...h.rect, order: o, active: h === activeHit }))
        );
        viewer.setHighlights(rects, 'search');
    };

    const renderResults = () => {
        if (!resultsBox || !resultsList) return;
        resultsBox.hidden = !searchState.term;
        const emptyLabel = (searchState.failed ? resultsBox.dataset.labelError : resultsBox.dataset.labelEmpty) || '';
        if (hitCounter) {
            hitCounter.textContent = searchState.hits.length ? `${searchState.activeIndex + 1} / ${searchState.hits.length}` : (searchState.term && emptyLabel) || '';
        }
        resultsList.replaceChildren();
        if (searchState.term && !searchState.hits.length) {
            const empty = document.createElement('li');
            empty.className = 'immersive__results-empty';
            empty.textContent = emptyLabel;
            resultsList.appendChild(empty);
            return;
        }
        searchState.hits.forEach((h, i) => {
            const li = document.createElement('li');
            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'immersive__results-item' + (i === searchState.activeIndex ? ' -active' : '');
            const page = document.createElement('span');
            page.className = 'immersive__results-page';
            page.textContent = h.page;
            const snippet = document.createElement('span');
            snippet.className = 'immersive__results-snippet';
            if (h.before || h.after) {
                const mark = document.createElement('mark');
                mark.className = 'immersive__results-match';
                mark.textContent = h.match || h.snippet || '';
                snippet.append(document.createTextNode(h.before ? `${h.before} ` : ''), mark, document.createTextNode(h.after ? ` ${h.after}` : ''));
            } else {
                snippet.textContent = h.snippet || '';
            }
            btn.append(page, snippet);
            btn.addEventListener('click', () => goToHit(i));
            li.append(btn);
            resultsList.appendChild(li);
        });
    };

    const goToHit = (i) => {
        if (!searchState.hits.length) return;
        searchState.activeIndex = (i + searchState.hits.length) % searchState.hits.length;
        const hit = searchState.hits[searchState.activeIndex];
        if (!viewer.getCurrentPages().includes(hit.order)) viewer.goToPage(hit.order);
        else renderHighlights();
        renderResults();
    };

    /** Only the latest submitted search may render; a slower older response is dropped. */
    const runSearch = async (term) => {
        const req = ++searchReq;
        let hits = [];
        let failed = false;
        if (term) {
            try {
                // undefined keeps the default fetch function and page cap
                hits = await search(pi, apiBase, term, undefined, undefined, { annotationsOnly: !!(annotationsOnlyBox && annotationsOnlyBox.checked) });
            } catch (e) {
                console.error('immersive fulltext search failed', e);
                failed = true;
            }
        }
        if (req !== searchReq) return;
        searchState.term = term;
        searchState.hits = hits;
        searchState.failed = failed;
        searchState.activeIndex = hits.length ? 0 : -1;
        renderResults();
        if (searchState.activeIndex >= 0) goToHit(0);
        else viewer.clearHighlights('search');
    };

    bag.add(viewer.onPageChange.subscribe(renderHighlights));

    const searchPanel = document.getElementById('immersivePanelSearch');
    const searchForm = searchPanel && searchPanel.querySelector('form');
    const searchInput = searchPanel && searchPanel.querySelector('input[type="text"]');
    if (searchForm && searchInput) {
        bag.listen(searchForm, 'submit', (e) => {
            e.preventDefault();
            runSearch(searchInput.value.trim());
        });

        const clearBtn = document.createElement('button');
        clearBtn.type = 'button';
        clearBtn.className = 'immersive__search-clear';
        clearBtn.textContent = '×';
        clearBtn.setAttribute('aria-label', (resultsBox && resultsBox.dataset.labelReset) || '');
        clearBtn.hidden = !searchInput.value;
        const group = searchInput.closest('.input-group') || searchInput.parentElement;
        group.insertBefore(clearBtn, group.querySelector('.input-group-addon') || null);
        bag.add(() => clearBtn.remove());
        const syncClear = () => {
            clearBtn.hidden = !searchInput.value;
        };
        const resetSearch = () => {
            searchInput.value = '';
            syncClear();
            runSearch('');
            searchInput.focus();
        };
        bag.listen(searchInput, 'input', syncClear);
        bag.listen(clearBtn, 'click', resetSearch);
        bag.listen(searchInput, 'keydown', (e) => {
            if (e.key === 'Escape' && searchInput.value) {
                e.preventDefault();
                e.stopPropagation();
                resetSearch();
            }
        });
    }
    if (annotationsOnlyBox) {
        // Re-run the last submitted search in the new scope; nothing to do before the first search
        bag.listen(annotationsOnlyBox, 'change', () => {
            if (searchState.term) runSearch(searchState.term);
        });
    }
    document.querySelectorAll('[data-immersive-hit]').forEach((btn) => {
        bag.listen(btn, 'click', () => {
            const n = searchState.hits.length;
            goToHit(btn.dataset.immersiveHit === 'next' ? nextIndex(searchState.activeIndex, n) : prevIndex(searchState.activeIndex, n));
        });
    });
    return { destroy: bag.dispose };
}
