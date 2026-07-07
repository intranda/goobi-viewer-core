/** Search panel wiring: in-work IIIF content search with hit list and highlights. */

import { search, nextIndex, prevIndex } from './ivFulltextSearch.mjs';

/**
 * Wires the in-place IIIF content search: result list in the left panel,
 * prev/next hit buttons, and hit highlights on the image.
 *
 * @param {IvViewer} viewer
 * @param {string} pi
 * @param {string} apiBase
 */
export function setupFulltextSearch(viewer, pi, apiBase) {
    const searchState = { hits: [], activeIndex: -1, term: '' };
    const resultsBox = document.getElementById('immersiveSearchResults');
    const resultsList = document.getElementById('immersiveResultsList');
    const hitCounter = document.getElementById('immersiveHitCounter');
    const hitsOnOrder = (order) => searchState.hits.filter((h) => h.order === order);

    const renderHighlights = () => {
        const activeHit = searchState.hits[searchState.activeIndex];
        const rects = viewer.getCurrentPages().flatMap((o) =>
            hitsOnOrder(o)
                .filter((h) => h.rect)
                .map((h) => ({ ...h.rect, order: o, active: h === activeHit }))
        );
        viewer.setHighlights(rects);
    };

    const renderResults = () => {
        if (!resultsBox || !resultsList) return;
        resultsBox.hidden = !searchState.term;
        if (hitCounter) {
            hitCounter.textContent = searchState.hits.length
                ? `${searchState.activeIndex + 1} / ${searchState.hits.length}`
                : (searchState.term && resultsBox.dataset.labelEmpty) || '';
        }
        resultsList.innerHTML = '';
        if (searchState.term && !searchState.hits.length) {
            const empty = document.createElement('li');
            empty.className = 'immersive__results-empty';
            empty.textContent = resultsBox.dataset.labelEmpty || '';
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

    const runSearch = async (term) => {
        searchState.term = term;
        searchState.hits = term
            ? await search(pi, apiBase, term).catch((e) => {
                  console.error('immersive fulltext search failed', e);
                  return [];
              })
            : [];
        searchState.activeIndex = searchState.hits.length ? 0 : -1;
        renderResults();
        if (searchState.activeIndex >= 0) goToHit(0);
        else viewer.clearHighlights();
    };

    viewer.onPageChange.subscribe(renderHighlights);

    const searchPanel = document.getElementById('immersivePanelSearch');
    const searchForm = searchPanel && searchPanel.querySelector('form');
    const searchInput = searchPanel && searchPanel.querySelector('input[type="text"]');
    if (searchForm && searchInput) {
        searchForm.addEventListener('submit', (e) => {
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
        const syncClear = () => {
            clearBtn.hidden = !searchInput.value;
        };
        const resetSearch = () => {
            searchInput.value = '';
            syncClear();
            runSearch('');
            searchInput.focus();
        };
        searchInput.addEventListener('input', syncClear);
        clearBtn.addEventListener('click', resetSearch);
        searchInput.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && searchInput.value) {
                e.preventDefault();
                e.stopPropagation();
                resetSearch();
            }
        });
    }
    document.querySelectorAll('[data-immersive-hit]').forEach((btn) => {
        btn.addEventListener('click', () => {
            const n = searchState.hits.length;
            goToHit(btn.dataset.immersiveHit === 'next' ? nextIndex(searchState.activeIndex, n) : prevIndex(searchState.activeIndex, n));
        });
    });
}
