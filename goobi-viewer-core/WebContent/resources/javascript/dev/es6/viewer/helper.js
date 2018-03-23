class Helper {
 truncateString(str, size) {
 let strSize = parseInt(str.length);

 if (strSize > size) {
 return str.substring(0, size) + "...";
 } else {
 return str;
 }
 }
}
