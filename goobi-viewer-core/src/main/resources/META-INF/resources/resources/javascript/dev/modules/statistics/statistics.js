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
 * Methodensammlung für Statistik-Graphen mit jqplot Implementierte Graphen: *
 * PublicationTypes * MostEditedWorks * NumberOfPages * MostImportedWorksTrend *
 * CrowdSourcingProgress Jeder Graph muss mit Statistics.<Graphname>.init(config)
 * initiallisiert werden. Das config-Objekt enthält dabei die zugrundeliegenden Daten.
 * Anschließend kann der Graph mit Statistics.<Graphname>.plot(divId) gemalt werden. Um
 * Mouseover-Anzeige zu verwenden, muss sie mit Statistics.initMouseover(document, window)
 * initialisiert werden Um für PublicationTypes aud Mausklick eine Suche nach passenden
 * Werken zu starten muss dies mit initSearchQuery(plotId, inputId, buttonId)
 * initialisiert werden
 * 
 * @version 3.2.0 
 */
 
var Statistics = ( function() { 
    'use strict';
    
    var _debug = false;
    
    jQuery.jqplot.config.enablePlugins = true;
    
    var Statistics = {};
    
    Statistics.chartLabel = null;
    
    Statistics.initMouseover = function( document, window ) {
        $( document ).mousemove( function( ev ) {
            
            if ( Statistics.chartLabel != null ) { 
                
                var top = window.pageYOffset || document.documentElement.scrollTop;
                var left = window.pageXOffset || document.documentElement.scrollLeft;
                
                var mouseX = ev.pageX + 10 - left; // these are going to be how jquery
                // knows where to put the div that
                // will be our tooltip
                var mouseY = ev.pageY - 20 - top;
                
                document.getElementById('chartpseudotooltip' ).innerHTML = Statistics.chartLabel;
                var cssObj = {
                    'position': 'fixed',
                    'left': mouseX + 'px', // usually needs more offset here
                    'top': mouseY + 'px',
                    'z-index': 10000,
                    'max-width': '300px'
                };
                $( '#chartpseudotooltip' ).css( cssObj );
                $( '#chartpseudotooltip' ).show();
                
            }
            else {
                $( '#chartpseudotooltip' ).hide();
            }
        } );
    }

    Statistics.PublicationTypes = function( config ) {
        this.config = config;
    }

    Statistics.PublicationTypes.prototype.plot = function( divId ) {
        
        var typeList = this.config.labelList.substring( 1, this.config.labelList.length - 1 );
        typeList = typeList.split( ',' );
        
        var typeNumberList = new Array();
        
        for ( var i = 0; i < typeList.length; i++ ) {
            var itemList = typeList[ i ].split( "::" );
            typeList[ i ] = itemList[ 0 ] + " (" + itemList[ 1 ] + ")";
            itemList[ 1 ] = parseInt( itemList[ 1 ] );
            typeNumberList.push( itemList );
        }
        // data = itemList;
        
        var plot1 = jQuery.jqplot( divId, [ typeNumberList ], {
            title: ' ',
            seriesDefaults: {
                shadow: false,
                renderer: jQuery.jqplot.PieRenderer,
                rendererOptions: {
                    startAngle: -90,
                    padding: 2,
                    sliceMargin: 2,
                    dataLabels: typeList,
                    dataLabelThreshold: 5,
                    dataLabelPositionFactor: 0.6,
                    showDataLabels: false,
                }
            },
            highlighter: {
                show: false
            },
            cursor: {
                show: false
            }
        } );
        
        var $div = $( "#" + divId );
        var labelDesc = this.config.labelDesc;
        
        $div.bind( 'jqplotDataHighlight', function( ev, seriesIndex, pointIndex, data ) {
            Statistics.chartLabel = typeNumberList[ pointIndex ][ 0 ] + " (" + typeNumberList[ pointIndex ][ 1 ] + " " + labelDesc + ")";
        } );
        
        $div.bind( 'jqplotDataUnhighlight', function( ev ) {
            Statistics.chartLabel = null;
        } );
        
        $div.bind( 'jqplotDataMouseOver', function( ev, seriesIndex, pointIndex, data ) {
            $( this ).css( {
                'cursor': 'pointer'
            } );
        } );
    }

    Statistics.PublicationTypes.prototype.initSearchQuery = function( plotId, inputId, buttonId ) {
        $( '#' + plotId ).bind( 'jqplotDataClick', function( ev, seriesIndex, pointIndex, data ) {
            var publicationName = data[ 2 ];
            // var searchString = '(DOCSTRCT:"' + publicationName + '") AND (DOCTYPE:"DOCSTRCT")';
            var facetString = 'DOCSTRCT:"' + publicationName + '";;DOCTYPE:DOCSTRCT;;';
            
            if ( _debug ) {
                console.log( "search for " + encodeURI( facetString ) );
            }
            
            $( '#' + inputId ).val( facetString );
            $( '#' + buttonId ).click();
        } );
    }

    Statistics.MostEditedWorks = function( config ) {
        this.config = config;
    }

    Statistics.MostEditedWorks.prototype.plot = function( divId ) {
        var labelList = this.config.labelList.substring( 1, this.config.labelList.length - 1 );
        labelList = labelList.split( ',' );
        
        var graphList = new Array();
        var fullLabelList = new Array();
        var maxLabelLength = 40;
        
        for ( var i = 0; i < labelList.length; i++ ) {
            var itemList = labelList[ i ].split( "::" );
            fullLabelList[ i ] = itemList[ 0 ];
            labelList[ i ] = Statistics.shortString( itemList[ 0 ], maxLabelLength );
            itemList[ 1 ] = parseInt( itemList[ 1 ] );
            graphList.push( itemList[ 1 ] );
        }
        
        var plot1 = jQuery.jqplot( divId, [ graphList ], {
            seriesDefaults: {
                renderer: jQuery.jqplot.BarRenderer,
                rendererOptions: {
                    barDirection: 'horizontal',
                    varyBarColor: true
                }
            },
            series: [ {
                pointLabels: {
                    show: true,
                    stackedValue: true,
                    labels: labelList,
                    location: 'ne',
                    edgeTolerance: -50
                }
            } ],
            axes: {
                yaxis: {
                    ticks: labelList,
                    renderer: jQuery.jqplot.CategoryAxisRenderer,
                    rendererOptions: {
                        tickRenderer: jQuery.jqplot.AxisTickRenderer,
                        tickOptions: {
                            show: false,
                            showLabel: false
                        }
                    }
                },
                xaxis: {
                    padMax: 1.4,
                    rendererOptions: {
                        tickRenderer: jQuery.jqplot.AxisTickRenderer,
                        tickOptions: {
                            show: true,
                            showLabel: true
                        }
                    }
                }
            },
            cursor: {
                show: false
            },
            highlighter: {
                show: false
            }
        } );
        
        var $div = $( '#chart-most-edited' );
        
        $div.bind( 'jqplotDataHighlight', function( ev, seriesIndex, pointIndex, data ) {
            // chartLabel = graphList[pointIndex] + " " + labelDesc;
            Statistics.chartLabel = fullLabelList[ pointIndex ];
        } );
        
        $div.bind( 'jqplotDataUnhighlight', function( ev ) {
            Statistics.chartLabel = null;
        } );
    }

    Statistics.NumberOfPages = function( config ) {
        this.config = config;
    }

    Statistics.NumberOfPages.prototype.plot = function( divId ) {
        var labelList = [ this.config.titlePages + "::" + this.config.numPages, this.config.titleFullTexts + "::" + this.config.numFullTexts ];
        
        var graphList = new Array();
        
        for ( var i = 0; i < labelList.length; i++ ) {
            var itemList = labelList[ i ].split( "::" );
            labelList[ i ] = itemList[ 0 ];
            itemList[ 1 ] = parseInt( itemList[ 1 ] );
            graphList.push( itemList[ 1 ] );
        }
        
        var plot1 = jQuery.jqplot( divId, [ graphList ], {
            seriesDefaults: {
                renderer: jQuery.jqplot.BarRenderer,
                rendererOptions: {
                    barDirection: 'vertical',
                    varyBarColor: true
                }
            },
            series: [ {
                pointLabels: {
                    show: false,
                    labels: labelList,
                    location: 'n'
                }
            } ],
            axes: {
                xaxis: {
                    ticks: labelList,
                    renderer: jQuery.jqplot.CategoryAxisRenderer,
                    rendererOptions: {
                        tickRenderer: jQuery.jqplot.AxisTickRenderer,
                        tickOptions: {
                            show: true,
                            showLabel: true
                        }
                    }
                },
                yaxis: {
                    min: 0,
                    rendererOptions: {
                        tickOptions: {
                            show: true,
                            showLabel: true
                        }
                    }
                }
            },
            cursor: {
                show: false
            },
            highlighter: {
                show: false
            }
        } );
        
        var $div = $( "#" + divId );
        
        $div.bind( 'jqplotDataHighlight', function( ev, seriesIndex, pointIndex, data ) {
            // chartLabel = graphList[pointIndex] + " " + labelList[pointIndex].split('
            // ')[1];
            Statistics.chartLabel = graphList[ pointIndex ] + " " + labelList[ pointIndex ];
        } );
        
        $div.bind( 'jqplotDataUnhighlight', function( ev ) {
            Statistics.chartLabel = null;
        } );
    }

    Statistics.MostImportedWorksTrend = function( config ) {
        this.config = config;
    }

    Statistics.MostImportedWorksTrend.prototype.plot = function( divId ) {
        var labelList = this.config.labelList.substring( 1, this.config.labelList.length - 1 );
        labelList = labelList.split( ', ' );
        var graphList = new Array();
        var lastDate, date;
        
        for ( var i = 0; i < labelList.length; i++ ) {
            var itemList = labelList[ i ].split( "::" );
            labelList[ i ] = itemList[ 0 ];
            var date = new jQuery.jsDate( parseInt( itemList[ 0 ] ) );
            itemList[ 0 ] = date;
            itemList[ 1 ] = parseInt( itemList[ 1 ] );
            graphList.push( itemList );
            
            if ( lastDate == null ) {
                lastDate = date;
            }
        }
        
        var plot1 = jQuery.jqplot( divId, [ graphList ], {
            seriesDefaults: {
                fill: false,
                lineWidth: 5,
                fillColor: '#fffdf6'
            
            },
            series: [ {
                showMarker: false,
                pointLabels: {
                    show: false
                }
            } ],
            axes: {
                xaxis: {
                    // max: lastDate,
                    // min: date,
                    renderer: jQuery.jqplot.DateAxisRenderer,
                    tickOptions: {
                        formatString: "%b %Y",
                    	markSize: 10,
                    }
                },
                yaxis: {
                    padMax: 1.01,
                    padMin: 1.01,
                    tickOptions: {
                    	markSize: 10,
                    },
                }
            },
            cursor: {
                show: false
            },
            highlighter: {
                show: true,
                showTooltip: true,
                showMarker: false,
                formatString: '%s, %s ' + this.config.labelDesc
            },
            seriesColors: [ "#DAA520" ]
        } );
    }

    Statistics.CrowdSourcingProgress = function( config ) {
        this.config = config;
    }

    Statistics.CrowdSourcingProgress.prototype.plot = function( fulltextId, contentId ) {
        $( '#' + fulltextId ).css( 'width', Statistics.getBarSize( this.config.fulltextProgressString, 160 ) );
        $( '#' + contentId ).css( 'width', Statistics.getBarSize( this.config.contentProgressString, 160 ) );
    }

    Statistics.getBarSize = function( inputString, barWidth ) {
        var parts = inputString.split( '/' );
        var completedPages = parseFloat( parts[ 0 ] );
        var totalPages = parseFloat( parts[ 1 ] );
        var progress = completedPages / totalPages * barWidth;
        return progress;
    }

    Statistics.shortString = function( string, maxLength ) {
        if ( string.length <= maxLength ) {
            return string;
        }
        else {
            var wordArray = string.split( ' ' );
            var newString = "";
            var nextString = "";
            var counter = 0;
            while ( counter < 1 || nextString.length < maxLength ) {
                newString = nextString;
                nextString = nextString + " " + wordArray[ counter ];
                counter++;
            }
            return newString + " [...]";
        }
    }
    
    function _addDays(date, days) {
    	var result = new Date();
    	  result.setDate(date.getDate() + days);
    	  return result;
    }

    return Statistics;
    
} )( jQuery );
