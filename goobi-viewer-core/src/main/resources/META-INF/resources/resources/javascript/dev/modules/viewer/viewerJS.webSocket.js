
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

    //prototype methods
    viewer.WebSocket.prototype.sendMessage = function(message) {
        this.socket.send(message);
    };
    viewer.WebSocket.prototype.close = function(reason, statusCode) {
        this.socket.close(statusCode, reason);
    };       
     
    return viewer;
    
 } )( viewerJS || {}, jQuery );
 
 