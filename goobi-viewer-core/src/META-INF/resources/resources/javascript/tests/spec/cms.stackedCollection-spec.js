describe('cmsJS.stackedCollections', () => {
    
    
    beforeEach(() => {
    });

    it("is defined ", function() {
        expect(cmsJS.stackedCollection).toBeDefined();
    })
    
    describe("readIIIFPresentationStringValue ", function() {
        it("reads the correct value from single string ", function() {
            expect(cmsJS.stackedCollection.readIIIFPresentationStringValue("Test", "fr")).toEqual("Test");
        })
        it("reads the correct value from single object ", function() {
            var element = {
                    '@value': 'Test2',
                    '@language': 'es'
            }
            expect(cmsJS.stackedCollection.readIIIFPresentationStringValue(element, "fr")).toEqual('Test2');
        })
        it("reads the correct value from object array", function() {
            var element = [{
                    '@value': 'Test_es',
                    '@language': 'es'
                },
                {
                    '@value': 'Test_fr',
                    '@language': 'fr'
                },
                {
                    '@value': 'Test_en',
                    '@language': 'en'
                },
                {
                    '@value': 'Test_de',
                    '@language': 'de'
                }];
            expect(cmsJS.stackedCollection.readIIIFPresentationStringValue(element, "es")).toEqual('Test_es');
            expect(cmsJS.stackedCollection.readIIIFPresentationStringValue(element, "de")).toEqual('Test_de');
            expect(cmsJS.stackedCollection.readIIIFPresentationStringValue(element, "en")).toEqual('Test_en');
            expect(cmsJS.stackedCollection.readIIIFPresentationStringValue(element, "fr")).toEqual('Test_fr');
            expect(cmsJS.stackedCollection.readIIIFPresentationStringValue(element, "ar")).toEqual('Test_en');
        })
        it("reads the correct value from string array", function() {
            var element = [
                'Test1',
                'Test2',
                'Test3',
                'Test4'
           ];
            expect(cmsJS.stackedCollection.readIIIFPresentationStringValue(element, "fr")).toEqual('Test1');
            expect(cmsJS.stackedCollection.readIIIFPresentationStringValue(element, '')).toEqual('Test1');
        })
    })
});