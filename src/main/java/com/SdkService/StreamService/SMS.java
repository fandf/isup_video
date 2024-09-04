package com.SdkService.StreamService;


import com.common.HandleStreamV2;
import com.common.osSelect;
import com.SdkService.CmsService.CMS;
import com.SdkService.CmsService.HCISUPCMS;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.awt.peer.LabelPeer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Component
public class SMS {


    public SMS() {
    }

    public static HCISUPSMS hcISUPSMS = null;

    public static Map<Integer,HandleStreamV2> concurrentMap = new HashMap<>();
    public static Map<Integer,Integer> PreviewHandSAndSessionIDandMap=new HashMap<>();
    public static Map<Integer,Integer> SessionIDAndPreviewHandleMap=new HashMap<>();
    public static Map<Integer,Integer> LuserIDandSessionMap=new HashMap<>();
//    public  int SmsHandle = -1; //SMS监听句柄
     static  FPREVIEW_NEWLINK_CB fPREVIEW_NEWLINK_CB;//预览监听回调函数实现
     static  FPREVIEW_DATA_CB_WIN fPREVIEW_DATA_CB_WIN;//预览回调函数实现

//    public  int lPreviewHandle = -1; //
//    public static int sessionID = -1; //预览sessionID

//    public static boolean SMSListenerFlag=false;

    public static int Count=0;//
    HCISUPSMS.NET_EHOME_LISTEN_PREVIEW_CFG struPreviewListen = new HCISUPSMS.NET_EHOME_LISTEN_PREVIEW_CFG();


    @Value("${ehome.in-ip}")
    private String ehomeInIp;

    @Value("${ehome.pu-ip}")
    private String ehomePuIp;

    @Value("${ehome.sms-preview-port}")
    private short ehomeSmsPreViewPort;

    @Value("${ehome.rtmp-url}")
    private String rtmpUrl;

    @Value("${ehome.hls-url}")
    private String hlsUrl;

    /**
     * 实例化 hcISUPSMS 对象
     *
     * @return
     */
    private static boolean CreateSDKInstance() {
        if (hcISUPSMS == null) {
            synchronized (HCISUPSMS.class) {
                String strDllPath = "";
                try {
                    if (osSelect.isWindows())
                        //win系统加载库路径
                        strDllPath = System.getProperty("user.dir") + "\\lib\\HCISUPStream.dll";
                    else if (osSelect.isLinux())
                        //Linux系统加载库路径
                        strDllPath = System.getProperty("user.dir") + "/lib/libHCISUPStream.so";
                    hcISUPSMS = (HCISUPSMS) Native.loadLibrary(strDllPath, HCISUPSMS.class);
                } catch (Exception ex) {
                    log.error("loadLibrary: " + strDllPath + " Error: " + ex.getMessage());
                    return false;
                }
            }
        }
        return true;
    }

