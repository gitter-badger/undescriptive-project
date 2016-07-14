package com.github.javadev.undescriptive.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.github.javadev.undescriptive.ApiException;
import com.github.javadev.undescriptive.protocol.response.*;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.AsyncHttpClientConfig.Builder;
import com.ning.http.client.FluentStringsMap;
import com.ning.http.client.Realm;
import com.ning.http.client.Response;

import java.io.IOException;

public class AsyncClient {
    private static final String BASE_URL = "http://www.dragonsofmugloar.com/api/game";

    private final static ObjectMapper MAPPER = new ObjectMapper()
        .registerModule(new JodaModule())
        .registerModule(new SimpleModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final AsyncHttpClient httpClient;
    private final String baseUrl;

    private AsyncClient(
            final AsyncHttpClient httpClient,
            final String baseUrl) {
        this.httpClient = httpClient;
        this.baseUrl = baseUrl;
    }

    private static AsyncHttpClientConfig commonSetup(final AsyncHttpClientConfig.Builder configBuilder) {
        final Realm realm = new Realm.RealmBuilder().build();
        configBuilder.setRealm(realm);
        return configBuilder.build();
    }

    public static AsyncClient createDefault() {
        return new AsyncClient(
            new AsyncHttpClient(commonSetup(new Builder())), BASE_URL);
    }

    public void close() {
        this.httpClient.close();
    }

    public void closeAsynchronously() {
        this.httpClient.closeAsynchronously();
    }

    private BoundRequestBuilder get(final String resourceUrl, final FluentStringsMap params) {
        return this.httpClient.prepareGet(this.baseUrl + resourceUrl).setQueryParameters(params);
    }

    private BoundRequestBuilder post(final String resourceUrl/*, final HasParams hasParams*/) {
        final BoundRequestBuilder builder = this.httpClient.preparePost(this.baseUrl + resourceUrl);
//        final Collection<Param> params = hasParams.getParams();

//        for (final Param param : params) {
//            for (final String s : param.getStringParam()) {
//                builder.addParameter(param.getName(), s);
//            }
//        }
        return builder;
    }

    private static <T> ListenableFuture<T> execute(
            final Class<T> clazz,
            final BoundRequestBuilder request) {
        final SettableFuture<T> guavaFut = SettableFuture.create();
        try {
            request.execute(new GuavaFutureConverter<T>(clazz, guavaFut));
        }
        catch (final IOException e) {
            guavaFut.setException(e);
        }
        return guavaFut;
    }

    private static class GuavaFutureConverter<T> extends AsyncCompletionHandler<T> {
        final Class<T> clazz;
        final SettableFuture<T> guavaFut;

        public GuavaFutureConverter(
                final Class<T> clazz,
                final SettableFuture<T> guavaFut) {
            this.clazz = clazz;
            this.guavaFut = guavaFut;
        }

        private static boolean isSuccess(final Response response) {
            final int statusCode = response.getStatusCode();
            return (statusCode > 199 && statusCode < 400);
        }

        @Override
        public void onThrowable(final Throwable t) {
            guavaFut.setException(t);
        }

        @Override
        public T onCompleted(final Response response) throws Exception {
            if (isSuccess(response)) {
                final T value = MAPPER.readValue(response.getResponseBody(), clazz);
                guavaFut.set(value);
                return value;
            }
            else {
                final ErrorResponse error = MAPPER.readValue(response.getResponseBody(), ErrorResponse.class);
                final ApiException exception = new ApiException(response.getUri(), error);
                guavaFut.setException(exception);
                throw exception;
            }
        }
    }
}