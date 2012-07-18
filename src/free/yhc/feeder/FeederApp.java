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

package free.yhc.feeder;

import android.app.Application;
import android.content.res.Configuration;
import free.yhc.feeder.model.DBPolicy;
import free.yhc.feeder.model.DBThread;
import free.yhc.feeder.model.RTTask;
import free.yhc.feeder.model.UIPolicy;
import free.yhc.feeder.model.UnexpectedExceptionHandler;
import free.yhc.feeder.model.UsageReport;
import free.yhc.feeder.model.Utils;

public class FeederApp extends Application {
    @Override
    public void
    onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void
    onCreate() {
        super.onCreate();

        // NOTE
        // Order is important

        // Utils.init() SHOULD be called before calling other init() functions,
        //   because Utils has application context and offer it to other modules.
        // And most modules uses application context in it's early stage - ex. constructor.
        Utils.init(getApplicationContext());

        // register default customized uncaught exception handler for error collecting.
        Thread.setDefaultUncaughtExceptionHandler(UnexpectedExceptionHandler.get());

        // Create singleton instances
        // At this class, DB class cannot be instanciate explicitly because of i's visibility.
        // But, DBThread instanciate DB class in it.
        DBThread.get().start();
        UIPolicy.get();
        DBPolicy.get();
        RTTask.get();
        UsageReport.get();
        LookAndFeel.get();
    }

    @Override
    public void
    onLowMemory() {
        super.onLowMemory();
    }
}