    @PostConstruct
    public void SMS_Init() {
        if (hcISUPSMS == null) {
            if (!CreateSDKInstance()) {
                log.error("加载SMS SDK 失败");
                return;
            }
        }
        //根据系统加载对应的库
        if (osSelect.isWindows()) {
            HCISUPCMS.BYTE_ARRAY ptrByteArrayCrypto = new HCISUPCMS.BYTE_ARRAY(256);
            String strPathCrypto = System.getProperty("user.dir") + "\\lib\\libeay32.dll"; //Linux版本是libcrypto.so库文件的路径
            System.arraycopy(strPathCrypto.getBytes(), 0, ptrByteArrayCrypto.byValue, 0, strPathCrypto.length());
            ptrByteArrayCrypto.write();
            if (!hcISUPSMS.NET_ESTREAM_SetSDKInitCfg(0, ptrByteArrayCrypto.getPointer())) {
                System.out.println("NET_ESTREAM_SetSDKInitCfg 0 failed, error:" + hcISUPSMS.NET_ESTREAM_GetLastError());
            }

            HCISUPCMS.BYTE_ARRAY ptrByteArraySsl = new HCISUPCMS.BYTE_ARRAY(256);
            String strPathSsl = System.getProperty("user.dir") + "\\lib\\ssleay32.dll";    //Linux版本是libssl.so库文件的路径
            System.arraycopy(strPathSsl.getBytes(), 0, ptrByteArraySsl.byValue, 0, strPathSsl.length());
            ptrByteArraySsl.write();
            if (!hcISUPSMS.NET_ESTREAM_SetSDKInitCfg(1, ptrByteArraySsl.getPointer())) {
                System.out.println("NET_ESTREAM_SetSDKInitCfg 1 failed, error:" + hcISUPSMS.NET_ESTREAM_GetLastError());
            }
            //流媒体初始化
            boolean b = hcISUPSMS.NET_ESTREAM_Init();
            if(b){
                log.info("SMS 流媒体初始化成功!");
                SMS_StartListen();
            }else {
                log.error("SMS 流媒体初始化失败! 错误码:"+ hcISUPSMS.NET_ESTREAM_GetLastError());
            }
            //设置HCAapSDKCom组件库文件夹所在路径
            HCISUPCMS.BYTE_ARRAY ptrByteArrayCom = new HCISUPCMS.BYTE_ARRAY(256);
            String strPathCom = System.getProperty("user.dir") + "\\lib\\HCAapSDKCom";      //只支持绝对路径，建议使用英文路径
            System.arraycopy(strPathCom.getBytes(), 0, ptrByteArrayCom.byValue, 0, strPathCom.length());
            ptrByteArrayCom.write();
            if (!hcISUPSMS.NET_ESTREAM_SetSDKLocalCfg(5, ptrByteArrayCom.getPointer())) {
                System.out.println("NET_ESTREAM_SetSDKLocalCfg 5 failed, error:" + hcISUPSMS.NET_ESTREAM_GetLastError());
            }
            hcISUPSMS.NET_ESTREAM_SetLogToFile(3, "..\\EHomeSDKLog", false);
        } else if (osSelect.isLinux()) {
            //设置libcrypto.so所在路径
            HCISUPCMS.BYTE_ARRAY ptrByteArrayCrypto = new HCISUPCMS.BYTE_ARRAY(256);
            String strPathCrypto = System.getProperty("user.dir") + "/lib/libcrypto.so"; //Linux版本是libcrypto.so库文件的路径
            System.arraycopy(strPathCrypto.getBytes(), 0, ptrByteArrayCrypto.byValue, 0, strPathCrypto.length());
            ptrByteArrayCrypto.write();
            if (!hcISUPSMS.NET_ESTREAM_SetSDKInitCfg(0, ptrByteArrayCrypto.getPointer())) {
                System.out.println("NET_ESTREAM_SetSDKInitCfg 0 failed, error:" + hcISUPSMS.NET_ESTREAM_GetLastError());
            }
            //设置libssl.so所在路径
            HCISUPCMS.BYTE_ARRAY ptrByteArraySsl = new HCISUPCMS.BYTE_ARRAY(256);
            String strPathSsl = System.getProperty("user.dir") + "/lib/libssl.so";    //Linux版本是libssl.so库文件的路径
            System.arraycopy(strPathSsl.getBytes(), 0, ptrByteArraySsl.byValue, 0, strPathSsl.length());
            ptrByteArraySsl.write();
            if (!hcISUPSMS.NET_ESTREAM_SetSDKInitCfg(1, ptrByteArraySsl.getPointer())) {
                System.out.println("NET_ESTREAM_SetSDKInitCfg 1 failed, error:" + hcISUPSMS.NET_ESTREAM_GetLastError());
            }
            //流媒体初始化
            boolean b = hcISUPSMS.NET_ESTREAM_Init();
            if(b){
                log.info("SMS 流媒体初始化成功!");
                SMS_StartListen();
            }else {
                log.error("SMS 流媒体初始化失败! 错误码:"+ hcISUPSMS.NET_ESTREAM_GetLastError());
            }
            //设置HCAapSDKCom组件库文件夹所在路径
            HCISUPCMS.BYTE_ARRAY ptrByteArrayCom = new HCISUPCMS.BYTE_ARRAY(256);
            String strPathCom = System.getProperty("user.dir") + "/lib/HCAapSDKCom/";      //只支持绝对路径，建议使用英文路径
            System.arraycopy(strPathCom.getBytes(), 0, ptrByteArrayCom.byValue, 0, strPathCom.length());
            ptrByteArrayCom.write();
            if (!hcISUPSMS.NET_ESTREAM_SetSDKLocalCfg(5, ptrByteArrayCom.getPointer())) {
                System.out.println("NET_ESTREAM_SetSDKLocalCfg 5 failed, error:" + hcISUPSMS.NET_ESTREAM_GetLastError());
            }
            hcISUPSMS.NET_ESTREAM_SetLogToFile(3, "./EHomeSDKLog", false);
        }

    }

