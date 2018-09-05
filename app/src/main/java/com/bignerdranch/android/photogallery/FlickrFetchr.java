package com.bignerdranch.android.photogallery;

import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by djn on 9/5/18.
 * Key:
 23e2d1e9f64e2567b1c013bd5a4f867c

 Secret:
 7a0dd0dc865442ba
 */

public class FlickrFetchr
{
    private static final String TAG = "FlickrFetchr";
    private static final String API_KEY = "23e2d1e9f64e2567b1c013bd5a4f867c";

     public byte[] getUrlBytes(String urlSpec) throws IOException
     {
         //creates a connection to urlSpec
         URL url = new URL(urlSpec);
         HttpURLConnection connection = (HttpURLConnection) url.openConnection();

         try {
             ByteArrayOutputStream out = new ByteArrayOutputStream();
             InputStream in = connection.getInputStream();

             //if connection failed
             if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                 throw new IOException(connection.getResponseMessage() +
                                        ": with " +
                                        urlSpec);
             }

             int bytesRead = 0;
             byte[] buffer = new byte[1024];

             //Reads bytes from in into buffer, writes the bytes in the buffer into out when the number
             //of bytes read is not zero; The assignment operator in Java returns the assigned value
             while ((bytesRead = in.read(buffer)) > 0) {
                 out.write(buffer, 0, bytesRead);
             }

             out.close();
             return out.toByteArray();

         } finally {
             connection.disconnect();
         }
     }

     public String getUrlString(String urlSpec) throws IOException
     {
         return new String(getUrlBytes(urlSpec));
     }

     public List<GalleryItem> fetchItems()
     {
         List<GalleryItem> items = new ArrayList<>();

         try {
             String url = Uri.parse("https://api.flickr.com/services/rest/")
                     .buildUpon()
                     .appendQueryParameter("method", "flickr.photos.getRecent")
                     .appendQueryParameter("api_key", API_KEY)
                     .appendQueryParameter("format", "json")
                     .appendQueryParameter("nojsoncallback", "1")
                     .appendQueryParameter("extras", "url_s")
                     .build().toString();
             String jsonString = getUrlString(url);
             Log.i(TAG, "Received JSON: " + jsonString);
             JSONObject jsonBody = new JSONObject(jsonString);// parses a JSON string
             parseItems(items, jsonBody);
         } catch (IOException ioe) {
             Log.e(TAG, "Failed to fetch items", ioe);
         } catch (JSONException je) {
             Log.e(TAG, "Failed to parse JSON", je);
         }

         return items;

     }

     private void parseItems(List<GalleryItem> items, JSONObject jsonBody) throws IOException, JSONException
     {
         JSONObject photosJsonObject = jsonBody.getJSONObject("photos") ;
         JSONArray photoJsonArray = photosJsonObject.getJSONArray("photo");

         //Constructs an item for each photo and adds them to the list
        for (int i = 0; i < photoJsonArray.length(); i++) {
            JSONObject photoJsonObject = photoJsonArray.getJSONObject(i);

            GalleryItem item = new GalleryItem();
            item.setId(photoJsonObject.getString("id"));
            item.setCaption(photoJsonObject.getString("title"));

            if (!photoJsonObject.has("url_s")) {
                continue;
            }

            //returns the url to the small picture
            item.setUrl(photoJsonObject.getString("url_s"));
            items.add(item);
        }
     }
}