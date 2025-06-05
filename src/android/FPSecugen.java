package sa.com.plugin;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;

import SecuGen.FDxSDKPro.JSGFPLib;
import SecuGen.FDxSDKPro.SGFDxConstant;
import SecuGen.FDxSDKPro.SGFDxDeviceName;
import SecuGen.FDxSDKPro.SGFDxErrorCode;
import SecuGen.FDxSDKPro.SGFDxTemplateFormat;
import SecuGen.FDxSDKPro.SGFingerInfo;
import SecuGen.FDxSDKPro.SGFingerPosition;
import SecuGen.FDxSDKPro.SGImpressionType;
import SecuGen.FDxSDKPro.SGWSQLib;

public class FPSecugen extends CordovaPlugin {

    static final String TAG = "SecuGen USB";
    private static String templatePath = "/sdcard/Download/fprints/";
    private static String serverUrl = "";
    private static String serverUrlFilepath = "";
    private static String serverKey = "";
    private static String projectName = "";
    private static String templateFormat = "";

    private IntentFilter filter;

    private int QUALITY_VALUE = 0;

    // actions
    private static final String ACTION_REQUEST_PERMISSION = "requestPermission";
    private static final String COOLMETHOD = "coolMethod";
    private static final String REGISTER = "register";
    private static final String IDENTIFY = "identify";
    private static final String ACTION_CAPTURE = "capture";
    private static final String ACTION_CLOSE = "close";
    private static final String BLINK = "blink";
    private static final String VERIFY = "verify";
    private static final String SCAN = "scan";
    private static final String OPEN = "open";
    private static final String EXITAPP = "exitapp";
    private byte[] mRegisterImage;
    private byte[] mVerifyImage;
    private byte[] mRegisterTemplate;
    private int[] mMaxTemplateSize;
    private int mImageWidth;
    private int mImageHeight;
    private boolean mLed;

    private JSGFPLib sgfplib;

    private Context context;

    long dwTimeStart = 0, dwTimeEnd = 0, dwTimeElapsed = 0;
    // UsbManager instance to deal with permission and opening
    private UsbManager manager;

    //    private AfisEngine afis;
    private ScanProperties props;
    private PendingIntent mPermissionIntent;
    private boolean usbPermissionRequested;
    private int mImageDPI;
    private boolean bSecuGenDeviceOpened;
    private String serialNumber;
    private UsbBroadcastReceiver usbReceiver;
    private int count = 0;

