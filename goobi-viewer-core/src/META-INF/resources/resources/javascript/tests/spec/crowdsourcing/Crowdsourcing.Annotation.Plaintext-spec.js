describe('Crowdsourcing.Annotation.Plaintext', () => {
    
    const  simpleAnnotation = {
        "@context": "http://www.w3.org/ns/anno.jsonld",
        "id": "http://example.org/anno5",
        "type": "Annotation",
        "body": {
          "type" : "TextualBody",
          "value" : "<p>j'adore !</p>",
          "format" : "text/html",
          "language" : "fr"
        },
        "target": "http://example.org/photo1"
      }
    
    const  fragmentAnnotation = {
            "@context": "http://www.w3.org/ns/anno.jsonld",
            "id": "http://example.org/anno5",
            "type": "Annotation",
            "body": {
              "type" : "TextualBody",
              "value" : "j'adore !",
              "format" : "text/plain",
              "language" : "fr"
            },
            "target": {
                "source": "http://example.org/image1",
                "selector": {
                    "type": "FragmentSelector",
                    "conformsTo": "http://www.w3.org/TR/media-frags/",
                    "value": "xywh=60,70,100,200"
                }
            }
          }
    
    beforeEach(() => {
    });

    it("is defined ", function() {
        expect(Crowdsourcing.Annotation.Plaintext).toBeDefined();
    })
    
    describe("Plaintext annotation constructor ", function() {
        it("creates a functional annotation object ", function() {            
            let anno1 = new Crowdsourcing.Annotation.Plaintext(simpleAnnotation);
            expect(anno1).toBeDefined();
            expect(anno1.getText()).toBe(simpleAnnotation.body.value);
        })
        it("creates separate deep copies of original ", function() {            
            let anno1 = new Crowdsourcing.Annotation.Plaintext(simpleAnnotation);
            let anno2 = new Crowdsourcing.Annotation.Plaintext(simpleAnnotation);
            anno1.setText("Text 1");
            anno2.setText("Text 2");
            expect(simpleAnnotation.body.value).toBe("<p>j'adore !</p>");
            expect(anno1.getText()).toBe("Text 1");
            expect(anno2.getText()).toBe("Text 2");
        })
        it("can be copied from other annotation ", function() {            
            let anno = new Crowdsourcing.Annotation.Plaintext(simpleAnnotation);
            let annoCopy = new Crowdsourcing.Annotation.Plaintext(anno);
            expect(annoCopy.target).toEqual(anno.target);
            expect(annoCopy.body).toEqual(anno.body);
            annoCopy.setText("TEST");
            expect(anno.getText()).toBe("<p>j'adore !</p>");
            expect(annoCopy.getText()).toBe("TEST");
        })
    })
    
    describe("Fragment target ", function() {
        it("is read from original annotation ", function() {
            let anno = new Crowdsourcing.Annotation.Plaintext(fragmentAnnotation);
            expect(anno.target.selector.value).toBe("xywh=60,70,100,200");
        })
        it("converts to rectangle object ", function() {
            let anno = new Crowdsourcing.Annotation.Plaintext(fragmentAnnotation);
            expect(anno.getRegion().x).toBe(60);
            expect(anno.getRegion().y).toBe(70);
            expect(anno.getRegion().width).toBe(100);
            expect(anno.getRegion().height).toBe(200);
        })
        it("can be modified ", function() {
            let anno = new Crowdsourcing.Annotation.Plaintext(fragmentAnnotation);
            let rect = {
                    x: 1000,
                    y: 2000,
                    width: 50,
                    height: 20
            }
            anno.setRegion(rect);
            expect(anno.getRegion().x).toBe(1000);
            expect(anno.getRegion().y).toBe(2000);
            expect(anno.getRegion().width).toBe(50);
            expect(anno.getRegion().height).toBe(20);
        })
        it("can be created ", function() {
            let anno = new Crowdsourcing.Annotation.Plaintext(simpleAnnotation);
            expect(anno.getRegion()).not.toBeDefined(); 
            let rect = {
                    x: 1000,
                    y: 2000,
                    width: 50,
                    height: 20
            }
            anno.setRegion(rect);
            expect(anno.getRegion().x).toBe(1000);
            expect(anno.getRegion().y).toBe(2000);
            expect(anno.getRegion().width).toBe(50);
            expect(anno.getRegion().height).toBe(20);
        })
    })
});