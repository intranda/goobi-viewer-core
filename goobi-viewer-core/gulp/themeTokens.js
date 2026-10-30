/*
Theme token report for customer theme updates (`npm run theme:tokens`): which token lines of
a theme still differ from the core defaults, which are redundant, renamed or unknown.
Not a check — nothing here fails a build.
*/

const fs = require('fs');
const path = require('path');
const less = require('less');
const postcss = require('postcss');
const { MODULE_ROOT, LESS_ROOT, parseLess, withoutStrings, COLOR_LITERAL } = require('./tokenCheck');

/* ---------- theme token report (customer theme updates) ---------- */

// Renamed and removed public tokens: report hints and R1 messages. Removed = null.
const RENAMED_TOKENS = new Map([
    // Core renames 10/2026 (grammar --<property>-<role>[-prominence][-state]; single colours read the palette line)
    ['--bg-general', '--bg-page'],
    ['--fg-general', '--fg-default'],
    ['--bg-content-box', '--bg-surface'],
    ['--fg-content-box', '--fg-default'],
    ['--bg-popover', '--bg-elevated'],
    ['--fg-popover', '--fg-default'],
    ['--bg-header', '--header-bg'],
    ['--fg-header', '--header-fg'],
    ['--bg-canvas', '--bg-sunken'],
    ['--bg-image-frame', 'var(--color-image-frame)'],
    ['--fg-heading-content-box', '--fg-heading'],
    ['--fg-link-active', '--fg-link-pressed'],
    ['--fg-link-content-box', '--fg-link-subtle'],
    ['--fg-hover', '--fg-default'],
    ['--fg-inverse', '--fg-on-inverse'],
    ['--fg-on-control', 'var(--color-on-ocr-word)'],
    ['--border-faint', '--border-subtle'],
    ['--border-emphasis', '--border-contrast'],
    ['--brand', '--fg-brand (or --border-brand on border properties)'],
    ['--brand-hover', '--fg-brand-hover'],
    ['--brand-strong', '--bg-brand-strong'],
    ['--bg-brand-tint', '--bg-brand-subtle'],
    ['--accent', '--bg-accent'],
    ['--bg-success', '--bg-success-subtle'],
    ['--fg-on-success', '--fg-on-success-subtle'],
    ['--bg-success-hover', '--bg-success-subtle-hover'],
    ['--bg-success-strong', '--bg-success'],
    ['--fg-on-success-strong', '--fg-on-success'],
    ['--bg-danger', '--bg-danger-subtle'],
    ['--fg-on-danger', '--fg-on-danger-subtle'],
    ['--bg-danger-hover', '--bg-danger-subtle-hover'],
    ['--bg-danger-strong', '--bg-danger'],
    ['--fg-on-danger-strong', '--fg-on-danger'],
    ['--bg-warning', '--bg-warning-subtle'],
    ['--fg-on-warning', '--fg-on-warning-subtle'],
    ['--fg-on-warning-hover', 'var(--color-maintenance-link-hover)'],
    ['--bg-warning-strong', '--bg-warning'],
    ['--fg-on-warning-strong', '--fg-on-warning'],
    ['--bg-info-strong', '--bg-info'],
    ['--fg-on-info-strong', '--fg-on-info'],
    ['--bg-map-marker-primary', 'var(--color-map-marker)'],
    ['--bg-map-marker-secondary', 'var(--color-map-marker-alt)'],
    ['--fg-on-bright', '--fg-black'],
    ['--bg-rss', 'var(--color-rss)'],
    ['--outline-selection', 'var(--color-scroll-marker)'],
    ['--fg-selection-region', 'var(--color-region)'],
    ['--bg-notification', 'var(--color-toast)'],
    ['--highlight', '--bg-highlight'],
    ['--ocr-block', 'var(--color-ocr-block)'],
    ['--ocr-line', 'var(--color-ocr-line)'],
    ['--ocr-word', 'var(--color-ocr-word)'],
    ['--shadow-container', '--shadow-surface'],
    ['--shadow-floating', '--shadow-elevated'],
    ['--bg-viewer-canvas', '--bg-stage'],
    ['--bg-viewer-canvas-stripe', 'var(--_stripe)'],
    ['--bg-document-highlight', 'var(--color-document-highlight)'],
    ['--bg-document-highlight-selected', 'var(--color-document-highlight-selected)'],
    ['--bg-document-selection', 'var(--color-document-selection)'],
    ['--outline-document-focus', 'var(--color-document-focus)'],
    ['--outline-document-drawing', 'var(--color-document-selection)'],
    ['--fg-notation-active', 'var(--color-document-notation)'],
    ['--chart-chronology', 'var(--color-chart-chronology)'],
    ['--bg-entity-scale-handle', 'var(--color-chart-ner)'],
    ['--bg-hero', '--bg-brand-strong'],
    ['--bg-hero-gradient', null],
    ['--color-hero', '--bg-brand-strong'],
    ['--color-hero-gradient', null],
    ['--bg-mask', '--bg-scrim'],
    ['--bg-login-surface', 'var(--color-login-surface)'],
    ['--fg-login-surface', 'var(--color-login-text)'],
    ['--bg-login-field', 'var(--color-login-surface)'],
    ['--fg-login-field', 'var(--color-login-text)'],
    ['--border-login-field', 'var(--color-login-field-border)'],
    ['--fg-login-label', 'var(--color-login-label)'],
    ['--border-login-box', 'var(--color-login-box-border)'],
    ['--bg-login-aside', 'var(--color-login-aside)'],
    ['--fg-login-aside', '--fg-white'],
    ['--bg-login-aside-tile', 'var(--color-login-tile)'],
    ['--bg-login-aside-tile-hover', 'var(--color-login-tile-hover)'],
    ['--radius-container-large', '--view-object-image-radius'],
    ['--font-size-small', '--font-size-caption'],
    ['--font-size-medium', '--font-size-body-small'],
    ['--font-size-normal', '--font-size-body'],
    ['--font-size-large', '--font-size-body-large'],
    ['--font-size-xlarge', '--font-size-heading-small'],
    // Feature layer dissolved: the five remaining feature tokens read their role directly
    ['--widget-body-icon', '--fg-default'],
    ['--widget-topbar-icon', '--fg-brand'],
    ['--datepicker-day-bg-hover', '--bg-hover'],
    ['--datepicker-day-fg-disabled', '--fg-placeholder'],
    ['--rss-badge-fg', '--fg-black'],
    ['--ci-color1-dark', '--color-brand-400'],
    ['--ci-hovercolor1', '--bg-brand-hover'],
    ['--ci-color1', '--color-brand'],
    ['--container-small-border-radius', '--radius-container-small'],
    ['--container-border-radius', '--radius-container'],
    ['--button-border-radius', '--radius-button'],
    ['--container-box-shadow', '--shadow-surface'],
    ['--color-theme-switch-dark-display', '--display-light-only'],
    ['--color-theme-switch-light-display', '--display-dark-only'],
    ['--placeholder', '--fg-placeholder'],
    ['--lk-general', '--fg-link'],
    ['--lk-general-hover', '--fg-link-hover'],
    ['--lk-general-active', '--fg-link-pressed'],
    ['--lk-content-box', '--fg-link-subtle'],
    ['--lh-general', '--fg-heading'],
    ['--lh-content-box', '--fg-heading'],
    ['--bg-active', '--bg-control-pressed'],
    ['--fg-controls', 'var(--color-on-ocr-word)'],
    ['--border-color', '--border-default'],
    ['--accent-color', '--bg-accent'],
    ['--focus-ring', '--outline-focus'],
    ['--mask-opaque', '--bg-scrim'],
    ['--focus-ring-inner', '--outline-focus-inner'],
    ['--print-fg', '--fg-print'],
    ['--print-link', '--fg-print-link'],
    ['--print-border', '--border-print'],
    ['--widget-icon-color-body', '--widget-body-icon'],
    ['--widget-icon-color-topbar', '--widget-topbar-icon'],
    ['--widget-icon-color-expand', '--widget-expand-icon'],
    ['--login-nav-background', '--login-nav-bg'],
    ['--login-nav-background-hover', '--login-nav-bg-hover'],
    ['--login-nav-text-color', '--login-nav-fg'],
    ['--login-nav-text-color-hover', '--login-nav-fg-hover'],
    ['--login-nav-icon-color', '--login-nav-icon'],
    ['--login-nav-icon-color-hover', '--login-nav-icon-hover'],
    ['--login-nav-bookmarks-text-color', '--login-nav-bookmarks-fg'],
    ['--login-nav-bookmarks-text-color-hover', '--login-nav-bookmarks-fg-hover'],
    ['--login-nav-bookmarks-counter-background', '--login-nav-bookmarks-counter-bg'],
    ['--search-toolbar-background-active', '--search-toolbar-bg-active'],
    ['--search-toolbar-text-color-active', '--search-toolbar-fg-active'],
    ['--search-toolbar-icon-color-active', '--search-toolbar-icon-active'],
    ['--search-toolbar-text-color-hover', '--search-toolbar-fg-hover'],
    ['--search-toolbar-focus-outline-color', '--search-toolbar-outline-focus'],
    ['--toc-page-link-color', '--toc-page-link-fg'],
    ['--image-controls-background', '--image-controls-bg'],
    ['--image-controls-container-border-color', '--image-controls-container-border'],
    ['--image-controls-icon-color', '--image-controls-icon'],
    ['--datepicker-day-hover-bg', '--datepicker-day-bg-hover'],
    ['--datepicker-day-selected-bg', '--datepicker-day-bg-selected'],
    ['--datepicker-day-selected-fg', '--datepicker-day-fg-selected'],
    ['--datepicker-day-today-border-color', '--datepicker-day-border-today'],
    ['--datepicker-day-disabled-fg', '--datepicker-day-fg-disabled'],
    ['--datepicker-day-hasdata-bg', null],
    ['--datepicker-day-hasdata-fg', '--datepicker-day-fg-has-data'],
    ['--datepicker-day-hasdata-hover-bg', null],
    ['--datepicker-day-hasdata-hover-fg', '--datepicker-day-fg-has-data-hover'],
    ['--datepicker-day-range-bg', null],
    ['--ns-bg', '--newspaper-search-bg'],
    ['--ns-border-color', '--newspaper-search-border'],
    ['--ns-tab-bg', '--newspaper-search-tab-bg'],
    ['--ns-tab-border-color', '--newspaper-search-tab-border'],
    ['--ns-tab-fg', '--newspaper-search-tab-fg'],
    ['--ns-tab-fg-hover', '--newspaper-search-tab-fg-hover'],
    ['--ns-tab-active-color', '--newspaper-search-tab-fg-active'],
    ['--ns-label-fg', '--newspaper-search-label-fg'],
    ['--quick-filter-text-color', '--quick-filter-fg'],
    ['--quick-filter-dropdown-hover-bg', '--quick-filter-dropdown-bg-hover'],
    ['--quick-filter-invalid-color', '--quick-filter-fg-invalid'],
    ['--quick-filter-accent-color', '--quick-filter-accent'],
    ['--quick-filter-submit-icon-color', '--quick-filter-submit-icon'],
    ['--quick-filter-checkbox-active', '--quick-filter-checkbox-bg-active'],
    ['--quick-filter-status-color', '--quick-filter-status-fg'],
    ['--wc-dot-size', '--widget-calendar-dot-size'],
    ['--wc-dot-color', '--widget-calendar-dot-bg'],
    ['--wc-dot-color-on-selected', '--widget-calendar-dot-bg-selected'],
    ['--wc-popover-bg', '--widget-calendar-popover-bg'],
    ['--wc-popover-border', '--widget-calendar-popover-border'],
    ['--wc-popover-shadow', '--widget-calendar-popover-shadow'],
    ['--wc-popover-radius', '--widget-calendar-popover-radius'],
    ['--wc-popover-min-width', '--widget-calendar-popover-min-width'],
    ['--wc-popover-item-padding', '--widget-calendar-popover-item-padding'],
    ['--wc-popover-item-color', '--widget-calendar-popover-item-fg'],
    ['--wc-popover-item-hover-bg', '--widget-calendar-popover-item-bg-hover'],
    ['--wc-popover-item-hover-color', '--widget-calendar-popover-item-fg-hover'],
    ['--wc-popover-font-size', '--widget-calendar-popover-font-size'],
    ['--combined-facets-color', '--combined-facets-fg'],
    ['--combined-facets-accent-contrast', '--combined-facets-fg-on-accent'],
    ['--combined-facets-muted', '--combined-facets-fg-muted'],
    ['--combined-facets-box', '--combined-facets-box-bg'],
    ['--combined-facets-badge-background', '--combined-facets-badge-bg'],
    ['--combined-facets-badge-background-hover', '--combined-facets-badge-bg-hover'],
    ['--combined-facets-badge-color', '--combined-facets-badge-fg'],
    ['--combined-facets-badge-muted', '--combined-facets-badge-fg-muted'],
    ['--combined-facets-badge-contrast', '--combined-facets-badge-fg-contrast'],
    ['--active-facets-badge-background', '--active-facets-badge-bg'],
    ['--active-facets-badge-background-hover', '--active-facets-badge-bg-hover'],
    ['--active-facets-badge-color', '--active-facets-badge-fg'],
    ['--active-facets-badge-muted', null],
    ['--active-facets-badge-contrast', '--active-facets-badge-fg-contrast'],
    ['--facet-control-color', null],
    ['--archives-title-color', '--archives-title-fg'],
    ['--archives-description-color', '--archives-description-fg'],
    ['--archives-placeholder-icon-color', '--archives-placeholder-icon'],
    ['--ner-person-color', '--ner-person-fg'],
    ['--ner-location-color', '--ner-location-fg'],
    ['--ner-institution-color', '--ner-institution-fg'],
    ['--sidebar-preview-highlight', '--sidebar-preview-outline'],
    ['--image-controls-region-active-color', '--image-controls-region-fg-active'],
    ['--pdf-selected-highlight-bg', '--pdf-highlight-bg-selected'],
    ['--image-region-focus-color', '--image-region-outline-focus'],
    ['--image-region-drawing-color', '--image-region-outline-drawing'],
    ['--chronology-chart-color', '--chronology-chart-fg'],
    ['--mei-note-active-color', '--mei-note-fg-active'],
    ['--login-nav-bookmarks-background', null],
    ['--login-nav-dropdown-border-color', null],
    ['--search-toolbar-divider-color', null],
    ['--search-toolbar-icon-color', null],
    ['--search-toolbar-icon-color-hover', null],
    ['--toc-page-hover-bg', null],
    ['--toc-page-toggle-icon-color', null],
    ['--toc-page-toggle-icon-color-hover', '--toc-page-toggle-icon-hover'],
    ['--toc-page-link-color-hover', null],
    ['--toc-page-border-color', null],
    ['--image-controls-border-color', null],
    ['--image-controls-icon-color-hover', '--fg-on-control-hover'],
    ['--image-controls-icon-color-active', '--image-controls-icon-active'],
    ['--image-controls-action-background', null],
    ['--image-controls-action-background-hover', '--bg-control-hover'],
    ['--image-controls-slider-thumb-background', null],
    ['--image-controls-slider-border-color', '--image-controls-slider-border'],
    ['--quick-filter-label-color', '--quick-filter-label-fg'],
    ['--quick-filter-control-hover-bg', '--quick-filter-control-bg-hover'],
    ['--immersive-chrome-hover-bg', '--immersive-chrome-bg-hover'],
    ['--immersive-chrome-border-color', null],
    ['--immersive-key-border-color', null],
    ['--immersive-metadata-hover-bg', null],
    ['--combined-facets-background', '--bg-brand-tint'],
    ['--facet-control-active', null],
    ['--facet-control-active-fg', null],
    ['--archives-overlay-text', null],
    ['--numeric-paginator-active-bg', '--numeric-paginator-bg-active'],
    ['--calendar-day-hover-bg', '--calendar-day-bg-hover'],
    ['--calendar-day-hover-fg', '--calendar-day-fg-hover'],
    ['--ner-navigation-hover-bg', '--ner-navigation-bg-hover'],
    ['--entity-work-color', '--entity-work-bg'],
    ['--entity-person-color', '--entity-person-bg'],
    ['--entity-agent-color', '--entity-agent-bg'],
    ['--entity-award-color', '--entity-award-bg'],
    ['--entity-event-color', '--entity-event-bg'],
    ['--border', '--border-faint (a color now — readers write border: 1px solid var(--border-faint))'],
    ['--border-controls', '--border-input (a color now — readers write border: 1px solid var(--border-input))'],
    ['--archives-card-border', '--archives-card-border-color (a color now)'],
    ['--immersive-accent-rgb', '--immersive-accent (a color, no rgb triplet)'],
    ['--immersive-ocr-block-rgb', '--immersive-ocr-block (a color)'],
    ['--immersive-ocr-line-rgb', '--immersive-ocr-line (a color)'],
    ['--immersive-ocr-word-rgb', '--immersive-ocr-word (a color)'],
    ['--widget-expand-icon', '--brand'],
    ['--login-nav-bg', '--bg-inverse'],
    ['--login-nav-bg-hover', '--bg-inverse-hover'],
    ['--login-nav-fg', '--fg-inverse'],
    ['--login-nav-fg-hover', '--fg-inverse'],
    ['--login-nav-icon', '--fg-inverse'],
    ['--login-nav-icon-hover', '--fg-inverse'],
    ['--login-nav-bookmarks-fg', '--fg-inverse'],
    ['--login-nav-bookmarks-fg-hover', '--highlight'],
    ['--login-nav-bookmarks-counter-bg', '--bg-brand'],
    ['--search-toolbar-bg-active', '--bg-brand'],
    ['--search-toolbar-fg-active', '--fg-on-brand'],
    ['--search-toolbar-icon-active', '--fg-on-brand'],
    ['--search-toolbar-button-radius', '--radius-button'],
    ['--search-toolbar-outline-focus', '--outline-focus'],
    ['--toc-page-link-fg', '--fg-link-content-box'],
    ['--image-controls-bg', '--bg-content-box'],
    ['--image-controls-icon', '--fg-general'],
    ['--datepicker-bg', '--bg-popover'],
    ['--datepicker-header-bg', '--bg-brand'],
    ['--datepicker-header-fg', '--fg-on-brand'],
    ['--datepicker-weekday-fg', '--fg-on-brand'],
    ['--datepicker-day-fg', '--fg-popover'],
    ['--datepicker-day-bg-selected', '--bg-brand'],
    ['--datepicker-day-fg-selected', '--fg-on-brand'],
    ['--datepicker-day-border-today', '--brand'],
    ['--datepicker-day-fg-has-data', '--fg-popover'],
    ['--datepicker-day-fg-has-data-hover', '--fg-popover'],
    ['--quick-filter-bg', '--bg-content-box'],
    ['--quick-filter-fg', '--fg-content-box'],
    ['--quick-filter-dropdown-bg', '--bg-content-box'],
    ['--quick-filter-fields-bg', '--bg-subtle'],
    ['--quick-filter-dropdown-bg-hover', '--bg-subtle'],
    ['--quick-filter-toggle-bg', '--bg-subtle'],
    ['--quick-filter-fg-invalid', '--fg-danger'],
    ['--quick-filter-accent', '--brand'],
    ['--quick-filter-submit-bg', '--bg-brand'],
    ['--quick-filter-submit-icon', '--fg-on-brand'],
    ['--quick-filter-checkbox-bg-active', '--bg-brand'],
    ['--quick-filter-status-fg', '--brand'],
    ['--immersive-canvas-bg', '--bg-canvas'],
    ['--immersive-chrome-bg', '--bg-content-box'],
    ['--immersive-chrome-fg', '--fg-content-box'],
    ['--immersive-accent', '--brand'],
    ['--immersive-ocr-block', '--ocr-block'],
    ['--immersive-ocr-line', '--ocr-line'],
    ['--immersive-ocr-word', '--ocr-word'],
    ['--widget-calendar-dot-bg', '--brand'],
    ['--widget-calendar-dot-bg-selected', '--fg-on-brand'],
    ['--widget-calendar-popover-bg', '--bg-popover'],
    ['--widget-calendar-popover-border', '--border-default'],
    ['--widget-calendar-popover-item-fg', '--fg-popover'],
    ['--widget-calendar-popover-item-bg-hover', '--bg-hover'],
    ['--widget-calendar-popover-item-fg-hover', '--fg-popover'],
    ['--widget-calendar-popover-font-size', '--font-size-caption'],
    ['--combined-facets-fg', '--fg-general'],
    ['--combined-facets-accent', '--brand'],
    ['--combined-facets-accent-bg', '--bg-brand'],
    ['--combined-facets-fg-on-accent', '--fg-on-brand'],
    ['--combined-facets-divider', '--border-default'],
    ['--combined-facets-fg-muted', '--fg-muted'],
    ['--combined-facets-box-bg', '--bg-general'],
    ['--combined-facets-badge-bg', '--bg-brand'],
    ['--combined-facets-badge-bg-hover', '--bg-brand-hover'],
    ['--combined-facets-badge-fg', '--fg-on-brand'],
    ['--combined-facets-badge-fg-muted', null],
    ['--combined-facets-badge-fg-contrast', '--brand'],
    ['--active-facets-badge-bg', '--bg-brand'],
    ['--active-facets-badge-bg-hover', '--bg-brand-hover'],
    ['--active-facets-badge-fg', '--fg-on-brand'],
    ['--active-facets-badge-radius', '--radius-button'],
    ['--active-facets-badge-fg-contrast', '--brand'],
    ['--footer-topnav-bg', '--brand-strong'],
    ['--archives-card-bg', '--bg-content-box'],
    ['--archives-title-fg', '--fg-content-box'],
    ['--archives-description-fg', '--fg-general'],
    ['--archives-placeholder-bg', '--bg-content-box'],
    ['--archives-placeholder-icon', '--fg-general'],
    ['--ner-person-fg', '--fg-entity-person'],
    ['--ner-location-fg', '--fg-entity-location'],
    ['--ner-institution-fg', '--fg-entity-institution'],
    ['--map-marker-primary-bg', '--bg-map-marker-primary'],
    ['--map-marker-primary-fg', '--fg-inverse'],
    ['--map-marker-secondary-bg', '--bg-map-marker-secondary'],
    ['--map-marker-secondary-fg', '--fg-on-bright'],
    ['--rss-badge-bg', '--bg-rss'],
    ['--sidebar-preview-outline', '--outline-selection'],
    ['--image-controls-region-fg-active', '--fg-selection-region'],
    ['--pdf-highlight-bg', '--bg-document-highlight'],
    ['--pdf-highlight-bg-selected', '--bg-document-highlight-selected'],
    ['--pdf-selection-bg', '--bg-document-selection'],
    ['--image-region-outline-focus', '--outline-document-focus'],
    ['--image-region-outline-drawing', '--outline-document-drawing'],
    ['--ner-scale-handle-bg', '--bg-entity-scale-handle'],
    ['--chronology-chart-fg', '--chart-chronology'],
    ['--archives-hero-bg', '--bg-hero'],
    ['--archives-hero-gradient-bg', '--bg-hero-gradient'],
    ['--mei-note-fg-active', '--fg-notation-active'],
    ['--search-toolbar-fg-hover', null],
    ['--search-toolbar-shadow-active', null],
    ['--image-controls-container-border', null],
    ['--quick-filter-dropdown-shadow', null],
    ['--quick-filter-input-shadow', null],
    ['--combined-facets-badge-border', null],
    ['--active-facets-badge-border', null],
    ['--widget-calendar-dot-size', null],
    ['--widget-calendar-popover-min-width', null],
    ['--widget-calendar-popover-item-padding', null],
    ['--immersive-thumb-shadow', null],
    ['--immersive-thumb-shadow-hover', null],
    ['--widget-calendar-popover-shadow', null],
    ['--quick-filter-radius', '--radius-button'],
    ['--quick-filter-radius-sm', '--radius-button'],
    ['--immersive-radius', '--radius-container-small'],
    ['--immersive-radius-lg', '--radius-container'],
    ['--widget-calendar-popover-radius', '--radius-container-small'],
    ['--combined-facets-radius', '--radius-button'],
    ['--facet-control-radius', '--radius-container-small'],
    ['--quick-filter-control-bg-hover', '--bg-control'],
    ['--immersive-chrome-bg-hover', '--bg-control'],
    ['--immersive-key-bg', '--bg-control'],
    ['--image-controls-icon-active', '--fg-on-control-hover'],
    ['--image-controls-slider-border', null],
    ['--quick-filter-border', null],
    ['--toc-page-toggle-icon-hover', '--fg-content-box'],
    ['--quick-filter-label-fg', '--fg-muted'],
    ['--immersive-rail-bg', '--bg-popover'],
    ['--immersive-ocr-word-fg', '--fg-on-control'],
    ['--archives-card-border-color', '--border-faint'],
    ['--immersive-chevron-scrim', null],
    ['--newspaper-search-bg', null],
    ['--newspaper-search-border', null],
    ['--newspaper-search-label-fg', null],
    ['--newspaper-search-tab-bg', null],
    ['--newspaper-search-tab-border', null],
    ['--newspaper-search-tab-fg', null],
    ['--newspaper-search-tab-fg-active', null],
    ['--newspaper-search-tab-fg-hover', null],
    ['--datepicker-border-color', null],
    ['--active-facets-badge-more-color', null],
    ['--facet-boolean-background', null],
    ['--archives-card-shadow-hover', null],
    ['--widget-toc-hover-bg', null],
    ['--widget-toc-toggle-icon-color', null],
    ['--widget-toc-toggle-icon-color-hover', null],
    ['--widget-toc-link-color-hover', null],
    ['--widget-toc-active-color', null],
    // Layer 3 dissolved: components read roles directly; a successor exists only where a role was made for it
    ['--bg-control-active', null],
    ['--active-facets-badge-fg-muted', null],
    ['--active-facets-badge-shadow-hover', null],
    ['--archives-overlay-bg', null],
    ['--archives-overlay-fg', null],
    ['--combined-facets-bg', '--bg-brand-tint'],
    ['--combined-facets-scrollbar', null],
    ['--datepicker-day-bg-has-data', null],
    ['--datepicker-day-bg-has-data-hover', null],
    ['--datepicker-day-bg-in-range', null],
    ['--facet-control-bg-active', null],
    ['--facet-control-border', null],
    ['--facet-control-fg', null],
    ['--facet-control-fg-active', null],
    ['--image-controls-action-bg', null],
    ['--image-controls-action-bg-hover', '--bg-control-hover'],
    ['--image-controls-border', null],
    ['--image-controls-icon-hover', '--fg-on-control-hover'],
    ['--image-controls-slider-thumb-bg', null],
    ['--immersive-canvas-fg', null],
    ['--immersive-chrome-border', null],
    ['--immersive-chrome-shadow', '--shadow-floating'],
    ['--immersive-divider', null],
    ['--immersive-edge-inset', null],
    ['--immersive-highlight-bg', null],
    ['--immersive-key-border', null],
    ['--immersive-logo-invert', '--filter-mono-image'],
    ['--immersive-metadata-bg-hover', null],
    ['--immersive-overlay-shadow', null],
    ['--immersive-panel-shadow', '--shadow-floating'],
    ['--immersive-rail-width', null],
    ['--immersive-slider-track', null],
    ['--immersive-transition', null],
    ['--login-nav-bookmarks-bg', null],
    ['--login-nav-dropdown-border', null],
    ['--quick-filter-field-separator', null],
    ['--search-toolbar-divider', null],
    ['--search-toolbar-icon', null],
    ['--search-toolbar-icon-hover', null],
    ['--theme-logo-dark-display', '--display-dark-only'],
    ['--theme-logo-light-display', '--display-light-only'],
    ['--theme-switch-dark-display', '--display-light-only'],
    ['--theme-switch-light-display', '--display-dark-only'],
    ['--toc-page-bg-hover', null],
    ['--toc-page-border', null],
    ['--toc-page-link-fg-hover', null],
    ['--toc-page-toggle-icon', null],
    ['--color-warning-surface-dark', null],
    ['--fg-on-accent', null],
    ['--fg-on-highlight', null],
]);
const renameHint = (name) => (RENAMED_TOKENS.has(name) ? (RENAMED_TOKENS.get(name) ? `renamed — now ${RENAMED_TOKENS.get(name)}` : 'removed — delete the read') : null);
const THEME_BUCKETS = ['light', 'dark', 'base', 'palette', 'root'];
const VERDICT_ORDER = ['unknown', 'renamed', 'keep', 'pair', 'redundant'];

