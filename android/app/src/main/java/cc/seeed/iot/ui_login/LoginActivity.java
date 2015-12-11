package cc.seeed.iot.ui_login;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.ButterKnife;
import butterknife.InjectView;
import cc.seeed.iot.MyApplication;
import cc.seeed.iot.R;
import cc.seeed.iot.datastruct.User;
import cc.seeed.iot.ui_main.MainScreenActivity;
import cc.seeed.iot.util.Common;
import cc.seeed.iot.webapi.IotApi;
import cc.seeed.iot.webapi.IotService;
import cc.seeed.iot.webapi.model.UserResponse;
import retrofit.Callback;
import retrofit.RetrofitError;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private static final int REQUEST_SIGNUP = 0;
    User user;

    @InjectView(R.id.input_email)
    EditText _emailText;
    @InjectView(R.id.input_password)
    EditText _passwordText;
    @InjectView(R.id.btn_login)
    Button _loginButton;
    @InjectView(R.id.forgot_pwd)
    TextView _forgotPwd;
    @InjectView(R.id.link_server)
    TextView _serverLink;
    @InjectView(R.id.link_signup)
    TextView _signupLink;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.inject(this);

        user = ((MyApplication) getApplication()).getUser();


        _loginButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                login();
            }
        });

        _forgotPwd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ResetActivity.class);
                startActivity(intent);
            }
        });

        _serverLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), SelServerActivity.class);
                startActivity(intent);
            }
        });

        _signupLink.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // Start the Signup activity
                Intent intent = new Intent(getApplicationContext(), SignupActivity.class);
                startActivityForResult(intent, REQUEST_SIGNUP);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh_layout();
    }

    private void refresh_layout() {
        String ota_server_ip = ((MyApplication) getApplication()).getOtaServerIP();
        if (ota_server_ip.equals(Common.OTA_INTERNATIONAL_IP)) {
            _serverLink.setText(getString(R.string.serverOn) + " International" + getString(R.string.change));
        } else if (ota_server_ip.equals(Common.OTA_CHINA_IP)) {
            _serverLink.setText(getString(R.string.serverOn) + " China" + getString(R.string.change));
        } else {
            _serverLink.setText(getString(R.string.serverOn) + " " + ota_server_ip + getString(R.string.change));
        }
    }

    public void login() {
        Log.d(TAG, "Login");

        if (!validate()) {
            onLoginFailed();
            return;
        }

        _loginButton.setEnabled(false);

        final ProgressDialog progressDialog = new ProgressDialog(LoginActivity.this,
                R.style.AppTheme_Dark_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Authenticating...");
        progressDialog.show();

        //Implement your own authentication logic here.
        attemptLogin(progressDialog);

    }

    public void onLoginSuccess(String email, UserResponse userResponse) {
        _loginButton.setEnabled(true);

        user.email = email;
        user.user_key = userResponse.token;
        user.user_id = userResponse.user_id;
        ((MyApplication) getApplication()).setUser(user);
        ((MyApplication) getApplication()).setLoginState(true);
        Intent intent = new Intent(this, MainScreenActivity.class);
        startActivity(intent);

    }

    public void onLoginFailed() {
        Toast.makeText(getBaseContext(), "Login failed", Toast.LENGTH_LONG).show();

        _loginButton.setEnabled(true);
    }

    public boolean validate() {
        boolean valid = true;

        String email = _emailText.getText().toString();
        String password = _passwordText.getText().toString();

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _emailText.setError("enter a valid email address");
            valid = false;
        } else {
            _emailText.setError(null);
        }

        if (password.isEmpty() || password.length() < 6) {
            _passwordText.setError("enter more than six characters");
            valid = false;
        } else {
            _passwordText.setError(null);
        }

        return valid;
    }

    private void attemptLogin(final ProgressDialog progressDialog) {
        final String email = _emailText.getText().toString();
        String password = _passwordText.getText().toString();

        IotApi api = new IotApi();
        IotService iot = api.getService();
        iot.userLogin(email, password, new Callback<UserResponse>() {
            @Override
            public void success(UserResponse userResponse, retrofit.client.Response response) {
                String status = userResponse.status;
                if (status.equals("200")) {
                    onLoginSuccess(email, userResponse);
                } else {
                    onLoginFailed();
                    _emailText.setError(userResponse.msg);
                    _emailText.requestFocus();
                }
                progressDialog.dismiss();
            }

            @Override
            public void failure(RetrofitError error) {
                progressDialog.dismiss();
                onLoginFailed();
//                Toast.makeText(LoginActivity.this, R.string.ConnectServerFail, Toast.LENGTH_LONG).show();
            }
        });
    }
}
