package org.opencv.android;

import java.io.File;
import java.util.StringTokenizer;

import org.opencv.core.Core;
import org.opencv.android.OpenCVLoader;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

/**
 * 兼容旧接口的现代化 AsyncServiceHelper。
 *
 * 改动要点：
 * - 移除了对 org.opencv.engine.OpenCVEngineInterface 的依赖（不再使用 OpenCV Manager）。
 * - 保留原有 public 接口（initOpenCV(...) 等）不变，以便调用方无需修改。
 * - 优先尝试静态加载 OpenCV（OpenCVLoader.initDebug()），失败时尝试 OpenCVLoader.initAsync(...)。
 * - 若都失败，则调用 InstallService(...) 提示用户去安装（保留原来提示 Play 商店的逻辑）。
 * - 原代码中大量调用 mAppContext.unbindService(mServiceConnection) 的位置都改为安全解绑（如果未绑定则忽略），避免异常。
 */
class AsyncServiceHelper
{
    public static boolean initOpenCV(String Version, final Context AppContext,
                                     final LoaderCallbackInterface Callback)
    {
        AsyncServiceHelper helper = new AsyncServiceHelper(Version, AppContext, Callback);

        // 旧实现是 bind 到 org.opencv.engine 服务。现代实现：优先静态加载（内嵌 .so）
        Log.d(TAG, "InitOpenCV called. version: " + Version);

        // Try static init first
        if (OpenCVLoader.initDebug()) {
            Log.d(TAG, "Static OpenCVLoader.initDebug() succeeded");
            helper.logBuildInfo();
            Callback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
            return true;
        }

        // If static failed -> try async init (this still uses libs packaged in APK/aar)
        Log.d(TAG, "Static init failed. Trying OpenCVLoader.initAsync()");
        boolean asyncStarted = OpenCVLoader.initAsync(Version, AppContext, Callback);
        if (asyncStarted) {
            Log.d(TAG, "OpenCVLoader.initAsync() started");
            return true;
        }

        // If async couldn't start (very rare), fallback to "install" flow to mimic old behavior
        Log.d(TAG, "Async init couldn't start. Falling back to InstallService()");
        InstallService(AppContext, Callback);
        return false;
    }

    protected AsyncServiceHelper(String Version, Context AppContext, LoaderCallbackInterface Callback)
    {
        mOpenCVersion = Version;
        mUserAppCallback = Callback;
        mAppContext = AppContext;
    }

    protected static final String TAG = "OpenCVManager/Helper";
    protected static final int MINIMUM_ENGINE_VERSION = 2;
    // removed mEngineService (no OpenCV Manager)
    protected LoaderCallbackInterface mUserAppCallback;
    protected String mOpenCVersion;
    protected Context mAppContext;
    protected static boolean mServiceInstallationProgress = false;
    protected static boolean mLibraryInstallationProgress = false;

    // 保留 InstallServiceQuiet / InstallService 行为 —— 引导用户去 Play 商店安装（作为回退）
    protected static boolean InstallServiceQuiet(Context context)
    {
        boolean result = true;
        try
        {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(OPEN_CV_SERVICE_URL));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
        catch(Exception e)
        {
            result = false;
        }

        return result;
    }

