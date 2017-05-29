/*
 * Copyright 2015 OpenMarket Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.matrix.androidsdk.HomeserverConnectionConfig;
import org.matrix.androidsdk.MXSession;
import org.matrix.androidsdk.rest.callback.SimpleApiCallback;
import org.matrix.androidsdk.rest.model.MatrixError;
import org.matrix.androidsdk.rest.model.login.Credentials;
import org.matrix.androidsdk.rest.model.login.LoginFlow;
import im.vector.LoginHandler;
import im.vector.Matrix;
import im.vector.R;
import im.vector.util.ThemeUtils;

import java.util.List;

/**
 * Displays the login screen.
 */
public class LoginActivity extends MXCActionBarActivity {

    private static final String LOG_TAG = "LoginActivity";
    static final int ACCOUNT_CREATION_ACTIVITY_REQUEST_CODE = 314;
    static final int FALLBACK_LOGIN_ACTIVITY_REQUEST_CODE = 315;

    public static final String LOGIN_PREF = "vector_login";
    public static final String PASSWORD_PREF = "vector_password";


    // saved parameters index
    private static final String SAVED_EMAIL_ADDRESS = "SAVED_EMAIL_ADDRESS";
    private static final String SAVED_PASSWORD_ADDRESS = "SAVED_PASSWORD_ADDRESS";
    private static final String SAVED_IS_SERVER_URL_EXPANDED = "SAVED_IS_SERVER_URL_EXPANDED";
    private static final String SAVED_HOMESERVERURL = "SAVED_HOMESERVERURL";
    private static final String SAVED_IDENTITY_SERVERURL = "SAVED_IDENTITY_SERVERURL";

    // graphical items
    // login button
    private Button mLoginButton;

    // create account button
    private Button mRegisterButton;

    // the account name
    private TextView mEmailTextView;

    // the password
    private TextView mPasswordTextView;

    // forgot my password
    private TextView mPasswordForgottenTxtView;

    // the home server text
    private EditText mHomeServerText;

    // the identity server text
    private EditText mIdentityServerText;

    // used to display a UI mask on the screen
    private RelativeLayout mLoginMaskView;

    private boolean mIsHomeServerUrlIsDisplayed;
    private View mDisplayHomeServerUrlView;
    private View mHomeServerUrlsLayout;
    private ImageView mExpandImageView;

    String mHomeServerUrl = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeUtils.activitySetTheme(this);
        setContentView(R.layout.activity_vector_login);

