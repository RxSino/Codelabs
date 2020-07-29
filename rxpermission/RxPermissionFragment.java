package sino.android.storage.rxpermission;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.HashMap;
import java.util.Map;

import io.reactivex.subjects.PublishSubject;

public class RxPermissionFragment extends Fragment {

    private static final int REQUEST_CODE = 256;

    private Map<String, PublishSubject<RxPermission>> mSubjectMap = new HashMap<>();

    public RxPermissionFragment() {
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setRetainInstance(true);
    }

    public PublishSubject<RxPermission> getSubjectByPermission(String permission) {
        return mSubjectMap.get(permission);
    }

    public void setSubjectByPermission(String permission, PublishSubject<RxPermission> subject) {
        mSubjectMap.put(permission, subject);
    }

    public boolean containsByPermission(String permission) {
        return mSubjectMap.containsKey(permission);
    }

    public boolean isGranted(String permission) {
        if (getContext() != null) {
            return ContextCompat.checkSelfPermission(getContext(), permission) == PackageManager.PERMISSION_GRANTED;
        }
        return false;
    }

    public void startRequestPermissions(String[] permissions) {
        requestPermissions(permissions, REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != REQUEST_CODE) {
            return;
        }

        boolean[] rationale = new boolean[permissions.length];

        for (int i = 0; i < permissions.length; i++) {
            rationale[i] = shouldShowRequestPermissionRationale(permissions[i]);
        }

        onRequestPermissionsResult(permissions, grantResults, rationale);
    }

    private void onRequestPermissionsResult(String[] permissions, int[] grantResults, boolean[] rationale) {
        for (int i = 0; i < permissions.length; i++) {
            PublishSubject<RxPermission> subject = mSubjectMap.get(permissions[i]);
            if (subject == null) {
                Log.d("rxpermission", "理论上不会出现");
                return;
            }
            mSubjectMap.remove(permissions[i]);
            boolean granted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
            subject.onNext(new RxPermission(permissions[i], granted, rationale[i]));
            subject.onComplete();
        }
    }

}
