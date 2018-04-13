describe('cmsJS.stackedCollections', () => {
    
    
    beforeEach(() => {
    });

    it("is defined ", function() {
        expect(cmsJS.stackedCollection).toBeDefined();
    })
    
    describe("readIIIFPresentationStringValue ", function() {
        fit("reads the correct value from single string", function() {
            expect(cmsJS.stackedCollection.readIIIFPresentationStringValue("Test", "fr")).toEqual("Test");
        })
        fit("reads the correct value from single object", function() {
            var element = {
                    '@value': 'Test2',
                    '@language': 'es'
            }
            expect(cmsJS.stackedCollection.readIIIFPresentationStringValue(element, "fr")).toEqual('Test2');
        })
    })
});