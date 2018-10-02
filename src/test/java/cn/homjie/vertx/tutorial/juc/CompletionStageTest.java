package cn.homjie.vertx.tutorial.juc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author jiehong.jh
 * @date 2018/10/2
 */
@Slf4j
public class CompletionStageTest {

    @Test
    public void futureExample() throws ExecutionException, InterruptedException {
        String threadNamePrefix = "future-executor-";
        ExecutorService executor = Executors.newFixedThreadPool(3, new SimpleThreadFactory(threadNamePrefix));
        int i = 0;
        log.info("{}", ++i);
        Future<String> future = executor.submit(() -> {
            log.info("start execute");
            sleep(300);
            log.info("execute ok");
            return "Done";
        });
        log.info("{}", ++i);
        log.info("future: {}", future.get());
        FutureTask<String> futureTask = new FutureTask<>(() -> {
            log.info("start execute");
            sleep(200);
            log.info("execute ok");
            return "Done";
        });
        log.info("{}", ++i);
        executor.submit(futureTask);
        log.info("futureTask result: {}", futureTask.get());
    }

    @Test(expected = CancellationException.class)
    public void futureCancleExample() throws ExecutionException, InterruptedException {
        String threadNamePrefix = "future-executor-";
        ExecutorService executor = Executors.newFixedThreadPool(3, new SimpleThreadFactory(threadNamePrefix));
        int i = 0;
        log.info("{}", ++i);
        Future<String> future = executor.submit(() -> {
            log.info("start execute");
            sleep(200);
            log.info("execute ok");
            return "Done";
        });
        log.info("{}", ++i);
        sleep(100);
        log.info("{}", ++i);
        assertFalse(future.isDone());
        // true 响应中断 sleep
        future.cancel(true);
        assertTrue(future.isDone());
        log.info("{}", ++i);
        sleep(200);
        log.info("{}", ++i);
        // throw CancellationException
        System.out.println(future.get());
    }

    @Test
    public void completedFutureExample() {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        log.info("start..");
        new Thread(() -> {
            log.info("new thread run");
            // 阻塞等待结果完成
            log.info("future get: {}", future.join());
        }).start();
        sleep(200);
        future.complete(5);
        log.info("complete 5");
        sleep(100);

        // 使用一个预定义的结果创建一个完成的CompletableFuture
        CompletableFuture<String> cf = CompletableFuture.completedFuture("message");
        assertTrue(cf.isDone());
        // 在future完成的情况下会返回结果
        assertEquals("message", cf.getNow("default"));
        cf = CompletableFuture.completedFuture(null);
        assertTrue(cf.isDone());
        assertNull(cf.getNow("Unreachable"));
    }

    @Test
    public void runAsyncExample() {
        int i = 0;
        log.info("{}", ++i);
        // 在没有指定executor的情况下， 异步执行通过ForkJoinPool实现，它使用守护线程去执行任务
        CompletableFuture cf = CompletableFuture
            .runAsync(() -> {
                assertTrue(Thread.currentThread().isDaemon());
                sleep(300);
                log.info("async run ok");
            });
        log.info("{}", ++i);
        assertFalse(cf.isDone());
        log.info("{}", ++i);
        sleep(500);
        log.info("{}", ++i);
        assertTrue(cf.isDone());
        log.info("{}", ++i);
    }

    @Test
    public void getNowExample() {
        // getNow 除非结果执行完成，否则无论抛出异常和未执行完都返回默认值
        CompletableFuture<Integer> cf = CompletableFuture.supplyAsync(() -> {
            throw new ArithmeticException("/ by zero");
        });
        log.info("Exception happen: {}", cf.getNow(1));

        cf = CompletableFuture.supplyAsync(() -> {
            log.info("start calc");
            sleep(200);
            log.info("finish calc");
            return 2;
        });
        log.info("The result is not ready yet: {}", cf.getNow(1));
        log.info("join return result: {}", cf.join());
        log.info("result is ok: {}", cf.getNow(-1));
    }

