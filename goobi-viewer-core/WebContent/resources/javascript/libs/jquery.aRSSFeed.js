// jQuery plugin - Simple RSS Aggregator
;
( function( $ ) {
    var truncateString = function( str ) {
        var strSize = parseInt( str.length );
        
        if ( strSize > 50 ) {
            return str.substring( 0, 50 ) + '...';
        }
        else {
            return str;
        }
    };
    
    $.fn.aRSSFeed = function() {
        return this.each( function() {
            var $Cont = $( this );
            var iMaxNum = parseInt( $Cont.attr( 'rssnum' ) || 0 );
            var sFeedURL = $Cont.attr( 'rss_url' );
            var lastImportsMsg = $Cont.attr( 'lastImportsMsg' );
            var weekDayNames = $Cont.attr( 'weekDayNames' ).split( "," );
            var monthNames = $Cont.attr( 'monthNames' ).split( "," );
            
            if ( sFeedURL == undefined ) {
                return false;
            }
            
            $.getFeed( {
                url: sFeedURL,
                success: function( feed ) {
                    if ( feed != undefined && feed.items ) {
                        var sCode = "", iCount = 0;
                        
                        for ( var iItemId = 0; iItemId < feed.items.length; iItemId++ ) {
                            var item = feed.items[ iItemId ], sDate, a, oDate;
                            
                            if ( null != ( a = item.updated.match( /(\d+)-(\d+)-(\d+)T(\d+):(\d+):(\d+)Z/ ) ) ) {
                                oDate = new Date( a[ 1 ], a[ 2 ] - 1, a[ 3 ], a[ 4 ], a[ 5 ], a[ 6 ], 0 );
                            }
                            else {
                                oDate = new Date( item.updated );
                            }
                            var time = oDate.toLocaleTimeString();
                            
                            sDate = weekDayNames[ oDate.getDay() ];
                            sDate += ", ";
                            sDate += oDate.getDate();
                            sDate += ". ";
                            sDate += monthNames[ oDate.getMonth() ];
                            sDate += " ";
                            sDate += oDate.getFullYear();
                            sDate += " ";
                            sDate += time.substring(0, time.length - 3);
                            sCode += '<div class="rss-elem">';
                            sCode += '<dl>';
                            sCode += '<dt class="rss-elem-title">';
                            sCode += '<a href="' + item.link + '" title="' + item.title + '" data-ajax="false">';
                            sCode += truncateString( item.title );
                            sCode += '</a>';
                            sCode += '</dt>';
                            sCode += '<dd class="rss-elem-info">';
                            sCode += '<small>' + lastImportsMsg + '</small><br />';
                            sCode += '<small>' + sDate + '</small>';
                            sCode += '</dd>';
                            sCode += '</div>';
                            
                            iCount++;
                            
                            if ( iCount == iMaxNum ) {
                                break;
                            }
                        }
                    }
                    
                    $Cont.html( sCode );
                }
            } );
        } );
    };
} )( jQuery );
