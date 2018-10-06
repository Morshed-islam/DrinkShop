package com.techmorshed.drinkshop;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.support.annotation.Nullable;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.facebook.accountkit.Account;
import com.facebook.accountkit.AccountKit;
import com.facebook.accountkit.AccountKitCallback;
import com.facebook.accountkit.AccountKitError;
import com.facebook.accountkit.AccountKitLoginResult;
import com.facebook.accountkit.ui.AccountKitActivity;
import com.facebook.accountkit.ui.AccountKitConfiguration;
import com.facebook.accountkit.ui.LoginType;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.szagurskii.patternedtextwatcher.PatternedTextWatcher;
import com.techmorshed.drinkshop.Utils.Common;
import com.techmorshed.drinkshop.model.CheckUserResponse;
import com.techmorshed.drinkshop.model.User;
import com.techmorshed.drinkshop.retrofit.IDrinkShopAPI;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import dmax.dialog.SpotsDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class MainActivity extends AppCompatActivity {

    Button _continue;
    IDrinkShopAPI mService;
    private static final int REQUEST_CODE = 100;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mService = Common.getApi();

        _continue = findViewById(R.id.btn_continue);
        _continue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startLoginPage(LoginType.PHONE);

            }
        });



    }

    private void startLoginPage(LoginType loginType) {

        Intent intent = new Intent(this,AccountKitActivity.class);
        AccountKitConfiguration.AccountKitConfigurationBuilder builder =
                new AccountKitConfiguration.AccountKitConfigurationBuilder(loginType,
                        AccountKitActivity.ResponseType.TOKEN);


        intent.putExtra(AccountKitActivity.ACCOUNT_KIT_ACTIVITY_CONFIGURATION,builder.build());
        startActivityForResult(intent,REQUEST_CODE);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE){

            AccountKitLoginResult result = data.getParcelableExtra(AccountKitLoginResult.RESULT_KEY);

            if (result.getError() != null){

                Toast.makeText(this, ""+result.getError().getErrorType().getMessage(), Toast.LENGTH_SHORT).show();
            }
            else if (result.wasCancelled()){
                Toast.makeText(this, "cancel", Toast.LENGTH_SHORT).show();
            }
            else{

                if (result.getAccessToken() != null)
                {
                     final AlertDialog alertDialog = new SpotsDialog(MainActivity.this);
                    alertDialog.show();
                    alertDialog.setMessage("Please waiting...");

                    //get user phone and check exists
                    AccountKit.getCurrentAccount(new AccountKitCallback<Account>() {
                        @Override
                        public void onSuccess(final Account account) {

                            mService.checkUSerExists(account.getPhoneNumber().toString())
                                    .enqueue(new Callback<CheckUserResponse>() {
                                        @Override
                                        public void onResponse(Call<CheckUserResponse> call, Response<CheckUserResponse> response) {
                                            CheckUserResponse userResponse = response.body();
                                            if (userResponse.isExists()){
                                                //id user already exists , just start new activity
                                                startActivity(new Intent(getApplicationContext(),HomeActivity.class));
                                                finish();

                                            }else{
                                                alertDialog.dismiss();
                                                showRegisterDialog(account.getPhoneNumber().toString());

                                            }
                                        }

                                        @Override
                                        public void onFailure(Call<CheckUserResponse> call, Throwable t) {

                                        }
                                    });

                        }

                        @Override
                        public void onError(AccountKitError accountKitError) {

                            Log.d("Error",accountKitError.getErrorType().getMessage());
                        }
                    });

                }
            }

        }
    }

    private void showRegisterDialog(final String phone) {


        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Register");


        LayoutInflater layoutInflater = this.getLayoutInflater();
        View register_layout = layoutInflater.inflate(R.layout.register_layout,null);


        final MaterialEditText edt_name = register_layout.findViewById(R.id.edit_name);
        final MaterialEditText edt_address = register_layout.findViewById(R.id.edit_address);
        final MaterialEditText edt_birthdate = register_layout.findViewById(R.id.edit_birthdate);
        Button btn_register = register_layout.findViewById(R.id.register_btn_continue);


        edt_birthdate.addTextChangedListener(new PatternedTextWatcher("####-##-##"));

        builder.setView(register_layout);
        final AlertDialog dialog = builder.create();

        btn_register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                dialog.dismiss();

                if (TextUtils.isEmpty(edt_address.getText().toString()))
                {
                    Toast.makeText(MainActivity.this, "Please Enter your address", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(edt_birthdate.getText().toString()))
                {
                    Toast.makeText(MainActivity.this, "Please Enter your birthdate", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(edt_name.getText().toString()))
                {
                    Toast.makeText(MainActivity.this, "Please Enter your name", Toast.LENGTH_SHORT).show();
                    return;
                }

                final AlertDialog waitingDialog = new SpotsDialog(MainActivity.this);
                waitingDialog.show();
                waitingDialog.setMessage("Please waiting dude........");

                mService.registerNewUser(phone,
                        edt_name.getText().toString(),
                        edt_birthdate.getText().toString(),
                        edt_address.getText().toString())
                        .enqueue(new Callback<User>() {
                            @Override
                            public void onResponse(Call<User> call, Response<User> response) {
                                waitingDialog.dismiss();

                                User user = response.body();
                                if (TextUtils.isEmpty(user.getError_msg())){
                                    Toast.makeText(MainActivity.this, "User Register Successfully!!", Toast.LENGTH_SHORT).show();
                                    //start new activity
                                    startActivity(new Intent(getApplicationContext(),HomeActivity.class));
                                    finish();

                                }
                            }

                            @Override
                            public void onFailure(Call<User> call, Throwable t) {

                                waitingDialog.dismiss();
                            }
                        });

            }
        });


        dialog.show();

   }

    private void printShahKey() {
        try {

            PackageInfo info = getPackageManager().getPackageInfo("com.techmorshed.drinkshop",PackageManager.GET_SIGNATURES);

            for (Signature signature : info.signatures){

                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("KEYSHAH",Base64.encodeToString(md.digest(),Base64.DEFAULT));
            }

        }catch (PackageManager.NameNotFoundException e){

        }catch (NoSuchAlgorithmException e){

        }


    }
}
