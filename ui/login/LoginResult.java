package com.travity.ui.login;

import androidx.annotation.Nullable;

/**
 * Authentication result : success (user details) or error message.
 */
public class LoginResult {
    @Nullable
    private com.travity.ui.login.LoggedInUserView success;
    @Nullable
    private Integer error;

    public LoginResult(@Nullable Integer error) {
        this.error = error;
    }

    public LoginResult(@Nullable com.travity.ui.login.LoggedInUserView success) {
        this.success = success;
    }

    @Nullable
    public LoggedInUserView getSuccess() {
        return success;
    }

    @Nullable
    public Integer getError() {
        return error;
    }
}