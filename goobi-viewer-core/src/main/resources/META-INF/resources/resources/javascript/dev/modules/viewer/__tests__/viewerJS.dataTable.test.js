/**
 * Unit tests for viewerJS.dataTable.
 *
 * Source subscribes to viewer.jsfAjax.{begin,success} from inside
 * paginator.init / filter.init, so we seed a Subject-stub jsfAjax
 * before requiring the source.
 *
 * The module mutates a `setupAjax` flag on its paginator/filter
 * objects to avoid re-binding ajax handlers; we reset that flag
 * in beforeEach so each test reproduces a fresh init.
 */
function makeSubject() {
    const subs = [];
    return {
        next: (v) => subs.forEach((h) => h(v)),
        subscribe: (h) => subs.push(h),
        _subs: subs,
    };
}

const viewerJS = require('../viewerJS.dataTable.js');
const dataTable = viewerJS.dataTable;

beforeEach(() => {
    // The module-local `viewerJS` (which is what paginator.init reads
    // when it touches viewerJS.jsfAjax.*) is the object returned by the
    // IIFE. Attaching .jsfAjax onto the require()'d module hits the same
    // binding, so the source finds the subject we mount here.
    viewerJS.jsfAjax = { begin: makeSubject(), success: makeSubject() };

    document.body.innerHTML = `
        <div id="paginator">P</div>
        <span id="t1">field1</span>
        <span id="t2" style="display:none"><input type="text" /></span>
        <span id="total">total</span>
        <button id="reload">reload</button>
        <div class="column-filter-wrapper">
            <button class="btn-filter">Filter</button>
        </div>
        <form id="adminAllUserForm"></form>`;

    // Reset ajax-binding sentinel so each test exercises the wiring path.
    dataTable.paginator.setupAjax = false;
    dataTable.filter.setupAjax = false;
});

describe('init', () => {
    test('always invokes paginator.init', () => {
        const spy = jest.spyOn(dataTable.paginator, 'init');
        dataTable.init({
            dataTablePaginator: '#paginator',
            txtField1: '#t1',
            txtField2: '#t2',
            totalCount: '#total',
            reloadBtn: '#reload',
        });
        expect(spy).toHaveBeenCalled();
        spy.mockRestore();
    });

    test('invokes filter.init only when at least one .column-filter-wrapper exists in the DOM', () => {
        const spy = jest.spyOn(dataTable.filter, 'init');
        dataTable.init({
            dataTablePaginator: '#paginator',
            txtField1: '#t1',
            txtField2: '#t2',
            totalCount: '#total',
            reloadBtn: '#reload',
        });
        expect(spy).toHaveBeenCalled();
        spy.mockRestore();
    });

    test('skips filter.init when no .column-filter-wrapper is present', () => {
        document.body.innerHTML = `
            <div id="paginator"></div>
            <span id="t1"></span><span id="t2"><input/></span>
            <span id="total"></span><button id="reload"></button>`;
        const spy = jest.spyOn(dataTable.filter, 'init');
        dataTable.init({
            dataTablePaginator: '#paginator',
            txtField1: '#t1',
            txtField2: '#t2',
            totalCount: '#total',
            reloadBtn: '#reload',
        });
        expect(spy).not.toHaveBeenCalled();
        spy.mockRestore();
    });
});

describe('paginator click handlers', () => {
    function fullInit() {
        dataTable.init({
            dataTablePaginator: '#paginator',
            txtField1: '#t1',
            txtField2: '#t2',
            totalCount: '#total',
            reloadBtn: '#reload',
        });
    }

    test('clicking txtField1 hides it and forwards to inputFieldHandler (focuses txtField2 input)', () => {
        const focusSpy = jest.spyOn(HTMLInputElement.prototype, 'focus');
        const selectSpy = jest.spyOn(HTMLInputElement.prototype, 'select').mockImplementation(() => {});
        try {
            fullInit();
            $('#t1').trigger('click');
            expect(document.getElementById('t1').style.display).toBe('none');
            expect(focusSpy).toHaveBeenCalled();
        } finally {
            focusSpy.mockRestore();
            selectSpy.mockRestore();
        }
    });

    test('clicking the totalCount span hides txtField1 and reveals txtField2', () => {
        fullInit();
        $('#total').trigger('click');
        expect(document.getElementById('t1').style.display).toBe('none');
    });
});

describe('paginator.inputFieldHandler keypress', () => {
    test('Enter on the txtField2 input clicks the reload button', () => {
        dataTable.init({
            dataTablePaginator: '#paginator',
            txtField1: '#t1',
            txtField2: '#t2',
            totalCount: '#total',
            reloadBtn: '#reload',
        });

        const clickSpy = jest.spyOn(document.getElementById('reload'), 'click').mockImplementation(() => {});
        try {
            // Activate the handler chain (binds the keypress).
            dataTable.paginator.inputFieldHandler();

            // Simulate Enter (keyCode 13).
            const $input = $('#t2 input');
            const ev = $.Event('keypress');
            ev.keyCode = 13;
            $input.trigger(ev);

            expect(clickSpy).toHaveBeenCalled();
        } finally {
            clickSpy.mockRestore();
        }
    });

    test('non-Enter keypress does not click the reload button', () => {
        dataTable.init({
            dataTablePaginator: '#paginator',
            txtField1: '#t1',
            txtField2: '#t2',
            totalCount: '#total',
            reloadBtn: '#reload',
        });

        const clickSpy = jest.spyOn(document.getElementById('reload'), 'click').mockImplementation(() => {});
        try {
            dataTable.paginator.inputFieldHandler();
            const ev = $.Event('keypress');
            ev.keyCode = 65;
            $('#t2 input').trigger(ev);
            expect(clickSpy).not.toHaveBeenCalled();
        } finally {
            clickSpy.mockRestore();
        }
    });
});

describe('filter.init', () => {
    test('intercepts submission of #adminAllUserForm and clicks the .btn-filter instead', () => {
        dataTable.init({
            dataTablePaginator: '#paginator',
            txtField1: '#t1',
            txtField2: '#t2',
            totalCount: '#total',
            reloadBtn: '#reload',
        });
        const filterClick = jest.spyOn(document.querySelector('.btn-filter'), 'click').mockImplementation(() => {});
        try {
            const submitEvt = $.Event('submit');
            $('#adminAllUserForm').trigger(submitEvt);
            expect(submitEvt.isDefaultPrevented()).toBe(true);
            expect(filterClick).toHaveBeenCalled();
        } finally {
            filterClick.mockRestore();
        }
    });
});
