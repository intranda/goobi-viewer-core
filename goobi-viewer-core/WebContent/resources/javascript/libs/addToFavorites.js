/**
 * @author maik
 */
function CreateBookmarkLink(title, url) {
	
//title = "test.html";
//url = "http://www.123-test.com/";

if (window.sidebar) {
// Mozilla Firefox Bookmark
//alert("FIREFOX!");
window.sidebar.addPanel(title, url,"");
} else if( window.external ) {
// IE Favorite
//alert("YES IE");
window.external.AddFavorite( url, title);
}
else if(window.opera && window.print) {
// Opera Hotlist
return true; }
}