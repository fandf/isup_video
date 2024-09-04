package com.SdkService.CmsService;

import com.common.osSelect;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;

@Slf4j
@Component
public class CMS {
    public static HCISUPCMS hcISUPCMS = null;
    public static int CmsHandle = -1; //CMS监听句柄

    static FRegisterCallBack fRegisterCallBack;//注册回调函数实现

    HCISUPCMS.NET_EHOME_CMS_LISTEN_PARAM struCMSListenParam = new HCISUPCMS.NET_EHOME_CMS_LISTEN_PARAM();

    @Value("${ehome.pu-ip}")
    private String ehomePuIp;

    @Value("${ehome.in-ip}")
    private String ehomeInIp;

    @Value("${ehome.cms-port}")
    private short ehomeCmsPort;

//    @Value("${ehome.ams-prot}")
//    private short ehomeAmsProt;

    @Value("${ehome.secret-key}")
    private String secretKey;
    /**
     * 实例化 hcISUPCMS 对象
     *
     * @return
     */
    private static boolean CreateSDKInstance() {
        if (hcISUPCMS == null) {
            synchronized (HCISUPCMS.class) {
                String strDllPath = "";
                try {
                    //System.setProperty("jna.debug_load", "true");
                    if (osSelect.isWindows())
                        //win系统加载库路径(路径不要带中文)
                        strDllPath = System.getProperty("user.dir") + "\\lib\\HCISUPCMS.dll";
                    else if (osSelect.isLinux())
                        //Linux系统加载库路径(路径不要带中文)
                        strDllPath = System.getProperty("user.dir")+"/lib/libHCISUPCMS.so";
                    hcISUPCMS = (HCISUPCMS) Native.loadLibrary(strDllPath, HCISUPCMS.class);
                } catch (Exception ex) {
                    log.error("loadLibrary: " + strDllPath + " Error: " + ex.getMessage());
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 初始化CMS注册中心
     * @throws IOException
     */
    @PostConstruct
    public void CMS_Init() throws IOException {

        if (hcISUPCMS == null) {
            if (!CreateSDKInstance()) {
                log.error("加载CMS SDK 失败");
                return;
            }
        }
        //根据系统加载对应的库
        if (osSelect.isWindows()) {
            //设置openSSL库的路径
            HCISUPCMS.BYTE_ARRAY ptrByteArrayCrypto = new HCISUPCMS.BYTE_ARRAY(256);
            String strPathCrypto = System.getProperty("user.dir") + "\\lib\\libeay32.dll"; //Linux版本是libcrypto.so库文件的路径
            System.arraycopy(strPathCrypto.getBytes(), 0, ptrByteArrayCrypto.byValue, 0, strPathCrypto.length());
            ptrByteArrayCrypto.write();
            hcISUPCMS.NET_ECMS_SetSDKInitCfg(0, ptrByteArrayCrypto.getPointer());

            //设置libssl.so所在路径
            HCISUPCMS.BYTE_ARRAY ptrByteArraySsl = new HCISUPCMS.BYTE_ARRAY(256);
            String strPathSsl = System.getProperty("user.dir") + "\\lib\\ssleay32.dll";    //Linux版本是libssl.so库文件的路径
            System.arraycopy(strPathSsl.getBytes(), 0, ptrByteArraySsl.byValue, 0, strPathSsl.length());
            ptrByteArraySsl.write();
            hcISUPCMS.NET_ECMS_SetSDKInitCfg(1, ptrByteArraySsl.getPointer());
            //注册服务初始化
            boolean binit = hcISUPCMS.NET_ECMS_Init();
            if(binit){
                log.info("CMS 注册中心初始化成功!");
                CMS_StartListen();
            }else {
                log.error("CMS 注册中心初始化失败! 错误码:"+hcISUPCMS.NET_ECMS_GetLastError());
            }
            //设置HCAapSDKCom组件库文件夹所在路径
            HCISUPCMS.BYTE_ARRAY ptrByteArrayCom = new HCISUPCMS.BYTE_ARRAY(256);
            String strPathCom = System.getProperty("user.dir") + "\\lib\\HCAapSDKCom";        //只支持绝对路径，建议使用英文路径
            System.arraycopy(strPathCom.getBytes(), 0, ptrByteArrayCom.byValue, 0, strPathCom.length());
            ptrByteArrayCom.write();
            hcISUPCMS.NET_ECMS_SetSDKLocalCfg(5, ptrByteArrayCom.getPointer());

        }
        else if (osSelect.isLinux()) {
            HCISUPCMS.BYTE_ARRAY ptrByteArrayCrypto = new HCISUPCMS.BYTE_ARRAY(256);
            String strPathCrypto = System.getProperty("user.dir") + "/lib/libcrypto.so"; //Linux版本是libcrypto.so库文件的路径
            System.arraycopy(strPathCrypto.getBytes(), 0, ptrByteArrayCrypto.byValue, 0, strPathCrypto.length());
            ptrByteArrayCrypto.write();
            hcISUPCMS.NET_ECMS_SetSDKInitCfg(0, ptrByteArrayCrypto.getPointer());

            //设置libssl.so所在路径
            HCISUPCMS.BYTE_ARRAY ptrByteArraySsl = new HCISUPCMS.BYTE_ARRAY(256);
            String strPathSsl = System.getProperty("user.dir") + "/lib/libssl.so";    //Linux版本是libssl.so库文件的路径
            System.arraycopy(strPathSsl.getBytes(), 0, ptrByteArraySsl.byValue, 0, strPathSsl.length());
            ptrByteArraySsl.write();
            hcISUPCMS.NET_ECMS_SetSDKInitCfg(1, ptrByteArraySsl.getPointer());
            //注册服务初始化
            boolean binit = hcISUPCMS.NET_ECMS_Init();
            if(binit){
                log.info("CMS 注册中心初始化成功!");
                CMS_StartListen();
            }else {
                log.error("CMS 注册中心初始化失败! 错误码:"+hcISUPCMS.NET_ECMS_GetLastError());
            }
            //设置HCAapSDKCom组件库文件夹所在路径
            HCISUPCMS.BYTE_ARRAY ptrByteArrayCom = new HCISUPCMS.BYTE_ARRAY(256);
            String strPathCom = System.getProperty("user.dir") + "/lib/HCAapSDKCom/";        //只支持绝对路径，建议使用英文路径
            System.arraycopy(strPathCom.getBytes(), 0, ptrByteArrayCom.byValue, 0, strPathCom.length());
            ptrByteArrayCom.write();
            hcISUPCMS.NET_ECMS_SetSDKLocalCfg(5, ptrByteArrayCom.getPointer());
        }

    }


    /**
     * 开启CMS监听 以接收设备注册信息
     */
    public void CMS_StartListen()
    {
        //实例化注册回调函数，便于处理设备事件
        if (fRegisterCallBack == null) {
            fRegisterCallBack = new FRegisterCallBack();
        }
        //设置CMS监听参数
        struCMSListenParam.struAddress.szIP=ehomeInIp.getBytes();
        struCMSListenParam.struAddress.wPort = ehomeCmsPort;
        struCMSListenParam.fnCB = fRegisterCallBack;
        struCMSListenParam.write();

        //启动监听，接收设备注册信息
        CmsHandle = hcISUPCMS.NET_ECMS_StartListen(struCMSListenParam);
        if (CmsHandle < 0) {
            log.error("CMS注册中心监听失败, 错误码:" + hcISUPCMS.NET_ECMS_GetLastError());
            hcISUPCMS.NET_ECMS_Fini();
            return;
        }
        String CmsListenInfo = new String(struCMSListenParam.struAddress.szIP).trim() + "_" + struCMSListenParam.struAddress.wPort;
        log.info("CMS注册服务器:" + CmsListenInfo + "监听成功!");

    }

    //当有设备注册后 回调的函数
    public class FRegisterCallBack implements HCISUPCMS.DEVICE_REGISTER_CB {
        public boolean invoke(int lUserID, int dwDataType, Pointer pOutBuffer, int dwOutLen, Pointer pInBuffer, int dwInLen, Pointer pUser) {

            log.info("注册回调 ,dwDataType:" + dwDataType + ", lUserID:" + lUserID);

            switch (dwDataType) {
                case HCISUPCMS.EHOME_REGISTER_TYPE.ENUM_DEV_ON:  //设备上线
                    HCISUPCMS.NET_EHOME_DEV_REG_INFO_V12 strDevRegInfo = new HCISUPCMS.NET_EHOME_DEV_REG_INFO_V12();
                    strDevRegInfo.write();
                    Pointer pDevRegInfo = strDevRegInfo.getPointer();
                    pDevRegInfo.write(0, pOutBuffer.getByteArray(0, strDevRegInfo.size()), 0, strDevRegInfo.size());
                    strDevRegInfo.read();

                    log.info("设备上线==========>,DeviceID:"+ new String(strDevRegInfo.struRegInfo.byDeviceID).trim());

                    // FIXME demo逻辑中默认只支持一台设备的功能演示，多台设备需要自行调整这里设备登录后的句柄信息
//                    IsupTest.lLoginID = lUserID;
                    return true;
                case HCISUPCMS.EHOME_REGISTER_TYPE.ENUM_DEV_AUTH: //ENUM_DEV_AUTH
                    strDevRegInfo = new HCISUPCMS.NET_EHOME_DEV_REG_INFO_V12();
                    strDevRegInfo.write();
                    pDevRegInfo = strDevRegInfo.getPointer();
                    pDevRegInfo.write(0, pOutBuffer.getByteArray(0, strDevRegInfo.size()), 0, strDevRegInfo.size());
                    strDevRegInfo.read();
                    byte[] bs = new byte[0];
                    String szEHomeKey = secretKey; //ISUP5.0登录校验值
                    bs = szEHomeKey.getBytes();
                    pInBuffer.write(0, bs, 0, szEHomeKey.length());
                    break;
                case HCISUPCMS.EHOME_REGISTER_TYPE.ENUM_DEV_SESSIONKEY: //Ehome5.0设备Sessionkey回调
                    strDevRegInfo = new HCISUPCMS.NET_EHOME_DEV_REG_INFO_V12();
                    strDevRegInfo.write();
                    pDevRegInfo = strDevRegInfo.getPointer();
                    pDevRegInfo.write(0, pOutBuffer.getByteArray(0, strDevRegInfo.size()), 0, strDevRegInfo.size());
                    strDevRegInfo.read();
                    HCISUPCMS.NET_EHOME_DEV_SESSIONKEY struSessionKey = new HCISUPCMS.NET_EHOME_DEV_SESSIONKEY();
                    System.arraycopy(strDevRegInfo.struRegInfo.byDeviceID, 0, struSessionKey.sDeviceID, 0, strDevRegInfo.struRegInfo.byDeviceID.length);
                    System.arraycopy(strDevRegInfo.struRegInfo.bySessionKey, 0, struSessionKey.sSessionKey, 0, strDevRegInfo.struRegInfo.bySessionKey.length);
                    struSessionKey.write();
                    Pointer pSessionKey = struSessionKey.getPointer();
                    hcISUPCMS.NET_ECMS_SetDeviceSessionKey(pSessionKey);
//                    AlarmDemo.hcEHomeAlarm.NET_EALARM_SetDeviceSessionKey(pSessionKey);
                    break;
                case HCISUPCMS.EHOME_REGISTER_TYPE.ENUM_DEV_DAS_REQ: //Ehome5.0设备重定向请求回调
                    String dasInfo = "{\n" +
                            "    \"Type\":\"DAS\",\n" +
                            "    \"DasInfo\": {\n" +
                            "        \"Address\":\"" + ehomePuIp + "\",\n" +
                            "        \"Domain\":\"\",\n" +
                            "        \"ServerID\":\"\",\n" +
                            "        \"Port\":" + ehomeCmsPort + ",\n" +
                            "        \"UdpPort\":\n" +
                            "    }\n" +
                            "}";
                    byte[] bs1 = dasInfo.getBytes();
                    pInBuffer.write(0, bs1, 0, dasInfo.length());
                    break;
                default:
                    log.info("回调类型为:"+dwDataType);
                    break;
            }
            return true;
        }
    }
}
