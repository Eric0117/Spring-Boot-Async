package com.example.demo.controller;

import com.example.demo.service.AsyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.util.concurrent.*;

/**
 * @Author Eric
 * @Description
 * @Since 22. 9. 8.
 **/
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/main")
public class MainController {

    private final AsyncService asyncService;

    @GetMapping("sync/{value}")
    public String sync(@PathVariable String value) throws InterruptedException {
        System.out.println("Call Hello in sync Method! Variable : " + value);
        TimeUnit.SECONDS.sleep(10);
        return value + " Finished.";
    }

    @GetMapping("callable/{value}")
    public Callable<String> callable(@PathVariable String value) {
        System.out.println("Call Hello in callable Method! Variable : " + value);
        return () -> {
            TimeUnit.SECONDS.sleep(10);
            return value + " Finished.";
        };
    }

    @GetMapping("deferredResult/{value}")
    public DeferredResult deferredResult(@PathVariable String value) {
        System.out.println("Call Hello in deferredResult Method! Variable : " + value);
        DeferredResult<String> rtn = new DeferredResult<>();
        Runnable anotherThread = () -> {
            try {
                TimeUnit.SECONDS.sleep(10);
                rtn.setResult(value + " Finished.");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };

        new Thread(anotherThread).start();

        return rtn;
    }

    @GetMapping("responseBodyEmitter/{value}")
    public ResponseBodyEmitter responseBodyEmitter(@PathVariable String value) {
        System.out.println("Call Hello in responseBodyEmitter Method! Variable : " + value);
        ResponseBodyEmitter emitter = new ResponseBodyEmitter();

        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                for (int i = 0; i < 15; i++) {
                    emitter.send("value " + i + " chunck\n");
                    Thread.sleep(100);
                }
            } catch (Exception e) {
            }
        });

        return emitter;
    }


    /////////////////////////////////

    @GetMapping("future/{value}")
    public String future(@PathVariable String value) throws ExecutionException, InterruptedException {
        final Future<String> future = asyncService.getValue(value);
        final String result = future.get();
        System.out.println("I think i'm done.");
        return result;
    }

    @GetMapping("listenableFuture/{value}")
    public ListenableFuture<String> listenableFuture(@PathVariable String value) throws ExecutionException, InterruptedException {
        final ListenableFuture<String> futureByListen = asyncService.getValueByListen(value);

        futureByListen.addCallback(
            string -> {},
            e -> {throw new RuntimeException();}
        );
        System.out.println("I think i'm done.");
        return futureByListen;
    }

    @GetMapping("completableFuture/{value}")
    public CompletableFuture comple(@PathVariable String value) {
        return asyncService.getValueByCompletableFuture1(value)
                .thenCompose(asyncService::getValueByCompletableFuture2)
                .thenCompose(asyncService::getValueByCompletableFuture3);
    }

    @GetMapping("async/{value}")
    public CompletableFuture<String> async(@PathVariable String value)  {
        return asyncService.getValueByAsync(value);
    }
}
