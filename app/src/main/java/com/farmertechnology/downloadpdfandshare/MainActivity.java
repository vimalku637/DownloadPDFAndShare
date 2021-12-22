package com.farmertechnology.downloadpdfandshare;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class MainActivity extends AppCompatActivity {
    private Context context = this;
    public static SweetAlertDialog mDialog;
    private static final int REQUEST = 112;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        doPermissionFirst();
        MaterialButton btnDownloadPDFAndShare = findViewById(R.id.btnDownloadPDFAndShare);
        btnDownloadPDFAndShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isInternetOn(context)) {
                    showProgressDialog(context);
                    String url = "http://telemedicine.nutrisoft.in/apiV1-17/prescription/Prescription_22182.pdf";
                    String pdfName = "Prescription_22182.pdf";

                    new DownloadFile().execute(url, pdfName);

                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //share PDF file after 5 sec.
                            dismissProgressDialog();

                            //open new dialog
                            new SweetAlertDialog(context, SweetAlertDialog.WARNING_TYPE)
                                    .setTitleText("Are you sure?")
                                    .setContentText("Want to share '.PDF' file!")
                                    .setConfirmText("Share")
                                    .setCancelText("Cancel")
                                    .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                        @Override
                                        public void onClick(SweetAlertDialog sDialog) {
                                            sDialog.dismiss();

                                            File myFile = null;
                                            myFile = new File("/storage/emulated/0/DOWNLOAD_PDF/" + pdfName);
                                            Uri uri = FileProvider.getUriForFile(MainActivity.this, MainActivity.this.getPackageName() + ".provider", myFile);

                                            Intent intentShareFile = new Intent(Intent.ACTION_SEND);
                                            if (myFile.exists()) {
                                                intentShareFile.setType("application/pdf");
                                                intentShareFile.putExtra(Intent.EXTRA_STREAM, uri);

                                                intentShareFile.putExtra(Intent.EXTRA_SUBJECT, "Sharing file");
                                                intentShareFile.putExtra(Intent.EXTRA_TEXT, "Sharing file from V-PDF share");
                                                context.startActivity(Intent.createChooser(intentShareFile, "Share file via"));
                                            }
                                        }
                                    })
                                    .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                        @Override
                                        public void onClick(SweetAlertDialog sDialog) {
                                            sDialog.dismiss();
                                        }
                                    })
                                    .show();
                        }
                    }, 10000);
                } else {
                    showPopupForNoInternet(context);
                }
            }
        });
    }

    private class DownloadFile extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... strings) {
            String fileUrl = strings[0];   // -> http://maven.apache.org/maven-1.x/maven.pdf
            String fileName = strings[1];  // -> maven.pdf
            String extStorageDirectory = Environment.getExternalStorageDirectory().toString();
            File folder = new File(extStorageDirectory, "DOWNLOAD_PDF");
            folder.mkdir();

            File pdfFile = new File(folder, fileName);

            try {
                pdfFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            FileDownloader.downloadFile(fileUrl, pdfFile);
            return null;
        }
    }

    public void showPopupForNoInternet(Context context) {
        new SweetAlertDialog(context, SweetAlertDialog.WARNING_TYPE)
                .setTitleText("Alert!")
                .setContentText("No internet available. Before download the .pdf file make sure you have internet connection.!!")
                .setConfirmText("Ok")
                .setCancelText("Cancel")
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        sDialog.dismiss();
                    }
                })
                .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        sDialog.dismiss();
                    }
                })
                .show();
    }

    public static boolean isInternetOn(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        assert connectivityManager != null;
        if (connectivityManager.getNetworkInfo(0).getState() == android.net.NetworkInfo.State.CONNECTED
                || connectivityManager.getNetworkInfo(0).getState() == android.net.NetworkInfo.State.CONNECTING
                || connectivityManager.getNetworkInfo(1).getState() == android.net.NetworkInfo.State.CONNECTING
                || connectivityManager.getNetworkInfo(1).getState() == android.net.NetworkInfo.State.CONNECTED) {

            return true;

        } else if (connectivityManager.getNetworkInfo(0).getState() == android.net.NetworkInfo.State.DISCONNECTED
                || connectivityManager.getNetworkInfo(1).getState() == android.net.NetworkInfo.State.DISCONNECTED) {
            return false;
        }
        return false;
    }

    public static void showProgressDialog(Context context) {
        mDialog = new SweetAlertDialog(context, SweetAlertDialog.PROGRESS_TYPE);
        mDialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
        mDialog.setTitleText("Please wait while downloading the '.PDF' file.");
        mDialog.setCancelable(false);
        mDialog.show();
    }

    public static void dismissProgressDialog() {
        mDialog.dismiss();
    }

    private boolean doPermissionFirst() {
        if (Build.VERSION.SDK_INT >= 23) {
            Log.d("TAG", "@@@ IN IF Build.VERSION.SDK_INT >= 23");
            String[] PERMISSIONS = {
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_FINE_LOCATION
            };
            if (!hasPermissions(this, PERMISSIONS)) {
                Log.d("TAG", "@@@ IN IF hasPermissions");
                ActivityCompat.requestPermissions((Activity) this, PERMISSIONS, REQUEST);
            } else {
                Log.d("TAG", "@@@ IN ELSE hasPermissions");
            }
        } else {
            Log.d("TAG", "@@@ IN ELSE  Build.VERSION.SDK_INT >= 23");
        }
        return true;
    }

    private static boolean hasPermissions(Context context, String... permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("TAG", "@@@ PERMISSIONS grant");
                } else {
                    Log.d("TAG", "@@@ PERMISSIONS Denied");
                    Toast.makeText(this, "PERMISSIONS Denied", Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}