    /**
     * 开启实时预览监听(带界面窗口)
     */
    public void SMS_StartListen() {

        //预览监听
        if (fPREVIEW_NEWLINK_CB == null) {
            fPREVIEW_NEWLINK_CB = new FPREVIEW_NEWLINK_CB();
        }
        struPreviewListen.struIPAdress.szIP=ehomeInIp.getBytes();
        struPreviewListen.struIPAdress.wPort = ehomeSmsPreViewPort; //流媒体服务器监听端口
        struPreviewListen.fnNewLinkCB = fPREVIEW_NEWLINK_CB; //预览连接请求回调函数
        struPreviewListen.pUser = null;
        struPreviewListen.byLinkMode = 0; //0- TCP方式，1- UDP方式
        struPreviewListen.write();

        int SmsHandle = hcISUPSMS.NET_ESTREAM_StartListenPreview(struPreviewListen);

        if (SmsHandle <0) {
            log.error("SMS流媒体服务监听失败, 错误码:"+hcISUPSMS.NET_ESTREAM_GetLastError());
//            SMSListenerFlag=false;
            hcISUPSMS.NET_ESTREAM_Fini();
            return;
            }
        else {
            String StreamListenInfo = new String(struPreviewListen.struIPAdress.szIP).trim() + "_" + struPreviewListen.struIPAdress.wPort;
            log.info("SMS流媒体服务:" + StreamListenInfo + "监听成功!");
//            SMSListenerFlag=true;
        }
    }


    /**
     * 实时预览数据回调（带窗口）
     */
    public class FPREVIEW_NEWLINK_CB implements HCISUPSMS.PREVIEW_NEWLINK_CB {
        @Override
        public boolean invoke(int lLinkHandle, HCISUPSMS.NET_EHOME_NEWLINK_CB_MSG pNewLinkCBMsg, Pointer pUserData) {

            HCISUPSMS.NET_EHOME_PREVIEW_DATA_CB_PARAM struDataCB = new HCISUPSMS.NET_EHOME_PREVIEW_DATA_CB_PARAM();

            log.info("参数解析,lLinkHandle:"+lLinkHandle+"设备会话ID"+pNewLinkCBMsg.iSessionID);

            //双向存储session和lLinkHandle
            PreviewHandSAndSessionIDandMap.put(lLinkHandle,pNewLinkCBMsg.iSessionID);
            SessionIDAndPreviewHandleMap.put(pNewLinkCBMsg.iSessionID,lLinkHandle);

            if (fPREVIEW_DATA_CB_WIN == null) {
                fPREVIEW_DATA_CB_WIN = new FPREVIEW_DATA_CB_WIN();
            }

            struDataCB.fnPreviewDataCB = fPREVIEW_DATA_CB_WIN;
            //注册回调函数以接收实时码流
            if (!hcISUPSMS.NET_ESTREAM_SetPreviewDataCB(lLinkHandle, struDataCB)) {
                log.error("NET_ESTREAM_SetPreviewDataCB failed err:：" + hcISUPSMS.NET_ESTREAM_GetLastError());
                return false;
            }
            return true;
        }
    }
    /**
     * 预览数据的回调函数 - 窗口实时预览
     */
    public class FPREVIEW_DATA_CB_WIN implements HCISUPSMS.PREVIEW_DATA_CB {

        //实时流回调函数/
        @Override
        public void invoke(int iPreviewHandle, HCISUPSMS.NET_EHOME_PREVIEW_CB_MSG pPreviewCBMsg, Pointer pUserData) {


            switch (pPreviewCBMsg.byDataType) {
                case HCNetSDK.NET_DVR_SYSHEAD: //系统头
                {
                    log.info("系统头:"+pPreviewCBMsg.pRecvdata);
                }
                case HCNetSDK.NET_DVR_STREAMDATA:   //码流数据
                {

                    byte[] dataStream = pPreviewCBMsg.pRecvdata.getByteArray(0, pPreviewCBMsg.dwDataLen);

                    if(dataStream!=null){
                        Integer l = PreviewHandSAndSessionIDandMap.get(iPreviewHandle);
                        HandleStreamV2 handleStreamV2 = concurrentMap.get(l);
                        handleStreamV2.processStream(dataStream);
                    }
                }
            }
        }
    }

