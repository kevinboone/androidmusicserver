var message_tick = 0;

/*
  Called on loading main page
*/
function onload ()
  {
  setInterval (tick, 5000);
  set_message ("Android music player ready");
  make_command_request ("status", response_callback_transport_status);
  }

/* 
   Called every 5 seconds by timer created at page load time
*/
function tick()
  {
  message_tick++;
  if (message_tick >= 2)
    {
    set_message("");
    message_tick = 0;
    }
  make_command_request ("status", response_callback_transport_status);
  }




function response_callback_transport_status (response_text)
  {
  var obj = eval ('(' + response_text + ')'); 
  set_transport_title (obj.title);
  set_transport_status (obj.transport_status);
  set_transport_position (msectominsec (obj.transport_position));
  set_transport_duration (msectominsec (obj.transport_duration));
  set_transport_artist (obj.artist);
  set_transport_album (obj.album);
  }

/*
  Make an HTTP request on the specified uri, and call callback with
  the results when complete
*/
function make_request (uri, callback)
  {
  var http_request = false;          
  if (window.XMLHttpRequest) 
    { // Mozilla, Safari, ...              
    http_request = new XMLHttpRequest();              
    if (http_request.overrideMimeType) 
      {                  
      http_request.overrideMimeType('text/plain');                  
      }          
    } 
  else if (window.ActiveXObject) 
    { // IE              
    try 
      {                  
      http_request = new ActiveXObject("Msxml2.XMLHTTP");              
      } 
    catch (e) 
      {                  
      try 
        {                      
        http_request = new ActiveXObject("Microsoft.XMLHTTP");
        } 
      catch (e) 
        {}              
      }          
    }          
  if (!http_request) 
    {              
    alert('Giving up :( Cannot create an XMLHTTP instance');              
    return false;          
    }          
  http_request.onreadystatechange = function() 
    { 
    do_http_request_complete (callback, http_request);
    };          
  http_request.open('GET', uri, true);          
  http_request.timeout = 10000; // Got to have _some_ value
  http_request.send(null);      
  }

/*
  do_http_request_complete
  Helper function for make_request
*/
function do_http_request_complete (callback, http_request) 
  {          
  if (http_request.readyState == 4) 
    {              
    if (http_request.status == 200) 
      {                  
      set_message ("");
      callback (http_request.responseText);              
      } 
    else 
      {                  
      //alert('There was a problem with the request.');              
      }          
    }      
  }  


/*
  stop() invoked from a link on the HTML page
*/
function stop()
  {
  make_command_request ("stop", response_callback_gen_status);
  }


/*
  play() invoked from a link on the HTML page
*/
function play()
  {
  make_command_request ("play", response_callback_gen_status);
  }


/*
  play_file_now(file) invoked from a link on the HTML page
*/
function play_file_now(file)
  {
  make_command_request ("play_file_now " + file , response_callback_gen_status);
  }


/*
  play_album_now(album) invoked from a link on the HTML page
*/
function play_album_now(album)
  {
  make_command_request ("play_album_now " + album, 
    response_callback_gen_status);
  }


/*
  add_to_playlist(file) invoked from a link on the HTML page
*/
function add_to_playlist(file)
  {
  make_command_request ("add_to_playlist " + file , response_callback_gen_status);
  }


/*
  add_album_to_playlist(album) invoked from a link on the HTML page
*/
function add_album_to_playlist(album)
  {
  make_command_request ("add_album_to_playlist " + album , response_callback_gen_status);
  }



/*
  pause() invoked by a link on the HTML page
*/
function pause()
  {
  make_command_request ("pause", response_callback_gen_status);
  }


/*
  prev() invoked by a link on the HTML page
*/
function prev()
  {
  make_command_request ("prev", response_callback_gen_status);
  }


/*
  next() invoked by a link on the HTML page
*/
function next()
  {
  make_command_request ("next", response_callback_gen_status);
  }


/*
  clear_playlist() invoked by a link on the HTML page
*/
function clear_playlist()
  {
  make_command_request ("clear_playlist", response_callback_gen_status);
  }


/*
  rescan_catalog() invoked by a link on the HTML page
*/
function rescan_catalog()
  {
  make_command_request ("rescan_catalog", response_callback_gen_status);
  }


/*
  random_album() invoked by a link on the HTML page
*/
function random_album()
  {
  make_command_request ("random_album", response_callback_gen_status);
  }


/*
  shuffle_playlist() invoked by a link on the HTML page
*/
function shuffle_playlist()
  {
  make_command_request ("shuffle_playlist", response_callback_gen_status);
  }


/*
  rescan_filesystem() invoked by a link on the HTML page
*/
function rescan_filesystem()
  {
  make_command_request ("rescan_filesystem", response_callback_gen_status);
  }


/*
  volume_up() invoked by a link on the HTML page
*/
function volume_up()
  {
  make_command_request ("volume_up", response_callback_gen_status);
  }


/*
  enable_eq() invoked by a link on the HTML page or this file
*/
function enable_eq()
  {
  make_command_request ("enable_eq", response_callback_gen_status);
  }

/*
  disable_eq() invoked by a link on the HTML page or this file
*/
function disable_eq()
  {
  make_command_request ("disable_eq", response_callback_gen_status);
  }

/*
  enable_bass_boost() invoked by a link on the HTML page or this file
*/
function enable_bass_boost()
  {
  make_command_request ("enable_bass_boost", response_callback_gen_status);
  }


/*
  disable_bass_boost() invoked by a link on the HTML page or this file
*/
function disable_bass_boost()
  {
  make_command_request ("disable_bass_boost", response_callback_gen_status);
  }


