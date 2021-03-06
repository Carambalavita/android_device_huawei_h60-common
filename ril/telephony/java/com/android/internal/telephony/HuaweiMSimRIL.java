/*
 * Copyright (C) 2015 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.telephony;

import android.os.Message;
import android.os.Parcel;
import android.os.SystemProperties;
import android.content.Context;

import android.telephony.Rlog;
import android.telephony.SignalStrength;

public class HuaweiMSimRIL extends RIL implements CommandsInterface {

    public static final int NETWORK_TYPE_TDS = 0x11;
    public static final int NETWORK_TYPE_TDS_HSDPA = 0x12; //=> 8
    public static final int NETWORK_TYPE_TDS_HSUPA = 0x13; //=> 9

    public HuaweiMSimRIL(Context context, int networkMode, int cdmaSubscription) {
        this(context, networkMode, cdmaSubscription, null);
    }

    public HuaweiMSimRIL(Context context, int networkMode, int cdmaSubscription, Integer instanceId) {
        super(context, networkMode, cdmaSubscription, instanceId);
    }

    @Override
    protected Object
    responseSignalStrength(Parcel p) {
        int[] response = new int[16];
        for (int i = 0 ; i < 16 ; i++) {
            response[i] = p.readInt();
        }

        int gsmSignalStrength = response[0]; // Valid values are (0-31, 99) as defined in TS 27.007 8.5
        int gsmBitErrorRate = response[1]; // bit error rate (0-7, 99) as defined in TS 27.007 8.5
        int mWcdmaRscp = response[2]; // added by huawei
        int mWcdmaEcio = response[3]; // added by huawei
        int cdmaDbm = response[4];
        int cdmaEcio = response[5];
        int evdoDbm = response[6]; // -75 to -105, 99
        int evdoEcio = response[7];
        int evdoSnr = response[8]; // Valid values are 0-8.  8 is the highest signal to noise ratio
        int lteSignalStrength = response[9]; // 0 to 12, 63
        int lteRsrp = response[10]; // -85 to -140, -44
        int lteRsrq = response[11]; // -3 to -20
        int lteRssnr = response[12]; // 130 to -30, -200
        int lteCqi = response[13];
        int mGsm = response[14];
        int mRat = response[15]; // added by huawei       

        Rlog.e(RILJ_LOG_TAG, "---------- HEX ----------");
        Rlog.e(RILJ_LOG_TAG, "gsmSignalStrength:" + String.format("%x", gsmSignalStrength));
        Rlog.e(RILJ_LOG_TAG, "gsmBitErrorRate:" + String.format("%x", gsmBitErrorRate));
        Rlog.e(RILJ_LOG_TAG, "mWcdmaRscp:" + String.format("%x", mWcdmaRscp));
        Rlog.e(RILJ_LOG_TAG, "mWcdmaEcio:" + String.format("%x", mWcdmaEcio));
        Rlog.e(RILJ_LOG_TAG, "cdmaDbm:" + String.format("%x", cdmaDbm));
        Rlog.e(RILJ_LOG_TAG, "cdmaEcio:" + String.format("%x", cdmaEcio));
        Rlog.e(RILJ_LOG_TAG, "evdoDbm:" + String.format("%x", evdoDbm));
        Rlog.e(RILJ_LOG_TAG, "evdoEcio:" + String.format("%x", evdoEcio));
        Rlog.e(RILJ_LOG_TAG, "evdoSnr:" + String.format("%x", evdoSnr));
        Rlog.e(RILJ_LOG_TAG, "lteSignalStrength:" + String.format("%x", lteSignalStrength));
        Rlog.e(RILJ_LOG_TAG, "lteRsrp:" + String.format("%x", lteRsrp));
        Rlog.e(RILJ_LOG_TAG, "lteRsrq:" + String.format("%x", lteRsrq));
        Rlog.e(RILJ_LOG_TAG, "lteRssnr:" + String.format("%x", lteRssnr));
        Rlog.e(RILJ_LOG_TAG, "lteCqi:" + String.format("%x", lteCqi));
        Rlog.e(RILJ_LOG_TAG, "mGsm:" + String.format("%x", mGsm));
        Rlog.e(RILJ_LOG_TAG, "mRat:" + String.format("%x", mRat));

        if (lteRsrp != 0) // LTE
        {
            if (lteRsrp > -44) lteSignalStrength = 64; // None or Unknown
            else if (lteRsrp >= -85) lteSignalStrength = 63; // Great
            else if (lteRsrp >= -95) lteSignalStrength = 11; // Good
            else if (lteRsrp >= -105) lteSignalStrength = 7; // Moderate
            else if (lteRsrp >= -115) lteSignalStrength = 4; // Poor
            else if (lteRsrp >= -140) lteSignalStrength = 64; // None or Unknown
        }
        else if (gsmSignalStrength == 0 && lteRsrp == 0) // 3G
        {  
            lteRsrp = (mWcdmaRscp & 0xFF) - 256;
            lteRsrq = (mWcdmaEcio & 0xFF) - 256;

            if (lteRsrp > -44) { // None or Unknown
                lteSignalStrength = 64;
                lteRssnr = -200;
            } else if (lteRsrp >= -85) { // Great
                lteSignalStrength = 63;
                lteRssnr = 300;
            } else if (lteRsrp >= -95) { // Good
                lteSignalStrength = 11;
                lteRssnr = 129;
            } else if (lteRsrp >= -105) { // Moderate
                lteSignalStrength = 7;
                lteRssnr = 44;
            } else if (lteRsrp >= -115) { // Poor
                lteSignalStrength = 4;
                lteRssnr = 9;
            } else if (lteRsrp >= -140) { // None or Unknown
                lteSignalStrength = 64;
                lteRssnr = -200;
            }
        }
        else if (mWcdmaRscp == 0 && lteRsrp == 0) // 2G
        {         
            lteRsrp = (gsmSignalStrength & 0xFF) - 256;

            if (lteRsrp > -44) { // None or Unknown
                lteSignalStrength = 64;
                lteRsrq = -21;
                lteRssnr = -200;
            } else if (lteRsrp >= -85) { // Great
                lteSignalStrength = 63;
                lteRsrq = -3;
                lteRssnr = 300;
            } else if (lteRsrp >= -95) { // Good
                lteSignalStrength = 11;
                lteRsrq = -7;
                lteRssnr = 129;
            } else if (lteRsrp >= -105) { // Moderate
                lteSignalStrength = 7;
                lteRsrq = -12;
                lteRssnr = 44;
            } else if (lteRsrp >= -115) { // Poor
                lteSignalStrength = 4;
                lteRsrq = -17;
                lteRssnr = 9;
            } else if (lteRsrp >= -140) { // None or Unknown
                lteSignalStrength = 64;
                lteRsrq = -21;
                lteRssnr = -200;
            }
        }

        gsmSignalStrength = 0;
        gsmBitErrorRate = 0;
        cdmaDbm = -1;
        cdmaEcio = -1;
        evdoDbm = -1;
        evdoEcio = -1;
        evdoSnr = -1;

        Rlog.e(RILJ_LOG_TAG, "---------- MOD ----------");
        Rlog.e(RILJ_LOG_TAG, "lteSignalStrength:" + lteSignalStrength);
        Rlog.e(RILJ_LOG_TAG, "lteRsrp:" + lteRsrp);
        Rlog.e(RILJ_LOG_TAG, "lteRsrq:" + lteRsrq);
        Rlog.e(RILJ_LOG_TAG, "lteRssnr:" + lteRssnr);
        Rlog.e(RILJ_LOG_TAG, "lteCqi:" + lteCqi);
        Rlog.e(RILJ_LOG_TAG, "-------------------------");

        SignalStrength signalStrength = new SignalStrength(
            gsmSignalStrength, gsmBitErrorRate, cdmaDbm, cdmaEcio, evdoDbm, 
            evdoEcio, evdoSnr, lteSignalStrength, -lteRsrp, -lteRsrq, 
            lteRssnr, lteCqi, true);

        return signalStrength;
    }
}
