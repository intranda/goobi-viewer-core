
describe("OpenSeadragon related tests ", function() {
    describe("Coordinate calculations ", function() {
        it("rounds to the nearest 10^(-14)", function() {
            var a = 3.141592653589793;
            var b = 3.141592653589794;
            expect(b-a).toBeGreaterThan(0.0);
            var aa = parseFloat(a.toPrecision(15));
            var bb = parseFloat(b.toPrecision(15));
            expect(aa).toEqual(3.14159265358979);
            expect(bb).toEqual(3.14159265358979);
            expect(bb-aa).toEqual(0.0);
        })
    })
})

describe("Browser compability ", function() {
  describe("String startsWith", function() {
      it(" returns true for the same string", function() {
          var a = "abcdefg";
          var b = "abcdefg";
          expect(a.startsWith(b)).toBe(true);
      })
      it(" returns true for the beginning of the string", function() {
          var a = "abcdefg";
          var b = "a";
          var c = "ab";
          var d = "abcd";
          expect(a.startsWith(b)).toBe(true);
          expect(a.startsWith(c)).toBe(true);
          expect(a.startsWith(d)).toBe(true);
      })
      it(" returns false for a different string", function() {
          var a = "abcdefg";
          var b = "zyxwvu";
          expect(a.startsWith(b)).not.toBe(true);
      })
      it(" returns false for a substring not at the beginning", function() {
          var a = "abcdefg";
          var b = "bcde";
          expect(a.startsWith(b)).not.toBe(true);
      })
  })  
  describe("Number isNaN", function() {
      it(" returns false for any number other than NaN", function() {
          expect(Number.isNaN(0)).not.toBe(true);
          expect(Number.isNaN(7)).not.toBe(true);
          expect(Number.isNaN(-7)).not.toBe(true);
          expect(Number.isNaN(3.14796238947)).not.toBe(true);
          expect(Number.isNaN(Infinity)).not.toBe(true);
      })
      it(" returns tue for NaN", function() {
          expect(Number.isNaN(NaN)).toBe(true);
      })
      it(" returns false anything other than a number", function() {
          expect(Number.isNaN("0")).not.toBe(true);
          expect(Number.isNaN("NaN")).not.toBe(true);
          expect(Number.isNaN({a:0,b:234,c:"sdaf"})).not.toBe(true);
      })
  })
})