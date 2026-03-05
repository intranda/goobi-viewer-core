const _default = {
    container : "#world",
    loadStory : "#loadStoryEditor",
    closeStory : "#closeStoryEditor",
    sizeText : "#objectSize",
    explorer: "#voyager",
    story: "#voyagerStory",
    startLoad: "#startLoadButton",
    loadProgress: "#loadingProgress"
} 

const _debug = false;

export default class Voyager3dView {

    constructor(config) {
        this.config = jQuery.extend(true, {}, _default, config);
        this.container = document.querySelector(this.config.container);
       // console.log('init voyager3d', this);
        if (this.isVisible()) {
            this.loaded = this.initView().then(() => {
                if (_debug) console.log('object data loaded');
            });
        }
    }

    async initView() {
        this.loadStoryButton = document.querySelector(this.config.loadStory);
        this.closeStoryButton = document.querySelector(this.config.closeStory);
        this.startLoadButton = document.querySelector(this.config.startLoad);
        this.loadingProgressMonitor = document.querySelector(this.config.loadProgress);
        this.objectUrl = this.container.dataset.objectUrl;
        this.resourcePath = this.container.dataset.resourcePath; 
        if(this.objectUrl) {

            this.startLoadButton?.addEventListener("click", e => this.loadExplorer());
            this.loadStoryButton?.addEventListener("click", e => this.loadStory());
            this.closeStoryButton?.addEventListener("click", e => this.loadExplorer());

            if(_debug) console.log("load object from ", this.objectUrl);
            this.sceneData = await this.getSceneData();
            if(_debug) console.log("scene data loaded ", this.sceneData);
            this.showLoadSizeText();
        }
    }

    loadExplorer() {
        const explorer = `<voyager-explorer id='voyager' bgcolor='#015999 #000' bgstyle='LinearGradient' style='display:none;' document='${this.getSceneUrl()}' uiMode='menu' resourceRoot='${this.resourcePath}'></voyager-explorer>`;
        
        show(this.loadStoryButton);
        hide(this.closeStoryButton);
        this.unloadObject();
        this.unloadStory();
        hide(this.startLoadButton);
        show(this.loadingProgressMonitor);


        const tempElement = document.createElement('div');
        tempElement.innerHTML = explorer;

        if(_debug)console.log("appending ", ...tempElement.children, " to ", this.container);
        this.container.append(...tempElement.children);

        this.voyager = document.getElementById(this.config.explorer);
        voyager.addEventListener("model-load", e => {
            if(_debug)console.log("loaded ", e);
            show(voyager);
        })
    }



    loadStory() {
            
        const story = `<voyager-story id='voyagerStory' bgcolor='#015999 #000' bgstyle='LinearGradient' style='display:none;' document='${this.getSceneUrl()}'></voyager-story>`;

        hide(this.loadStoryButton);
        show(this.closeStoryButton);
        this.unloadObject();
        this.unloadStory();
        hide(this.startLoadButton);
        show(this.loadingProgressMonitor);

        const tempElement = document.createElement('div');
        tempElement.innerHTML = story;

        console.log("appending ", ...tempElement.children, " to ", this.container);
        this.container.append(...tempElement.children);

        const voyager = document.getElementById(this.config.story);
        voyager.addEventListener("model-load", e => {
            console.log("loaded ", e);
            show(voyager);
        })

    }

    showLoadSizeText() { 
        const size = this.getTotalSize(this.sceneData);
        if(_debug)("object size is ", size, "byte");
        const sizeText = document.querySelector(this.config.sizeText);
        if(size && sizeText) {
           sizeText.innerHTML = (size / 1000 / 1000).toFixed(2) + " MB";
        }
    }

    isVisible() {
        return this.container && $(this.container).is(":visible");
    }



    getTotalSize(scene) {
        return scene.models.flatMap(m => m.derivatives).flatMap(d => d.assets).map(a => a.byteSize).reduce((a, b) => a + b, 0);
    }

    getSceneUrl() {
        return this.objectUrl + "/scene.svx.json";
    }

    async getSceneData() {
        return fetch(this.getSceneUrl())
        .then(res => res.json());
    }


    unloadObject() {
        document.querySelector(this.config.explorer)?.remove();
    }

    unloadStory() {
        document.querySelector(this.config.story)?.remove();
    }
}

function hide(element) {
    if (element) {
        element.style.display = "none";
    }
}

function show(element) {
    if (element) {
        element.style.display = "block";
    }
}
