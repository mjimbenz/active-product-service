package com.bank.active_product.utils;

import io.reactivex.rxjava3.core.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class RxAdapter {

    // --- Mono -> Single ---
    public static <T> Single<T> monoToSingle(Mono<T> mono) {
        return Single.fromPublisher(mono);
    }

    // --- Mono -> Maybe ---
    public static <T> Maybe<T> monoToMaybe(Mono<T> mono) {
        return Maybe.fromPublisher(mono);
    }

    // --- Mono -> Completable ---
    public static Completable monoToCompletable(Mono<?> mono) {
        return Completable.fromPublisher(mono);
    }

    // --- Flux -> Observable ---
    public static <T> Observable<T> fluxToObservable(Flux<T> flux) {
        return Observable.fromPublisher(flux);
    }

    // --- Single -> Mono ---
    public static <T> Mono<T> singleToMono(Single<T> single) {
        return Mono.from(single.toFlowable());
    }

    // --- Observable -> Flux ---
    public static <T> Flux<T> observableToFlux(Observable<T> observable) {
        return Flux.from(observable.toFlowable(BackpressureStrategy.BUFFER));
    }
}