/*
 *  Kevin's Music Server for Android
 *  Copyright (c)2015
 *  Distributed under the terms of the GNU Public Licence, version 2.0
 */

package net.kevinboone.androidmediaserver;
import java.util.*;
import java.text.*;
import java.io.*;
import java.net.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.media.MediaPlayer;
import android.util.Log;
import android.media.*;

public class WebServer extends NanoHTTPD implements
   MediaPlayer.OnCompletionListener,
   AudioManager.OnAudioFocusChangeListener
{
protected static String DOCROOT="/";
private MediaPlayer mediaPlayer = new MediaPlayer();
private String currentPlaybackUri = null; //File 
private TrackInfo currentPlaybackTrackInfo = null;
private Context context = null;
protected List<TrackInfo> playlist = new Vector<TrackInfo>();
protected int currentPlaylistIndex = -1;
protected AudioDatabase audioDatabase = null;
private String lastModifiedString = null; 
private Date lastModifiedDate = null; 

public WebServer (Context context)
  {
  super(30000);
  this.context = context;
  setLastModifiedToNow();
  mediaPlayer.setOnCompletionListener (this);
  RemoteControlReceiver.setWebServer (this);
  audioDatabase = new AudioDatabase();
  audioDatabase.scan (context);
  }

  @Override
  public Response serve (String uri, Method method, 
                              Map<String, String> header,
                              Map<String, String> parameters,
                              Map<String, String> files)
    {
    Log.w ("AMP", "Got URI: " + uri);

    /* 
       Check If-Modified-Since. We only set a Last-Modified on images at
       present, because every other response will be quite small. So, in
       principle, we need not really check the dates at all, as only 
       images will have an If-Modified-Since header, and they won't change
       in the life of the program. Still, better to do things properly, 
       I guess. The base date for the modification test is when the 
       program starts, lacking any better date baseline.
    */

    // Watch out -- NanoHTTPD lowercases header names :/

    String ifModifiedHeader = header.get("if-modified-since");
    if (ifModifiedHeader != null && ifModifiedHeader.length() != 0)
      {
      SimpleDateFormat gmtFrmt = new SimpleDateFormat
        ("E, d MMM yyyy HH:mm:ss 'GMT'");
      gmtFrmt.setTimeZone(TimeZone.getTimeZone("GMT"));
      try
        {
        Date ifModifiedDate = gmtFrmt.parse (ifModifiedHeader);
        if (ifModifiedDate.getTime() <= lastModifiedDate.getTime())
          {
          return new NanoHTTPD.Response 
            (Response.Status.NOT_MODIFIED, "text/plain", 
               "Not modified"); 
          }
        }
      catch (Exception e)
        {
        // Broken client -- what the heck can we do?
        }
      }



    if (uri.indexOf ("~~") == 0)
      {
      return serveResource (uri.substring(2));
      }
    else if (uri.indexOf ("/~~") == 0)
      {
      return serveResource (uri.substring(3));
      }
    else if (uri.indexOf ("cmd") == 0)
      {
      return (handleCommand (parameters.get("cmd")));
      }
    else if (uri.indexOf ("cover") == 0)
      {
      return (handleCover (parameters.get("album")));
      }
    else if (uri.indexOf ("/cover") == 0)
      {
      return (handleCover (parameters.get("album")));
      }
    else if (uri.indexOf ("/cmd") == 0)
      {
      return (handleCommand (parameters.get("cmd")));
      }
    else if ("/".equals (uri) || "".equals (uri))
      { 
      return new NanoHTTPD.Response (Response.Status.OK, "text/html", 
            makeRedirect ("/gui_home"));
      }
    else
      { 
      if (uri.indexOf ("gui_files") == 0)
        return handleGuiFiles (parameters);
      if (uri.indexOf ("/gui_files") == 0)
        return handleGuiFiles (parameters);
      if (uri.indexOf ("gui_albums_by_genre") == 0)
        return handleGuiAlbumsByGenre (parameters);
      if (uri.indexOf ("/gui_albums_by_genre") == 0)
        return handleGuiAlbumsByGenre (parameters);
      if (uri.indexOf ("gui_albums_by_artist") == 0)
        return handleGuiAlbumsByArtist (parameters);
      if (uri.indexOf ("/gui_albums_by_artist") == 0)
        return handleGuiAlbumsByArtist (parameters);
      if (uri.indexOf ("gui_playlist") == 0)
        return handleGuiPlaylist (parameters);
      if (uri.indexOf ("/gui_playlist") == 0)
        return handleGuiPlaylist (parameters);
      if (uri.indexOf ("gui_home") == 0)
        return handleGuiHome (parameters);
      if (uri.indexOf ("/gui_home") == 0)
        return handleGuiHome (parameters);
      if (uri.indexOf ("gui_albums") == 0)
        return handleGuiAlbums (parameters);
      if (uri.indexOf ("/gui_albums") == 0)
        return handleGuiAlbums (parameters);
      if (uri.indexOf ("gui_genres") == 0)
        return handleGuiGenres (parameters);
      if (uri.indexOf ("/gui_genres") == 0)
        return handleGuiGenres (parameters);
      if (uri.indexOf ("gui_artists") == 0)
        return handleGuiArtists (parameters);
      if (uri.indexOf ("/gui_artists") == 0)
        return handleGuiArtists (parameters);
      if (uri.indexOf ("gui_tracks_by_album") == 0)
        return handleGuiTracksByAlbum (parameters);
      if (uri.indexOf ("/gui_tracks_by_album") == 0)
        return handleGuiTracksByAlbum (parameters);
      return new NanoHTTPD.Response 
        (Response.Status.OK, "text/plain", "Unknown request: " + uri); // TODO
      }
    }

  private void setLastModifiedToNow()
    {
    SimpleDateFormat gmtFrmt = new SimpleDateFormat
      ("E, d MMM yyyy HH:mm:ss 'GMT'");
    gmtFrmt.setTimeZone(TimeZone.getTimeZone("GMT"));
    lastModifiedDate = new Date();
    lastModifiedString = gmtFrmt.format(lastModifiedDate);
    }

  /**
   Form a string to append to any URL, containing the parameters that
   are generally carried forward from request to request.
   */
  String makeGenParams (Map<String, String> parameters)
    {
    String s = "";
    String covers = parameters.get("covers");
    if (covers != null && covers.length() > 0)
      s += "covers=" + covers + "&";
    return s;
    }

  /**
    Format a set of albums as a table.
  */
  String makeAlbumListFromSet (Map<String,String> parameters, boolean covers,
       Set<String> albums)
    {
    StringBuffer sb = new StringBuffer();
    sb.append ("<table>");
    for (String album : albums)
      {
      sb.append ("<tr>");
      sb.append ("<td valign=\"top\">");
      if (covers)
        sb.append ("<img width=\"64\" src=\"/cover?album=" 
         + URLEncoder.encode (album) + "\"/>"); 
      sb.append ("</td>");
      sb.append ("<td valign=\"top\">");
      sb.append (album);
      sb.append ("<br/>");
      sb.append (" <a href=\"javascript:play_album_now('" 
              + EscapeUtils.escapeJSON (album) + 
                "')\"><span class=\"textbuttonspan\">Play now</span></a> ");
      sb.append (" <a href=\"javascript:add_album_to_playlist('" 
              + EscapeUtils.escapeJSON (album) + 
                "')\"><span class=\"textbuttonspan\">Add</span></a> ");
      sb.append (" <a href=\"/gui_tracks_by_album?album=" 
              + URLEncoder.encode (album) + "&" + makeGenParams (parameters) + 
                "\"><span class=\"textbuttonspan\">Open</span></a> ");
      sb.append ("</td>");
      sb.append ("</tr>");
      }
    sb.append ("</table>\n");
    return new String (sb);
    }

  
  /**
    Formats a list of albums, maintained by MediaDatabase
  */
  String makeAlbumList (Map<String,String> parameters, boolean covers)
    {
    StringBuffer sb = new StringBuffer();
    sb.append ("<span class=\"pagetitle\">" + "Albums" + "</span><p/>");
    sb.append ("<table>");

    Set<String> albums = audioDatabase.getAlbums();
    sb.append (makeAlbumListFromSet (parameters, covers, albums));
    return new String (sb);
    }


  /**
    Formats a list of genres, maintained by MediaDatabase
  */
  String makeGenreList (Map<String,String> parameters, boolean covers)
    {
    StringBuffer sb = new StringBuffer();
    sb.append ("<span class=\"pagetitle\">" + "Genres" + "</span><p/>");
    sb.append ("<table>");

    Set<String> genres = audioDatabase.getGenres();
    for (String genre: genres)
      {
      sb.append ("<tr>");
      sb.append ("<td valign=\"top\">");
// Nothing in the image slot yet
      sb.append ("</td>");
      sb.append ("<td valign=\"top\">");
      sb.append (" <a href=\"/gui_albums_by_genre?genre=" 
              + URLEncoder.encode (genre) + "&" + makeGenParams (parameters) + 
                "\"><span>" + genre + "</span></a> ");
      sb.append ("</td>");
      sb.append ("</tr>");
      }
    sb.append ("</table>\n");
    return new String (sb);
    }


  /**
    Formats a list of artists, maintained by MediaDatabase
  */
  String makeArtistList (Map<String,String> parameters, boolean covers)
    {
    StringBuffer sb = new StringBuffer();
    sb.append ("<span class=\"pagetitle\">" + "Artists" + "</span><p/>");
    sb.append ("<table>");

    Set<String> artists = audioDatabase.getArtists();
    for (String artist: artists)
      {
      sb.append ("<tr>");
      sb.append ("<td valign=\"top\">");
// Nothing in the image slot yet
      sb.append ("</td>");
      sb.append ("<td valign=\"top\">");
      sb.append (" <a href=\"/gui_albums_by_artist?artist=" 
              + URLEncoder.encode (artist) + "&" + makeGenParams (parameters) + 
                "\"><span>" + artist + "</span></a> ");
      sb.append ("</td>");
      sb.append ("</tr>");
      }
    sb.append ("</table>\n");
    return new String (sb);
    }

  
  
  
  /**
    Format a list of tracks matching the specified album.
  */
  String makeTracksByAlbum (String album, boolean covers)
    {
    StringBuffer sb = new StringBuffer();

    sb.append ("<span class=\"pagetitle\">" + album + "</span><p/>");

    List<String> uris = audioDatabase.getAlbumURIs (context, album);
    
    if (covers)
      {
      sb.append ("<img width=\"128\" src=\"/cover?album=" 
         + URLEncoder.encode (album) + "\"/>"); 
      sb.append ("<p/>");
      sb.append ("<br flush=\"all\"/>\n");
      }      

    for (String uri : uris)
      {
      sb.append (" <a href=\"javascript:play_file_now('" 
              + EscapeUtils.escapeJSON (uri) + 
                "')\"><span class=\"textbuttonspan\">Play now</span></a> ");
      sb.append (" <a href=\"javascript:add_to_playlist('" 
              + EscapeUtils.escapeJSON (uri) + 
                "')\"><span class=\"textbuttonspan\">Add</span></a> ");
      TrackInfo ti = audioDatabase.getTrackInfo (context, uri);
      sb.append (ti.title);
      sb.append ("<br/>");
      }
    return new String (sb);
    }


  /**
    Format a list of albums matching the specified genre.
  */
  String makeAlbumsByGenre (Map<String,String> parameters, 
      String genre, boolean covers)
    {
    StringBuffer sb = new StringBuffer();

    sb.append ("<span class=\"pagetitle\">Albums in genre '" 
     + genre + "'</span><p/>");

    Set<String> albums = audioDatabase.getAlbumsByGenre (context, genre);
    sb.append (makeAlbumListFromSet (parameters, covers, albums));

    return new String (sb);
    }

  /**
    Format a list of albums matching the specified artist.
  */
  String makeAlbumsByArtist (Map<String,String> parameters, 
      String artist, boolean covers)
    {
    StringBuffer sb = new StringBuffer();

    sb.append ("<span class=\"pagetitle\">Albums by artist '" 
     + artist + "'</span><p/>");

    Set<String> albums = audioDatabase.getAlbumsByArtist (context, artist);
    sb.append (makeAlbumListFromSet (parameters, covers, albums));

    return new String (sb);
    }



  /**
    Formats the specified path as an HTML directory list, to be 
    inserted between the page headers and footers.
  */
  String makeDirList (String uri, Map<String,String> parameters)
    {
    File file = new File (DOCROOT + "/" + uri);
    StringBuffer sb = new StringBuffer();

    sb.append ("<span class=\"pagetitle\">" + uri + "</span><p/>");

    if (uri.equals("") || uri.equals ("/"))
      {
      // At docroot -- do nowt
      }
    else
      {
      String parent = "/";
      int p = uri.lastIndexOf ('/');
      if (p > 0)
        parent = uri.substring (0, p); 
      sb.append ("<a href=\"gui_files?path=" 
        + URLEncoder.encode (parent) + 
        "\"><span class=\"textbuttonspan\">Up</span></a><br/>");
      }

    String[] list = file.list();
    Arrays.sort (list);

    if (list != null)
      {
      for (int i = 0; i < list.length; i++)
        {
        if (list[i].startsWith (".")) continue;
        File direntry = new File (DOCROOT + "/" + uri + "/" + list[i]);
        if (direntry.isFile ())
          {
          if (isPlayableFile (list[i]))
            {
            sb.append (" <a href=\"javascript:play_file_now('" 
              + EscapeUtils.escapeJSON (uri + "/" + list[i]) + 
                "')\"><span class=\"textbuttonspan\">Play now</span></a>");

            sb.append (" <a href=\"javascript:add_to_playlist('" 
              + EscapeUtils.escapeJSON (uri + "/" + list[i]) + 
                "')\"><span class=\"textbuttonspan\">Add</span> </a>");
            sb.append (list[i]);
            sb.append ("<br/>\n");
            }
          }
        else if (direntry.isDirectory ())
          {
          if ("/".equals (uri))
            sb.append ("<a href=\"gui_files?path=" + uri +  
             URLEncoder.encode (list[i]) + "\">");
          else
            sb.append ("<a href=\"gui_files?path=" + uri + "/" + 
             URLEncoder.encode (list[i]) + "\">");
          sb.append ("<span class=\"textbuttonspan\">Open</span>");
          sb.append ("</a> ");
          sb.append (" <a href=\"javascript:add_to_playlist('" 
              + EscapeUtils.escapeJSON (uri + "/" + list[i]) 
              + "')\"><span class=\"textbuttonspan\">Add files</span> </a>");
          sb.append (list[i]);
          sb.append ("<br/>\n");
          }
        }
      }
    else
      {
      // Probably no premissions on the directory
      sb.append ("Sorry, can't list directory");
      }
    return new String (sb);
    }


  /**
  Start playback of the file, and set the currentUri.
  */
  void playFileNow (String uri)
      throws IOException
    {
    mediaPlayer.reset();
    if (uri.startsWith ("content:"))
      {
      android.net.Uri contentUri = android.net.Uri.parse (uri);
      mediaPlayer.setDataSource (context, contentUri);
      }
    else
      {
      String filename = DOCROOT + "/" + uri;
      mediaPlayer.setDataSource (filename);
      }
    mediaPlayer.prepare();
    getAudioFocus();
    currentPlaybackUri = uri;
    currentPlaybackTrackInfo = audioDatabase.getTrackInfo (context, uri);
    mediaPlayer.start();
    }


  /**
  Adds the specified item, which might be a directory, to the playlist,
  and return the number of items added.
  */
  int addToPlaylist (String uri)
      throws IOException
    {
    File f = new File (DOCROOT + "/" + uri);
    if (f.isDirectory ())
      {
      String[] list = f.list();      
      int count = 0;
      for (String name : list)
        {
        String cand = uri + "/" + name;
        File f2 = new File (DOCROOT + "/" + cand);
        if (isPlayableFile (f2.toString()))
          {
          TrackInfo ti = audioDatabase.getTrackInfo (context, cand);
          playlist.add (ti);
          count++;
          }
        }
      return count;
      }
    else
      {
      TrackInfo ti = audioDatabase.getTrackInfo (context, uri);
      playlist.add (ti);
      return 1; 
      }
    }


  String makeControls (Map<String,String> parameters)
    {
    StringBuffer sb = new StringBuffer();

    sb.append ("<p/>");
    sb.append ("<hr/>");
    sb.append ("<a href=\"/\">Home</a> | ");
    sb.append ("<a href=\"/gui_files\">Files</a> | ");
    sb.append ("<a href=\"/gui_albums?" + makeGenParams (parameters) 
       + "\">Albums</a> | ");
    sb.append ("<a href=\"/gui_genres?" + makeGenParams (parameters) 
       + "\">Genres</a> | ");
    sb.append ("<a href=\"/gui_artists?" + makeGenParams (parameters) 
       + "\">Artists</a> | ");
    sb.append ("<a href=\"/gui_playlist?" + makeGenParams (parameters) 
       + "\">Playlist</a>");
    sb.append ("<p/>");

    return new String (sb);
    }


  NanoHTTPD.Response serveFileResource (String filePath) 
    {
    try
      {
      InputStream is = new FileInputStream (new File (filePath));
      return new NanoHTTPD.Response (Response.Status.OK, 
        FileUtils.getMimeType (filePath), is);
      }
    catch (Exception e)
      {
      return new NanoHTTPD.Response (Response.Status.NOT_FOUND, 
        "text/plain", e.toString());
      }
    }

  /**
 * Handles a URL of the form /cover?album=xxx.
 */
  NanoHTTPD.Response handleCover (String album) 
    {
    BitmapFactory.Options bfo = new BitmapFactory.Options();
    bfo.inJustDecodeBounds = true;
    String filePath = null;

    try
      {
      byte[] ep = null;
      List<String> trackUris = audioDatabase.getAlbumURIs (context, album);
      for (String uri : trackUris)
        {
        if (filePath == null)
          filePath = audioDatabase.getFilePathFromContentUri 
            (context, android.net.Uri.parse(uri));
        ep = audioDatabase.getEmbeddedPicture (context, uri);
        if (ep != null)
          {
          break;
          }
        }
      if (ep != null)
        {
        Bitmap bm = BitmapFactory.decodeByteArray (ep, 0, ep.length, bfo); 

        String mimeType = bfo.outMimeType;
  
        InputStream is = new ByteArrayInputStream (ep); 
        NanoHTTPD.Response resp = new NanoHTTPD.Response 
          (Response.Status.OK, mimeType, is);
        resp.addHeader ("Last-Modified", lastModifiedString);
        resp.addHeader ("Cache-Control", "public");
        SimpleDateFormat gmtFrmt = new SimpleDateFormat
          ("E, d MMM yyyy HH:mm:ss 'GMT'");
        gmtFrmt.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date oneHour = new Date (new Date().getTime() + 3600000);
        resp.addHeader ("Expires", gmtFrmt.format (oneHour));
        //is.close();
        return resp;
        }
      else if (filePath != null)
        {
        String coverFile = CoverUtils.getCoverFileForTrackFile (filePath);
        Log.w ("XXX", "COVER FILE=" + coverFile); 
        if (coverFile != null)
          return serveFileResource (coverFile);
        else
          return serveResource ("default_cover_png");
        }
      else
        return serveResource ("default_cover_png");
      }
    catch (Exception e)
      {
      return serveResource ("default_cover_png");
      }
    }



  /**
 * Handles a URL of the form /cmd?cmd=xxx.
 */
  NanoHTTPD.Response handleCommand (String cmd) 
    {
    try
      {
      if ("status".equalsIgnoreCase (cmd))
        {
        return new NanoHTTPD.Response (Response.Status.OK, "text/plain", 
            getTransportStatusAsJSON());
        }
      else if ("stop".equalsIgnoreCase (cmd))
        {
        stopPlayback();
        }
      else if ("pause".equalsIgnoreCase (cmd))
        {
        pause();
        }
      else if (cmd.toLowerCase().startsWith ("play_file_now"))
        {
        String uri = cmd.substring (14); 
        playFileNow (uri);
        return new NanoHTTPD.Response (Response.Status.OK, "text/plain", 
            makeJSONStatusResponse (0));
        }
      else if (cmd.toLowerCase().startsWith ("add_album_to_playlist"))
        {
        String album = cmd.substring (22); 
        return addAlbumToPlaylist (album);
        }
      else if (cmd.toLowerCase().startsWith ("add_to_playlist"))
        {
        String uri = cmd.substring (16); 
        int n = addToPlaylist (uri);
        return new NanoHTTPD.Response (Response.Status.OK, "text/plain", 
            makeJSONStatusResponse (0, "Added " + n + " items(s) to playlist"));
        }
      else if (cmd.toLowerCase().startsWith ("play_album_now"))
        {
        String album = cmd.substring (15); 
        return playAlbumNow (album);
        }
      else if ("play".equalsIgnoreCase (cmd))
        {
        return play();
        }
      else if ("next".equalsIgnoreCase (cmd))
        {
        return playNextInPlaylist();
        }
      else if ("clear_playlist".equalsIgnoreCase (cmd))
        {
        return clearPlaylist();
        }
      else if ("random_album".equalsIgnoreCase (cmd))
        {
        return playRandomAlbum();
        }
      else if ("prev".equalsIgnoreCase (cmd))
        {
        return playPrevInPlaylist();
        }
      else if ("volume_up".equalsIgnoreCase (cmd))
        {
        return volumeUp();
        }
      else if ("volume_down".equalsIgnoreCase (cmd))
        {
        return volumeDown();
        }
      else if ("rescan_catalog".equalsIgnoreCase (cmd))
        {
        return rescanCatalog();
        }
      else if ("rescan_filesystem".equalsIgnoreCase (cmd))
        {
        return rescanFilesystem();
        }
      }
    catch (Exception e)
      {
      return new NanoHTTPD.Response (Response.Status.OK, "text/plain", 
        makeJSONStatusResponse (e));
      }

    // We should neve get here, unless somebody is probing the server
    //   with random commands
    return new NanoHTTPD.Response (Response.Status.OK, "text/html", "?");
    }


  /**
  Returns an embedded binary object with the appropriate MIME type. THis is
  all a but ugly, but it's hard to provide bunary files with an Android
  app in a better way.
  */
  NanoHTTPD.Response serveResource (String resource)
    {
    if ("styles_css".equals (resource))
      {
      InputStream is = context.getResources().openRawResource 
        (R.raw.styles_css);
      return new NanoHTTPD.Response (Response.Status.OK, "text/css", is);
      }
    else if ("functions_js".equals (resource))
      {
      InputStream is = context.getResources().openRawResource 
        (R.raw.functions_js);
      return new NanoHTTPD.Response (Response.Status.OK, "text/javascript", is);
      }
    else if ("logo_png".equals (resource))
      {
      InputStream is = context.getResources().openRawResource 
        (R.raw.logo_png);
      return new NanoHTTPD.Response (Response.Status.OK, "image/png", is);
      }
    else if ("default_cover_png".equals (resource))
      {
      InputStream is = context.getResources().openRawResource 
        (R.raw.default_cover_png);
      return new NanoHTTPD.Response (Response.Status.OK, "image/png", is);
      }
    else if ("playbutton_png".equals (resource))
      {
      InputStream is = context.getResources().openRawResource 
        (R.raw.playbutton_png);
      return new NanoHTTPD.Response (Response.Status.OK, "image/png", is);
      }
    else if ("prevbutton_png".equals (resource))
      {
      InputStream is = context.getResources().openRawResource 
        (R.raw.prevbutton_png);
      return new NanoHTTPD.Response (Response.Status.OK, "image/png", is);
      }
    else if ("pausebutton_png".equals (resource))
      {
      InputStream is = context.getResources().openRawResource 
        (R.raw.pausebutton_png);
      return new NanoHTTPD.Response (Response.Status.OK, "image/png", is);
      }
    else if ("nextbutton_png".equals (resource))
      {
      InputStream is = context.getResources().openRawResource 
        (R.raw.nextbutton_png);
      return new NanoHTTPD.Response (Response.Status.OK, "image/png", is);
      }
    else if ("stopbutton_png".equals (resource))
      {
      InputStream is = context.getResources().openRawResource 
        (R.raw.stopbutton_png);
      return new NanoHTTPD.Response (Response.Status.OK, "image/png", is);
      }
    else if ("vol_up_png".equals (resource))
      {
      InputStream is = context.getResources().openRawResource 
        (R.raw.vol_up_png);
      return new NanoHTTPD.Response (Response.Status.OK, "image/png", is);
      }
    else if ("vol_down_png".equals (resource))
      {
      InputStream is = context.getResources().openRawResource 
        (R.raw.vol_down_png);
      return new NanoHTTPD.Response (Response.Status.OK, "image/png", is);
      }
    else
      return new NanoHTTPD.Response 
        (Response.Status.OK, "text/plain", "No resource " + resource);
    }


  protected String makeHtmlHeader ()
    {
    try
      {
      InputStream is = context.getResources().openRawResource 
        (R.raw.header_html);
      Scanner s = new Scanner(is).useDelimiter("\\A");
      String ret = s.hasNext() ? s.next() : "";
      is.close();
      return ret;
      }
    catch (Exception e)
      {
      return "Argh! No HTML header";
      }
    }


  protected String makeHtmlFooter ()
    {
    return "</div></body></html>\n";
    }

  protected String makeRedirect (String url)
    {
    StringBuffer sb = new StringBuffer();
    sb.append ("<html><head>");
    sb.append ("<meta http-equiv=\"refresh\" content=\"0;url=" + 
      url + "\">");
    sb.append ("</head><body>");
    sb.append ("</body><html>");
    return new String (sb);
    }


  protected String getTransportStatusAsJSON ()
    {
    // TODO
    String transport = "stopped";
    String uri = "";
    String duration = "0";
    String position = "0";
    String title = "";
    String album = "";
    String artist = "";
    if (mediaPlayer.isPlaying())
      {
      transport = "playing";
      if (currentPlaybackTrackInfo != null)
        {
        title = currentPlaybackTrackInfo.title;
        album = currentPlaybackTrackInfo.album;
        artist = currentPlaybackTrackInfo.artist;
        }
      uri = currentPlaybackUri;
      duration = "" + mediaPlayer.getDuration();
      position = "" + mediaPlayer.getCurrentPosition();
      }
    else if (currentPlaybackUri != null)
      {
      transport = "paused";
      if (currentPlaybackTrackInfo != null)
        {
        title = currentPlaybackTrackInfo.title;
        album = currentPlaybackTrackInfo.album;
        artist = currentPlaybackTrackInfo.artist;
        }
      uri = currentPlaybackUri;
      duration = "" + mediaPlayer.getDuration();
      position = "" + mediaPlayer.getCurrentPosition();
      }
    String ret="{" + "status:0,"  + 
      "transport_status:'" + EscapeUtils.escapeJSON(transport) + "'," +
      "transport_position:'" + EscapeUtils.escapeJSON(position) + "'," +
      "transport_duration:'" + EscapeUtils.escapeJSON(duration) + "'," +
      "album:'" + EscapeUtils.escapeJSON(album) + "'," +
      "artist:'" + EscapeUtils.escapeJSON(artist) + "'," +
      "title:'" + EscapeUtils.escapeJSON (title) 
        + "'," +
      "uri:'" + EscapeUtils.escapeJSON (uri) + "'}";
     return ret;
    }


  /** Convenience method that makes a JSON response to the browser
      based on an error code only. */
  protected String makeJSONStatusResponse (int code)
    {
    String message = Errors.perror (code); 
    return makeJSONStatusResponse (code, message);
    }


  /** Convenience method that makes a JSON response to the browser
      based on an error code and a message. */
  protected String makeJSONStatusResponse (int code, String message)
    {
    return "{status:" + code + ",message:'" + EscapeUtils.escapeJSON(message) + "'}";
    }


  /** Convenience method that makes a JSON response to the browser
      based on an exception. */
  protected String makeJSONStatusResponse (Exception e)
    {
    return makeJSONStatusResponse (-1, e.toString());
    }

  /**
    Make the track list-by-album page for the "album" specified in the request
  */
  protected Response handleGuiTracksByAlbum (Map<String,String> parameters)
    {
    String album = parameters.get("album");
    if (album == null)
      album = ""; // Prevent a crash

    boolean covers = false;
    if ("true".equals (parameters.get("covers")))
      covers = true;

    String answer = makeHtmlHeader();
    answer += makeTracksByAlbum (album, covers);
    answer += makeControls(parameters);
    answer += makeHtmlFooter();
    return new NanoHTTPD.Response (answer);
    }


  /**
    Make the album-list-by-genre page for the "genre" specified in the request
  */
  protected Response handleGuiAlbumsByGenre (Map<String,String> parameters)
    {
    String genre = parameters.get("genre");
    if (genre == null)
      genre = ""; // Prevent a crash

    boolean covers = false;
    if ("true".equals (parameters.get("covers")))
      covers = true;

    String answer = makeHtmlHeader();
    answer += makeAlbumsByGenre (parameters, genre, covers);
    answer += makeControls(parameters);
    answer += makeHtmlFooter();
    return new NanoHTTPD.Response (answer);
    }


  /**
    Make the album-list-by-artist page for the "artist " 
    specified in the request.
  */
  protected Response handleGuiAlbumsByArtist (Map<String,String> parameters)
    {
    String artist = parameters.get("artist");
    if (artist == null)
      artist = ""; // Prevent a crash

    boolean covers = false;
    if ("true".equals (parameters.get("covers")))
      covers = true;

    String answer = makeHtmlHeader();
    answer += makeAlbumsByArtist (parameters, artist, covers);
    answer += makeControls(parameters);
    answer += makeHtmlFooter();
    return new NanoHTTPD.Response (answer);
    }



  /**
    Make the file list page for the "path" specified in the request
  */
  protected Response handleGuiFiles (Map<String,String> parameters)
    {
    String path = parameters.get("path");
    if (path == null || path.length() == 0)
      path = "/";

    String answer = makeHtmlHeader();
    answer += makeDirList (path, parameters); 
    answer += makeControls(parameters);
    answer += makeHtmlFooter();
    return new NanoHTTPD.Response (answer);
    }


  /**
    Make the album list page. 
  */
  protected Response handleGuiAlbums (Map<String,String> parameters)
    {
    boolean covers = false;
    if ("true".equals (parameters.get("covers")))
      covers = true;
    String answer = makeHtmlHeader();
    answer += makeAlbumList (parameters, covers); 
    answer += makeControls(parameters);
    answer += makeHtmlFooter();
    return new NanoHTTPD.Response (answer);
    }

  /**
    Make the genre list page. 
  */
  protected Response handleGuiGenres (Map<String,String> parameters)
    {
    boolean covers = false;
    if ("true".equals (parameters.get("covers")))
      covers = true;
    String answer = makeHtmlHeader();
    answer += makeGenreList (parameters, covers); 
    answer += makeControls(parameters);
    answer += makeHtmlFooter();
    return new NanoHTTPD.Response (answer);
    }

  /**
    Make the artist list page. 
  */
  protected Response handleGuiArtists (Map<String,String> parameters)
    {
    boolean covers = false;
    if ("true".equals (parameters.get("covers")))
      covers = true;
    String answer = makeHtmlHeader();
    answer += makeArtistList (parameters, covers); 
    answer += makeControls(parameters);
    answer += makeHtmlFooter();
    return new NanoHTTPD.Response (answer);
    }




  
  protected String makeHomePage (Map<String,String> parameters)
    {
    StringBuffer sb = new StringBuffer();

    sb.append 
     ("<span class=\"pagesubtitle\">Browse albums with covers</span><br/>");
    sb.append ("&nbsp;&nbsp;<a href=\"/gui_albums?covers=true\">Browse all albums</a><br/>");
    sb.append ("&nbsp;&nbsp;<a href=\"/gui_genres?covers=true\">Browse albums by genre</a><br/>");
    sb.append ("&nbsp;&nbsp;<a href=\"/gui_artists?covers=true\">Browse albums by artist</a><br/>");
    sb.append ("<p/>");

    sb.append 
     ("<span class=\"pagesubtitle\">Browse albums without covers</span><br/>");
    sb.append ("&nbsp;&nbsp;<a href=\"/gui_albums?covers=false\">Browse all albums</a><br/>");
    sb.append ("&nbsp;&nbsp;<a href=\"/gui_genres?covers=false\">Browse albums by genre</a><br/>");
    sb.append ("&nbsp;&nbsp;<a href=\"/gui_artists?covers=false\">Browse albums by artist</a><br/>");
    sb.append ("<p/>");

    sb.append 
     ("<span class=\"pagesubtitle\">Browse files</span><br/>");
    sb.append ("&nbsp;&nbsp;<a href=\"/gui_files\">Browse whole filesystem</a><br/>");
    sb.append ("<p/>");

    sb.append 
     ("<span class=\"pagesubtitle\">Playlist</span><br/>");
    sb.append ("&nbsp;&nbsp;<a href=\"javascript:random_album()\">Play a randomly-selected album</a><br/>");
    sb.append ("&nbsp;&nbsp;<a href=\"/gui_playlist\">View the current playlist</a><br/>");
    sb.append ("&nbsp;&nbsp;<a href=\"javascript:clear_playlist()\">Clear the playlist</a>");
    sb.append ("<p/>");

    sb.append 
     ("<span class=\"pagesubtitle\">Administration</span><br/>");
    sb.append ("&nbsp;&nbsp;<a href=\"javascript:rescan_catalog()\">Rescan the media catalog (quick)</a><br/>");
    sb.append ("&nbsp;&nbsp;<a href=\"javascript:rescan_filesystem()\">Rescan the filesystem (slow)</a><br/>");
    sb.append ("<p/>");


    sb.append 
     ("<span class=\"pagesubtitle\">Help</span><br/>");
    sb.append ("&nbsp;&nbsp;<a href=\"http://kevinboone.net/README_androidmusicserver.html\">Read the on-line documentation</a><br/>");
    sb.append ("<p/>");

    return new String (sb);
    }

  /*
    Make the home page 
  */
  protected Response handleGuiHome (Map<String,String> parameters)
    {
    String answer = makeHtmlHeader();
    answer += "<span class=\"pagetitle\">Main index</span><p/>"; 
    answer += makeHomePage(parameters);
    answer += makeControls(parameters);
    answer += makeHtmlFooter();
    return new NanoHTTPD.Response (answer);
    }


  /*
    Make the playlist page 
  */
  protected Response handleGuiPlaylist (Map<String,String> parameters)
    {
    String answer = makeHtmlHeader();
    answer += "<span class=\"pagetitle\">Playlist</span><p/>"; 
    if (playlist.size() == 0)
      {
      answer += "<i>Playlist is empty</i>"; 
      }
    else
      {
      for (TrackInfo ti : playlist)
        {
        answer += ti.title; 
        answer += "<br/>"; 
        }
      }
    answer += makeControls(parameters);
    answer += makeHtmlFooter();
    return new NanoHTTPD.Response (answer);
    }


  /** Returns true if the filename suggests mp3, aac, etc. */
  boolean isPlayableFile (String name)
    {
    int p = name.lastIndexOf ('.');
    if (p <= 0) return false;
    String ext = name.substring (p);
    if (".mp3".equalsIgnoreCase (ext)) return true;
    if (".m4a".equalsIgnoreCase (ext)) return true;
    if (".aac".equalsIgnoreCase (ext)) return true;
    if (".ogg".equalsIgnoreCase (ext)) return true;
    if (".wma".equalsIgnoreCase (ext)) return true;
    if (".flac".equalsIgnoreCase (ext)) return true;
    return false;
    }

  /** Handle play request, but playing either the playlist, or
      unpausing if we are paused. */
  NanoHTTPD.Response play()
    {
    if (currentPlaybackUri != null)
          {
          // We are paused (or even plaing), not stopped
          getAudioFocus();
          mediaPlayer.start();
          return new NanoHTTPD.Response (Response.Status.OK, "text/plain", 
            makeJSONStatusResponse (0));
          }
    else
          return playFromStartOfPlaylist();
    }


  /** Play the specified item, or return a JSON-format error response 
      if that is not possible. The current playlst index and current
      URI are set. */
  NanoHTTPD.Response playInPlaylist (int index)
    {
    if (playlist.size() == 0)
      return new NanoHTTPD.Response (Response.Status.OK, "text/plain", 
        makeJSONStatusResponse 
         (Errors.ERR_PL_EMPTY, Errors.perror (Errors.ERR_PL_EMPTY)));

    if (index < 0 || index >= playlist.size())
      return new NanoHTTPD.Response (Response.Status.OK, "text/plain", 
        makeJSONStatusResponse 
         (Errors.ERR_PL_RANGE, Errors.perror (Errors.ERR_PL_RANGE)));

    String uri = playlist.get (index).uri;
    currentPlaylistIndex = index;
    try
      {
      playFileNow (uri);
      return new NanoHTTPD.Response (Response.Status.OK, "text/plain", 
        makeJSONStatusResponse (0));
      }
    catch (Exception e) 
      {
      return new NanoHTTPD.Response (Response.Status.OK, "text/plain", 
        makeJSONStatusResponse (e));
      }
    }


  /** Stops playback. */
  NanoHTTPD.Response stopPlayback()
    {
    mediaPlayer.reset();
    releaseAudioFocus();
    currentPlaybackUri = null;
    currentPlaybackTrackInfo = null; 
    return new NanoHTTPD.Response (Response.Status.OK, "text/plain", 
      makeJSONStatusResponse (0));
    }

  /** Adds the whole album to the playlist, clearing the old playlist, 
 *    and starts it. */
  NanoHTTPD.Response playAlbumNow (String album)
    {
    List<String> albumURIs = audioDatabase.getAlbumURIs (context, album);
    int count = 0;
    clearPlaylist();
    for (String uri : albumURIs)
      {
      TrackInfo ti = audioDatabase.getTrackInfo (context, uri);
      playlist.add (ti);
      count++;
      }
 
    playFromStartOfPlaylist();

    return new NanoHTTPD.Response (Response.Status.OK, "text/plain", 
      makeJSONStatusResponse (0, "Put " + count + " item(s) in playlist"));
    }


  /** Adds the whole album to the playlist, preserving the old playlist. */
  NanoHTTPD.Response addAlbumToPlaylist (String album)
    {
    List<String> albumURIs = audioDatabase.getAlbumURIs (context, album);
    int count = 0;
    for (String uri : albumURIs)
      {
      TrackInfo ti = audioDatabase.getTrackInfo (context, uri);
      playlist.add (ti);
      count++;
      }
 
    return new NanoHTTPD.Response (Response.Status.OK, "text/plain", 
      makeJSONStatusResponse (0, "Added " + count + " item(s) to playlist"));
    }


  /** Play from item zero, or return a JSON-format error response 
      if that is not possible. The current playlst index and current
      URI are set. */
  NanoHTTPD.Response playFromStartOfPlaylist()
    {
    return playInPlaylist (0);
    }


  /** Play next item in playlist, or return a JSON-format error response 
      if that is not possible. The current playlst index and current
      URI are set. */
  NanoHTTPD.Response playNextInPlaylist()
    {
    if (playlist.size() == 0)
      return new NanoHTTPD.Response (Response.Status.OK, "text/plain", 
        makeJSONStatusResponse 
         (Errors.ERR_PL_EMPTY, Errors.perror (Errors.ERR_PL_EMPTY)));
    
    currentPlaylistIndex++;

    if (currentPlaylistIndex >= playlist.size())
      {
      currentPlaylistIndex = playlist.size();
      return new NanoHTTPD.Response (Response.Status.OK, "text/plain", 
        makeJSONStatusResponse 
         (Errors.ERR_PL_RANGE, Errors.perror (Errors.ERR_PL_RANGE)));
      }

    return playInPlaylist (currentPlaylistIndex);
    }


  /** Play previous item in playlist, or return a JSON-format error response 
      if that is not possible. The current playlst index and current
      URI are set. */
  NanoHTTPD.Response playPrevInPlaylist()
    {
    if (playlist.size() == 0)
      return new NanoHTTPD.Response (Response.Status.OK, "text/plain", 
        makeJSONStatusResponse 
         (Errors.ERR_PL_EMPTY, Errors.perror (Errors.ERR_PL_EMPTY)));
    
    currentPlaylistIndex--;

    if (currentPlaylistIndex < 0) 
      {
      currentPlaylistIndex = 0;
      return new NanoHTTPD.Response (Response.Status.OK, "text/plain", 
        makeJSONStatusResponse 
         (Errors.ERR_PL_RANGE, Errors.perror (Errors.ERR_PL_RANGE)));
      }

    return playInPlaylist (currentPlaylistIndex);
    }

  /** Clear the playlist. This method always returns a JSON-format
      success code. */
  NanoHTTPD.Response clearPlaylist()
    {
    mediaPlayer.reset();
    releaseAudioFocus();
    playlist = new Vector<TrackInfo>();
    currentPlaybackUri = null;
    currentPlaybackTrackInfo = null;
    return new NanoHTTPD.Response (Response.Status.OK, "text/plain", 
        makeJSONStatusResponse 
         (0));
    }

  /** Play a random album. */ 
  NanoHTTPD.Response playRandomAlbum()
    {
    Set<String> albums = audioDatabase.getAlbums();
    int size = albums.size();
    String album = "";
    int item = new Random().nextInt(size); 
    int i = 0;
    for (String cand: albums)
      {
      i++;
      if (i == item)
        {
        album = cand;
        break;
        }
      }

    playAlbumNow (album);
    return new NanoHTTPD.Response (Response.Status.OK, "text/plain", 
        makeJSONStatusResponse 
         (0, "Playing album '" + album + "'"));
    }


  /** Pause. */
  NanoHTTPD.Response pause()
    {
    mediaPlayer.pause();
    return new NanoHTTPD.Response (Response.Status.OK, "text/plain", 
            makeJSONStatusResponse (0));
    }

  /** Volume up. */
  NanoHTTPD.Response volumeUp()
    {
    AudioManager audioManager = (AudioManager) 
      context.getSystemService (Context.AUDIO_SERVICE);
    audioManager.adjustStreamVolume 
      (AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, 0);
    return new NanoHTTPD.Response (Response.Status.OK, "text/plain", 
        makeJSONStatusResponse 
         (0));
    }

  /** Volume dow */
  NanoHTTPD.Response volumeDown()
    {
    AudioManager audioManager = (AudioManager) 
      context.getSystemService (Context.AUDIO_SERVICE);
    audioManager.adjustStreamVolume 
      (AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0);
    return new NanoHTTPD.Response (Response.Status.OK, "text/plain", 
        makeJSONStatusResponse 
         (0));
    }

  /** Rescan the Android media catalog */
  NanoHTTPD.Response rescanCatalog ()
    {
    audioDatabase.scan(context);
    return new NanoHTTPD.Response (Response.Status.OK, "text/plain", 
        makeJSONStatusResponse 
         (0, "Rescan complete"));
    }

  /** Rescan the whole filesystem */
  NanoHTTPD.Response rescanFilesystem ()
    {
    context.sendBroadcast 
      (new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, 
         android.net.Uri.parse
            ("file://" + Environment.getExternalStorageDirectory())));
    return new NanoHTTPD.Response (Response.Status.OK, "text/plain", 
        makeJSONStatusResponse 
         (0, "Rescan initiated"));
    }


  /** Invoked by the Android player when playback of an item is complete.
      Try to advance to the next playlist item. */
  public void onCompletion (MediaPlayer mp)
    {
    currentPlaybackUri = null; // set current URI so it' s clear we
                               // aren't just paused
    currentPlaybackTrackInfo = null; 
    releaseAudioFocus();
    Log.w ("AMP", "Playback completed: " + currentPlaybackUri);
    if (playlist.size() > 0)
      {
      playNextInPlaylist();
      }
    }


  public void getAudioFocus()
    {
    AudioManager am = (AudioManager) context.getSystemService
      (Context.AUDIO_SERVICE);
    am.requestAudioFocus(this, AudioManager.STREAM_MUSIC, 
      AudioManager.AUDIOFOCUS_GAIN);
    am.registerMediaButtonEventReceiver (new ComponentName 
      (context.getPackageName(), RemoteControlReceiver.class.getName()));
    }


  public void releaseAudioFocus()
    {
    AudioManager am = (AudioManager) context.getSystemService
      (Context.AUDIO_SERVICE);
    am.abandonAudioFocus (this);
    am.unregisterMediaButtonEventReceiver (new ComponentName 
      (context.getPackageName(), RemoteControlReceiver.class.getName()));
    }


  @Override
  public void onAudioFocusChange (int focusChange)
    {
    }

  /** Stop the Android audio player before shutting down. */
  @Override
  public void stop()
    {
    stopPlayback();
    super.stop();
    }

}


