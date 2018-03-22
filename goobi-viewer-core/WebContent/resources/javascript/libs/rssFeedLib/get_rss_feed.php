<?php

$sUrl = (isset($_GET['url']) && $_GET['url'] != '') ? $_GET['url'] : 'http://www.boonex.com/unity/extensions/latest/?rss=1';

header( 'Content-Type: text/xml' );
readfile($sUrl);

?>