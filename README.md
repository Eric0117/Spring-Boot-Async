# Spring-Boot-Async

Java & Spring 환경에서의 비동기 처리방법의 여러가지 개념을 알아보고 간단하게 구현해보겠습니다.

Blocking / Non-Blocking은 무엇이고 Sync / Async는 무엇일까요?
![pic1](https://user-images.githubusercontent.com/79642391/189126314-9562aaf7-803d-4e0c-9696-4438d1c0c22a.png)

Blocking / Non-Blocking은 호출된 함수가 호출한 함수에게 제어권을 바로 주느냐 주지 않느냐,

Sync / Async는 호출된 함수의 종료를 호출한 함수가 처리하느냐, 호출된 함수가 처리하느냐 의 차이라고 할 수 있겠습니다.

> `Blocking` A 함수가 B 함수를 호출 할 때, B 함수가 자신의 작업이 종료되기 전까지 A 함수에게 제어권을 돌려주지 않는것
>
> `Non-Blocking` A 함수가 B 함수를 호출할 때, B 함수가 A 함수에게 제어권을 바로 넘겨주면서 A 함수가 다른 일을 할 수 있도록 하는것
>
> `Synchronous` A 함수가 B 함수를 호출 할 때, B 함수의 결과를 A 함수가 처리하는것
>
> `Asynchronous` A 함수가 B 함수를 호출 할 때, B 함수의 결과를 B 함수가 처리하는 것(Callback)

Spring에서는 Blocking/Sync 방식으로 동작하고, Node.js는 Non-Blocking/Async 방식으로 동작합니다.
- - -

Tomcat은 server.xml 내에 있는 `Executor` configuration을 통해 server의 Thread pool을 설정합니다.

```
<Executor name="tomcatThreadPool" namePrefix="catalina-exec-" maxThreads="200" minSpareThreads="25"/>
```

Spring Boot에서는 application.properties에서 설정할 수 있습니다.
```
Spring Boot 2.3 이전
server.tomcat.max-threads=200
```
```
Spring Boot 2.3이후 
server.tomcat.threads.max=200 
```
`minSpareThreads`은 Thread pool 초기 개수이고, `maxThread`는 Thread pool 최대 Thread 수입니다. Default 설정은 각각 `25`, `200`입니다.

학습을 위해 톰캣의 Thread pool 개수를 1로 설정하고 진행하겠습니다.
```
# application.properties
server.tomcat.threads.max=1
```

- - - - -
# Async in Controller
## Servelt Thread (Blocking)
default thread를 1로 설정하고, 아래와 같이 컨트롤러를 작성합니다. 

요청을 받으면 로그를 출력 후 10초간 대기하고, value + " Finished."를 반환하는 함수입니다. 
```java
@GetMapping("/{value}")
public String sync(@PathVariable String value) throws InterruptedException {
    System.out.println("Call Hello in sync Method! Variable : " + value);
    TimeUnit.SECONDS.sleep(10);
    return value + " Finished.";
}
```

최초 요청 후 모든 Thread가 사용중이기 때문에 새로고침을 해서 재 요청을 보내도 첫번째 요청이 끝날 때 까지 Pending합니다.


## Servelt Thread (Non-Blocking)

이제 같은 환경에서 Non-Blocking으로 요청을 처리하는법을 알아보겠습니다.

### - Callable

Callable은 값을 바로 반환하는 대신에 java.util.concurrent.Callable를 먼저 반환하고, Spring에서 관리하는 별도의 Thread에서 값을 반환합니다.

위의 과정을 진행하는 동안 주요 Servlet Container Thread는 해당 요청에서 벗어나 다른 요청을 받을 수 있게 됩니다. 그리고 Spring MVC는 TaskExecutor을 이용해 각각의 Thread에서 Callable 작업을 수행합니다. 그리고 Callable 작업이 종료된 후 Callable 객체를 반환합니다. 그럼 다시 요청이 Servlet Container로 돌아가게 되고 Spring MVC Thread에게 Callable 객체를 반환받습니다.

```java
@GetMapping("callable/{value}")
public Callable<String> callable(@PathVariable String value) {
    System.out.println("Call Hello in callable Method! Variable : " + value);
    return () -> {
      TimeUnit.SECONDS.sleep(10);
      return value + " Finished.";
    };
}
```
위의 코드를 작성하고 요청을 해보면, 첫번째 요청으로 Thread가 10초간 쉬는 도중에 또 다른 요청이 들어와도 callable 메소드에 진입하는것을 볼 수 있습니다.


### - DeferredResult

DeferredResult 또한 다른 Thread로부터 생산된 값을 반환하는 것은 동일합니다. 하지만 Callable과는 다르게 Thread가 Spring MVC에서 관리되지 않습니다.
Controller는 DeferredResult를 반환하고, 내부 Queue나 List에 생성한 DeferredResult를 보관하고 있습니다.

```java
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
```


### - ResponseBodyEmitter
Callable, DeferredResult가 하나의 결과를 생성해 요청을 처리했다면, ResponseBodyEmitter는 여러개의 결과를 만들어 요청을 처리할 수 있습니다.

Http5가 표준이 되면서 하나의 Http 요청에 여러개의 응답이 가능해졌습니다. 그 이유는 ResponseBodyEmitter를 통해서 여러개의 오브젝트를 보낼 수 있기 때문입니다.

send()를 통해 하나 이상의 결과를 반환 할 수 있고, 더이상 보낼 게 없을 때 complete() 메소드를 실행하면 Response가 반환됩니다.
```java
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
```

위의 코드를 작성한 후 요청해보면 아래와 같은 Response를 얻을 수 있습니다. 100ms간격으로 `value 0 chunck`~`value 14 chunck`를 리턴합니다.
<img width="945" alt="스크린샷 2022-09-08 22 40 39" src="https://user-images.githubusercontent.com/79642391/189137430-749f0299-d4d7-4558-8841-d776064b80f9.png">


- - - - -

# Async in Service
Controller 계층에서 비동기 처리하는 방법 외에 Service 계층에서 비동기 처리하는 방법을 알아보겠습니다.


### - Future
Future은 자바 1.5에서 등장한 비동기 계산의 결과를 나타내는 Interface입니다.

같은 Thread에서 메소드를 호출할 때는 결과를 리턴값을 받지만, 비동기적으로 작업을 수행할 때는 결과값을 전달받을 수 있는 무언가의 Interface가 필요한데, Future이 그 역할을 합니다.

```java
public Future<String> getValue(String value) {
    final ExecutorService executorService = Executors.newSingleThreadExecutor();
    return executorService.submit(() -> {
        TimeUnit.SECONDS.sleep(10);
        return value + " Finished.";
    });
}
```
AsyncService를 만들고, getValue 메소드를 작성하였습니다. 다음으로 Controller에서 getValue를 호출하는 메소드를 작성하겠습니다.

```java
@GetMapping("future/{value}")
public String future(@PathVariable String value) throws ExecutionException, InterruptedException {
    final Future<String> future = asyncService.getValue(value);
    final String result = future.get();
    System.out.println("I think i'm done.");
    return result;
}
```

작성을 마친 뒤, future/value로 요청을 보내면 10초 대기 후 "value Finished." 를 반환받았습니다.

`future.get()`이 실행되는 순간 FutureTask의 작업이 시작되고 10초후에 result를 반환합니다. 하지만 `future.get()` 메소드를 호출하는 현재 Thread는 Blocking 됩니다.

FutureTask의 작업이 오래 걸리더라도 `System.out.println("I think i'm done.");` 라인이 바로 실행되길 바라지만 말이죠. 이것이 Future를 이용한 비동기 처리의 한계점입니다.

### - ListenableFuture

Future는 응답이 끝날 때 까지 Blocking 됩니다. Spring 4.0부터 이 한계를 극복하기 위한 ListenableFuture이 등장하였습니다. ListenableFuture는 Future의 Callback method를 사용합니다. 즉 `future.get()`을 기다려서 처리하는 것이 아닌 작업이 성공했을 때, 실패했을 때 처리해야 할 Callback method를 정의하는 것 입니다.

```java
public ListenableFuture<String> getValueByListen(String value) {
    SimpleAsyncTaskExecutor t = new SimpleAsyncTaskExecutor();
    return t.submitListenable(() -> {
        TimeUnit.SECONDS.sleep(10);
        return value + " Finished.2";
    });
}
```

```java
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
```
addCallback 메서드에 성공했을 때(위 코드에선 빈 함수입니다.), 실패했을 때 작업을 미리 정의하였습니다. listenableFuture/value로 요청했을 때, Blocking 없이 I think i'm done. 로그가 출력되는것을 확인할 수 있습니다.

### - CompletableFuture
위의 ListenableFuture를 구현할 때 Callback을 작성해야 했습니다. 예제에서는 Callback이 하나였지만 실무에서 여러개의 비동기 처리를 작업해야 하고 그에 따른 Callback이 여러개가 된다면 어떻게 될까요? 벌써부터 Callback Hell에 빠질 생각에 아찔해집니다.

그 불편함을 알았는지 Java 8에서 CompletableFuture이 등장하였습니다. 

```java
@GetMapping("completableFuture/{value}")
public CompletableFuture comple(@PathVariable String value) {
    return asyncService.getValueByCompletableFuture1(value)
            .thenCompose(asyncService::getValueByCompletableFuture2)
            .thenCompose(asyncService::getValueByCompletableFuture3);
}
```
Callback 처리를 아주 간단하고 가독성 있게 작성할 수 있게 되었습니다.

CompletableFuture의 `thenCompose`를 사용해서 각 메소드의 결과값을 파이프라인 형태로 처리한 것입니다.

```java
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
```

`xxxAsync`, `thenCompose`, `thenCombine`, `thenApply(Async)` 등 CompletableFuture의 여러가지 Method는 [이곳](https://www.baeldung.com/java-completablefuture)에서 확인할 수 있습니다.

### - @Async

Spring MVC 3.2부터 Servlet 3.0 기반의 비동기 요청 처리가 가능해졌습니다. @Async 어노테이션을 사용해 해당 메소드를 비동기적으로 호출할 수 있는데요, 해당 메소드를 호출한 호출자(caller)는 즉시 리턴하고 메소드의 실제 실행은 Spring TaskExecutor에 의해 실행됩니다.

비동기로 실행되는 메소드는 Future 형식의 값을 반환하고, 호출자는 해당 Future의 get() 메소드를 호출하기 전에 다른 작업을 수행할 수 있습니다.

@Async 어노테이션 활성화를 위해 @EnableAsync를 먼저 선언해야 합니다.

```java
@SpringBootApplication
@EnableAsync // this
public class DemoApplication {
...
```

이제 비동기 처리를 하고싶은 메소드에 @Async 어노테이션을 선언하면 됩니다. 반환값 없이 void로도 처리할 수 있지만 리턴 값에 대한 처리가 필요하다면 위에서 언급한 비동기 클래스를 반환하면 됩니다.


```java
@GetMapping("async/{value}")
public CompletableFuture<String> async(@PathVariable String value)  {
    return asyncService.getValueByAsync(value);
}
```

```java
@Async
public CompletableFuture<String> getValueByAsync(String value) {
    try {
        TimeUnit.SECONDS.sleep(10);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
    return CompletableFuture.completedFuture(value + " Finished.");
}
```

@Async 어노테이션을 사용할 때는 Public 메소드에만 사용 가능하고, 같은 인스턴스 내의 메소드끼리만 호출할 때는 비동기 호출이 되지 않는것을 주의해야합니다.