/*
  volume_down() invoked by a link on the HTML page
*/
function volume_down()
  {
  make_command_request ("volume_down", response_callback_gen_status);
  }


/*
  set_eq_level () invoked by a link on the HTML page or this page
*/
function set_eq_level(band, level)
  {
  make_command_request ("set_eq_level " + band + "," + level, 
     response_callback_gen_status);
  }


/*
  set_bb_level () invoked by a link on the HTML page or this page
*/
function set_bb_level(level)
  {
  make_command_request ("set_bb_level " + level, 
     response_callback_gen_status);
  }


/*
  set_vol_level () invoked by a link on the HTML page or this page
*/
function set_vol_level(level)
  {
  make_command_request ("set_vol_level " + level, 
     response_callback_gen_status);
  }



function make_command_request (cmd, callback)
  {
  self_uri = parse_uri (window.location.href);

    
  // The 'random' param is added to work around a stupid caching
  //  bug in IE
  cmd_uri = "http://" + self_uri.host  + ":" + self_uri.port + 
    "/cmd?cmd=" + encodeURIComponent (cmd) + "&random=" + Math.random();

  make_request (cmd_uri, callback);

  //if (cmd != "get_transport_status")
  //     set_message ("Communicating with server...");
  }


/*
  A general callback to be attached to server commands that generate
  no specific response except a status message (play, 
  add_to_playlist, etc)
*/
function response_callback_gen_status (response_text)
  {
  var obj = eval ('(' + response_text + ')'); 
  set_message (obj.message);
  }

/*
  The following functions just set text strings to page elements
*/
function set_message (msg)
  {
  document.getElementById ("messagecell").innerHTML = msg;
  }

function set_transport_uri (s)
  {
  document.getElementById ("transport_uri").innerHTML = s;
  }

function set_transport_title (s)
  {
  document.getElementById ("transport_title").innerHTML = s;
  }

function set_transport_album (s)
  {
  document.getElementById ("transport_album").innerHTML = s;
  }

function set_transport_artist (s)
  {
  document.getElementById ("transport_artist").innerHTML = s;
  }

function set_transport_status (s)
  {
  document.getElementById ("transport_status").innerHTML = s;
  }

function set_transport_position (s)
  {
  document.getElementById ("transport_position").innerHTML = s;
  }

function set_transport_duration (s)
  {
  document.getElementById ("transport_duration").innerHTML = s;
  }

/* 
parse_uri
Parse a uri into host, port, etc. Result are obatined in a structure
*/
function parse_uri (str) {
	var	o   = parse_uri.options,
		m   = o.parser[o.strictMode ? "strict" : "loose"].exec(str),
		uri = {},
		i   = 14;

	while (i--) uri[o.key[i]] = m[i] || "";

	uri[o.q.name] = {};
	uri[o.key[12]].replace(o.q.parser, function ($0, $1, $2) {
		if ($1) uri[o.q.name][$1] = $2;
	});

	return uri;
};


parse_uri.options = {
	strictMode: false,
	key: ["source","protocol","authority","userInfo","user","password","host","port","relative","path","directory","file","query","anchor"],
	q:   {
		name:   "queryKey",
		parser: /(?:^|&)([^&=]*)=?([^&]*)/g
	},
	parser: {
		strict: /^(?:([^:\/?#]+):)?(?:\/\/((?:(([^:@]*)(?::([^:@]*))?)?@)?([^:\/?#]*)(?::(\d*))?))?((((?:[^?#\/]*\/)*)([^?#]*))(?:\?([^#]*))?(?:#(.*))?)/,
		loose:  /^(?:(?![^:@]+:[^:@\/]*@)([^:\/?#.]+):)?(?:\/\/)?((?:(([^:@]*)(?::([^:@]*))?)?@)?([^:\/?#]*)(?::(\d*))?)(((\/(?:[^?#](?![^?#\/]*\.[^?#\/.]+(?:[?#]|$)))*\/?)?([^?#\/]*))(?:\?([^#]*))?(?:#(.*))?)/
	}
};


/* 
  Convert a time in msec to min:sec
*/
function msectominsec (msec)
  {
  var totalsec = Math.floor (msec / 1000);
  if (totalsec < 0) totalsec = 0; // Work around Xine bug
  var min = Math.floor (totalsec / 60);
  var sec = totalsec - min * 60;
  var smin = "" + min;
  if (min < 10) smin = "0" + smin;
  var ssec = "" + sec;
  if (sec < 10) ssec = "0" + ssec;
  return "" + smin + ":" + ssec; 
  }

/*
  Called when EQ is enabled or disabled on the gui_eq page
*/
function onClickEqEnabled (cb)
  {
  if (cb.checked)
    enable_eq ();
  else
    disable_eq ();
  }
 

/*
  Called when bass boost is enabled or disabled on the gui_eq page
*/
function onClickBBEnabled (cb)
  {
  if (cb.checked)
    enable_bass_boost ();
  else
    disable_bass_boost ();
  }
 

/*
  Called when an EQ slider is moved
*/
function onChangeEqSlider (band, value)
  {
  set_eq_level (band, value);
  }


/*
  Called when the BB slider is moved
*/
function onChangeBBSlider (value)
  {
  set_bb_level (value);
  }


/*
  Called when the Volume slider is moved
*/
function onChangeVolSlider (value)
  {
  set_vol_level (value);
  }


/* We have to go to prodigious lengths to get a delay in JS.
   In this case, we need to interpose a delay between executing a 
   command on the server, and having the page refresh itself. */
function refresh()
  {
  window.location.reload(true);
  }


function delay_and_refresh()
  {
  setTimeout ("refresh()", 300);
  }




