package com.example.myapplication;

import android.bluetooth.BluetoothSocket;
import android.graphics.Color;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class middle_set extends AppCompatActivity {

    // 출력 테스트 delete
    private TextView showResultTextView;

    // 스레드 관련
    private volatile boolean stopThread = false;
    private Thread dataReceivingThread;
    // 블루투스 관련
    private BluetoothSocket bluetoothSocket;    // 데이터 불러오기 위한 소켓
    // handler 설정
    private Handler handler = new Handler(Looper.getMainLooper());
    private Button getDataButton;   // 센서 값을 받아오는 버튼
    private Button backToMainButton;    // 메인 화면으로 돌아가는 버튼
    private ImageView checkPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.middle_setting);

        // Bluetooth 클래스 인스턴스 가져오기
        Bluetooth bluetoothApp = (Bluetooth) getApplicationContext();
        bluetoothSocket = bluetoothApp.getBluetoothSocket();

        getDataButton = findViewById(R.id.getDataButton);   // 센서 값 받아오는 버튼
        backToMainButton = findViewById(R.id.backToMain);   // 메인으로 돌아가는 버튼
        checkPosition = findViewById(R.id.checkPosition);   // 좌 우 차이값을 통해 화면에 표시
        showResultTextView = findViewById(R.id.test); // 테스트용(확인 후 지울것) delete

        // getDataButton 클릭 이벤트 설정
        getDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 블루투스 연결 상태에 따른 처리
                if (bluetoothApp.isBluetoothConnected()) {
                    getDataButton.setText("검사중");
                    getDataButton.setBackgroundColor(Color.RED);
                    startDataReceivingThread();
                } else {
                    Toast.makeText(middle_set.this, "블루투스 연결이 필요합니다.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // backToMainButton 클릭 이벤트 설정
        backToMainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 스레드가 동작 중이면 종료
                stopDataReceivingThread();
                // MainActivity로 이동
                Intent intent = new Intent(middle_set.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
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

                                    if (isCorrectPose(diffValues)) {
                                        checkPosition.setImageResource(R.drawable.middle_weight);
                                    } else if (isRightPose(diffValues)) {
                                        checkPosition.setImageResource(R.drawable.right_weight);
                                    } else if (isLeftPose(diffValues)) {
                                        checkPosition.setImageResource(R.drawable.left_weight);
                                    }

//                                    String finalMessage = resultMessage.toString();
//                                    handler.post(new Runnable() {
//                                        @Override
//                                        public void run() {
//                                            if (!finalMessage.isEmpty()) {
//                                                showResultTextView.setText(finalMessage);
//                                            } else {
//                                                showResultTextView.setText("활성화된 센서가 없습니다.");
//                                            }
//                                        }
//                                    });
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
}
