$(function () {
    "use strict";

    var content = $('#content');
    var input = $('#input');
    var status = $('#status');
    var myName = false;
    var author = null;
    var logged = false;
    var socket = atmosphere;
    //var request = { url: document.location.toString() + '/update',
    var urlpath = window.location.pathname.substring(0, window.location.pathname.lastIndexOf("/")) +'/update';
    var request = { url: urlpath,
        contentType: "application/json",
        logLevel: 'debug',
        transport: 'sse',
        reconnectInterval: 5000,

        fallbackTransport: 'long-polling'};


    request.onOpen = function (response) {
    	console.log('atmosphere onOpen');
        content.html($('<p>', { text: 'Atmosphere connected using ' + response.transport }));
        input.removeAttr('disabled').focus();
    };

    request.onReconnect = function (request, response) {
    	console.log('atmosphere onReconnect');
        content.html($('<p>', { text: 'Connection lost, trying to reconnect. Trying to reconnect ' + request.reconnectInterval}));
        input.attr('disabled', 'disabled');
    };

    request.onReopen = function (response) {
    	console.log('atmosphere onReopen');
        input.removeAttr('disabled').focus();
        content.html($('<p>', { text: 'Atmosphere re-connected using ' + response.transport }));
    };

    request.onMessage = function (response) {
        var message = response.responseBody;
        console.log(message);
        try {
            var json = atmosphere.util.parseJSON(message);
        } catch (e) {
            console.log('This doesn\'t look like a valid JSON: ', message);
            return;
        }

        input.removeAttr('disabled').focus();
        if (!logged) {
            logged = true;
            status.text(myName + ': ').css('color', 'blue');
        } else {
            var color = json.author === author ? 'blue' : 'black';
          	var datetime = new Date();
            content.append('<p><span style="color:' + color + '">' + json.author + '</span> @ ' + +(datetime.getHours() < 10 ? '0' + datetime.getHours() : datetime.getHours()) + ':'
                + (datetime.getMinutes() < 10 ? '0' + datetime.getMinutes() : datetime.getMinutes())
                + ': ' + json.message + '</p>');
            if(json.message == 'AUTH_COMPLETE') {
            	subSocket.push(atmosphere.util.stringifyJSON({ author: author, message: 'redirect' }));
            	window.location.replace('done.html');
            }
            
        }
    };

    request.onClose = function (response) {
    	console.log('atmosphere onClose');
        content.html($('<p>', { text: 'Server closed the connection after a timeout' }));
        input.attr('disabled', 'disabled');
    };

    request.onError = function (response) {
    	console.log('atmosphere onError');
        content.html($('<p>', { text: 'Sorry, but there\'s some problem with your '
            + 'socket or the server is down' }));
    };
    var subSocket = socket.subscribe(request);

    
    author = new Date().getTime() / 1000;
    var msg = 'start';
    subSocket.push(atmosphere.util.stringifyJSON({ author: author, message: msg }));
    $(this).val('');
    myName = author;


    function addMessage(author, message, color) {
    	var datetime = new Date();
        content.append('<p><span style="color:' + color + '">' + author + '</span> @ ' + +(datetime.getHours() < 10 ? '0' + datetime.getHours() : datetime.getHours()) + ':'
            + (datetime.getMinutes() < 10 ? '0' + datetime.getMinutes() : datetime.getMinutes())
            + ': ' + message + '</p>');
    }
});