    public void initialize(CordovaInterface cordova, CordovaWebView view) {
        super.initialize(cordova, view);

        context = cordova.getActivity().getBaseContext();

        String path = "/sdcard/Download/fprints/";
        File templatePathFile = new File(templatePath);
        templatePathFile.mkdirs();
        FPSecugen.setTemplatePath(path);
        // LOG.d(TAG,"this.cordova.getActivity().getPackageName(): " + this.cordova.getActivity().getPackageName());
        // int id = context.getResources().getIdentifier("templatePath", "string", this.cordova.getActivity().getPackageName());
        // LOG.d(TAG,"templatePath id: " + id);
        // String translatedValue = context.getResources().getString(id);
        // LOG.d(TAG,"translatedValue: " + translatedValue);
        // File templatePathFile = new File(templatePath);
        // templatePathFile.mkdirs();
        // SecugenPlugin.setTemplatePath(translatedValue);
        // id = context.getResources().getIdentifier("serverUrl", "string", this.cordova.getActivity().getPackageName());
        // LOG.d(TAG,"serverUrl id: " + id);
        // String serverUrl = context.getResources().getString(id);
        // LOG.d(TAG,"serverUrl: " + serverUrl);
        // SecugenPlugin.setServerUrl(serverUrl);
        // id = context.getResources().getIdentifier("serverKey", "string", this.cordova.getActivity().getPackageName());
        // String serverKey = context.getResources().getString(id);
        // LOG.d(TAG,"serverKey: " + serverKey);
        // SecugenPlugin.setServerKey(serverKey);
        // id = context.getResources().getIdentifier("projectName", "string", this.cordova.getActivity().getPackageName());
        // String projectName = context.getResources().getString(id);
        // LOG.d(TAG,"projectName: " + projectName);
        // SecugenPlugin.setProjectName(projectName);
        // id = context.getResources().getIdentifier("templateFormat", "string", this.cordova.getActivity().getPackageName());
        // String templateFormat = context.getResources().getString(id);
        // LOG.d(TAG,"templateFormat: " + templateFormat);
        // SecugenPlugin.setTemplateFormat(templateFormat);
        // id = context.getResources().getIdentifier("serverUrlFilepath", "string", this.cordova.getActivity().getPackageName());
        // LOG.d(TAG,"serverUrlFilepath id: " + id);
        // String serverUrlFilepath = context.getResources().getString(id);
        // LOG.d(TAG,"serverUrlFilepath: " + serverUrlFilepath);
        // SecugenPlugin.setServerUrlFilepath(serverUrlFilepath);
    }

    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {
        mMaxTemplateSize = new int[1];
        if (action.equals("greet")) {

            String name = data.getString(0);
            String message = "Hello, " + name;
            callbackContext.success(message);

            return true;

        } else if (action.equals(ACTION_REQUEST_PERMISSION)) {
            requestPermission2(callbackContext);
            return true;
        } else if (action.equals(ACTION_CAPTURE)) {
            capture(callbackContext);
            return true;

        } else if (action.equals(OPEN)) {
            initDeviceSettings();
            return true;
        } else if (action.equals(ACTION_CLOSE)) {

            closeDevice();
            callbackContext.success("Closed");

            return true;

        } else if (action.equals(EXITAPP)) {
            exitApplication();
            callbackContext.success("Closed Application");

            return true;
        } else if (action.equals("close")) {

            String name = data.getString(0);
            String message = "Hello, " + name;
            callbackContext.success(message);

            return true;

        } else {
            return false;
        }
    }

    private int analyzeRidgeClarity(byte[] imageData, int width, int height) {
        int transitions = 0;
        int cleanTransitions = 0;

        // Analyze horizontal scanlines at regular intervals
        int scanlines = 10;
        int step = height / scanlines;

        for (int y = step; y < height; y += step) {
            // Convert signed byte to unsigned int (0-255)
            int lastVal = imageData[y * width] & 0xFF;
            boolean inRidge = (lastVal < 128);

            for (int x = 1; x < width; x++) {
                // Convert signed byte to unsigned int (0-255)
                int val = imageData[y * width + x] & 0xFF;
                boolean currentRidge = (val < 128);

                if (inRidge != currentRidge) {
                    transitions++;
                    // Check for clean transitions
                    if (Math.abs(val - lastVal) > 20) { // Reduced threshold
                        cleanTransitions++;
                    }
                    inRidge = currentRidge;
                }
                lastVal = val;
            }
        }

        // Debug info
        Log.d("RidgeClarity", "Transitions: " + transitions + ", Clean: " + cleanTransitions);
        return (transitions > 0) ? (cleanTransitions * 100 / transitions) : 0;
    }