    protected static void InstallService(final Context AppContext, final LoaderCallbackInterface Callback)
    {
        if (!mServiceInstallationProgress)
        {
            Log.d(TAG, "Request new service installation (fallback to market)");
            InstallCallbackInterface InstallQuery = new InstallCallbackInterface() {
                private LoaderCallbackInterface mUserAppCallback = Callback;
                public String getPackageName()
                {
                    return "OpenCV Manager";
                }
                public void install() {
                    Log.d(TAG, "Trying to install OpenCV Manager via Google Play (fallback)");
                    boolean result = InstallServiceQuiet(AppContext);
                    if (result)
                    {
                        mServiceInstallationProgress = true;
                        Log.d(TAG, "Package installation started");
                    }
                    else
                    {
                        Log.d(TAG, "OpenCV package was not installed!");
                        int Status = LoaderCallbackInterface.MARKET_ERROR;
                        Log.d(TAG, "Init finished with status " + Status);
                        Log.d(TAG, "Calling using callback");
                        mUserAppCallback.onManagerConnected(Status);
                    }
                }

                public void cancel()
                {
                    Log.d(TAG, "OpenCV library installation was canceled");
                    int Status = LoaderCallbackInterface.INSTALL_CANCELED;
                    Log.d(TAG, "Init finished with status " + Status);
                    Log.d(TAG, "Calling using callback");
                    mUserAppCallback.onManagerConnected(Status);
                }

                public void wait_install()
                {
                    Log.e(TAG, "Installation was not started! Nothing to wait!");
                }
            };

            Callback.onPackageInstall(InstallCallbackInterface.NEW_INSTALLATION, InstallQuery);
        }
        else
        {
            Log.d(TAG, "Waiting current installation process");
            InstallCallbackInterface WaitQuery = new InstallCallbackInterface() {
                private LoaderCallbackInterface mUserAppCallback = Callback;
                public String getPackageName()
                {
                    return "OpenCV Manager";
                }
                public void install()
                {
                    Log.e(TAG, "Nothing to install we just wait current installation");
                }
                public void cancel()
                {
                    Log.d(TAG, "Waiting for OpenCV canceled by user");
                    mServiceInstallationProgress = false;
                    int Status = LoaderCallbackInterface.INSTALL_CANCELED;
                    Log.d(TAG, "Init finished with status " + Status);
                    Log.d(TAG, "Calling using callback");
                    mUserAppCallback.onManagerConnected(Status);
                }
                public void wait_install()
                {
                    InstallServiceQuiet(AppContext);
                }
            };

            Callback.onPackageInstall(InstallCallbackInterface.INSTALLATION_PROGRESS, WaitQuery);
        }
    }

    /**
     *  URL of OpenCV Manager page on Google Play Market.
     */
    protected static final String OPEN_CV_SERVICE_URL = "market://details?id=org.opencv.engine";

    // ---------- 以下保留了原来类中用于加载 .so 的辅助方法（仍可使用） ----------

    /**
     * 尝试加载单个 native 库绝对路径（System.load）
     */
    private boolean loadLibrary(String AbsPath)
    {
        boolean result = true;

        Log.d(TAG, "Trying to load library " + AbsPath);
        try
        {
            System.load(AbsPath);
            Log.d(TAG, "OpenCV libs init was ok!");
        }
        catch(UnsatisfiedLinkError e)
        {
            Log.d(TAG, "Cannot load library \"" + AbsPath + "\"");
            e.printStackTrace();
            result = false;
        }

        return result;
    }

    /**
     * 如果你从某个路径和 libs 列表手动加载 native 库，可以调用此方法。
     * 在我们的现代实现中通常不需要（使用 OpenCVLoader），但保留以兼容调用者。
     */
    private boolean initOpenCVLibs(String Path, String Libs)
    {
        Log.d(TAG, "Trying to init OpenCV libs");
        if ((null != Path) && (Path.length() != 0))
        {
            boolean result = true;
            if ((null != Libs) && (Libs.length() != 0))
            {
                Log.d(TAG, "Trying to load libs by dependency list");
                StringTokenizer splitter = new StringTokenizer(Libs, ";");
                while(splitter.hasMoreTokens())
                {
                    String AbsLibraryPath = Path + File.separator + splitter.nextToken();
                    result &= loadLibrary(AbsLibraryPath);
                }
            }
            else
            {
                // If the dependencies list is not defined or empty.
                String AbsLibraryPath = Path + File.separator + "libopencv_java3.so";
                result = loadLibrary(AbsLibraryPath);
            }

            return result;
        }
        else
        {
            Log.d(TAG, "Library path \"" + Path + "\" is empty");
            return false;
        }
    }

    // 打印 OpenCV build 信息
    private void logBuildInfo() {
        try {
            String eol = System.getProperty("line.separator");
            for (String str : Core.getBuildInformation().split(eol))
                Log.i(TAG, str);
        } catch (Exception e) {
            // ignore
        }
    }
}
