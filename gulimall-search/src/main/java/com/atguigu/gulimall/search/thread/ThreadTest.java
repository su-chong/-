package com.atguigu.gulimall.search.thread;

import java.util.concurrent.*;

public class ThreadTest {
    public static ExecutorService executor = Executors.newFixedThreadPool(10);

    // 用CompletableFuture做
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        System.out.println("main()已启动");

        /**
         * 下面用到的所有CompletableFuture的方法都返回的是CompletableFuture
         * 用runAsync(),里面没有result(运行结果)
         */
//        CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> {
//            System.out.println("正在运行:" + Thread.currentThread().getId());
//            int i = 10 / 2;
//            System.out.println("运行结果:" + i);
//        }, executor);

        /**
         * 用supplyAsync()，里面有result
         * whenComplete()可拿到exception和result，但不能改result
         * exceptionally()可拿到exception，可改return
         */
//        CompletableFuture<Integer> future2 = CompletableFuture.supplyAsync(() -> {
//            System.out.println("正在运行:" + Thread.currentThread().getId());
//            int i = 10 / 0;
//            System.out.println("运行结果:" + i);
//            return i;
//        }, executor).whenComplete((result,exception)->{
//            // 可感知exception，不能改return
//            System.out.println("异步任务已完成。结果是"+result+"；异常是"+exception);
//        }).exceptionally(throwable -> {
//            // 可感知exception，可改return
//            return 10;
//        });

        /**
         * handle()可拿到exception，也可改result
         */
//        CompletableFuture<Integer> future3 = CompletableFuture.supplyAsync(() -> {
//            System.out.println("正在运行:" + Thread.currentThread().getId());
//            int i = 10 / 2;
//            System.out.println("运行结果:" + i);
//            return i;
//        }, executor).handle((res,thr) -> {
//           if(res != null) {
//               return res * 2;
//           }
//           if(thr != null) {
//               return 0;
//           }
//           return 0;
//        });
//        Integer integer = future3.get();


        /**
         * 上面搞的是一件任务，本方法搞多件任务(称为线程串行化)
         * thenRunAsync不拿CompletableFuture里的result
         * thenAcceptAsync拿CompletableFuture里的result
         * thenApplyAsync拿CompletableFuture里的result，可改result
         */
//        CompletableFuture<String> future4 = CompletableFuture.supplyAsync(() -> {
//            System.out.println("正在运行:" + Thread.currentThread().getId());
//            int i = 10 / 2;
//            System.out.println("运行结果:" + i);
//            return i;
//        }, executor).thenApplyAsync((res) -> {
//            System.out.println("任务2启动了" + res);
//            return "Hello" + res;
//        }, executor);
//        System.out.println("main()已结束" + future21.get());

        /**
         * 这里搞：完成两件事后做xxx
         * thenAfterBothAsync()
         * thenAcceptAsync()
         * thenCombineAsync()
         */
//        CompletableFuture<Integer> future01 = CompletableFuture.supplyAsync(() -> {
//            System.out.println("任务1已开始：" + Thread.currentThread().getId());
//            int i = 10 / 2;
//            System.out.println("任务1已结束");
//            return i;
//        }, executor);
//
//        CompletableFuture<Integer> future02 = CompletableFuture.supplyAsync(() -> {
//            System.out.println("任务2已开始:" + Thread.currentThread().getId());
//            int i = 12 / 3;
//            System.out.println("任务2已结束");
//            return i;
//        }, executor);
//
//        CompletableFuture<Integer> future03 = future02.thenCombineAsync(future01, (res1, res2) -> {
//            System.out.println("任务3已开始");
//            return res1 + res2;
//        }, executor);
//
//        System.out.println("main()已结束 " + future03.get());

        /**
         * 这里搞：完成两件事中的任意一件后做xxx
         * runAfterEitherAsync()
         * acceptEitherAsync()
         * applyToEitherAsync()
         */
//        CompletableFuture<Integer> future01 = CompletableFuture.supplyAsync(() -> {
//            System.out.println("任务1已开始：" + Thread.currentThread().getId());
//            int i = 10 / 2;
//            System.out.println("任务1已结束");
//            return i;
//        }, executor);
//
//        CompletableFuture<Integer> future02 = CompletableFuture.supplyAsync(() -> {
//            System.out.println("任务2已开始:" + Thread.currentThread().getId());
//            int i = 12 / 3;
//            try {
//                Thread.sleep(3000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            System.out.println("任务2已结束");
//            return i;
//        }, executor);
//
//        CompletableFuture<Integer> future03 = future02.applyToEitherAsync(future01, (res) -> {
//            System.out.println("任务3已开始");
//            return res;
//        }, executor);
//
//        System.out.println("main()已结束 " + future03.get());

        /**
         * 这里搞：完成几件事后做xxx，完成几件事中的任意一件后做xxx
         * allOf() 做完所有事后，返回一个空的CompletableFuture
         * anyOf() 做完任意一件事后，返回这件事的result
         */
        CompletableFuture<String> future01 = CompletableFuture.supplyAsync(() -> {
            System.out.println("查询1");
            return "信息点1";
        }, executor);

        CompletableFuture<String> future02 = CompletableFuture.supplyAsync(() -> {
            System.out.println("查询2");
            return "信息点2";
        }, executor);

        CompletableFuture<String> future03 = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(3000);
                System.out.println("查询3");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "信息点3";
        }, executor);

        CompletableFuture<Object> anyOf = CompletableFuture.anyOf(future01, future02, future03);
        // get()阻塞式等待

        System.out.println("main()已结束 "+ anyOf.get());
    }

    public static void thread(String[] args) {
        System.out.println("main()已启动");

//        Theard01 thread01 = new Theard01();
//        thread01.start();

//        runnable01 runnable01 = new runnable01();
//        new Thread(runnable01).start();

//        FutureTask<Integer> futureTask = new FutureTask<>(new callable01());
//        new Thread(futureTask).start();
//        // 阻塞式等待
//        Integer integer = futureTask.get();

        // 造线程池的第一种方式：
        // public static ExecutorService service = Executors.newFixedThreadPool(10);
        executor.execute(new runnable01());

        // 造线程池的第二种方式：
        ThreadPoolExecutor executor = new ThreadPoolExecutor(5, 200, 10, TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(100000),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy());

        System.out.println("main()已结束");
    }

    // 一. 继承Thread
    public static class Theard01 extends Thread{

        @Override
        public void run() {
            System.out.println("正在运行:" + Thread.currentThread().getId());
            int i = 2 / 2;
            System.out.println("运行结果:" + i);
        }
    }

    // 二. 实现runnable接口
    public static class runnable01 implements Runnable {
        @Override
        public void run() {
            System.out.println("正在运行:" + Thread.currentThread().getId());
            int i = 4 / 2;
            System.out.println("运行结果:" + i);
        }
    }

    // 三. 实现callable接口
    public static class callable01 implements Callable<Integer> {

        @Override
        public Integer call() throws Exception {
            System.out.println("正在运行:" + Thread.currentThread().getId());
            int i = 6 / 2;
            System.out.println("运行结果:" + i);
            return i;
        }
    }

}