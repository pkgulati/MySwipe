package kpraveen.in.myswipe;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity  {


    // UI references.
    private EditText mUserView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    TextView registerLink;

    private boolean mLoginInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // Set up the login form.
        mUserView = (EditText) findViewById(R.id.userId);
        //populateAutoComplete();

        registerLink = (TextView)findViewById(R.id.registerLink);

        SharedPreferences sharedPref = getSharedPreferences("application", Context.MODE_PRIVATE);
        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
        mLoginInProgress= false;
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (mLoginInProgress) {
            return;
        }

        // Reset errors.
        mUserView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        final String user = mUserView.getText().toString().trim();
        String password = mPasswordView.getText().toString().trim();

        boolean cancel = false;
        View focusView = null;

        if (TextUtils.isEmpty(user)) {
            mUserView.setError(getString(R.string.error_field_required));
            focusView = mUserView;
            cancel = true;
        }

        if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mLoginInProgress = true;
            boolean mReturn = false;
            try {
                String URL = TheApplication.baseUrl + "/api/login";
                JSONObject jsonBody = new JSONObject();
                jsonBody.put("user", user);
                jsonBody.put("password", password);
                Log.d(TheApplication.TAG, "postingData " + jsonBody.toString());
                final String mRequestBody = jsonBody.toString();
                StringRequest stringRequest = new StringRequest(Request.Method.POST, URL, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String str) {
                        showProgress(false);
                        mLoginInProgress = false;
                        Log.i(TheApplication.TAG, "onResponse");
                        try {
                            JSONObject response = new JSONObject(str);
                            SharedPreferences pref = getApplicationContext().getSharedPreferences("application", 0); // 0 - for private mode
                            SharedPreferences.Editor editor = pref.edit();
                            editor.putBoolean("loggedIn", true);
                            editor.putString("username", response.getString("username"));
                            editor.putString("accessToken", response.getString("id"));
                            editor.putString("userId", response.getString("userId"));
                            TheApplication.accessToken = response.getString("id");
                            TheApplication.username = response.getString("username");
                            TheApplication.userId = response.getString("userId");
                            editor.commit();
                            TheApplication.registerDevice();
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        } catch (Throwable t) {
                            Log.e(TheApplication.TAG, "Could not parse malformed JSON: \"" + str + "\"");
                            mPasswordView.setError("Something went wrong during login");
                            mPasswordView.requestFocus();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TheApplication.TAG, "onErrorResponse");
                        showProgress(false);
                        mLoginInProgress = false;
                        String message = null;
                        if (error instanceof NetworkError) {
                            message = "Cannot connect to Internet...Please check your connection!";
                        } else if (error instanceof ServerError) {
                            message = "The server could not be found. Please try again after some time!!";
                        } else if (error instanceof ParseError) {
                            message = "Parsing error! Please try again after some time!!";
                        } else if (error instanceof NoConnectionError) {
                            message = "Cannot connect to Internet...Please check your connection!";
                        } else if (error instanceof TimeoutError) {
                            message = "Connection TimeOut! Please check your internet connection.";
                        }
                        if (message != null) {
                            Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // if (error instanceof AuthFailureError) {
                        //     message = "Cannot connect to Internet...Please check your connection!";
                         //}

                        if (error == null || error.networkResponse == null) {
                            return;
                        }

                        String errorMessage = "";
                        //get status code here
                        final String statusCode = String.valueOf(error.networkResponse.statusCode);
                        //get response body and parse with appropriate encoding
                        try {
                            String body = new String(error.networkResponse.data,"UTF-8");
                            try {
                                JSONObject response = new JSONObject(body);
                                JSONObject response2 = (JSONObject)(response.get("error"));
                                errorMessage = response2.getString("message");
                            } catch (Throwable e) {

                            }
                        } catch (UnsupportedEncodingException e) {
                            // exception
                        }

                        Log.e(TheApplication.TAG, "Error pkg");
                        Log.e(TheApplication.TAG, statusCode);

                        if (errorMessage.isEmpty()) {
                            mPasswordView.setError(getString(R.string.error_incorrect_password));
                        } else {
                            mPasswordView.setError(errorMessage);
                        }
                        mPasswordView.requestFocus();
                    }
                }) {
                    @Override
                    public String getBodyContentType() {
                        return "application/json; charset=utf-8";
                    }

                    @Override
                    public byte[] getBody() throws AuthFailureError {
                        try {
                            return mRequestBody == null ? null : mRequestBody.getBytes("utf-8");
                        } catch (UnsupportedEncodingException uee) {
                            Log.d(TheApplication.TAG, "Unsupported Encoding ");
                            return null;
                        }
                    }
                };
                int MY_SOCKET_TIMEOUT_MS=10000;

                stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                        MY_SOCKET_TIMEOUT_MS,
                        0,
                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                TheApplication.requestQueue.add(stringRequest);
                Log.d(TheApplication.TAG, "Request added");
            } catch (JSONException e) {
                Log.d(TheApplication.TAG, "Json exception ");
            }
        }
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }
}

