package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.concurrent.*;

/**
 * @Author Eric
 * @Description
 * @Since 22. 9. 8.
 **/
@Service
@RequiredArgsConstructor
public class AsyncService {

    public Future<String> getValue(String value) {
        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        return executorService.submit(() -> {
            TimeUnit.SECONDS.sleep(10);
            return value + " Finished.";
        });
    }

    public ListenableFuture<String> getValueByListen(String value) {
        SimpleAsyncTaskExecutor t = new SimpleAsyncTaskExecutor();
        return t.submitListenable(() -> {
            TimeUnit.SECONDS.sleep(10);
            return value + " Finished.2";
        });
    }

    public CompletableFuture<String> getValueByCompletableFuture1(String value) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return value + "Finished. => 1";
        });
    }

    public CompletableFuture<String> getValueByCompletableFuture2(String value) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return value + "Finished. => 2";
        });
    }

    public CompletableFuture<String> getValueByCompletableFuture3(String value) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return value + "Finished. => 3";
        });
    }

    @Async
    public CompletableFuture<String> getValueByAsync(String value) {
        try {
            TimeUnit.SECONDS.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return CompletableFuture.completedFuture(value + " Finished.");
    }
}
