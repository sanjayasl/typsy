package com.example.typsy.data;

import android.support.annotation.NonNull;

import com.example.typsy.data.local.LocalDataSource;
import com.example.typsy.data.local.entity.Price;
import com.example.typsy.data.network.RemoteDataSource;

/**
 * Created by gravity on 10/26/17.
 */
// A simple implementation of a network bound request using callbacks
public abstract class PriceSource {
    private LocalDataSource mLocal;
    private RemoteDataSource mRemote;
    private Callback.ApiCallbak apiCallbak;
    private Callback.DBCallbak dbCallbak;

    PriceSource(@NonNull LocalDataSource local, @NonNull RemoteDataSource remote, @NonNull String fsym, @NonNull String tsym) {
        mLocal = local;
        mRemote = remote;

        // first sets data to null and state to loading
        onResponse(Resource.loading(null));

        // callback for db events
        dbCallbak = new Callback.DBCallbak() {
            @Override
            public void initialData(Price p) {
                onResponse(Resource.loading(p));
                mRemote.getPriceResponse(fsym, tsym, apiCallbak);
            }

            @Override
            public void finalData(Price p) {
                mLocal.savePrice(p);
                onResponse(Resource.success(p));
            }

            @Override
            public void dbError(Throwable t) {
                onErr(Resource.error(t.toString(), null));
                mRemote.getPriceResponse(fsym, tsym, apiCallbak);
            }

            @Override
            public void onApiError(Throwable t) {
                onErr(Resource.error(t.toString(), null));
            }

            @Override
            public void onComplete() {
                onFinish();
            }
        };

        // callback for network events
        apiCallbak = new Callback.ApiCallbak() {
            @Override
            public void onApiResponse(Price p) {
                dbCallbak.finalData(p);
                // creates a request loop for live updates - commented out
                /*mRemote.getPriceResponse(fsym, tsym, apiCallbak);*/
            }

            @Override
            public void apiError(Throwable t) {
                dbCallbak.onApiError(t);
            }
        };
        mLocal.getPrice(fsym, tsym, dbCallbak);
    }

    protected abstract void onResponse(Resource<Price> resource);

    protected abstract void onErr(Resource<Price> resource);

    protected abstract void onFinish();
}