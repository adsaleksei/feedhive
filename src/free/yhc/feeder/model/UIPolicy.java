/*****************************************************************************
 *    Copyright (C) 2012 Younghyung Cho. <yhcting77@gmail.com>
 *
 *    This file is part of Feeder.
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as
 *    published by the Free Software Foundation either version 3 of the
 *    License, or (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License
 *    (<http://www.gnu.org/licenses/lgpl.html>) for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *****************************************************************************/

package free.yhc.feeder.model;

import static free.yhc.feeder.model.Utils.eAssert;
import static free.yhc.feeder.model.Utils.isValidValue;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
/*
 * Functions related with UIPolicy...
 *     - initial setting of values.
 */
public class UIPolicy {

    public static final String PREF_KEY_APP_ROOT = "app_root";
    public static final long   USAGE_INFO_UPDATE_PERIOD = 1000 * 60 * 60 * 24 * 7; // (ms) 7 days = 1 week

    // ext2, ext3, ext4 allows 255 bytes for filename.
    // but 'char' type in java is 2byte (16-bit unicode).
    // So, maximum character for filename in java on extN is 127.
    private static final int    MAX_FILENAME_LENGTH = 127;

    private static String appRootDir;
    private static File   appTempDirFile;
    private static File   appLogDirFile;
    private static File   appErrLogFile;
    private static File   appUsageLogFile;

    /**
     * Guessing default action type from Feed data.
     * @param cParD
     * @param iParD
     * @return
     *   Feed.Channel.FActxxxx
     */
    static long
    decideActionType(long action, Feed.Channel.ParD cParD, Feed.Item.ParD iParD) {
        long    actFlag;

        if (null == iParD) {
            if (Feed.FINVALID == action)
                return Feed.FINVALID; // do nothing if there is no items at first insertion.

            // default value
            actFlag = Feed.Channel.FACT_TGT_LINK | Feed.Channel.FACT_OP_OPEN | Feed.Channel.FACT_PROG_DEFAULT;
        }

        if (isValidValue(iParD.enclosureUrl)) {
            if (Feed.Channel.CHANN_TYPE_EMBEDDED_MEDIA == cParD.type)
                actFlag = Feed.Channel.FACT_TGT_ENCLOSURE | Feed.Channel.FACT_OP_OPEN | Feed.Channel.FACT_PROG_EX;
            else
                actFlag = Feed.Channel.FACT_TGT_ENCLOSURE | Feed.Channel.FACT_OP_DN | Feed.Channel.FACT_PROG_DEFAULT;
        } else
            actFlag = Feed.Channel.FACT_TGT_LINK | Feed.Channel.FACT_OP_OPEN | Feed.Channel.FACT_PROG_DEFAULT;

        // NOTE
        // FACT_PROG_IN/EX can be configurable by user
        // So, this flag should not be changed except for action is invalid value.
        if (Feed.FINVALID == action)
            // In case of newly inserted channel (first decision), FACT_PROG_XX should be set as recommended one.
            return Utils.bitSet(action, actFlag,
                                Feed.Channel.MACT_TGT | Feed.Channel.MACT_OP | Feed.Channel.MACT_PROG);
        else
            // If this is NOT first decision, user may change FACT_PROG_XX setting (UX scenario support this.)
            // So, in this case, FACT_PROG_XX SHOULD NOT be changed.
            return Utils.bitSet(action, actFlag,
                    Feed.Channel.MACT_TGT | Feed.Channel.MACT_OP);
    }

    /**
     * Check that is this valid item?
     * (Result of parsing has enough information required by this application?)
     * @param item
     * @return
     */
    static boolean
    verifyConstraints(Feed.Item.ParD item) {
        // 'title' is mandatory!!!
        if (!isValidValue(item.title))
            return false;

        // Item should have one of link or enclosure url.
        if (!isValidValue(item.link)
             && !isValidValue(item.enclosureUrl))
            return false;

        return true;
    }

    /**
     * Check that is this valid channel?
     * (Result of parsing has enough information required by this application?)
     * @param ch
     * @return
     */
    static boolean
    verifyConstraints(Feed.Channel.ParD ch) {
        if (!isValidValue(ch.title))
            return false;

        return true;
    }

    /**
     * Initialize
     * @param context
     */
    public static void
    init(Context context)  {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String appRoot = prefs.getString(PREF_KEY_APP_ROOT, "/sdcard/yhcFeeder");
        setAppDirectories(appRoot);
        cleanTempFiles();
    }

    /**
     * SHOULD be called only by FeederPreferenceActivity.
     * @param root
     */
    public static void
    setAppDirectories(String root) {
        appRootDir = root;
        new File(appRootDir).mkdirs();
        if (!root.endsWith("/"))
            appRootDir += "/";

        appTempDirFile = new File(appRootDir + "temp/");
        appTempDirFile.mkdirs();
        appLogDirFile = new File(appRootDir + "log/");
        appLogDirFile.mkdirs();
        appErrLogFile = new File(appLogDirFile.getAbsoluteFile() + "/last_error");
        appUsageLogFile = new File(appLogDirFile.getAbsoluteFile() + "/usage_file");
    }