    @Test(expected = ExecutionException.class)
    public void supplyAsyncExample() throws ExecutionException, InterruptedException {
        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
            throw new ArithmeticException("/ by zero");
        });
        // java.util.concurrent.CompletionException: java.lang.ArithmeticException: / by zero
        //future.join();

        // java.util.concurrent.ExecutionException: java.lang.ArithmeticException: / by zero
        future.get();
    }

    @Test
    public void thenApplyExample() {
        // then 这个阶段的动作发生当前的阶段正常完成之后
        // apply 会对前一阶段的结果应用一个函数
        CompletableFuture<String> cf = CompletableFuture
            .completedFuture("message")
            .thenApply(s -> {
                assertFalse(Thread.currentThread().isDaemon());
                log.info("to upper case in main thread");
                return s.toUpperCase();
            });
        // 应用的函数将被阻塞，getNow只有在函数完成才会执行
        assertEquals("MESSAGE", cf.getNow("Unreachable"));
    }

    @Test
    public void thenApplyAsyncExample() {
        int i = 0;
        log.info("{}", ++i);
        // thenApply 加 Async 后缀，串联起来的 CompletableFuture 可以异步地执行
        CompletableFuture<String> cf = CompletableFuture
            .completedFuture("message")
            .thenApplyAsync(s -> {
                // 使用ForkJoinPool.commonPool()
                assertTrue(Thread.currentThread().isDaemon());
                log.info("to upper case");
                sleep(300);
                return s.toUpperCase();
            });
        log.info("{}", ++i);
        assertNull(cf.getNow(null));
        log.info("{}", ++i);
        assertEquals("MESSAGE", cf.join());
        log.info("{}", ++i);
    }

    @Test
    public void thenApplyAsyncWithExecutorExample() {
        int i = 0;
        log.info("{}", ++i);
        String threadNamePrefix = "custom-executor-";
        ExecutorService executor = Executors.newFixedThreadPool(3, new SimpleThreadFactory(threadNamePrefix));
        CompletableFuture<String> cf = CompletableFuture
            .completedFuture("message")
            .thenApplyAsync(s -> {
                assertTrue(Thread.currentThread().getName().startsWith(threadNamePrefix));
                assertFalse(Thread.currentThread().isDaemon());
                log.info("to upper case");
                sleep(300);
                return s.toUpperCase();
            }, executor);
        log.info("{}", ++i);
        assertNull(cf.getNow(null));
        log.info("{}", ++i);
        assertEquals("MESSAGE", cf.join());
        log.info("{}", ++i);
    }

    @Test
    public void thenAcceptExample() {
        // 同步地执行消费前一阶段的结果
        StringBuilder result = new StringBuilder();
        CompletableFuture
            .completedFuture("thenAccept message")
            .thenAccept(result::append);
        assertTrue("Result was empty", result.length() > 0);
        log.info("result: {}", result);
    }

    @Test
    public void thenAcceptAsyncExample() {
        int i = 0;
        log.info("{}", ++i);
        StringBuilder result = new StringBuilder();
        CompletableFuture cf = CompletableFuture
            .completedFuture("thenAcceptAsync message")
            .thenAcceptAsync(msg -> {
                log.info("to append message");
                sleep(100);
                result.append(msg);
            });
        log.info("{}", ++i);
        cf.join();
        log.info("{}", ++i);
        assertTrue("Result was empty", result.length() > 0);
    }

    @Test
    public void completeExceptionallyExample() {
        int i = 0;
        log.info("{}", ++i);
        CompletableFuture<String> cf = CompletableFuture
            .completedFuture("message")
            // 在第一个函数完成后，异步地应用转大写字母函数。
            .thenApplyAsync(msg -> {
                log.info("to upper case");
                sleep(200);
                log.info("Replace implement");
                return msg.toUpperCase();
            });
        log.info("{}", ++i);
        // 创建了一个新的handler阶段，当原先的 CompletableFuture 的值计算完成或者抛出异常的时候，会触发这个 CompletableFuture 对象的计算
        CompletableFuture<String> exceptionHandler = cf.handle((s, th) -> (th != null) ? "message upon cancel" : s);
        log.info("{}", ++i);
        // 显式地用异常完成第二个阶段
        cf.completeExceptionally(new RuntimeException("completed exceptionally"));
        log.info("{}", ++i);
        assertTrue("Was not completed exceptionally", cf.isCompletedExceptionally());
        log.info("{}", ++i);
        try {
            cf.join();
            log.info("Unreachable");
        } catch (CompletionException ex) { // just for testing
            log.info("{}", ++i);
            assertEquals("completed exceptionally", ex.getCause().getMessage());
        }
        log.info("{}", ++i);
        assertEquals("message upon cancel", exceptionHandler.join());
        log.info("{}", ++i);
        sleep(300);
        // 不响应中断，实现替换
        log.info("{}", ++i);
    }

    @Test
    public void cancelExample() {
        int i = 0;
        log.info("{}", ++i);
        CompletableFuture<String> cf = CompletableFuture
            .completedFuture("message")
            .thenApplyAsync(msg -> {
                log.info("to upper case");
                sleep(300);
                log.info("Replace implement");
                return msg.toUpperCase();
            });
        sleep(200);
        log.info("{}", ++i);
        CompletableFuture<String> cf2 = cf.exceptionally(throwable -> "canceled message");
        log.info("{}", ++i);
        // 布尔参数并没有被使用，这是因为它并没有使用中断去取消操作
        // cancel 等价于 completeExceptionally(new CancellationException())。
        assertTrue("Was not canceled", cf.cancel(true));
        log.info("{}", ++i);
        assertTrue("Was not completed exceptionally", cf.isCompletedExceptionally());
        log.info("{}", ++i);
        assertEquals("canceled message", cf2.join());
        log.info("{}", ++i);
        sleep(200);
        // 不响应中断，实现替换
        log.info("{}", ++i);
    }

    @Test
    public void applyToEitherExample() {
        int i = 0;
        log.info("{}", ++i);
        String original = "Message";
        // 调整cf1的sleep时间为100或者300，可看到不同的结果
        CompletableFuture<String> cf1 = CompletableFuture
            .completedFuture(original)
            .thenApplyAsync(msg -> {
                log.info("{} to upper case", msg);
                sleep(100);
                log.info("Replace implement");
                return msg.toUpperCase();
            });
        log.info("{}", ++i);
        // 在两个完成的阶段其中之一上应用函数，哪个先完成用哪个
        CompletableFuture<String> cf2 = cf1.applyToEither(
            CompletableFuture.completedFuture(original)
                .thenApplyAsync(msg -> {
                    log.info("{} to lower case", msg);
                    sleep(200);
                    log.info("Replace implement");
                    return msg.toLowerCase();
                }),
            s -> s + " from applyToEither");
        log.info("{}", ++i);
        String join = cf2.join();
        log.info("cf2 applyToEither: {}", join);
        assertTrue(join.endsWith(" from applyToEither"));
        log.info("{}", ++i);
        sleep(200);
        log.info("{}", ++i);
    }

    @Test
    public void acceptEitherExample() {
        int i = 0;
        log.info("{}", ++i);
        String original = "Message";
        StringBuilder result = new StringBuilder();
        CompletableFuture cf = CompletableFuture
            .completedFuture(original)
            .thenApplyAsync(msg -> {
                log.info("{} to upper case", msg);
                sleep(100);
                log.info("Replace implement");
                return msg.toUpperCase();
            })
            // 在两个完成的阶段其中之一上应用函数，哪个先完成用哪个
            .acceptEither(
                CompletableFuture
                    .completedFuture(original)
                    .thenApplyAsync(msg -> {
                        log.info("{} to lower case", msg);
                        sleep(200);
                        log.info("Replace implement");
                        return msg.toLowerCase();
                    }),
                s -> result.append(s).append("acceptEither"));
        log.info("{}", ++i);
        cf.join();
        String s = result.toString();
        log.info("cf acceptEither: {}", s);
        assertTrue("Result was empty", s.endsWith("acceptEither"));
        log.info("{}", ++i);
        sleep(200);
        log.info("{}", ++i);
    }

    @Test
    public void runAfterBothExample() {
        String original = "Message";
        StringBuilder result = new StringBuilder();
        // 等待两个阶段完成后执行了一个Runnable，下面所有的阶段都是同步执行的
        CompletableFuture
            .completedFuture(original)
            .thenApply(String::toUpperCase)
            .runAfterBoth(
                CompletableFuture
                    .completedFuture(original)
                    .thenApply(String::toLowerCase),
                () -> result.append("done"));
        assertTrue("Result was empty", result.length() > 0);
    }

    @Test
    public void thenAcceptBothExample() {
        String original = "Message";
        StringBuilder result = new StringBuilder();
        // 使用BiConsumer处理两个阶段的结果
        CompletableFuture
            .completedFuture(original)
            .thenApply(String::toUpperCase)
            .thenAcceptBoth(
                CompletableFuture
                    .completedFuture(original)
                    .thenApply(String::toLowerCase),
                (s1, s2) -> result.append(s1).append(s2));
        assertEquals("MESSAGEmessage", result.toString());
    }

    @Test
    public void thenCombineExample() {
        int i = 0;
        log.info("{}", ++i);
        String original = "Message";
        // 使用BiFunction处理两个阶段的结果
        CompletableFuture<String> cf = CompletableFuture
            .completedFuture(original)
            .thenApply(msg -> {
                log.info("{} to upper case", msg);
                sleep(300);
                log.info("Replace implement");
                return msg.toUpperCase();
            })
            .thenCombine(
                CompletableFuture
                    .completedFuture(original)
                    .thenApply(msg -> {
                        log.info("{} to lower case", msg);
                        sleep(200);
                        log.info("Replace implement");
                        return msg.toLowerCase();
                    }),
                (s1, s2) -> s1 + s2);
        log.info("{}", ++i);
        // 整个流水线是同步的，所以getNow()会得到最终的结果，它把大写和小写字符串连接起来
        assertEquals("MESSAGEmessage", cf.getNow(null));
        log.info("{}", ++i);
    }

    @Test
    public void thenCombineAsyncExample() {
        int i = 0;
        log.info("{}", ++i);
        String original = "Message";
        // 异步使用BiFunction处理两个阶段的结果
        CompletableFuture cf = CompletableFuture
            .completedFuture(original)
            .thenApplyAsync(msg -> {
                log.info("{} to upper case", msg);
                sleep(300);
                log.info("Replace implement");
                return msg.toUpperCase();
            })
            // 依赖的前两个阶段异步地执行，所以thenCombine()也异步地执行，即时它没有Async后缀。
            .thenCombine(
                CompletableFuture
                    .completedFuture(original)
                    .thenApplyAsync(msg -> {
                        log.info("{} to lower case", msg);
                        sleep(200);
                        log.info("Replace implement");
                        return msg.toLowerCase();
                    }),
                (s1, s2) -> s1 + s2);
        log.info("{}", ++i);
        // 需要join方法等待结果的完成
        assertEquals("MESSAGEmessage", cf.join());
        log.info("{}", ++i);
    }

    @Test
    public void thenComposeExample() {
        int i = 0;
        log.info("{}", ++i);
        String original = "Message";
        CompletableFuture cf = CompletableFuture
            .completedFuture(original)
            .thenApply(msg -> {
                log.info("{} to upper case", msg);
                sleep(300);
                log.info("Replace implement");
                return msg.toUpperCase();
            })
            // thenCompose 等待第一个阶段的完成(大写转换)， 它的结果传给一个指定的返回CompletableFuture函数
            .thenCompose(upper -> CompletableFuture
                .completedFuture(original)
                .thenApply(msg -> {
                    log.info("{} to lower case", msg);
                    sleep(200);
                    log.info("Replace implement");
                    return msg.toLowerCase();
                })
                .thenApply(s -> upper + s));
        log.info("{}", ++i);
        assertEquals("MESSAGEmessage", cf.join());
        log.info("{}", ++i);
    }

    @Test
    public void anyOfExample() {
        int i = 0;
        log.info("{}", ++i);
        StringBuilder result = new StringBuilder();
        List<String> messages = Arrays.asList("a", "b", "c");
        Map<String, Integer> sleepMap = new HashMap<>();
        sleepMap.put("a", 200);
        sleepMap.put("b", 300);
        sleepMap.put("c", 100);
        // 当几个阶段中的一个完成，创建一个完成的阶段。
        // 以下阶段都是同步地执行(thenApply)，从anyOf中创建的CompletableFuture会立即完成
        CompletableFuture<Object> cf = CompletableFuture
            .anyOf(messages.stream()
                .map(msg -> CompletableFuture
                    .completedFuture(msg)
                    // 可以改为 thenApplyAsync
                    .thenApply(s -> {
                        log.info("{} to upper case", s);
                        sleep(sleepMap.get(s));
                        log.info("Replace implement");
                        return s.toUpperCase();
                    }))
                .toArray(CompletableFuture[]::new))
            .whenComplete((res, th) -> {
                if (th == null) {
                    log.info("res: {}", res);
                    result.append(res);
                }
            });
        log.info("{}", ++i);
        log.info("result: {}", cf.join());
        assertTrue("Result was empty", result.length() > 0);
        sleep(300);
        log.info("{}", ++i);
    }

    @Test
    public void allOfExample() {
        int i = 0;
        log.info("{}", ++i);
        List<String> messages = Arrays.asList("a", "b", "c");
        Map<String, Integer> sleepMap = new HashMap<>();
        sleepMap.put("a", 200);
        sleepMap.put("b", 300);
        sleepMap.put("c", 100);
        // 当所有的阶段都完成后创建一个阶段
        CompletableFuture<Void> cf = CompletableFuture
            .allOf(messages.stream()
                .map(msg -> CompletableFuture
                    .completedFuture(msg)
                    // 可以改为 thenApplyAsync
                    .thenApply(s -> {
                        log.info("{} to upper case", s);
                        sleep(sleepMap.get(s));
                        log.info("Replace implement");
                        return s.toUpperCase();
                    }))
                .toArray(CompletableFuture[]::new)
            ).whenComplete((res, th) -> {
                if (th == null) {
                    assertNull(res);
                    log.info("done");
                }
            });
        log.info("{}", ++i);
        cf.join();
        log.info("{}", ++i);
    }

    private void sleep(long time) {
        try {
            TimeUnit.MILLISECONDS.sleep(time);
        } catch (InterruptedException e) {
            log.error("Sleep interrupt", e);
        }
    }
}
