package com.example.myapplication;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 1;         // 블루투스 활성화 요청 코드
    private static final int REQUEST_BLUETOOTH_CONNECT = 2; // 블루투스 연결 권한 요청 코드 (API 31 이상)
    private static final String DEVICE_NAME = "HC-06"; // 연결할 블루투스 디바이스 이름 (예: HC-06)
    // 블루투스 통신에 사용되는 UUID (블루투스 SPP UUID)
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private BluetoothAdapter bluetoothAdapter;  // 블루투스 어댑터 객체 : 페어링된 디바이스 목록을 가져옴
    private Bluetooth bluetoothApp;  // Bluetooth 클래스 객체 : 블루투스 소켓 전역 관리

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Bluetooth 클래스 인스턴스 가져오기
        bluetoothApp = (Bluetooth) getApplicationContext();

        // Adjust window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // "측정하기" 버튼 클릭 시 측정화면으로 넘어가는 기능
        Button measureButton = findViewById(R.id.MeasureButton);
        measureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bluetoothApp.isBluetoothConnected()) {
                    // 블루투스 연결이 되어 있는 경우 측정 화면으로 이동
                    Intent intent = new Intent(MainActivity.this, MeasureActivity.class);
                    startActivity(intent);
                } else {
                    // 블루투스 연결이 되어 있지 않은 경우 Toast 메시지 표시
                    Toast.makeText(MainActivity.this, "블루투스 연결을 확인해주세요", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 블루투스 연결을 위한 manager 설정
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        Button connectButton = findViewById(R.id.ConnectButton);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check bluetooth activation
                if (!bluetoothAdapter.isEnabled()) {
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                }
                // Check the permission of bluetooth from this app
                else if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_CONNECT);
                }
                else {
                    Toast.makeText(MainActivity.this, "연결을 시도합니다", Toast.LENGTH_SHORT).show();
                    connectToDevice(DEVICE_NAME);
                }
            }
        });

        // SensorCheckButton 클릭 시 middle_set 페이지로 넘어가는 기능 추가
        Button sensorCheckButton = findViewById(R.id.SensorCheckButton);
        sensorCheckButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, middle_set.class);
                // 블루투스 연결 상태를 전달
                intent.putExtra("isBluetoothConnected", bluetoothApp.isBluetoothConnected());
                startActivity(intent);
            }
        });
    }

    // 페어링된 블루투스 디바이스 중 HC-06 블루투스 센서에 연결하는 코드
    private void connectToDevice(String deviceName) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        // Check the counts of devices that can connect to mobile
        if (pairedDevices.size() > 0) {
            // Check the list of devices
            for (BluetoothDevice device : pairedDevices) {
                // If the device name is same as 'HC-06'
                if (device.getName().equals(deviceName)) {
                    // Try to connect
                    try {
                        BluetoothSocket bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                        bluetoothSocket.connect();
                        Toast.makeText(this, "블루투스 장치와 연결되었습니다: " + deviceName, Toast.LENGTH_SHORT).show();

                        // BluetoothSocket을 Bluetooth 클래스에 저장
                        bluetoothApp.setBluetoothSocket(bluetoothSocket);

                        return;
                    }
                    catch (IOException e) {
                        Log.e("Bluetooth", "연결 중 오류 발생", e);
                        Toast.makeText(this, "연결 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
            Toast.makeText(this, "장치를 찾을 수 없습니다: " + deviceName, Toast.LENGTH_SHORT).show();
        }
        // 찾은 device 수가 0 이라면
        else {
            Toast.makeText(this, "페어링된 장치가 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }
}
