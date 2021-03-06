package com.link.platform.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.link.platform.R;
import com.link.platform.activity.setting.LocalSetting;
import com.link.platform.util.UIHelper;

public class LoginActivity extends Activity {

    private EditText input_name;
    private Button btn_ensure;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        input_name = (EditText)this.findViewById(R.id.activity_login_name_edit);
        btn_ensure = (Button)this.findViewById(R.id.activity_submit_button);

        btn_ensure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = input_name.getText().toString();
                if( name.equals( "" ) ) {
                    UIHelper.makeToast("请输入您的昵称");
                    return;
                }
                LocalSetting.getInstance().setLocalName( name );

                Intent intent = new Intent(LoginActivity.this , MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
        input_name.setText(LocalSetting.getInstance().getLocalName());

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