    public static String
    getAppRootDirectoryPath() {
        return appRootDir;
    }

    public static String
    getPredefinedChannelsAssetPath() {
        Locale lc = java.util.Locale.getDefault();
        String file;
        if (Locale.KOREA.equals(lc) || Locale.KOREAN.equals(lc))
            file = "channels_kr.xml";
        else
            file = "channels_en.xml";
        return file;
    }

    /**
     * Create clean channel dir.
     * If directory already exists, all files in it are deleted.
     * @param cid
     * @return
     */
    public static boolean
    makeChannelDir(long cid) {
        File f = new File(appRootDir + cid);
        if (f.exists())
            Utils.removeFileRecursive(f, true);
        return f.mkdir();
    }

    /**
     * This deletes channel directory itself.
     * @param cid
     * @return
     */
    public static boolean
    removeChannelDir(long cid) {
        return Utils.removeFileRecursive(new File(appRootDir + cid), true);
    }

    public static boolean
    cleanChannelDir(long cid) {
        return Utils.removeFileRecursive(new File(appRootDir + cid), false);
    }

    public static File
    getNewTempFile() {
        File ret = null;
        try {
            ret = File.createTempFile("free.yhc.feeder", null, appTempDirFile);
        } catch (IOException e){}
        return ret;
    }

    public static void
    cleanTempFiles() {
        Utils.removeFileRecursive(appTempDirFile, false);
    }

    public static File
    getErrLogFile() {
        return appErrLogFile;
    }

    public static File
    getUsageLogFile() {
        return appUsageLogFile;
    }

    /**
     * Get file which contains data for given feed item.
     * Usually, this file is downloaded from internet.
     * (Ex. downloaded web page / downloaded mp3 etc)
     * @param id
     * @return
     */
    public static File
    getItemDataFile(long id) {
        return getItemDataFile(id, -1, null, null);
    }

    // NOTE
    // Why this parameter is given even if we can get from DB?
    // This is only for performance reason!
    // postfix : usually, extension;
    /**
     * NOTE
     * Why these parameters - title, url - are given even if we can get from DB?
     * This is only for performance reason!
     * @param id
     *   item id
     * @param cid
     *   channel id of this item. if '< 0', than value read from DB is used.
     * @param title
     *   title of this item. if '== null' or 'isEmpty()', than value read from DB is used.
     * @param url
     *   target url of this item. link or enclosure is possible.
     *   if '== null' or is 'isEmpty()', then value read from DB is used.
     * @return
     */
    public static File
    getItemDataFile(long id, long cid, String title, String url) {
        if (cid < 0)
            cid = DBPolicy.S().getItemInfoLong(id, DB.ColumnItem.CHANNELID);

        if (!Utils.isValidValue(title))
            title = DBPolicy.S().getItemInfoString(id, DB.ColumnItem.TITLE);

        if (!Utils.isValidValue(url)) {
            long action = DBPolicy.S().getChannelInfoLong(cid, DB.ColumnChannel.ACTION);
            if (Feed.Channel.isActTgtLink(action))
                url = DBPolicy.S().getItemInfoString(id, DB.ColumnItem.LINK);
            else if (Feed.Channel.isActTgtEnclosure(action))
                url = DBPolicy.S().getItemInfoString(id, DB.ColumnItem.ENCLOSURE_URL);
            else
                url = "";
        }

        // we don't need to create valid filename with empty url value.
        if (url.isEmpty())
            return null;

        String ext = Utils.getExtentionFromUrl(url);

        // Title may include character that is not allowed as file name
        // (ex. '/')
        // Item is id is preserved even after update.
        // So, item ID can be used as file name to match item and file.
        String fname = Utils.convertToFilename(title) + "_" + id;
        int endIndex = MAX_FILENAME_LENGTH - ext.length() - 1; // '- 1' for '.'
        if (endIndex > fname.length())
            endIndex = fname.length();

        fname = fname.substring(0, endIndex);
        fname = fname + '.' + ext;

        // NOTE
        //   In most UNIX file systems, only '/' and 'null' are reserved.
        //   So, we don't worry about "converting string to valid file name".
        return new File(appRootDir + cid + "/" + fname);
    }

    /**
     * Get BG task thread priority from shared preference.
     * @param context
     * @return
     *   Value of Java Thread priority (between Thread.MIN_PRIORITY and Thread.MAX_PRIORITY)
     */
    public static int
    getPrefBGTaskPriority(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String prio = prefs.getString("bgtask_prio", "low");
        if ("low".equals(prio))
            return Thread.MIN_PRIORITY;
        else if ("medium".equals(prio))
            return (Thread.NORM_PRIORITY + Thread.MIN_PRIORITY) / 2;
        else if ("high".equals(prio))
            return Thread.NORM_PRIORITY;
        else {
            eAssert(false);
            return Thread.MIN_PRIORITY;
        }
    }
}
