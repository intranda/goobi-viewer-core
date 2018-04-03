describe('Helper', () => {
    beforeEach(() => {
        helper = new Helper();
    });

    afterEach(() => {});
    
    describe('Truncate String', () => {
        it('should return the given string', () => {
            const result = helper.truncateString('Hello Kitty!', 50);
            
            expect(result).toBe('Hello Kitty!');
        });
        
        it('should return a truncated string', () => {
            const result = helper.truncateString('Hello Kitty!', 5);
            
            expect(result).toBe('Hello...');
        });
    });
});