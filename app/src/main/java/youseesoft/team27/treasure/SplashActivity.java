package youseesoft.team27.treasure;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set splash screen layout
        setContentView(R.layout.activity_splash);

        // Delay and navigate to LoginActivity
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();  // finish splash so user can't go back
        }, 2500); // 2500 milliseconds = 2.5 seconds
    }
}
