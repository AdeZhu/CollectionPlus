package com.example.acer.collectionplus.Model;

import android.util.Log;

import com.example.acer.collectionplus.Base.BaseBean;
import com.example.acer.collectionplus.Helper.AnalyzeHtml;
import com.example.acer.collectionplus.Helper.SharedHelper;
import com.example.acer.collectionplus.Helper.TimeHelper;
import com.example.acer.collectionplus.Http.HttpUtils;
import com.example.acer.collectionplus.JavaBean.DirBean;
import com.example.acer.collectionplus.JavaBean.LinkBean;
import com.example.acer.collectionplus.JavaBean.SimpleDirBean;
import com.example.acer.collectionplus.RetrofitInterface.RestAPI;
import com.example.acer.collectionplus.ViewModel.AddLoadListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

public class AddModel implements IAddModel{
    private static final String TAG = "AddModel";
    List<SimpleDirBean> dirBeans = new ArrayList<>();

    /**
     * 加载收藏夹以及链接标题
     *
     * @param content
     */
    @Override
    public void LoadData(final String content , final AddLoadListener<SimpleDirBean> listener) {
        //加载收藏夹
        String username = SharedHelper.getInstance().getValue("username");
        Observable<DirBean>  observable =   HttpUtils.getRetrofit().create(RestAPI.class).getDirList(username);
        observable.subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableObserver<DirBean>() {
                    @Override
                    public void onNext(DirBean dirBean) {
                        DirOnNext(dirBean);
                    }

                    @Override
                    public void onError(Throwable e) {
                        listener.loadFailure(e.getMessage());
                    }

                    @Override
                    public void onComplete() {
                        listener.loadSuccess(dirBeans);
                        listener.loadComplete();
                    }
                });
        //load title
        Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> e) throws Exception {
                String title = new AnalyzeHtml().GetTitle(content);
                Log.d(TAG, "subscribe: "+title);
                e.onNext(title);
                e.onComplete();
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableObserver<String>() {

                    @Override
                    public void onNext(String title) {
                        Log.d(TAG, "onNext: "+title);
                        listener.loadSuccessForTitle(title);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG, "onError: "+e.getMessage());
                        listener.loadFailure(e.getMessage());
                    }

                    @Override
                    public void onComplete() {
                        listener.loadComplete();
                    }
                });
    }


    @Override
    public void saveData( LinkBean.Entity entity, final AddLoadListener<SimpleDirBean> listener) {
        String username = SharedHelper.getInstance().getValue("username");
        Map<String ,String > map = new HashMap<>();
        map.put("value",entity.getValue());
        map.put("dirname",entity.getDirname());
        map.put("read","0");
        map.put("source",entity.getSource());
        map.put("title",entity.getTitle());
        map.put("type",entity.getType());
        map.put("time",entity.getTime());
        map.put("username",username);
        HttpUtils.getRetrofit().create(RestAPI.class).saveLink(map)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableObserver<BaseBean>(){

                    @Override
                    public void onNext(BaseBean baseBean) {
                        Log.d(TAG, "onNext: ");
                        boolean flag = baseBean.isSuccess();
                        if(!flag){
                            String info = baseBean.getInfo();
                            listener.loadFailure(info);
                        }
                    }


                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG, "onError: "+e.getMessage());
                        listener.loadFailure(e.getMessage());
                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG, "onComplete: ");
                        listener.loadComplete();
                    }
                });
    }

    /**
     * 把dirBean转化为SimpleDirBean
     *
     * @param dirBean
     */
    private void DirOnNext(DirBean dirBean) {
        List<DirBean.Entity> entities = dirBean.getData();
        dirBeans.clear();
        if (entities != null && entities.size() > 0) {
            for (DirBean.Entity entity : entities) {
                String dirname = entity.getDirname();
                String time = TimeHelper.getFormatedTime(entity.getTime());
                SimpleDirBean sdb = new SimpleDirBean();
                sdb.dirname.set(dirname);
                sdb.time.set(time);
                sdb.isClicked.set(false);
                dirBeans.add(sdb);
            }
        }

    }
}