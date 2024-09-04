package com.example.myapplication;

import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class MeasureActivity extends AppCompatActivity {

    private NumberPicker hourPicker;
    private NumberPicker minutePicker;
    private NumberPicker secondPicker;
    private TextView timerTextView;
    private CountDownTimer countDownTimer;

    // 자세별 시간 측정을 위한 변수
    private long correctPoseTime = 0;
    private long rightPoseTime = 0;
    private long leftPoseTime = 0;
    private long lastTimestamp;

    // 스레드 관련
    private volatile boolean stopThread = false;
    private Thread dataReceivingThread;
    // 블루투스 관련
    private BluetoothSocket bluetoothSocket;    // 데이터 불러오기 위한 소켓
    // handler 설정
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.measure_page);

        // Bluetooth 클래스 인스턴스 가져오기
        Bluetooth bluetoothApp = (Bluetooth) getApplicationContext();
        bluetoothSocket = bluetoothApp.getBluetoothSocket();

        hourPicker = findViewById(R.id.hourPicker);
        minutePicker = findViewById(R.id.minutePicker);
        secondPicker = findViewById(R.id.secondPicker);
        timerTextView = findViewById(R.id.timerTextView);

        // NumberPicker 설정
        hourPicker.setTextColor(Color.BLACK);
        minutePicker.setTextColor(Color.BLACK);
        secondPicker.setTextColor(Color.BLACK);

        // NumberPicker 설정
        hourPicker.setMinValue(0);
        hourPicker.setMaxValue(23);
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);
        secondPicker.setMinValue(0);
        secondPicker.setMaxValue(59);

        // "측정 시작" 버튼 클릭 시 선택된 시간 값 읽어오기
        Button startMeasureButton = findViewById(R.id.startMeasureButton);
        startMeasureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int hours = hourPicker.getValue();
                int minutes = minutePicker.getValue();
                int seconds = secondPicker.getValue();

                if (hours == 0 && minutes == 0 && seconds == 0){
                    Toast.makeText(MeasureActivity.this, "측정할 시간을 설정해주세요.", Toast.LENGTH_SHORT).show();
                }
                else{
                    startMeasureButton.setText("측정중");
                    startMeasureButton.setBackgroundColor(Color.RED);
                    startMeasureButton.setEnabled(false);
                    // 선택된 시간을 밀리초로 변환
                    long totalTimeInMillis = (hours * 3600 + minutes * 60 + seconds) * 1000;

                    // 타이머가 이미 실행 중이면 취소
                    if (countDownTimer != null) {
                        countDownTimer.cancel();
                    }

                    // CountDownTimer 생성 및 시작
                    countDownTimer = new CountDownTimer(totalTimeInMillis, 1000) {
                        @Override
                        public void onTick(long millisUntilFinished) {
                            // 남은 시간을 시, 분, 초로 변환하여 표시
                            int hours = (int) (millisUntilFinished / 3600000);
                            int minutes = (int) (millisUntilFinished % 3600000) / 60000;
                            int seconds = (int) (millisUntilFinished % 60000) / 1000;
                            timerTextView.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
                        }

                        @Override
                        public void onFinish() {
                            timerTextView.setText("00:00:00");
                            Toast.makeText(MeasureActivity.this, "타이머가 종료되었습니다.", Toast.LENGTH_SHORT).show();
                            moveToResultActivity();
                        }
                    }.start();

                    Toast.makeText(MeasureActivity.this, "타이머 시작: " + hours + "시간 " + minutes + "분 " + seconds + "초", Toast.LENGTH_SHORT).show();
                    // "측정 시작" 버튼 클릭 시 센서 데이터 수신 스레드 시작
                    startDataReceivingThread();
                }
            }
        });

        // "돌아가기" 버튼 클릭 시 현재 액티비티 종료
        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 타이머가 실행 중이면 취소
                if (countDownTimer != null) {
                    countDownTimer.cancel();
                }
                // 스레드 종료
                stopDataReceivingThread();
                finish();
            }
        });

        // "결과 확인" 버튼 클릭 시 결과 확인 로직 추가
        Button checkResultButton = findViewById(R.id.checkResultButton);
        checkResultButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 결과 확인 로직 추가
                if (countDownTimer == null) {
                    Toast.makeText(MeasureActivity.this, "측정된 값이 없습니다", Toast.LENGTH_SHORT).show();
                } else {
                    moveToResultActivity();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 액티비티 종료 시 스레드 종료
        stopDataReceivingThread();
    }

    private void startDataReceivingThread() {
        stopThread = false;  // 스레드 시작 전에 stopThread 초기화
        dataReceivingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream inputStream = null;
                StringBuilder dataBuffer = new StringBuilder(); // 데이터 조각들을 저장할 임시 버퍼
                try {
                    inputStream = bluetoothSocket.getInputStream();
                    byte[] buffer = new byte[1024];  // 버퍼 크기 설정

                    lastTimestamp = System.currentTimeMillis(); // 초기 타임스탬프 설정

                    while (!stopThread) {
                        int bytes = inputStream.read(buffer);  // 데이터 읽기
                        if (bytes > 0) {
                            // 바이트를 문자열로 변환하고 임시 버퍼에 추가
                            String readMessage = new String(buffer, 0, bytes, StandardCharsets.UTF_8);
                            dataBuffer.append(readMessage);

                            // 전체 메시지에서 처리 가능한 데이터 찾기
                            int endIndex;
                            while ((endIndex = dataBuffer.indexOf("\n")) != -1) {  // 개행 문자를 끝으로 가정
                                String completeData = dataBuffer.substring(0, endIndex).trim();
                                dataBuffer.delete(0, endIndex + 1); // 처리된 데이터 제거

                                try {
                                    // 메시지를 파싱하여 int 배열로 변환
                                    int[] sensorValues = parseSensorData(completeData);
                                    StringBuilder resultMessage = new StringBuilder();

                                    // 센서의 각 위치별 평균값 계산
                                    // 센서를 총 6구역으로 나누고 아래부터 좌, 우 순으로
                                    // 왼쪽 : 0,2,4 / 오른쪽 1,3,5
                                    int[] meanValues = new int[6];
                                    meanValues[0] = (sensorValues[0]+sensorValues[1]+sensorValues[2]+sensorValues[3]+sensorValues[4])/5;
                                    meanValues[1] = (sensorValues[26]+sensorValues[27]+sensorValues[28]+sensorValues[29]+sensorValues[30])/5;
                                    meanValues[2] = (sensorValues[5]+sensorValues[6]+sensorValues[7]+sensorValues[8]+sensorValues[9]+sensorValues[11]+sensorValues[13])/7;
                                    meanValues[3] = (sensorValues[19]+sensorValues[21]+sensorValues[22]+sensorValues[23]+sensorValues[24]+sensorValues[25]+sensorValues[17])/7;
                                    meanValues[4] = (sensorValues[10]+sensorValues[12]+sensorValues[14])/3;
                                    meanValues[5] = (sensorValues[16]+sensorValues[18]+sensorValues[20])/3;

                                    int[] diffValues = new int[3];
                                    diffValues[0] = meanValues[0]-meanValues[1];
                                    diffValues[1] = meanValues[2]-meanValues[3];
                                    diffValues[2] = meanValues[4]-meanValues[5];

                                    // delete
                                    resultMessage.append("0-1: " + diffValues[0] + "\n"
                                            + "2-3: " + diffValues[1] + "\n"
                                            + "4-5: " + diffValues[2] + "\n"
                                    );

                                    // 현재 시간과 이전 타임스탬프를 사용하여 경과 시간 계산
                                    long currentTime = System.currentTimeMillis();
                                    long elapsedTime = currentTime - lastTimestamp;
                                    lastTimestamp = currentTime;

                                    if (isCorrectPose(diffValues)) {
                                        correctPoseTime += elapsedTime;
                                    } else if (isRightPose(diffValues)) {
                                        rightPoseTime += elapsedTime;
                                    } else if (isLeftPose(diffValues)) {
                                        leftPoseTime += elapsedTime;
                                    }
                                } catch (Exception e) {
                                    // 파싱 중 예외 처리
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        dataReceivingThread.start();
    }

    private void stopDataReceivingThread() {
        stopThread = true;
        if (dataReceivingThread != null) {
            try {
                dataReceivingThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            dataReceivingThread = null;
        }
    }

    private int[] parseSensorData(String data) {
        String[] strings = data.replaceAll("[\\[\\]]", "").split(",");
        int[] result = new int[strings.length];
        for (int i = 0; i < strings.length; i++) {
            try {
                result[i] = Integer.parseInt(strings[i].trim());
            } catch (NumberFormatException e) {
                Log.e("ParsingError", "Failed to parse integer from data: " + strings[i], e);
                result[i] = 0;
            }
        }
        return result;
    }

    private boolean isCorrectPose(int[] diffValues) {
        for (int diff : diffValues) {
            if (diff < -33 || diff > 25) {
                return false;
            }
        }
        return true;
    }

    private boolean isLeftPose(int[] diffValues) {
        for (int diff : diffValues) {
            if (diff >= 25) {
                return true;
            }
        }
        return false;
    }

    private boolean isRightPose(int[] diffValues) {
        for (int diff : diffValues) {
            if (diff < -33) {
                return true;
            }
        }
        return false;
    }

    private void moveToResultActivity() {
        stopDataReceivingThread(); // 스레드를 종료합니다.
        Intent intent = new Intent(MeasureActivity.this, ResultActivity.class);
        intent.putExtra("correctPoseTime", correctPoseTime / 1000); // 초 단위로 변환
        intent.putExtra("rightPoseTime", rightPoseTime / 1000); // 초 단위로 변환
        intent.putExtra("leftPoseTime", leftPoseTime / 1000); // 초 단위로 변환
        startActivity(intent);
    }
}