    private float calculateEdgeQuality(byte[] imageData, int width, int height) {
        byte[] edges = new byte[width * height];

        // Apply simple edge detection
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                // Simplified Sobel operator
                int gx = Math.abs(imageData[y*width + x+1] & 0xFF - imageData[y*width + x-1] & 0xFF);
                int gy = Math.abs(imageData[(y+1)*width + x] & 0xFF - imageData[(y-1)*width + x] & 0xFF);
                edges[y*width + x] = (byte)Math.min(255, gx + gy);
            }
        }

        // Calculate edge quality metrics
        int strongEdges = 0;
        int weakEdges = 0;

        for (byte edge : edges) {
            int val = edge & 0xFF;
            if (val > 80) strongEdges++;
            else if (val > 30) weakEdges++;
        }

        // Good ratio of strong to weak edges indicates clear pattern
        return strongEdges > 0 ? (float)strongEdges / (strongEdges + weakEdges) * 100 : 0;
    }

    private int assessPatternContinuity(byte[] imageData, int width, int height) {
        // Sample ridge widths across the image
        int[] ridgeWidths = new int[100];
        int samples = 0;

        for (int y = height/4; y < height*3/4; y += height/20) {
            int currentRun = 0;
            // Set initial ridge state based on first pixel
            boolean inRidge = (imageData[y * width] & 0xFF) < 128;

            for (int x = 0; x < width; x++) {
                // Convert signed byte to unsigned int (0-255)
                boolean isRidge = (imageData[y * width + x] & 0xFF) < 128;

                if (isRidge == inRidge) {
                    currentRun++;
                } else {
                    // Save the run length if we're exiting a ridge
                    if (inRidge && samples < ridgeWidths.length && currentRun > 1) {
                        ridgeWidths[samples++] = currentRun;
                    }
                    currentRun = 1;
                    inRidge = isRidge;
                }
            }

            // Don't forget the last run if it's a ridge
            if (inRidge && samples < ridgeWidths.length && currentRun > 1) {
                ridgeWidths[samples++] = currentRun;
            }
        }

        // Debug info
        Log.d("PatternContinuity", "Samples collected: " + samples);

        // Calculate consistency score
        return calculateConsistencyScore(ridgeWidths, samples);
    }

    private int calculateConsistencyScore(int[] widths, int count) {
        if (count < 3) {
            Log.d("ConsistencyScore", "Not enough samples: " + count);
            return 0; // Not enough data points
        }

        // Calculate mean
        double sum = 0;
        for (int i = 0; i < count; i++) {
            sum += widths[i];
        }
        double mean = sum / count;

        if (mean <= 0) {
            Log.d("ConsistencyScore", "Invalid mean: " + mean);
            return 0;
        }

        // Calculate standard deviation
        double variance = 0;
        for (int i = 0; i < count; i++) {
            variance += Math.pow(widths[i] - mean, 2);
        }
        double stdDev = Math.sqrt(variance / count);

        // Convert to score (lower std dev = higher consistency = higher score)
        double normalizedStdDev = Math.min(stdDev / mean, 1.0);
        int score = (int)(100 * (1.0 - normalizedStdDev));

        Log.d("ConsistencyScore", "Mean: " + mean + ", StdDev: " + stdDev + ", Score: " + score);
        return score;
    }

    // Replace the existing finalScore method with this improved version
    private int finalScore(long nistScore, int imageQuality, byte[] imageData) {
        if (nistScore < 0) {
            return 0;
        }

        // Calculate additional quality metrics
        int ridgeClarity = analyzeRidgeClarity(imageData, mImageWidth, mImageHeight);
        float edgeQuality = calculateEdgeQuality(imageData, mImageWidth, mImageHeight);
        int patternContinuity = assessPatternContinuity(imageData, mImageWidth, mImageHeight);

        // Normalize NIST score (lower is better in NIST)
        float normalizedNist = (6 - nistScore) * 20; // Convert to 0-100 scale

        // Weighted combination with reduced emphasis on pure darkness
        float combinedScore =
            imageQuality * 0.25f +          // Basic darkness
            normalizedNist * 0.25f +        // Standard quality measure
            ridgeClarity * 0.15f +          // Ridge clarity (important for clean lines)
            edgeQuality * 0.30f +           // Edge quality (important for vectors)
            patternContinuity * 0.05f;      // Pattern consistency

        return Math.round(combinedScore);
    }

    /*private void openDevice() {

//        Toast.makeText(context, "Permission", Toast.LENGTH_SHORT).show();
        debugMessage("Opening SecuGen Device\n");
        long error = sgfplib.OpenDevice(0);
        debugMessage("OpenDevice() ret: " + error + "\n");
        if (error == SGFDxErrorCode.SGFDX_ERROR_NONE) {
            bSecuGenDeviceOpened = true;
            SecuGen.FDxSDKPro.SGDeviceInfoParam deviceInfo = new SecuGen.FDxSDKPro.SGDeviceInfoParam();
            error = sgfplib.GetDeviceInfo(deviceInfo);
            debugMessage("GetDeviceInfo() ret: " + error + "\n");
            mImageWidth = deviceInfo.imageWidth;
            mImageHeight = deviceInfo.imageHeight;
            mImageDPI = deviceInfo.imageDPI;
            serialNumber = new String(deviceInfo.deviceSN());
            debugMessage("Image width: " + mImageWidth + "\n");
            debugMessage("Image height: " + mImageHeight + "\n");
            debugMessage("Image resolution: " + mImageDPI + "\n");
            debugMessage("Serial Number: " + new String(deviceInfo.deviceSN()) + "\n");
            sgfplib.SetTemplateFormat(SGFDxTemplateFormat.TEMPLATE_FORMAT_SG400);
            sgfplib.GetMaxTemplateSize(mMaxTemplateSize);
            debugMessage("TEMPLATE_FORMAT_SG400 SIZE: " + mMaxTemplateSize[0] + "\n");
//                        mRegisterTemplate = new byte[mMaxTemplateSize[0]];
//                        mVerifyTemplate = new byte[mMaxTemplateSize[0]];
//                        EnableControls();
//                        boolean smartCaptureEnabled = this.mToggleButtonSmartCapture.isChecked();
//                        if (smartCaptureEnabled)
//                            sgfplib.WriteData(SGFDxConstant.WRITEDATA_COMMAND_ENABLE_SMART_CAPTURE, (byte) 1);
//                        else
            sgfplib.WriteData(SGFDxConstant.WRITEDATA_COMMAND_ENABLE_SMART_CAPTURE, (byte) 0);
//                        if (mAutoOnEnabled) {
//                            autoOn.start();
//                            DisableControls();
//                        }
        } else {
            debugMessage("Waiting for USB Permission\n");
        }
    }*/

    private void requestPermission2(final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                manager = (UsbManager) cordova.getActivity().getSystemService(Context.USB_SERVICE);
                sgfplib = new JSGFPLib((Context) cordova.getActivity().getBaseContext(), (UsbManager) context.getSystemService(Context.USB_SERVICE));

                mLed = false;

                long error = sgfplib.Init(SGFDxDeviceName.SG_DEV_AUTO);
                if (error != SGFDxErrorCode.SGFDX_ERROR_NONE) {
                    String message = "Fingerprint device initialization failed!";
                    if (error == SGFDxErrorCode.SGFDX_ERROR_DEVICE_NOT_FOUND) {
                        message = "Error: Either a fingerprint device is not attached or the attached fingerprint device is not supported.";
                    }
                    debugMessage(message);
                    PluginResult result = new PluginResult(PluginResult.Status.ERROR, message);
                    result.setKeepCallback(true);
                    callbackContext.sendPluginResult(result);
                } else {
                    UsbDevice usbDevice = sgfplib.GetUsbDevice();
                    if (usbDevice == null) {
                        String message = "Error: Fingerprint sensor not found!";
                        debugMessage(message);
                        PluginResult result = new PluginResult(PluginResult.Status.ERROR, message);
                        result.setKeepCallback(true);
                        callbackContext.sendPluginResult(result);
                    }

                    // create the intent that will be used to get the permission
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(cordova.getActivity(), 0, new Intent(UsbBroadcastReceiver.USB_PERMISSION), PendingIntent.FLAG_MUTABLE);
                    // and a filter on the permission we ask
                    IntentFilter filter = new IntentFilter();
                    filter.addAction(UsbBroadcastReceiver.USB_PERMISSION);
                    // this broadcast receiver will handle the permission results
                    usbReceiver = new UsbBroadcastReceiver(callbackContext, cordova.getActivity());
                    cordova.getActivity().registerReceiver(usbReceiver, filter);
                    // finally ask for the permission
                    manager.requestPermission(usbDevice, pendingIntent);
//                    initDeviceSettings();
                }
            }
        });
    }

    private void exitApplication() {
        cordova.getActivity().finish();
    }

    public void initDeviceSettings() {
        long error;
//        openDeviceRequestCount++;
        error = sgfplib.OpenDevice(0);
        debugMessage("OpenDevice() ret: " + error + "\n");
        boolean isOpened = error == SGFDxErrorCode.SGFDX_ERROR_NONE;

        if (!isOpened) {
            //init setting again...
            /*if(openDeviceRequestCount < 40) {
                initDeviceSettings();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }*/

            return;
        } else {
            debugMessage("Device opened successfully");
//            openDeviceRequestCount = 0;
        }

//        boolean deviceInUse = sgfplib.DeviceInUse();
//        debugMessage("Device In Use  =" + deviceInUse+"");
        SecuGen.FDxSDKPro.SGDeviceInfoParam deviceInfo = new SecuGen.FDxSDKPro.SGDeviceInfoParam();
        error = sgfplib.GetDeviceInfo(deviceInfo);
        debugMessage("GetDeviceInfo() ret: " + error + "\n");
        mImageWidth = deviceInfo.imageWidth;
        mImageHeight = deviceInfo.imageHeight;
        serialNumber = new String(deviceInfo.deviceSN());
        debugMessage("Setting props: mImageWidth: " + mImageWidth + " mImageHeight: " + mImageHeight);
        props = new ScanProperties(mImageWidth, mImageHeight);
//                  sgfplib.SetTemplateFormat(SGFDxTemplateFormat.TEMPLATE_FORMAT_ISO19794);
//                  sgfplib.SetTemplateFormat(SGFDxTemplateFormat.TEMPLATE_FORMAT_SG400);
        Field fieldName;
        try {
            fieldName = SGFDxTemplateFormat.class.getField(FPSecugen.getTemplateFormat());
            short templateValue = fieldName.getShort(null);
            debugMessage("templateValue: " + templateValue);
            sgfplib.SetTemplateFormat(templateValue);
        } catch (NoSuchFieldException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        sgfplib.GetMaxTemplateSize(mMaxTemplateSize);
        debugMessage("mMaxTemplateSize: " + mMaxTemplateSize[0] + "\n");
        mRegisterTemplate = new byte[mMaxTemplateSize[0]];
        sgfplib.writeData((byte) 5, (byte) 1);
    }

    private void closeDevice() {
        if (sgfplib != null) {
            sgfplib.CloseDevice();
            sgfplib.Close();
        }

        if (usbReceiver != null)
            cordova.getActivity().unregisterReceiver(usbReceiver);
    }

    public void capture(final CallbackContext callbackContext) {
        try {
            captureFingerPrint1(callbackContext);
        } catch (IOException ex) {
            ex.printStackTrace();
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, "Exception"));
        } catch (Exception ex) {
//            openDevice();
            ex.printStackTrace();
            Toast.makeText(context, "Capture Again! something went wrong", Toast.LENGTH_SHORT).show();
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, "Exception"));
        }
    }

    private float scoreMappingFunction (long nistScore, int imageQuality) {
        int reverse = -(int)nistScore + 5;
        float linerlyScaled = reverse * 0.8f;
        float scaled = reverse * 20;
        float ImageQualityScaled = customFunction(imageQuality) * 0.2f;
        return (scaled + ImageQualityScaled);
    }

    private float customFunction(float scaledScore) {
        if (scaledScore < 20) {
            return (float) sigmoidLowerBound(scaledScore);
        } else {
            return (float) sigmoidUpperBound(scaledScore);
        }
    }

    private double sigmoidLowerBound (float x) {
        return 100 / (1 + Math.exp((-x + 20) * 0.13));
    }

    private double sigmoidUpperBound (float x) {
        return 100 / (1 + Math.exp((-x + 20) * 0.05));
    }

    public ImageData captureFingerPrint1(final CallbackContext callbackContext) throws IOException {
//        sgfplib.

        long dwTimeStart = 0, dwTimeEnd = 0, dwTimeElapsed = 0;
        byte[] buffer = new byte[mImageWidth * mImageHeight];

        dwTimeStart = System.currentTimeMillis();

        sgfplib.SetLedOn(true);
        long result = sgfplib.GetImageEx(buffer, 30000, 0);

        String NFIQString = "";

//        DumpFile("capture2016.raw", buffer);
        dwTimeEnd = System.currentTimeMillis();
        dwTimeElapsed = dwTimeEnd - dwTimeStart;
        debugMessage("getImageEx(10000,50) ret:" + result + " [" + dwTimeElapsed + "ms]" + NFIQString + "\n");

        int[] quality = new int[1];
        SGFingerInfo fingerInfo = new SGFingerInfo();
        int encodePixelDepth = 8;
        int encodePPI = 500;
        int[] wsqImageOutSize = new int[1];
        byte[] wsqImage = null;

//        Toast.makeText(cordova.getActivity(), "captured", Toast.LENGTH_SHORT).show();//(TAG, byteArrayToBase64(buffer));

        if (result == SGFDxErrorCode.SGFDX_ERROR_NONE) {
            result = sgfplib.GetImageQuality(mImageWidth, mImageHeight, buffer, quality);
            long nistScore = sgfplib.ComputeNFIQEx(buffer, mImageWidth, mImageHeight, 500);

            if (result == SGFDxErrorCode.SGFDX_ERROR_NONE) {
                fingerInfo.FingerNumber = SGFingerPosition.SG_FINGPOS_LI;
                fingerInfo.ImageQuality = quality[0];
                fingerInfo.ImpressionType = SGImpressionType.SG_IMPTYPE_NP;
                fingerInfo.ViewNumber = 1;

                // Calculate metrics for better vectorisation quality assessment
                int ridgeClarity = analyzeRidgeClarity(buffer, mImageWidth, mImageHeight);
                float edgeQuality = calculateEdgeQuality(buffer, mImageWidth, mImageHeight);
                int patternContinuity = assessPatternContinuity(buffer, mImageWidth, mImageHeight);

                // Use improved final score calculation
                int finalScore = finalScore(nistScore, fingerInfo.ImageQuality, buffer);

                Log.d("FP Image Quality", fingerInfo.ImageQuality + "");
                Log.d("FP NIST Score", nistScore + "");
                Log.d("FP Ridge Clarity", ridgeClarity + "");
                Log.d("FP Edge Quality", edgeQuality + "");
                Log.d("FP Pattern Continuity", patternContinuity + "");
                Log.d("FP Final Score", finalScore + "");

                if (fingerInfo.ImageQuality >= QUALITY_VALUE) {

                    result = sgfplib.WSQGetEncodedImageSize(wsqImageOutSize,
                            SGWSQLib.BITRATE_5_TO_1, buffer, mImageWidth,
                            mImageHeight, encodePixelDepth, encodePPI);

                    if (result == SGFDxErrorCode.SGFDX_ERROR_NONE) {
                        wsqImage = new byte[wsqImageOutSize[0]];
                        result = sgfplib.WSQEncode(wsqImage,
                                SGWSQLib.BITRATE_5_TO_1, buffer,
                                mImageWidth, mImageHeight, encodePixelDepth,
                                encodePPI);

//                        sgfplib.WS

                        if (result == SGFDxErrorCode.SGFDX_ERROR_NONE) {
                            //TODO send base64 image
                            Bitmap bitmap = this.toGrayscale(buffer);
//                            Utils.saveImageFile(context, callbackContext, bitmap, "fp");
//                            File file = new java.io.File(Environment
//                                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
//                                    +"fp.png");
//
//                            Log.d("FP Path", file.getAbsolutePath());

                            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
                            byte[] byteArray = byteArrayOutputStream.toByteArray();

                            String encoded = Base64.encodeToString(byteArray, Base64.DEFAULT);
                            String wsqEncoded = Base64.encodeToString(wsqImage, Base64.DEFAULT);
//                            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
//                            bos.write(buffer);
//                            bos.flush();
//                            bos.close();
                            JSONObject json = new JSONObject();
                            try {
                                // Inside the try block where you build your JSON response:
                                json.put("image", encoded);
                                json.put("wsqImage", wsqEncoded);
                                json.put("errorCode", 0);
                                json.put("quality", fingerInfo.ImageQuality);
                                json.put("nistScore", nistScore);
                                json.put("ridgeClarity", ridgeClarity);
                                json.put("edgeQuality", edgeQuality);
                                json.put("patternContinuity", patternContinuity);
                                json.put("finalScore", finalScore);
                                json.put("serialNumber", serialNumber);
                            } catch (JSONException ex) {
                                Log.d("Exception", "JSON Exception");
                            }

                            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, json);
                            callbackContext.sendPluginResult(pluginResult);
                            return new ImageData(0);
                        }
                    }
                } else {
                    result = 10001;

                    //send callback error
                    PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, "Quality of the fingerprint is less than " + QUALITY_VALUE);
                    callbackContext.sendPluginResult(pluginResult);
                }

//                buffer


            } else {
                PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, "Quality Error Code: " + result);
                callbackContext.sendPluginResult(pluginResult);
            }
        } else {
            PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, result);
            callbackContext.sendPluginResult(pluginResult);
        }


        buffer = null;

        return new ImageData(result, buffer, wsqImage, fingerInfo.ImageQuality);
    }

    //Converts image to grayscale (NEW)
    public Bitmap toGrayscale(byte[] mImageBuffer) {
        byte[] Bits = new byte[mImageBuffer.length * 4];
        for (int i = 0; i < mImageBuffer.length; i++) {
            Bits[i * 4] = Bits[i * 4 + 1] = Bits[i * 4 + 2] = mImageBuffer[i]; // Invert the source bits
            Bits[i * 4 + 3] = -1;// 0xff, that's the alpha.
        }

        Bitmap bmpGrayscale = Bitmap.createBitmap(mImageWidth, mImageHeight, Bitmap.Config.ARGB_8888);
        //Bitmap bm contains the fingerprint img
        bmpGrayscale.copyPixelsFromBuffer(ByteBuffer.wrap(Bits));
        return bmpGrayscale;
    }

    public static String getTemplateFormat() {
        return templateFormat;
    }

    public static String getTemplatePath() {
        return templatePath;
    }

    public static void setTemplatePath(String templatePath) {
        FPSecugen.templatePath = templatePath;
    }

    private void debugMessage(String message) {
        Log.d("Cordova FP", message);
    }

    public class UsbBroadcastReceiver extends BroadcastReceiver {
        // logging tag
        private final String TAG = "UsbBroadcastReceiver";
        // usb permission tag name
        public static final String USB_PERMISSION = "com.example.plugin.USB_PERMISSION";
        // cordova callback context to notify the success/error to the cordova app
        private CallbackContext callbackContext;
        // cordova activity to use it to unregister this broadcast receiver
        private Activity activity;

        /**
         * Custom broadcast receiver that will handle the cordova callback context
         *
         * @param callbackContext
         * @param activity
         */
        public UsbBroadcastReceiver(CallbackContext callbackContext, Activity activity) {
            this.callbackContext = callbackContext;
            this.activity = activity;
        }

        public UsbBroadcastReceiver(Activity activity) {
            this.activity = activity;
        }

//        private int count = 0;


        /**
         * Handle permission answer
         *
         * @param context
         * @param intent
         * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (USB_PERMISSION.equals(action)) {
                // deal with the user answer about the permission
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    Log.d(TAG, "Permission to connect to the device was accepted!");
                    initDeviceSettings();
                    if (callbackContext != null)
                        callbackContext.success("Permission to connect to the device was accepted!");
                } else {
                    Log.d(TAG, "Permission to connect to the device was denied!");
                    if (callbackContext != null)
                        callbackContext.error("Permission to connect to the device was denied!");
                }
                // unregister the broadcast receiver since it's no longer needed
                activity.unregisterReceiver(this);
            }
        }
    }
}
