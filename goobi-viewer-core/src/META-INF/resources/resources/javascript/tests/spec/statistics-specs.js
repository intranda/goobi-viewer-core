describe("Statistics tests", function() {
    
    describe("General statistics module", function() {        
        it("is defined", function() {
            expect(Statistics).toBeDefined();
        })
        describe("Publication types", function() { 
            it("is defined", function() {
                expect(Statistics.PublicationTypes).toEqual(jasmine.any(Function));
                expect(Statistics.PublicationTypes.prototype.plot).toEqual(jasmine.any(Function));
            })
            it("executes jqplot", function() {
                spyOn(jQuery, "jqplot");
                var plotter = new Statistics.PublicationTypes( {
                    labelList : "[Monograph::1337::Monographie,Manuscript::42::Handschrift,Periodical::21::Zeitschrift]",
                    labelDesc : "Werke",
                });
                plotter.plot("graph-div");
                expect(jQuery.jqplot).toHaveBeenCalled();
                expect(jQuery.jqplot).toHaveBeenCalledWith('graph-div', [[ ['Monograph', 1337, 'Monographie'],
                                                                           ['Manuscript', 42, 'Handschrift'],
                                                                           ['Periodical', 21, 'Zeitschrift'] ]], jasmine.any(Object));
            })
        })
        describe("Most edited works", function() { 
            it("is defined", function() {
                expect(Statistics.MostEditedWorks).toEqual(jasmine.any(Function));
                expect(Statistics.MostEditedWorks.prototype.plot).toEqual(jasmine.any(Function));
            })
            it("executes jqplot", function() {
                spyOn(jQuery, "jqplot");
                var plotter = new Statistics.MostEditedWorks( {
                    labelList : "[data]",
                    labelDesc : "Label",
                });
                plotter.plot("graph-div");
                expect(jQuery.jqplot).toHaveBeenCalled();                                   
            })
        })
        describe("Number of pages", function() { 
            it("is defined", function() {
                expect(Statistics.NumberOfPages).toEqual(jasmine.any(Function));
                expect(Statistics.NumberOfPages.prototype.plot).toEqual(jasmine.any(Function));
            })
            it("executes jqplot", function() {
                spyOn(jQuery, "jqplot");
                var plotter = new Statistics.NumberOfPages( {
                    labelList : "[data]",
                    labelDesc : "Label",
                });
                plotter.plot("graph-div");
                expect(jQuery.jqplot).toHaveBeenCalled();                                   
            })
        })
        describe("Most imported works trend", function() { 
            it("is defined", function() {
                expect(Statistics.MostImportedWorksTrend).toEqual(jasmine.any(Function));
                expect(Statistics.MostImportedWorksTrend.prototype.plot).toEqual(jasmine.any(Function));
            })
            it("executes jqplot", function() {
                spyOn(jQuery, "jqplot");
                var plotter = new Statistics.MostImportedWorksTrend( {
                    labelList : "[data]",
                    labelDesc : "Label",
                });
                plotter.plot("graph-div");
                expect(jQuery.jqplot).toHaveBeenCalled();                                   
            })
        })
        describe("Crowdsourcing progress", function() { 
            it("is defined", function() {
                expect(Statistics.CrowdSourcingProgress).toEqual(jasmine.any(Function));
                expect(Statistics.CrowdSourcingProgress.prototype.plot).toEqual(jasmine.any(Function));
            })
            it("does not execute jqplot", function() {
                spyOn(jQuery, "jqplot");
                var plotter = new Statistics.CrowdSourcingProgress( {
                    fulltextProgressString : "progress-fulltext",
                    contentProgressString : "progress-content",
                });
                plotter.plot("fulltext-div", "content-div");
                expect(jQuery.jqplot).not.toHaveBeenCalled();                                   
            })
        })
    })
})