
 var viewerJS = ( function( viewer ) {
    
    var _debug = false;
    
    if(!rxjs) {
        throw "Missing dependencies for WebSocket";
    }
    
    viewer.WebSocket = function(host, contextPath, socketPath) {
        this.onOpen = new rxjs.Subject();
        this.onMessage = new rxjs.Subject();
        this.onError = new rxjs.Subject();
        this.onClose = new rxjs.Subject();
        
        var protocol = "ws";
        
        if (window.location.protocol == "https:") {
            protocol = "wss";
        }
        
        var socketUrl = protocol + "://" + host + contextPath + socketPath;
        if(_debug) {
            console.log("connecting to viewer session socket at ", socketUrl);
        }
        
        this.socket = new WebSocket(socketUrl);
        this.socket.onopen = (event) => this.onOpen.next(event);
        this.socket.onmessage = (event) => this.onMessage.next(event);
        this.socket.onerror = (event) => this.onError.next(event);
        this.socket.onclose = (event) => this.onClose.next(event);
    };
    
    //pseudo-constant containing path to socket
    viewer.WebSocket.PATH_SESSION_SOCKET = "/session.socket";
    viewer.WebSocket.PATH_CAMPAIGN_SOCKET = "/crowdsourcing/campaign.socket";
    viewer.WebSocket.PATH_CONFIG_EDITOR_SOCKET = "/admin/config/edit.socket";
    viewer.WebSocket.PATH_DOWNLOAD_TASK = "/tasks/download/monitor.socket";

    //prototype methods
    viewer.WebSocket.prototype.sendMessage = function(message) {
        if(_debug)console.log("send ", message, " open: ", this.isOpen());
        this.socket.send(message);
    };
    viewer.WebSocket.prototype.close = function(reason, statusCode) {
        this.socket.close(statusCode, reason);
    };     
    viewer.WebSocket.prototype.isOpen = function() {
        return this.socket && this.socket.readyState === WebSocket.OPEN;
    };     
     
    return viewer;
    
 } )( viewerJS || {}, jQuery );
 
 