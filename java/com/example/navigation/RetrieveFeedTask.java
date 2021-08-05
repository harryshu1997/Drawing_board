package com.example.navigation;

import android.graphics.Bitmap;
import android.os.AsyncTask;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.net.URL;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * 项目名： Navigation
 * 文件名： RetrieveFeedTask
 *
 * @Author: Zhihao Shu(Harry)
 * @email: 919900130@qq.com
 * 创建日期：2020 2020/7/29 17:14
 * 描述：
 */
class RetrieveFeedTask extends AsyncTask<String, Void, Bitmap> {
    @Override
    protected Bitmap doInBackground(String... strings) {
        return null;
    }
//
//    private Exception exception;
//
//    protected RSSFeed doInBackground(String... urls) {
//        try {
//            URL url = new URL(urls[0]);
//            SAXParserFactory factory = SAXParserFactory.newInstance();
//            SAXParser parser = factory.newSAXParser();
//            XMLReader xmlreader = parser.getXMLReader();
//            RssHandler theRSSHandler = new RssHandler();
//            xmlreader.setContentHandler(theRSSHandler);
//            InputSource is = new InputSource(url.openStream());
//            xmlreader.parse(is);
//
//            return theRSSHandler.getFeed();
//        } catch (Exception e) {
//            this.exception = e;
//
//            return null;
//        } finally {
//            is.close();
//        }
//    }
//
//    protected void onPostExecute(RSSFeed feed) {
//        // TODO: check this.exception
//        // TODO: do something with the feed
//    }
}
