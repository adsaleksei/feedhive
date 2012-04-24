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

import static free.yhc.feeder.model.Utils.logI;
import android.content.Context;
import android.os.PowerManager;

public class BGTaskUpdateChannel extends BGTask<BGTaskUpdateChannel.Arg, Object> {
    private static final String WLTag = "free.yhc.feeder.BGTaskUpdateChannel";

    private Context            context;
    private PowerManager.WakeLock wl;
    private volatile NetLoader loader = null;
    private Arg                arg    = null;

    public static class Arg {
        long    cid        = -1;
        String  customIconref = null;

        public Arg(long cid) {
            this.cid = cid;
        }
        public Arg(long cid, String customIconref) {
            this.cid = cid;
            this.customIconref = customIconref;
        }

    }

    public
    BGTaskUpdateChannel(Context context, Arg arg) {
        super(arg);
        this.context = context;
        wl = ((PowerManager)context.getSystemService(Context.POWER_SERVICE))
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WLTag);
        setPriority(UIPolicy.getPrefBGTaskPriority(context));
    }

    @Override
    protected void
    onPreRun() {
        synchronized (wl) {
            wl.acquire();
        }
    }

    @Override
    protected void
    onPostRun (Err result) {
        synchronized (wl) {
            wl.release();
        }
    }

    @Override
    protected void
    onCancel(Object param) {
        // If task is cancelled before started, then Wakelock under-lock exception is issued!
        synchronized (wl) {
            if (wl.isHeld())
                wl.release();
        }
    }


    @Override
    protected Err
    doBGTask(Arg arg) {
        this.arg = arg;
        try {
            loader = new NetLoader();
            if (null == arg.customIconref)
                loader.updateLoad(arg.cid);
            else
                loader.updateLoad(arg.cid, arg.customIconref);
        } catch (FeederException e) {
            logI("BGTaskUpdateChannel : Updating [" + arg.cid + "] : interrupted!");
            return e.getError();
        }
        return Err.NoErr;
    }

    @Override
    public boolean
    cancel(Object param) {
        // I may misunderstand that canceling background task may corrupt DB
        //   by interrupting in the middle of transaction.
        // But java thread doesn't interrupt it's executing.
        // So, I don't worry about this (different from C.)
        super.cancel(param); // cancel thread
        if (null != loader)
            loader.cancel();     // This is HACK for fast-interrupt.
        return true;
    }
}
