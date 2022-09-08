package com.example.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @Author Eric
 * @Description
 * @Since 22. 9. 8.
 **/
@RestController
@RequestMapping("/api/v1/main")
public class MainController {

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
}
