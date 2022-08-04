console.log("index.js invoked");
	
var configFileTextArea;
var type;
var configFileEditor;
var readOnly;
var selected_row;
//var nightMode;
//var theme;
	
	
if (readOnly === undefined) {
	readOnly = true;
}
	
//initTextArea();
					 
var table = document.getElementById("file-form:file-table");
var rows = document.getElementsByTagName("tr");
	
for (let i = 0; i < rows.length; ++i) {
	var currentRow = table.rows[i];
	currentRow.setAttribute('id', 'row'+i);
	currentRow.onclick = function() {
	/*
		Array.from(this.parentElement.children).forEach(function(el){
			el.classList.remove('selected-row');
		});
	*/ 
	[...this.parentElement.children].forEach((el) => el.classList.remove("selected-row"));
	this.classList.add('selected-row');
	};
}

function initTextArea() {
	let fileTypeElement = document.getElementById("currentConfigFileType");
	if (fileTypeElement !== null){
		type = fileTypeElement.innerHTML.trim(); // "properties" or "xml"
	} else {
		type = "xml";
	}
//	nightMode = document.getElementById("nightMode").innerHTML.trim(); // "true" or "false"
	if (typeof type == "undefined") {
		type = "xml";
	}
//	
//	if (nightMode == "true") {
//		theme = "blackboard";
//	} else {
//		theme = "default";
//	}
//
	configFileTextArea = document.getElementById("editor-form:editor");

	if (configFileTextArea !== null) {
		if (configFileEditor) {
			configFileEditor.toTextArea();
		}
		configFileEditor = CodeMirror.fromTextArea(configFileTextArea, {
			lineNumbers: true,
			mode: type,
			readOnly: readOnly,
			autofocus: true,
			indextUnit: 4,
			extraKeys: {
				"F11": function(cm) {
					cm.setOption("fullScreen", !cm.getOption("fullScreen"));
				},
				"Esc": function(cm) {
					if (cm.getOption("fullScreen")) {
						cm.setOption("fullScreen", false);
					}
				}
			}
			
		}); 
//		configFileEditor.focus();

		setTimeout(function(){
			configFileEditor.refresh();	
		}, 100);
	}
}

function setEditable(editable, number, isButton) {
	selected_row = document.getElementById('row'+number);
	
	readOnly = !editable;
	if (readOnly && isButton) {
		alert("Die Rechte im Dateisystem müssen korrigiert werden um diese Datei zu bearbeiten.");
	}
}

function saveEditor() {
	configFileEditor.save();
}

