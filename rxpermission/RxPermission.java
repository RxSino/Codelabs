package sino.android.storage.rxpermission;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.functions.BiConsumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;

public class RxPermission {
    public String name;
    public boolean granted;
    public boolean shouldShowRequestPermissionRationale;

    public RxPermission(String name, boolean granted) {
        this(name, granted, false);
    }

    public RxPermission(String name, boolean granted, boolean shouldShowRequestPermissionRationale) {
        this.name = name;
        this.granted = granted;
        this.shouldShowRequestPermissionRationale = shouldShowRequestPermissionRationale;
    }


    public RxPermission(List<RxPermission> permissions) {
        name = combineName(permissions);
        granted = combineGranted(permissions);
        shouldShowRequestPermissionRationale = combineRationale(permissions);
    }

    private String combineName(List<RxPermission> permissions) {
        return Observable.fromIterable(permissions)
                .map(new Function<RxPermission, String>() {
                    @Override
                    public String apply(RxPermission rxPermission) throws Exception {
                        return rxPermission.name;
                    }
                }).collectInto(new StringBuilder(), new BiConsumer<StringBuilder, String>() {
                    @Override
                    public void accept(StringBuilder stringBuilder, String s) throws Exception {
                        if (stringBuilder.length() == 0) {
                            stringBuilder.append(s);
                        } else {
                            stringBuilder.append(", ").append(s);
                        }
                    }
                }).blockingGet().toString();
    }

    private Boolean combineGranted(List<RxPermission> permissions) {
        return Observable.fromIterable(permissions)
                .all(new Predicate<RxPermission>() {
                    @Override
                    public boolean test(RxPermission rxPermission) throws Exception {
                        return rxPermission.granted;
                    }
                }).blockingGet();
    }

    private Boolean combineRationale(List<RxPermission> permissions) {
        return Observable.fromIterable(permissions)
                .all(new Predicate<RxPermission>() {
                    @Override
                    public boolean test(RxPermission rxPermission) throws Exception {
                        return rxPermission.shouldShowRequestPermissionRationale;
                    }
                }).blockingGet();
    }

    @Override
    public String toString() {
        return "RxPermission{" +
                "name='" + name + '\'' +
                ", granted=" + granted +
                ", shouldShowRequestPermissionRationale=" + shouldShowRequestPermissionRationale +
                '}';
    }
}
