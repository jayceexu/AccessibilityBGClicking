package com.example.jianxu.ppppaccwear;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Base64;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayDeque;
import java.util.List;

public class AccService extends AccessibilityService implements
        DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        MessageApi.MessageListener {

    final static String TAG = "AccessibilityService";
    private GoogleApiClient mGoogleApiClient;
    private static final String MESSAGE = "/message";
    private static final String SEPARATOR = "||";
    private static int id = 1;
    @Override
    public void onCreate() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApiIfAvailable(Wearable.API)
                .build();
        mGoogleApiClient.connect();

        Log.i(TAG, "Creating..........");
        super.onCreate();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.i(TAG, "BEGIN___onAccessibilityEvent............");


        List<AccessibilityWindowInfo> windows = getWindows();

        for (AccessibilityWindowInfo window : windows) {
            Log.i(TAG, "AccessibilityWindowInfo: " + window);
        }
        //getForegroundActivity();


        AccessibilityNodeInfo source = event.getSource();
        if (source == null) {
            Log.i(TAG, "event.getSource() is null");
            return;
        }
        //Log.i(TAG, "..........." + source.getClassName().toString());
        Log.i(TAG, "Source Node: " + source);
//
//        String str;
//        if (source.getParent()!=null)
//            str = printTreeRecursive(source.getParent());
//        else {
//            str = printTreeRecursive(source);
//            Log.i(TAG, "Source parent node is null !!");
//        }
        Bitmap bt;
        if (source.getParent()!=null)
            bt = getBitmapRecursive(source.getParent());
        else {
            bt = getBitmapRecursive(source);
        }
        String msg = BitMapToString(bt);
        if (msg != "")
            sendMessage(MESSAGE, BitMapToString(bt));
    }

    public String BitMapToString(Bitmap bitmap){
        if (bitmap == null) {
            return "";
        }
        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG,100, baos);
        byte [] b=baos.toByteArray();
        String temp=Base64.encodeToString(b, Base64.DEFAULT);
        return temp;
    }

    // This should be deleted
    @Override
    public void onAccessibilityEventForBackground(String s, AccessibilityEvent accessibilityEvent) {
        Log.i(TAG, "BEGIN___onAccessibilityEventForBackground............");

    }


    private void getForegroundActivity() {
        ActivityManager am = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
        //Log.d(TAG, "CURRENT Activity ::" + taskInfo.get(0).topActivity.getClassName());

        ComponentName componentInfo = taskInfo.get(0).topActivity;
        componentInfo.getPackageName();
        Log.i(TAG, componentInfo.getPackageName().toString());

    }

    private void printTree(AccessibilityNodeInfo root) {
        ArrayDeque<AccessibilityNodeInfo> deque = new ArrayDeque<AccessibilityNodeInfo>();
        deque.addLast(root);

        while(!deque.isEmpty()) {
            AccessibilityNodeInfo node = deque.pollFirst();
            if (node == null) {
                continue;
            }

            printNode(node);

            int cnt = node.getChildCount();
            for (int i = 0; i < cnt; ++i) {
                AccessibilityNodeInfo tmp = node.getChild(i);
                deque.addLast(tmp);
            }
        }

    }

    private String printTreeRecursive(AccessibilityNodeInfo root) {
        if (root == null)
            return SEPARATOR;

        String strRoot = printNode(root);

        if (root.getChildCount() == 0) {
            return strRoot + SEPARATOR;
        }

        int cnt = root.getChildCount();
        String strChildren = "";
        for(int i = 0; i < cnt; i++) {
            strChildren += printTreeRecursive(root.getChild(i)) + SEPARATOR;
        }
        return strRoot + strChildren;
    }

    private String printNode(AccessibilityNodeInfo node) {
        if(node == null || node.getClassName() == null)
            return SEPARATOR;

//        StackTraceElement[] stacktrace = Thread.currentThread().getStackTrace();
//        StackTraceElement se = stacktrace[2];//maybe this number needs to be corrected
//        String methodName = se.getMethodName();
//        Log.w(TAG, "stack: " + methodName);

        String sendStr = node.getClassName().toString()
                + " Text: " + (node.getText() == null ? "null" : node.getText().toString());


        boolean isSpotify = node.getClassName() != null && node.getClassName().toString().endsWith("ImageButton")
                && node.getContentDescription() != null
                && (node.getContentDescription().toString().contains("Play") || node.getContentDescription().toString().contains("Pause"));

        boolean isXiami = node.getClassName() != null && node.getClassName().toString().endsWith("ImageView")
                && node.getViewIdResourceName() != null
                && (node.getViewIdResourceName() == ("fm.xiami.main:id/player_btn_play")
                || node.getViewIdResourceName() == ("fm.xiami.main:id/player_btn_pause"));

        boolean is4sound = node.getClassName() != null && node.getClassName().toString().endsWith("ImageView")
                && node.getViewIdResourceName() != null
                && (node.getViewIdResourceName().equals("com.uramaks.music.player:id/play")
                            || node.getViewIdResourceName().equals("com.uramaks.music.player:id/pause"));

        boolean is1800 = node.getClassName() != null && node.getClassName().toString().endsWith("Button")
                && node.getViewIdResourceName() != null
                && (node.getViewIdResourceName().endsWith("left_minus") || node.getViewIdResourceName().endsWith("left_plus"));

        Log.i(TAG, "ID is " + id + "; isSpotify " + isSpotify + ", isXiami " + isXiami);

        if (isSpotify) {

        //if (node.getText() != null && (node.getText().toString().contains("Play") || node.getText().toString().contains("Pause"))) {
        //if (node.getClassName()!=null && node.getClassName().toString().contains("android.widget.FrameLayout"))

//        String res =  (node.getViewIdResourceName() == null ? "" : node.getViewIdResourceName());
//        if (node.isClickable() &&"com.android.dialer:id/two".equals(res)) {
            try {
                Thread.sleep(4000);
            } catch (Exception e) {

            }
            Log.i(TAG, "PerformAction on node " + node);
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK);

            File dir = new File(Environment.getExternalStorageDirectory().toString() + "/bitmaps");
            Log.i("DataPicker", "create dir " + dir.getPath());
            //dir.mkdirs();
            boolean isDirCreated = dir.exists() || dir.mkdirs();
            if (isDirCreated) {
                String path = dir.getPath();
                Log.d(TAG, "dir created success:" + path);
            } else {
                Log.e(TAG, "dir failed to create");
            }


            if (dir.canWrite()) {
                Log.i(TAG, dir.getPath() + " Can write!!!");
            } else {
                Log.e(TAG, dir.getPath() + " Not writable!!!!!");
            }
            ++id;

            Bundle bitmapBundle = new Bundle();
            node.requestSnapshot(bitmapBundle);
            Bitmap nodeBitmap = (Bitmap) bitmapBundle.get("bitmap");
            if (nodeBitmap == null) {
                Log.w(TAG, "Bitmap is null!!!!!");
            } else {
                storeBitmap(nodeBitmap, "nodebitmaps", Integer.toString(id));
                Log.i(TAG, "Storing......bitmap on the directory, bitmap.count is " + nodeBitmap.getByteCount());
            }
        }
        return sendStr + SEPARATOR;
    }


    private Bitmap getBitmapRecursive(AccessibilityNodeInfo root) {
        if (root == null)
            return null;

        String strRoot = printNode(root);
        Bitmap bt = getBitmap(root);
        if (bt != null)
            return bt;

        int cnt = root.getChildCount();
        String strChildren = "";
        for(int i = 0; i < cnt; i++) {
            bt = getBitmapRecursive(root.getChild(i));
            if (bt != null) {
                return bt;
            }
        }
        return bt;
    }

    private Bitmap getBitmap(AccessibilityNodeInfo node) {
        if(node == null || node.getClassName() == null)
            return null;

        if (node.getClassName() != null && node.getClassName().toString().endsWith("ImageView")
                && node.getViewIdResourceName() != null
                && (node.getViewIdResourceName().contains("player_btn_play") || node.getViewIdResourceName().contains("player_btn_pause"))) {

             try {
                Thread.sleep(2000);
            } catch (Exception e) {

            }
            Log.i(TAG, "PerformAction on node " + node);
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK);

            File dir = new File(Environment.getExternalStorageDirectory().toString() + "/bitmaps");
            Log.i("DataPicker", "create dir " + dir.getPath());
            //dir.mkdirs();
            boolean isDirCreated = dir.exists() || dir.mkdirs();
            if (isDirCreated) {
                String path = dir.getPath();
                Log.d(TAG, "dir created success:" + path);
            } else {
                Log.e(TAG, "dir failed to create");
            }


            if (dir.canWrite()) {
                Log.i(TAG, dir.getPath() + " Can write!!!");
            } else {
                Log.e(TAG, dir.getPath() + " Not writable!!!!!");
            }
            ++id;

            Bundle bitmapBundle = new Bundle();
            node.requestSnapshot(bitmapBundle);
            Bitmap nodeBitmap = (Bitmap) bitmapBundle.get("bitmap");
            if (nodeBitmap == null) {
                Log.w(TAG, "Bitmap is null!!!!!");
            } else {
                storeBitmap(nodeBitmap, "nodebitmaps", Integer.toString(id));
                Log.i(TAG, "Storing......bitmap on the directory, bitmap.count is " + nodeBitmap.getByteCount());
            }
            return nodeBitmap;
        }
        return null;
    }

    @Override
    public void onInterrupt() {
        Log.i(TAG, "Interrupt()  happended.......");
    }


    public static void storeBitmap(Bitmap bitmap, String folder, String imageName) {
        try {
            //create app folder
            File sdcard = Environment.getExternalStorageDirectory();
            File dir = new File(sdcard.getPath() + File.separator + folder);
            String appFolder = "";

            boolean isDirCreated = dir.exists() || dir.mkdirs();

            if (isDirCreated) {
                appFolder = dir.getPath();
                Log.d(TAG, "dir created success:" + appFolder);
            } else {
                Log.e(TAG, "dir failed to create");
            }

            File imageFile = new File(appFolder
                    + File.separator
                    + imageName
                    + ".jpg");

            FileOutputStream outputStream = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream);
//            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);

            outputStream.flush();
            outputStream.close();

        } catch (Throwable e) {
            // Several error may come out with file handling or OOM
            e.printStackTrace();
        }
    }

    @Override
    protected void onServiceConnected() {
        Log.i(TAG, "onServiceConnected ..............");
//        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
//        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
//        this.setServiceInfo(info);
    }

    @Override
    public AccessibilityNodeInfo getRootInActiveWindow() {
        //Log.i(TAG, "getRootInActiveWindow.....");
        return super.getRootInActiveWindow();
    }

    @Override
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        Log.i(TAG, "registerReceiver.....");
        return super.registerReceiver(receiver, filter);
    }


    private void sendMessage(final String path, final String text) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();

                for (Node node: nodes.getNodes()) {
                    MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                            mGoogleApiClient, node.getId(), path, text.getBytes() ).await();
                }
            }
        }).start();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected....");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("Connection Suspended", "Connection suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d("Connection Failed", "Connection failed");
    }

    public void onDataChanged(DataEventBuffer dataEvents) {

    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.i(TAG, "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~Received msg: " + messageEvent.getData());
    }


}
