package com.travity.ui.login;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.travity.MainActivity;
import com.travity.R;

import com.travity.SplashActivity;
import com.travity.data.Member;
import com.travity.data.db.MembersSQLiteHelper;
import com.travity.ui.login.LoginViewModel;
import com.travity.ui.login.LoginViewModelFactory;
import com.travity.util.textdrawable.TextDrawable;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;

import gujc.directtalk9.common.Util9;

public class LoginActivity extends AppCompatActivity {

    private LoginViewModel loginViewModel;
    SharedPreferences sharedPreferences;
    TextInputEditText usernameEditText;
    TextInputEditText passwordEditText;
    private boolean isSignin = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        loginViewModel = new ViewModelProvider(this, new LoginViewModelFactory())
                .get(LoginViewModel.class);

        usernameEditText = findViewById(R.id.username);
        passwordEditText = findViewById(R.id.password);
//        final Button loginButton = findViewById(R.id.login);
        final TextView login = findViewById(R.id.login);
        final TextView signin = findViewById(R.id.signin);
        final ProgressBar loadingProgressBar = findViewById(R.id.loading);

        sharedPreferences = getSharedPreferences("travity", Activity.MODE_PRIVATE);
        String id = sharedPreferences.getString("user_id", "");
        if (!"".equals(id)) {
            usernameEditText.setText(id);
        }

        loginViewModel.getLoginFormState().observe(this, new Observer<LoginFormState>() {
            @Override
            public void onChanged(@Nullable LoginFormState loginFormState) {
                if (loginFormState == null) {
                    return;
                }
//                loginButton.setEnabled(loginFormState.isDataValid());
                if (loginFormState.getUsernameError() != null) {
                    usernameEditText.setError(getString(loginFormState.getUsernameError()));
                }
                if (loginFormState.getPasswordError() != null) {
                    passwordEditText.setError(getString(loginFormState.getPasswordError()));
                }
            }
        });

        loginViewModel.getLoginResult().observe(this, new Observer<LoginResult>() {
            @Override
            public void onChanged(@Nullable LoginResult loginResult) {
                if (loginResult == null) {
                    return;
                }
/*
                if (!validateForm()) {
                    return;
                }
*/
                loadingProgressBar.setVisibility(View.GONE);
                if (loginResult.getError() != null) {
                    showLoginFailed(loginResult.getError());
                }
                if (loginResult.getSuccess() != null) {
                    updateUiWithUser(loginResult.getSuccess());
                }
//                setResult(Activity.RESULT_OK);

                //Complete and destroy login activity once successful
//                finish();
            }
        });

        TextWatcher afterTextChangedListener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // ignore
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // ignore
            }

            @Override
            public void afterTextChanged(Editable s) {
                loginViewModel.loginDataChanged(usernameEditText.getText().toString(),
                        passwordEditText.getText().toString());
            }
        };
        usernameEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    loginViewModel.login(usernameEditText.getText().toString(),
                            passwordEditText.getText().toString());
                }
                return false;
            }
        });

/*
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadingProgressBar.setVisibility(View.VISIBLE);
                loginViewModel.login(usernameEditText.getText().toString(),
                        passwordEditText.getText().toString());
            }
        });
 */
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isSignin = false;
                loadingProgressBar.setVisibility(View.VISIBLE);
                loginViewModel.login(usernameEditText.getText().toString(),
                        passwordEditText.getText().toString());
            }
        });
        signin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isSignin = true;
                loadingProgressBar.setVisibility(View.VISIBLE);
                loginViewModel.login(usernameEditText.getText().toString(),
                        passwordEditText.getText().toString());
            }
        });
    }

    private void createProfile() {
        String email = (String) usernameEditText.getText().toString();
        String initial = null;
        String[] tmpName = email.split("\\.");
        if (tmpName.length > 1) {
            initial = String.valueOf(tmpName[0].toUpperCase().charAt(0))
                    + String.valueOf(tmpName[1].toUpperCase().charAt(0));
        } else {
            initial = email.substring(0, 2);
        }
		sharedPreferences.edit().putString("user_initial", initial).apply();
        TextDrawable.getIconInitialName(initial);

/*
        // open the database of the application context
        MembersSQLiteHelper dbMembers = new MembersSQLiteHelper(this);

        // save profile
        Member member = new Member(0,
                            null,
                            null,
                            null,
                            email,
                            null,
                            null,
                            null);
        dbMembers.createMember(member);*/
    }

    private String extractIDFromEmail(String email){
        String[] parts = email.split("@");
        return parts[0];
    }

    private void processLogin() {
//        Log.d(String.valueOf(R.string.app_name), "usernameEditText.getText().toString(): " + usernameEditText.getText().toString());
//        Log.d(String.valueOf(R.string.app_name), "passwordEditText.getText().toString(): " + passwordEditText.getText().toString());

        FirebaseAuth.getInstance().signInWithEmailAndPassword(usernameEditText.getText().toString(), passwordEditText.getText().toString())
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    String id = usernameEditText.getText().toString();
                    sharedPreferences.edit().putString("user_id", id).apply();
                    sharedPreferences.edit().putString("user_name", extractIDFromEmail(id)).apply();

                    Intent intent = new Intent(getBaseContext(), com.travity.MainActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Util9.showMessage(getApplicationContext(), task.getException().getMessage());
                }
            }
        });
    }
    private void processSignin() {
        final String id = usernameEditText.getText().toString();

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(id, passwordEditText.getText().toString())
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    final String uid = FirebaseAuth.getInstance().getUid();

                    gujc.directtalk9.model.UserModel userModel = new gujc.directtalk9.model.UserModel();
                    userModel.setUid(uid);
                    userModel.setUserid(id);
                    userModel.setUsernm(extractIDFromEmail(id));
                    userModel.setUsermsg("...");

                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    db.collection("users").document(uid)
                            .set(userModel)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Intent intent = new Intent(getBaseContext(), com.travity.MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                    Log.d(String.valueOf(R.string.app_name), "DocumentSnapshot added with ID: " + uid);
                                }
                            });
                    sharedPreferences.edit().putString("user_id", id).apply();
                    sharedPreferences.edit().putString("user_name", extractIDFromEmail(id)).apply();
                } else {
                    Util9.showMessage(getApplicationContext(), task.getException().getMessage());
                }
            }
        });
    }

    private void updateUiWithUser(LoggedInUserView model) {
//        sharedPreferences.edit().putString("user_id", usernameEditText.getText().toString()).apply();

//        createProfile();
        if (isSignin) {
            processSignin();
        } else {
            processLogin();
        }
/*
        Intent intent = null;
        intent = new Intent(getBaseContext(), MainActivity.class);
        startActivity(intent);
        finish();
*/
    }

    private void showLoginFailed(@StringRes Integer errorString) {
        Toast.makeText(getApplicationContext(), errorString, Toast.LENGTH_SHORT).show();
    }
}