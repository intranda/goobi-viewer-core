/** Unit tests for the library adapter: every getter reflects window at read time. */
import { env } from '../shared/ivEnv.mjs';

test('reads the libraries lazily from window', () => {
    expect(env.ImageView).toBeUndefined();
    global.ImageView = { Image: class {} };
    expect(env.ImageView).toBe(global.ImageView);
    delete global.ImageView;
    expect(env.ImageView).toBeUndefined();
    global.riot = { mount() {} };
    global.rxjs = { Subject: class {} };
    global.viewerJS = { widgetToc: {} };
    expect(env.riot).toBe(global.riot);
    expect(env.rxjs).toBe(global.rxjs);
    expect(env.viewerJS).toBe(global.viewerJS);
    global.immersiveFilterConfig = { filters: {} };
    expect(env.immersiveFilterConfig).toBe(global.immersiveFilterConfig);
    delete global.immersiveFilterConfig;
    delete global.riot;
    delete global.rxjs;
    delete global.viewerJS;
});

test('$ prefers window.$ and falls back to window.jQuery', () => {
    const dollar = global.$;
    const jquery = global.jQuery;
    global.$ = undefined;
    global.jQuery = () => 'jq';
    expect(env.$).toBe(global.jQuery);
    global.$ = () => 'dollar';
    expect(env.$).toBe(global.$);
    global.$ = dollar;
    global.jQuery = jquery;
});