        // resume the application
        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            Log.e(LOG_TAG, "Resume the application");
            finish();
            return;
        }

        if (hasCredentials()) {
            Log.e(LOG_TAG, "goToSplash because the credentials are already provided.");
            goToSplash();
            finish();
            return;
        }

        // bind UI widgets
        mLoginMaskView = (RelativeLayout)findViewById(R.id.flow_ui_mask_login);
        mEmailTextView = (EditText) findViewById(R.id.login_user_name);
        mHomeServerText = (EditText) findViewById(R.id.login_matrix_server_url);
        mIdentityServerText = (EditText) findViewById(R.id.login_identity_url);
        mPasswordTextView = (EditText) findViewById(R.id.editText_password);
        mPasswordForgottenTxtView = (TextView) findViewById(R.id.login_forgot_password);
        mLoginButton = (Button)findViewById(R.id.button_login);
        mRegisterButton = (Button)findViewById(R.id.button_register);
        mDisplayHomeServerUrlView = findViewById(R.id.display_server_url_layout);
        mHomeServerUrlsLayout =  findViewById(R.id.login_matrix_server_options_layout);
        mExpandImageView = (ImageView)findViewById(R.id.display_server_url_expand_icon);

        if (null != savedInstanceState) {
            if (savedInstanceState.containsKey(SAVED_EMAIL_ADDRESS)) {
                mEmailTextView.setText(savedInstanceState.getString(SAVED_EMAIL_ADDRESS));
            }

            if (savedInstanceState.containsKey(SAVED_PASSWORD_ADDRESS)) {
                mPasswordTextView.setText(savedInstanceState.getString(SAVED_PASSWORD_ADDRESS));
            }

            if (savedInstanceState.containsKey(SAVED_IS_SERVER_URL_EXPANDED)) {
                mIsHomeServerUrlIsDisplayed = savedInstanceState.getBoolean(SAVED_IS_SERVER_URL_EXPANDED);
            }

            if (savedInstanceState.containsKey(SAVED_HOMESERVERURL)) {
                mHomeServerText.setText(savedInstanceState.getString(SAVED_HOMESERVERURL));
            }

            if (savedInstanceState.containsKey(SAVED_IDENTITY_SERVERURL)) {
                mIdentityServerText.setText(savedInstanceState.getString(SAVED_IDENTITY_SERVERURL));
            }
        } else {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(LoginActivity.this);

            mEmailTextView.setText(preferences.getString(LOGIN_PREF, ""));
            mPasswordTextView.setText(preferences.getString(PASSWORD_PREF, ""));
        }

        // TODO implement the forgot password
        mPasswordForgottenTxtView.setVisibility(View.GONE);

        mLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String username = mEmailTextView.getText().toString().trim();
                String password = mPasswordTextView.getText().toString().trim();
                String serverUrl = mHomeServerText.getText().toString().trim();
                String identityServerUrl = mIdentityServerText.getText().toString().trim();
                onLoginClick(serverUrl, identityServerUrl, username, password);
            }
        });

        // account creation handler
        mRegisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String hs = mHomeServerText.getText().toString();

                boolean validHomeServer = false;

                try {
                    Uri hsUri = Uri.parse(hs);
                    validHomeServer = "http".equals(hsUri.getScheme()) || "https".equals(hsUri.getScheme());
                } catch (Exception e) {
                    Log.w(LOG_TAG,"## Exception: "+e.getMessage());
                }

                if (!validHomeServer) {
                    Toast.makeText(LoginActivity.this, getString(R.string.login_error_invalid_home_server), Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent intent = new Intent(LoginActivity.this, AccountCreationActivity.class);
                intent.putExtra(AccountCreationActivity.EXTRA_HOME_SERVER_ID, hs);
                startActivityForResult(intent, ACCOUNT_CREATION_ACTIVITY_REQUEST_CODE);
            }
        });

        // home server input validity: if the user taps on the next / done button
        mHomeServerText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {

                    // the user validates the home server url
                    if (!TextUtils.equals(mHomeServerUrl, mHomeServerText.getText().toString())) {
                        mHomeServerUrl = mHomeServerText.getText().toString();
                        checkFlows();
                        return true;
                    }
                }

                return false;
            }
        });

        // home server input validity: when focus changes
        mHomeServerText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    if (!TextUtils.equals(mHomeServerUrl, mHomeServerText.getText().toString())) {
                        mHomeServerUrl = mHomeServerText.getText().toString();
                        checkFlows();
                    }
                }
            }
        });

        // "forgot password?" handler
        mPasswordForgottenTxtView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(LoginActivity.this, "Not implemented..", Toast.LENGTH_SHORT).show();
            }
        });

        mDisplayHomeServerUrlView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIsHomeServerUrlIsDisplayed = !mIsHomeServerUrlIsDisplayed;
                refreshHomeServerTextDisplay();
            }
        });

        refreshHomeServerTextDisplay();

        // reset the badge counter
        CommonActivityUtils.updateBadgeCount(this, 0);

        mEmailTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(LoginActivity.this);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(LOGIN_PREF, mEmailTextView.getText().toString());
                editor.commit();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        mPasswordTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(LoginActivity.this);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(PASSWORD_PREF, mPasswordTextView.getText().toString());
                editor.commit();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

        // retrieve the home server path
        mHomeServerUrl = mHomeServerText.getText().toString();

        // check if the login supports the server flows
        checkFlows();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);

        if (!TextUtils.isEmpty(mEmailTextView.getText().toString().trim())) {
            savedInstanceState.putString(SAVED_EMAIL_ADDRESS, mEmailTextView.getText().toString().trim());
        }

        if (!TextUtils.isEmpty(mPasswordTextView.getText().toString().trim())) {
            savedInstanceState.putString(SAVED_PASSWORD_ADDRESS, mPasswordTextView.getText().toString().trim());
        }

        savedInstanceState.putBoolean(SAVED_IS_SERVER_URL_EXPANDED, mIsHomeServerUrlIsDisplayed);

        if (!TextUtils.isEmpty(mHomeServerText.getText().toString().trim())) {
            savedInstanceState.putString(SAVED_HOMESERVERURL, mHomeServerText.getText().toString().trim());
        }
    }

    /**
     * Refresh the visibility of mHomeServerText
     */
    private void refreshHomeServerTextDisplay() {
        mHomeServerUrlsLayout.setVisibility(mIsHomeServerUrlIsDisplayed ? View.VISIBLE : View.GONE);
        mExpandImageView.setImageResource(mIsHomeServerUrlIsDisplayed ? R.drawable.ic_material_arrow_drop_down_black : R.drawable.ic_material_arrow_drop_up_black);
    }

    private void onLoginClick(String hsUrlString, String identityUrlString, String username, String password) {
        // --------------------- sanity tests for input values.. ---------------------
        if (!hsUrlString.startsWith("http")) {
            Toast.makeText(this, getString(R.string.login_error_must_start_http), Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, getString(R.string.login_error_invalid_credentials), Toast.LENGTH_SHORT).show();
            return;
        }

        if (!hsUrlString.startsWith("http://") && !hsUrlString.startsWith("https://")) {
            hsUrlString = "https://" + hsUrlString;
        }

        if (!identityUrlString.startsWith("http://") && !identityUrlString.startsWith("https://")) {
            identityUrlString = "https://" + identityUrlString;
        }

        // ---------------------------------------------------------------------------

        Uri hsUrl = Uri.parse(hsUrlString);
        final HomeserverConnectionConfig hsConfig = new HomeserverConnectionConfig(hsUrl);

        hsConfig.setIdentityServerUri(Uri.parse(identityUrlString));

        // disable UI actions
        setFlowsMaskEnabled(true);

        try {
            LoginHandler loginHandler = new LoginHandler();
            loginHandler.login(this, hsConfig, username, password, new SimpleApiCallback<HomeserverConnectionConfig>(this) {
                @Override
                public void onSuccess(HomeserverConnectionConfig c) {
                    setFlowsMaskEnabled(false);
                    goToSplash();
                    LoginActivity.this.finish();
                }

                @Override
                public void onNetworkError(Exception e) {
                    Log.e(LOG_TAG, "Network Error: " + e.getMessage(), e);
                    setFlowsMaskEnabled(false);
                    Toast.makeText(getApplicationContext(), getString(R.string.login_error_network_error), Toast.LENGTH_LONG).show();
                }

                @Override
                public void onUnexpectedError(Exception e) {
                    setFlowsMaskEnabled(false);
                    String msg = getString(R.string.login_error_unable_login) + " : " + e.getMessage();
                    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
                }

                @Override
                public void onMatrixError(MatrixError e) {
                    setFlowsMaskEnabled(false);
                    String msg = getString(R.string.login_error_unable_login) + " : " + e.error + "(" + e.errcode + ")";
                    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.login_error_invalid_home_server), Toast.LENGTH_SHORT).show();
            setFlowsMaskEnabled(false);
            setLoginButtonsEnabled(true);
        }
    }

    private boolean hasCredentials() {
        try {
            return Matrix.getInstance(this).getDefaultSession() != null;
        } catch (Exception e) {
            Log.w(LOG_TAG,"## Exception: "+e.getMessage());
        }

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    // getDefaultSession could trigger an exception if the login data are corrupted
                    CommonActivityUtils.logout(LoginActivity.this);
                } catch (Exception e) {
                    Log.w(LOG_TAG, "## Exception: " + e.getMessage());
                }
            }
        });

        return false;
    }

    private void goToSplash() {
        Log.e(LOG_TAG, "Go to splash.");
        startActivity(new Intent(this, SplashActivity.class));
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data)  {
        if ((ACCOUNT_CREATION_ACTIVITY_REQUEST_CODE == requestCode) || (FALLBACK_LOGIN_ACTIVITY_REQUEST_CODE == requestCode)) {
            if (resultCode == RESULT_OK) {
                String homeServer = data.getStringExtra("homeServer");
                String homeServerUrl = data.getStringExtra("homeServerUrl");
                String userId = data.getStringExtra("userId");
                String accessToken = data.getStringExtra("accessToken");

                // build a credential with the provided items
                Credentials credentials = new Credentials();
                credentials.userId = userId;
                credentials.homeServer = homeServer;
                credentials.accessToken = accessToken;

                final HomeserverConnectionConfig hsConfig = new HomeserverConnectionConfig(
                    Uri.parse(homeServerUrl), credentials
                );

                Log.e(LOG_TAG, "Account creation succeeds");

                // let's go...
                MXSession session = Matrix.getInstance(getApplicationContext()).createSession(hsConfig);
                Matrix.getInstance(getApplicationContext()).addSession(session);
                goToSplash();
                LoginActivity.this.finish();
            } else if ((resultCode == RESULT_CANCELED) && (FALLBACK_LOGIN_ACTIVITY_REQUEST_CODE == requestCode)) {
                // reset the home server to let the user writes a valid one.
                mHomeServerText.setText("https://");
                setLoginButtonsEnabled(false);
            }
        }
    }

    /**
     * @return the homeserver config. null if the url is not valid
     */
    private HomeserverConnectionConfig getHsConfig() {
        String hsUrlString = mHomeServerText.getText().toString();

        if ((null == hsUrlString) || !hsUrlString.startsWith("http") || TextUtils.equals(hsUrlString, "http://") || TextUtils.equals(hsUrlString, "https://")) {
            Toast.makeText(this,getString(R.string.login_error_must_start_http),Toast.LENGTH_SHORT).show();
            return null;
        }

        if(!hsUrlString.startsWith("http://") && !hsUrlString.startsWith("https://")){
            hsUrlString = "https://" + hsUrlString;
        }

        return new HomeserverConnectionConfig(Uri.parse(hsUrlString));
    }

    /**
     * display a loading screen mask over the login screen
     * @param aIsMaskEnabled true to enable the loading screen, false otherwise
     */
    private void setFlowsMaskEnabled(boolean aIsMaskEnabled) {
        // disable/enable login buttons
        setLoginButtonsEnabled(!aIsMaskEnabled);

        if(null != mLoginMaskView) {
            mLoginMaskView.setVisibility(aIsMaskEnabled ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * @param enabled enabled/disabled the login buttons
     */
    private void setLoginButtonsEnabled(Boolean enabled) {
        mLoginButton.setEnabled(enabled);
        mRegisterButton.setEnabled(enabled);

        mLoginButton.setAlpha(enabled ? 1.0f : 0.5f);
        mRegisterButton.setAlpha(enabled ? 1.0f : 0.5f);
    }

    /**
     * Check the homeserver flows.
     * i.e checks if this login page is enough to perform a registration.
     * else switcth to a fallback page
     */
    private void checkFlows() {
        try {
            LoginHandler loginHandler = new LoginHandler();
            final HomeserverConnectionConfig hsConfig = getHsConfig();

            // invalid URL
            if (null == hsConfig) {
                setLoginButtonsEnabled(false);
            } else {
                setFlowsMaskEnabled(true);

                loginHandler.getSupportedFlows(LoginActivity.this, hsConfig, new SimpleApiCallback<List<LoginFlow>>() {
                    @Override
                    public void onSuccess(List<LoginFlow> flows) {
                        setFlowsMaskEnabled(false);
                        setLoginButtonsEnabled(true);
                        Boolean isSupported = true;

                        // supported only m.login.password by now
                        for(LoginFlow flow : flows) {
                            isSupported &= TextUtils.equals("m.login.password", flow.type);
                        }

                        // if not supported, switch to the fallback login
                        if (!isSupported) {
                            Intent intent = new Intent(LoginActivity.this, FallbackLoginActivity.class);
                            intent.putExtra(FallbackLoginActivity.EXTRA_HOME_SERVER_ID, hsConfig.getHomeserverUri().toString());
                            startActivityForResult(intent, FALLBACK_LOGIN_ACTIVITY_REQUEST_CODE);
                        }
                    }

                    private void onError(String errorMessage) {
                        setFlowsMaskEnabled(false);
                        setLoginButtonsEnabled(false);
                        Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onNetworkError(Exception e) {
                        Log.e(LOG_TAG, "Network Error: " + e.getMessage(), e);
                        onError(getString(R.string.login_error_unable_login) + " : " + e.getLocalizedMessage());
                    }

                    @Override
                    public void onUnexpectedError(Exception e) {
                        onError(getString(R.string.login_error_unable_login) + " : " + e.getLocalizedMessage());
                    }

                    @Override
                    public void onMatrixError(MatrixError e) {
                        onError(getString(R.string.login_error_unable_login) + " : " + e.error + "(" + e.errcode + ")");
                    }
                });
            }
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), getString(R.string.login_error_invalid_home_server), Toast.LENGTH_SHORT).show();
            setLoginButtonsEnabled(true);
            setFlowsMaskEnabled(false);
        }
    }
}
