package com.x.otg_ddmo.chooser;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import com.tencent.bugly.crashreport.BuglyLog;
import com.x.otg_ddmo.R;

import java.util.ArrayList;

public class FileActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";   //记录标识
    public static final int RESULTCODE = 9;
    @Override
    protected void onDestroy() {
        mDirectoryFragment.onFragmentDestroy();
        super.onDestroy();
    }

    private FragmentManager fragmentManager = null;
    private FragmentTransaction fragmentTransaction = null;
    private DirectoryFragment mDirectoryFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file);


       /* toolbar = (Toolbar) findViewById(R.id.tool_bar);
        toolbar.setTitle("Directory");
        setSupportActionBar(toolbar);*/
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        fragmentManager = getSupportFragmentManager();
        fragmentTransaction = fragmentManager.beginTransaction();

        mDirectoryFragment = new DirectoryFragment();
        mDirectoryFragment.setDelegate(new DirectoryFragment.DocumentSelectActivityDelegate() {

            @Override
            public void startDocumentSelectActivity() {

            }

            @Override
            public void didSelectFiles(DirectoryFragment activity,
                                       ArrayList<String> files) {
                BuglyLog.d(TAG,"select files num:"+files.size());
                        ((FileActivity) activity.getActivity()).CallResult(files.get(0));
                //FileActivity.this.CallResult(files.get(0));
               /* Intent intent = new Intent();
                intent.putExtra("PATH", files.get(0));
                mDirectoryFragment.onActivityResult(RESULTCODE, RESULTCODE, intent);
                mDirectoryFragment.finishFragment();*/
                //mDirectoryFragment.showErrorBox(files.get(0).toString());
            }

            @Override
            public void updateToolBarName(String name) {
                /*toolbar.setTitle(name);*/

            }
        });
        fragmentTransaction.add(R.id.fragment_container, mDirectoryFragment, "" + mDirectoryFragment.toString());
        fragmentTransaction.commit();

    }

    public void CallResult(String path){
        Intent intent = new Intent();
        intent.putExtra("PATH", path);
        setResult(RESULTCODE, intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (mDirectoryFragment.onBackPressed_()) {
            super.onBackPressed();
        }
    }

}
