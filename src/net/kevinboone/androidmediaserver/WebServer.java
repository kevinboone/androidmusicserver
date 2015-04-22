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
import android.content.res.*;
import android.graphics.*;
import android.media.MediaPlayer;
import android.util.Log;
import android.media.*;
import android.media.audiofx.*;
import net.kevinboone.androidmusicplayer.Player;
import net.kevinboone.androidmusicplayer.PlayerException;
import net.kevinboone.androidmusicplayer.TrackInfo;
import net.kevinboone.androidmusicplayer.Errors;
import net.kevinboone.androidmusicplayer.SearchSpec;
import net.kevinboone.textutils.*;

public class WebServer extends NanoHTTPD 
{
protected static String DOCROOT="/";
private Context context = null;
private String lastModifiedString = null; 
private Date lastModifiedDate = null; 
private Player player;
private final String htmlTemplate;

public WebServer (Context context)
  {
  super (Main.port);
  this.context = context;
  player = new Player (context);
  htmlTemplate = getHtmlTemplate ();
  setLastModifiedToNow();
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
      if (uri.indexOf ("gui_albums_by_composer") == 0)
        return handleGuiAlbumsByComposer (parameters);
      if (uri.indexOf ("/gui_albums_by_composer") == 0)
        return handleGuiAlbumsByComposer (parameters);
      if (uri.indexOf ("gui_albums_by_artist") == 0)
        return handleGuiAlbumsByArtist (parameters);
      if (uri.indexOf ("/gui_albums_by_artist") == 0)
        return handleGuiAlbumsByArtist (parameters);
      if (uri.indexOf ("gui_tracks_by_album") == 0)
        return handleGuiTracksByAlbum (parameters);
      if (uri.indexOf ("/gui_tracks_by_album") == 0)
        return handleGuiTracksByAlbum (parameters);
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
      if (uri.indexOf ("gui_tracks") == 0)
        return handleGuiTracks (parameters);
      if (uri.indexOf ("/gui_tracks") == 0)
        return handleGuiTracks (parameters);
      if (uri.indexOf ("gui_composers") == 0)
        return handleGuiComposers (parameters);
      if (uri.indexOf ("/gui_composers") == 0)
        return handleGuiComposers (parameters);
      if (uri.indexOf ("gui_artists") == 0)
        return handleGuiArtists (parameters);
      if (uri.indexOf ("/gui_artists") == 0)
        return handleGuiArtists (parameters);
      if (uri.indexOf ("gui_search") == 0)
        return handleGuiSearch (parameters);
      if (uri.indexOf ("/gui_search") == 0)
        return handleGuiSearch (parameters);
      if (uri.indexOf ("/gui_eq") == 0)
        return handleGuiEq (parameters);
      if (uri.indexOf ("gui_eq") == 0)
        return handleGuiEq (parameters);
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

    Set<String> albums = player.getAlbums();
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

    Set<String> genres = player.getGenres();
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
  String makeArtistListFromSet (Map<String,String> parameters, 
      boolean covers, Set<String> artists)
    {
    StringBuffer sb = new StringBuffer();
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
    Formats a list of artists, maintained by MediaDatabase
  */
  String makeArtistList (Map<String,String> parameters, boolean covers)
    {
    StringBuffer sb = new StringBuffer();
    sb.append ("<span class=\"pagetitle\">" + "Artists" + "</span><p/>");
    sb.append ("<table>");

    Set<String> artists = player.getArtists();
    sb.append (makeArtistListFromSet (parameters, covers, artists));
    return new String (sb);
    }

  
  /**
    Formats a list of composers, maintained by MediaDatabase
  */
  String makeComposerListFromSet (Map<String,String> parameters, 
      boolean covers, Set<String> composers)
    {
    StringBuffer sb = new StringBuffer();
    for (String composer: composers)
      {
      sb.append ("<tr>");
      sb.append ("<td valign=\"top\">");
// Nothing in the image slot yet
      sb.append ("</td>");
      sb.append ("<td valign=\"top\">");
      sb.append (" <a href=\"/gui_albums_by_composer?composer=" 
              + URLEncoder.encode (composer) + "&" + 
                makeGenParams (parameters) + 
                "\"><span>" + composer + "</span></a> ");
      sb.append ("</td>");
      sb.append ("</tr>");
      }
    sb.append ("</table>\n");
    return new String (sb);
    }

  
  /**
    Formats a list of composers, maintained by MediaDatabase
  */
  String makeComposerList (Map<String,String> parameters, boolean covers)
    {
    StringBuffer sb = new StringBuffer();
    sb.append ("<span class=\"pagetitle\">" + "Composers" + "</span><p/>");
    sb.append ("<table>");

    Set<String> composers = player.getComposers();
    sb.append (makeComposerListFromSet (parameters, covers, composers));
    return new String (sb);
    }

  
  
  
  
  /**
    Format a list of tracks matching the specified album.
  */
  String makeTracksByAlbum (String album, boolean covers)
    {
    StringBuffer sb = new StringBuffer();

    sb.append ("<span class=\"pagetitle\">" + album + "</span><p/>");

    List<String> uris = player.getAlbumTrackUris (album);
    
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
      TrackInfo ti = player.getTrackInfo (uri);
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

    Set<String> albums = player.getAlbumsByGenre (genre);
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

    sb.append ("<span class=\"pagetitle\">Albums including artist '" 
     + artist + "'</span><p/>");

    Set<String> albums = player.getAlbumsByArtist (artist);
    sb.append (makeAlbumListFromSet (parameters, covers, albums));

    return new String (sb);
    }


  /**
    Format a list of albums matching the specified composer.
  */
  String makeAlbumsByComposer (Map<String,String> parameters, 
      String composer, boolean covers)
    {
    StringBuffer sb = new StringBuffer();

    sb.append ("<span class=\"pagetitle\">Albums including composer '" 
     + composer + "'</span><p/>");

    Set<String> albums = player.getAlbumsByComposer (composer);
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
          if (Player.isPlayableFile (list[i]))
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
      throws PlayerException 
    {
    player.playFileNow (uri);
    }


  /**
  Adds the specified filesystem item, which might be a directory, 
  and is relative to the DOCROOT, to the playlist,
  and return the number of items added.
  */
  int addToPlaylist (String uri)
      throws PlayerException 
    {
    return player.addFileOrDirectoryToPlaylist (DOCROOT + "/" + uri);
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
      List<String> trackUris = player.getAlbumTrackUris (album);
      for (String uri : trackUris)
        {
        if (filePath == null)
          filePath = player.getFilePathFromContentUri 
            (android.net.Uri.parse(uri));
        ep = player.getEmbeddedPictureForTrackUri (uri);
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
        return stopPlayback();
        }
      else if ("pause".equalsIgnoreCase (cmd))
        {
        return pause();
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
      else if (cmd.toLowerCase().startsWith ("set_eq_level"))
        {
        String arg = cmd.substring (13); 
        int p = arg.indexOf (','); 
        String sBand = arg.substring (0, p);
        String sLevel = arg.substring (p+1);
        int band = Integer.parseInt (sBand);
        int level = Integer.parseInt (sLevel);
        return setEqLevel (band, level);
        }
      else if (cmd.toLowerCase().startsWith ("set_bb_level"))
        {
        String arg = cmd.substring (13); 
        int level = Integer.parseInt (arg);
        return setBBLevel (level);
        }
      else if (cmd.toLowerCase().startsWith ("set_vol_level"))
        {
        String arg = cmd.substring (14); 
        int level = Integer.parseInt (arg);
        return setVolLevel (level);
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
      else if ("shuffle_playlist".equalsIgnoreCase (cmd))
        {
        return shufflePlaylist();
        }
      else if ("random_album".equalsIgnoreCase (cmd))
        {
        return playRandomAlbum();
        }
      else if ("prev".equalsIgnoreCase (cmd))
        {
        return playPrevInPlaylist();
        }
      else if ("enable_bass_boost".equalsIgnoreCase (cmd))
        {
        return enableBassBoost();
        }
      else if ("disable_bass_boost".equalsIgnoreCase (cmd))
        {
        return disableBassBoost();
        }
      else if ("enable_eq".equalsIgnoreCase (cmd))
        {
        return enableEq();
        }
      else if ("disable_eq".equalsIgnoreCase (cmd))
        {
        return disableEq();
        }
      else if ("volume_down".equalsIgnoreCase (cmd))
        {
        return volumeDown();
        }
      else if ("volume_up".equalsIgnoreCase (cmd))
        {
        return volumeUp();
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
  Returns an embedded binary object with the appropriate MIME type,
  from the assets/docroot directory
  */
  NanoHTTPD.Response serveResource (String resource)
    {
    String assetFile = "docroot/" + resource;
    int p = assetFile.lastIndexOf ('_');
    if (p > 0)
      assetFile = assetFile.substring (0, p) + "." + assetFile.substring (p+1);
    try
      {
      AssetManager am = context.getAssets();
      InputStream is = am.open (assetFile); 
      String mimeType = FileUtils.getMimeType (resource);
      return new NanoHTTPD.Response (Response.Status.OK, mimeType, is);
      }
    catch (IOException e)
      {
      return new NanoHTTPD.Response (Response.Status.INTERNAL_ERROR, 
        "text/plain", e.toString());
      }
    }


  protected String getHtmlTemplate ()
    {
    try
      {
      InputStream is = context.getResources().openRawResource 
        (R.raw.template);
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
    if (player.isPlaying())
      {
      TrackInfo currentPlaybackTrackInfo = 
        player.getCurrentPlaybackTrackInfo();
      transport = "playing";
      if (currentPlaybackTrackInfo != null)
        {
        title = currentPlaybackTrackInfo.title;
        album = currentPlaybackTrackInfo.album;
        artist = currentPlaybackTrackInfo.artist;
        }
      uri = player.getCurrentPlaybackUri();
      duration = "" + player.getCurrentPlaybackDurationMsec();
      position = "" + player.getCurrentPlaybackPositionMsec();
      }
    else if (player.getCurrentPlaybackUri() != null)
      {
      transport = "paused";
      TrackInfo currentPlaybackTrackInfo = 
        player.getCurrentPlaybackTrackInfo();
      if (currentPlaybackTrackInfo != null)
        {
        title = currentPlaybackTrackInfo.title;
        album = currentPlaybackTrackInfo.album;
        artist = currentPlaybackTrackInfo.artist;
        }
      uri = player.getCurrentPlaybackUri();
      duration = "" + player.getCurrentPlaybackDurationMsec();
      position = "" + player.getCurrentPlaybackPositionMsec();
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
 *   Put the HTML header and footer around the bdody text
 **/
  protected String wrapHtml (String body, Map<String,String> parameters)
    {
Log.w ("XXX", "HELLO");
Log.w ("XXX", "HTML=" + htmlTemplate);
    return htmlTemplate.replace ("%%BODY%%", body);  
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

    String answer = makeTracksByAlbum (album, covers);
    answer += makeControls(parameters);
    answer = wrapHtml (answer, parameters);
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

    String answer = makeAlbumsByGenre (parameters, genre, covers);
    answer += makeControls(parameters);
    answer = wrapHtml (answer, parameters);
    return new NanoHTTPD.Response (answer);
    }


  /**
    Make the search results page.
  */
  protected Response handleGuiSearch (Map<String,String> parameters)
    {
    String search = parameters.get("search");
    if (search == null)
      search = ""; // Prevent a crash

    String answer = makeSearchResults (parameters, search, false); // FRIG -- voers
    answer += makeControls(parameters);
    answer = wrapHtml (answer, parameters);
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

    String answer = makeAlbumsByArtist (parameters, artist, covers);
    answer += makeControls(parameters);
    answer = wrapHtml (answer, parameters);
    return new NanoHTTPD.Response (answer);
    }


  /**
    Make the album-list-by-composer page for the "composer" 
    specified in the request.
  */
  protected Response handleGuiAlbumsByComposer (Map<String,String> parameters)
    {
    String composer = parameters.get("composer");
    if (composer == null)
      composer = ""; // Prevent a crash

    boolean covers = false;
    if ("true".equals (parameters.get("covers")))
      covers = true;

    String answer = makeAlbumsByComposer (parameters, composer, covers);
    answer += makeControls(parameters);
    answer = wrapHtml (answer, parameters);
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

    String answer = makeDirList (path, parameters); 
    answer += makeControls(parameters);
    answer = wrapHtml (answer, parameters);
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
    String answer = makeAlbumList (parameters, covers); 
    answer += makeControls(parameters);
    answer = wrapHtml (answer, parameters);
    return new NanoHTTPD.Response (answer);
    }


  /**
    Make the track list page. 
  */
  protected Response handleGuiTracks (Map<String,String> parameters)
    {
    boolean covers = false;
    if ("true".equals (parameters.get("covers")))
      covers = true;
    int start = 0;
    String sStart = parameters.get("start");
    if (sStart != null && sStart.length() > 0)
      start = Integer.parseInt (sStart);
    String answer = makeTrackList (parameters, covers, start, null); //TOD search 
    answer += makeControls(parameters);
    answer = wrapHtml (answer, parameters);
    return new NanoHTTPD.Response (answer);
    }


  protected Response handleGuiEq (Map<String,String> parameters)
    {
    String answer = makeEq (parameters); 
    answer += makeControls(parameters);
    answer = wrapHtml (answer, parameters);
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
    String answer = makeGenreList (parameters, covers); 
    answer += makeControls(parameters);
    answer = wrapHtml (answer, parameters);
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
    String answer = makeArtistList (parameters, covers); 
    answer += makeControls(parameters);
    answer = wrapHtml (answer, parameters);
    return new NanoHTTPD.Response (answer);
    }


  /**
    Make the composer list page. 
  */
  protected Response handleGuiComposers (Map<String,String> parameters)
    {
    boolean covers = false;
    if ("true".equals (parameters.get("covers")))
      covers = true;
    String answer = makeComposerList (parameters, covers); 
    answer += makeControls(parameters);
    answer = wrapHtml (answer, parameters);
    return new NanoHTTPD.Response (answer);
    }


  protected String makeTrackListFromSetOfUris (Map<String, String> parameters, 
      boolean covers, Set<String> trackUris)
    {
    StringBuffer sb = new StringBuffer();

    for (String trackUri : trackUris)
        {
        TrackInfo ti = player.getTrackInfo (trackUri);
        sb.append ("<table callspacing=\"0\" cellpadding=\"5\">");
        sb.append ("<tr>");
        sb.append ("<td valign=\"top\">");
        if (covers)
          sb.append ("<img width=\"64\" src=\"/cover?album=" 
           + URLEncoder.encode (ti.album) + "\"/>"); 
        sb.append ("</td>");
        sb.append ("<td valign=\"top\">");
        sb.append (" <a href=\"javascript:play_file_now('" 
              + EscapeUtils.escapeJSON (trackUri) + 
                "')\"><span class=\"textbuttonspan\">Play now</span></a> ");
        sb.append (" <a href=\"javascript:add_to_playlist('" 
              + EscapeUtils.escapeJSON (trackUri) + 
                "')\"><span class=\"textbuttonspan\">Add</span></a> ");
        sb.append ("<br/>");
        sb.append (ti.title);
        sb.append ("<br/>");
        sb.append (ti.album);
        sb.append (" | ");
        sb.append (ti.artist);
        sb.append ("</td>");
        sb.append ("</tr>");
        sb.append ("</table>\n");
        }

    return new String (sb);
    }

  protected String makeTrackList (Map<String, String> parameters, 
      boolean covers, int start, String search)
    {
    // TODO add search
    StringBuffer sb = new StringBuffer();

    sb.append 
     ("<span class=\"pagesubtitle\">Tracks</span><br/>");
    sb.append ("<p/>\n");

    if (start != 0)
      {
      String prevUrl = "/gui_tracks?start=" + "0" 
       + "&" + makeGenParams (parameters);
      sb.append ("<a href=\"" + prevUrl 
       + "\"><span class=\"textbuttonspan\">First</span> </a>");
      }    

    if (start >= Main.tracksPerPage)
      {
      String prevUrl = "/gui_tracks?start=" + (start - Main.tracksPerPage) 
       + "&" + makeGenParams (parameters);
      sb.append ("<a href=\"" + prevUrl 
       + "\"><span class=\"textbuttonspan\">Previous</span> </a>");
      }    

    int approxNumTracks = player.getApproxNumTracks();
    Set<String> trackUris = player.findTracks (null, start, Main.tracksPerPage);
    if (trackUris.size() == 0)
      {
      sb.append ("<p/>No more tracks<p/>");
      }
    else
      {
      String nextUrl = "/gui_tracks?start=" + (start + Main.tracksPerPage) 
       + "&" + makeGenParams (parameters);
      sb.append ("<a href=\"" + nextUrl + 
       "\"><span class=\"textbuttonspan\">Next</span></a>");
      sb.append ("<p/>\n");

      int lastTrackNum = start + 1 + trackUris.size();
      if (lastTrackNum > approxNumTracks) lastTrackNum = approxNumTracks;

      sb.append ("<i>Tracks " + (start + 1) + "-" + lastTrackNum 
         + " of " 
          + approxNumTracks + "</i><p/>\n");
  
      sb.append (makeTrackListFromSetOfUris (parameters, covers, trackUris));
      }

    return new String (sb);
    }


  protected String makeSearchResults (Map<String,String> parameters, 
      String search, boolean covers)
    {
    StringBuffer sb = new StringBuffer();

    sb.append 
     ("<span class=\"pagesubtitle\">Search results</span><br/>");
    sb.append ("<p/>\n");

    if (search.length() == 0)
      {
      sb.append ("Search text cannot be empty.\n");
      }
    else
      {
      sb.append 
       ("<span class=\"pagesubsubtitle\">Album results for '" + search 
        +  "'</span><br/>");

      SearchSpec ss = new SearchSpec (search);
      Set<String> albums = player.getMatchingAlbums 
        (ss, Main.maxSearchResults); 
      
      if (albums.size() > 0)
        sb.append (makeAlbumListFromSet (parameters, covers, albums));
      else
        sb.append ("No matches\n");

      sb.append ("<p/>\n");

      sb.append 
       ("<span class=\"pagesubsubtitle\">Artist results for '" + search 
        +  "'</span><br/>");

      Set<String> artists = player.getMatchingArtists
        (ss, Main.maxSearchResults); 

      if (artists.size() > 0)
        sb.append (makeArtistListFromSet (parameters, covers, artists));
      else
        sb.append ("No matches\n");

      sb.append ("<p/>\n");

      sb.append 
       ("<span class=\"pagesubsubtitle\">Composer results for '" + search 
        +  "'</span><br/>");

      Set<String> composers = player.getMatchingComposers
        (ss, Main.maxSearchResults); 

      if (composers.size() > 0)
        sb.append (makeComposerListFromSet (parameters, covers, composers));
      else
        sb.append ("No matches\n");

      sb.append ("<p/>\n");

      sb.append 
       ("<span class=\"pagesubsubtitle\">Track results for '" + search 
        +  "'</span><br/>");

      Set<String> tracks = player.findTracks 
        (ss, 0, Main.maxSearchResults); 

      if (tracks.size() > 0)
        sb.append (makeTrackListFromSetOfUris (parameters, covers, tracks));
      else
        sb.append ("No matches\n");

      sb.append ("<p/>\n");
      }

    return new String (sb);
    }


  protected String makeEq (Map<String,String> parameters)
    {
    StringBuffer sb = new StringBuffer();

    sb.append 
     ("<span class=\"pagesubtitle\">EQ</span><br/>");
    sb.append ("<p/>\n");

    boolean enabled = player.getEqEnabled();
    if (enabled)
      {
      sb.append 
        //("<input type=\"checkbox\" name=\"eq_enabled\" checked=\"checked\" onclick='onClickEqEnabled(this);window.location.reload(true);'/> Enabled <br/>\n");
        ("<input type=\"checkbox\" name=\"eq_enabled\" checked=\"checked\" onclick='onClickEqEnabled(this);delay_and_refresh();'/> Enabled <br/>\n");

      sb.append ("<p/>\n");

      int numBands = player.getEqNumberOfBands();
      int minLevel = player.getEqMinLevel(); 
      int maxLevel = player.getEqMaxLevel();
      for (int i = 0; i < numBands && i < player.MAX_EQ_BANDS; i++)
        {
        int level = player.getEqBandLevel (i); 
        String sFreqRange = player.getEqBandFreqRange (i);

        sb.append (sFreqRange);
        sb.append ("<br/>\n");
        sb.append ("<div class=\"eqslider\">\n");
        sb.append ("<input id=\"eqslider" + i + "\" type=\"range\" min=\"" + 
          minLevel + "\" max=\"" + maxLevel + "\" step=\"1\" value=\"" + 
            level + "\" onchange=\"onChangeEqSlider(" + i + ",this.value)\"/>\n");
        sb.append ("</div>\n");
        sb.append ("<p/>\n");
        }
      }
   else
      {
      sb.append 
        //("<input type=\"checkbox\" name=\"eq_enabled\" onclick='onClickEqEnabled(this);window.location.reload(true);'/> Enabled <br/>\n");
        ("<input type=\"checkbox\" name=\"eq_enabled\" onclick='onClickEqEnabled(this);delay_and_refresh();'/> Enabled <br/>\n");
      }

    sb.append ("<p/>\n");
    sb.append 
     ("<span class=\"pagesubtitle\">Bass boost</span><br/>");
    sb.append ("<p/>\n");

    boolean bbEnabled = player.getBBEnabled();
    if (bbEnabled)
      {
      int bbStrength = player.getBBStrength(); 
      sb.append  ("<input type=\"checkbox\" name=\"bb_enabled\" checked=\"checked\" onclick='onClickBBEnabled(this);delay_and_refresh();'/> Enabled <br/>\n");

      sb.append ("<p/>\n");
      sb.append ("Bass boost");
      sb.append ("<br/>\n");
      
      sb.append ("<div class=\"bbslider\">\n");
        sb.append ("<input id=\"bbslider\" type=\"range\" min=\"" + 
          "0" + "\" max=\"" + "1000" + "\" step=\"1\" value=\"" + 
            bbStrength + "\" onchange=\"onChangeBBSlider(this.value)\"/>\n");
        sb.append ("</div>\n");

      sb.append ("<p/>\n");
      } 
    else
      {
      sb.append 
        ("<input type=\"checkbox\" name=\"bb_enabled\" onclick='onClickBBEnabled(this);delay_and_refresh();'/> Enabled <br/>\n");
      sb.append ("<p/>\n");
      }

    AudioManager audioManager = (AudioManager) 
      context.getSystemService (Context.AUDIO_SERVICE);
    int maxVol = audioManager.getStreamMaxVolume 
      (AudioManager.STREAM_MUSIC);
    int vol = audioManager.getStreamVolume 
      (AudioManager.STREAM_MUSIC);

    sb.append 
     ("<span class=\"pagesubtitle\">Volume</span><br/>");
    sb.append ("<p/>\n");

    sb.append ("<div class=\"volslider\">\n");
    sb.append ("<input id=\"volslider\" type=\"range\" min=\"" + 
          "0" + "\" max=\"" + maxVol + "\" step=\"1\" value=\"" + 
            vol + "\" onchange=\"onChangeVolSlider(this.value)\"/>\n");
    sb.append ("</div>\n");

    sb.append ("<p/>\n");

    return new String (sb);
    }

  
  protected String makeHomePage (Map<String,String> parameters)
    {
    StringBuffer sb = new StringBuffer();

    sb.append 
     ("<span class=\"pagesubtitle\">Browse with covers</span><br/>");
    sb.append ("&nbsp;&nbsp;<a href=\"/gui_albums?covers=true\">Browse all albums</a><br/>");
    sb.append ("&nbsp;&nbsp;<a href=\"/gui_genres?covers=true\">Browse albums by genre</a><br/>");
    sb.append ("&nbsp;&nbsp;<a href=\"/gui_artists?covers=true\">Browse albums by artist</a><br/>");
    sb.append ("&nbsp;&nbsp;<a href=\"/gui_composers?covers=true\">Browse albums by composer</a><br/>");
    sb.append ("&nbsp;&nbsp;<a href=\"/gui_tracks?covers=true\">Browse tracks</a><br/>");
    sb.append ("<p/>");

    sb.append 
     ("<span class=\"pagesubtitle\">Browse without covers</span><br/>");
    sb.append ("&nbsp;&nbsp;<a href=\"/gui_albums?covers=false\">Browse all albums</a><br/>");
    sb.append ("&nbsp;&nbsp;<a href=\"/gui_genres?covers=false\">Browse albums by genre</a><br/>");
    sb.append ("&nbsp;&nbsp;<a href=\"/gui_artists?covers=false\">Browse albums by artist</a><br/>");
    sb.append ("&nbsp;&nbsp;<a href=\"/gui_composers?covers=false\">Browse albums by composer</a><br/>");
    sb.append ("&nbsp;&nbsp;<a href=\"/gui_tracks?covers=false\">Browse tracks</a><br/>");
    sb.append ("<p/>");

    sb.append 
     ("<span class=\"pagesubtitle\">Browse files</span><br/>");
    sb.append ("&nbsp;&nbsp;<a href=\"/gui_files\">Browse whole filesystem</a><br/>");
    sb.append ("<p/>");

    sb.append 
     ("<span class=\"pagesubtitle\">Playlist</span><br/>");
    sb.append ("&nbsp;&nbsp;<a href=\"javascript:random_album()\">Play a randomly-selected album</a><br/>");
    sb.append ("&nbsp;&nbsp;<a href=\"/gui_playlist\">View the current playlist</a><br/>");
    sb.append ("&nbsp;&nbsp;<a href=\"javascript:clear_playlist()\">Clear the playlist</a><br/>");
    sb.append ("&nbsp;&nbsp;<a href=\"javascript:shuffle_playlist()\">Shuffle the playlist</a>");
    sb.append ("<p/>");

    sb.append 
     ("<span class=\"pagesubtitle\">Audio</span><br/>");
    sb.append ("&nbsp;&nbsp;<a href=\"/gui_eq\">Equalizer, etc</a><br/>");
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
    String answer = "<span class=\"pagetitle\">Main index</span><p/>"; 
    answer += makeHomePage(parameters);
    answer += makeControls(parameters);
    answer = wrapHtml (answer, parameters);
    return new NanoHTTPD.Response (answer);
    }


  /*
    Make the playlist page 
  */
  protected Response handleGuiPlaylist (Map<String,String> parameters)
    {
    String answer = 
      "<span class=\"pagetitle\">Playlist</span><p/>"; 
    if (player.getPlaylist().size() == 0)
      {
      answer += "<i>Playlist is empty</i>"; 
      }
    else
      {
      for (TrackInfo ti : player.getPlaylist())
        {
        answer += ti.title; 
        answer += "<br/>"; 
        }
      answer += "<p/>\n";
      answer += 
       "<a href=\"javascript:shuffle_playlist();delay_and_refresh()\">[Shuffle]</a> | ";
      answer += 
       "<a href=\"javascript:clear_playlist();delay_and_refresh()\">[Clear]</a>";
      }
    answer += "<p/>\n";
  
    answer += makeControls(parameters);
    answer = wrapHtml (answer, parameters);
    return new NanoHTTPD.Response (answer);
    }


  /** Handle play request, but playing either the playlist, or
      unpausing if we are paused. */
  NanoHTTPD.Response play()
    {
    try
      {
      player.play ();
      return new NanoHTTPD.Response (Response.Status.OK, "text/plain", 
        makeJSONStatusResponse (0));
      }
    catch (PlayerException e)
      {
      int code = e.getCode();
      return new NanoHTTPD.Response (Response.Status.OK, "text/plain", 
        makeJSONStatusResponse (code, e.getMessage()));
      }
    }


  /** Play the specified item, or return a JSON-format error response 
      if that is not possible. The current playlst index and current
      URI are set. */
  NanoHTTPD.Response playInPlaylist (int index)
    {
    try
      {
      player.playInPlaylist (index);
      return new NanoHTTPD.Response (Response.Status.OK, "text/plain", 
        makeJSONStatusResponse (0));
      }
    catch (PlayerException e)
      {
      int code = e.getCode();
      return new NanoHTTPD.Response (Response.Status.OK, "text/plain", 
        makeJSONStatusResponse (code, e.getMessage()));
      }
    }


  /** Stops playback. */
  NanoHTTPD.Response stopPlayback()
    {
    player.stop();
    return new NanoHTTPD.Response (Response.Status.OK, "text/plain", 
      makeJSONStatusResponse (0));
    }


  NanoHTTPD.Response setEqLevel (int band, int level)
    {
    player.setEqBandLevel (band, level);
    return new NanoHTTPD.Response (Response.Status.OK, "text/plain", 
      makeJSONStatusResponse (0));
    }


  NanoHTTPD.Response setBBLevel (int level)
    {
    player.setBBStrength (level);
    return new NanoHTTPD.Response (Response.Status.OK, "text/plain", 
      makeJSONStatusResponse (0));
    }


  NanoHTTPD.Response setVolLevel (int level)
    {
    AudioManager audioManager = (AudioManager) 
      context.getSystemService (Context.AUDIO_SERVICE);
    audioManager.setStreamVolume (AudioManager.STREAM_MUSIC,  level, 0);
    return new NanoHTTPD.Response (Response.Status.OK, "text/plain", 
      makeJSONStatusResponse (0));
    }


  /** Adds the whole album to the playlist, clearing the old playlist, 
 *    and starts it. */
  NanoHTTPD.Response playAlbumNow (String album)
    {
    try
      {
      int count = player.playAlbumNow (album);
      return new NanoHTTPD.Response (Response.Status.OK, "text/plain", 
        makeJSONStatusResponse (0, "Put " + count + " item(s) in playlist"));
      }
    catch (PlayerException e)
      {
      int code = e.getCode();
      return new NanoHTTPD.Response (Response.Status.OK, "text/plain", 
        makeJSONStatusResponse (code, e.getMessage()));
      }
    }


  /** Adds the whole album to the playlist, preserving the old playlist. */
  NanoHTTPD.Response addAlbumToPlaylist (String album)
    {
    try
      {
      int count = player.addAlbumToPlaylist (album);
      return new NanoHTTPD.Response (Response.Status.OK, "text/plain", 
        makeJSONStatusResponse (0, "Added " + count + " item(s) to playlist"));
      }
    catch (PlayerException e)
      {
      int code = e.getCode();
      return new NanoHTTPD.Response (Response.Status.OK, "text/plain", 
        makeJSONStatusResponse (code, e.getMessage()));
      }
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
    try
      {
      player.movePlaylistIndexForward();
      player.playCurrentPlaylistItem();
      return new NanoHTTPD.Response (Response.Status.OK, "text/plain", 
        makeJSONStatusResponse (0));
      }
    catch (PlayerException e)
      {
      int code = e.getCode();
      return new NanoHTTPD.Response (Response.Status.OK, "text/plain", 
        makeJSONStatusResponse (code, e.getMessage()));
      }
    }


  /** Play previous item in playlist, or return a JSON-format error response 
      if that is not possible. The current playlst index and current
      URI are set. */
  NanoHTTPD.Response playPrevInPlaylist()
    {
    try
      {
      player.movePlaylistIndexBack();
      player.playCurrentPlaylistItem();
      return new NanoHTTPD.Response (Response.Status.OK, "text/plain", 
        makeJSONStatusResponse (0));
      }
    catch (PlayerException e)
      {
      int code = e.getCode();
      return new NanoHTTPD.Response (Response.Status.OK, "text/plain", 
        makeJSONStatusResponse (code, e.getMessage()));
      }
    }


  /** Shuffle the playlist. */
  NanoHTTPD.Response shufflePlaylist()
    {
    if (player.getPlaylist().size() == 0)
      return new NanoHTTPD.Response (Response.Status.OK, "text/plain", 
        makeJSONStatusResponse 
         (Errors.ERR_PL_EMPTY, Errors.perror (Errors.ERR_PL_EMPTY)));
    
    player.shufflePlaylist();

    return new NanoHTTPD.Response (Response.Status.OK, "text/plain", 
        makeJSONStatusResponse (0));
    }


  /** Clear the playlist. This method always returns a JSON-format
      success code. */
  NanoHTTPD.Response clearPlaylist()
    {
    player.clearPlaylist();
    return new NanoHTTPD.Response (Response.Status.OK, "text/plain", 
        makeJSONStatusResponse 
         (0));
    }


  /** Play a random album. */ 
  NanoHTTPD.Response playRandomAlbum()
    {
    Set<String> albums = player.getAlbums();
    int size = albums.size(); 
    if (size == 0)
      return new NanoHTTPD.Response (Response.Status.OK, "text/plain", 
        makeJSONStatusResponse 
         (Errors.ERR_NO_ALBUMS));

    String album = "";

    int item = new Random().nextInt(size); 
    int i = 0;
    for (String cand: albums)
      {
      i++;
      if (i == item || i == albums.size() - 1)
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
    player.pause();
    return new NanoHTTPD.Response (Response.Status.OK, "text/plain", 
            makeJSONStatusResponse (0));
    }


  /** Enable bass boost. */
  NanoHTTPD.Response enableBassBoost()
    {
    player.setBBEnabled (true);
    return new NanoHTTPD.Response (Response.Status.OK, "text/plain", 
      makeJSONStatusResponse (0, "Bass boost enabled"));
    }


  /** Disable bass boost. */
  NanoHTTPD.Response disableBassBoost()
    {
    player.setBBEnabled (false);
    return new NanoHTTPD.Response (Response.Status.OK, "text/plain", 
      makeJSONStatusResponse (0, "Bass boost disabled"));
    }


  /** Enable EQ. */
  NanoHTTPD.Response enableEq()
    {
    player.setEqEnabled (true);
    return new NanoHTTPD.Response (Response.Status.OK, "text/plain", 
      makeJSONStatusResponse (0, "EQ enabled"));
    }


  /** Disable EQ. */
  NanoHTTPD.Response disableEq()
    {
    player.setEqEnabled (false);
    return new NanoHTTPD.Response (Response.Status.OK, "text/plain", 
      makeJSONStatusResponse (0, "EQ disabled"));
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
    player.scanAudioDatabase ();
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


  /** Stop the Android audio player before shutting down. Overrides
 *    stop() in Activity. */
  @Override
  public void stop()
    {
    stopPlayback();
    super.stop();
    }

}


