/**
 * This file is part of the Goobi viewer - a content presentation and management
 * application for digitized objects.
 *
 * Visit these websites for more information. - http://www.intranda.com -
 * http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 *
 * @version 25.03
 * @module adminJS.logViewer
 * @description Module for the admin log viewer with WebSocket streaming and REST polling fallback.
 */
var adminJS = (function (admin) {
    'use strict';

    // ── Constants ─────────────────────────────────────────
    const BUFFER_MAX = 2000;
    const RENDER_INTERVAL_MS = 500;
    const WS_TIMEOUT_MS = 5000;
    const POLL_INTERVAL_MS = 3000;
    const WS_RECONNECT_BASE_MS = 1000;
    const WS_RECONNECT_MAX_MS = 30000;

    // ── State ─────────────────────────────────────────────
    let root = null;
    let apiBase = '';
    let wsBase = '';
    let buffer = [];
    let bufferDirty = false;
    let autoScroll = true;
    let currentLogFile = null;
    let ws = null;
    let pollTimer = null;
    let reconnectTimer = null;
    let reconnectDelay = WS_RECONNECT_BASE_MS;
    let renderTimer = null;
    let pollOffset = 0;
    let isPolling = false;
    let adminContent = null;
    let domObserver = null;
    let filterDebounce = null;
    let pollFailCount = 0;

    // ── Helpers ───────────────────────────────────────────
    const $ = (sel) => root.querySelector(sel);
    const $$ = (sel) => root.querySelectorAll(sel);

    // ── Connection ────────────────────────────────────────
    const getApiUrl = (logFile) => apiBase + '/' + logFile;

    const getWsUrl = (logFile) => {
        const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:';
        return protocol + '//' + location.host + wsBase + '?logfile=' + logFile;
    };

    const connect = () => {
        const timeout = setTimeout(fallbackToPolling, WS_TIMEOUT_MS);
        try {
            ws = new WebSocket(getWsUrl(currentLogFile));
            ws.onopen = () => {
                clearTimeout(timeout);
                reconnectDelay = WS_RECONNECT_BASE_MS;
                pollFailCount = 0;
                setStatus('live');
                stopPolling();
            };
            ws.onmessage = (e) => {
                try {
                    const data = JSON.parse(e.data);
                    // Server sends a JSON array per flush cycle (one sendText call per session)
                    if (Array.isArray(data)) {
                        data.forEach(addLine);
                    } else {
                        addLine(data);
                    }
                } catch (x) {
                    /* malformed JSON */
                }
            };
            ws.onclose = () => {
                clearTimeout(timeout);
                if (!isPolling) fallbackToPolling();
            };
            ws.onerror = () => clearTimeout(timeout);
        } catch (e) {
            clearTimeout(timeout);
            fallbackToPolling();
        }
    };

    const fallbackToPolling = () => {
        isPolling = true;
        setStatus('polling');
        if (!pollTimer) pollTimer = setInterval(poll, POLL_INTERVAL_MS);
        scheduleReconnect();
    };

    const scheduleReconnect = () => {
        if (reconnectTimer) return;
        reconnectTimer = setTimeout(() => {
            reconnectTimer = null;
            reconnectDelay = Math.min(reconnectDelay * 2, WS_RECONNECT_MAX_MS);
            isPolling = false;
            if (ws) {
                ws.close();
                ws = null;
            }
            connect();
        }, reconnectDelay);
    };

    const stopPolling = () => {
        if (pollTimer) {
            clearInterval(pollTimer);
            pollTimer = null;
        }
        if (reconnectTimer) {
            clearTimeout(reconnectTimer);
            reconnectTimer = null;
        }
        isPolling = false;
    };

    const poll = () => {
        fetch(getApiUrl(currentLogFile) + '?sinceOffset=' + pollOffset + '&t=' + Date.now())
            .then((r) => r.json())
            .then((d) => {
                pollFailCount = 0;
                pollOffset = d.nextOffset || pollOffset;
                (d.lines || []).forEach(addLine);
            })
            .catch(() => {
                pollFailCount++;
                if (pollFailCount >= 3) {
                    setStatus('error');
                }
            });
    };

    const loadInitial = () => {
        fetch(getApiUrl(currentLogFile) + '?t=' + Date.now())
            .then((r) => r.json())
            .then((d) => {
                pollOffset = d.nextOffset || 0;
                (d.lines || []).forEach(addLine);
                renderBuffer();
                if (autoScroll) scrollToBottom();
            })
            .catch((err) => console.warn('LogViewer initial load error:', err));
    };

    // ── Buffer ────────────────────────────────────────────
    const addLine = (line) => {
        buffer.push(line);
        if (buffer.length > BUFFER_MAX) buffer.shift();
        bufferDirty = true;
    };

    const startRenderLoop = () => {
        if (renderTimer) return;
        renderTimer = setInterval(() => {
            if (!bufferDirty) return;
            bufferDirty = false;
            renderBuffer();
            if (autoScroll) scrollToBottom();
        }, RENDER_INTERVAL_MS);
    };

    const stopRenderLoop = () => {
        if (renderTimer) {
            clearInterval(renderTimer);
            renderTimer = null;
        }
    };

    // ── Rendering ─────────────────────────────────────────
    const esc = (s) => (s || '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

    const LEVEL_CLASSES = {
        ERROR: 'logviewer__line-level--error',
        WARN: 'logviewer__line-level--warn',
        INFO: 'logviewer__line-level--info',
        DEBUG: 'logviewer__line-level--debug',
        TRACE: 'logviewer__line-level--trace',
    };

    const highlightLine = (line) => {
        let html = esc(line);
        const tokens = [];

        const protect = (span) => {
            tokens.push(span);
            return '\x00T' + (tokens.length - 1) + '\x00';
        };

        // ── Phase 1: Extract URLs and strings ───────────────────────
        html = html.replace(/(https?:\/\/[^\s"')\]&]*(?:&amp;[^\s"')\]&]*)*)/g, (url) => {
            return protect(
                '<a href="' + url + '" class="logviewer__hl-url" target="_blank" rel="noopener">' + url + '</a>'
            );
        });

        html = html.replace(/(&quot;)((?:[^&]|&(?!quot;))*)(&quot;)/g, (m, open, inner, close) => {
            return protect('<span class="logviewer__hl-str">' + open + inner + close + '</span>');
        });

        html = html.replace(/(')([^']*?)(')/g, (m, open, inner, close) => {
            return protect('<span class="logviewer__hl-str">' + open + inner + close + '</span>');
        });

        // ── Phase 2: Structural patterns ────────────────────────────
        html = html.replace(/(~?\[)([^\]]*?)(\])\s*$/g, (m, open, inner, close) => {
            const hl = inner.replace(/([\d][\d.]*[\w]*)/g, '<span class="logviewer__hl-ver">$1</span>');
            return protect('<span class="logviewer__hl-jar">' + open + hl + close + '</span>');
        });

        html = html.replace(/^(Caused by:)/, (m) => {
            return protect('<span class="logviewer__hl-cause">' + m + '</span>');
        });

        // ── Phase 3: bat-style token patterns ───────────────────────
        html = html.replace(/(\w+)(=)/g, (m, key, eq) => {
            return protect(
                '<span class="logviewer__hl-key">' + key + '</span><span class="logviewer__hl-op">' + eq + '</span>'
            );
        });

        html = html.replace(/\b(\d{4}[-/]\d{2}[-/]\d{2})\b/g, (m) => {
            return protect('<span class="logviewer__hl-date">' + m + '</span>');
        });

        html = html.replace(/\b((?:[01]\d|2[0-3]):[0-5]\d:[0-5]\d(?:\.\d+)?)\b/g, function (m) {
            return protect('<span class="logviewer__hl-num">' + m + '</span>');
        });

        html = html.replace(
            /\b((?:(?:25[0-5]|2[0-4]\d|[01]?\d\d?)\.){3}(?:25[0-5]|2[0-4]\d|[01]?\d\d?))\b/g,
            function (m) {
                return protect('<span class="logviewer__hl-ip">' + m + '</span>');
            }
        );

        html = html.replace(/\b(fail(?:ure|ed)?|error|exception|fatal|critical)\b/gi, function (m) {
            return protect('<span class="logviewer__hl-exc">' + m + '</span>');
        });

        // ── Phase 4: Number catch-all (only unprotected text remains) ─
        html = html.replace(/\b(0x[0-9a-fA-F]{2,})\b/g, '<span class="logviewer__hl-num">$1</span>');
        html = html.replace(/\b(\d+\.\d+)\b/g, '<span class="logviewer__hl-num">$1</span>');
        html = html.replace(/\b(\d{2,})\b/g, '<span class="logviewer__hl-num">$1</span>');

        // ── Phase 5: Restore all tokens ─────────────────────────────
        html = html.replace(/\x00T(\d+)\x00/g, function (m, idx) {
            return tokens[parseInt(idx)];
        });

        return html;
    };

    const highlightMessage = (msg) => {
        if (!msg) return '';
        return msg
            .split('\n')
            .map(function (line) {
                var indent = '      ';
                if (/^Caused by:/.test(line)) {
                    indent = '  ';
                }
                return indent + highlightLine(line);
            })
            .join('\n');
    };

    const renderBuffer = () => {
        const levelSel = $('#levelFilter').value;
        const textSel = ($('#textFilter').value || '').toLowerCase();
        let html = '';

        buffer.forEach((l) => {
            if (levelSel !== 'ALL' && l.level && l.level !== levelSel) return;
            const full = (
                (l.timestamp || '') +
                ' ' +
                (l.level || '') +
                ' ' +
                (l.location || '') +
                ' ' +
                (l.message || '')
            ).toLowerCase();
            if (textSel && full.indexOf(textSel) === -1) return;

            const levelLower = (l.level || '').toLowerCase().trim();
            const lineClass =
                'logviewer__line' +
                (levelLower === 'error' ? ' logviewer__line--error' : '') +
                (levelLower === 'warn' ? ' logviewer__line--warn' : '');

            // Continuation line (no level) — render message only
            if (!l.level) {
                html +=
                    '<div class="logviewer__line logviewer__line--continuation">' +
                    '<span class="logviewer__line-message">' +
                    highlightLine(l.message || '') +
                    '</span></div>';
                return;
            }

            // Header line: LEVEL  TIMESTAMP [thread] full.location
            const levelPad = (l.level || '').length < 5 ? ' '.repeat(5 - (l.level || '').length) : '';

            let headerHtml =
                '<span class="logviewer__line-level ' +
                (LEVEL_CLASSES[l.level] || '') +
                '">' +
                esc(l.level) +
                '</span>' +
                levelPad +
                ' <span class="logviewer__line-timestamp">' +
                esc(l.timestamp) +
                '</span>' +
                ' <span class="logviewer__line-dim">[' +
                esc(l.thread || 'main') +
                ']</span>' +
                ' <span class="logviewer__line-location">' +
                esc(l.location || '') +
                '</span>';

            // Message with syntax highlighting
            let msgHtml = '';
            if (l.message) {
                msgHtml = '\n<span class="logviewer__line-message">' + highlightMessage(l.message) + '</span>';
            }

            html += '<div class="' + lineClass + '">' + headerHtml + msgHtml + '</div>';
        });

        // Search highlight: mark matches in rendered HTML (skip tags)
        if (textSel && textSel.length >= 3) {
            var escaped = textSel.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
            var re = new RegExp('(' + escaped + ')', 'gi');
            html = html.replace(/(<[^>]*>)|([^<]+)/g, function (m, tag, text) {
                if (tag) return tag;
                return text.replace(re, '<mark class="logviewer__hl-search">$1</mark>');
            });
        }

        $('#logOutput').innerHTML = html;
    };

    const applyFilters = () => {
        renderBuffer();
        scrollToBottom();
    };

    const applyFiltersDebounced = () => {
        if (filterDebounce) clearTimeout(filterDebounce);
        filterDebounce = setTimeout(applyFilters, 300);
    };

    // ── UI State ──────────────────────────────────────────
    const setStatus = (mode) => {
        var dotClass = 'logviewer__tab-dot logviewer__tab-dot--' + mode;
        $$('.logviewer__tab-dot').forEach((d) => {
            d.className = dotClass;
        });
    };

    const scrollToBottom = () => {
        const out = $('#logOutput');
        out.scrollTop = out.scrollHeight;
        autoScroll = true;
        updateScrollBtn();
    };

    const updateScrollBtn = () => {
        var btn = $('#btnScrollBottom');
        if (btn) btn.style.display = autoScroll ? 'none' : 'flex';
    };

    const switchTab = (logFile) => {
        if (ws) {
            ws.close();
            ws = null;
        }
        stopPolling();
        stopRenderLoop();
        $$('.logviewer__tab').forEach((t) => {
            const active = t.dataset.logfile === logFile;
            t.className = 'logviewer__tab' + (active ? ' logviewer__tab--active' : '');
        });
        currentLogFile = logFile;
        autoScroll = true;
        reconnectDelay = WS_RECONNECT_BASE_MS;
        pollFailCount = 0;
        buffer = [];
        bufferDirty = false;
        pollOffset = 0;
        $('#logOutput').innerHTML = '';
        loadInitial();
        connect();
        startRenderLoop();
    };

    // ── Layout ───────────────────────────────────────────
    const lockViewport = () => {
        adminContent = root.closest('.admin__content');
        if (!adminContent) return;
        var rect = adminContent.getBoundingClientRect();
        adminContent.style.position = 'fixed';
        adminContent.style.top = '3.5rem';
        adminContent.style.bottom = '1rem';
        adminContent.style.left = rect.left + 'px';
        adminContent.style.right = '0';
        adminContent.style.overflow = 'hidden';
    };

    const unlockViewport = () => {
        if (!adminContent) return;
        adminContent.style.position = '';
        adminContent.style.top = '';
        adminContent.style.bottom = '';
        adminContent.style.left = '';
        adminContent.style.right = '';
        adminContent.style.overflow = '';
        adminContent = null;
    };

    const resizeOutput = () => {
        // Update fixed left position on resize
        if (adminContent) {
            var sidebar = document.querySelector('.admin__sidebar-wrapper');
            if (sidebar) adminContent.style.left = sidebar.offsetWidth + 'px';
        }

        var out = $('#logOutput');
        if (!out) return;
        var top = out.getBoundingClientRect().top;
        var pad = 16;
        var available = window.innerHeight - top - pad;
        out.style.height = available + 'px';

        var wrapper = root.closest('.admin__content-wrapper');
        if (wrapper) {
            var blocks = wrapper.querySelectorAll('.admin__content-side .admin__default-block');
            var h = root.offsetHeight + 'px';
            blocks.forEach(function (b) {
                b.style.maxHeight = h;
            });
        }
    };

    // ── Lifecycle ─────────────────────────────────────────
    const onScroll = () => {
        const out = $('#logOutput');
        var atBottom = out.scrollTop + out.clientHeight >= out.scrollHeight - 10;
        if (atBottom && !autoScroll) {
            autoScroll = true;
            updateScrollBtn();
        } else if (!atBottom && autoScroll) {
            autoScroll = false;
            updateScrollBtn();
        }
    };

    const onWheel = (e) => {
        var out = $('#logOutput');
        var atTop = out.scrollTop === 0;
        var atBottom = out.scrollTop + out.clientHeight >= out.scrollHeight - 1;
        if ((atTop && e.deltaY < 0) || (atBottom && e.deltaY > 0)) {
            e.preventDefault();
        }
    };

    const init = (rootElement) => {
        root = rootElement;
        apiBase = root.dataset.apiBase;
        wsBase = root.dataset.wsBase;

        // Bind events
        $('#levelFilter').addEventListener('change', applyFilters);
        $('#textFilter').addEventListener('input', applyFiltersDebounced);
        $('#logOutput').addEventListener('scroll', onScroll);
        $('#logOutput').addEventListener('wheel', onWheel, { passive: false });
        $('#btnScrollBottom').addEventListener('click', scrollToBottom);

        // Bind tab events
        $$('.logviewer__tab').forEach((tab) => {
            tab.addEventListener('click', () => switchTab(tab.dataset.logfile));
        });

        // Lock content area to viewport, size output dynamically
        lockViewport();
        resizeOutput();
        window.addEventListener('resize', resizeOutput);

        // Watch for DOM removal (JSF AJAX navigation)
        domObserver = new MutationObserver(() => {
            if (!document.contains(root)) {
                destroy();
            }
        });
        domObserver.observe(document.body, { childList: true, subtree: true });

        // Activate first tab
        const firstTab = root.querySelector('.logviewer__tab');
        if (firstTab) switchTab(firstTab.dataset.logfile);
    };

    const destroy = () => {
        if (ws) {
            ws.close();
            ws = null;
        }
        stopPolling();
        stopRenderLoop();
        if (filterDebounce) {
            clearTimeout(filterDebounce);
            filterDebounce = null;
        }

        // Remove all event listeners
        var logOutput = $('#logOutput');
        if (logOutput) {
            logOutput.removeEventListener('scroll', onScroll);
            logOutput.removeEventListener('wheel', onWheel);
        }
        var levelFilter = $('#levelFilter');
        if (levelFilter) levelFilter.removeEventListener('change', applyFilters);
        var textFilter = $('#textFilter');
        if (textFilter) textFilter.removeEventListener('input', applyFiltersDebounced);
        var scrollBtn = $('#btnScrollBottom');
        if (scrollBtn) scrollBtn.removeEventListener('click', scrollToBottom);

        // Stop DOM observer
        if (domObserver) {
            domObserver.disconnect();
            domObserver = null;
        }

        unlockViewport();
        window.removeEventListener('resize', resizeOutput);
    };

    // ── Public API ────────────────────────────────────────
    admin.logViewer = { init, destroy };
    return admin;
})(adminJS || {});
