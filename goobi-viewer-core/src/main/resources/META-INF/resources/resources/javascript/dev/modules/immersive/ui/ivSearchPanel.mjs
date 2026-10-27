/** Search panel wiring: in-work IIIF content search with hit list and highlights. */

import { search, nextIndex, prevIndex } from '../data/ivFulltextSearch.mjs';
import { loadLayoutTagIndex } from '../data/ivLayoutTags.mjs';
import { styleClassTokens } from '../shared/ivDom.mjs';
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
    const searchState = { allHits: [], hits: [], activeIndex: -1, term: '', failed: false, tagNames: null, filter: 'all' };
    let searchReq = 0;
    const resultsBox = document.getElementById('immersiveSearchResults');
    const resultsList = document.getElementById('immersiveResultsList');
    const hitCounter = document.getElementById('immersiveHitCounter');
    const filterBox = document.getElementById('immersiveHitFilter');
    const filterBtns = filterBox ? Array.from(filterBox.querySelectorAll('[data-immersive-hit-filter]')) : [];
    const hitsOnOrder = (order) => searchState.hits.filter((h) => h.order === order);
    const annotationHits = () => searchState.allHits.filter((h) => h.layoutTag);
    const filteredHits = () => (searchState.filter === 'annotations' ? annotationHits() : searchState.allHits);

    const loadTagNames = () =>
        loadLayoutTagIndex(pi, apiBase, fetch, document.documentElement.lang)
            .then((index) => new Map(index.tags.map((t) => [t.label, t.name])))
            .catch(() => null);

    const tagLine = (h) => {
        const tag = document.createElement('span');
        tag.className = 'immersive__results-tag';
        const tokens = styleClassTokens(h.styleClass);
        if (tokens.length) {
            const dot = document.createElement('span');
            dot.className = 'immersive__layout-tags-dot';
            dot.classList.add(...tokens);
            dot.setAttribute('aria-hidden', 'true');
            tag.append(dot);
        }
        const label = resultsBox.dataset.labelAnnotation || '';
        const name = searchState.tagNames && searchState.tagNames.get(h.layoutTag);
        if (name && label) {
            const prefix = document.createElement('span');
            prefix.className = 'sr-only';
            prefix.textContent = `${label} `;
            tag.append(prefix);
        }
        tag.append(document.createTextNode(name || label || h.layoutTag));
        return tag;
    };

    const renderHighlights = () => {
        const activeHit = searchState.hits[searchState.activeIndex];
        const rects = viewer.getCurrentPages().flatMap((o) =>
            hitsOnOrder(o)
                .filter((h) => h.rect)
                .map((h) => ({ ...h.rect, order: o, active: h === activeHit }))
        );
        viewer.setHighlights(rects, 'search');
    };

    const renderFilter = () => {
        if (!filterBox) return;
        const annotationCount = annotationHits().length;
        filterBox.hidden = !annotationCount;
        filterBtns.forEach((btn) => {
            const value = btn.dataset.immersiveHitFilter;
            const on = value === searchState.filter;
            btn.classList.toggle('-active', on);
            btn.setAttribute('aria-pressed', String(on));
            const count = btn.querySelector('.immersive__results-filter-count');
            if (count) count.textContent = String(value === 'annotations' ? annotationCount : searchState.allHits.length);
        });
    };

    const renderResults = () => {
        if (!resultsBox || !resultsList) return;
        resultsBox.hidden = !searchState.term;
        renderFilter();
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
            const body = document.createElement('span');
            body.className = 'immersive__results-body';
            const snippet = document.createElement('span');
            snippet.className = 'immersive__results-snippet';
            if (h.before || h.match || h.after) {
                const mark = document.createElement('mark');
                mark.className = 'immersive__results-match';
                mark.textContent = h.match || h.snippet || '';
                snippet.append(document.createTextNode(h.before ? `${h.before} ` : ''), mark, document.createTextNode(h.after ? ` ${h.after}` : ''));
            } else {
                snippet.textContent = h.snippet || '';
            }
            body.append(snippet);
            if (h.layoutTag) body.append(tagLine(h));
            btn.append(page, body);
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

    const showFirstHit = () => {
        searchState.activeIndex = searchState.hits.length ? 0 : -1;
        renderResults();
        if (searchState.activeIndex >= 0) goToHit(0);
        else viewer.clearHighlights('search');
    };

    const setFilter = (filter) => {
        if (filter === searchState.filter) return;
        const activeHit = searchState.hits[searchState.activeIndex];
        searchState.filter = filter;
        searchState.hits = filteredHits();
        const kept = searchState.hits.indexOf(activeHit);
        if (kept < 0) {
            showFirstHit();
            return;
        }
        searchState.activeIndex = kept;
        renderResults();
        renderHighlights();
    };

    /** Only the latest submitted search may render; a slower older response is dropped. */
    const runSearch = async (term) => {
        const req = ++searchReq;
        let hits = [];
        let failed = false;
        let tagNames = null;
        if (term) {
            try {
                hits = await search(pi, apiBase, term);
            } catch (e) {
                console.error('immersive fulltext search failed', e);
                failed = true;
            }
            if (hits.some((h) => h.layoutTag)) tagNames = await loadTagNames();
        }
        if (req !== searchReq) return;
        searchState.term = term;
        searchState.allHits = hits;
        searchState.filter = 'all';
        searchState.hits = filteredHits();
        searchState.failed = failed;
        searchState.tagNames = tagNames;
        showFirstHit();
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
    filterBtns.forEach((btn) => {
        bag.listen(btn, 'click', () => setFilter(btn.dataset.immersiveHitFilter));
    });
    document.querySelectorAll('[data-immersive-hit]').forEach((btn) => {
        bag.listen(btn, 'click', () => {
            const n = searchState.hits.length;
            goToHit(btn.dataset.immersiveHit === 'next' ? nextIndex(searchState.activeIndex, n) : prevIndex(searchState.activeIndex, n));
        });
    });
    return { destroy: bag.dispose };
}
