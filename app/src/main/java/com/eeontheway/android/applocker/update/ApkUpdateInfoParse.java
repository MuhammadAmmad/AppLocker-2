package com.eeontheway.android.applocker.update;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * APK升级文件解析器
 * @author lishutong
 * @version v1.0
 * @Time 2016-12-15
 */
public class ApkUpdateInfoParse {
    private String filePath;

    public ApkUpdateInfoParse(String filePath) {
        this.filePath = filePath;
    }

    public PackageUpdateInfo getUpdateInfo () throws IOException, XmlPullParserException {
        PackageUpdateInfo info = null;

        FileInputStream is = new FileInputStream(filePath);
        XmlPullParser parser = Xml.newPullParser();
        try {
            parser.setInput(is, "UTF-8");
            int eventType = parser.getEventType();

            endParse:
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        info = new PackageUpdateInfo();
                        break;

                    case XmlPullParser.START_TAG:
                        String name = parser.getName();
                        if (name.equals("version")) {
                            info.setVersion(Integer.parseInt(parser.nextText()));
                        } else if (name.equals("updateLog")) {
                            info.setUpdateLog(parser.nextText());
                        } else if (name.equals("downloadUrl")) {
                            info.setUrl(parser.nextText());
                        } else if (name.equals("canBeSkipped")) {
                            info.setCanBeSkipped(Boolean.parseBoolean(parser.getText()));
                        }
                        break;

                    case XmlPullParser.END_DOCUMENT:
                        break endParse;
                }

                eventType = parser.next();
            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        }


        return info;
    }
}
