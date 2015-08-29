package com.example.sony.downloader;

import android.os.Environment;
import android.util.Log;

import org.jsoup.nodes.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.w3c.dom.*;
import java.io.*;
import java.net.*;
import java.util.*;

import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
// Jsoup API reference:
// http://jsoup.org/apidocs/

/**
 * This is a helper class for the Downloader application. It does the
 * actual work of downloading a file from a given URL. It also includes
 * "fake" download methods to simulate downloading the file without
 * actually using the network for testing purposes.
 *
 */
public class Downloader {
    /*
     * Downloads the file found at the given URL, into the Android
     * device's Downloads folder in its external storage, and returns
     * the file name it was saved to.
     */
    public static String
    download(String url_string)
    {
        File folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        Log.v("Downloader", "downloading from " + url_string + " to " + folder);
        try {
            Thread.sleep(2000);
        }
        catch (InterruptedException ie) {
            // empty
        }

        if (!folder.exists())
        {
            // create folder
            folder.mkdirs();
        }

        // download the file into a memory buffer
        byte[] bytes = downloadToByteArray(url_string);

        // store the memory buffer contents into a file
        File url_file = new File(url_string);
        String file_name = url_file.getName();
        File out_file = new File(folder, file_name);
        try {
            FileOutputStream out = new FileOutputStream(out_file);
            out.write(bytes);
            out.close();
            return file_name;
        }
        catch (IOException ie) {
            throw new RuntimeException(ie);
        }
    }

    /*
     * This method pretends to download the file found at the given URL.
     * Returns the file name it would have been saved to.
     */
    public static String
    downloadFake(String url_string)
    {
        Log.d("Downloader", "downloadFake called ...");
        File folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File url_file = new File(url_string);
        String file_name = url_file.getName();
        File out_file = new File(folder, file_name);
        Log.v("Downloader", "downloading " + url_string + " to " + out_file);

        try
        {
            Thread.sleep(3000);
        }
        catch (InterruptedException ie)
        {
            // empty
        }
        return file_name;
    }

    /*
     * This method retrieves all of the links like <a href="http://example.com/foo.html">...</a>
     * from the page and returns their href URLs as an array.
     * Does not work on some pages due to invalid HTML content.
     *
     * Update: Decided to change original method since many web pages are messy.
     * Many web pages causes this function to break. Now, I am using Jsoup, which is
     * a opens ource Java HTML parser. Now this function can handle for
     * some messy HTML web pages.
     *
     * Issues:
     *      When fetching all the links, depending on the HTML file there
     *      may be some links that are not necessarily correct. That is
     *      some links does not actually lead to an object to download.
     *      It is just link to another web page.
     *
     */
    public static String[]
    getAllLinks(String web_page_url)
    {
        Log.d("Downloader", "getAllLinks called ...");
        ArrayList<String> list = new ArrayList<>();
        try {
            // get and parse HTML file from url
            Document document = Jsoup.connect(web_page_url).get();

            Elements links = document.getElementsByTag("a");    // list of all <a> tag elements
            for (Element link: links)
            {
                String href = link.attr("abs:href");
                try {
                    URL url = new URL(href);     // try to parse string as URL;
                                                            // throws MalformedURLException if cannot
                    list.add(href);
                }
                catch (MalformedURLException mf_url_e)
                {
                    // invalid URL; do not add
                }
            }

            return list.toArray(new String[0]);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

     /*
      * Original approach
      * Works well if HTML file is clean. Otherwise, parser throws exceptions.
      * There is no built in HTML parser in Java, so this is parser for XML file. Thus,
      * messy HTML files does not work.

    public static String[]
    getAllLinks(String web_page_url)
    {
        Log.d("Downloader", "getAllLinks called ...");
        try {
            ArrayList<String> list = new ArrayList<>();
            byte[] bytes = downloadToByteArray(web_page_url);

            Log.d("Downloader", "starting to build doc - getAllLinks");
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(true);                     // awareness of namespace
            documentBuilderFactory.setIgnoringElementContentWhitespace(true);   // ignore whitespace
            documentBuilderFactory.setValidating(false);                        // validate while parsing

            DocumentBuilder document_builder = documentBuilderFactory.newDocumentBuilder();

            URL web_url = new URL(web_page_url);
            InputStream input_stream = web_url.openStream();

            Document document = document_builder.parse(input_stream);
            //Document document = document_builder.parse(new ByteArrayInputStream(bytes));    // represents entire HTML document

            NodeList link_nodes = document.getElementsByTagName("a");       // node list of all <a> tags in document
            for (int i = 0; i < link_nodes.getLength(); i++)
            {
                Node node = link_nodes.item(i);
                Node href_node = node.getAttributes().getNamedItem("href");
                if(href_node != null)
                {
                    String href_url_string = href_node.getNodeValue();     // url string
                    try {
                        URL url = new URL(href_url_string);     // try to parse string as URL;
                                                                // throws MalformedURLException if cannot
                        list.add(href_url_string);
                    }
                    catch (MalformedURLException mf_url_e)
                    {
                        // invalid URL; do not add
                    }
                }
            }

            return list.toArray(new String[0]);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
*/
    /*
     * Reads the entire contents of the given file from the device's Downloads folder.
     * Returns the file's content as the text string.
     */
    public static String
    readEntireFile(String filename)
    {
        try {
            File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(directory, filename);
            StringBuilder string_builder = new StringBuilder();
            BufferedReader reader = new BufferedReader(new FileReader(file));
            while (reader.ready())
            {
                string_builder.append((char) reader.read());
            }
            return string_builder.toString();
        }
        catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    /*
     * Downloads the file found at the URL into a memory buffer of bytes.
     * Returns the bytes as an array.
     */
    private static byte[]
    downloadToByteArray(String url_string)
    {
        Log.d("Downloader", "downloadToByteArray called ...");
        try {
            // download the file into a memory buffer
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            URL url = new URL(url_string);
            InputStream input_stream = url.openStream();
            BufferedReader buffered_reader = new BufferedReader(new InputStreamReader(input_stream));
            int byte_read;
            while ((byte_read = buffered_reader.read()) != -1)
            {
                bytes.write(byte_read);
            }
            input_stream.close();
            Log.d("Downloader", "finished reading to byte stream - downloadToByteArray");
            return bytes.toByteArray();
        }
        catch(IOException e){
            throw new RuntimeException(e);
        }
    }
}
