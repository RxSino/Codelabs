package sino.android.storage.module.demo;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import sino.android.storage.base.BaseActivity;
import sino.android.storage.databinding.DemoActPermissionBinding;
import sino.android.storage.rxpermission.RxPermission;
import sino.android.storage.rxpermission.RxPermissions;
import sino.android.storage.rxs.SubObserver;

public class RxPermissionActivity extends BaseActivity {

    public static void start(Context context) {
        Intent starter = new Intent(context, RxPermissionActivity.class);
        context.startActivity(starter);
    }

    DemoActPermissionBinding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = DemoActPermissionBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        mBinding.returnView.setOnClickListener(v -> onBackPressed());
        mBinding.startBtn.setOnClickListener(v -> onRequest());
    }

    /**
     * 原生权限
     */
    private void onPermissionTest() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.CAMERA
            }, 1234);

        } else {
            Log.d("sino", "onPermissionTest: " + "granted");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1234) {
            if (grantResults.length > 0) {
                onPermissionsResult(permissions, grantResults);
            }
        }
    }

    private void onPermissionsResult(String[] permissions, int[] grantResults) {
        boolean allGranted = true;
        boolean[] showRationales = new boolean[permissions.length];
        boolean showRationale = false;
        for (int i = 0; i < grantResults.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                showRationales[i] = ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[i]);
                if (showRationales[i]) {
                    showRationale = true;
                }
            }
        }

        if (allGranted) {
            // 所有请求都允许了
        }

        if (showRationale) {
            ///onAlertDialog();
        }
    }

    // RxPermission
    private void onRequest() {
        new RxPermissions(this)
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA,
                        Manifest.permission.CAMERA,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                .subscribe(new SubObserver<Boolean>() {
                    @Override
                    public void onNext(Boolean aBoolean) {
                        Log.d("sino", "onPermissionsTest: " + aBoolean);
                    }
                });
    }

    /**
     * 此方法用来请求一个权限比较好。
     */
    private void onRequestEach() {
        new RxPermissions(this)
                .requestEach(Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                .subscribe(new SubObserver<RxPermission>() {
                    @Override
                    public void onNext(RxPermission permission) {
                        // will emit 3 objects
                        if (permission.granted) {
                            Log.d("sino", "onRequestEach: true");
                        } else if (permission.shouldShowRequestPermissionRationale) {
                            onDenyPermissionDialog();
                        } else {
                            onNeverAskAgainDialog();
                        }
                    }
                });
    }

    private void onRequestEachCombined() {
        new RxPermissions(this)
                .requestEachCombined(Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                .subscribe(new SubObserver<RxPermission>() {
                    @Override
                    public void onNext(RxPermission permission) {
                        if (permission.granted) {
                            Log.d("sino", "onRequestEach: true");
                        } else if (permission.shouldShowRequestPermissionRationale) {
                            onDenyPermissionDialog();
                        } else {
                            onNeverAskAgainDialog();
                        }
                    }
                });
    }

    private void onDenyPermissionDialog() {
        new AlertDialog.Builder(this)
                .setMessage("请允许相关权限")
                .setPositiveButton("好", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onRequestEachCombined();
                    }
                })
                .show();
    }

    private void onNeverAskAgainDialog() {
        new AlertDialog.Builder(this)
                .setMessage("请设置相关权限")
                .setPositiveButton("好", null)
                .setNegativeButton("设置", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startSettings();
                    }
                })
                .show();
    }

    private void startSettings() {
        Uri uri = Uri.parse("package:" + getPackageName());
        Intent starter = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri);
        starter.addCategory(Intent.CATEGORY_DEFAULT);
        startActivityForResult(starter, 233);
    }

}
