package chao.app.ami;

import android.app.Application;
import android.util.Log;

import com.squareup.leakcanary.LeakCanary;

import chao.app.ami.launcher.drawer.DrawerManager;
import chao.app.ami.proxy.ProxyManager;
import chao.app.ami.text.TextManager;
import chao.app.ami.utils.Util;

/**
 * @author chao.qin
 * @since 2017/7/27
 *
 * 调试工具
 *
 */

public class Ami {

    private static final String TAG = "AMI";
    private static Application mApp;
    private static Ami mInstance;

    public static final int LIFECYCLE_LEVEL_FULL = 3;
    public static final int LIFECYCLE_LEVEL_SIMPLE = 2;
    public static final int LIFECYCLE_LEVEL_CREATE = 1;
    public static final int LIFECYCLE_LEVEL_NONE = 0;

    private static int mLifecycle = LIFECYCLE_LEVEL_NONE;

    private Ami(Application app) {
        mApp = app;
    }

    public static void init(Application app, int drawerId) {
        if (!Util.isHostAppDebugMode(app)) {
            return;
        }
        if (mInstance != null) {
            return;
        }
        mInstance = new Ami(app);
        LeakCanary.install(app);
        DrawerManager.init(app, drawerId);
        ProxyManager.init(app);
        TextManager.init();
    }


    public static Application getApp() {
        return mApp;
    }

    public static void setLifecycleLevel(int level) {
        mLifecycle = level;
    }

    public static void log(String log) {
        log(TAG, log);
    }

    public static void log(String tag, String log) {
        Log.d(tag, log);
    }

    public static void lifecycle(String tag, String log, int level) {
        if (mLifecycle == LIFECYCLE_LEVEL_NONE) {
            return;
        }
        if (level > mLifecycle) {
            return;
        }
        log(tag, log);
    }


}