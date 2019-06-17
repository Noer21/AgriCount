package com.example.agricount;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private EditText username, password;
    private String usernameVal, passwordVal;
    private Button signin;
    private ProgressDialog progressBar;
    private RequestQueue mQueue;
    private String url = "http://agricount.codepanda.id/users/login";

    private SharedPreferences sharedPreferences;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.getSupportActionBar().hide();

        sharedPreferences = getSharedPreferences("mysharedpreference", MODE_PRIVATE);

        mQueue = Volley.newRequestQueue(this);
        username = findViewById(R.id.username);
        password = findViewById(R.id.password);

        signin = findViewById(R.id.signin);
        signin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptLogin();
            }
        });
    }

    private void login(){
        // creating progress bar dialog
        progressBar = new ProgressDialog(this);
        progressBar.setCancelable(true);
        progressBar.setMessage("Memuat Data ...");
        progressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressBar.show();

        StringRequest req = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            Integer status = jsonObject.getInt("status");
                            if (status == 200){
                                JSONObject data = jsonObject.getJSONObject("data");
                                progressBar.dismiss();

                                Integer id = data.getInt("id");
                                sharedPreferences.edit().putInt("userID", id).apply();
                                goToHome();

                            }
                            else{
                                Toast.makeText(MainActivity.this, "Gagal Masuk, Periksa Username dan Password Anda!", Toast.LENGTH_SHORT).show();
                                progressBar.dismiss();
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(MainActivity.this, "Gagal Masuk, Periksa Jaringan Anda!", Toast.LENGTH_SHORT).show();
                            progressBar.dismiss();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                        Toast.makeText(MainActivity.this, "Gagal Masuk, Periksa Jaringan Anda.", Toast.LENGTH_SHORT).show();
                        progressBar.dismiss();
                    }
                })
        {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("username", usernameVal);
                params.put("password", passwordVal);
                return params;
            }
        };

        mQueue.add(req);
    }

    private void attemptLogin() {
        // Reset errors.
        username.setError(null);
        password.setError(null);

        // Store values at the time of the login attempt.
        usernameVal = this.username.getText().toString().trim();
        passwordVal = this.password.getText().toString().trim();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!isPasswordValid(passwordVal)) {
            password.setError(getString(R.string.error_empty_password));
            focusView = password;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(usernameVal)) {
            username.setError(getString(R.string.error_empty_username));
            focusView = username;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            Log.d("pass", passwordVal);
            Log.d("name", usernameVal);
            login();

        }
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 1;
    }


    private void goToHome() {
        Intent intent = new Intent(MainActivity.this, HomeActivity.class);
        startActivity(intent);
        finish(); // biar hapus dari stack page.
    }

}
