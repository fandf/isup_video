package com.controller;

import com.common.AjaxResult;
import com.SdkService.StreamService.SMS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@RestController
public class EHomeController {


    @Autowired
    private SMS sms;

    /**
     * 根据摄像头编号开始推流
     */
    @PostMapping("startPushStream/{luserId}")
    public AjaxResult startPushStream(@PathVariable("luserId") Integer luserId) {

        CompletableFuture<String> completableFuture = new CompletableFuture<>();

        sms.RealPlay(luserId, completableFuture);

        try {
            String result = completableFuture.get();
            System.out.println("异步结果是" + result);
            if (Objects.equals(result, "true")) {
                return AjaxResult.success();
            }
            return AjaxResult.error();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据摄像头编号停止推流
     */
    @PostMapping(value = "/stopPushStream/{luserId}")
    public AjaxResult stopPreviewDevice(@PathVariable("luserId") Integer luserId) {
        Integer i = SMS.LuserIDandSessionMap.get(luserId);
        sms.StopRealPlay(luserId, i, SMS.SessionIDAndPreviewHandleMap.get(i));
        return AjaxResult.success();
    }

}
