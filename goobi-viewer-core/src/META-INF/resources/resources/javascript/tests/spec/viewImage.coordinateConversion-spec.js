/**
 * This Spec tests the viewer control Methods in viewImageJS.
 * 
 * @version 1.0
 * @author Marc Lettau-Poelchen
 * @copyright intranda GmbH 2017
 */
describe( 'ImageView coordinate conversion Tests', function() {

		it(" correctly rotates (0,1) around center", function() {
			
			let p = {x: 0, y:1};
			let pr = ImageView.CoordinateConversion.rotate(p, -45, false);
			expect(pr.x).toBeCloseTo(-1/Math.sqrt(2), 0.01);
			expect(pr.y).toBeCloseTo(1/Math.sqrt(2), 0.01);
			
			pr = ImageView.CoordinateConversion.rotate(p, 45, true);
			expect(pr.x).toBeCloseTo(-1/Math.sqrt(2), 0.01);
			expect(pr.y).toBeCloseTo(1/Math.sqrt(2), 0.01);
			
			pr = ImageView.CoordinateConversion.rotate(p, -45, true);
			expect(pr.x).toBeCloseTo(1/Math.sqrt(2), 0.01);
			expect(pr.y).toBeCloseTo(1/Math.sqrt(2), 0.01);
			
			pr = ImageView.CoordinateConversion.rotate(p, 45, false);
			expect(pr.x).toBeCloseTo(1/Math.sqrt(2), 0.01);
			expect(pr.y).toBeCloseTo(1/Math.sqrt(2), 0.01);
			
		})
		
		it(" correctly rotates (1/sqrt(2), -1/sqrt(2)) around center", function() {
			
			let p = {x: 1/Math.sqrt(2), y: -1/Math.sqrt(2)};
			let pr = ImageView.CoordinateConversion.rotate(p, -45, false);
			expect(pr.x).toBeCloseTo(1, 0.01);
			expect(pr.y).toBeCloseTo(0, 0.01);
			
			pr = ImageView.CoordinateConversion.rotate(p, 45, true);
			expect(pr.x).toBeCloseTo(1, 0.01);
			expect(pr.y).toBeCloseTo(0, 0.01);
//			
			pr = ImageView.CoordinateConversion.rotate(p, -45, true);
			expect(pr.x).toBeCloseTo(0, 0.01);
			expect(pr.y).toBeCloseTo(-1, 0.01);
			
			pr = ImageView.CoordinateConversion.rotate(p, 45, false);
			expect(pr.x).toBeCloseTo(0, 0.01);
			expect(pr.y).toBeCloseTo(-1, 0.01);
			
		})
} );