    /**
     * 开启预览，
     *
     * @param luserID 预览通道号
     */
    public void RealPlay(int luserID,CompletableFuture<String> completableFutureOne) {

        HCISUPCMS.NET_EHOME_PREVIEWINFO_IN struPreviewIn = new HCISUPCMS.NET_EHOME_PREVIEWINFO_IN();
        struPreviewIn.iChannel = 1; //通道号
        struPreviewIn.dwLinkMode = 0; //0- TCP方式，1- UDP方式
        struPreviewIn.dwStreamType = 0; //码流类型：0- 主码流，1- 子码流, 2- 第三码流
        struPreviewIn.struStreamSever.szIP =ehomePuIp.getBytes(); ;//流媒体服务器IP地址,公网地址
        struPreviewIn.struStreamSever.wPort = ehomeSmsPreViewPort; //流媒体服务器端口，需要跟服务器启动监听端口一致
        struPreviewIn.write();
        //预览请求
        HCISUPCMS.NET_EHOME_PREVIEWINFO_OUT struPreviewOut = new HCISUPCMS.NET_EHOME_PREVIEWINFO_OUT();
        //请求开始预览
        if (!CMS.hcISUPCMS.NET_ECMS_StartGetRealStream(luserID, struPreviewIn, struPreviewOut)) {
            log.error("请求开始预览失败,错误码:"+CMS.hcISUPCMS.NET_ECMS_GetLastError());
            return;
        } else {
            struPreviewOut.read();
            log.info("请求预览成功, sessionID:" + struPreviewOut.lSessionID);
//            sessionID = struPreviewOut.lSessionID;
        }
        HCISUPCMS.NET_EHOME_PUSHSTREAM_IN struPushInfoIn = new HCISUPCMS.NET_EHOME_PUSHSTREAM_IN();
        struPushInfoIn.read();
        struPushInfoIn.dwSize = struPushInfoIn.size();
        struPushInfoIn.lSessionID = struPreviewOut.lSessionID;
        struPushInfoIn.write();
        HCISUPCMS.NET_EHOME_PUSHSTREAM_OUT struPushInfoOut = new HCISUPCMS.NET_EHOME_PUSHSTREAM_OUT();
        struPushInfoOut.read();
        struPushInfoOut.dwSize = struPushInfoOut.size();
        struPushInfoOut.write();
        //中心管理服务器（CMS）向设备发送请求，设备开始传输预览实时码流
        if (!CMS.hcISUPCMS.NET_ECMS_StartPushRealStream(luserID, struPushInfoIn, struPushInfoOut)) {
            log.error("CMS向设备发送请求预览实时码流失败, error code:" + CMS.hcISUPCMS.NET_ECMS_GetLastError());
            return;
        } else {
            log.info("CMS向设备发送请求预览实时码流成功, sessionID:" + struPushInfoIn.lSessionID);

            if(LuserIDandSessionMap.get(luserID)==null){
                LuserIDandSessionMap.put(luserID,struPushInfoIn.lSessionID);
            }

            if(concurrentMap.get(struPushInfoIn.lSessionID)==null){
                log.info("加入concurrentMap :" + luserID);
                concurrentMap.put(struPushInfoIn.lSessionID, new HandleStreamV2(rtmpUrl+luserID, completableFutureOne));
            }
        }
    }

    /**
     * 停止预览,Stream服务停止实时流转发，CMS向设备发送停止预览请求
     */
    public void StopRealPlay(int luserID,int sessionID,int lPreviewHandle) {
        //停止某一通道转发预览实时码流
        if (!hcISUPSMS.NET_ESTREAM_StopPreview(lPreviewHandle)) {
            log.error("停止转发预览实时码流失败, 错误码: " + hcISUPSMS.NET_ESTREAM_GetLastError());
            return;
        }
        log.info("停止Stream的实时流转发");
        //请求停止预览
        if (!CMS.hcISUPCMS.NET_ECMS_StopGetRealStream(luserID, sessionID)) {
            System.out.println("请求停止预览失败,错误码: " + CMS.hcISUPCMS.NET_ECMS_GetLastError());
            return;
        }
        //停止线程
        HandleStreamV2 handleStreamV2 = concurrentMap.get(sessionID);
        handleStreamV2.stopProcessing();

        concurrentMap.remove(sessionID);
        PreviewHandSAndSessionIDandMap.remove(lPreviewHandle);
        LuserIDandSessionMap.remove(luserID);
        SessionIDAndPreviewHandleMap.remove(sessionID);

        if(!concurrentMap.containsKey(sessionID)&&!PreviewHandSAndSessionIDandMap.containsKey(lPreviewHandle)&&!LuserIDandSessionMap.containsKey(luserID)&&!SessionIDAndPreviewHandleMap.containsKey(sessionID)){
            log.info("会话"+sessionID+"相关资源已被清空");
        }


        log.info("CMS已发送停止预览请求");
    }

}
