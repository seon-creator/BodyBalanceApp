package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class ResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pose_result);

        TextView positionATime = findViewById(R.id.positionATime);
        TextView positionBTime = findViewById(R.id.positionBTime);
        TextView positionCTime = findViewById(R.id.positionCTime);
        TextView summaryEvaluation = findViewById(R.id.summaryEvaluation);

        ProgressBar progressBarA = findViewById(R.id.progressBarA);
        ProgressBar progressBarB = findViewById(R.id.progressBarB);
        ProgressBar progressBarC = findViewById(R.id.progressBarC);

        // 메인 화면으로 돌아가는 버튼
        Button backToHomeButton = findViewById(R.id.backToHomeButton);
        backToHomeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ResultActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        // Intent에서 데이터 가져오기
        long correctPoseTime = getIntent().getLongExtra("correctPoseTime", 0);
        long rightPoseTime = getIntent().getLongExtra("rightPoseTime", 0);
        long leftPoseTime = getIntent().getLongExtra("leftPoseTime", 0);

        // 전체 시간 계산
        long totalTime = correctPoseTime + rightPoseTime + leftPoseTime;

        // 비율 계산
        int correctPosePercentage = (int) ((correctPoseTime * 100) / totalTime);
        int rightPosePercentage = (int) ((rightPoseTime * 100) / totalTime);
        int leftPosePercentage = (int) ((leftPoseTime * 100) / totalTime);

        // 가져온 데이터로 TextView 설정
        positionATime.setText(String.format("올바른 자세 시간: %d 초 (%d%%)", correctPoseTime, correctPosePercentage));
        positionBTime.setText(String.format("오른쪽 쏠린 시간: %d 초 (%d%%)", rightPoseTime, rightPosePercentage));
        positionCTime.setText(String.format("왼쪽 쏠린 시간: %d 초 (%d%%)", leftPoseTime, leftPosePercentage));

        // ProgressBar 설정
        progressBarA.setProgress(correctPosePercentage);
        progressBarB.setProgress(rightPosePercentage);
        progressBarC.setProgress(leftPosePercentage);

        // 평가 문구 설정
        String evaluation;
        if (correctPosePercentage >= 80) {
            evaluation = "바른 자세를 잘 유지하고 있습니다.\n";
            if (rightPosePercentage > leftPosePercentage) {
                evaluation += " 추가로 오른쪽으로 더 치우쳐 있습니다.";
            } else if (leftPosePercentage > rightPosePercentage) {
                evaluation += " 추가로 왼쪽으로 더 치우쳐 있습니다.";
            }
        } else if (correctPosePercentage >= 60) {
            evaluation = "비교적 자세를 잘 유지하고 있습니다\n";
            if (rightPosePercentage > leftPosePercentage) {
                evaluation += " 추가로 오른쪽으로 더 치우쳐 있습니다.";
            } else if (leftPosePercentage > rightPosePercentage) {
                evaluation += " 추가로 왼쪽으로 더 치우쳐 있습니다.";
            }
        } else {
            evaluation = "바른 자세 유지를 위한 노력이 필요합니다\n";
            if (rightPosePercentage > leftPosePercentage) {
                evaluation += " 추가로 오른쪽으로 더 치우쳐 있습니다.";
            } else if (leftPosePercentage > rightPosePercentage) {
                evaluation += " 추가로 왼쪽으로 더 치우쳐 있습니다.";
            }
        }

        summaryEvaluation.setText(evaluation);
    }
}
