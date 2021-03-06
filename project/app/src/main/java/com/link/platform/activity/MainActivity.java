package com.link.platform.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;

import com.link.platform.R;
import com.link.platform.item.WiFiItem;
import com.link.platform.message.BaseMessage;
import com.link.platform.message.MessageCenter;
import com.link.platform.message.MessageListenerDelegate;
import com.link.platform.message.MessageTable;
import com.link.platform.message.MessageWithObject;
import com.link.platform.ui.adapter.ConversationAdapter;
import com.link.platform.ui.component.LoadMoreListView;
import com.link.platform.util.UIHelper;
import com.link.platform.util.Utils;
import com.link.platform.wifi.wifi.WiFiManager;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends Activity implements MessageListenerDelegate , AdapterView.OnItemClickListener, LoadMoreListView.OnRefreshListener {

    public final static String TAG = "MainActivity";

    private LoadMoreListView conversation_listview;
    private ConversationAdapter adapter;

    private EditText search_bar;
    private List<WiFiItem> list;
    private WiFiItem current_wifi;
    private ProgressDialog d;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_conversation_list);

        conversation_listview = (LoadMoreListView)findViewById(R.id.conversation_list);
        conversation_listview.setPullRefreshEnable(true);
        conversation_listview.setOnRefreshListener(this);

        adapter = new ConversationAdapter(this);
        adapter.setData(null);
        conversation_listview.setAdapter(adapter);
        conversation_listview.setOnItemClickListener(this);
        search_bar = (EditText)findViewById(R.id.search_bar);

        search_bar.addTextChangedListener(mTextWatcher);

        search_bar.clearFocus();
        conversation_listview.requestFocus();

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        d = new ProgressDialog(this);
        d.setTitle("正在搜索附近的房间");
        d.setMessage("请等待...");
    }

    @Override
    public void onResume() {
        super.onResume();

        MessageCenter.getInstance().registerListener(this , MessageTable.MSG_OPEN_WIFI_FINISH );
        MessageCenter.getInstance().registerListener(this , MessageTable.MSG_CLOSE_WIFI_FINISH );
        MessageCenter.getInstance().registerListener(this , MessageTable.MSG_GET_SCAN_RESULT );
        MessageCenter.getInstance().registerListener(this , MessageTable.MSG_CONNECT_WIFI_FINISH );

        WiFiManager.getInstance().setContext(this);
        WiFiManager.getInstance().openWiFi();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        MessageCenter.getInstance().removeListener(this);
        WiFiManager.getInstance().clear();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        MenuItem makesure = menu.findItem( R.id.action_create );

        makesure.setShowAsAction( MenuItem.SHOW_AS_ACTION_IF_ROOM );
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_create) {
            Intent intent = new Intent(this , CreateRoomActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private TextWatcher mTextWatcher = new TextWatcher() {

        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void afterTextChanged(Editable s) {
            quickSearch(s.toString());
        }
    };

    private void quickSearch(String key) {
        adapter.getFilter().filter(key);
    }

    @Override
    public void onListenerExit() {

    }

    @Override
    public void getMessage(BaseMessage message) {
        String id = message.getMsgId();
        MessageWithObject msg = (MessageWithObject)message;
        if( id.equals( MessageTable.MSG_OPEN_WIFI_FINISH) ) {
            boolean result = Boolean.valueOf( msg.getObject().toString() );
            if( result ) {
                WiFiManager.getInstance().StartScan();
                d.show();
            }
        }
        else if( id.equals( MessageTable.MSG_GET_SCAN_RESULT ) ) {
            List<WiFiItem> list = new ArrayList<WiFiItem>();
            List<ScanResult> wifi_list = WiFiManager.getInstance().GetWifiList();
            for( ScanResult wifi : wifi_list ) {
                String SSID = wifi.SSID;
                boolean isLock = false;
                if( wifi.capabilities.contains("WEP") || wifi.capabilities.contains("WPA" ) ) {
                    isLock = true;
                }
                list.add( new WiFiItem( SSID, isLock ) );
            }
            this.list = list;
            adapter.setData( list );
            adapter.notifyDataSetChanged();
            d.dismiss();
            conversation_listview.onRefreshComplete();
        }
        else if( id.equals( MessageTable.MSG_CLOSE_WIFI_FINISH) ) {

        }
        else if( id.equals( MessageTable.MSG_CONNECT_WIFI_FINISH) ) {
            boolean result = Boolean.valueOf( msg.getObject().toString() );
            if( result && current_wifi != null ) {
                Intent intent = new Intent(this, ConversationActivity.class);
                intent.putExtra(ConversationActivity.PARAM_ROOM_NAME , current_wifi.name.substring(6) );
                intent.putExtra(ConversationActivity.PARAM_IS_HOST, false);
                startActivity(intent);
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

        final WiFiItem item = list.get(i-1);
        // TODO password dialog
        Log.d(TAG, "Join " + item.name);
        current_wifi = item;
        if( item.isLock ) {

            View layout = LayoutInflater.from(this).inflate(R.layout.ui_password_input_dialog, null);
            final EditText edit_password = (EditText)layout.findViewById(R.id.dialog_password);
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("请输入")
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .setView(layout)
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            String password = edit_password.getText().toString();
                            if( password.length() < 8 ) {
                                UIHelper.makeToast("密码长度不足8位");
                                return;
                            }
                            WiFiManager manager = WiFiManager.getInstance();
                            manager.addNetWork( manager.CreateWifiConfiguration(item.name , password));
                            dialogInterface.dismiss();
                        }
                    })
                    .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    })
                    .show();
        }
        else {
            WiFiManager manager = WiFiManager.getInstance();
            manager.addNetWork( manager.CreateWifiConfiguration(item.name , ""));
        }

    }

    @Override
    public void onRefresh() {
        WiFiManager.getInstance().StartScan();
    }
}
