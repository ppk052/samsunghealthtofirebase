package com.example.samsunghealthtofirebase;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.samsung.android.sdk.health.data.HealthDataService;
import com.samsung.android.sdk.health.data.HealthDataStore;
import com.samsung.android.sdk.health.data.data.HealthDataPoint;
import com.samsung.android.sdk.health.data.permission.AccessType;
import com.samsung.android.sdk.health.data.permission.Permission;
import com.samsung.android.sdk.health.data.request.DataType;
import com.samsung.android.sdk.health.data.request.DataTypes;
import com.samsung.android.sdk.health.data.request.Ordering;
import com.samsung.android.sdk.health.data.request.ReadDataRequest;
import com.samsung.android.sdk.health.data.response.DataResponse;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.function.Consumer;


public class MainActivity extends AppCompatActivity {

    Button button;
    TextView textView;
    LocalDateTime heartDate;
    Float heartRate;
    FirebaseDatabase database;
    DatabaseReference heartRateRef;
    DatabaseReference heartDateRef;
    DatabaseReference checkDateRef;
    Boolean sync = false;

    private final Set<Permission> permissions = Set.of(Permission.of(DataTypes.HEART_RATE, AccessType.READ));
    private Handler syncHandler;
    private static final long syncInterval = 5000;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.mainText);
        button = findViewById(R.id.getButton);
        button.setText("동기화시작");
        button.setOnClickListener(view -> reconnect());
        database = FirebaseDatabase.getInstance();
        heartRateRef = database.getReference("HeartRate/HeartRate");
        heartDateRef = database.getReference("HeartRate/HeartDate");
        checkDateRef = database.getReference("HeartRate/CheckDate");
        syncHandler = new Handler(Looper.getMainLooper());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private final Runnable heartRateSuncTask = new Runnable() {
        @Override
        public void run() {
            if(sync) {
                connect();
            } else {
                syncHandler.removeCallbacks(this);
                return;
            }
            syncHandler.postDelayed(this, syncInterval);
        }
    };


    public void connect() {
        try{
            HealthDataStore healthStore = HealthDataService.getStore(this);
            checkPermissions(healthStore);
        }
        catch (Exception e){
            textView.setText(e.toString());
        }
    }

    public void reconnect() {
        if(button.getText() == "동기화시작"){
            button.setText("동기화중...");
            sync = true;
            syncHandler.post(heartRateSuncTask);
        } else {
            button.setText("동기화시작");
            sync = false;
            syncHandler.removeCallbacks(heartRateSuncTask);
        }
    }

    private void checkPermissions(HealthDataStore healthStore) {
        // 1. 성공 콜백 (onSuccess) 정의
        Consumer<Set<Permission>> onSuccess = grantedPermissions -> {
            // 이 람다식은 UI 스레드에서 실행됩니다.
            if (grantedPermissions.containsAll(permissions)) {
                readHeartRateData(healthStore);
            } else {
                textView.setText("2. 권한 부족. 사용자에게 요청 팝업 표시.");
                requestPermissions(healthStore);
            }
        };

        // 2. 실패 콜백 (onError) 정의
        Consumer<Throwable> onError = e -> {
            textView.setText("2. 권한 조회 실패: " + e.getMessage());
        };

        // 3. setCallback 호출 (Looper.getMainLooper() 사용)
        healthStore.getGrantedPermissionsAsync(permissions)
                .setCallback(
                        Looper.getMainLooper(), // 콜백을 UI 스레드에서 실행
                        onSuccess,             // 성공 시 로직
                        onError                // 실패 시 로직
                );
    }

    private void requestPermissions(HealthDataStore healthStore) {

        // 1. 성공 콜백 (onSuccess) 정의
        Consumer<Set<Permission>> onSuccess = grantedPermissions -> {
            // 이 람다식은 UI 스레드에서 실행됩니다.
            if (grantedPermissions.containsAll(permissions)) {
                textView.setText("3. 권한 요청 승인됨. 데이터 읽기 시도.");
                readHeartRateData(healthStore); // 데이터 읽기 호출
            } else {
                textView.setText("3. 권한 요청 거부됨. 데이터 접근 불가.");
                Toast.makeText(MainActivity.this, "데이터 접근 권한이 필요합니다.", Toast.LENGTH_LONG).show();
            }
        };

        // 2. 실패 콜백 (onError) 정의
        Consumer<Throwable> onError = e -> {
            textView.setText("3. 권한 요청 실패: " + e.getMessage());
            //Log.e(TAG, "권한 요청 중 오류", e);
        };

        // 3. setCallback 호출 (Looper.getMainLooper() 사용)
        healthStore.requestPermissionsAsync(permissions, this)
                .setCallback(
                        Looper.getMainLooper(),
                        onSuccess,
                        onError
                );
    }

    private void readHeartRateData(HealthDataStore healthStore) {
        try {
            // 1. Raw use of parameterized class 'ReadDataRequest' 경고 해결을 위해 타입 명시
            ReadDataRequest readRequest = DataTypes.HEART_RATE.getReadDataRequestBuilder()
                    .setOrdering(Ordering.DESC)
                    .setPageSize(1)
                    .build();
            // 2. 데이터 읽기 요청 및 비동기 콜백 설정
            // 2.1. 성공 콜백 (onSuccess) 정의
            Consumer<DataResponse<HealthDataPoint>> onSuccess = response -> {
                String count = "0";
                try {
                    // 💡 getDataList() 사용으로 수정
                    count = String.valueOf(response.getDataList().size());
                } finally {
                    // ❌ close() 메소드를 제거합니다.
                    // DataResponse에 close()가 없으므로 호출하지 않습니다.
                }
                HealthDataPoint data = response.getDataList().get(0);
                if(heartDate != data.getEndLocalDateTime()){
                    heartDate = data.getEndLocalDateTime();
                    heartRate = data.getValue(DataType.HeartRateType.HEART_RATE);
                    heartRateRef.setValue(heartRate);
                    heartDateRef.setValue(heartDate.toString());
                }
                checkDateRef.setValue(LocalDateTime.now().toString());
            };
            // 2.2. 실패 콜백 (onError) 정의
            Consumer<Throwable> onError = e -> {
                textView.setText("4. 데이터 읽기 실패: " + e.getMessage());
                //Log.e(TAG, "데이터 읽기 중 오류", e);
            };
            // 2.3. setCallback 호출
            healthStore.readDataAsync(readRequest)
                    .setCallback(
                            Looper.getMainLooper(),
                            onSuccess,
                            onError
                    );
        } catch (Exception e) {
            textView.setText("Read Request Exception: " + e.getMessage());
        }
    }
}