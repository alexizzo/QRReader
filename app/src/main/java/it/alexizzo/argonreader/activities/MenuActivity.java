package it.alexizzo.argonreader.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.zxing.qrcode.QRCodeReader;

import it.alexizzo.argonreader.R;

/**
 * Created by alessandro on 08/08/16.
 */
public class MenuActivity extends Activity {

    private Button mCameraActivityButton, mListActivityButton, mZxhingButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        mListActivityButton = (Button) findViewById(R.id.b_list);
        mCameraActivityButton = (Button) findViewById(R.id.b_camera);
        mZxhingButton = (Button) findViewById(R.id.b_zxhing);
        mListActivityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MenuActivity.this, ListActivity.class);
                startActivity(i);
                overridePendingTransition(R.anim.in_to_up, R.anim.null_anim);

            }
        });
        mCameraActivityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MenuActivity.this, CameraActivity.class);
                startActivity(i);
                overridePendingTransition(R.anim.in_to_up, R.anim.null_anim);
            }
        });
        mZxhingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent("com.google.zxing.client.android.SCAN");
                i.setPackage(getPackageName());
                i.putExtra("SCAN_MODE", "SCAN_MODE");
                startActivityForResult(i, 30);
                overridePendingTransition(R.anim.in_to_up, R.anim.null_anim);
            }
        });
    }


    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == 0) {
            if (resultCode == RESULT_OK) {
                String contents = intent.getStringExtra("SCAN_RESULT");
                String format = intent.getStringExtra("SCAN_RESULT_FORMAT");
                // Handle successful scan
            } else if (resultCode == RESULT_CANCELED) {
                // Handle cancel
            }
        }
    }
}