function themeBucket(stack) {
    for (const header of stack) {
        if (/^\.mx-theme-light(\(\))?$/.test(header)) return 'light';
        if (/^\.mx-theme-dark(\(\))?$/.test(header)) return 'dark';
        if (/^\.mx-theme-base\(\)$/.test(header)) return 'base';
        if (/^\.mx-theme-palette\(\)$/.test(header)) return 'palette';
    }
    return 'root';
}

function normalizeValue(value) {
    return value
        .trim()
        .toLowerCase()
        .replace(/#([0-9a-f])([0-9a-f])([0-9a-f])(?![0-9a-f])/g, '#$1$1$2$2$3$3')
        .replace(/\s*,\s*/g, ',')
        .replace(/\s+/g, ' ');
}

function resolveTokens(value, lookup, depth = 0) {
    if (depth > 12) return value;
    const next = value.replace(/var\(\s*(--[\w-]+)\s*(?:,\s*([^()]*(?:\([^()]*\))?[^()]*))?\)/g, (match, name, fallback) => {
        const found = lookup(name);
        if (found !== undefined) return found;
        return fallback !== undefined ? fallback.trim() : match;
    });
    return next === value ? value : resolveTokens(next, lookup, depth + 1);
}

/**
 * Compares the token overrides of a theme with the core defaults of the same mode.
 *
 * @param {string[]} sources LESS sources of the theme (mixins.less, variables.less)
 * @param {{light: Map<string,string>, dark: Map<string,string>}} core emitted core tokens per mode
 * @returns {Array<{token: string, mode: string, line: number, verdict: string, theme?: string, core?: string, literal?: boolean, hint?: string}>}
 */
function classifyThemeTokens(sources, core) {
    const declared = Object.fromEntries(THEME_BUCKETS.map((bucket) => [bucket, new Map()]));
    for (const source of sources) for (const d of parseLess(source).declarations) declared[themeBucket(d.stack)].set(d.name, { value: d.value, line: d.line });
    const coreHas = (name) => core.light.has(name) || core.dark.has(name);
    const lookupFor = (mode) => {
        const theme = new Map([...declared.palette, ...declared.base, ...declared.root, ...declared[mode]].map(([name, d]) => [name, d.value]));
        return (name) => (theme.has(name) ? theme.get(name) : core[mode].get(name));
    };
    const coreLookup = (mode) => (name) => core[mode].get(name);
    const rows = [];
    for (const bucket of THEME_BUCKETS) {
        for (const [name, { value, line }] of declared[bucket]) {
            const modes = bucket === 'light' || bucket === 'dark' ? [bucket] : ['light', 'dark'];
            const row = { token: name, mode: bucket, line, theme: value, literal: COLOR_LITERAL.test(withoutStrings(value)) };
            if (!coreHas(name)) {
                const renamed = RENAMED_TOKENS.get(name);
                const target = renamed && /^--[\w-]+$/.test(renamed) && coreHas(renamed) ? renamed : null;
                if (
                    target &&
                    modes.every((mode) => normalizeValue(resolveTokens(value, lookupFor(mode))) === normalizeValue(resolveTokens(core[mode].get(target) ?? '', coreLookup(mode))))
                ) {
                    rows.push({
                        ...row,
                        verdict: 'redundant',
                        core: modes.map((mode) => core[mode].get(target)).join(' | '),
                        hint: `old name of ${target}; equals its core default`,
                    });
                } else if (renamed) rows.push({ ...row, verdict: 'renamed', hint: `now ${renamed}` });
                else
                    rows.push({
                        ...row,
                        verdict: 'unknown',
                        hint: RENAMED_TOKENS.has(name) ? 'removed — delete the line' : 'no core token of this name — theme-only feature or removed',
                    });
                continue;
            }
            row.core = modes.map((mode) => core[mode].get(name)).join(' | ');
            const same = modes.every(
                (mode) => normalizeValue(resolveTokens(value, lookupFor(mode))) === normalizeValue(resolveTokens(core[mode].get(name) ?? '', coreLookup(mode)))
            );
            row.verdict = same ? 'redundant' : 'keep';
            if (!same && row.literal && bucket !== 'palette') row.hint = 'color literal — declare it as --color-* in .mx-theme-palette() and read it here';
            rows.push(row);
        }
    }
    for (const [mode, other] of [
        ['light', 'dark'],
        ['dark', 'light'],
    ]) {
        for (const [name, { line }] of declared[mode]) {
            if (declared[other].has(name) || !coreHas(name)) continue;
            if (rows.some((r) => r.token === name && r.mode === mode && r.verdict === 'keep'))
                rows.push({ token: name, mode, line, verdict: 'pair', hint: `add a value to .mx-theme-${other}() as well` });
        }
    }
    return rows.sort((a, b) => VERDICT_ORDER.indexOf(a.verdict) - VERDICT_ORDER.indexOf(b.verdict) || a.token.localeCompare(b.token) || a.mode.localeCompare(b.mode));
}

/**
 * Compiles the core token layer and classifies the overrides of a theme against it.
 *
 * @param {string} themeLessRoot the theme's css/less directory
 * @param {{moduleRoot?: string, lessRoot?: string}} [options]
 * @returns {Promise<Array>} rows of classifyThemeTokens
 */
async function themeTokenReport(themeLessRoot, { moduleRoot = MODULE_ROOT, lessRoot = LESS_ROOT } = {}) {
    const root = path.resolve(moduleRoot, lessRoot);
    const filename = path.join(root, 'constructor.less');
    const output = await less.render(fs.readFileSync(filename, 'utf8'), { filename, paths: [root, path.join(root, 'crowdsourcing')] });
    const light = new Map();
    const dark = new Map();
    postcss.parse(output.css).walkRules((rule) => {
        for (let parent = rule.parent; parent.type !== 'root'; parent = parent.parent) if (parent.type === 'atrule') return;
        if (rule.selector === ':root') rule.walkDecls(/^--/, (d) => light.set(d.prop, d.value) && dark.set(d.prop, d.value));
        else if (rule.selector === '.color-theme-dark') rule.walkDecls(/^--/, (d) => dark.set(d.prop, d.value));
    });
    const themeRoot = path.resolve(moduleRoot, themeLessRoot);
    const sources = ['misc/mixins.less', 'misc/variables.less']
        .filter((file) => fs.existsSync(path.join(themeRoot, file)))
        .map((file) => fs.readFileSync(path.join(themeRoot, file), 'utf8'));
    return classifyThemeTokens(sources, { light, dark });
}

const VERDICT_TEXT = {
    unknown: 'unknown — no core token of this name (theme-only feature, or removed: delete)',
    renamed: 'renamed — the core token has a new name (rename the line, keep the value)',
    keep: 'keep — differs from the core default',
    pair: 'pair — declared in one mode only',
    redundant: 'redundant — equals the core default (delete)',
};

function formatThemeTokenReport(rows) {
    const lines = [VERDICT_ORDER.map((v) => `${v} ${rows.filter((r) => r.verdict === v).length}`).join(' · ')];
    for (const verdict of VERDICT_ORDER) {
        const group = rows.filter((r) => r.verdict === verdict);
        if (!group.length) continue;
        lines.push('', VERDICT_TEXT[verdict]);
        for (const r of group) {
            const value = r.theme === undefined ? '' : `  ${r.theme}`;
            const core = r.core === undefined ? '' : `  → core ${r.core}`;
            lines.push(`  ${r.token}  ${r.mode}:${r.line}${value}${core}${r.hint ? `  — ${r.hint}` : ''}`);
        }
    }
    return lines.join('\n');
}

module.exports = { RENAMED_TOKENS, classifyThemeTokens, themeTokenReport, formatThemeTokenReport };
