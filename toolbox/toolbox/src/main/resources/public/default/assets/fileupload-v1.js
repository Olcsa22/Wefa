function buildFileUpload(connector) {

	function textTrunc(str, length, ending) {
		if (length == null) {
			length = 20;
		}

		if (ending == null) {
			ending = '...';
		}

		if (str.length > length) {
			return str.substring(0, length - ending.length) + ending;
		} else {
			return str;
		}
	}
	
	// var hintStr = connector.getState().hintStr;
	var uploadId = connector.getState().uploadId;
	
	var contextPath = connector.getState().contextPath;
	
	var browseId = connector.getState().browseId;
	var areaId = connector.getState().areaId;
	var progDispId = connector.getState().progDispId;
	
	var isMultiFileAllowed = connector.getState().isMultiFileAllowed;
	// var maxFilesFileSize = connector.getState().maxFilesFileSize;
	
	var imgTargetMime = connector.getState().imgTargetMime;
	var maxPxWidth = connector.getState().maxPxWidth;
	var maxPxHeight = connector.getState().maxPxHeight;
	var resizeMethod = connector.getState().resizeMethod;
	
	var allowedMime = connector.getState().allowedMime;
		
	var options = { 
		url: contextPath + "/api/files/vaadin/" + uploadId,
		clickable: document.getElementById(browseId),
		createImageThumbnails: false,
		previewTemplate : '<div style="display:none"></div>', // turns off preview
		uploadMultiple: true,
		autoDiscover: false,
		timeout: 600000 // 10 min
		// maxFilesFileSize: 1
	}
		
	if (allowedMime) {
		options.acceptedFiles = allowedMime;
	}

	if (maxPxWidth && maxPxHeight) {

		options.resizeWidth = maxPxWidth;
		options.resizeHeight = maxPxHeight;

		if (imgTargetMime && maxPxWidth && maxPxHeight) {
			options.resizeMimeType = imgTargetMime;
		}
		
		options.resizeMethod = resizeMethod;
	}
		
	if (typeof window.activeDropzone !== 'undefined') {
		
		// korabbi futo peldany leallitasa (ez olyankor van, ha ket olyan view/dialog kozott valt, ahol mindkettoben van fileupload)
		
		if (window.activeDropzoneProgressInfoToServerUrl) {
			
			console.log("abandon type 1s1: " + window.activeDropzoneProgressInfoToServerUrl);
		
			jQuery.get(window.activeDropzoneProgressInfoToServerUrl + '?finishedForNow=true' );
			window.activeDropzoneProgressInfoToServerUrl = null;
		}
		
		if (window.activeDropzone) {
		
			console.log("abandon type 1s2");
		
			window.activeDropzone.off();
			window.activeDropzone.destroy();
			window.activeDropzone = null;
		}
		
	}
		
	var myDropzone = new Dropzone(document.getElementById(areaId), options);
	var progressInfoToServerUrl = contextPath + "/api/files/vaadin/progress/" + uploadId;
			
	window.activeDropzone = myDropzone;
	window.activeDropzoneProgressInfoToServerUrl = progressInfoToServerUrl;
		
	// console.log("buildFileUpload");
	
	// TODO: see totaluploadprogress, queuecomplete events instead (the currently used events are being fired per uploaded file) (this might be ok for now)
	
	var dzA = 0;
	
	function showDzOverlay(){
		dzA++;
		document.getElementById(areaId).classList.add("upload-dropzone-overlay");
	}
	
	function hideDzOverlay(){
		document.getElementById(areaId).classList.remove("upload-dropzone-overlay");
	}
	
	function hideDzOverlayTimeout(){
		var dzB = dzA; dzB++; dzA=dzB;
		setTimeout(function(){ if (dzB == dzA) document.getElementById(areaId).classList.remove("upload-dropzone-overlay"); }, 250);
	}
		
	myDropzone.on("dragover", function() { 
		showDzOverlay();
	}); 
	myDropzone.on("dragleave", function() { 	
		hideDzOverlayTimeout();
	}); 
	myDropzone.on("dragend", function() { 		
		hideDzOverlay();
	}); 
	myDropzone.on("drop", function() { 
		hideDzOverlay();
	}); 
	myDropzone.on("addedfile", function() { 
		hideDzOverlay();
	}); 
	
	myDropzone.on("complete", function(file) {
		console.log("upload completed: " + file);
		setTimeout(function(){ document.getElementById(progDispId).innerHTML = "" }, 500); //így mindenképp látszik egy kis időre
		
	}); 
		
	var lastProgressInfoSendToServerOn = 0;

	myDropzone.on("uploadprogress", function(file, progress, bytesSent) {
		
		var t = /*textTrunc(file.name, 20) + " " +*/  progress.toFixed(0) + " %"; //TODO: CSS text-overflow
		
		var c = document.getElementById(progDispId);
		
		if (c) {
			c.innerHTML =  '<span class="fa fa-circle-o-notch fa-spin"></span>&nbsp;' + t;
			
			var currentMillis = new Date().getTime();
			
			if ((currentMillis - lastProgressInfoSendToServerOn) > 15000) {
				lastProgressInfoSendToServerOn = currentMillis;
				jQuery.get(progressInfoToServerUrl);
			}
		} else {
		
			// korabbi "elhagyott" peldany leallitasa (ez olyankor van, ha olyan view/dialog-ra valt, ahol az ujabbon nincs fileupload egyaltalan)
					
			console.log("abandon type 2: " + progressInfoToServerUrl);
					
			jQuery.get(progressInfoToServerUrl + '?finishedForNow=true' );
			myDropzone.off();
			myDropzone.destroy();
		}
		
	});
	
	myDropzone.on("completemultiple", function(file) {
		jQuery.get(progressInfoToServerUrl + '?finishedForNow=true' );

	}); 

}

hu_lanoga_toolbox_vaadin_component_file_FileUploadComponent = function() {
	this.onStateChange = function() {
		buildFileUpload(this);
	}
	
};

