/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
(function () {
    'use strict';

    var SELECT_ID = 'hb-version-switcher';
    var BANNER_ID = 'hb-version-banner';
    var STYLE_ID = 'hb-version-style';
    var MAX_WAIT_MS = 15000;

    // All versions share one origin, so the Docsify search plugin would share one localStorage index (keyed by
    // '#/page', the same in every version) and show the results of whichever version was indexed first. A namespace per
    // version dir separates them. Must run synchronously: this script is injected after the Docsify scripts, before
    // DOMContentLoaded, when Docsify reads its config. The tagged index.html files can't be changed, hence here.
    if (window.$docsify) {
        window.$docsify.search = window.$docsify.search || {};
        window.$docsify.search.namespace = 'hawkbit-docs' + location.pathname.replace(/[^/]*$/, '');
    }

    function currentPath(versions) {
        var pathname = location.pathname;
        var best = '/';
        for (var i = 0; i < versions.length; i++) {
            var p = versions[i].path;
            if (p && p !== '/' && pathname.indexOf(p) === 0 && p.length > best.length) {
                best = p;
            }
        }
        return best;
    }

    function injectStyle() {
        if (document.getElementById(STYLE_ID)) {
            return;
        }
        var style = document.createElement('style');
        style.id = STYLE_ID;
        style.textContent =
            '#' + SELECT_ID + '{display:block;width:calc(100% - 30px);margin:10px 15px 15px;padding:4px 8px;' +
            'background:#43464A;color:#f0eeea;border:1px solid #0ea5ea;border-radius:4px;font-size:14px;cursor:pointer;}' +
            '#' + SELECT_ID + ':focus{outline:none;border-color:#027abe;box-shadow:0 0 0 2px rgba(14,165,234,.4);}' +
            '#' + SELECT_ID + ' option{background:#43464A;color:#f0eeea;}' +
            // In normal flow inside .content, so it sits below Docsify's top padding and pushes the article down.
            '#' + BANNER_ID + '{position:relative;z-index:1;max-width:80%;margin:0 auto 10px;padding:10px 15px;' +
            'background:#fff4e5;color:#43464A;border-left:4px solid #0ea5ea;border-radius:4px;font-size:15px;line-height:1.5;}' +
            '#' + BANNER_ID + ' a{color:#027abe;font-weight:600;margin-left:6px;text-decoration:underline;}';
        (document.head || document.documentElement).appendChild(style);
    }

    function buildSelect(versions, current) {
        var select = document.createElement('select');
        select.id = SELECT_ID;
        select.setAttribute('aria-label', 'Documentation version');
        for (var i = 0; i < versions.length; i++) {
            var opt = document.createElement('option');
            opt.value = versions[i].path;
            opt.textContent = versions[i].label;
            if (versions[i].path === current) {
                opt.selected = true;
            }
            select.appendChild(opt);
        }
        select.addEventListener('change', function () {
            location.href = select.value + location.hash;
        });
        return select;
    }

    function buildBanner(versions, current) {
        var label = current;
        for (var i = 0; i < versions.length; i++) {
            if (versions[i].path === current) {
                label = versions[i].label;
            }
        }
        var banner = document.createElement('div');
        banner.id = BANNER_ID;
        banner.setAttribute('role', 'note');
        var text = current === '/master/'
            ? 'You are viewing docs for unreleased master.'
            : 'You are viewing docs for an older release (' + label + ').';
        banner.appendChild(document.createTextNode(text));
        var link = document.createElement('a');
        link.textContent = 'Go to the latest release docs';
        link.href = '/' + location.hash;
        banner.appendChild(link);
        return banner;
    }

    function ensureSelect(versions, current) {
        if (document.getElementById(SELECT_ID)) {
            return true;
        }
        var sidebar = document.querySelector('.sidebar');
        if (!sidebar) {
            return false;
        }
        var select = buildSelect(versions, current);
        var appName = sidebar.querySelector('.app-name');
        if (appName && appName.parentNode) {
            appName.parentNode.insertBefore(select, appName.nextSibling);
        } else {
            sidebar.insertBefore(select, sidebar.firstChild);
        }
        return true;
    }

    function ensureBanner(versions, current) {
        if (current === '/' || document.getElementById(BANNER_ID)) {
            return true;
        }
        var content = document.querySelector('section.content');
        var banner = buildBanner(versions, current);
        if (content) {
            content.insertBefore(banner, content.firstChild);
            return true;
        }
        // Body fallback is only used after waiting gave up; until then keep waiting for Docsify.
        return false;
    }

    function init(versions) {
        if (!versions || !versions.length) {
            return;
        }
        var current = currentPath(versions);
        injectStyle();

        var ensureAll = function () {
            var a = ensureSelect(versions, current);
            var b = ensureBanner(versions, current);
            return a && b;
        };

        // Docsify mounts .sidebar / section.content asynchronously, so observe the DOM until both elements are in place,
        // then stop. Route changes only re-render .sidebar-nav and .markdown-section, which don't contain our elements
        // (they're siblings); the hashchange handler below re-checks anyway, as a cheap safety net.
        if (!ensureAll()) {
            var scheduled = false;
            if (window.MutationObserver) {
                var observer = new MutationObserver(function () {
                    if (scheduled) {
                        return;
                    }
                    scheduled = true;
                    setTimeout(function () {
                        scheduled = false;
                        if (ensureAll()) {
                            observer.disconnect();
                        }
                    }, 50);
                });
                observer.observe(document.documentElement, {childList: true, subtree: true});
            } else {
                var interval = setInterval(function () {
                    if (ensureAll()) {
                        clearInterval(interval);
                    }
                }, 500);
            }
        }

        setTimeout(function () {
            if (current !== '/' && !document.getElementById(BANNER_ID) && document.body) {
                document.body.insertBefore(buildBanner(versions, current), document.body.firstChild);
            }
        }, MAX_WAIT_MS);

        window.addEventListener('hashchange', function () {
            ensureAll();
            var link = document.querySelector('#' + BANNER_ID + ' a');
            if (link) {
                link.href = '/' + location.hash;
            }
        });
    }

    function start() {
        if (!window.fetch) {
            return;
        }
        fetch('/versions.json', {cache: 'no-cache'})
            .then(function (res) {
                if (!res.ok) {
                    throw new Error('HTTP ' + res.status);
                }
                return res.json();
            })
            .then(init)
            .catch(function (err) {
                if (window.console) {
                    console.warn('version-switcher: cannot load /versions.json', err);
                }
            });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', start);
    } else {
        start();
    }
})();
