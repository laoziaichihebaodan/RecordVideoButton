package com.example.recordvideobutton;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;


import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private boolean isSend;
    private int currentTime = 0;
    @SuppressLint("HandlerLeak")
    private final Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1 && isSend){
                currentTime = currentTime +20;
                updateCurrentTime();
                sendDelayMessage();
            }
        }
    };
    private RecordVideoButton recordVideoButton;
    private TextView tvRecordTime;
    private TextView tvState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recordVideoButton = findViewById(R.id.recordVideoButton);
        tvRecordTime = findViewById(R.id.record_time);
        tvState = findViewById(R.id.tv_state);
        recordVideoButton.setOnRecordStateChangedListener(new RecordVideoButton.OnRecordStateChangedListener() {
            @Override
            public void onRecordStart() {
                isSend = true;
                sendDelayMessage();
                tvState.setText("开始录制");
//                Toast.makeText(MainActivity.this, "开始录制", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onRecordPause() {
                isSend = false;
                //手动调用暂停录制
                recordVideoButton.recordPause();
                tvState.setText("暂停录制");
//                Toast.makeText(MainActivity.this, "暂停录制", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onContinueRecord() {
                isSend = true;
                sendDelayMessage();
                tvState.setText("继续录制");
//                Toast.makeText(MainActivity.this, "继续录制", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onRecordStop() {
                isSend = false;
                tvState.setText("结束录制(初始状态)");
//                Toast.makeText(MainActivity.this, "结束录制", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDeleteLastPart(int partsSize, long duration) {
                currentTime = (int) duration;
                updateCurrentTime();
                tvState.setText("删除最后录制片段\npartSize:"+partsSize+"\nduration:"+duration);
//                Toast.makeText(MainActivity.this, "删除最后录制片段，partSize:"+partsSize+"-duration:"+duration, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendDelayMessage(){
        Message message = Message.obtain();
        message.what = 1;
        message.arg1 = currentTime;
        recordVideoButton.setCurrentRecordTime(currentTime);
        handler.sendMessageDelayed(message,1);

    }

    private void updateCurrentTime(){
        float second = currentTime / 1000f;
        tvRecordTime .setText(String.format(Locale.CHINA, "%.1f", second)+"秒");
    }

    public void deleteLastPart(View view) {
        recordVideoButton.deleteLastPartRecord();
    }
}
