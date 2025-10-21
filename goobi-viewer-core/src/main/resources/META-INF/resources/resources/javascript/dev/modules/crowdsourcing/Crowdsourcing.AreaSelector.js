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
 * @description Base-Module which initialize the global admin object. * 
 * @version 3.4.0
 * @module Crowdsourcing.js
 * @requires jQuery
 */
var Crowdsourcing = ( function(crowdsourcing) {
    'use strict';

    crowdsourcing.AreaSelector = function(item, multiple, colors) {
        
        this.drawer = null;
        this.transformer = null;
        this.rects = [];
        this.finishedDrawing = new rxjs.Subject();
        this.finishedTransforming = new rxjs.Subject();
        this.lastRectangleId = -1;

        this.drawActive = true;
        this.transformActive = true;
        
        this.crowdsourcingItem = item;
        this.multiRect = multiple;
        this.colors = colors ? new ImageView.ColorIterator(colors) : new ImageView.ColorIterator(ImageView.ColorIterator.randomColor);
    }
    
    crowdsourcing.AreaSelector.prototype.init = function() {
        this.crowdsourcingItem.onImageOpen( (imageView) => {
        	console.log("init drawer", imageView, this);
            if(!this.drawer) {
                this.createDrawer(imageView);
                
                // this.crowdsourcingItem.onImageRotated( (degree) => {
                //     this.rects.forEach(function(overlay) {
                //         overlay.rect = overlay.rect.rotate(degree);
                //     })
                // });
            }
        });
        
    }

    crowdsourcing.AreaSelector.prototype.createDrawer = function (imageView) {
        window.imageView = imageView;
        this.drawer = new ImageView.Draw(imageView, this.getStyle("red"), e => e.shiftKey && this.allowDrawing());
        this.drawer.finishedDrawing().subscribe( rect => {
            if(this.rect && !this.multiRect) {
                this.removeOverlay(this.rect)
            }

            const overlayConfig = {
        		style: this.drawer.config.style,
                startCondition: this.allowTransforming()
        	}
            const overlay = new ImageView.Overlay(rect.bounds.viewport, overlayConfig, this.getNextId());
            this.drawer.config.style = this.getStyle();
    		overlay.draw(imageView)
            .then(o => {
                console.log("created overlay ", overlay);
                overlay.transformer = this.createTransformer(imageView, overlay);
                this.rect = overlay;
                this.rects.push(this.rect);
                this.finishedDrawing.next(_getResultObject(overlay, imageView));
            });
    	});
    }
    
    crowdsourcing.AreaSelector.prototype.allowDrawing = function() {
        return this.drawActive;
    }

    crowdsourcing.AreaSelector.prototype.allowTransforming = function() {
        return this.transformActive;
    }
    
    crowdsourcing.AreaSelector.prototype.reset = function() {
        this.colors.index = 0;
        this.rects.forEach(function(rect) {
            rect.transformer?.close();
            rect.remove();
        }.bind(this));
        this.rects = [];
    }

    crowdsourcing.AreaSelector.prototype.createTransformer = function(imageView, overlay) {
        console.log("create transformer", overlay);
        const config = {
            startCondition: (e) => !e.shiftKey
        }
        const transformer = new ImageView.Transform(imageView, overlay, config); 
        transformer.finishedTransforming().pipe(rxjs.operators.map((overlay) => _getResultObject(overlay, imageView))).subscribe(this.finishedTransforming);
        return transformer;

    }

    crowdsourcing.AreaSelector.prototype.getStyle = function(color) {
        let style = {
        		borderWidth: 2,
        		borderColor: color ? color : this.colors.next()
        }
        return style;
    }
    
    crowdsourcing.AreaSelector.prototype.getRect = function(id) {
        return this.rects.find( rect => rect.id == id);
    }

    crowdsourcing.AreaSelector.prototype.getNextId = function(){
        const idCount = ++this.lastRectangleId;
        return `image_area_${idCount}`;
    }
    
    crowdsourcing.AreaSelector.prototype.addOverlay = function(annotation, viewer) {
        if(annotation.getRegion()) {            
            let rect = ImageView.CoordinateConversion.scaleToOpenSeadragon(annotation.getRegion(), viewer.openseadragon, viewer.openseadragon.world.getItemAt(0).source)
            //rect = rect.rotate(-viewer.openseadragon.viewport.getRotation());
            const overlay = new ImageView.Overlay(rect, {style: this.getStyle()}, this.getNextId());
            annotation.setColor(overlay.config.style.borderColor);
            annotation.overlayId = overlay.id;
            this.lastRectangleId = overlay.id;
            overlay.draw(viewer);
            overlay.transformer = this.createTransformer(viewer, overlay);
            //console.log("%c add overlay " + annotation.getText(), "background: " + annotation.getColor());
            this.rects.push(overlay);
        }
    } 

    crowdsourcing.AreaSelector.prototype.removeOverlay = function(object) {
        let rect = this.rects.find(rect => rect.id == object.overlayId);
        if(rect) {
            rect.transformer?.close();
            rect.remove();
            let index = this.rects.indexOf(rect);
            if(index > -1) {                
                this.rects.splice(index, 1);
            }
            if(this.rect == rect) {
                this.rect = undefined;
            }
        }
    }
    
    crowdsourcing.AreaSelector.prototype.setDrawingStyle = function(style) {
        if(this.drawer) {
            this.drawer.style = style;
        }
    }

    
    crowdsourcing.AreaSelector.prototype.disableDrawer = function() {
        this.drawActive = false;
    }
    
    crowdsourcing.AreaSelector.prototype.enableDrawer = function() {
        this.drawActive = true;
    }
    
    crowdsourcing.AreaSelector.prototype.disableTransformer = function() {
        this.transformActive = false;
    }
    
    crowdsourcing.AreaSelector.prototype.enableTransformer = function() {
        this.transformActive = true;
    }
    
    function _getResultObject(overlay, image) {
        //let region = overlay.bounds.rotate(-image.getRotation());
        let region = overlay.bounds;
        let scaledRegion = ImageView.CoordinateConversion.scaleToImage(region, image.openseadragon, image.openseadragon.world.getItemAt(0).source);
        scaledRegion.x = Math.max(0, scaledRegion.x);
        scaledRegion.y = Math.max(0, scaledRegion.y);
        let result = {
                id: overlay.id,
                color: overlay.config.style.borderColor,
                region: scaledRegion
        }
        return result;
    }

return crowdsourcing;

} )( Crowdsourcing );


