package com.example.jianxu.ppppaccwear;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.support.wearable.view.WearableFrameLayout;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MainActivity extends Activity
        implements MessageApi.MessageListener,
        GoogleApiClient.ConnectionCallbacks {

    private GoogleApiClient mGoogleApiClient;
    private static final String TAG = "Receiver";
    private static final String WEAR_MESSAGE_PATH = "/message";
    private static final String SEPARATOR = "||";

    private HashMap<String,String> mHashTV;

    private Bitmap mBitmap;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .build();
        mGoogleApiClient.connect();

        mHashTV = new HashMap<>();

        final LinearLayout stub = (LinearLayout) findViewById(R.id.watch_view_stub);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        TextView view = new TextView(this);
        view.setText("Click me,");
        stub.addView(view, params);
    }

    /**
     * @param encodedString
     * @return bitmap (from given string)
     */
    public Bitmap StringToBitMap(String encodedString){
        try {
            byte [] encodeByte=Base64.decode(encodedString, Base64.DEFAULT);
            Bitmap bitmap= BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
            return bitmap;
        } catch(Exception e) {
            e.getMessage();
            return null;
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "onConnectionSuspended");
    }

    @Override
    public void onMessageReceived(final MessageEvent messageEvent) {
        Log.i(TAG,"Received message.~~~~~~~~~~~~~~~~~~~~~~~" + messageEvent.getData().length);


        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                //Log.i(TAG, "Received data: " + (new String(messageEvent.getData())));
//
//                if (messageEvent.getPath().equalsIgnoreCase(WEAR_MESSAGE_PATH)) {
//                    Log.i(TAG, "Received data: " + (new String(messageEvent.getData())));
//                }
//                analyzeDOM(new String(messageEvent.getData()));
//                constructUI();
//
//
//            }

            @Override
            public void run() {
                //Log.i(TAG, "Received data: " + (new String(messageEvent.getData())));

                //analyzeDOM(new String(messageEvent.getData()));
                mBitmap = StringToBitMap(new String(messageEvent.getData()));
                constructBitmap();
            }
        });
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

    private void analyzeDOM(String tree) {
        int i = 0;
        mHashTV.clear();
        for (; i < tree.length();) {
            int offset = tree.indexOf(SEPARATOR, i);
            if (offset != i) {
                String componentStr = new String(tree.substring(i, offset));
                //Log.i(TAG, "Substr: " + componentStr);


                int space = componentStr.indexOf(" ");
                int textBeg = componentStr.indexOf("Text: ");

                String textStr = componentStr.substring(textBeg + 6);

                Log.i(TAG, "Component: " + componentStr.substring(0, space) + " text: " + textStr);

                if (textStr != "null" && componentStr.contains("TextView")) {
                    Log.i(TAG, "here.......");
                    // Should be the whole component as it is unique
                    mHashTV.put(componentStr, textStr);
                }
            }
            i = offset + SEPARATOR.length();
        }
    }

    private void constructUI() {
        final LinearLayout stub = (LinearLayout) findViewById(R.id.watch_view_stub);
        stub.removeAllViews();

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);

//        Button btn = new Button(getApplicationContext());
//        btn.setText("Button Stone");
//        stub.addView(btn, params);

        Iterator it = mHashTV.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            Log.i(TAG, "Hashmap: key " + pair.getKey() + " value: " + pair.getValue());

            TextView textView = new TextView(getApplicationContext());
            textView.setText(pair.getValue().toString());
            stub.addView(textView, params);
        }

    }


    private void constructBitmap() {
        final LinearLayout stub = (LinearLayout) findViewById(R.id.watch_view_stub);
        stub.removeAllViews();

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        ImageView imageView = new ImageView(getApplicationContext());
        imageView.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.i(TAG,"Sending message ~~~~~~~~~~~~~~~~~~~~~~~");
                        sendMessage(WEAR_MESSAGE_PATH, "this is the message from the smartwatch");
                    }
                }
        );
        imageView.setImageBitmap(mBitmap);
        stub.addView(imageView, params);
    }

}
