package com.avoice.uam.util;

import android.util.Xml;

import com.avoice.uam.model.Track;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;

public class RadioXmlParser {
    private static final String ns = null; //no namespaces required so we won't use one
    /*TAGS*/
    private static final String START = "tracks";
    private static final String TRACK_TAG = "track";
    private static final String TITLE_TAG = "title";
    private static final String ARTISTS_TAG = "artists";
    private static final String COVER_TAG = "cover";
    private static final String CALLMEBACK_TAG = "callmeback";

    public Track parse(InputStream inputStream) throws XmlPullParserException, IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(inputStream, null);
            parser.nextTag();
            return readFeed(parser);
        } finally {
            inputStream.close();
        }
    }

    private Track readFeed(XmlPullParser parser) throws XmlPullParserException, IOException {
        Track track = null;

        parser.require(XmlPullParser.START_TAG, ns, START);
        while (parser.next() != XmlPullParser.END_TAG) {
            if(parser.getEventType() != XmlPullParser.START_TAG) {
                //continue if it's not a start tag of some kind
                continue;
            }
            String tagName = parser.getName();
            //It is <track> tag that we need
            if(tagName.equals(TRACK_TAG)) {
                track = readTrack(parser);
            } else {
                skip(parser);
            }
        }
        return track;
    }

    /**
     * Parses the contents of an entry. <br> If it encounters a title, artists, cover or callmeback
     * tag, hands them off to their respective <i>read</i> methods for processing.
     * Otherwise, skips the tag.
     * @param parser
     * @return
     */
    private Track readTrack(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, TRACK_TAG);
        String title = null;
        String artists = null;
        String cover = null;
        String callmeback = null;

        while (parser.next() != XmlPullParser.END_TAG) {
            if(parser.getEventType() != XmlPullParser.START_TAG) {
                //continue if it's not a start tag of some kind
                continue;
            }
            String tagName = parser.getName();
            switch (tagName) {
                case TITLE_TAG:
                    title = readTitle(parser);
                    break;
                case ARTISTS_TAG:
                    artists = readArtists(parser);
                    break;
                case COVER_TAG:
                    cover = readCover(parser);
                    break;
                case CALLMEBACK_TAG:
                    callmeback = readCallmeback(parser);
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        return new Track(title, artists, cover, callmeback);
    }

    //region Specific "read" methods
    private String readTitle(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, TITLE_TAG);
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, TITLE_TAG);
        return title;
    }

    private String readArtists(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, ARTISTS_TAG);
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, ARTISTS_TAG);
        return title;
    }

    private String readCover(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, COVER_TAG);
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, COVER_TAG);
        return title;
    }

    private String readCallmeback(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, CALLMEBACK_TAG);
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, CALLMEBACK_TAG);
        return title;
    }

    //endregion

    /**
     * Extracts text values for all tags
     * @param parser
     * @return
     * @throws IOException
     * @throws XmlPullParserException
     */
    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if(parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }
    /**
     * Skips tags the parser isn't interested in. Uses depth to handle nested tags. i.e.,
     * if the next tag after a START_TAG isn't a matching END_TAG, it keeps going until it
     * finds the matching END_TAG (as indicated by the value of "depth" being 0).
     * @param parser
     */
    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException,
            IllegalStateException{
        if(parser.getEventType() != XmlPullParser.START_DOCUMENT) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }
}
