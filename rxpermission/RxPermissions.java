package sino.android.storage.rxpermission;

import android.util.Log;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.functions.Function;
import io.reactivex.subjects.PublishSubject;

public class RxPermissions {

    @FunctionalInterface
    public interface Lazy<V> {
        V get();
    }

    private static final String TAG = RxPermissions.class.getSimpleName();
    private static final Object TRIGGER = new Object();

    private Lazy<RxPermissionFragment> mFragmentLazy;

    public RxPermissions(FragmentActivity activity) {
        mFragmentLazy = getLazySingleton(activity.getSupportFragmentManager());
    }

    public RxPermissions(Fragment fragment) {
        mFragmentLazy = getLazySingleton(fragment.getChildFragmentManager());
    }

    private Lazy<RxPermissionFragment> getLazySingleton(FragmentManager fragmentManager) {
        return new Lazy<RxPermissionFragment>() {
            private RxPermissionFragment mFragment;

            @Override
            public synchronized RxPermissionFragment get() {
                if (mFragment == null) {
                    mFragment = getFragment(fragmentManager);
                }
                return mFragment;
            }
        };
    }

    private RxPermissionFragment getFragment(FragmentManager fragmentManager) {
        RxPermissionFragment fragment = findFragment(fragmentManager);
        boolean isNewInstance = fragment == null;
        if (isNewInstance) {
            fragment = new RxPermissionFragment();
            fragmentManager.beginTransaction()
                    .add(fragment, TAG)
                    .commitNow();
        }
        return fragment;
    }

    private RxPermissionFragment findFragment(FragmentManager fragmentManager) {
        return (RxPermissionFragment) fragmentManager.findFragmentByTag(TAG);
    }

    public Observable<Boolean> request(String... permissions) {
        return Observable.just(TRIGGER).compose(ensure(permissions));
    }

    public Observable<RxPermission> requestEach(String... permissions) {
        return Observable.just(TRIGGER).compose(ensureEach(permissions));
    }

    public Observable<RxPermission> requestEachCombined(String... permissions) {
        return Observable.just(TRIGGER).compose(ensureEachCombined(permissions));
    }

    private <T> ObservableTransformer<T, Boolean> ensure(String... permissions) {
        return new ObservableTransformer<T, Boolean>() {
            @Override
            public ObservableSource<Boolean> apply(Observable<T> upstream) {
                return request(upstream, permissions)
                        .buffer(permissions.length)
                        .flatMap(new Function<List<RxPermission>, ObservableSource<Boolean>>() {
                            @Override
                            public ObservableSource<Boolean> apply(List<RxPermission> rxPermissions) throws Exception {
                                if (rxPermissions.isEmpty()) {
                                    return Observable.empty();
                                }
                                for (RxPermission permission : rxPermissions) {
                                    Log.d("rxpermission", "apply: " + permission.toString());
                                    if (!permission.granted) {
                                        return Observable.just(false);
                                    }
                                }
                                return Observable.just(true);
                            }
                        });
            }
        };
    }

    private <T> ObservableTransformer<T, RxPermission> ensureEach(String... permissions) {
        return new ObservableTransformer<T, RxPermission>() {
            @Override
            public ObservableSource<RxPermission> apply(Observable<T> upstream) {
                return request(upstream, permissions);
            }
        };
    }

    private <T> ObservableTransformer<T, RxPermission> ensureEachCombined(String... permissions) {
        return new ObservableTransformer<T, RxPermission>() {
            @Override
            public ObservableSource<RxPermission> apply(Observable<T> upstream) {
                return request(upstream, permissions)
                        .buffer(permissions.length)
                        .flatMap(new Function<List<RxPermission>, ObservableSource<RxPermission>>() {
                            @Override
                            public ObservableSource<RxPermission> apply(List<RxPermission> rxPermissions) throws Exception {
                                if (rxPermissions.isEmpty()) {
                                    return Observable.empty();
                                }
                                return Observable.just(new RxPermission(rxPermissions));
                            }
                        });
            }
        };
    }

    private Observable<RxPermission> request(Observable<?> trigger, String... permissions) {
        if (permissions == null || permissions.length == 0) {
            throw new IllegalArgumentException("请求权限不能为空");
        }

        return Observable.just(TRIGGER)
                .flatMap(new Function<Object, ObservableSource<RxPermission>>() {
                    @Override
                    public ObservableSource<RxPermission> apply(Object o) throws Exception {
                        return requestInternal(permissions);
                    }
                });
    }

    // ？？？
    private Observable<?> oneOf(Observable<?> trigger, Observable<?> pending) {
        // 理论上不会为null
        if (trigger == null) {
            return Observable.just(TRIGGER);
        }

        return Observable.merge(trigger, pending);
    }

    // ？？？
    private Observable<?> pending(String... permissions) {
        for (String permission : permissions) {
            if (!mFragmentLazy.get().containsByPermission(permission)) {
                return Observable.empty();
            }
        }

        // 所有请求权限都申请过了，好像永远不会走？
        return Observable.just(TRIGGER);
    }

    private Observable<RxPermission> requestInternal(String... permissions) {
        List<Observable<RxPermission>> list = new ArrayList<>(permissions.length);
        List<String> unrequestedPermissions = new ArrayList<>();

        for (String permission : permissions) {
            if (mFragmentLazy.get().isGranted(permission)) {
                list.add(Observable.just(new RxPermission(permission, true)));
                continue;
            }

            PublishSubject<RxPermission> subject = mFragmentLazy.get().getSubjectByPermission(permission);
            if (subject == null) {
                unrequestedPermissions.add(permission);
                subject = PublishSubject.create();
                mFragmentLazy.get().setSubjectByPermission(permission, subject);
            }
            list.add(subject);
        }

        if (!unrequestedPermissions.isEmpty()) {
            String[] array = unrequestedPermissions.toArray(new String[0]);
            mFragmentLazy.get().startRequestPermissions(array);
        }

        return Observable.concat(Observable.fromIterable(list));
    }

}
