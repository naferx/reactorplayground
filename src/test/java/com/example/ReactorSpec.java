package com.example;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SynchronousSink;
import reactor.test.StepVerifier;

import org.junit.Test;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.concurrent.Callable;

public class ReactorSpec {

    @Test
    public void testFluxBasic() {
        Flux<String> source = Flux.just(
                "John", "Monica", "Mark", "Cloe", "Frank",
                "Casper", "Olivia", "Emily", "Cate")
                .filter(name -> name.length() == 4)
                .map(String::toUpperCase);

        Flux.generate(
                () -> Tuples.of(0, 1),
                (state, sink) -> {
                    sink.next(state.getT1());
                    return Tuples.of(state.getT2(), state.getT1() + state.getT2());
                }
        );

        StepVerifier
                .create(source)
                .expectNext("JOHN")
                .expectNextMatches(name -> name.startsWith("MA"))
                .expectNext("CLOE", "CATE")
                .expectComplete()
                .verify();
    }


    @Test
    public void testFluxGenerate() {

        Flux<Integer> source = Flux.<Integer, Integer>generate(() -> 1, (s, o) -> {
            if (s < 11) {
                o.next(s);
            } else {
                o.complete();
            }
            return s + 1;
        });

        StepVerifier
                .create(source)
                .expectNext(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .expectComplete()
                .verify();

    }


    @Test
    public void testFluxGeneratePage()  throws Exception {

        Flux<Integer> source = Flux.generate(
                () -> Tuples.of(getPage(1), 1),
                (s, o) -> {
                    int results = s.getT1().block();
                    int page = s.getT2();
                    if (results == 0) o.complete();
                    else o.next(results);
                    return Tuples.of(getPage(page + 1), page + 1);
                }
        );


        StepVerifier
                .create(performPageableRequestExpand(1))
                .expectNext(10, 10, 10, 10, 10)
                .expectComplete()
                .verify();

    }

    public Mono<Integer> getPage(int page) {
        int result = page <= 5 ? 10 : 0;
        return Mono.just(result);
    }

    private  Flux<Integer> performPageableRequestExpand(int page) {
        return getPage2(page)
               .expand(result -> {
                   return result.getT1() == 0 ? Flux.empty() : getPage2(result.getT2() + 1);
               }).map(a -> a.getT1()).filter(b -> b != 0);

    }


    public Mono<Tuple2<Integer, Integer>> getPage2(int page) {
        int result = page <= 5 ? 10 : 0;
        return Mono.just(Tuples.of(result, page));
    }

}


