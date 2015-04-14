package net.kevinboone.androidmediaserver.client;

import java.io.*;
import java.net.*;
import org.json.*;

public class Client
{
  protected int port;
  protected String host;

  public Client (String host, int port)
    {
    this.host = host;
    this.port = port;
    }

  private String streamToString (java.io.InputStream is) 
    {
    java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
    return s.hasNext() ? s.next() : "";
    }

  public JSONObject runCommand (String command)
      throws ClientException, IOException
    {
    try 
      {
      URL url = new URL ("http://" + host + ":" + port + "/cmd?cmd=" + 
        URLEncoder.encode (command));
      InputStream is = url.openStream ();
      String result = streamToString (is);
      //System.out.println ("result= "+ result);
      JSONObject jo = new JSONObject (result);
      is.close();
      return jo;
      }
    catch (MalformedURLException e)
      {
      // This should never happen, unless the JVM is broken
      throw new ClientException (e.toString());
      }
    }


  protected void checkJSONResponse (JSONObject response) 
      throws ClientException
    {
    int status = response.getInt ("status");
    if (status != 0)
      {
      String msg = response.getString ("message");
      if (msg == null)
        throw new ClientException ("Server returned error code " + status);
      else
        throw new ClientException (msg);
      }
    }


  public void play () throws ClientException, IOException 
    {
    JSONObject response = runCommand ("play"); 
    checkJSONResponse (response);
    }


  public void pause () throws ClientException, IOException 
    {
    JSONObject response = runCommand ("pause"); 
    checkJSONResponse (response);
    }


  public void stop () throws ClientException, IOException 
    {
    JSONObject response = runCommand ("stop"); 
    checkJSONResponse (response);
    }


  public void next () throws ClientException, IOException 
    {
    JSONObject response = runCommand ("next"); 
    checkJSONResponse (response);
    }

  public void prev () throws ClientException, IOException 
    {
    JSONObject response = runCommand ("prev"); 
    checkJSONResponse (response);
    }


  public Status getStatus () throws ClientException, IOException 
    {
    try
      {
      JSONObject response = runCommand ("status"); 
      checkJSONResponse (response);
      Status status = new Status();
      String sTransportStatus = response.getString ("transport_status");
      if ("playing".equals (sTransportStatus))
        status.setTransportStatus (Status.TransportStatus.PLAYING); 
      else if ("stopped".equals (sTransportStatus))
        status.setTransportStatus (Status.TransportStatus.STOPPED); 
      else if ("paused".equals (sTransportStatus))
        status.setTransportStatus (Status.TransportStatus.PAUSED); 
      else
        status.setTransportStatus (Status.TransportStatus.UNKNOWN); 

      status.setTitle (response.getString ("title"));
      status.setUri (response.getString ("uri"));
      status.setArtist (response.getString ("artist"));
      status.setAlbum (response.getString ("album"));
      status.setPosition (Integer.parseInt (response.getString 
        ("transport_position")));
      status.setDuration (Integer.parseInt (response.getString 
        ("transport_duration")));

      return status;
      }
    catch (JSONException e)
      {
      throw new ClientException ("Error parsing response from server: " + 
         e.toString());
      }
    }

  public static void main (String[] args) throws Exception
    {
    Client client = new Client ("192.168.1.104", 30000); // TEST -- change
    if (args.length == 0)
      {
      Status ts = client.getStatus ();
      System.out.println (ts);
      }
    else
      {
      if ("play".equals (args[0]))
        client.play();
      else if ("pause".equals (args[0]))
        client.pause();
      else if ("stop".equals (args[0]))
        client.stop();
      else if ("next".equals (args[0]))
        client.next();
      else if ("prev".equals (args[0]))
        client.prev();
      }
    }

}


